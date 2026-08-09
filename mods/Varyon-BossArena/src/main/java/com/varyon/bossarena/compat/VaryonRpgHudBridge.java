package com.varyon.bossarena.compat;

import fr.varyon.vrpg.ui.ClassXpHud;
import fr.varyon.vrpg.ui.ProfessionXpHud;

import java.util.UUID;

/**
 * Hard references to Varyon-RPG types. Loaded only when {@link VaryonRpgHud} confirms RPG is present.
 */
final class VaryonRpgHudBridge {

    private VaryonRpgHudBridge() {}

    /**
     * Hides or restores the RPG class/profession HUDs for one player.
     *
     * <p>Uses {@code get} rather than {@code getOrCreate}: a player who has no RPG HUD must not have
     * one created just so BossArena can hide it.
     */
    static void setHidden(UUID playerUuid, boolean hidden) {
        ClassXpHud classHud = ClassXpHud.get(playerUuid);
        if (classHud != null) {
            classHud.setHidden(hidden);
        }
        ProfessionXpHud professionHud = ProfessionXpHud.get(playerUuid);
        if (professionHud != null) {
            professionHud.setHidden(hidden);
        }
    }
}
