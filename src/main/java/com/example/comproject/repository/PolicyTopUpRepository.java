package com.example.comproject.repository;

import com.example.comproject.entity.PolicyTopUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PolicyTopUpRepository extends JpaRepository<PolicyTopUp, UUID> {
    List<PolicyTopUp> findByPolicyApplicationId(Long policyApplicationId);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM PolicyTopUp t WHERE t.policyApplication.agent.user.id = :agentUserId AND t.status IN (com.example.comproject.entity.PolicyTopUp.TopUpStatus.REQUESTED, com.example.comproject.entity.PolicyTopUp.TopUpStatus.UNDER_REVIEW, com.example.comproject.entity.PolicyTopUp.TopUpStatus.AGENT_APPROVED, com.example.comproject.entity.PolicyTopUp.TopUpStatus.APPROVED)")
    List<PolicyTopUp> findPendingByAgent(Long agentUserId);
}
