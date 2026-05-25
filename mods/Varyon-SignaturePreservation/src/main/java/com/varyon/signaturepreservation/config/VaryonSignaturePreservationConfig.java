package com.varyon.signaturepreservation.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class VaryonSignaturePreservationConfig {
    public static final BuilderCodec<VaryonSignaturePreservationConfig> CODEC =
            BuilderCodec.builder(VaryonSignaturePreservationConfig.class, VaryonSignaturePreservationConfig::new)
                    .append(
                            new KeyedCodec<>("Enabled", Codec.BOOLEAN),
                            (config, value) -> config.enabled = value,
                            config -> config.enabled)
                    .add()
                    .append(
                            new KeyedCodec<>("Debug", Codec.BOOLEAN),
                            (config, value) -> config.debug = value,
                            config -> config.debug)
                    .add()
                    .append(
                            new KeyedCodec<>("RestoreDelayMs", Codec.LONG),
                            (config, value) -> config.restoreDelayMs = value,
                            config -> config.restoreDelayMs)
                    .add()
                    .build();

    private boolean enabled = true;
    private boolean debug = false;
    private long restoreDelayMs = 100L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public long getRestoreDelayMs() {
        return restoreDelayMs;
    }

    public void setRestoreDelayMs(long restoreDelayMs) {
        this.restoreDelayMs = restoreDelayMs;
    }

    @Override
    public String toString() {
        return "VaryonSignaturePreservationConfig{enabled="
                + enabled
                + ", debug="
                + debug
                + ", restoreDelayMs="
                + restoreDelayMs
                + "}";
    }
}
