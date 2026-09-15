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

/**
 * Endpoints for analyzing suspicious text messages, screenshots, and URLs.
 */
@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
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

        this.messageAnalysisService = messageAnalysisService;
        this.ocrService = ocrService;
        this.urlAnalysisService = urlAnalysisService;
        this.scamCheckRepository = scamCheckRepository;
        this.currentUserService = currentUserService;
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

        String url = request.get("url");

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

            String extractedText =
                    ocrService.extractText(imageFile);

            String cleanedText =
                    cleanOcrText(extractedText);

            /*
             * IMPORTANT:
             * Do not send random OCR characters to the LLM.
             *
             * Many normal photos contain shapes, skin, furniture,
             * shadows, etc. Tesseract can sometimes interpret these
             * as random characters.
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
             * Only genuine readable text reaches the normal
             * phishing/scam analysis pipeline.
             */
            MessageAnalysisResult result =
                    messageAnalysisService.analyze(
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

            return Map.of(
                    "error",
                    "Failed to process image: " + e.getMessage()
            );
        }
    }

    /**
     * Cleans OCR output before deciding whether it is meaningful.
     */
    private String cleanOcrText(String text) {

        if (text == null) {
            return "";
        }

        String cleaned = text
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return cleaned;
    }

    /**
     * Stronger OCR validation.
     *
     * Random OCR output should NOT be treated as a real message.
     */
    private boolean hasMeaningfulText(String text) {

        if (text == null || text.isBlank()) {
            return false;
        }

        String cleaned = text.trim();

        /*
         * Very short OCR output is normally not enough
         * to perform reliable scam analysis.
         */
        if (cleaned.length() < 12) {
            return false;
        }

        /*
         * Count alphabetic characters.
         */
        int letterCount = 0;

        for (char c : cleaned.toCharArray()) {
            if (Character.isLetter(c)) {
                letterCount++;
            }
        }

        /*
         * Require enough alphabetic content.
         */
        if (letterCount < 8) {
            return false;
        }

        /*
         * Calculate percentage of alphabetic characters.
         *
         * Random OCR often contains many numbers and symbols.
         */
        double letterRatio =
                (double) letterCount / cleaned.length();

        if (letterRatio < 0.45) {
            return false;
        }

        /*
         * Extract word-like pieces.
         */
        String[] words =
                cleaned.split("\\s+");

        int meaningfulWordCount = 0;

        for (String word : words) {

            String lettersOnly =
                    word.replaceAll("[^A-Za-z]", "");

            /*
             * A real English word normally contains
             * at least 3 alphabetic characters.
             */
            if (lettersOnly.length() >= 3) {
                meaningfulWordCount++;
            }
        }

        /*
         * Require at least TWO meaningful words.
         *
         * Example:
         *
         * "URGENT VERIFY ACCOUNT"
         * -> accepted
         *
         * "x7@#k91"
         * -> rejected
         *
         * "a8Jk29"
         * -> rejected
         */
        if (meaningfulWordCount < 2) {
            return false;
        }

        /*
         * Reject strings that look like one long random
         * alphanumeric OCR token.
         */
        boolean containsSpace = cleaned.contains(" ");

        if (!containsSpace) {
            return false;
        }

        /*
         * Reject text dominated by unusual symbols.
         */
        int digitCount = 0;
        int symbolCount = 0;

        for (char c : cleaned.toCharArray()) {

            if (Character.isDigit(c)) {
                digitCount++;
            } else if (!Character.isLetter(c)
                    && !Character.isWhitespace(c)) {
                symbolCount++;
            }
        }

        if (symbolCount > cleaned.length() * 0.20) {
            return false;
        }

        /*
         * If the OCR output contains an excessive amount
         * of digits compared with letters, it is probably
         * not a readable message.
         */
        if (digitCount > letterCount) {
            return false;
        }

        return true;
    }

    /**
     * Creates a safe result when the image does not contain
     * enough readable text for phishing analysis.
     */
    private MessageAnalysisResult createNoTextResult() {

        MessageAnalysisResult result =
                new MessageAnalysisResult();

        result.setScam(false);

        result.setScamCategory(
                "No Suspicious Text Detected"
        );

        result.setRiskScore(5);

        result.setRiskLevel("LOW");

        result.setRuleIndicators(
                List.of(
                        "No meaningful readable text was detected in the image"
                )
        );

        result.setLlmExplanation(
                "The image did not contain enough readable text " +
                "for reliable phishing or scam analysis. " +
                "Random OCR characters were ignored."
        );

        result.setRecommendation(
                "No phishing message was detected from the readable text. " +
                "If the image contains a message, upload a clearer " +
                "screenshot with readable text."
        );

        return result;
    }

    private void saveCheckRecord(
            String checkType,
            String inputSummary,
            MessageAnalysisResult result) {

        ScamCheckRecord record =
                new ScamCheckRecord();

        record.setCheckType(checkType);

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
                currentUserService.getCurrentUserEmail()
        );

        record.setCheckedAt(
                LocalDateTime.now()
        );

        scamCheckRepository.save(record);
    }

    @GetMapping("/history")
    public List<ScamCheckRecord> getHistory() {

        String email =
                currentUserService.getCurrentUserEmail();

        return scamCheckRepository
                .findByOwnerEmailOrderByCheckedAtDesc(
                        email
                );
    }
}