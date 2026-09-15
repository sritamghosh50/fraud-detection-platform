package com.frauddetect.starter.service;

import com.frauddetect.starter.model.ScamCheckRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScamCheckRepository extends JpaRepository<ScamCheckRecord, Long> {

    List<ScamCheckRecord> findByOwnerEmailOrderByCheckedAtDesc(String ownerEmail);
}