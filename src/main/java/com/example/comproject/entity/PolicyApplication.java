package com.example.comproject.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "policy_applications")
public class PolicyApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "policyApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments;

    @OneToMany(mappedBy = "policyApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Claim> claims;

    @OneToMany(mappedBy = "policyApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PolicyTopUp> topUps;

    @Column(unique = true)
    private String policyNumber;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "plan_id")
    private Policy plan;

    @ManyToOne
    @JoinColumn(name = "business_profile_id")
    private BusinessProfile businessProfile;

    @ManyToOne
    @JoinColumn(name = "agent_id")
    private Agent agent;


    @ManyToOne
    @JoinColumn(name = "claim_officer_id")
    private ClaimOfficer claimOfficer;


    private BigDecimal selectedCoverageAmount;
    private BigDecimal premiumAmount;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @Enumerated(EnumType.STRING)
    private PaymentPlan paymentPlan;

    private LocalDate nextPaymentDueDate;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    private BigDecimal commissionAmount;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "gl_detail_id", referencedColumnName = "id")
    private GeneralLiabilityDetail glDetail;

    // Top-up tracking
    private BigDecimal originalAggregateLimit;
    private BigDecimal currentAggregateLimit;
    private Integer topUpCount = 0;
    private BigDecimal totalTopUpAmount = BigDecimal.ZERO;

    // Renewal tracking
    private Long renewedFromPolicyId;
    private Long renewedToPolicyId;
    private Boolean renewalInitiated = false;
    private LocalDate renewalReminderSentAt;

    public enum ApplicationStatus {
        SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED, ACTIVE, EXPIRED, CANCELLED
    }

    public enum PaymentPlan {
        MONTHLY, SIX_MONTHS, ANNUALLY
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Policy getPlan() { return plan; }
    public void setPlan(Policy plan) { this.plan = plan; }
    public BusinessProfile getBusinessProfile() { return businessProfile; }
    public void setBusinessProfile(BusinessProfile businessProfile) { this.businessProfile = businessProfile; }
    public Agent getAgent() { return agent; }
    public void setAgent(Agent agent) { this.agent = agent; }
    public BigDecimal getSelectedCoverageAmount() { return selectedCoverageAmount; }
    public void setSelectedCoverageAmount(BigDecimal selectedCoverageAmount) { this.selectedCoverageAmount = selectedCoverageAmount; }
    public BigDecimal getPremiumAmount() { return premiumAmount; }
    public void setPremiumAmount(BigDecimal premiumAmount) { this.premiumAmount = premiumAmount; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; }
    public GeneralLiabilityDetail getGlDetail() { return glDetail; }
    public void setGlDetail(GeneralLiabilityDetail glDetail) { this.glDetail = glDetail; }
   
    public ClaimOfficer getClaimOfficer() { return claimOfficer; }
    public void setClaimOfficer(ClaimOfficer claimOfficer) { this.claimOfficer = claimOfficer; }
    public PaymentPlan getPaymentPlan() { return paymentPlan; }
    public void setPaymentPlan(PaymentPlan paymentPlan) { this.paymentPlan = paymentPlan; }
    public LocalDate getNextPaymentDueDate() { return nextPaymentDueDate; }
    public void setNextPaymentDueDate(LocalDate nextPaymentDueDate) { this.nextPaymentDueDate = nextPaymentDueDate; }

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

    public List<Claim> getClaims() { return claims; }
    public void setClaims(List<Claim> claims) { this.claims = claims; }
}
