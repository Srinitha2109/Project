package com.example.comproject.service;

import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.example.comproject.dto.ClaimDTO;
import com.example.comproject.dto.ClaimDocumentDTO;
import com.example.comproject.entity.Claim;
import com.example.comproject.entity.PolicyApplication;
import com.example.comproject.exception.InvalidOperationException;
import com.example.comproject.exception.ResourceNotFoundException;
import com.example.comproject.repository.ClaimOfficerRepository;
import com.example.comproject.repository.ClaimRepository;
import com.example.comproject.repository.ClaimDocumentRepository;
import com.example.comproject.repository.PolicyApplicationRepository;
import com.example.comproject.repository.OCRResultRepository;
import com.example.comproject.entity.OCRResult;
import com.example.comproject.entity.ClaimDocument;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ClaimService {
    private final ClaimRepository claimRepository;
    private final PolicyApplicationRepository policyApplicationRepository;
    private final ClaimOfficerRepository claimOfficerRepository;
    private final ClaimDocumentRepository claimDocumentRepository;
    private final FileStorageService fileStorageService;
    private final ClaimDocumentService claimDocumentService;
    private final AppNotificationService notificationService;
    
    @Autowired
    private OCRResultRepository ocrResultRepository;
    
    @Autowired
    private TesseractOCRService tesseractOCRService;
    
    @Autowired
    private GeminiDataExtractorService geminiDataExtractorService;

    public ClaimService(ClaimRepository claimRepository,
                       PolicyApplicationRepository policyApplicationRepository,
                       ClaimOfficerRepository claimOfficerRepository,
                       ClaimDocumentRepository claimDocumentRepository,
                       FileStorageService fileStorageService,
                       ClaimDocumentService claimDocumentService,
                       AppNotificationService notificationService) {
        this.claimRepository = claimRepository;
        this.policyApplicationRepository = policyApplicationRepository;
        this.claimOfficerRepository = claimOfficerRepository;
        this.claimDocumentRepository = claimDocumentRepository;
        this.fileStorageService = fileStorageService;
        this.claimDocumentService = claimDocumentService;
        this.notificationService = notificationService;
    }

    public ClaimDTO createClaim(ClaimDTO dto, List<org.springframework.web.multipart.MultipartFile> documents) {
        Claim claim = new Claim();
        claim.setClaimNumber(generateClaimNumber());
        PolicyApplication app = policyApplicationRepository.findById(dto.getPolicyApplicationId()).orElseThrow();
        
        // Prevent new claim if there is an active pending claim
        boolean hasActiveClaim = claimRepository.findByPolicyApplicationId(app.getId()).stream()
            .anyMatch(c -> c.getStatus() == Claim.ClaimStatus.SUBMITTED || 
                          c.getStatus() == Claim.ClaimStatus.ASSIGNED || 
                          c.getStatus() == Claim.ClaimStatus.UNDER_INVESTIGATION);
        
        if (hasActiveClaim) {
            throw new InvalidOperationException("You already have an active claim under review. Please wait for it to be processed before raising another.");
        }

        claim.setPolicyApplication(app);

        // Check available coverage balance
        java.math.BigDecimal totalSettled = claimRepository.findByPolicyApplicationId(app.getId()).stream()
            .filter(c -> c.getStatus() == Claim.ClaimStatus.SETTLED || 
                        c.getStatus() == Claim.ClaimStatus.APPROVED ||
                        c.getStatus() == Claim.ClaimStatus.PARTIALLY_APPROVED)
            .map(Claim::getClaimAmount)
            .filter(amt -> amt != null)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        java.math.BigDecimal activeLimit = app.getCurrentAggregateLimit() != null ? app.getCurrentAggregateLimit() : app.getSelectedCoverageAmount();
        java.math.BigDecimal availableBalance = activeLimit.subtract(totalSettled);
        if (dto.getClaimAmount().compareTo(availableBalance) > 0) {
            throw new InvalidOperationException("Claim amount exceeds available coverage balance: Rs. " + availableBalance);
        }
        
        if (app.getBusinessProfile() != null && app.getBusinessProfile().getClaimOfficer() != null) {
            claim.setClaimOfficer(app.getBusinessProfile().getClaimOfficer());
        } else if (app.getClaimOfficer() != null) {
            claim.setClaimOfficer(app.getClaimOfficer());
        }

        claim.setDescription(dto.getDescription());
        claim.setClaimAmount(dto.getClaimAmount());
        claim.setIncidentDate(dto.getIncidentDate());
        claim.setIncidentLocation(dto.getIncidentLocation());
        claim.setStatus(Claim.ClaimStatus.SUBMITTED);
        
        Claim savedClaim = claimRepository.save(claim);
        
        if (documents != null && !documents.isEmpty()) {
            for (org.springframework.web.multipart.MultipartFile file : documents) {
                String fileName = fileStorageService.storeFile(file);
                com.example.comproject.entity.ClaimDocument doc = new com.example.comproject.entity.ClaimDocument();
                doc.setClaim(savedClaim);
                doc.setFileName(file.getOriginalFilename());
                doc.setFilePath(fileName);
                doc.setFileType(file.getContentType());
                doc.setFileSize(file.getSize());
                doc.setUploadedAt(java.time.LocalDateTime.now());
                com.example.comproject.entity.ClaimDocument savedDoc = claimDocumentService.uploadDocument(doc);
                
                // Re-run OCR instantly and save to DB silently for pure backend validation tracking
                try {
                    String extractedText = tesseractOCRService.extractTextFromPdf(file);
                    String rawJson = geminiDataExtractorService.extractStructuredData(extractedText, claim.getPolicyApplication().getPlan().getPolicyName());
                    
                    OCRResult ocrResult = new OCRResult();
                    ocrResult.setClaimDocument(savedDoc);
                    ocrResult.setProcessingStatus(OCRResult.ProcessingStatus.COMPLETED);
                    ocrResult.setRawExtractedText(extractedText);
                    // Just store raw Json dump mappings in desc
                    ocrResult.setExtractedDamageDesc(rawJson);
                    ocrResultRepository.save(ocrResult);
                } catch(Exception e) {
                   System.err.println("Backend OCR DB Save execution failed, skipped.");
                }
                
            }
        }
        
        ClaimDTO result = toDTO(savedClaim);

        // Notify assigned claim officer
        if (savedClaim.getClaimOfficer() != null && savedClaim.getClaimOfficer().getUser() != null) {
            notificationService.notify(
                savedClaim.getClaimOfficer().getUser(),
                "New claim (" + savedClaim.getClaimNumber() + ") has been raised by "
                    + app.getUser().getFullName() + " under policy " + app.getPolicyNumber() + ". Please review.",
                "CLAIM_RAISED"
            );
        }
        // Notify assigned agent
        if (app.getAgent() != null && app.getAgent().getUser() != null) {
            notificationService.notify(
                app.getAgent().getUser(),
                "A claim (" + savedClaim.getClaimNumber() + ") has been raised by your client "
                    + app.getUser().getFullName() + " under policy " + app.getPolicyNumber() + ".",
                "CLAIM_RAISED"
            );
        }

        return result;
    }

    public ClaimDTO assignClaimOfficer(Long claimId, Long claimOfficerId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found"));
        
        if (claim.getStatus() != Claim.ClaimStatus.SUBMITTED) {
            throw new InvalidOperationException("Can only assign officer to SUBMITTED claims");
        }

        claim.setClaimOfficer(claimOfficerRepository.findById(claimOfficerId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim officer not found")));
        claim.setStatus(Claim.ClaimStatus.ASSIGNED);
        
        return toDTO(claimRepository.save(claim));
    }

    public ClaimDTO approveClaim(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found"));
        
        if (claim.getClaimOfficer() == null) {
            throw new InvalidOperationException("Claim officer must be assigned");
        }

        // Check available coverage balance before approval
        PolicyApplication app = claim.getPolicyApplication();
        java.math.BigDecimal currentTotalApproved = claimRepository.findByPolicyApplicationId(app.getId()).stream()
            .filter(c -> (c.getStatus() == Claim.ClaimStatus.SETTLED || 
                         c.getStatus() == Claim.ClaimStatus.APPROVED ||
                         c.getStatus() == Claim.ClaimStatus.PARTIALLY_APPROVED) && 
                         !c.getId().equals(claim.getId())) // Exclude self if already approved (unlikely but safe)
            .map(Claim::getClaimAmount)
            .filter(amt -> amt != null)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        java.math.BigDecimal currentLimit = app.getCurrentAggregateLimit() != null ? app.getCurrentAggregateLimit() : app.getSelectedCoverageAmount();
        java.math.BigDecimal remainingBalance = currentLimit.subtract(currentTotalApproved);
        if (claim.getClaimAmount().compareTo(remainingBalance) > 0) {
            throw new InvalidOperationException("Approving this claim would exceed the available coverage balance: Rs. " + remainingBalance);
        }

        claim.setStatus(Claim.ClaimStatus.APPROVED);
        Claim savedClaim = claimRepository.save(claim);

        // After approval, check balance thresholds for notifications
        BigDecimal totalSettledAfter = currentTotalApproved.add(claim.getClaimAmount());
        
        java.math.MathContext mc = new java.math.MathContext(4);
        BigDecimal usagePercent = totalSettledAfter.divide(currentLimit, mc).multiply(new BigDecimal("100"));

        if (usagePercent.compareTo(new BigDecimal("100")) >= 0) {
            app.setStatus(PolicyApplication.ApplicationStatus.EXPIRED);
            policyApplicationRepository.save(app);
            String renewalFrom = app.getEndDate() != null ? 
                app.getEndDate().getMonth().name().substring(0, 3) + " " + app.getEndDate().getYear() : "next period";
            notificationService.notify(app.getUser(), 
                "Your claim balance is exhausted. Your policy is now EXPIRED. No further claims can be processed until renewal. Policy renewal available from " + renewalFrom, 
                "POLICY_EXHAUSTED");
            // Auto-initiate renewal logic could be called here or handled separately
        } else if (usagePercent.compareTo(new BigDecimal("90")) >= 0) {
            BigDecimal remaining = currentLimit.subtract(totalSettledAfter);
            notificationService.notify(app.getUser(), 
                "Critical: 90% of your claim limit used. Only Rs. " + remaining + " remaining. Consider policy top-up.", 
                "BALANCE_WARNING_CRITICAL");
        } else if (usagePercent.compareTo(new BigDecimal("75")) >= 0) {
            notificationService.notify(app.getUser(), 
                "Warning: 75% of your annual claim limit has been used.", 
                "BALANCE_WARNING_HIGH");
        }

        // Notify the policyholder about claim approval
        if (claim.getPolicyApplication() != null && claim.getPolicyApplication().getUser() != null) {
            notificationService.notify(
                claim.getPolicyApplication().getUser(),
                "Your claim (" + claim.getClaimNumber() + ") has been approved by the claims officer.",
                "CLAIM_APPROVED"
            );
        }

        return toDTO(savedClaim);
    }

    public ClaimDTO rejectClaim(Long claimId, String reason) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found"));
        
        if (claim.getClaimOfficer() == null) {
            throw new InvalidOperationException("Claim officer must be assigned");
        }

        claim.setStatus(Claim.ClaimStatus.REJECTED);
        claim.setRejectionReason(reason);
        ClaimDTO result = toDTO(claimRepository.save(claim));

        // Notify the policyholder
        if (claim.getPolicyApplication() != null && claim.getPolicyApplication().getUser() != null) {
            notificationService.notify(
                claim.getPolicyApplication().getUser(),
                "Your claim (" + claim.getClaimNumber() + ") has been rejected. Reason: " + reason,
                "CLAIM_REJECTED"
            );
        }

        return result;
    }

    private String generateClaimNumber() {
        long uniquePart = (System.nanoTime() % 1000000);
        return String.format("CLM-%06d", uniquePart);
    }

    public ClaimDTO getClaimById(Long id) {
        return claimRepository.findById(id).map(this::toDTO).orElse(null);
    }

    public List<ClaimDTO> getClaimsByPolicyApplication(Long policyApplicationId) {
        return claimRepository.findByPolicyApplicationId(policyApplicationId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<ClaimDTO> getClaimsByClaimOfficer(Long claimOfficerId) {
        return claimRepository.findByClaimOfficerIdOrPolicyApplicationClaimOfficerIdOrPolicyApplicationBusinessProfileClaimOfficerId(
            claimOfficerId, claimOfficerId, claimOfficerId
        ).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<ClaimDTO> getClaimsByUserId(Long userId) {
        return claimRepository.findAll().stream()
                .filter(c -> c.getPolicyApplication().getUser().getId() == userId)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ClaimDTO updateClaim(Long id, ClaimDTO dto) {
        if (claimRepository.existsById(id)) {
            Claim claim = claimRepository.findById(id).orElseThrow();
            claim.setDescription(dto.getDescription());
            claim.setClaimAmount(dto.getClaimAmount());
            claim.setIncidentDate(dto.getIncidentDate());
            claim.setIncidentLocation(dto.getIncidentLocation());
            claim.setStatus(dto.getStatus());
            if (dto.getClaimOfficerId() != null) {
                claim.setClaimOfficer(claimOfficerRepository.findById(dto.getClaimOfficerId()).orElse(null));
            }
            return toDTO(claimRepository.save(claim));
        }
        return null;
    }

    private ClaimDTO toDTO(Claim claim) {
        ClaimDTO dto = new ClaimDTO();
        dto.setId(claim.getId());
        dto.setClaimNumber(claim.getClaimNumber());
        dto.setPolicyApplicationId(claim.getPolicyApplication().getId());
        dto.setPolicyNumber(claim.getPolicyApplication().getPolicyNumber());
        dto.setDescription(claim.getDescription());
        dto.setClaimAmount(claim.getClaimAmount());
        dto.setIncidentDate(claim.getIncidentDate());
        dto.setIncidentLocation(claim.getIncidentLocation());
        dto.setStatus(claim.getStatus());
        if (claim.getClaimOfficer() != null) dto.setClaimOfficerId(claim.getClaimOfficer().getId());
        dto.setRejectionReason(claim.getRejectionReason());
        
        // Add policyholder name and plan name
        if (claim.getPolicyApplication() != null) {
            if (claim.getPolicyApplication().getUser() != null) {
                dto.setPolicyholderName(claim.getPolicyApplication().getUser().getFullName());
            }
            if (claim.getPolicyApplication().getPlan() != null) {
                dto.setPlanName(claim.getPolicyApplication().getPlan().getPolicyName());
            }
        }

        // Populate documents
        List<ClaimDocument> docs = claimDocumentRepository.findByClaimId(claim.getId());
        if (docs != null) {
            dto.setDocuments(docs.stream().map(d -> {
                ClaimDocumentDTO ddto = new ClaimDocumentDTO();
                ddto.setId(d.getId());
                ddto.setFileName(d.getFileName());
                ddto.setFilePath(d.getFilePath());
                ddto.setFileType(d.getFileType());
                ddto.setFileSize(d.getFileSize());
                ddto.setUploadedAt(d.getUploadedAt());
                return ddto;
            }).collect(Collectors.toList()));
            
            // Extract OCR amount for fraud detection
            BigDecimal maxOcrAmount = null;
            for (ClaimDocument doc : docs) {
                java.util.Optional<OCRResult> ocrOpt = ocrResultRepository.findByClaimDocumentId(doc.getId());
                if (ocrOpt.isPresent()) {
                    OCRResult ocr = ocrOpt.get();
                    if (ocr.getExtractedDamageDesc() != null) {
                        try {
                            org.json.JSONObject jsonObj = new org.json.JSONObject(ocr.getExtractedDamageDesc());
                            if (jsonObj.has("extractedAmount") && !jsonObj.isNull("extractedAmount")) {
                                String amtStr = String.valueOf(jsonObj.get("extractedAmount")).replaceAll("[^0-9.]", "");
                                if (!amtStr.isEmpty()) {
                                    BigDecimal amt = new BigDecimal(amtStr);
                                    if (maxOcrAmount == null || amt.compareTo(maxOcrAmount) > 0) {
                                        maxOcrAmount = amt;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // ignore json parse error
                        }
                    }
                }
            }
            boolean amountMismatch = false;
            if (maxOcrAmount != null) {
                dto.setOcrExtractedAmount(maxOcrAmount);
                if (dto.getClaimAmount() != null && dto.getClaimAmount().compareTo(maxOcrAmount) > 0) {
                    amountMismatch = true;
                }
            }

            // Check for Frequent Claiming Fraud (multiple claims within 30 days)
            boolean frequentClaimSuspect = false;
            PolicyApplication app = claim.getPolicyApplication();
            if (app != null && app.getClaims() != null && claim.getIncidentDate() != null) {
                for (Claim otherClaim : app.getClaims()) {
                    if (!otherClaim.getId().equals(claim.getId()) && otherClaim.getIncidentDate() != null) {
                        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                            otherClaim.getIncidentDate(), 
                            claim.getIncidentDate()
                        );
                        if (Math.abs(daysBetween) <= 30) {
                            frequentClaimSuspect = true;
                            break;
                        }
                    }
                }
            }

            // Flag as suspected fraud if either condition is met
            if (amountMismatch || frequentClaimSuspect) {
                dto.setFraudSuspected(true);
                StringBuilder reasons = new StringBuilder();
                if (amountMismatch) reasons.append("Amount Mismatch (Inflation) ");
                if (frequentClaimSuspect) reasons.append("Frequent Claiming (Churn Suspect)");
                dto.setFraudReason(reasons.toString().trim());
            } else {
                dto.setFraudSuspected(false);
            }
        }
        
        return dto;
    }

}