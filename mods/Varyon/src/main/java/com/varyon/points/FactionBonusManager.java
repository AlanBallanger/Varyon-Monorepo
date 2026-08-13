package com.varyon.points;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.faction.FactionManager;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Bonus temporaire de dégâts/HP accordé aux membres de la faction dominante,
 * selon le palier atteint par la jauge globale de faction.
 *
 * Paliers (par rapport à la faction dominante, i.e. celle du côté duquel penche la jauge) :
 *   [0, 1/3]   -> +5% dégâts
 *   (1/3, 2/3] -> +5% dégâts, +5% HP max
 *   (2/3, max] -> +10% dégâts, +10% HP max
 */
public final class FactionBonusManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String HP_MODIFIER_KEY = "varyon.faction.hp";

    public static final BonusTier NONE = new BonusTier(1.0f, 1.0f);
    private static final BonusTier TIER_1 = new BonusTier(1.05f, 1.00f);
    private static final BonusTier TIER_2 = new BonusTier(1.05f, 1.05f);
    private static final BonusTier TIER_3 = new BonusTier(1.10f, 1.10f);

    private final PointsManager pointsManager;
    private final FactionManager factionManager;

    /** Dernier multiplicateur HP appliqué par joueur, pour n'écrire le modifier que sur changement. */
    private final Map<UUID, Float> lastAppliedHpMult = new ConcurrentHashMap<>();

    private Integer healthIdx = null;

    public FactionBonusManager(@Nonnull PointsManager pointsManager, @Nonnull FactionManager factionManager) {
        this.pointsManager = pointsManager;
        this.factionManager = factionManager;
    }

    public record BonusTier(float damageMultiplier, float hpMultiplier) {
    }

    /**
     * Palier de bonus pour une faction donnée, à partir du solde brut de la jauge.
     * Ne renvoie un bonus que si {@code faction} est la faction dominante (le signe du
     * solde correspond à son multiplicateur de perspective).
     */
    @Nonnull
    public BonusTier getTier(@Nonnull FactionManager.Faction faction, int rawBalance, int gaugeAbsMax) {
        if (gaugeAbsMax <= 0) return NONE;
        int perspective = faction.perspectiveGlobalBalance(rawBalance);
        if (perspective <= 0) return NONE;

        double ratio = (double) perspective / (double) gaugeAbsMax;
        if (ratio > 2.0 / 3.0) return TIER_3;
        if (ratio > 1.0 / 3.0) return TIER_2;
        return TIER_1;
    }

    @Nonnull
    public BonusTier getTierForPlayer(@Nonnull PlayerRef playerRef) {
        FactionManager.Faction faction = factionManager.getFaction(playerRef);
        if (faction == null) return NONE;
        return getTier(faction, pointsManager.getGlobalBalance(), pointsManager.getGuildGaugeAbsMax());
    }

    public float getDamageMultiplier(@Nonnull PlayerRef playerRef) {
        return getTierForPlayer(playerRef).damageMultiplier();
    }

    /** Recalcule et réapplique le bonus HP pour tous les joueurs actuellement en ligne. */
    public void refreshAllOnline() {
        try {
            for (PlayerRef playerRef : Universe.get().getPlayers()) {
                if (playerRef == null || !playerRef.isValid()) continue;
                try {
                    Ref ref = playerRef.getReference();
                    Store store = ref.getStore();
                    World world = ((EntityStore) store.getExternalData()).getWorld();
                    world.execute(() -> applyHpBonus(playerRef));
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).log("Failed to schedule HP bonus refresh for " + playerRef.getUsername() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("FactionBonusManager.refreshAllOnline: " + e.getMessage());
        }
    }

    public void applyHpBonus(@Nonnull PlayerRef playerRef) {
        try {
            if (!playerRef.isValid()) return;
            EntityStatMap statMap = playerRef.getComponent(EntityStatMap.getComponentType());
            if (statMap == null) return;

            int hIdx = getHealthIdx();
            if (hIdx < 0) return;

            float mult = getTierForPlayer(playerRef).hpMultiplier();
            UUID uuid = playerRef.getUuid();
            Float previous = lastAppliedHpMult.get(uuid);
            if (previous != null && previous == mult) {
                return;
            }

            if (mult == 1.0f) {
                statMap.removeModifier(EntityStatMap.Predictable.ALL, hIdx, HP_MODIFIER_KEY);
            } else {
                statMap.putModifier(EntityStatMap.Predictable.ALL, hIdx, HP_MODIFIER_KEY,
                    new StaticModifier(Modifier.ModifierTarget.MAX,
                        StaticModifier.CalculationType.MULTIPLICATIVE, mult));
            }
            statMap.maximizeStatValue(EntityStatMap.Predictable.ALL, hIdx);
            lastAppliedHpMult.put(uuid, mult);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("FactionBonusManager.applyHpBonus for " + playerRef.getUsername() + ": " + e.getMessage());
        }
    }

    public void clearPlayer(@Nonnull UUID uuid) {
        lastAppliedHpMult.remove(uuid);
    }

    private int getHealthIdx() {
        if (healthIdx == null) {
            try {
                healthIdx = DefaultEntityStatTypes.getHealth();
            } catch (Exception e) {
                healthIdx = -1;
            }
        }
        return healthIdx;
    }
}
