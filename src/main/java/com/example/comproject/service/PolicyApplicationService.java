package com.example.comproject.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.example.comproject.dto.PolicyApplicationDTO;
import com.example.comproject.dto.GeneralLiabilityApplicationRequestDTO;
import com.example.comproject.dto.GeneralLiabilityDetailDTO;
import com.example.comproject.dto.ClaimDTO;
import com.example.comproject.dto.PolicyTopUpDTO;
import com.example.comproject.entity.GeneralLiabilityDetail;
import com.example.comproject.entity.PolicyTopUp;
import com.example.comproject.repository.PolicyTopUpRepository;
import com.example.comproject.entity.Agent;
import com.example.comproject.entity.BusinessProfile;
import com.example.comproject.entity.Claim;
import com.example.comproject.entity.ClaimOfficer;
import com.example.comproject.entity.Policy;
import com.example.comproject.entity.PolicyApplication;
import com.example.comproject.entity.User;
import com.example.comproject.exception.InvalidOperationException;
import com.example.comproject.exception.ResourceNotFoundException;
import com.example.comproject.repository.AgentRepository;
import com.example.comproject.repository.BusinessProfileRepository;
import com.example.comproject.repository.ClaimOfficerRepository;
import com.example.comproject.repository.ClaimRepository;
import com.example.comproject.repository.PolicyApplicationRepository;
import com.example.comproject.repository.PolicyRepository;
import com.example.comproject.repository.UserRepository;

@Service
public class PolicyApplicationService {
    private final PolicyApplicationRepository policyApplicationRepository;
    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final AgentRepository agentRepository;
    private final ClaimOfficerRepository claimOfficerRepository;
    private final ClaimRepository claimRepository;
    private final PolicyTopUpRepository topUpRepository;
    private final AppNotificationService notificationService;

    public PolicyApplicationService(PolicyApplicationRepository policyApplicationRepository,
                                   UserRepository userRepository,
                                   PolicyRepository policyRepository,
                                   BusinessProfileRepository businessProfileRepository,
                                   AgentRepository agentRepository,
                                   ClaimOfficerRepository claimOfficerRepository,
                                   ClaimRepository claimRepository,
                                   PolicyTopUpRepository topUpRepository,
                                   AppNotificationService notificationService) {
        this.policyApplicationRepository = policyApplicationRepository;
        this.userRepository = userRepository;
        this.policyRepository = policyRepository;
        this.businessProfileRepository = businessProfileRepository;
        this.agentRepository = agentRepository;
        this.claimOfficerRepository = claimOfficerRepository;
        this.claimRepository = claimRepository;
        this.topUpRepository = topUpRepository;
        this.notificationService = notificationService;
    }

    // Step 1: Policyholder submits application
    public PolicyApplicationDTO createApplication(PolicyApplicationDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (user.getRole() != User.Role.POLICYHOLDER) {
            throw new InvalidOperationException("Only policyholders can apply for policies");
        }

        Policy policy = policyRepository.findById(dto.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));

