package com.frauddetect.starter.service;

import com.frauddetect.starter.model.FraudCheckResult;
import com.frauddetect.starter.model.Transaction;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests FraudRuleService in isolation - no Spring, no database, no Kafka.
 * We manually create a "fake" VelocityCheckService so this test doesn't
 * need a real Redis connection.
 */
class FraudRuleServiceTest {

    @Test
    void safeTransactionShouldNotBeFlagged() {
        // Create a fake VelocityCheckService that always says "not exceeded"
        VelocityCheckService fakeVelocityService = Mockito.mock(VelocityCheckService.class);
        Mockito.when(fakeVelocityService.isVelocityExceeded(Mockito.anyString())).thenReturn(false);

        FraudRuleService service = new FraudRuleService(fakeVelocityService);

        Transaction transaction = new Transaction();
        transaction.setTransactionId("TEST001");
        transaction.setUserId("testuser");
        transaction.setAmount(500);
        transaction.setCountry("IN");
        transaction.setUserHomeCountry("IN");
        transaction.setHourOfDay(14);

        FraudCheckResult result = service.check(transaction);

        assertFalse(result.isFlaggedAsFraud(), "A small, same-country, daytime transaction should not be flagged");
        assertEquals(0, result.getRiskScore());
    }

    @Test
    void largeAmountFromDifferentCountryAtNightShouldBeFlagged() {
        VelocityCheckService fakeVelocityService = Mockito.mock(VelocityCheckService.class);
        Mockito.when(fakeVelocityService.isVelocityExceeded(Mockito.anyString())).thenReturn(false);

        FraudRuleService service = new FraudRuleService(fakeVelocityService);

        Transaction transaction = new Transaction();
        transaction.setTransactionId("TEST002");
        transaction.setUserId("testuser");
        transaction.setAmount(150000);
        transaction.setCountry("US");
        transaction.setUserHomeCountry("IN");
        transaction.setHourOfDay(3);

        FraudCheckResult result = service.check(transaction);

        assertTrue(result.isFlaggedAsFraud(), "A large, foreign, late-night transaction should be flagged");
        assertTrue(result.getRiskScore() >= 50, "Risk score should be at least 50 for this clearly suspicious case");
    }

    @Test
    void velocityExceededShouldAddRiskEvenForSmallAmount() {
        // This time, simulate the user having made too many transactions recently
        VelocityCheckService fakeVelocityService = Mockito.mock(VelocityCheckService.class);
        Mockito.when(fakeVelocityService.isVelocityExceeded(Mockito.anyString())).thenReturn(true);

        FraudRuleService service = new FraudRuleService(fakeVelocityService);

        Transaction transaction = new Transaction();
        transaction.setTransactionId("TEST003");
        transaction.setUserId("testuser");
        transaction.setAmount(500);
        transaction.setCountry("IN");
        transaction.setUserHomeCountry("IN");
        transaction.setHourOfDay(14);

        FraudCheckResult result = service.check(transaction);

        assertEquals(35, result.getRiskScore(), "Velocity violation alone should add exactly 35 points");
    }
}