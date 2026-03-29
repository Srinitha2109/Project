package com.example.comproject.service;

import com.example.comproject.dto.PolicyTopUpDTO;
import com.example.comproject.entity.PolicyApplication;
import com.example.comproject.entity.PolicyTopUp;
import com.example.comproject.entity.User;
import com.example.comproject.exception.InvalidOperationException;
import com.example.comproject.exception.ResourceNotFoundException;
import com.example.comproject.repository.PolicyApplicationRepository;
import com.example.comproject.repository.PolicyTopUpRepository;
import com.example.comproject.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PolicyTopUpService {
    private final PolicyTopUpRepository topUpRepository;
    private final PolicyApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final PolicyApplicationService policyApplicationService;
    private final AppNotificationService notificationService;

    public PolicyTopUpService(PolicyTopUpRepository topUpRepository,
            PolicyApplicationRepository applicationRepository,
            UserRepository userRepository,
            PolicyApplicationService policyApplicationService,
            AppNotificationService notificationService) {
        this.topUpRepository = topUpRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.policyApplicationService = policyApplicationService;
        this.notificationService = notificationService;
    }

    @Transactional
    public PolicyTopUp requestTopUp(Long policyId, BigDecimal amount, Long userId) {
        PolicyApplication application = applicationRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Integer count = application.getTopUpCount();
        boolean hasExisting = topUpRepository.findByPolicyApplicationId(policyId).stream()
                .anyMatch(t -> t.getStatus() != PolicyTopUp.TopUpStatus.REJECTED);

        if (hasExisting || (count != null && count >= 1)) {
            throw new InvalidOperationException("Only one top-up request is allowed per policy application");
        }

        PolicyTopUp topUp = new PolicyTopUp();
        topUp.setPolicyApplication(application);
        topUp.setRequestedBy(user);
        topUp.setTopUpAmount(amount);
        // Additional premium calculation logic (e.g., 5% of top-up amount)
        topUp.setAdditionalPremium(amount.multiply(new BigDecimal("0.05")));
        topUp.setStatus(PolicyTopUp.TopUpStatus.REQUESTED);
        topUp.setRequestedAt(LocalDateTime.now());

        PolicyTopUp saved = topUpRepository.save(topUp);

        // Notify agent
        if (application.getAgent() != null && application.getAgent().getUser() != null) {
            notificationService.notify(application.getAgent().getUser(),
                    "New top-up request for policy " + application.getPolicyNumber() + " (Amount: Rs. " + amount + ")",
                    "TOP_UP_REQUESTED");
        }

        return saved;
    }

    @Transactional
    public PolicyTopUp approveAfterAgentReview(UUID topUpId, Long approvedByUserId) {
        PolicyTopUp topUp = topUpRepository.findById(topUpId)
                .orElseThrow(() -> new ResourceNotFoundException("Top-up request not found"));

        User approvedBy = userRepository.findById(approvedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Approver user not found"));

        if (topUp.getStatus() != PolicyTopUp.TopUpStatus.REQUESTED
                && topUp.getStatus() != PolicyTopUp.TopUpStatus.UNDER_REVIEW) {
            throw new InvalidOperationException("Request is not in a provable state");
        }

        PolicyApplication application = topUp.getPolicyApplication();

        // Update application limits
        BigDecimal currentLimit = application.getCurrentAggregateLimit() != null
                ? application.getCurrentAggregateLimit()
                : (application.getSelectedCoverageAmount() != null ? application.getSelectedCoverageAmount()
                        : BigDecimal.ZERO);
        BigDecimal newLimit = currentLimit.add(topUp.getTopUpAmount());
        application.setCurrentAggregateLimit(newLimit);

        // Update recurring premium based on new coverage
        BigDecimal updatedMonthlyPremium = policyApplicationService.calculatePremium(
                application.getPlan(),
                newLimit,
                application.getBusinessProfile(),
                application.getPaymentPlan());
        application.setPremiumAmount(updatedMonthlyPremium);

        // Trigger immediate premium payment (due today)
        application.setNextPaymentDueDate(LocalDate.now());

        application.setTopUpCount((application.getTopUpCount() == null ? 0 : application.getTopUpCount()) + 1);
        application.setTotalTopUpAmount(
                (application.getTotalTopUpAmount() == null ? BigDecimal.ZERO : application.getTotalTopUpAmount())
                        .add(topUp.getTopUpAmount()));

        // If policy was expired due to 0 balance, reactivate it if the new limit > 0
        if (application.getStatus() == PolicyApplication.ApplicationStatus.EXPIRED) {
            application.setStatus(PolicyApplication.ApplicationStatus.ACTIVE);
        }

        applicationRepository.save(application);

        topUp.setStatus(PolicyTopUp.TopUpStatus.APPROVED);
        topUp.setApprovedBy(approvedBy);
        topUp.setApprovedAt(LocalDateTime.now());

        PolicyTopUp saved = topUpRepository.save(topUp);

        // Notify policyholder
        notificationService.notify(application.getUser(),
                "Your top-up request for policy " + application.getPolicyNumber()
                        + " has been APPROVED. Coverage and updated premium active now.",
                "TOP_UP_APPROVED");

        return saved;
    }

    @Transactional
    public PolicyTopUp confirmPaymentAndAddLimit(UUID topUpId) {
        // This method is kept for API compatibility but status is managed in review
        PolicyTopUp topUp = topUpRepository.findById(topUpId)
                .orElseThrow(() -> new ResourceNotFoundException("Top-up request not found"));

        if (topUp.getStatus() != PolicyTopUp.TopUpStatus.APPROVED) {
            topUp.setStatus(PolicyTopUp.TopUpStatus.APPROVED);
            topUpRepository.save(topUp);
        }
        return topUp;
    }

    public PolicyTopUp rejectTopUp(UUID topUpId, String reason) {
        PolicyTopUp topUp = topUpRepository.findById(topUpId)
                .orElseThrow(() -> new ResourceNotFoundException("Top-up request not found"));

        topUp.setStatus(PolicyTopUp.TopUpStatus.REJECTED);
        topUp.setRejectionReason(reason);

        PolicyTopUp saved = topUpRepository.save(topUp);

        // Notify policyholder
        notificationService.notify(topUp.getPolicyApplication().getUser(),
                "Your top-up request for policy " + topUp.getPolicyApplication().getPolicyNumber()
                        + " has been rejected. Reason: " + reason,
                "TOP_UP_REJECTED");

        return saved;
    }

    public List<PolicyTopUp> getTopUpsByPolicy(Long policyId) {
        return topUpRepository.findByPolicyApplicationId(policyId);
    }

    public List<PolicyTopUpDTO> getPendingTopUpsByAgent(Long agentUserId) {
        return topUpRepository.findPendingByAgent(agentUserId).stream()
                .map(t -> {
                    PolicyTopUpDTO dto = PolicyTopUpDTO.fromEntity(t);
                    PolicyApplication app = t.getPolicyApplication();
                    if (app != null) {
                        BigDecimal currentLimit = app.getCurrentAggregateLimit() != null
                                ? app.getCurrentAggregateLimit()
                                : (app.getSelectedCoverageAmount() != null ? app.getSelectedCoverageAmount()
                                        : BigDecimal.ZERO);
                        BigDecimal expectedLimit = currentLimit.add(t.getTopUpAmount());
                        BigDecimal expectedPrem = policyApplicationService.calculatePremium(
                                app.getPlan(),
                                expectedLimit,
                                app.getBusinessProfile(),
                                app.getPaymentPlan());
                        dto.setExpectedNewPremium(expectedPrem);
                        dto.setCurrentPremium(app.getPremiumAmount());
                        if (app.getUser() != null) {
                            dto.setRequestedByUserName(app.getUser().getFullName());
                        }
                        if (app.getBusinessProfile() != null) {
                            dto.setBusinessName(app.getBusinessProfile().getBusinessName());
                        }
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
