package com.example.comproject.dto;

import com.example.comproject.entity.PolicyApplication;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class GeneralLiabilityApplicationRequestDTO {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Plan ID is required")
    private Long planId;

    @NotNull(message = "Business Profile ID is required")
    private Long businessProfileId;

    @NotNull(message = "Coverage amount is required")
    private BigDecimal selectedCoverageAmount;

    @NotNull(message = "Payment plan is required")
    private PolicyApplication.PaymentPlan paymentPlan;

    private GeneralLiabilityDetailDTO glDetail;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Long getBusinessProfileId() { return businessProfileId; }
    public void setBusinessProfileId(Long businessProfileId) { this.businessProfileId = businessProfileId; }
    public BigDecimal getSelectedCoverageAmount() { return selectedCoverageAmount; }
    public void setSelectedCoverageAmount(BigDecimal selectedCoverageAmount) { this.selectedCoverageAmount = selectedCoverageAmount; }
    public PolicyApplication.PaymentPlan getPaymentPlan() { return paymentPlan; }
    public void setPaymentPlan(PolicyApplication.PaymentPlan paymentPlan) { this.paymentPlan = paymentPlan; }
    public GeneralLiabilityDetailDTO getGlDetail() { return glDetail; }
    public void setGlDetail(GeneralLiabilityDetailDTO glDetail) { this.glDetail = glDetail; }
}
