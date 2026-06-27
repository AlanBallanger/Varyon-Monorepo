package fr.varyon.vrpg.combat;

import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import fr.varyon.vrpg.integration.DamageFloatBridge;

import javax.annotation.Nonnull;

public final class CombatCritDetection {

    private CombatCritDetection() {}

    public static boolean isCriticalHit(@Nonnull Damage damage) {
        return DamageFloatBridge.isCritical(damage);
    }
}