        BusinessProfile businessProfile = businessProfileRepository.findById(dto.getBusinessProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found"));

        if (!businessProfile.getIsProfileCompleted()) {
            throw new InvalidOperationException("Please complete your business profile before applying");
        }

        if (dto.getSelectedCoverageAmount().compareTo(policy.getMinCoverageAmount()) < 0 ||
            dto.getSelectedCoverageAmount().compareTo(policy.getMaxCoverageAmount()) > 0) {
            throw new InvalidOperationException("Coverage amount must be between " + 
                policy.getMinCoverageAmount() + " and " + policy.getMaxCoverageAmount());
        }

        BigDecimal calculatedPremium = calculatePremium(policy, dto.getSelectedCoverageAmount(), businessProfile, 
                dto.getPaymentPlan() != null ? dto.getPaymentPlan() : PolicyApplication.PaymentPlan.MONTHLY);

        PolicyApplication application = new PolicyApplication();
        application.setUser(user);
        application.setPlan(policy);
        application.setBusinessProfile(businessProfile);
        application.setSelectedCoverageAmount(dto.getSelectedCoverageAmount());
        application.setPaymentPlan(dto.getPaymentPlan() != null ? dto.getPaymentPlan() : PolicyApplication.PaymentPlan.MONTHLY);
        application.setPremiumAmount(calculatedPremium);
        application.setStatus(PolicyApplication.ApplicationStatus.UNDER_REVIEW);
        application.setPolicyNumber(generateApplicationNumber());

        // Auto-assign agent and claim officer from policyholder's business profile
        if (businessProfile.getAgent() != null) {
            Agent agent = businessProfile.getAgent();
            application.setAgent(agent);
            if (agent.getCommissionRate() != null) {
                BigDecimal commission = calculatedPremium
                        .multiply(agent.getCommissionRate())
                        .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                application.setCommissionAmount(commission);
            }
        }
        if (businessProfile.getClaimOfficer() != null) {
            application.setClaimOfficer(businessProfile.getClaimOfficer());
        }

        PolicyApplicationDTO saved = toDTO(policyApplicationRepository.save(application));

        // Notify assigned agent
        if (application.getAgent() != null && application.getAgent().getUser() != null) {
            notificationService.notify(
                application.getAgent().getUser(),
                "New policy application (" + application.getPolicyNumber() + ") for \""
                    + policy.getPolicyName() + "\" has been submitted and requires your review.",
                "APPLICATION_SUBMITTED"
            );
        }
        // Notify assigned claim officer
        if (application.getClaimOfficer() != null && application.getClaimOfficer().getUser() != null) {
            notificationService.notify(
                application.getClaimOfficer().getUser(),
                "A new policy application (" + application.getPolicyNumber() + ") has been submitted by "
                    + user.getFullName() + ". You are assigned as claims officer.",
                "APPLICATION_SUBMITTED"
            );
        }

        return saved;
    }

    public PolicyApplicationDTO createGeneralLiabilityApplication(GeneralLiabilityApplicationRequestDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != User.Role.POLICYHOLDER) {
            throw new InvalidOperationException("Only policyholders can apply for policies");
        }

