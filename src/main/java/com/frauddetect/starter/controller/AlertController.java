package com.frauddetect.starter.controller;

import com.frauddetect.starter.model.Alert;
import com.frauddetect.starter.service.AlertRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Endpoints for alerts belonging to the authenticated user.
 *
 * - GET  /api/alerts/pending
 *      -> see the logged-in user's pending alerts
 *
 * - GET  /api/alerts
 *      -> see the logged-in user's alerts
 *
 * - POST /api/alerts/{id}/approve
 *      -> approve one of the logged-in user's alerts
 *
 * - POST /api/alerts/{id}/reject
 *      -> reject one of the logged-in user's alerts
 */
@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class AlertController {

    private final AlertRepository alertRepository;

    public AlertController(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    /**
     * Get the currently logged-in user's pending alerts.
     */
    @GetMapping("/pending")
    public List<Alert> getPendingAlerts() {

        String loggedInUser = getLoggedInUser();

        return alertRepository.findByUserIdAndReviewStatus(
                loggedInUser,
                "PENDING"
        );
    }

    /**
     * Get all alerts belonging to the currently logged-in user.
     */
    @GetMapping
    public List<Alert> getAllAlerts() {

        String loggedInUser = getLoggedInUser();

        return alertRepository.findByUserId(loggedInUser);
    }

    /**
     * Approve an alert belonging to the logged-in user.
     */
    @PostMapping("/{id}/approve")
    public Map<String, String> approveAlert(
            @PathVariable Long id,
            @RequestParam(defaultValue = "reviewer") String reviewedBy) {

        return updateAlertStatus(
                id,
                "APPROVED",
                reviewedBy
        );
    }

    /**
     * Reject an alert belonging to the logged-in user.
     */
    @PostMapping("/{id}/reject")
    public Map<String, String> rejectAlert(
            @PathVariable Long id,
            @RequestParam(defaultValue = "reviewer") String reviewedBy) {

        return updateAlertStatus(
                id,
                "REJECTED",
                reviewedBy
        );
    }

    /**
     * Update an alert only if it belongs to the authenticated user.
     */
    private Map<String, String> updateAlertStatus(
            Long id,
            String newStatus,
            String reviewedBy) {

        String loggedInUser = getLoggedInUser();

        Optional<Alert> alertOpt =
                alertRepository.findById(id);

        if (alertOpt.isEmpty()) {

            return Map.of(
                    "status",
                    "error",
                    "message",
                    "Alert with id " + id + " not found"
            );
        }

        Alert alert = alertOpt.get();

        // IMPORTANT:
        // Prevent one user from modifying another user's alert.
        if (!loggedInUser.equals(alert.getUserId())) {

            return Map.of(
                    "status",
                    "error",
                    "message",
                    "You are not authorized to modify this alert"
            );
        }

        alert.setReviewStatus(newStatus);
        alert.setReviewedAt(LocalDateTime.now());
        alert.setReviewedBy(loggedInUser);

        alertRepository.save(alert);

        return Map.of(
                "status",
                "success",
                "alertId",
                String.valueOf(id),
                "newStatus",
                newStatus
        );
    }

    /**
     * Get the email/username stored in the authenticated JWT.
     */
    private String getLoggedInUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        return authentication.getName();
    }
}