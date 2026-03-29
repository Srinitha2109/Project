package com.example.comproject.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "ocr_results")
public class OCRResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "claim_document_id")
    private ClaimDocument claimDocument;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    // Common fields
    private LocalDate extractedDate;
    private BigDecimal extractedAmount;
    private String extractedPersonName;
    private String extractedLocation;
    private String extractedRefNumber;
    private String extractedOrgName;

    // GL Bodily Injury specific
    private String extractedInjuryType;
    private String extractedDoctorName;
    private String extractedDoctorLicense;
    private String extractedRecoveryPeriod;

    // GL Property Damage specific
    @Column(columnDefinition = "TEXT")
    private String extractedDamageDesc;
    private BigDecimal extractedRepairCost;
    private String extractedAssessorName;

    // Auto Insurance specific
    private String extractedVehicleNo;
    private String extractedLicenseNo;

    // Workers Comp specific
    private String extractedEmployeeName;
    private BigDecimal extractedMonthlySalary;
    private String extractedDesignation;

    // OCR metadata
    @Column(columnDefinition = "TEXT")
    private String rawExtractedText;
    private BigDecimal confidenceScore;
    private String ocrEngine; // e.g., GOOGLE, TESSERACT
    
    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus;

    public enum DocumentType {
        INCIDENT_REPORT, MEDICAL_REPORT, HOSPITAL_BILL, POLICE_REPORT,
        WITNESS_STATEMENT, DAMAGE_ASSESSMENT, REPAIR_INVOICE, LAB_REPORT,
        DRIVING_LICENSE, RC_BOOK, SALARY_SLIP, EMPLOYMENT_LETTER
    }

    public enum ProcessingStatus {
        PENDING, COMPLETED, FAILED
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public ClaimDocument getClaimDocument() { return claimDocument; }
    public void setClaimDocument(ClaimDocument claimDocument) { this.claimDocument = claimDocument; }

    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }

    public LocalDate getExtractedDate() { return extractedDate; }
    public void setExtractedDate(LocalDate extractedDate) { this.extractedDate = extractedDate; }

    public BigDecimal getExtractedAmount() { return extractedAmount; }
    public void setExtractedAmount(BigDecimal extractedAmount) { this.extractedAmount = extractedAmount; }

    public String getExtractedPersonName() { return extractedPersonName; }
    public void setExtractedPersonName(String extractedPersonName) { this.extractedPersonName = extractedPersonName; }

    public String getExtractedLocation() { return extractedLocation; }
    public void setExtractedLocation(String extractedLocation) { this.extractedLocation = extractedLocation; }

    public String getExtractedRefNumber() { return extractedRefNumber; }
    public void setExtractedRefNumber(String extractedRefNumber) { this.extractedRefNumber = extractedRefNumber; }

    public String getExtractedOrgName() { return extractedOrgName; }
    public void setExtractedOrgName(String extractedOrgName) { this.extractedOrgName = extractedOrgName; }

    public String getExtractedInjuryType() { return extractedInjuryType; }
    public void setExtractedInjuryType(String extractedInjuryType) { this.extractedInjuryType = extractedInjuryType; }

    public String getExtractedDoctorName() { return extractedDoctorName; }
    public void setExtractedDoctorName(String extractedDoctorName) { this.extractedDoctorName = extractedDoctorName; }

    public String getExtractedDoctorLicense() { return extractedDoctorLicense; }
    public void setExtractedDoctorLicense(String extractedDoctorLicense) { this.extractedDoctorLicense = extractedDoctorLicense; }

    public String getExtractedRecoveryPeriod() { return extractedRecoveryPeriod; }
    public void setExtractedRecoveryPeriod(String extractedRecoveryPeriod) { this.extractedRecoveryPeriod = extractedRecoveryPeriod; }

    public String getExtractedDamageDesc() { return extractedDamageDesc; }
    public void setExtractedDamageDesc(String extractedDamageDesc) { this.extractedDamageDesc = extractedDamageDesc; }

    public BigDecimal getExtractedRepairCost() { return extractedRepairCost; }
    public void setExtractedRepairCost(BigDecimal extractedRepairCost) { this.extractedRepairCost = extractedRepairCost; }

    public String getExtractedAssessorName() { return extractedAssessorName; }
    public void setExtractedAssessorName(String extractedAssessorName) { this.extractedAssessorName = extractedAssessorName; }

    public String getExtractedVehicleNo() { return extractedVehicleNo; }
    public void setExtractedVehicleNo(String extractedVehicleNo) { this.extractedVehicleNo = extractedVehicleNo; }

    public String getExtractedLicenseNo() { return extractedLicenseNo; }
    public void setExtractedLicenseNo(String extractedLicenseNo) { this.extractedLicenseNo = extractedLicenseNo; }

    public String getExtractedEmployeeName() { return extractedEmployeeName; }
    public void setExtractedEmployeeName(String extractedEmployeeName) { this.extractedEmployeeName = extractedEmployeeName; }

    public BigDecimal getExtractedMonthlySalary() { return extractedMonthlySalary; }
    public void setExtractedMonthlySalary(BigDecimal extractedMonthlySalary) { this.extractedMonthlySalary = extractedMonthlySalary; }

    public String getExtractedDesignation() { return extractedDesignation; }
    public void setExtractedDesignation(String extractedDesignation) { this.extractedDesignation = extractedDesignation; }

    public String getRawExtractedText() { return rawExtractedText; }
    public void setRawExtractedText(String rawExtractedText) { this.rawExtractedText = rawExtractedText; }

    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getOcrEngine() { return ocrEngine; }
    public void setOcrEngine(String ocrEngine) { this.ocrEngine = ocrEngine; }

    public ProcessingStatus getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(ProcessingStatus processingStatus) { this.processingStatus = processingStatus; }
}
