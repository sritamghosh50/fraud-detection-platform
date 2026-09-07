package com.frauddetect.starter.model;

/**
 * What the user submits when checking a suspicious message for scam signs.
 */
public class MessageAnalysisRequest {

    private String messageText;

    public MessageAnalysisRequest() {
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }
}