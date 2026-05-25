package com.varyon.comet.config.model;

/**
 * Optional extra item roll after comet wave (Varyon ring 1–10). {@code chance} is 0.0–1.0.
 */
public record VaryonMineralBonus(String itemId, double chance) {
}
