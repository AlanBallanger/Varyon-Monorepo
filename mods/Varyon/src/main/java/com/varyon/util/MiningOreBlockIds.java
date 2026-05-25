package com.varyon.util;

import javax.annotation.Nonnull;
import java.util.Locale;

public final class MiningOreBlockIds {

    private MiningOreBlockIds() {}

    public static boolean isExcludedFromVaryonOreRewards(@Nonnull String blockId) {
        return blockId.toLowerCase(Locale.ROOT).contains("_cracked");
    }
}
