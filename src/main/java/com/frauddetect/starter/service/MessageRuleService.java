package com.frauddetect.starter.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class MessageRuleService {

    private static final String[] URGENCY_WORDS = {
            "urgent",
            "immediately",
            "act now",
            "act fast",
            "limited time",
            "final notice",
            "last warning",
            "expires today",
            "within 24 hours",
            "right now",
            "do not ignore",
            "account will be blocked",
            "account will be suspended"
    };

    private static final String[] MONEY_WORDS = {
            "send money",
            "transfer money",
            "bank transfer",
            "wire transfer",
            "upi",
            "payment",
            "pay now",
            "make payment",
            "processing fee",
            "registration fee",
            "security deposit",
            "advance payment",
            "refund",
            "cashback",
            "loan"
    };

    private static final String[] CREDENTIAL_WORDS = {
            "otp",
            "one time password",
            "password",
            "pin",
            "cvv",
            "bank details",
            "account number",
            "card number",
            "credit card",
            "debit card",
            "aadhaar",
            "pan card",
            "pan number",
            "verification code",
            "login details"
    };

    private static final String[] PRIZE_WORDS = {
            "winner",
            "you won",
            "won a prize",
            "lottery",
            "prize",
            "reward",
            "congratulations",
            "cash prize",
            "lucky winner",
            "claim your prize",
            "free gift"
    };

    private static final String[] IMPERSONATION_WORDS = {
            "bank",
            "police",
            "income tax",
            "income tax department",
            "customs",
            "government",
            "courier",
            "amazon",
            "flipkart",
            "google",
            "microsoft",
            "paypal",
            "netflix",
            "sbi",
            "hdfc",
            "icici",
            "axis bank"
    };

    private static final String[] PHISHING_WORDS = {
            "verify your account",
            "verify account",
            "confirm your identity",
            "confirm identity",
            "suspicious login",
            "reset your password",
            "update your account",
            "click to verify",
            "click here to verify",
            "login immediately",
            "security alert"
    };

    private static final Pattern URL_PATTERN =
            Pattern.compile(
                    "(https?://\\S+|www\\.\\S+)",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern PHONE_PATTERN =
            Pattern.compile(
                    "(\\+?\\d[\\d\\s-]{8,}\\d)"
            );

    public RuleScanResult scan(String text) {

        String safeText =
                text == null
                        ? ""
                        : text.trim();

        String lowerText =
                safeText.toLowerCase();

        List<String> indicators =
                new ArrayList<>();

        int score = 0;

        /*
         * ---------------------------------------------------------
         * URGENCY
         * ---------------------------------------------------------
         */

        int urgencyMatches = 0;

        for (String word : URGENCY_WORDS) {

            if (lowerText.contains(word)) {

                urgencyMatches++;

                indicators.add(
                        "Contains urgency language: \"" +
                                word +
                                "\""
                );
            }
        }

        if (urgencyMatches > 0) {

            score +=
                    Math.min(
                            urgencyMatches * 10,
                            25
                    );
        }

        /*
         * ---------------------------------------------------------
         * MONEY / PAYMENT
         * ---------------------------------------------------------
         */

        int moneyMatches = 0;

        for (String word : MONEY_WORDS) {

            if (lowerText.contains(word)) {

                moneyMatches++;

                indicators.add(
                        "Mentions payment or money: \"" +
                                word +
                                "\""
                );
            }
        }

        if (moneyMatches > 0) {

            score +=
                    Math.min(
                            moneyMatches * 15,
                            30
                    );
        }

        /*
         * ---------------------------------------------------------
         * CREDENTIALS
         * ---------------------------------------------------------
         */

        int credentialMatches = 0;

        for (String word : CREDENTIAL_WORDS) {

            if (lowerText.contains(word)) {

                credentialMatches++;

                indicators.add(
                        "Requests sensitive information: \"" +
                                word +
                                "\""
                );
            }
        }

        if (credentialMatches > 0) {

            score +=
                    Math.min(
                            credentialMatches * 18,
                            35
                    );
        }

        /*
         * ---------------------------------------------------------
         * PRIZE / LOTTERY
         * ---------------------------------------------------------
         */

        int prizeMatches = 0;

        for (String word : PRIZE_WORDS) {

            if (lowerText.contains(word)) {

                prizeMatches++;

                indicators.add(
                        "Contains prize/reward language: \"" +
                                word +
                                "\""
                );
            }
        }

        if (prizeMatches > 0) {

            score +=
                    Math.min(
                            prizeMatches * 15,
                            30
                    );
        }

        /*
         * ---------------------------------------------------------
         * IMPERSONATION
         * ---------------------------------------------------------
         */

        int impersonationMatches = 0;

        for (String word : IMPERSONATION_WORDS) {

            if (lowerText.contains(word)) {

                impersonationMatches++;

                indicators.add(
                        "Mentions a commonly impersonated organisation: \"" +
                                word +
                                "\""
                );
            }
        }

        if (impersonationMatches > 0) {

            score +=
                    Math.min(
                            impersonationMatches * 8,
                            20
                    );
        }

        /*
         * ---------------------------------------------------------
         * PHISHING
         * ---------------------------------------------------------
         */

        int phishingMatches = 0;

        for (String word : PHISHING_WORDS) {

            if (lowerText.contains(word)) {

                phishingMatches++;

                indicators.add(
                        "Contains phishing language: \"" +
                                word +
                                "\""
                );
            }
        }

        if (phishingMatches > 0) {

            score +=
                    Math.min(
                            phishingMatches * 15,
                            30
                    );
        }

        /*
         * ---------------------------------------------------------
         * URL
         * ---------------------------------------------------------
         */

        int urlCount = 0;

        var urlMatcher =
                URL_PATTERN.matcher(
                        safeText
                );

        while (urlMatcher.find()) {
            urlCount++;
        }

        if (urlCount > 0) {

            score +=
                    Math.min(
                            urlCount * 12,
                            24
                    );

            indicators.add(
                    "Contains " +
                            urlCount +
                            " web link(s)"
            );
        }

        /*
         * ---------------------------------------------------------
         * PHONE NUMBER
         * ---------------------------------------------------------
         */

        if (PHONE_PATTERN.matcher(
                safeText
        ).find()) {

            score += 5;

            indicators.add(
                    "Contains a phone/contact number"
            );
        }

        /*
         * ---------------------------------------------------------
         * EXCESSIVE EXCLAMATION
         * ---------------------------------------------------------
         */

        long exclamationCount =
                safeText.chars()
                        .filter(c -> c == '!')
                        .count();

        if (exclamationCount >= 3) {

            score += 5;

            indicators.add(
                    "Uses excessive exclamation marks"
            );
        }

        /*
         * ---------------------------------------------------------
         * SUSPICIOUS COMBINATION BONUS
         * ---------------------------------------------------------
         */

        if (credentialMatches > 0
                && (moneyMatches > 0
                || urgencyMatches > 0)) {

            score += 15;

            indicators.add(
                    "Combines sensitive-information requests "
                            + "with urgency or payment pressure"
            );
        }

        if (urlCount > 0
                && urgencyMatches > 0) {

            score += 10;

            indicators.add(
                    "Combines a link with urgency language"
            );
        }

        /*
         * ---------------------------------------------------------
         * LIMIT
         * ---------------------------------------------------------
         */

        score =
                Math.min(
                        100,
                        Math.max(
                                0,
                                score
                        )
                );

        /*
         * ---------------------------------------------------------
         * CATEGORY
         * ---------------------------------------------------------
         */

        String category;

        if (credentialMatches > 0
                && phishingMatches > 0) {

            category = "Possible Phishing";

        } else if (prizeMatches > 0) {

            category = "Possible Lottery/Prize Scam";

        } else if (moneyMatches > 0
                && urgencyMatches > 0) {

            category = "Possible Payment Scam";

        } else if (impersonationMatches > 0
                && credentialMatches > 0) {

            category = "Possible Impersonation Scam";

        } else if (urlCount > 0
                && phishingMatches > 0) {

            category = "Possible Phishing";

        } else if (moneyMatches > 0) {

            category = "Possible Financial Scam";

        } else if (urgencyMatches > 0) {

            category = "Suspicious Message";

        } else if (score >= 20) {

            category = "Potential Scam";

        } else {

            category = "No Strong Scam Indicators";
        }

        return new RuleScanResult(
                score,
                indicators,
                category
        );
    }

    public static class RuleScanResult {

        public final int score;

        public final List<String> indicators;

        public final String likelyCategory;

        public RuleScanResult(
                int score,
                List<String> indicators,
                String likelyCategory) {

            this.score = score;

            this.indicators = indicators;

            this.likelyCategory =
                    likelyCategory;
        }
    }
}