package com.example.comproject.controller;

import com.example.comproject.dto.PolicyTopUpDTO;
import com.example.comproject.service.PolicyTopUpService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/policy-topups")
public class PolicyTopUpController {
    private final PolicyTopUpService topUpService;

    public PolicyTopUpController(PolicyTopUpService topUpService) {
        this.topUpService = topUpService;
    }

    @PreAuthorize("hasRole('POLICYHOLDER')")
    @PostMapping("/request")
    public ResponseEntity<PolicyTopUpDTO> requestTopUp(@RequestParam Long policyId, @RequestParam BigDecimal amount, @RequestParam Long userId) {
        return ResponseEntity.ok(PolicyTopUpDTO.fromEntity(topUpService.requestTopUp(policyId, amount, userId)));
    }

    @PreAuthorize("hasRole('POLICYHOLDER')")
    @PostMapping("/{topUpId}/confirm-payment")
    public ResponseEntity<PolicyTopUpDTO> confirmPayment(@PathVariable UUID topUpId) {
        return ResponseEntity.ok(PolicyTopUpDTO.fromEntity(topUpService.confirmPaymentAndAddLimit(topUpId)));
    }

    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @PostMapping("/{topUpId}/approve")
    public ResponseEntity<PolicyTopUpDTO> approveTopUp(@PathVariable UUID topUpId, @RequestParam Long adminUserId) {
        return ResponseEntity.ok(PolicyTopUpDTO.fromEntity(topUpService.approveAfterAgentReview(topUpId, adminUserId)));
    }

    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @PostMapping("/{topUpId}/reject")
    public ResponseEntity<PolicyTopUpDTO> rejectTopUp(@PathVariable UUID topUpId, @RequestParam String reason) {
        return ResponseEntity.ok(PolicyTopUpDTO.fromEntity(topUpService.rejectTopUp(topUpId, reason)));
    }

    @PreAuthorize("hasAnyRole('POLICYHOLDER', 'AGENT', 'ADMIN')")
    @GetMapping("/policy/{policyId}")
    public ResponseEntity<List<PolicyTopUpDTO>> getTopUpsByPolicy(@PathVariable Long policyId) {
        List<PolicyTopUpDTO> dtos = topUpService.getTopUpsByPolicy(policyId).stream()
                .map(PolicyTopUpDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @GetMapping("/agent/{agentUserId}/pending")
    public ResponseEntity<List<PolicyTopUpDTO>> getAgentPendingTopUps(@PathVariable Long agentUserId) {
        return ResponseEntity.ok(topUpService.getPendingTopUpsByAgent(agentUserId));
    }
}
