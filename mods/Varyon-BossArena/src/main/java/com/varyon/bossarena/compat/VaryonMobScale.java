package com.varyon.bossarena.compat;

import com.varyon.bossarena.boss.BossModifiers;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.semver.SemverRange;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optional Varyon zone-scale bridge.
 * When Varyon is loaded, absorbs {@code MobScalingComponent} HP/DMG into BossArena modifiers
 * so arenas follow zone tier: {@code final = varyon × boss × players}.
 */
public final class VaryonMobScale {
    private static final Logger LOGGER = Logger.getLogger("BossArena");
    public static final PluginIdentifier VARYON_PLUGIN_ID = new PluginIdentifier("Varyon", "Varyon");

    public record Scale(float healthMultiplier, float damageMultiplier, int mobLevel) {
        public static final Scale NONE = new Scale(1.0f, 1.0f, 0);

        public boolean isIdentity() {
            return Math.abs(healthMultiplier - 1.0f) < 0.0001f
                    && Math.abs(damageMultiplier - 1.0f) < 0.0001f;
        }
    }

    private static volatile Boolean varyonLoaded;
    private static volatile Boolean bridgeAvailable;

    private VaryonMobScale() {}

    public static boolean isVaryonLoaded() {
        Boolean cached = varyonLoaded;
        if (cached != null) {
            return cached;
        }
        PluginManager pluginManager = PluginManager.get();
        boolean loaded = pluginManager != null
                && pluginManager.hasPlugin(VARYON_PLUGIN_ID, SemverRange.WILDCARD);
        varyonLoaded = loaded;
        return loaded;
    }

    /**
     * Reads Varyon scale from the entity, multiplies into {@code mods}, then neutralizes
     * Varyon combat multipliers on the component so they are not applied twice.
     */
    public static BossModifiers absorbInto(Store<EntityStore> store,
                                           Ref<EntityStore> entityRef,
                                           BossModifiers mods) {
        BossModifiers safeMods = mods != null ? mods : identityMods();
        if (store == null || entityRef == null || !entityRef.isValid() || !isVaryonLoaded()) {
            return safeMods;
        }
        if (!ensureBridge()) {
            return safeMods;
        }
        try {
            Scale scale = VaryonMobScaleBridge.read(store, entityRef);
            if (scale == null) {
                scale = Scale.NONE;
            }
            LOGGER.info("[HPDIAG] absorbInto boss componentPresent=" + (scale != Scale.NONE)
                    + " hp×" + scale.healthMultiplier() + " dmg×" + scale.damageMultiplier());
            BossModifiers combined = applyWorldScale(safeMods, scale.healthMultiplier(), scale.damageMultiplier());
            VaryonMobScaleBridge.neutralizeCombatMultipliers(store, entityRef);
            if (!scale.isIdentity()) {
                LOGGER.info("Absorbed Varyon mob scale into BossArena mods: lvl=" + scale.mobLevel()
                        + " hp×" + scale.healthMultiplier()
                        + " dmg×" + scale.damageMultiplier()
                        + " -> bossHp×" + combined.hpMultiplier()
                        + " bossDmg×" + combined.damageMultiplier());
            }
            return combined;
        } catch (NoClassDefFoundError | Exception e) {
            LOGGER.log(Level.FINE, "Varyon mob scale absorb skipped", e);
            bridgeAvailable = false;
            return safeMods;
        }
    }

    /** Strips Varyon loot/essence from an entity so arena wave mobs drop nothing. No-op without Varyon. */
    public static void suppressVaryonDrops(Store<EntityStore> store, Ref<EntityStore> entityRef) {
        if (store == null || entityRef == null || !entityRef.isValid() || !isVaryonLoaded()) {
            return;
        }
        if (!ensureBridge()) {
            return;
        }
        try {
            VaryonMobScaleBridge.zeroLootAndEssence(store, entityRef);
        } catch (NoClassDefFoundError | Exception e) {
            LOGGER.log(Level.FINE, "Varyon drop suppression skipped", e);
        }
    }

    public static BossModifiers applyWorldScale(BossModifiers mods, float worldHp, float worldDamage) {
        BossModifiers base = mods != null ? mods : identityMods();
        float hp = positive(base.hpMultiplier()) * positive(worldHp);
        float damage = positive(base.damageMultiplier()) * positive(worldDamage);
        return new BossModifiers(
                hp,
                damage,
                base.speedMultiplier(),
                base.scaleMultiplier(),
                base.attackRateMultiplier(),
                base.abilityCooldownMultiplier(),
                base.knockbackGivenMultiplier(),
                base.knockbackTakenMultiplier(),
                base.turnRateMultiplier(),
                base.regenMultiplier()
        );
    }

    private static boolean ensureBridge() {
        Boolean available = bridgeAvailable;
        if (available != null) {
            return available;
        }
        try {
            Class.forName("com.varyon.bossarena.compat.VaryonMobScaleBridge");
            Class.forName("com.varyon.component.MobScalingComponent");
            bridgeAvailable = true;
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            bridgeAvailable = false;
            LOGGER.info("Varyon MobScaling bridge unavailable (Varyon classes not present).");
            return false;
        }
    }

    private static float positive(float value) {
        if (!Float.isFinite(value) || value <= 0.0f) {
            return 1.0f;
        }
        return value;
    }

    private static BossModifiers identityMods() {
        return new BossModifiers(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 0f);
    }

    /** Test / reload helper. */
    public static void resetCache() {
        varyonLoaded = null;
        bridgeAvailable = null;
    }
}
