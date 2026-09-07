package com.frauddetect.starter.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Scans a URL's structure for suspicious patterns - deterministic
 * red flags, same philosophy as MessageRuleService.
 */
@Service
public class UrlRuleService {

    private static final String[] LINK_SHORTENERS = {
            "bit.ly", "tinyurl.com", "goo.gl", "t.co", "ow.ly", "is.gd", "buff.ly", "rebrand.ly"
    };

    // Domains ending in these are sometimes used for cheap, disposable scam sites
    private static final String[] SUSPICIOUS_TLDS = {
            ".xyz", ".top", ".click", ".loan", ".work", ".gq", ".tk", ".ml"
    };

    // Common brands scammers impersonate with lookalike domains
    private static final String[] COMMONLY_IMPERSONATED = {
            "paypal", "amazon", "google", "microsoft", "apple", "netflix", "bank", "irs", "gov"
    };

    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile("https?://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

    public MessageRuleService.RuleScanResult scan(String url) {
        String lowerUrl = url.toLowerCase();
        List<String> indicators = new ArrayList<>();
        int score = 0;

        for (String shortener : LINK_SHORTENERS) {
            if (lowerUrl.contains(shortener)) {
                indicators.add("Uses a link shortener (" + shortener + ") which hides the real destination");
                score += 25;
            }
        }

        for (String tld : SUSPICIOUS_TLDS) {
            if (lowerUrl.contains(tld)) {
                indicators.add("Uses an unusual domain ending (" + tld + ") often associated with disposable websites");
                score += 20;
            }
        }

        if (IP_ADDRESS_PATTERN.matcher(lowerUrl).find()) {
            indicators.add("Uses a raw IP address instead of a normal domain name - highly unusual for legitimate sites");
            score += 35;
        }

        if (lowerUrl.contains("@")) {
            indicators.add("Contains an '@' symbol, which can be used to disguise the real destination");
            score += 30;
        }

        int hyphenCount = lowerUrl.length() - lowerUrl.replace("-", "").length();
        if (hyphenCount >= 3) {
            indicators.add("Contains " + hyphenCount + " hyphens, often used to mimic legitimate brand names");
            score += 15;
        }

        for (String brand : COMMONLY_IMPERSONATED) {
            if (lowerUrl.contains(brand) && !isLikelyOfficialDomain(lowerUrl, brand)) {
                indicators.add("Mentions \"" + brand + "\" but does not appear to be the official domain - possible impersonation");
                score += 30;
            }
        }

        score = Math.min(score, 100);

        String category = indicators.isEmpty() ? "No obvious structural red flags" : "Suspicious URL structure";

        return new MessageRuleService.RuleScanResult(score, indicators, category);
    }

    // Very simple check: does the brand name appear right before a known safe suffix?
    // This is intentionally basic - a real system would use a verified domain allowlist.
    private boolean isLikelyOfficialDomain(String url, String brand) {
        return url.contains("://" + brand + ".com") || url.contains("://www." + brand + ".com");
    }
}