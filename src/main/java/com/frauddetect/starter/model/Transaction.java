package com.frauddetect.starter.model;

/**
 * This class describes what one transaction looks like.
 * When someone sends us data, it must match these fields.
 */
public class Transaction {

    private String transactionId;
    private String userId;
    private double amount;
    private String country;         // country the transaction happened in
    private String userHomeCountry; // country the user normally lives/transacts in
    private int hourOfDay;          // 0-23, what time the transaction happened

    // Empty constructor - Spring needs this to convert incoming JSON into this object
    public Transaction() {
    }

    // Getters and setters - these let other code read/write the fields above
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
}