        Policy policy = policyRepository.findById(dto.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));

        BusinessProfile businessProfile = businessProfileRepository.findById(dto.getBusinessProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found"));

        if (!businessProfile.getIsProfileCompleted()) {
            throw new InvalidOperationException("Please complete your business profile before applying");
        }

        BigDecimal calculatedPremium = calculateGLPremium(policy, dto.getSelectedCoverageAmount(), businessProfile, 
                dto.getPaymentPlan(), dto.getGlDetail());

        PolicyApplication application = new PolicyApplication();
        application.setUser(user);
        application.setPlan(policy);
        application.setBusinessProfile(businessProfile);
        application.setSelectedCoverageAmount(dto.getSelectedCoverageAmount());
        application.setPaymentPlan(dto.getPaymentPlan());
        application.setPremiumAmount(calculatedPremium);
        application.setStatus(PolicyApplication.ApplicationStatus.SUBMITTED);
        application.setPolicyNumber(generateApplicationNumber());

        if (dto.getGlDetail() != null) {
            GeneralLiabilityDetail glDetail = new GeneralLiabilityDetail();
            mapGLDetailDTO(dto.getGlDetail(), glDetail);
            
            // Simplified Logic Check: Per Occurrence and Aggregate are now tied strictly 
            // to the Step 1 Coverage Amount for General Liability (Real-world 1:2 ratio).
            BigDecimal coverage = dto.getSelectedCoverageAmount();
            glDetail.setPerOccurrenceLimit(coverage);
            
            application.setGlDetail(glDetail);
            glDetail.setPolicyApplication(application);
            
            // Set application-level tracking to match coverage exactly (1:1)
            application.setOriginalAggregateLimit(coverage);
            application.setCurrentAggregateLimit(coverage);
        }

        // Auto-assign staff
        if (businessProfile.getAgent() != null) {
            Agent agent = businessProfile.getAgent();
            application.setAgent(agent);
            if (agent.getCommissionRate() != null) {
                BigDecimal commission = calculatedPremium
                        .multiply(agent.getCommissionRate())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                application.setCommissionAmount(commission);
            }
        }
        if (businessProfile.getClaimOfficer() != null) {
            application.setClaimOfficer(businessProfile.getClaimOfficer());
        }

        PolicyApplication savedApp = policyApplicationRepository.save(application);
        return toDTO(savedApp);
    }

    private void mapGLDetailDTO(GeneralLiabilityDetailDTO dto, GeneralLiabilityDetail entity) {
        entity.setCustomersVisitPremises(dto.getCustomersVisitPremises());
        entity.setDailyVisitorsRange(dto.getDailyVisitorsRange());
        entity.setHasParkingLot(dto.getHasParkingLot());
        entity.setSellPhysicalProducts(dto.getSellPhysicalProducts());
        entity.setProductsContactFood(dto.getProductsContactFood());
        entity.setProductsForChildren(dto.getProductsForChildren());
        entity.setWorkAtCustomerSites(dto.getWorkAtCustomerSites());
        entity.setWorkRemainsAfterCompletion(dto.getWorkRemainsAfterCompletion());
        entity.setUseSubcontractors(dto.getUseSubcontractors());
        entity.setHasPriorClaims(dto.getHasPriorClaims());
        entity.setClaimsCount(dto.getClaimsCount());
        entity.setTotalClaimsAmount(dto.getTotalClaimsAmount());
        entity.setCoverageType(dto.getCoverageType());
        entity.setPerOccurrenceLimit(dto.getPerOccurrenceLimit());
        entity.setIncludeProductsCompletedOps(dto.getIncludeProductsCompletedOps());
        entity.setIncludeAdvertisingInjury(dto.getIncludeAdvertisingInjury());
    }

    private BigDecimal calculateGLPremium(Policy policy, BigDecimal selectedCoverage, BusinessProfile profile, 
                                        PolicyApplication.PaymentPlan plan, GeneralLiabilityDetailDTO gl) {
        
        BigDecimal basePremium = calculatePremium(policy, selectedCoverage, profile, PolicyApplication.PaymentPlan.MONTHLY);
        BigDecimal multiplier = BigDecimal.ONE;

        if (gl != null) {
            // Section A: Premises
            if (Boolean.TRUE.equals(gl.getCustomersVisitPremises())) {
                multiplier = multiplier.add(new BigDecimal("0.1"));
                if ("Between 50 and 200".equals(gl.getDailyVisitorsRange())) multiplier = multiplier.add(new BigDecimal("0.05"));
                if ("More than 200".equals(gl.getDailyVisitorsRange())) multiplier = multiplier.add(new BigDecimal("0.15"));
                if (Boolean.TRUE.equals(gl.getHasParkingLot())) multiplier = multiplier.add(new BigDecimal("0.05"));
            }

            // Section B: Products
            if (Boolean.TRUE.equals(gl.getSellPhysicalProducts())) {
                multiplier = multiplier.add(new BigDecimal("0.1"));
                if (Boolean.TRUE.equals(gl.getProductsContactFood())) multiplier = multiplier.add(new BigDecimal("0.05"));
                if (Boolean.TRUE.equals(gl.getProductsForChildren())) multiplier = multiplier.add(new BigDecimal("0.1"));
            }

            // Section C: Work at Sites
            if (Boolean.TRUE.equals(gl.getWorkAtCustomerSites())) {
                multiplier = multiplier.add(new BigDecimal("0.15"));
                if (Boolean.TRUE.equals(gl.getWorkRemainsAfterCompletion())) multiplier = multiplier.add(new BigDecimal("0.1"));
                if (Boolean.TRUE.equals(gl.getUseSubcontractors())) multiplier = multiplier.add(new BigDecimal("0.1"));
            }

            // Section D: Claims History
            if (Boolean.TRUE.equals(gl.getHasPriorClaims())) {
                multiplier = multiplier.add(new BigDecimal("0.2"));
                if (gl.getClaimsCount() != null && gl.getClaimsCount() > 2) multiplier = multiplier.add(new BigDecimal("0.15"));
            }

            // Section E: Additional Coverages
            if (Boolean.TRUE.equals(gl.getIncludeProductsCompletedOps())) multiplier = multiplier.add(new BigDecimal("0.1"));
            if (Boolean.TRUE.equals(gl.getIncludeAdvertisingInjury())) multiplier = multiplier.add(new BigDecimal("0.05"));

            // Risk-based adjustment for high Per-Occurrence coverage
            // If user selects exactly the Max Aggregate as their per-occurrence limit, we add a surcharge for high severity risk.
            if (gl.getPerOccurrenceLimit() != null && 
                gl.getPerOccurrenceLimit().compareTo(selectedCoverage) == 0) {
                multiplier = multiplier.add(new BigDecimal("0.15")); // 15% severity surcharge
            }
        }

        BigDecimal adjustedMonthly = basePremium.multiply(multiplier);
        
        // Final period adjustment
        BigDecimal total = adjustedMonthly;
        if (plan == PolicyApplication.PaymentPlan.ANNUALLY) {
            total = adjustedMonthly.multiply(new BigDecimal("12")).multiply(new BigDecimal("0.9"));
        } else if (plan == PolicyApplication.PaymentPlan.SIX_MONTHS) {
            total = adjustedMonthly.multiply(new BigDecimal("6")).multiply(new BigDecimal("0.95"));
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    public List<PolicyApplicationDTO> getAllApplications() {
        return policyApplicationRepository.findAll().stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public BigDecimal calculatePremiumPreview(Long planId, BigDecimal coverageAmount, Long businessProfileId, 
                                            PolicyApplication.PaymentPlan paymentPlan, GeneralLiabilityDetailDTO glDetail) {
        Policy policy = policyRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));
        BusinessProfile businessProfile = businessProfileRepository.findById(businessProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found"));
        
        if (coverageAmount == null) {
             coverageAmount = policy.getMinCoverageAmount();
        }

        if (coverageAmount.compareTo(policy.getMinCoverageAmount()) < 0 ||
            coverageAmount.compareTo(policy.getMaxCoverageAmount()) > 0) {
            throw new InvalidOperationException("Coverage amount must be between " + 
                policy.getMinCoverageAmount() + " and " + policy.getMaxCoverageAmount());
        }

        PolicyApplication.PaymentPlan plan = paymentPlan != null ? paymentPlan : PolicyApplication.PaymentPlan.MONTHLY;
        
        if (glDetail != null) {
            return calculateGLPremium(policy, coverageAmount, businessProfile, plan, glDetail);
        }
        
        return calculatePremium(policy, coverageAmount, businessProfile, plan);
    }

    // Step 2: Admin assigns agent and claim officer to application
    public PolicyApplicationDTO assignStaff(Long applicationId, Long agentId, Long claimOfficerId) {
        PolicyApplication application = policyApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (application.getStatus() != PolicyApplication.ApplicationStatus.SUBMITTED) {
            throw new InvalidOperationException("Can only assign staff to SUBMITTED applications");
        }

        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));
                
        ClaimOfficer claimOfficer = claimOfficerRepository.findById(claimOfficerId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim officer not found"));

        BigDecimal commission = application.getPremiumAmount()
                .multiply(agent.getCommissionRate())
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);

        application.setAgent(agent);
        application.setClaimOfficer(claimOfficer);
        application.setCommissionAmount(commission);

        return toDTO(policyApplicationRepository.save(application));
    }



    // Step 4: Agent approves application
    public PolicyApplicationDTO approveApplication(Long applicationId) {
        PolicyApplication application = policyApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (application.getStatus() != PolicyApplication.ApplicationStatus.SUBMITTED && 
            application.getStatus() != PolicyApplication.ApplicationStatus.UNDER_REVIEW) {
            throw new InvalidOperationException("Only SUBMITTED or UNDER_REVIEW applications can be approved");
        }

        application.setStatus(PolicyApplication.ApplicationStatus.APPROVED);
        
        // Ensure aggregate limits match coverage amount (1:1 ratio)
        application.setOriginalAggregateLimit(application.getSelectedCoverageAmount());
        application.setCurrentAggregateLimit(application.getSelectedCoverageAmount());
        
        if (application.getStartDate() == null) {
            application.setStartDate(LocalDate.now());
        }
        if (application.getEndDate() == null) {
            application.setEndDate(application.getStartDate().plusYears(1));
        }

        PolicyApplicationDTO result = toDTO(policyApplicationRepository.save(application));

        // Notify the policyholder
        if (application.getUser() != null) {
            notificationService.notify(
                application.getUser(),
                "Great news! Your policy application (" + application.getPolicyNumber() + ") has been approved.",
                "APPLICATION_APPROVED"
            );
        }

        return result;
    }

    public PolicyApplicationDTO rejectApplication(Long applicationId, String reason) {
        PolicyApplication application = policyApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        application.setStatus(PolicyApplication.ApplicationStatus.REJECTED);
        application.setRejectionReason(reason);
        PolicyApplicationDTO result = toDTO(policyApplicationRepository.save(application));

        // Notify the policyholder
        if (application.getUser() != null) {
            notificationService.notify(
                application.getUser(),
                "Your policy application (" + application.getPolicyNumber() + ") has been rejected. Reason: " + reason,
                "APPLICATION_REJECTED"
            );
        }

        return result;
    }

    public PolicyApplicationDTO getApplicationById(Long id) {
        return policyApplicationRepository.findById(id).map(this::toDTO).orElse(null);
    }

    public List<PolicyApplicationDTO> getApplicationsByUserId(Long userId) {
        return policyApplicationRepository.findByUserId(userId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public List<PolicyApplicationDTO> getApplicationsByAgentId(Long userId) {
        Agent agent = agentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found for user ID: " + userId));
        
        return policyApplicationRepository.findByAgentId(agent.getId()).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public PolicyApplicationDTO updateApplication(Long id, PolicyApplicationDTO dto) {
        if (policyApplicationRepository.existsById(id)) {
            PolicyApplication application = policyApplicationRepository.findById(id).orElseThrow();
            if (dto.getSelectedCoverageAmount() != null) {
                application.setSelectedCoverageAmount(dto.getSelectedCoverageAmount());
            }
            if (dto.getPremiumAmount() != null) {
                application.setPremiumAmount(dto.getPremiumAmount());
            }
            if (dto.getStartDate() != null) {
                application.setStartDate(dto.getStartDate());
            }
            if (dto.getEndDate() != null) {
                application.setEndDate(dto.getEndDate());
            }
            if (dto.getStatus() != null) {
                application.setStatus(dto.getStatus());
            }
            if (dto.getRejectionReason() != null) {
                application.setRejectionReason(dto.getRejectionReason());
            }
            if (dto.getCommissionAmount() != null) {
                application.setCommissionAmount(dto.getCommissionAmount());
            }
            return toDTO(policyApplicationRepository.save(application));
        }
        return null;
    }

    private String generateApplicationNumber() {
        // Use more unique strategy: APP + yyyyMMdd + 4 random digits
        String datePart = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomPart = (int) (Math.random() * 9000) + 1000;
        return String.format("APP-%s-%04d", datePart, randomPart);
    }

    public BigDecimal calculatePremium(Policy policy, BigDecimal selectedCoverage, BusinessProfile businessProfile, PolicyApplication.PaymentPlan plan) {
        //if policy does not have max coverage then it return base premium
        if (policy.getMaxCoverageAmount() == null || policy.getMaxCoverageAmount().compareTo(BigDecimal.ZERO) == 0) {
            return policy.getBasePremium();
        }
        
        BigDecimal ratio = selectedCoverage.divide(policy.getMaxCoverageAmount(), 4, java.math.RoundingMode.HALF_UP);
        BigDecimal monthlyBase = policy.getBasePremium().multiply(ratio);
        BigDecimal riskMultiplier = BigDecimal.ONE;

        if (businessProfile.getEmployeeCount() != null) {
            if (businessProfile.getEmployeeCount() > 100) {
                riskMultiplier = riskMultiplier.add(new BigDecimal("0.3"));
            } else if (businessProfile.getEmployeeCount() > 50) {
                riskMultiplier = riskMultiplier.add(new BigDecimal("0.2"));
            } else if (businessProfile.getEmployeeCount() > 20) {
                riskMultiplier = riskMultiplier.add(new BigDecimal("0.1"));
            }
        }

        if (businessProfile.getAnnualRevenue() != null) {
            BigDecimal revenue = businessProfile.getAnnualRevenue();
            if (revenue.compareTo(new BigDecimal("10000000")) > 0) {
                riskMultiplier = riskMultiplier.add(new BigDecimal("0.25"));
            } else if (revenue.compareTo(new BigDecimal("5000000")) > 0) {
                riskMultiplier = riskMultiplier.add(new BigDecimal("0.15"));
            } else if (revenue.compareTo(new BigDecimal("1000000")) > 0) {
                riskMultiplier = riskMultiplier.add(new BigDecimal("0.05"));
            }
        }

        if (businessProfile.getIndustry() != null) {
            String industry = businessProfile.getIndustry().toLowerCase();
            if (industry.contains("construction") || industry.contains("manufacturing")) {
                riskMultiplier = riskMultiplier.add(new BigDecimal("0.4"));
            } else if (industry.contains("retail") || industry.contains("restaurant")) {
                riskMultiplier = riskMultiplier.add(new BigDecimal("0.2"));
            } else if (industry.contains("technology") || industry.contains("consulting")) {
                riskMultiplier = riskMultiplier.add(new BigDecimal("0.1"));
            }
        }

        BigDecimal monthlyPremium = monthlyBase.multiply(riskMultiplier).divide(new BigDecimal("10"), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalPremium = monthlyPremium;

        // Adjust based on Payment Plan
        if (plan != null) {
            switch (plan) {
                case ANNUALLY:
                    totalPremium = monthlyPremium.multiply(new BigDecimal("12")).multiply(new BigDecimal("0.90")); // 10% discount for annual
                    break;
                case SIX_MONTHS:
                    totalPremium = monthlyPremium.multiply(new BigDecimal("6")).multiply(new BigDecimal("0.95")); // 5% discount for 6 months
                    break;
                case MONTHLY:
                default:
                    totalPremium = monthlyPremium;
                    break;
            }
        }

        return totalPremium.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private PolicyApplicationDTO toDTO(PolicyApplication app) {
        PolicyApplicationDTO dto = new PolicyApplicationDTO();
        dto.setId(app.getId());
        dto.setPolicyNumber(app.getPolicyNumber());
        dto.setUserId(app.getUser().getId());
        dto.setPlanId(app.getPlan().getId());
        if (app.getBusinessProfile() != null) dto.setBusinessProfileId(app.getBusinessProfile().getId());
        if (app.getAgent() != null) dto.setAgentId(app.getAgent().getId());
        if (app.getClaimOfficer() != null) dto.setClaimOfficerId(app.getClaimOfficer().getId());
        dto.setSelectedCoverageAmount(app.getSelectedCoverageAmount());
        dto.setPremiumAmount(app.getPremiumAmount());
        dto.setStartDate(app.getStartDate());
        dto.setEndDate(app.getEndDate());
        dto.setStatus(app.getStatus());
        dto.setPaymentPlan(app.getPaymentPlan());
        dto.setNextPaymentDueDate(app.getNextPaymentDueDate());
        dto.setRejectionReason(app.getRejectionReason());
        dto.setCommissionAmount(app.getCommissionAmount());
        if (app.getPlan() != null) {
            dto.setPlanName(app.getPlan().getPolicyName());
        }

        if (app.getBusinessProfile() != null) {
            dto.setBusinessName(app.getBusinessProfile().getBusinessName());
            dto.setEmployeeCount(app.getBusinessProfile().getEmployeeCount());
            dto.setAnnualRevenue(app.getBusinessProfile().getAnnualRevenue());
            dto.setIndustry(app.getBusinessProfile().getIndustry());
        }

        // Calculate total settled (paid-out) claim amounts for this application
        BigDecimal settledTotal = claimRepository.findByPolicyApplicationId(app.getId()).stream()
                .filter(c -> c.getStatus() == com.example.comproject.entity.Claim.ClaimStatus.SETTLED || 
                            c.getStatus() == com.example.comproject.entity.Claim.ClaimStatus.APPROVED ||
                            c.getStatus() == com.example.comproject.entity.Claim.ClaimStatus.PARTIALLY_APPROVED)
                .map(Claim::getClaimAmount)
                .filter(amt -> amt != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalSettledAmount(settledTotal);
        
        if (app.getGlDetail() != null) {
            GeneralLiabilityDetailDTO glDto = new GeneralLiabilityDetailDTO();
            mapGLDetailEntityToDTO(app.getGlDetail(), glDto);
            dto.setGlDetail(glDto);
        }

        // New fields mapping
        dto.setOriginalAggregateLimit(app.getOriginalAggregateLimit());
        dto.setCurrentAggregateLimit(app.getCurrentAggregateLimit());
        dto.setTopUpCount(app.getTopUpCount());
        dto.setTotalTopUpAmount(app.getTotalTopUpAmount());
        dto.setRenewedFromPolicyId(app.getRenewedFromPolicyId());
        dto.setRenewedToPolicyId(app.getRenewedToPolicyId());
        dto.setRenewalInitiated(app.getRenewalInitiated());
        dto.setRenewalReminderSentAt(app.getRenewalReminderSentAt());

        // Populate claims list
        java.util.List<Claim> claimsList = claimRepository.findByPolicyApplicationId(app.getId());
        if (claimsList != null) {
            dto.setClaims(claimsList.stream().map(c -> {
                ClaimDTO cdto = new ClaimDTO();
                cdto.setId(c.getId());
                cdto.setClaimNumber(c.getClaimNumber());
                cdto.setClaimAmount(c.getClaimAmount());
                cdto.setStatus(c.getStatus());
                cdto.setIncidentDate(c.getIncidentDate());
                cdto.setIncidentLocation(c.getIncidentLocation());
                cdto.setDescription(c.getDescription());
                cdto.setRejectionReason(c.getRejectionReason());
                return cdto;
            }).collect(Collectors.toList()));
        }

        // Populate top-ups list
        java.util.List<PolicyTopUp> topUpsList = topUpRepository.findByPolicyApplicationId(app.getId());
        if (topUpsList != null) {
            dto.setTopUps(topUpsList.stream()
                    .map(PolicyTopUpDTO::fromEntity)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public PolicyApplicationDTO createRenewalApplication(Long oldPolicyId) {
        PolicyApplication oldApp = policyApplicationRepository.findById(oldPolicyId)
                .orElseThrow(() -> new ResourceNotFoundException("Old policy not found"));

        if (Boolean.TRUE.equals(oldApp.getRenewalInitiated())) {
            throw new InvalidOperationException("Renewal already initiated for this policy");
        }

        PolicyApplication renewalApp = new PolicyApplication();
        renewalApp.setUser(oldApp.getUser());
        renewalApp.setPlan(oldApp.getPlan());
        renewalApp.setBusinessProfile(oldApp.getBusinessProfile());
        renewalApp.setSelectedCoverageAmount(oldApp.getOriginalAggregateLimit());
        renewalApp.setPremiumAmount(oldApp.getPremiumAmount()); // Should re-calculate if necessary, but using same for now
        renewalApp.setStatus(PolicyApplication.ApplicationStatus.SUBMITTED);
        renewalApp.setPolicyNumber(generateApplicationNumber() + "-R");
        renewalApp.setAgent(oldApp.getAgent());
        renewalApp.setClaimOfficer(oldApp.getClaimOfficer());
        renewalApp.setPaymentPlan(oldApp.getPaymentPlan());
        renewalApp.setRenewedFromPolicyId(oldApp.getId());
        
        // Start date is one day after old policy ends
        renewalApp.setStartDate(oldApp.getEndDate().plusDays(1));
        renewalApp.setEndDate(renewalApp.getStartDate().plusYears(1));

        oldApp.setRenewalInitiated(true);
        policyApplicationRepository.save(oldApp);

        PolicyApplication saved = policyApplicationRepository.save(renewalApp);
        oldApp.setRenewedToPolicyId(saved.getId());
        policyApplicationRepository.save(oldApp);

        return toDTO(saved);
    }

    private void mapGLDetailEntityToDTO(GeneralLiabilityDetail entity, GeneralLiabilityDetailDTO dto) {
        dto.setCustomersVisitPremises(entity.getCustomersVisitPremises());
        dto.setDailyVisitorsRange(entity.getDailyVisitorsRange());
        dto.setHasParkingLot(entity.getHasParkingLot());
        dto.setSellPhysicalProducts(entity.getSellPhysicalProducts());
        dto.setProductsContactFood(entity.getProductsContactFood());
        dto.setProductsForChildren(entity.getProductsForChildren());
        dto.setWorkAtCustomerSites(entity.getWorkAtCustomerSites());
        dto.setWorkRemainsAfterCompletion(entity.getWorkRemainsAfterCompletion());
        dto.setUseSubcontractors(entity.getUseSubcontractors());
        dto.setHasPriorClaims(entity.getHasPriorClaims());
        dto.setClaimsCount(entity.getClaimsCount());
        dto.setTotalClaimsAmount(entity.getTotalClaimsAmount());
        dto.setCoverageType(entity.getCoverageType());
        dto.setPerOccurrenceLimit(entity.getPerOccurrenceLimit());
        dto.setIncludeProductsCompletedOps(entity.getIncludeProductsCompletedOps());
        dto.setIncludeAdvertisingInjury(entity.getIncludeAdvertisingInjury());
    }
}
