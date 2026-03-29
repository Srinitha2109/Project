package com.example.comproject.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiDataExtractorService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;
    
    @Value("${gemini.mock:false}")
    private boolean mockMode;

    @Value("${gemini.model:google/gemini-1.5-flash}")
    private String geminiModel;

    public String generateResponse(String prompt) {
        if (mockMode) return "I recommend General Liability insurance if you are running a business with physical premises.";
        
        RestTemplate restTemplate = new RestTemplate();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        
        JSONArray messages = new JSONArray();
        messages.put(message);
        
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", geminiModel);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 1000);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(geminiApiKey);
        headers.set("HTTP-Referer", "http://localhost:8080"); 
        headers.set("X-Title", "Fortify Capstone AI Advisor");

        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

        System.out.println("---- SENDING AI REQUEST TO OPENROUTER ----\nModel: " + geminiModel + "\nPrompt excerpt: " + (prompt.length() > 100 ? prompt.substring(0, 100) : prompt));

        try {
            String response = restTemplate.postForObject(geminiApiUrl, entity, String.class);
            JSONObject jsonResponse = new JSONObject(response);
            String content = jsonResponse.getJSONArray("choices")
                               .getJSONObject(0)
                               .getJSONObject("message")
                               .getString("content");
            System.out.println("---- RECEIVED AI RESPONSE ----\n" + (content.length() > 100 ? content.substring(0, 100) : content));
            return content;
        } catch (Exception e) {
            System.err.println("CRITICAL: AI Call Failed - " + e.getMessage());
            e.printStackTrace();
            return "I'm sorry, I'm having trouble connecting to my brain right now. Please try again later.";
        }
    }

    public String extractStructuredData(String rawText, String claimTypeStr) {
        if (mockMode) {
            System.out.println("====== MOCK MODE ENABLED ======");
            System.out.println("Bypassing Gemini due to Exhausted Quota. Returning Mock JSON.");
            String mockJson = "{\n" +
                              "  \"extractedDate\": \"2026-03-20\",\n" +
                              "  \"extractedLocation\": \"Downtown Intersection\",\n" +
                              "  \"extractedAmount\": \"248500\",\n" +
                              "  \"extractedDamageDesc\": \"Severe frontend damage and broken windshield\",\n" +
                              "  \"extractedPersonName\": \"John Doe\",\n" +
                              "  \"extractedDoctorName\": \"Dr. Smith\",\n" +
                              "  \"extractedRecoveryPeriod\": \"3 months\"\n" +
                              "}";
            System.out.println(mockJson);
            System.out.println("==================================================");
            return mockJson;
        }
        
        RestTemplate restTemplate = new RestTemplate();
        String url = geminiApiUrl; // OpenRouter uses Bearer Auth

        String prompt = "You are an AI assistant specialized in extracting structured data from insurance claim documents. " +
                "Extract relevant details based on this claim type: " + claimTypeStr + "\n" +
                "The text is extracted from PDF scans using OCR.\n" +
                "Please output ONLY valid JSON map of the following fields and avoid any markdown formatting:\n" +
                "- extractedDate (YYYY-MM-DD)\n" +
                "- extractedLocation\n" +
                "- extractedAmount (Map any 'total amount', 'total cost', 'total repair cost', or 'medical cost' here as a number solely)\n" +
                "- extractedPersonName\n" +
                "- extractedRefNumber\n" +
                "- extractedOrgName\n" +
                "If it's Bodily Injury (General Liability or Workers Comp), also extract:\n" +
                "- extractedInjuryType\n" +
                "- extractedDoctorName\n" +
                "- extractedDoctorLicense\n" +
                "- extractedRecoveryPeriod\n" +
                "- extractedEmployeeName (if Worker comp)\n" +
                "If it's Property Damage (General Liability or Auto), also extract:\n" +
                "- extractedDamageDesc\n" +
                "- extractedRepairCost\n" +
                "- extractedAssessorName\n" +
                "- extractedVehicleNo\n" +
                "- extractedLicenseNo\n" +
                "If a field cannot be found, omit it or set to null.\n" +
                "------------- RAW TEXT -------------\n" + rawText;

        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        
        JSONArray messages = new JSONArray();
        messages.put(message);
        
        JSONObject requestBody = new JSONObject();
        // Model identifier for OpenRouter 
        requestBody.put("model", geminiModel);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 2000); // Prevent OpenRouter from inferring 65k tokens and failing validation

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(geminiApiKey); // Uses Bearer Token
        headers.set("HTTP-Referer", "http://localhost:8080"); // Required for OpenRouter rankings optionally
        headers.set("X-Title", "Fortify Capstone");

        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

        try {
            String response = restTemplate.postForObject(url, entity, String.class);
            JSONObject jsonResponse = new JSONObject(response);
            String rawJson = jsonResponse.getJSONArray("choices")
                                         .getJSONObject(0)
                                         .getJSONObject("message")
                                         .getString("content");
                                         System.out.println("---- OPENROUTER RAW RESPONSE ----\n" + rawJson);
            int startIndex = rawJson.indexOf("{");
            int endIndex = rawJson.lastIndexOf("}");
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                rawJson = rawJson.substring(startIndex, endIndex + 1);
            }
            
            System.out.println("====== SUCCESS: OCR EXTRACTION CONFIRMATION ======");
            System.out.println("JSON Data extracted beautifully from PDF:");
            System.out.println(rawJson);
            System.out.println("==================================================");
            
            return rawJson;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to call Gemini API");
            return "{}";
        }
    }
}
