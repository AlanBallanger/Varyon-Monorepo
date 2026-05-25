package com.varyon.comet.wave;

import com.varyon.comet.wave.CometTier;

public final class CometWaveRunner {

    private CometWaveRunner() {}

    public static long getTierTimeoutMs(CometTier tier) {
        return WaveThemeProvider.getTimeoutMillis(tier);
    }
}
