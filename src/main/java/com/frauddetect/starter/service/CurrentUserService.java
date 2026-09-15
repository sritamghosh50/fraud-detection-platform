package com.frauddetect.starter.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Helper to get the email of the currently logged-in user.
 */
@Service
public class CurrentUserService {

    public String getCurrentUserEmail() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "No authenticated user found for this request."
            );
        }

        return authentication.getName();
    }
}