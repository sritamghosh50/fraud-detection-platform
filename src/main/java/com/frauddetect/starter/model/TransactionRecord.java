package com.frauddetect.starter.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

/**
 * This class represents one transaction record stored in the database.
 *
 * It stores:
 * 1. Original transaction information
 * 2. Rules engine result
 * 3. ML model result
 * 4. Final combined risk decision
 * 5. LLM-generated explanation
 */
@Entity
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transactionId;
    private String userId;
    private double amount;
    private String country;
    private String userHomeCountry;
    private int hourOfDay;

    // Rules engine result
    private boolean flaggedAsFraud;
    private int riskScore;

    private LocalDateTime checkedAt;

    // ML model result
    private Double mlRiskScore;
    private Boolean mlFlaggedAsFraud;

    // Final combined decision
    private Double finalRiskScore;
    private String riskLevel;
    private Boolean flaggedForReview;

    // LLM explanation
    @Column(length = 2000)
    private String llmExplanation;

    public TransactionRecord() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getUserHomeCountry() {
        return userHomeCountry;
    }

    public void setUserHomeCountry(String userHomeCountry) {
        this.userHomeCountry = userHomeCountry;
    }

    public int getHourOfDay() {
        return hourOfDay;
    }

    public void setHourOfDay(int hourOfDay) {
        this.hourOfDay = hourOfDay;
    }

    public boolean isFlaggedAsFraud() {
        return flaggedAsFraud;
    }

    public void setFlaggedAsFraud(boolean flaggedAsFraud) {
        this.flaggedAsFraud = flaggedAsFraud;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }

    public Double getMlRiskScore() {
        return mlRiskScore;
    }

    public void setMlRiskScore(Double mlRiskScore) {
        this.mlRiskScore = mlRiskScore;
    }

    public Boolean getMlFlaggedAsFraud() {
        return mlFlaggedAsFraud;
    }

    public void setMlFlaggedAsFraud(Boolean mlFlaggedAsFraud) {
        this.mlFlaggedAsFraud = mlFlaggedAsFraud;
    }

    public Double getFinalRiskScore() {
        return finalRiskScore;
    }

    public void setFinalRiskScore(Double finalRiskScore) {
        this.finalRiskScore = finalRiskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Boolean getFlaggedForReview() {
        return flaggedForReview;
    }

    public void setFlaggedForReview(Boolean flaggedForReview) {
        this.flaggedForReview = flaggedForReview;
    }

    public String getLlmExplanation() {
        return llmExplanation;
    }

    public void setLlmExplanation(String llmExplanation) {
        this.llmExplanation = llmExplanation;
    }
}