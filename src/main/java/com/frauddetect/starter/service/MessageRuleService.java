package com.frauddetect.starter.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans message text for concrete, checkable scam indicators.
 * This is deterministic (same input always gives same output) -
 * unlike the LLM, which we treat as a helpful but not fully trusted signal.
 */
@Service
public class MessageRuleService {

    // Phrases commonly used in scam messages to create urgency or pressure
    private static final String[] URGENCY_PHRASES = {
            "act now", "urgent", "immediately", "verify your account", "account suspended",
            "act fast", "limited time", "final notice", "your account will be locked",
            "click here now", "expires today"
    };

    // Phrases asking for money or sensitive info upfront - classic scam pattern
    private static final String[] REQUEST_PHRASES = {
            "otp", "one time password", "processing fee", "registration fee",
            "send money", "wire transfer", "gift card", "bank details",
            "social security number", "pan card", "aadhaar", "cvv", "pin number",
            "advance payment", "security deposit"
    };

    // Common scam categories and their keywords, used as a first guess
    // before the LLM refines it
    private static final String[] JOB_SCAM_WORDS = {"job offer", "work from home", "hiring", "salary", "interview"};
    private static final String[] LOTTERY_SCAM_WORDS = {"congratulations", "winner", "lottery", "prize", "claim your"};
    private static final String[] PHISHING_WORDS = {"verify your account", "confirm your identity", "suspicious login", "reset your password"};

    private static final Pattern URL_PATTERN = Pattern.compile("(https?://\\S+|www\\.\\S+|\\S+\\.(com|net|org|xyz|info|biz)\\S*)", Pattern.CASE_INSENSITIVE);

    public RuleScanResult scan(String text) {
        String lowerText = text.toLowerCase();
        List<String> indicators = new ArrayList<>();
        int score = 0;

        for (String phrase : URGENCY_PHRASES) {
            if (lowerText.contains(phrase)) {
                indicators.add("Contains urgency language: \"" + phrase + "\"");
                score += 15;
            }
        }

        for (String phrase : REQUEST_PHRASES) {
            if (lowerText.contains(phrase)) {
                indicators.add("Requests sensitive info or payment: \"" + phrase + "\"");
                score += 25;
            }
        }

        Matcher urlMatcher = URL_PATTERN.matcher(text);
        int urlCount = 0;
        while (urlMatcher.find()) {
            urlCount++;
        }
        if (urlCount > 0) {
            indicators.add("Contains " + urlCount + " link(s) - verify before clicking");
            score += 10 * urlCount;
        }

        String likelyCategory = guessCategory(lowerText);

        score = Math.min(score, 100);

        return new RuleScanResult(score, indicators, likelyCategory);
    }

    private String guessCategory(String lowerText) {
        if (containsAny(lowerText, JOB_SCAM_WORDS)) return "Possible Job Scam";
        if (containsAny(lowerText, LOTTERY_SCAM_WORDS)) return "Possible Lottery/Prize Scam";
        if (containsAny(lowerText, PHISHING_WORDS)) return "Possible Phishing";
        return "Unclear";
    }

    private boolean containsAny(String text, String[] words) {
        for (String word : words) {
            if (text.contains(word)) return true;
        }
        return false;
    }

    /**
     * Simple holder for what the rule scan found - not a full entity,
     * just used to pass data between our own classes.
     */
    public static class RuleScanResult {
        public final int score;
        public final List<String> indicators;
        public final String likelyCategory;

        public RuleScanResult(int score, List<String> indicators, String likelyCategory) {
            this.score = score;
            this.indicators = indicators;
            this.likelyCategory = likelyCategory;
        }
    }
}