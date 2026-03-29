package com.example.comproject.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.apache.pdfbox.text.PDFTextStripper;

@Service
public class TesseractOCRService {

    public String extractTextFromPdf(MultipartFile file) {
        StringBuilder extractedText = new StringBuilder();
        try {
            // using Tesseract with default eng
            Tesseract tesseract = new Tesseract();
            // In Windows, tesseract needs tessdata location. We can point it or let it use defaults.
            tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
            tesseract.setLanguage("eng");

            try (InputStream is = file.getInputStream();
                 PDDocument document = PDDocument.load(is)) {
                 
                // 1. Try extracting text using PDFTextStripper (for digital/searchable PDFs)
                PDFTextStripper textStripper = new PDFTextStripper();
                String basicText = textStripper.getText(document);
                if (basicText != null && basicText.trim().length() > 50) {
                    extractedText.append(basicText);
                    return extractedText.toString();
                }

                // 2. Fallback to Tesseract OCR for scanned PDFs
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                for (int page = 0; page < document.getNumberOfPages(); page++) {
                    BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
                    
                    // Workaround for Invalid Memory Access in Tess4J: write to temp file
                    File tempImage = File.createTempFile("ocr_temp_" + page, ".png");
                    ImageIO.write(bufferedImage, "png", tempImage);
                    
                    String text = tesseract.doOCR(tempImage);
                    extractedText.append(text).append("\n");
                    
                    tempImage.delete();
                }
            }
        } catch (Throwable t) {
            System.err.println("Tesseract encountered a fatal error, returning partial text. Reason: " + t.getMessage());
        }
        return extractedText.toString();
    }
}
