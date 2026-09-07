package com.frauddetect.starter.controller;

import com.frauddetect.starter.model.Alert;
import com.frauddetect.starter.service.AlertRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Endpoints for a human reviewer to see and act on flagged transactions.
 * - GET  /api/alerts/pending   -> see everything waiting for review
 * - POST /api/alerts/{id}/approve  -> mark as approved (false alarm / allowed)
 * - POST /api/alerts/{id}/reject   -> mark as rejected (confirmed fraud / blocked)
 * - GET  /api/alerts          -> see all alerts, any status
 */
@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "http://localhost:5173")
public class AlertController {

    private final AlertRepository alertRepository;

    public AlertController(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @GetMapping("/pending")
    public List<Alert> getPendingAlerts() {
        return alertRepository.findByReviewStatus("PENDING");
    }

    @GetMapping
    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }

    @PostMapping("/{id}/approve")
    public Map<String, String> approveAlert(@PathVariable Long id, @RequestParam(defaultValue = "reviewer") String reviewedBy) {
        return updateAlertStatus(id, "APPROVED", reviewedBy);
    }

    @PostMapping("/{id}/reject")
    public Map<String, String> rejectAlert(@PathVariable Long id, @RequestParam(defaultValue = "reviewer") String reviewedBy) {
        return updateAlertStatus(id, "REJECTED", reviewedBy);
    }

    private Map<String, String> updateAlertStatus(Long id, String newStatus, String reviewedBy) {
        Optional<Alert> alertOpt = alertRepository.findById(id);

        if (alertOpt.isEmpty()) {
            return Map.of("status", "error", "message", "Alert with id " + id + " not found");
        }

        Alert alert = alertOpt.get();
        alert.setReviewStatus(newStatus);
        alert.setReviewedAt(LocalDateTime.now());
        alert.setReviewedBy(reviewedBy);
        alertRepository.save(alert);

        return Map.of(
                "status", "success",
                "alertId", String.valueOf(id),
                "newStatus", newStatus
        );
    }
}