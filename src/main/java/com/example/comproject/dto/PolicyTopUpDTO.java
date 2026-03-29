package com.example.comproject.dto;

import com.example.comproject.entity.PolicyTopUp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PolicyTopUpDTO {
    private UUID id;
    private Long policyApplicationId;
    private String policyNumber;
    private Long requestedBy;
    private String requestedByUserName;
    private BigDecimal topUpAmount;
    private BigDecimal additionalPremium;
    private PolicyTopUp.TopUpStatus status;
    private Long approvedBy;
    private String rejectionReason;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private BigDecimal currentPremium;
    private BigDecimal expectedNewPremium;
    private String businessName;
    private Long planId;
    private BigDecimal totalCoverage;
    private String policyStatus;
    private String paymentPlan;

    public static PolicyTopUpDTO fromEntity(PolicyTopUp topUp) {
        PolicyTopUpDTO dto = new PolicyTopUpDTO();
        dto.setId(topUp.getId());
        if (topUp.getPolicyApplication() != null) {
            dto.setPolicyApplicationId(topUp.getPolicyApplication().getId());
            dto.setPolicyNumber(topUp.getPolicyApplication().getPolicyNumber());
        }
        if (topUp.getRequestedBy() != null) {
            dto.setRequestedBy(topUp.getRequestedBy().getId());
            dto.setRequestedByUserName(topUp.getRequestedBy().getFullName());
        }
        dto.setTopUpAmount(topUp.getTopUpAmount());
        dto.setAdditionalPremium(topUp.getAdditionalPremium());
        dto.setStatus(topUp.getStatus());
        if (topUp.getApprovedBy() != null) {
            dto.setApprovedBy(topUp.getApprovedBy().getId());
        }
        dto.setRejectionReason(topUp.getRejectionReason());
        dto.setRequestedAt(topUp.getRequestedAt());
        dto.setApprovedAt(topUp.getApprovedAt());
        if (topUp.getPolicyApplication() != null) {
            dto.setCurrentPremium(topUp.getPolicyApplication().getPremiumAmount());
            if (topUp.getPolicyApplication().getBusinessProfile() != null) {
                dto.setBusinessName(topUp.getPolicyApplication().getBusinessProfile().getBusinessName());
            }
            if (topUp.getPolicyApplication().getPlan() != null) {
                dto.setPlanId(topUp.getPolicyApplication().getPlan().getId());
            }
            BigDecimal currentLimit = topUp.getPolicyApplication().getCurrentAggregateLimit() != null
                    ? topUp.getPolicyApplication().getCurrentAggregateLimit()
                    : (topUp.getPolicyApplication().getSelectedCoverageAmount() != null
                            ? topUp.getPolicyApplication().getSelectedCoverageAmount()
                            : BigDecimal.ZERO);
            dto.setTotalCoverage(currentLimit);
            dto.setPolicyStatus(topUp.getPolicyApplication().getStatus().name());
            dto.setPaymentPlan(topUp.getPolicyApplication().getPaymentPlan().name());
        }
        return dto;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getPolicyApplicationId() {
        return policyApplicationId;
    }

    public void setPolicyApplicationId(Long policyApplicationId) {
        this.policyApplicationId = policyApplicationId;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public Long getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(Long requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getRequestedByUserName() {
        return requestedByUserName;
    }

    public void setRequestedByUserName(String requestedByUserName) {
        this.requestedByUserName = requestedByUserName;
    }

    public BigDecimal getTopUpAmount() {
        return topUpAmount;
    }

    public void setTopUpAmount(BigDecimal topUpAmount) {
        this.topUpAmount = topUpAmount;
    }

    public BigDecimal getAdditionalPremium() {
        return additionalPremium;
    }

    public void setAdditionalPremium(BigDecimal additionalPremium) {
        this.additionalPremium = additionalPremium;
    }

    public PolicyTopUp.TopUpStatus getStatus() {
        return status;
    }

    public void setStatus(PolicyTopUp.TopUpStatus status) {
        this.status = status;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Long approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public BigDecimal getCurrentPremium() {
        return currentPremium;
    }

    public void setCurrentPremium(BigDecimal currentPremium) {
        this.currentPremium = currentPremium;
    }

    public BigDecimal getExpectedNewPremium() {
        return expectedNewPremium;
    }

    public void setExpectedNewPremium(BigDecimal expectedNewPremium) {
        this.expectedNewPremium = expectedNewPremium;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public BigDecimal getTotalCoverage() {
        return totalCoverage;
    }

    public void setTotalCoverage(BigDecimal totalCoverage) {
        this.totalCoverage = totalCoverage;
    }

    public String getPolicyStatus() {
        return policyStatus;
    }

    public void setPolicyStatus(String policyStatus) {
        this.policyStatus = policyStatus;
    }

    public String getPaymentPlan() {
        return paymentPlan;
    }

    public void setPaymentPlan(String paymentPlan) {
        this.paymentPlan = paymentPlan;
    }
}
