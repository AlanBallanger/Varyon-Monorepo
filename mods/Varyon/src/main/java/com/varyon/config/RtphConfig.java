package com.varyon.config;

public class RtphConfig {
    private final int outerMax;
    private final int innerMax;

    public RtphConfig(int outerMax, int innerMax) {
        this.outerMax = outerMax;
        this.innerMax = innerMax;
    }

    public static RtphConfig createDefault() {
        return new RtphConfig(15000, 10000);
    }

    public int getOuterMax() { return outerMax; }
    public int getInnerMax() { return innerMax; }
}
