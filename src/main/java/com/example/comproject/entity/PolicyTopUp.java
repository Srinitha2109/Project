package com.example.comproject.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "policy_top_ups")
public class PolicyTopUp {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "policy_application_id")
    private PolicyApplication policyApplication;

    @ManyToOne
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    private BigDecimal topUpAmount;
    private BigDecimal additionalPremium;

    @Enumerated(EnumType.STRING)
    @Column(name = "top_up_status", columnDefinition = "VARCHAR(255)")
    private TopUpStatus status;

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;

    public enum TopUpStatus {
        REQUESTED, PENDING_PAYMENT, UNDER_REVIEW, AGENT_APPROVED, APPROVED, REJECTED
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public PolicyApplication getPolicyApplication() { return policyApplication; }
    public void setPolicyApplication(PolicyApplication policyApplication) { this.policyApplication = policyApplication; }
    public User getRequestedBy() { return requestedBy; }
    public void setRequestedBy(User requestedBy) { this.requestedBy = requestedBy; }
    public BigDecimal getTopUpAmount() { return topUpAmount; }
    public void setTopUpAmount(BigDecimal topUpAmount) { this.topUpAmount = topUpAmount; }
    public BigDecimal getAdditionalPremium() { return additionalPremium; }
    public void setAdditionalPremium(BigDecimal additionalPremium) { this.additionalPremium = additionalPremium; }
    public TopUpStatus getStatus() { return status; }
    public void setStatus(TopUpStatus status) { this.status = status; }
    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
}
