package com.frauddetect.starter.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetect.starter.service.JwtService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2LoginSuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;

    private final OAuth2AuthorizedClientService
            authorizedClientService;

    private final ObjectMapper objectMapper;

    private final RestTemplate restTemplate;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public OAuth2LoginSuccessHandler(
            JwtService jwtService,
            OAuth2AuthorizedClientService
                    authorizedClientService
    ) {

        this.jwtService =
                jwtService;

        this.authorizedClientService =
                authorizedClientService;

        this.objectMapper =
                new ObjectMapper();

        this.restTemplate =
                new RestTemplate();
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oauthUser =
                (OAuth2User)
                        authentication.getPrincipal();

        String provider =
                "unknown";

        if (authentication
                instanceof OAuth2AuthenticationToken) {

            OAuth2AuthenticationToken oauthToken =
                    (OAuth2AuthenticationToken)
                            authentication;

            provider =
                    oauthToken
                            .getAuthorizedClientRegistrationId();
        }

        System.out.println(
                "OAuth provider: " + provider
        );

        // =====================================================
        // GET USER NAME
        // =====================================================

        String fullName =
                oauthUser.getAttribute("name");

        if (fullName == null
                || fullName.isBlank()) {

            String login =
                    oauthUser.getAttribute("login");

            if (login != null
                    && !login.isBlank()) {

                fullName = login;
            }
        }

        if (fullName == null
                || fullName.isBlank()) {

            fullName = "User";
        }

        // =====================================================
        // GET EMAIL
        // =====================================================

        String email =
                oauthUser.getAttribute("email");

        System.out.println(
                "OAuth email from user profile: "
                + email
        );

        /*
         * Google normally gives the email directly.
         */
        if ("google".equalsIgnoreCase(provider)) {

            if (email == null
                    || email.isBlank()) {

                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Google did not provide an email address."
                );

                return;
            }
        }

        /*
         * GitHub can hide the email from the normal
         * user profile.
         *
         * In that case we call GitHub's:
         *
         * GET /user/emails
         *
         * using the real OAuth access token.
         */
        if ("github".equalsIgnoreCase(provider)) {

            if (email == null
                    || email.isBlank()) {

                email =
                        getGithubEmail(
                                authentication
                        );
            }

            System.out.println(
                    "GitHub email after lookup: "
                    + email
            );

            if (email == null
                    || email.isBlank()) {

                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Unable to get your GitHub email. " +
                        "Please make sure the GitHub OAuth application " +
                        "has the user:email permission."
                );

                return;
            }
        }

        // =====================================================
        // FINAL EMAIL CHECK
        // =====================================================

        if (email == null
                || email.isBlank()) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Unable to get email from OAuth provider."
            );

            return;
        }

        // =====================================================
        // GENERATE FRAUDGUARD JWT
        // =====================================================

        String token =
                jwtService.generateToken(email);

        System.out.println(
                "FraudGuard JWT generated for: "
                + email
        );

        // =====================================================
        // REDIRECT TO REACT
        // =====================================================

       String callbackUrl =
        frontendUrl
                + "/#/oauth2/callback";

        String redirectUrl =
                callbackUrl
                + "?token="
                + URLEncoder.encode(
                        token,
                        StandardCharsets.UTF_8
                )
                + "&email="
                + URLEncoder.encode(
                        email,
                        StandardCharsets.UTF_8
                )
                + "&fullName="
                + URLEncoder.encode(
                        fullName,
                        StandardCharsets.UTF_8
                );

        response.sendRedirect(
                redirectUrl
        );
    }

    // =========================================================
    // GET REAL GITHUB EMAIL
    // =========================================================

    private String getGithubEmail(
            Authentication authentication
    ) {

        try {

            if (!(authentication
                    instanceof OAuth2AuthenticationToken)) {

                return null;
            }

            OAuth2AuthenticationToken oauthToken =
                    (OAuth2AuthenticationToken)
                            authentication;

            String registrationId =
                    oauthToken
                            .getAuthorizedClientRegistrationId();

            OAuth2AuthorizedClient client =
                    authorizedClientService
                            .loadAuthorizedClient(
                                    registrationId,
                                    authentication.getName()
                            );

            if (client == null) {

                System.out.println(
                        "GitHub OAuth client not found."
                );

                return null;
            }

            if (client.getAccessToken() == null) {

                System.out.println(
                        "GitHub access token not found."
                );

                return null;
            }

            String accessToken =
                    client
                            .getAccessToken()
                            .getTokenValue();

            // =================================================
            // GITHUB API HEADERS
            // =================================================

            HttpHeaders headers =
                    new HttpHeaders();

            headers.setBearerAuth(
                    accessToken
            );

            headers.set(
                    "Accept",
                    "application/vnd.github+json"
            );

            headers.set(
                    "X-GitHub-Api-Version",
                    "2022-11-28"
            );

            HttpEntity<Void> entity =
                    new HttpEntity<>(
                            headers
                    );

            // =================================================
            // CALL GITHUB EMAIL API
            // =================================================

            ResponseEntity<String> result =
                    restTemplate.exchange(
                            "https://api.github.com/user/emails",
                            HttpMethod.GET,
                            entity,
                            String.class
                    );

            System.out.println(
                    "GitHub email API status: "
                    + result.getStatusCode()
            );

            if (!result.getStatusCode()
                    .is2xxSuccessful()) {

                return null;
            }

            String body =
                    result.getBody();

            if (body == null
                    || body.isBlank()) {

                return null;
            }

            // =================================================
            // PARSE EMAIL LIST
            // =================================================

            JsonNode emails =
                    objectMapper
                            .readTree(body);

            if (!emails.isArray()) {

                return null;
            }

            /*
             * First priority:
             * primary + verified email.
             */
            for (JsonNode emailNode :
                    emails) {

                boolean primary =
                        emailNode
                                .path("primary")
                                .asBoolean(false);

                boolean verified =
                        emailNode
                                .path("verified")
                                .asBoolean(false);

                String emailValue =
                        emailNode
                                .path("email")
                                .asText(null);

                if (primary
                        && verified
                        && emailValue != null
                        && !emailValue.isBlank()) {

                    return emailValue;
                }
            }

            /*
             * Second priority:
             * any verified email.
             */
            for (JsonNode emailNode :
                    emails) {

                boolean verified =
                        emailNode
                                .path("verified")
                                .asBoolean(false);

                String emailValue =
                        emailNode
                                .path("email")
                                .asText(null);

                if (verified
                        && emailValue != null
                        && !emailValue.isBlank()) {

                    return emailValue;
                }
            }

            /*
             * Last option:
             * any available email.
             */
            for (JsonNode emailNode :
                    emails) {

                String emailValue =
                        emailNode
                                .path("email")
                                .asText(null);

                if (emailValue != null
                        && !emailValue.isBlank()) {

                    return emailValue;
                }
            }

        } catch (Exception exception) {

            System.out.println(
                    "GitHub email lookup failed: "
                    + exception.getMessage()
            );
        }

        return null;
    }
}