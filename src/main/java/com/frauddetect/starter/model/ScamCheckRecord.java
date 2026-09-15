package com.frauddetect.starter.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Saves the result of a message, URL, or image scam check -
 * same idea as TransactionRecord, but for Stage 8's features.
 */
@Entity
public class ScamCheckRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String checkType; // "MESSAGE", "URL", or "IMAGE"

    @Column(length = 2000)
    private String inputSummary; // the pasted text, the URL, or the OCR-extracted text

    private String scamCategory;
    private int riskScore;
    private String riskLevel;
    private boolean scam;

    @Column(length = 2000)
    private String llmExplanation;

    @Column(length = 1000)
    private String recommendation;

    private String ownerEmail;
    private LocalDateTime checkedAt;

    public ScamCheckRecord() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCheckType() {
        return checkType;
    }

    public void setCheckType(String checkType) {
        this.checkType = checkType;
    }

    public String getInputSummary() {
        return inputSummary;
    }

    public void setInputSummary(String inputSummary) {
        this.inputSummary = inputSummary;
    }

    public String getScamCategory() {
        return scamCategory;
    }

    public void setScamCategory(String scamCategory) {
        this.scamCategory = scamCategory;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public boolean isScam() {
        return scam;
    }

    public void setScam(boolean scam) {
        this.scam = scam;
    }

    public String getLlmExplanation() {
        return llmExplanation;
    }

    public void setLlmExplanation(String llmExplanation) {
        this.llmExplanation = llmExplanation;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }
}