package com.varyon.elite.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.config.ConfigManager;
import com.varyon.elite.EliteConfig;
import com.varyon.elite.EliteRegistry;
import com.varyon.elite.MobClass;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Admin dashboard for the elite subsystem: live view, per-zone spawn chance + tier weights
 * (editable, saved to {@code elite.toml}), and a read-only economy view. Opened by
 * {@code /varyon-elite} (or {@code /varyon-elite dashboard}). Mirrors the interactive-page pattern of
 * {@code com.varyon.tiers.TierLeaderboardGui} / BossArena's config page.
 */
public final class EliteDashboardPage extends InteractiveCustomUIPage<EliteDashboardPage.EventDataClass> {

    private static final String LAYOUT = "EliteDashboard.ui";
    private static final String ZONE_ROW = "EliteDashboardZoneRow.ui";
    private static final String TEXT_ROW = "EliteDashboardTextRow.ui";

    private static final String TAB_LIVE = "live";
    private static final String TAB_ZONES = "zones";
    private static final String TAB_ECONOMY = "economy";

    private final EliteConfig config;
    private final Path dataDir;

    private String tab = TAB_LIVE;
    private String status = "";

    public EliteDashboardPage(@Nonnull PlayerRef playerRef, @Nonnull ConfigManager configManager, @Nonnull Path dataDir) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventDataClass.CODEC);
        this.config = configManager.getEliteConfig();
        this.dataDir = dataDir;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events,
                      @Nonnull Store<EntityStore> store) {
        cmd.append(LAYOUT);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "close"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabLive", EventData.of("Action", "tab_" + TAB_LIVE));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabZones", EventData.of("Action", "tab_" + TAB_ZONES));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabEconomy", EventData.of("Action", "tab_" + TAB_ECONOMY));

        boolean live = TAB_LIVE.equals(tab);
        boolean zones = TAB_ZONES.equals(tab);
        boolean economy = TAB_ECONOMY.equals(tab);
        cmd.set("#LivePanel.Visible", live);
        cmd.set("#ZonesPanel.Visible", zones);
        cmd.set("#EconomyPanel.Visible", economy);

        cmd.set("#StatusLabel.Visible", !status.isBlank());
        cmd.set("#StatusLabel.Text", status);

        if (live) {
            buildLive(cmd);
        } else if (zones) {
            buildZones(cmd, events);
        } else {
            buildEconomy(cmd);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull EventDataClass data) {
        String action = data.action;
        if (action == null || action.isBlank()) {
            return;
        }
        if ("close".equals(action)) {
            close();
            return;
        }
        if (action.startsWith("tab_")) {
            tab = switch (action.substring(4)) {
                case TAB_ZONES -> TAB_ZONES;
                case TAB_ECONOMY -> TAB_ECONOMY;
                default -> TAB_LIVE;
            };
            status = "";
            rebuild();
            return;
        }
        if ("save_zones".equals(action)) {
            applyAndSaveZones(data);
            rebuild();
        }
    }

    // ------------------------------------------------------------------

    private void buildLive(@Nonnull UICommandBuilder cmd) {
        cmd.clear("#LiveList");
        var elites = EliteRegistry.get().activeElites();

        var byTier = new int[EliteConfig.TIER_COUNT + 1];
        var byClass = new java.util.EnumMap<MobClass, Integer>(MobClass.class);
        for (EliteRegistry.ActiveElite e : elites) {
            if (e.tier() >= 1 && e.tier() <= EliteConfig.TIER_COUNT) {
                byTier[e.tier()]++;
            }
            byClass.merge(e.mobClass(), 1, Integer::sum);
        }
        StringBuilder cls = new StringBuilder();
        byClass.forEach((c, n) -> cls.append(c).append('=').append(n).append("  "));
        cmd.set("#LiveSummary.Text", String.format(Locale.ROOT,
                "Élites actifs : %d   |   T1=%d T2=%d T3=%d T4=%d T5=%d   |   %s",
                elites.size(), byTier[1], byTier[2], byTier[3], byTier[4], byTier[5],
                cls.toString().trim()));

        int i = 0;
        for (EliteRegistry.ActiveElite e : elites) {
            cmd.append("#LiveList", TEXT_ROW);
            String row = "#LiveList[" + i + "] #RowText.Text";
            cmd.set(row, String.format(Locale.ROOT, "%s  T%d  %s%s",
                    e.mobClass(), e.tier(), e.worldName(),
                    e.namedId() != null ? "  \"" + e.namedId() + "\"" : ""));
            i++;
        }
        if (i == 0) {
            cmd.append("#LiveList", TEXT_ROW);
            cmd.set("#LiveList[0] #RowText.Text", "Aucun élite actif.");
        }
    }

    private void buildZones(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder events) {
        cmd.clear("#ZonesList");

        // The Save button captures every zone field's current text at click time.
        EventData save = new EventData().append("Action", "save_zones");

        for (int z = 1; z <= EliteConfig.ZONE_COUNT; z++) {
            EliteConfig.ZoneEliteDef def = config.getZoneDef(z);
            cmd.append("#ZonesList", ZONE_ROW);
            String base = "#ZonesList[" + (z - 1) + "] ";
            cmd.set(base + "#ZoneLabel.Text", String.valueOf(z));
            if (def == null) {
                continue;
            }
            cmd.set(base + "#Chance.Value", trim(def.getEliteChance()));
            double[] w = def.getTierWeights();
            cmd.set(base + "#W1.Value", trim(w[0]));
            cmd.set(base + "#W2.Value", trim(w[1]));
            cmd.set(base + "#W3.Value", trim(w[2]));
            cmd.set(base + "#W4.Value", trim(w[3]));
            cmd.set(base + "#W5.Value", trim(w[4]));

            for (String f : new String[]{"Chance", "W1", "W2", "W3", "W4", "W5"}) {
                save.append("@Z" + z + f, base + "#" + f + ".Value");
            }
        }
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SaveZones", save);
    }

    private void buildEconomy(@Nonnull UICommandBuilder cmd) {
        cmd.clear("#EconomyList");
        int i = 0;
        i = econRow(cmd, i, "Multiplicateur XP par tier (Varyon-RPG) :");
        for (int t = 1; t <= EliteConfig.TIER_COUNT; t++) {
            i = econRow(cmd, i, String.format(Locale.ROOT, "  T%d  ×%.2f", t, config.getXpMultiplier(t)));
        }
        i = econRow(cmd, i, "Chance de Cristal du Vide par kill d'élite (drop = 1) :");
        for (int t = 1; t <= EliteConfig.TIER_COUNT; t++) {
            double chance = config.getVoidCrystalChance(t);
            if (chance <= 0) continue;
            i = econRow(cmd, i, String.format(Locale.ROOT, "  T%d  %.0f%%", t, chance * 100));
        }
        i = econRow(cmd, i, "Chance de Cœur de Corruption par kill d'élite (drop = 1) :");
        for (int t = 1; t <= EliteConfig.TIER_COUNT; t++) {
            double chance = config.getCorruptionCoreChance(t);
            if (chance <= 0) continue;
            i = econRow(cmd, i, String.format(Locale.ROOT, "  T%d  %.0f%%", t, chance * 100));
        }

        i = econRow(cmd, i, "");
        i = econRow(cmd, i, "Passif de classe : effet permanent par classe (voir MobClass).");
        i = econRow(cmd, i, String.format(Locale.ROOT, "Pause globale entre capacites : %d-%d ms",
                config.getGlobalAbilityCooldownMinMs(), config.getGlobalAbilityCooldownMaxMs()));

        i = econRow(cmd, i, "");
        i = econRow(cmd, i, "Règles de mob (" + config.getMobRules().size() + ") :");
        for (EliteConfig.MobRuleDef r : config.getMobRules()) {
            i = econRow(cmd, i, "  " + r.getKey()
                    + " → armes " + r.getAllowedWeaponFamilies()
                    + " armures " + r.getAllowedArmorMaterials());
        }

        if (!config.getWorldOverlays().isEmpty()) {
            i = econRow(cmd, i, "");
            i = econRow(cmd, i, "Overlays de monde (" + config.getWorldOverlays().size() + ") :");
            for (EliteConfig.WorldOverlay o : config.getWorldOverlays()) {
                i = econRow(cmd, i, "  " + o.getWorldPrefix() + "* - "
                        + o.zoneDefs().size() + " zone(s) surchargée(s)");
            }
        }

        econRow(cmd, i, "- éditer elite.toml pour changer ces valeurs -");
    }

    private int econRow(@Nonnull UICommandBuilder cmd, int i, @Nonnull String text) {
        cmd.append("#EconomyList", TEXT_ROW);
        cmd.set("#EconomyList[" + i + "] #RowText.Text", text);
        return i + 1;
    }

    private void applyAndSaveZones(@Nonnull EventDataClass data) {
        int changed = 0;
        for (int z = 1; z <= EliteConfig.ZONE_COUNT; z++) {
            EliteConfig.ZoneEliteDef def = config.getZoneDef(z);
            if (def == null) {
                continue;
            }
            Double chance = parse(data.zoneField(z, "Chance"));
            if (chance != null) {
                def.setEliteChance(Math.max(0.0, Math.min(1.0, chance)));
                changed++;
            }
            double[] w = def.getTierWeights();
            for (int t = 0; t < EliteConfig.TIER_COUNT; t++) {
                Double v = parse(data.zoneField(z, "W" + (t + 1)));
                if (v != null) {
                    w[t] = Math.max(0.0, v);
                    changed++;
                }
            }
        }
        try {
            config.save(dataDir);
            status = "Enregistré (" + changed + " champs) dans elite.toml.";
        } catch (Exception e) {
            status = "Échec de l'enregistrement : " + e.getMessage();
        }
    }

    private static String trim(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.valueOf((long) v);
        }
        return String.format(Locale.ROOT, "%.3f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static Double parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(raw.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static final class EventDataClass {
        public static final BuilderCodec<EventDataClass> CODEC = buildCodec();

        private static BuilderCodec<EventDataClass> buildCodec() {
            var b = BuilderCodec.builder(EventDataClass.class, EventDataClass::new);
            b.append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action).add();
            for (int z = 1; z <= EliteConfig.ZONE_COUNT; z++) {
                final int zone = z;
                for (String f : new String[]{"Chance", "W1", "W2", "W3", "W4", "W5"}) {
                    final String field = f;
                    b.append(new KeyedCodec<>("@Z" + zone + field, Codec.STRING),
                            (d, v) -> d.fields[idx(zone, field)] = v,
                            d -> d.fields[idx(zone, field)]).add();
                }
            }
            return b.build();
        }

        private static int idx(int zone, String field) {
            int f = switch (field) {
                case "Chance" -> 0;
                case "W1" -> 1;
                case "W2" -> 2;
                case "W3" -> 3;
                case "W4" -> 4;
                default -> 5;
            };
            return (zone - 1) * 6 + f;
        }

        private String action;
        private final String[] fields = new String[EliteConfig.ZONE_COUNT * 6];

        String zoneField(int zone, String field) {
            return fields[idx(zone, field)];
        }
    }
}
