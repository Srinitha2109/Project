package com.example.comproject.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.comproject.dto.PolicyDTO;
import com.example.comproject.service.GeminiDataExtractorService;
import com.example.comproject.service.PolicyService;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final GeminiDataExtractorService geminiService;
    private final PolicyService policyService;

    public AIController(GeminiDataExtractorService geminiService, PolicyService policyService) {
        this.geminiService = geminiService;
        this.policyService = policyService;
    }

    @PostMapping("/recommend")
    public ResponseEntity<AIResponse> recommendPolicy(@RequestBody ChatRequest request) {
        List<PolicyDTO> policies = policyService.getActivePolicies();
        
        String policyContext = policies.stream()
            .map(p -> String.format("- %s: %s (Type: %s)", p.getPolicyName(), p.getDescription(), p.getInsuranceType()))
            .collect(Collectors.joining("\n"));

        String prompt = """
            You are a professional insurance advisor for 'Fortify Insurance'.
            Based on the following available insurance policies:
            %s
            
            The user says: "%s"
            
            Recommend the best policy and explain why in 2-3 concise sentences. If no policy fits, suggest they contact our agent. Use a helpful, professional tone as a 'Fortify AI Assistant'.""".formatted(policyContext, request.getMessage());

        String aiMessage = geminiService.generateResponse(prompt);
        return ResponseEntity.ok(new AIResponse(aiMessage));
    }

    public static class ChatRequest {
        private String message;
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class AIResponse {
        private String response;
        public AIResponse(String response) { this.response = response; }
        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }
    }
}
