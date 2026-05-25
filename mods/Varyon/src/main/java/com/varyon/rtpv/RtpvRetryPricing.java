package com.varyon.rtpv;

public final class RtpvRetryPricing {
    private static final double STEP_PERCENT = 0.10;
    private static final double MAX_PREMIUM_PERCENT = 0.50;

    private RtpvRetryPricing() {}

    public static int retryCost(int chainBase, int retryOrdinal) {
        if (chainBase <= 0) {
            return 0;
        }
        if (retryOrdinal < 1) {
            return chainBase;
        }
        int stepPremium = (int) Math.round(STEP_PERCENT * chainBase * retryOrdinal);
        int maxPremium = (int) Math.round(MAX_PREMIUM_PERCENT * chainBase);
        int premium = Math.min(stepPremium, maxPremium);
        return chainBase + premium;
    }
}
