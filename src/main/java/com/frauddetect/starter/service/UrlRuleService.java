package com.frauddetect.starter.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UrlRuleService {

    private static final String[] LINK_SHORTENERS = {
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
            ".gq",
            ".tk",
            ".ml"
    };

    private static final String[] RESERVED_DOMAINS = {
            "example.com",
            "example.org",
            "example.net"
    };

    private static final String[] RESERVED_TLDS = {
            ".example",
            ".invalid",
            ".test",
            ".localhost"
    };

    /*
     * Official security testing domains.
     *
     * These are deterministic test cases.
     * We do NOT allow the LLM to downgrade them.
     */
    private static final String[] SECURITY_TEST_DOMAINS = {

            // Palo Alto Networks
            "test-phishing.testpanw.com",
            "test-malware.testpanw.com",
            "test-c2.testpanw.com",
            "test-ransomware.testpanw.com",
            "test-dnstun.testpanw.com",
            "test-dga.testpanw.com",
            "test-nrd.testpanw.com",
            "test-malicious-nrd.testpanw.com",
            "test-grayware.testpanw.com",
            "test-parked.testpanw.com",
            "test-proxy.testpanw.com",
            "test-fastflux.testpanw.com",
            "test-nxns.testpanw.com",
            "test-dangling-domain.testpanw.com",
            "test-dns-rebinding.testpanw.com",
            "test-dns-infiltration.testpanw.com",
            "test-wildcard-abuse.testpanw.com",
            "test-strategically-aged.testpanw.com",
            "test-compromised-dns.testpanw.com",
            "test-adtracking.testpanw.com",
            "test-cname-cloaking.testpanw.com",
            "test-stockpile-domain.testpanw.com",
            "test-squatting.testpanw.com",
            "test-subdomain-reputation.testpanw.com",
            "test-fake-software.testpanw.com",

            // CyberFOX
            "phishing_and_deception.test.cyberfox.com",
            "malware.test.cyberfox.com",
            "botnet.test.cyberfox.com"
    };

    private static final String[] COMMONLY_IMPERSONATED = {
            "paypal",
            "amazon",
            "google",
            "microsoft",
            "apple",
            "netflix",
            "bank",
            "irs",
            "gov"
    };

    private static final Pattern IP_ADDRESS_PATTERN =
            Pattern.compile(
                    "https?://\\d{1,3}(?:\\.\\d{1,3}){3}"
            );

    public MessageRuleService.RuleScanResult scan(String url) {

        String lowerUrl =
                url == null
                        ? ""
                        : url.toLowerCase().trim();

        List<String> indicators =
                new ArrayList<>();

        double score = 5;

        String hostname =
                extractHostname(lowerUrl);

        /*
         * ---------------------------------------------------------
         * INVALID URL
         * ---------------------------------------------------------
         */

        if (hostname == null || hostname.isBlank()) {

            return new MessageRuleService.RuleScanResult(
                    50,
                    List.of(
                            "Could not reliably identify the URL hostname"
                    ),
                    "Unclear URL Structure"
            );
        }


        /*
         * ---------------------------------------------------------
         * SECURITY TEST DOMAIN
         * ---------------------------------------------------------
         *
         * IMPORTANT:
         *
         * This check happens FIRST.
         *
         * These are controlled security-testing domains.
         * The score is forced to 95.
         *
         * The LLM cannot downgrade this result later.
         * ---------------------------------------------------------
         */

        String securityTestType =
                getSecurityTestType(hostname);

        if (securityTestType != null) {

            indicators.add(
                    "Matches an official security-testing domain for "
                            + securityTestType
            );

            return new MessageRuleService.RuleScanResult(
                    95,
                    indicators,
                    "Security Test Domain - " + securityTestType
            );
        }


        /*
         * ---------------------------------------------------------
         * RESERVED / INVALID DOMAIN
         * ---------------------------------------------------------
         */

        if (isReservedDomain(hostname)) {

            indicators.add(
                    "Uses a reserved/test domain (" +
                            hostname +
                            ") intended for examples, testing, "
                            + "or documentation"
            );

            score += 15;

            /*
             * Reserved domains are not automatically malicious.
             * Give them a controlled medium-low score.
             */

            score =
                    Math.max(
                            score,
                            20
                    );
        }


        /*
         * ---------------------------------------------------------
         * LINK SHORTENER
         * ---------------------------------------------------------
         */

        for (String shortener : LINK_SHORTENERS) {

            if (hostname.equals(shortener)
                    || hostname.endsWith("." + shortener)) {

                indicators.add(
                        "Uses a link shortener (" +
                                shortener +
                                ") which hides the final destination"
                );

                score += 20;

                break;
            }
        }


        /*
         * ---------------------------------------------------------
         * SUSPICIOUS TLD
         * ---------------------------------------------------------
         */

        for (String tld : SUSPICIOUS_TLDS) {

            if (hostname.endsWith(tld)) {

                indicators.add(
                        "Uses an unusual domain ending (" +
                                tld +
                                ") that can be associated with suspicious websites"
                );

                score += 15;

                break;
            }
        }


        /*
         * ---------------------------------------------------------
         * RAW IP ADDRESS
         * ---------------------------------------------------------
         */

        if (IP_ADDRESS_PATTERN.matcher(lowerUrl).find()) {

            indicators.add(
                    "Uses a raw IP address instead of a normal domain name"
            );

            score += 25;
        }


        /*
         * ---------------------------------------------------------
         * @ SYMBOL
         * ---------------------------------------------------------
         */

        if (lowerUrl.contains("@")) {

            indicators.add(
                    "Contains an '@' symbol, which can be used "
                            + "to disguise the real destination"
            );

            score += 25;
        }


        /*
         * ---------------------------------------------------------
         * HYPHENS
         * ---------------------------------------------------------
         */

        int hyphenCount =
                hostname.length()
                        - hostname.replace("-", "").length();

        if (hyphenCount >= 2) {

            indicators.add(
                    "Contains "
                            + hyphenCount
                            + " hyphen(s) in the domain, sometimes used "
                            + "to mimic legitimate brand names"
            );

            score +=
                    hyphenCount >= 3
                            ? 10
                            : 5;
        }


        /*
         * ---------------------------------------------------------
         * LONG HOSTNAME
         * ---------------------------------------------------------
         */

        if (hostname.length() > 40) {

            indicators.add(
                    "Uses an unusually long hostname"
            );

            score += 10;
        }


        /*
         * ---------------------------------------------------------
         * MULTIPLE SUBDOMAINS
         * ---------------------------------------------------------
         */

        int subdomainCount =
                Math.max(
                        0,
                        hostname.split("\\.").length - 2
                );

        if (subdomainCount >= 3) {

            indicators.add(
                    "Contains multiple subdomains, which can sometimes "
                            + "be used to make a domain appear more legitimate"
            );

            score += 10;
        }


        /*
         * ---------------------------------------------------------
         * SENSITIVE WORDS
         * ---------------------------------------------------------
         */

        String[] suspiciousWords = {
                "login",
                "verify",
                "verification",
                "secure",
                "security",
                "account",
                "update",
                "confirm",
                "password",
                "payment"
        };

        int suspiciousWordCount = 0;

        for (String word : suspiciousWords) {

            if (hostname.contains(word)) {

                suspiciousWordCount++;
            }
        }

        if (suspiciousWordCount >= 2) {

            indicators.add(
                    "Hostname contains multiple security/account-related words"
            );

            score += 10;
        }


        /*
         * ---------------------------------------------------------
         * BRAND IMPERSONATION
         * ---------------------------------------------------------
         */

        for (String brand : COMMONLY_IMPERSONATED) {

            if (hostname.contains(brand)
                    && !isLikelyOfficialDomain(
                            hostname,
                            brand
                    )) {

                indicators.add(
                        "Mentions \""
                                + brand
                                + "\" but does not appear to be the official "
                                + "domain - possible impersonation"
                );

                score += 30;

                break;
            }
        }


        /*
         * ---------------------------------------------------------
         * HTTPS
         * ---------------------------------------------------------
         */

        if (!lowerUrl.startsWith("https://")) {

            indicators.add(
                    "Does not use HTTPS"
            );

            score += 10;
        }


        /*
         * ---------------------------------------------------------
         * FINAL SCORE
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

        int finalScore =
                (int) Math.round(score);


        /*
         * ---------------------------------------------------------
         * CATEGORY
         * ---------------------------------------------------------
         */

        String category;

        if (isReservedDomain(hostname)) {

            category =
                    "Reserved / Test Domain";

        } else if (indicators.isEmpty()) {

            category =
                    "No Obvious Structural Red Flags";

        } else if (containsKnownBrand(hostname)) {

            category =
                    "Possible Brand Impersonation";

        } else {

            category =
                    "Suspicious URL Structure";
        }


        /*
         * ---------------------------------------------------------
         * DEBUG LOG
         * ---------------------------------------------------------
         */

        System.out.println(
                "========================================"
        );

        System.out.println(
                "FraudGuard URL Rule Analysis"
        );

        System.out.println(
                "URL: " + url
        );

        System.out.println(
                "Hostname: " + hostname
        );

        System.out.println(
                "Security Test Type: "
                        + securityTestType
        );

        System.out.println(
                "Rule Score: "
                        + finalScore
        );

        System.out.println(
                "Category: "
                        + category
        );

        System.out.println(
                "========================================"
        );


        return new MessageRuleService.RuleScanResult(
                finalScore,
                indicators,
                category
        );
    }


    /*
     * =========================================================
     * SECURITY TEST DOMAIN DETECTION
     * =========================================================
     */

    private String getSecurityTestType(
            String hostname
    ) {

        if (hostname == null
                || hostname.isBlank()) {

            return null;
        }

        /*
         * Palo Alto phishing
         */
        if (matchesDomain(
                hostname,
                "test-phishing.testpanw.com"
        )) {

            return "PHISHING";
        }


        /*
         * Palo Alto malware
         */
        if (matchesDomain(
                hostname,
                "test-malware.testpanw.com"
        )) {

            return "MALWARE";
        }


        /*
         * Palo Alto C2
         */
        if (matchesDomain(
                hostname,
                "test-c2.testpanw.com"
        )) {

            return "COMMAND AND CONTROL";
        }


        /*
         * Palo Alto ransomware
         */
        if (matchesDomain(
                hostname,
                "test-ransomware.testpanw.com"
        )) {

            return "RANSOMWARE";
        }


        /*
         * Other Palo Alto test domains
         */
        String[] paloAltoDomains = {

                "test-dnstun.testpanw.com",
                "test-dga.testpanw.com",
                "test-nrd.testpanw.com",
                "test-malicious-nrd.testpanw.com",
                "test-grayware.testpanw.com",
                "test-parked.testpanw.com",
                "test-proxy.testpanw.com",
                "test-fastflux.testpanw.com",
                "test-nxns.testpanw.com",
                "test-dangling-domain.testpanw.com",
                "test-dns-rebinding.testpanw.com",
                "test-dns-infiltration.testpanw.com",
                "test-wildcard-abuse.testpanw.com",
                "test-strategically-aged.testpanw.com",
                "test-compromised-dns.testpanw.com",
                "test-adtracking.testpanw.com",
                "test-cname-cloaking.testpanw.com",
                "test-stockpile-domain.testpanw.com",
                "test-squatting.testpanw.com",
                "test-subdomain-reputation.testpanw.com",
                "test-fake-software.testpanw.com"
        };

        for (String domain : paloAltoDomains) {

            if (matchesDomain(
                    hostname,
                    domain
            )) {

                return "SECURITY TEST";
            }
        }


        /*
         * CyberFOX phishing
         */
        if (matchesDomain(
                hostname,
                "phishing_and_deception.test.cyberfox.com"
        )) {

            return "PHISHING";
        }


        /*
         * CyberFOX malware
         */
        if (matchesDomain(
                hostname,
                "malware.test.cyberfox.com"
        )) {

            return "MALWARE";
        }


        /*
         * CyberFOX botnet
         */
        if (matchesDomain(
                hostname,
                "botnet.test.cyberfox.com"
        )) {

            return "BOTNET";
        }


        return null;
    }


    private boolean matchesDomain(
            String hostname,
            String domain
    ) {

        return hostname.equals(domain)
                || hostname.endsWith("." + domain);
    }


    /*
     * =========================================================
     * RESERVED DOMAIN DETECTION
     * =========================================================
     */

    private boolean isReservedDomain(
            String hostname
    ) {

        if (hostname == null
                || hostname.isBlank()) {

            return false;
        }

        for (String domain : RESERVED_DOMAINS) {

            if (hostname.equals(domain)
                    || hostname.endsWith("." + domain)) {

                return true;
            }
        }

        for (String tld : RESERVED_TLDS) {

            if (hostname.endsWith(tld)
                    || hostname.contains(tld + ".")) {

                return true;
            }
        }

        return false;
    }


    /*
     * =========================================================
     * KNOWN BRAND DETECTION
     * =========================================================
     */

    private boolean containsKnownBrand(
            String hostname
    ) {

        for (String brand : COMMONLY_IMPERSONATED) {

            if (hostname.contains(brand)
                    && !isLikelyOfficialDomain(
                            hostname,
                            brand
                    )) {

                return true;
            }
        }

        return false;
    }


    /*
     * =========================================================
     * HOSTNAME EXTRACTION
     * =========================================================
     */

    private String extractHostname(
            String url
    ) {

        try {

            URI uri =
                    new URI(url);

            String host =
                    uri.getHost();

            if (host != null) {

                return host.toLowerCase();
            }

        } catch (URISyntaxException ignored) {

            /*
             * Fall back to simple hostname extraction.
             */
        }


        String cleaned =
                url
                        .replaceFirst(
                                "^https?://",
                                ""
                        )
                        .split("/")[0]
                        .split(":")[0];

        return cleaned.toLowerCase();
    }


    /*
     * =========================================================
     * OFFICIAL DOMAIN CHECK
     * =========================================================
     */

    private boolean isLikelyOfficialDomain(
            String hostname,
            String brand
    ) {

        return hostname.equals(
                    brand + ".com"
                )
                || hostname.equals(
                    "www." + brand + ".com"
                );
    }
}