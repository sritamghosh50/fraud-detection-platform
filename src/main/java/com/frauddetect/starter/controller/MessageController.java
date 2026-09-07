package com.frauddetect.starter.controller;

import com.frauddetect.starter.model.MessageAnalysisRequest;
import com.frauddetect.starter.model.MessageAnalysisResult;
import com.frauddetect.starter.service.MessageAnalysisService;
import com.frauddetect.starter.service.OcrService;
import com.frauddetect.starter.service.UrlAnalysisService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Endpoints for analyzing suspicious text messages, screenshots, and URLs.
 *
 * POST /api/messages/analyze
 * -> Analyze pasted text
 *
 * POST /api/messages/analyze-image
 * -> Upload screenshot, run OCR, then analyze extracted text
 *
 * POST /api/messages/analyze-url
 * -> Analyze a URL for suspicious characteristics
 */
@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "http://localhost:5173")
public class MessageController {

    private final MessageAnalysisService messageAnalysisService;
    private final OcrService ocrService;
    private final UrlAnalysisService urlAnalysisService;

    public MessageController(
            MessageAnalysisService messageAnalysisService,
            OcrService ocrService,
            UrlAnalysisService urlAnalysisService) {

        this.messageAnalysisService = messageAnalysisService;
        this.ocrService = ocrService;
        this.urlAnalysisService = urlAnalysisService;
    }

    /**
     * Analyze pasted text.
     */
    @PostMapping("/analyze")
    public MessageAnalysisResult analyzeMessage(
            @RequestBody MessageAnalysisRequest request) {

        return messageAnalysisService.analyze(
                request.getMessageText()
        );
    }

    /**
     * Analyze a URL.
     *
     * Example request:
     * {
     *     "url": "http://paypal-secure-verify-account.xyz/login"
     * }
     */
    @PostMapping("/analyze-url")
    public MessageAnalysisResult analyzeUrl(
            @RequestBody Map<String, String> request) {

        String url = request.get("url");

        return urlAnalysisService.analyze(url);
    }

    /**
     * Analyze an uploaded screenshot.
     *
     * The image is first processed by OCR.
     * The extracted text is then passed to the same
     * MessageAnalysisService used by /analyze.
     */
    @PostMapping("/analyze-image")
    public Object analyzeImage(
            @RequestParam("image") MultipartFile imageFile) {

        try {
            String extractedText =
                    ocrService.extractText(imageFile);

            if (extractedText == null
                    || extractedText.trim().isEmpty()) {

                return Map.of(
                        "error",
                        "No readable text was found in the image. Try a clearer screenshot."
                );
            }

            MessageAnalysisResult result =
                    messageAnalysisService.analyze(extractedText);

            return Map.of(
                    "extractedText",
                    extractedText.trim(),

                    "analysis",
                    result
            );

        } catch (Exception e) {

            return Map.of(
                    "error",
                    "Failed to process image: " + e.getMessage()
            );
        }
    }
}