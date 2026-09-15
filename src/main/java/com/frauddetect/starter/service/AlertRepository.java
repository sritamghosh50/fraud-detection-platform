package com.frauddetect.starter.service;

import com.frauddetect.starter.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    // Find alerts by review status.
    List<Alert> findByReviewStatus(String reviewStatus);

    // Find only alerts belonging to a specific user.
    List<Alert> findByUserId(String userId);

    // Find alerts belonging to a specific user and review status.
    List<Alert> findByUserIdAndReviewStatus(
            String userId,
            String reviewStatus
    );
}