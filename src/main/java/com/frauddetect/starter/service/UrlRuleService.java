package com.frauddetect.starter.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UrlRuleService {

    private static final String[] SHORTENERS = {
            "bit.ly",
            "tinyurl.com",
            "goo.gl",
            "t.co",
            "ow.ly",
            "is.gd",
            "buff.ly",
            "rebrand.ly"
    };

    private static final String[] SUSPICIOUS_TLDS = {
            ".xyz",
            ".top",
            ".click",
            ".loan",
            ".work",
            ".tk",
            ".ml",
            ".gq"
    };

    private static final String[] SUSPICIOUS_WORDS = {
            "login",
            "verify",
            "verification",
            "secure",
            "security",
            "account",
            "update",
            "confirm",
            "password",
            "payment",
            "wallet",
            "bank",
            "claim",
            "reward",
            "prize",
            "winner",
            "free"
    };

    private static final String[] BRANDS = {
            "paypal",
            "amazon",
            "google",
            "microsoft",
            "apple",
            "netflix",
            "sbi",
            "hdfc",
            "icici",
            "axis"
    };

    private static final Pattern IP_PATTERN =
            Pattern.compile(
                    "^https?://\\d{1,3}(?:\\.\\d{1,3}){3}"
            );

    public MessageRuleService.RuleScanResult scan(
            String url) {

        String safeUrl =
                url == null
                        ? ""
                        : url.trim();

        String lower =
                safeUrl.toLowerCase();

        List<String> indicators =
                new ArrayList<>();

        int score = 0;

        if (safeUrl.isBlank()) {

            return new MessageRuleService.RuleScanResult(
                    50,
                    List.of("No URL was provided"),
                    "Invalid URL"
            );
        }

        String hostname =
                extractHostname(
                        safeUrl
                );

        if (hostname == null
                || hostname.isBlank()) {

            return new MessageRuleService.RuleScanResult(
                    50,
                    List.of(
                            "Could not identify the website hostname"
                    ),
                    "Invalid URL"
            );
        }

        /*
         * HTTPS
         */

        if (!lower.startsWith("https://")) {

            score += 12;

            indicators.add(
                    "The URL does not use HTTPS"
            );
        }

        /*
         * IP address
         */

        if (IP_PATTERN.matcher(
                lower
        ).find()) {

            score += 30;

            indicators.add(
                    "Uses an IP address instead of a domain name"
            );
        }

        /*
         * Punycode
         */

        if (hostname.contains(
                "xn--"
        )) {

            score += 25;

            indicators.add(
                    "Uses punycode, which can be used for look-alike domains"
            );
        }

        /*
         * URL length
         */

        if (safeUrl.length() > 120) {

            score += 10;

            indicators.add(
                    "URL is unusually long"
            );

        } else if (safeUrl.length() > 80) {

            score += 5;

            indicators.add(
                    "URL is relatively long"
            );
        }

        /*
         * @ symbol
         */

        if (safeUrl.contains("@")) {

            score += 25;

            indicators.add(
                    "Contains '@', which can disguise the actual destination"
            );
        }

        /*
         * Suspicious TLD
         */

        for (String tld :
                SUSPICIOUS_TLDS) {

            if (hostname.endsWith(tld)) {

                score += 20;

                indicators.add(
                        "Uses suspicious domain ending: "
                                + tld
                );

                break;
            }
        }

        /*
         * Shortener
         */

        for (String shortener :
                SHORTENERS) {

            if (hostname.equals(
                    shortener
            )
                    || hostname.endsWith(
                    "." + shortener
            )) {

                score += 20;

                indicators.add(
                        "Uses a URL shortener: "
                                + shortener
                );

                break;
            }
        }

        /*
         * Hyphens
         */

        int hyphens =
                hostname.length()
                        - hostname.replace(
                        "-",
                        ""
                ).length();

        if (hyphens >= 2) {

            score +=
                    hyphens >= 4
                            ? 12
                            : 6;

            indicators.add(
                    "Domain contains multiple hyphens"
            );
        }

        /*
         * Many subdomains
         */

        int dots =
                hostname.length()
                        - hostname.replace(
                        ".",
                        ""
                ).length();

        if (dots >= 3) {

            score += 10;

            indicators.add(
                    "Uses multiple subdomains"
            );
        }

        /*
         * Suspicious words anywhere in URL
         */

        int suspiciousWordMatches = 0;

        for (String word :
                SUSPICIOUS_WORDS) {

            if (lower.contains(word)) {

                suspiciousWordMatches++;

                indicators.add(
                        "Contains suspicious keyword: "
                                + word
                );
            }
        }

        if (suspiciousWordMatches > 0) {

            score +=
                    Math.min(
                            suspiciousWordMatches * 6,
                            24
                    );
        }

        /*
         * Brand impersonation
         */

        for (String brand :
                BRANDS) {

            if (hostname.contains(
                    brand
            )
                    && !isOfficialDomain(
                    hostname,
                    brand
            )) {

                score += 30;

                indicators.add(
                        "Possible impersonation of "
                                + brand
                );

                break;
            }
        }

        /*
         * Password / payment query
         */

        if (lower.contains(
                "password="
        )
                || lower.contains(
                "passwd="
        )
                || lower.contains(
                "cvv="
        )
                || lower.contains(
                "otp="
        )
                || lower.contains(
                "card=")) {

            score += 20;

            indicators.add(
                    "URL contains a sensitive-information parameter"
            );
        }

        /*
         * Final score
         */

        score =
                Math.min(
                        100,
                        Math.max(
                                0,
                                score
                        )
                );

        String category;

        if (score >= 70) {

            category =
                    "Highly Suspicious URL";

        } else if (score >= 40) {

            category =
                    "Suspicious URL Structure";

        } else if (score >= 15) {

            category =
                    "Some Risk Indicators";

        } else {

            category =
                    "No Strong URL Red Flags";
        }

        if (indicators.isEmpty()) {

            indicators.add(
                    "No strong structural red flags detected"
            );
        }

        return new MessageRuleService.RuleScanResult(
                score,
                indicators,
                category
        );
    }

    private String extractHostname(
            String url) {

        try {

            String normalized =
                    url;

            if (!normalized.matches(
                    "^https?://.*"
            )) {

                normalized =
                        "https://" +
                                normalized;
            }

            URI uri =
                    new URI(
                            normalized
                    );

            return uri.getHost();

        } catch (Exception ignored) {

            return null;
        }
    }

    private boolean isOfficialDomain(
            String hostname,
            String brand) {

        return hostname.equals(
                brand + ".com"
        )
                || hostname.equals(
                "www." + brand + ".com"
        );
    }
}