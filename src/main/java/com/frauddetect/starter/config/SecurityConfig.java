package com.frauddetect.starter.config;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;

import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;

    public SecurityConfig(
            JwtAuthFilter jwtAuthFilter,
            OAuth2LoginSuccessHandler oauth2LoginSuccessHandler
    ) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.oauth2LoginSuccessHandler = oauth2LoginSuccessHandler;
    }

    // =========================================================
    // PASSWORD ENCODER
    // =========================================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // =========================================================
    // OAUTH AUTHORIZATION REQUEST RESOLVER
    // =========================================================

    @Bean
    public OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository
    ) {

        DefaultOAuth2AuthorizationRequestResolver delegate =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository,
                        "/oauth2/authorization"
                );

        return new OAuth2AuthorizationRequestResolver() {

            @Override
            public OAuth2AuthorizationRequest resolve(
                    jakarta.servlet.http.HttpServletRequest request
            ) {

                OAuth2AuthorizationRequest authorizationRequest =
                        delegate.resolve(request);

                return customizeAuthorizationRequest(
                        authorizationRequest
                );
            }

            @Override
            public OAuth2AuthorizationRequest resolve(
                    jakarta.servlet.http.HttpServletRequest request,
                    String registrationId
            ) {

                OAuth2AuthorizationRequest authorizationRequest =
                        delegate.resolve(
                                request,
                                registrationId
                        );

                return customizeAuthorizationRequest(
                        authorizationRequest
                );
            }
        };
    }

    // =========================================================
    // CUSTOMIZE GOOGLE / GITHUB REQUEST
    // =========================================================

    private OAuth2AuthorizationRequest customizeAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest
    ) {

        if (authorizationRequest == null) {
            return null;
        }

        String registrationId =
                authorizationRequest.getAttribute(
                        "registration_id"
                );

        if (registrationId == null) {
            return authorizationRequest;
        }

        // GOOGLE
        if ("google".equalsIgnoreCase(registrationId)) {

            Map<String, Object> parameters =
                    new LinkedHashMap<>(
                            authorizationRequest
                                    .getAdditionalParameters()
                    );

            parameters.put(
                    "prompt",
                    "select_account consent"
            );

            return OAuth2AuthorizationRequest
                    .from(authorizationRequest)
                    .additionalParameters(parameters)
                    .build();
        }

        // GITHUB
        if ("github".equalsIgnoreCase(registrationId)) {

            Map<String, Object> parameters =
                    new LinkedHashMap<>(
                            authorizationRequest
                                    .getAdditionalParameters()
                    );

            parameters.put(
                    "prompt",
                    "select_account"
            );

            return OAuth2AuthorizationRequest
                    .from(authorizationRequest)
                    .additionalParameters(parameters)
                    .build();
        }

        return authorizationRequest;
    }

    // =========================================================
    // SECURITY FILTER CHAIN
    // =========================================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OAuth2AuthorizationRequestResolver authorizationRequestResolver
    ) throws Exception {

        http

                // -------------------------------------------------
                // CSRF
                // -------------------------------------------------

                .csrf(csrf -> csrf.disable())

                // -------------------------------------------------
                // CORS
                // -------------------------------------------------

                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )

                // -------------------------------------------------
                // SESSION
                // -------------------------------------------------

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )

                // -------------------------------------------------
                // AUTHORIZATION
                // -------------------------------------------------

                .authorizeHttpRequests(auth -> auth

                        // OPTIONS / CORS PREFLIGHT
                        .requestMatchers(
                                org.springframework.http.HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // NORMAL AUTHENTICATION APIs
                        .requestMatchers(
                                "/api/auth/**"
                        ).permitAll()

                        // OAUTH ENDPOINTS
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/**"
                        ).permitAll()

                        // HEALTH / MONITORING
                        .requestMatchers(
                                "/error",
                                "/actuator/health",
                                "/actuator/prometheus"
                        ).permitAll()

                        // EVERYTHING ELSE
                        .anyRequest().authenticated()
                )

                // -------------------------------------------------
                // OAUTH2 LOGIN
                // -------------------------------------------------

                .oauth2Login(oauth2 ->
                        oauth2
                                .authorizationEndpoint(
                                        authorization ->
                                                authorization
                                                        .authorizationRequestResolver(
                                                                authorizationRequestResolver
                                                        )
                                )
                                .successHandler(
                                        oauth2LoginSuccessHandler
                                )
                )

                // -------------------------------------------------
                // JWT FILTER
                // -------------------------------------------------

                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // =========================================================
    // CORS CONFIGURATION
    // =========================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(
                Arrays.asList(
                        "http://localhost:5173",
                        "http://localhost:5174",
                        "https://fraudguard-frontend-8eim.onrender.com"
                )
        );

        configuration.setAllowedMethods(
                Arrays.asList(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                Arrays.asList(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin"
                )
        );

        configuration.setExposedHeaders(
                Arrays.asList(
                        "Authorization"
                )
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}