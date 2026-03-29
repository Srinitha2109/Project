package com.example.comproject.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.comproject.entity.PolicyApplication;

public class PolicyApplicationDTO {
    private Long id;
    private String policyNumber;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Plan ID is required")
    private Long planId;

    @NotNull(message = "Business Profile ID is required")
    private Long businessProfileId;

    private Long agentId;
    private Long claimOfficerId;

    @NotNull(message = "Coverage amount is required")
    private BigDecimal selectedCoverageAmount;
    private BigDecimal premiumAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private PolicyApplication.ApplicationStatus status;
    private PolicyApplication.PaymentPlan paymentPlan;
    private LocalDate nextPaymentDueDate;
    private String rejectionReason;
    private BigDecimal commissionAmount;
    private BigDecimal totalSettledAmount;
    private GeneralLiabilityDetailDTO glDetail;

    // Top-up tracking
    private BigDecimal originalAggregateLimit;
    private BigDecimal currentAggregateLimit;
    private Integer topUpCount;
    private BigDecimal totalTopUpAmount;

    // Renewal tracking
    private Long renewedFromPolicyId;
    private Long renewedToPolicyId;
    private Boolean renewalInitiated;
    private LocalDate renewalReminderSentAt;

    // Added for Agent Review
    private String businessName;
    private Integer employeeCount;
    private BigDecimal annualRevenue;
    private String industry;
    private String planName;
    private java.util.List<ClaimDTO> claims;
    private java.util.List<PolicyTopUpDTO> topUps;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Long getBusinessProfileId() { return businessProfileId; }
    public void setBusinessProfileId(Long businessProfileId) { this.businessProfileId = businessProfileId; }
    public Long getAgentId() { return agentId; }
    public void setAgentId(Long agentId) { this.agentId = agentId; }
    public Long getClaimOfficerId() { return claimOfficerId; }
    public void setClaimOfficerId(Long claimOfficerId) { this.claimOfficerId = claimOfficerId; }
    public BigDecimal getSelectedCoverageAmount() { return selectedCoverageAmount; }
    public void setSelectedCoverageAmount(BigDecimal selectedCoverageAmount) { this.selectedCoverageAmount = selectedCoverageAmount; }
    public BigDecimal getPremiumAmount() { return premiumAmount; }
    public void setPremiumAmount(BigDecimal premiumAmount) { this.premiumAmount = premiumAmount; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public PolicyApplication.ApplicationStatus getStatus() { return status; }
    public void setStatus(PolicyApplication.ApplicationStatus status) { this.status = status; }
    public PolicyApplication.PaymentPlan getPaymentPlan() { return paymentPlan; }
    public void setPaymentPlan(PolicyApplication.PaymentPlan paymentPlan) { this.paymentPlan = paymentPlan; }
    public LocalDate getNextPaymentDueDate() { return nextPaymentDueDate; }
    public void setNextPaymentDueDate(LocalDate nextPaymentDueDate) { this.nextPaymentDueDate = nextPaymentDueDate; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public Integer getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(Integer employeeCount) { this.employeeCount = employeeCount; }
    public BigDecimal getAnnualRevenue() { return annualRevenue; }
    public void setAnnualRevenue(BigDecimal annualRevenue) { this.annualRevenue = annualRevenue; }
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public BigDecimal getTotalSettledAmount() { return totalSettledAmount; }
    public void setTotalSettledAmount(BigDecimal totalSettledAmount) { this.totalSettledAmount = totalSettledAmount; }
    public GeneralLiabilityDetailDTO getGlDetail() { return glDetail; }
    public void setGlDetail(GeneralLiabilityDetailDTO glDetail) { this.glDetail = glDetail; }

    public BigDecimal getOriginalAggregateLimit() { return originalAggregateLimit; }
    public void setOriginalAggregateLimit(BigDecimal originalAggregateLimit) { this.originalAggregateLimit = originalAggregateLimit; }
    public BigDecimal getCurrentAggregateLimit() { return currentAggregateLimit; }
    public void setCurrentAggregateLimit(BigDecimal currentAggregateLimit) { this.currentAggregateLimit = currentAggregateLimit; }
    public Integer getTopUpCount() { return topUpCount; }
    public void setTopUpCount(Integer topUpCount) { this.topUpCount = topUpCount; }
    public BigDecimal getTotalTopUpAmount() { return totalTopUpAmount; }
    public void setTotalTopUpAmount(BigDecimal totalTopUpAmount) { this.totalTopUpAmount = totalTopUpAmount; }
    public Long getRenewedFromPolicyId() { return renewedFromPolicyId; }
    public void setRenewedFromPolicyId(Long renewedFromPolicyId) { this.renewedFromPolicyId = renewedFromPolicyId; }
    public Long getRenewedToPolicyId() { return renewedToPolicyId; }
    public void setRenewedToPolicyId(Long renewedToPolicyId) { this.renewedToPolicyId = renewedToPolicyId; }
    public Boolean getRenewalInitiated() { return renewalInitiated; }
    public void setRenewalInitiated(Boolean renewalInitiated) { this.renewalInitiated = renewalInitiated; }
    public LocalDate getRenewalReminderSentAt() { return renewalReminderSentAt; }
    public void setRenewalReminderSentAt(LocalDate renewalReminderSentAt) { this.renewalReminderSentAt = renewalReminderSentAt; }

    public java.util.List<ClaimDTO> getClaims() { return claims; }
    public void setClaims(java.util.List<ClaimDTO> claims) { this.claims = claims; }
    public java.util.List<PolicyTopUpDTO> getTopUps() { return topUps; }
    public void setTopUps(java.util.List<PolicyTopUpDTO> topUps) { this.topUps = topUps; }
}
