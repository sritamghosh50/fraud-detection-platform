package com.frauddetect.starter.service;

import com.frauddetect.starter.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    // Spring automatically implements this just from the method name -
    // "find all alerts where reviewStatus equals this value"
    List<Alert> findByReviewStatus(String reviewStatus);
}