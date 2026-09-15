package com.frauddetect.starter.controller;

import com.frauddetect.starter.model.MessageAnalysisRequest;
import com.frauddetect.starter.model.MessageAnalysisResult;
import com.frauddetect.starter.model.ScamCheckRecord;
import com.frauddetect.starter.service.CurrentUserService;
import com.frauddetect.starter.service.MessageAnalysisService;
import com.frauddetect.starter.service.OcrService;
import com.frauddetect.starter.service.ScamCheckRepository;
import com.frauddetect.starter.service.UrlAnalysisService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174",
        "https://fraudguard-frontend-8eim.onrender.com"
})
public class MessageController {

    private final MessageAnalysisService messageAnalysisService;
    private final OcrService ocrService;
    private final UrlAnalysisService urlAnalysisService;
    private final ScamCheckRepository scamCheckRepository;
    private final CurrentUserService currentUserService;

    public MessageController(
            MessageAnalysisService messageAnalysisService,
            OcrService ocrService,
            UrlAnalysisService urlAnalysisService,
            ScamCheckRepository scamCheckRepository,
            CurrentUserService currentUserService) {

        this.messageAnalysisService =
                messageAnalysisService;

        this.ocrService =
                ocrService;

        this.urlAnalysisService =
                urlAnalysisService;

        this.scamCheckRepository =
                scamCheckRepository;

        this.currentUserService =
                currentUserService;
    }

    @PostMapping("/analyze")
    public MessageAnalysisResult analyzeMessage(
            @RequestBody MessageAnalysisRequest request) {

        MessageAnalysisResult result =
                messageAnalysisService.analyze(
                        request.getMessageText()
                );

        saveCheckRecord(
                "MESSAGE",
                request.getMessageText(),
                result
        );

        return result;
    }

    @PostMapping("/analyze-url")
    public MessageAnalysisResult analyzeUrl(
            @RequestBody Map<String, String> request) {

        String url =
                request.get("url");

        MessageAnalysisResult result =
                urlAnalysisService.analyze(url);

        saveCheckRecord(
                "URL",
                url,
                result
        );

        return result;
    }

    @PostMapping("/analyze-image")
    public Object analyzeImage(
            @RequestParam("image") MultipartFile imageFile) {

        try {

            /*
             * STEP 1
             * Extract text from image.
             */
            String extractedText =
                    ocrService.extractText(
                            imageFile
                    );

            String cleanedText =
                    cleanOcrText(
                            extractedText
                    );

            /*
             * STEP 2
             * If OCR could not find meaningful text,
             * return a clear result.
             */
            if (!hasMeaningfulText(cleanedText)) {

                MessageAnalysisResult result =
                        createNoTextResult();

                saveCheckRecord(
                        "IMAGE",
                        cleanedText,
                        result
                );

                return Map.of(
                        "extractedText",
                        cleanedText,
                        "analysis",
                        result
                );
            }

            /*
             * STEP 3
             * Analyze OCR text using the fast
             * deterministic message rules.
             *
             * No Ollama call here.
             */
            MessageAnalysisResult result =
                    messageAnalysisService.analyzeImageText(
                            cleanedText
                    );

            saveCheckRecord(
                    "IMAGE",
                    cleanedText,
                    result
            );

            return Map.of(
                    "extractedText",
                    cleanedText,
                    "analysis",
                    result
            );

        } catch (Exception e) {

            System.out.println(
                    "Image analysis failed: "
                            + e.getMessage()
            );

            return Map.of(
                    "error",
                    "Failed to process image: "
                            + e.getMessage()
            );
        }
    }

    private String cleanOcrText(
            String text) {

        if (text == null) {
            return "";
        }

        return text
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean hasMeaningfulText(
            String text) {

        if (text == null
                || text.isBlank()) {

            return false;
        }

        String cleaned =
                text.trim();

        if (cleaned.length() < 12) {
            return false;
        }

        int letterCount = 0;

        for (char c :
                cleaned.toCharArray()) {

            if (Character.isLetter(c)) {
                letterCount++;
            }
        }

        if (letterCount < 8) {
            return false;
        }

        double letterRatio =
                (double) letterCount
                        / cleaned.length();

        if (letterRatio < 0.45) {
            return false;
        }

        String[] words =
                cleaned.split("\\s+");

        int meaningfulWordCount = 0;

        for (String word : words) {

            String lettersOnly =
                    word.replaceAll(
                            "[^A-Za-z]",
                            ""
                    );

            if (lettersOnly.length() >= 3) {
                meaningfulWordCount++;
            }
        }

        if (meaningfulWordCount < 2) {
            return false;
        }

        if (!cleaned.contains(" ")) {
            return false;
        }

        int digitCount = 0;
        int symbolCount = 0;

        for (char c :
                cleaned.toCharArray()) {

            if (Character.isDigit(c)) {

                digitCount++;

            } else if (
                    !Character.isLetter(c)
                            && !Character.isWhitespace(c)
            ) {

                symbolCount++;
            }
        }

        if (symbolCount >
                cleaned.length() * 0.20) {

            return false;
        }

        if (digitCount > letterCount) {
            return false;
        }

        return true;
    }

    private MessageAnalysisResult createNoTextResult() {

        MessageAnalysisResult result =
                new MessageAnalysisResult();

        result.setScam(false);

        result.setScamCategory(
                "No Suspicious Text Detected"
        );

        result.setRiskScore(0);

        result.setRiskLevel("LOW");

        result.setRuleIndicators(
                List.of(
                        "No meaningful readable text was detected in the image"
                )
        );

        result.setLlmExplanation(
                "No readable scam or phishing text "
                        + "was detected in the image."
        );

        result.setRecommendation(
                "No suspicious text was detected. "
                        + "For better analysis, upload a clear screenshot "
                        + "if the image contains a message."
        );

        return result;
    }

    private void saveCheckRecord(
            String checkType,
            String inputSummary,
            MessageAnalysisResult result) {

        ScamCheckRecord record =
                new ScamCheckRecord();

        record.setCheckType(
                checkType
        );

        record.setInputSummary(
                inputSummary
        );

        record.setScamCategory(
                result.getScamCategory()
        );

        record.setRiskScore(
                result.getRiskScore()
        );

        record.setRiskLevel(
                result.getRiskLevel()
        );

        record.setScam(
                result.isScam()
        );

        record.setLlmExplanation(
                result.getLlmExplanation()
        );

        record.setRecommendation(
                result.getRecommendation()
        );

        record.setOwnerEmail(
                currentUserService
                        .getCurrentUserEmail()
        );

        record.setCheckedAt(
                LocalDateTime.now()
        );

        scamCheckRepository.save(
                record
        );
    }

    @GetMapping("/history")
    public List<ScamCheckRecord> getHistory() {

        String email =
                currentUserService
                        .getCurrentUserEmail();

        return scamCheckRepository
                .findByOwnerEmailOrderByCheckedAtDesc(
                        email
                );
    }
}