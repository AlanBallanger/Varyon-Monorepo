package com.varyon.bossarena.ui;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bas-droite : nom du boss + jauge de vie, puis jusqu'à 5 joueurs avec leur part de dégâts/DPS.
 * Une instance par joueur, affichée pendant toute la durée d'un combat de boss à proximité.
 */
public final class BossDpsHud extends CustomUIHud {

    public static final String HUD_KEY = "bossarena_dps_hud";
    private static final int MAX_ROWS = 5;

    private static final ConcurrentHashMap<UUID, BossDpsHud> INSTANCES = new ConcurrentHashMap<>();

    private boolean built = false;
    private boolean visible = false;

    public BossDpsHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, HUD_KEY);
    }

    @Nonnull
    public static BossDpsHud getOrCreate(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        BossDpsHud existing = INSTANCES.get(uuid);
        if (existing != null) {
            return existing;
        }
        BossDpsHud hud = new BossDpsHud(playerRef);
        BossDpsHud race = INSTANCES.putIfAbsent(uuid, hud);
        if (race != null) {
            return race;
        }
        player.getHudManager().addCustomHud(playerRef, hud);
        return hud;
    }

    @Nullable
    public static BossDpsHud get(@Nonnull UUID uuid) {
        return INSTANCES.get(uuid);
    }

    public static void remove(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        BossDpsHud hud = INSTANCES.remove(uuid);
        if (hud == null) {
            return;
        }
        player.getHudManager().removeCustomHud(playerRef, HUD_KEY);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        builder.append("BossDpsHud.ui");
        built = true;
    }

    /** @param bossName nom affiché ; {@code null}/vide masque le panneau. */
    public void updateFight(@Nullable String bossName,
                            float bossHpCurrent,
                            float bossHpMax,
                            @Nonnull List<PlayerDamageRow> rows) {
        updateFight(bossName, bossHpCurrent, bossHpMax, rows, null);
    }

    /**
     * @param bossName nom affiché ; {@code null}/vide masque le panneau.
     * @param personalKills {@code null} pendant le combat (affiche la jauge HP normalement) ;
     *                      une valeur non nulle bascule sur "Vous avez tué X monstres" à la place de
     *                      la jauge — utilisé pendant la rémanence post-mort du boss, quand le HP
     *                      n'a plus de sens.
     */
    public void updateFight(@Nullable String bossName,
                            float bossHpCurrent,
                            float bossHpMax,
                            @Nonnull List<PlayerDamageRow> rows,
                            @Nullable Integer personalKills) {
        if (!built) {
            return;
        }
        UICommandBuilder builder = new UICommandBuilder();

        if (bossName == null || bossName.isBlank()) {
            if (!visible && built) {
                return;
            }
            builder.set("#DpsHudPanel.Visible", false);
            builder.set("#WaveHudPanel.Visible", false);
            update(false, builder);
            visible = false;
            return;
        }
        visible = true;

        builder.set("#DpsHudPanel.Visible", true);
        builder.set("#WaveHudPanel.Visible", false);
        builder.set("#BossName.TextSpans", Message.raw(bossName));
        double bossRatio = bossHpMax > 0f ? Math.max(0.0, Math.min(1.0, bossHpCurrent / bossHpMax)) : 0.0;

        if (personalKills != null) {
            builder.set("#BossHpBar.Visible", false);
            builder.set("#BossKillsText.Visible", true);
            builder.set("#BossKillsText.TextSpans", Message.raw("Vous avez tué " + personalKills + " monstre"
                    + (personalKills > 1 ? "s" : "")));
        } else {
            builder.set("#BossHpBar.Visible", true);
            builder.set("#BossKillsText.Visible", false);
            builder.set("#BossHpBarFill.Value", bossRatio);
            builder.set("#BossHpText.TextSpans",
                    Message.raw(Math.round(bossHpCurrent) + " / " + Math.round(bossHpMax)
                            + " (" + Math.round(bossRatio * 100) + "%)"));
        }

        long totalDamage = 0L;
        for (PlayerDamageRow row : rows) {
            totalDamage += row.damage();
        }
        totalDamage = Math.max(1L, totalDamage);

        for (int i = 0; i < MAX_ROWS; i++) {
            int rowNumber = i + 1;
            // Separator N sits right above player block N — only show it once that row is populated,
            // and never above the first row (no separator needed when there's nothing above it).
            if (rowNumber > 1) {
                builder.set("#PlayerSeparator" + rowNumber + ".Visible", i < rows.size());
            }
            if (i < rows.size()) {
                PlayerDamageRow row = rows.get(i);
                builder.set("#Player" + rowNumber + "Rank.TextSpans", Message.raw("#" + rowNumber));
                builder.set("#Player" + rowNumber + "Name.TextSpans", Message.raw(row.playerName()));
                builder.set("#Player" + rowNumber + "Damage.TextSpans",
                        Message.raw("Dégâts : " + row.damage()));
                builder.set("#Player" + rowNumber + "Dps.TextSpans",
                        Message.raw("DPS : " + row.dps()));
                // Share of total damage dealt by the group, not relative to the top player.
                double ratio = Math.max(0.0, Math.min(1.0, (double) row.damage() / (double) totalDamage));
                builder.set("#Player" + rowNumber + "BarFill.Value", ratio);
                builder.set("#Player" + rowNumber + "BarText.TextSpans",
                        Message.raw(Math.round(ratio * 100) + "%"));
                builder.set("#Player" + rowNumber + "Block.Visible", true);
            } else {
                builder.set("#Player" + rowNumber + "Block.Visible", false);
            }
        }

        update(false, builder);
    }

    /**
     * Phase vague (avant l'apparition du boss primaire) : progression de la vague en cours et
     * compteurs de kills, à la place du panneau combat habituel. Les totaux inconnus (répétition
     * infinie de vague) sont passés à {@code 0} et affichés "?" plutôt que "0", pour ne pas laisser
     * croire que le combat est terminé.
     */
    public void updateWaveProgress(int currentWave, int totalWaves,
                                   int aliveMobs, int wavePlannedMobs,
                                   int totalKilled, int personalKills) {
        if (!built) {
            return;
        }
        UICommandBuilder builder = new UICommandBuilder();
        visible = true;

        builder.set("#DpsHudPanel.Visible", false);
        builder.set("#WaveHudPanel.Visible", true);

        builder.set("#WaveNumberText.TextSpans", Message.raw(
                "Vague " + currentWave + "/" + (totalWaves > 0 ? String.valueOf(totalWaves) : "?")));
        builder.set("#WaveMobsRemainingText.TextSpans", Message.raw(
                "Monstres restants : " + aliveMobs + "/" + (wavePlannedMobs > 0 ? String.valueOf(wavePlannedMobs) : "?")));
        builder.set("#WaveTotalKilledText.TextSpans", Message.raw(
                "Total de monstres : " + totalKilled));
        builder.set("#WavePersonalKillsText.TextSpans", Message.raw(
                "Mes monstres tués : " + personalKills));

        update(false, builder);
    }

    public boolean isVisible() {
        return visible;
    }

    public record PlayerDamageRow(String playerName, long damage, long dps) {
    }
}
