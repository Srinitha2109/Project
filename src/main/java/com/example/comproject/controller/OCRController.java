package com.example.comproject.controller;

import com.example.comproject.service.TesseractOCRService;
import com.example.comproject.service.GeminiDataExtractorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

@RestController
@RequestMapping("/api/ocr")
public class OCRController {

    private final TesseractOCRService tesseractOCRService;
    private final GeminiDataExtractorService geminiDataExtractorService;

    public OCRController(TesseractOCRService tesseractOCRService, GeminiDataExtractorService geminiDataExtractorService) {
        this.tesseractOCRService = tesseractOCRService;
        this.geminiDataExtractorService = geminiDataExtractorService;
    }

    @PreAuthorize("hasRole('POLICYHOLDER')")
    @PostMapping(value = "/extract", consumes = {"multipart/form-data"})
    public ResponseEntity<String> extractFields(
            @RequestParam("claimType") String claimType,
            @RequestPart("documents") List<MultipartFile> documents) {
        
        StringBuilder allText = new StringBuilder();
        for (MultipartFile file : documents) {
            String text = tesseractOCRService.extractTextFromPdf(file);
            // fallback if tesseract fails or its not a pdf (or we just process it directly)
            if (text == null || text.trim().isEmpty()) {
                // assume text could be read directly or fail gracefully
                text = "Tesseract extraction failed or empty.\n";
            }
            allText.append(text).append("\n");
        }
        
        String extractedJson = geminiDataExtractorService.extractStructuredData(allText.toString(), claimType);
        
        // return the JSON structure as string (spring will return application/json if we change content type, but string is fine for now, frontend parses it)
        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(extractedJson);
    }
}
