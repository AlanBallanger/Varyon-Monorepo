package com.varyon.config;

public class RtpsConfig {
    private final int minBlocks;
    private final int maxBlocks;

    public RtpsConfig(int minBlocks, int maxBlocks) {
        this.minBlocks = minBlocks;
        this.maxBlocks = maxBlocks;
    }

    public static RtpsConfig createDefault() {
        return new RtpsConfig(10000, 15000);
    }

    public int getMinBlocks() { return minBlocks; }
    public int getMaxBlocks() { return maxBlocks; }
}
