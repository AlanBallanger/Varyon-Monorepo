package com.varyon.elite;

import com.hypixel.hytale.logger.HytaleLogger;
import com.moandjiezana.toml.Toml;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;

/**
 * Runtime configuration for the elite subsystem, persisted as its own {@code elite.toml}
 * (same pattern as {@code ShopConfig}). See {@code docs/ELITE_MOBS_DESIGN.md}.
 */
public class EliteConfig {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String FILENAME = "elite.toml";

    public static final int TIER_COUNT = 5;
    public static final int ZONE_COUNT = 10;

    private boolean enabled;

    /** Indexed by zone id (1..10). Each entry: spawn chance + normalised tier weights. */
    private final Map<Integer, ZoneEliteDef> zoneDefs = new LinkedHashMap<>();

    /** Class weights; missing class = weight 0. Tier gating (MobClass.allowsTier) is applied separately. */
    private final Map<MobClass, Double> classWeights = new EnumMap<>(MobClass.class);

    /** Exact canonicalised NPC role keys that may always be promoted (in addition to the fragment lists). */
    private final Set<String> whitelist = new LinkedHashSet<>();

    /** A role is eligible for promotion if it contains one of these fragments... */
    private final List<String> eligibleFragments = new ArrayList<>();
    /** ...unless it also contains one of these (passive animals, merchants, named bosses, etc.). */
    private final List<String> eligibleExcludes = new ArrayList<>();
    /** Exact canonicalised NPC role keys that wear armour AND carry a weapon when promoted (an
     *  explicit whitelist, not a fragment match - a fragment like "skeleton" covers ~190 variants,
     *  and one-off exceptions like a headless mob or an unarmed reskin kept slipping through). */
    private final Set<String> humanoidRoles = new LinkedHashSet<>();

    /** XP multiplier applied by Varyon-RPG per elite tier (index 0 unused). */
    private final double[] xpMultiplierByTier = new double[TIER_COUNT + 1];

    /** Chance (0..1) an elite kill drops exactly 1 Cristal du Vide, by tier. */
    private final double[] voidCrystalChanceByTier = new double[TIER_COUNT + 1];

    /** Chance (0..1) an elite kill drops exactly 1 Coeur de Corruption, by tier. */
    private final double[] corruptionCoreChanceByTier = new double[TIER_COUNT + 1];

    /** seed -> named elite definition. */
    private final Map<Long, NamedElite> namedElites = new LinkedHashMap<>();

    /** Per-tier randomised stat ranges (HP mult, outgoing-damage mult, model size). Index 1..5. */
    private final ScalingDef[] scalingByTier = new ScalingDef[TIER_COUNT + 1];

    /** Random global pause after any ability completes, before the next may start (ms). */
    private long globalAbilityCooldownMinMs = 2000;
    private long globalAbilityCooldownMaxMs = 4000;

    /** Ordered mob rules - first match wins; decides eligibility + gear constraints. */
    private final List<MobRuleDef> mobRules = new ArrayList<>();

    /** World-name-prefix overlays: override zone defs / class weights for matching worlds. */
    private final List<WorldOverlay> worldOverlays = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public long getGlobalAbilityCooldownMinMs() {
        return globalAbilityCooldownMinMs;
    }

    public long getGlobalAbilityCooldownMaxMs() {
        return globalAbilityCooldownMaxMs;
    }

    /** Rolls a global ability cooldown in the configured window. */
    public long rollGlobalAbilityCooldownMs(@Nonnull java.util.Random rng) {
        long lo = Math.min(globalAbilityCooldownMinMs, globalAbilityCooldownMaxMs);
        long hi = Math.max(globalAbilityCooldownMinMs, globalAbilityCooldownMaxMs);
        return lo == hi ? lo : lo + (long) (rng.nextDouble() * (hi - lo));
    }

    @Nonnull
    public List<MobRuleDef> getMobRules() {
        return mobRules;
    }

    @Nonnull
    public List<WorldOverlay> getWorldOverlays() {
        return worldOverlays;
    }

    /** First mob rule matching {@code canonicalRole}, or {@code null} (falls back to whitelist). */
    @Nullable
    public MobRuleDef matchRule(@Nonnull String canonicalRole) {
        for (MobRuleDef r : mobRules) {
            if (r.matches(canonicalRole)) {
                return r;
            }
        }
        return null;
    }

    /** Zone def for a world: overlay's if any world overlay matches, else the global one. */
    @Nullable
    public ZoneEliteDef getZoneDef(int zoneId, @Nullable String worldName) {
        if (worldName != null) {
            for (WorldOverlay o : worldOverlays) {
                if (o.matches(worldName)) {
                    ZoneEliteDef d = o.zoneDefs.get(zoneId);
                    if (d != null) {
                        return d;
                    }
                }
            }
        }
        return zoneDefs.get(zoneId);
    }

    /** Class weights for a world: an overlay's (any of its zones) if one matches and is non-empty, else the global table. */
    @Nonnull
    public Map<MobClass, Double> getClassWeights(@Nullable String worldName) {
        if (worldName != null) {
            for (WorldOverlay o : worldOverlays) {
                if (o.matches(worldName) && !o.classWeights.isEmpty()) {
                    for (Map<MobClass, Double> w : o.classWeights.values()) {
                        if (w != null && !w.isEmpty()) {
                            return w;
                        }
                    }
                }
            }
        }
        return getClassWeights();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Copies every field from {@code other} in place, so systems holding this instance see the reload. */
    public void copyFrom(@Nonnull EliteConfig other) {
        this.enabled = other.enabled;
        this.zoneDefs.clear();
        this.zoneDefs.putAll(other.zoneDefs);
        this.classWeights.clear();
        this.classWeights.putAll(other.classWeights);
        this.whitelist.clear();
        this.whitelist.addAll(other.whitelist);
        this.eligibleFragments.clear();
        this.eligibleFragments.addAll(other.eligibleFragments);
        this.eligibleExcludes.clear();
        this.eligibleExcludes.addAll(other.eligibleExcludes);
        this.humanoidRoles.clear();
        this.humanoidRoles.addAll(other.humanoidRoles);
        System.arraycopy(other.xpMultiplierByTier, 0, this.xpMultiplierByTier, 0, xpMultiplierByTier.length);
        System.arraycopy(other.voidCrystalChanceByTier, 0, this.voidCrystalChanceByTier, 0, voidCrystalChanceByTier.length);
        System.arraycopy(other.corruptionCoreChanceByTier, 0, this.corruptionCoreChanceByTier, 0, corruptionCoreChanceByTier.length);
        this.namedElites.clear();
        this.namedElites.putAll(other.namedElites);
        System.arraycopy(other.scalingByTier, 0, this.scalingByTier, 0, scalingByTier.length);
        this.globalAbilityCooldownMinMs = other.globalAbilityCooldownMinMs;
        this.globalAbilityCooldownMaxMs = other.globalAbilityCooldownMaxMs;
        this.mobRules.clear();
        this.mobRules.addAll(other.mobRules);
        this.worldOverlays.clear();
        this.worldOverlays.addAll(other.worldOverlays);
    }

    @Nullable
    public ZoneEliteDef getZoneDef(int zoneId) {
        return zoneDefs.get(zoneId);
    }

    public void putZoneDef(int zoneId, @Nonnull ZoneEliteDef def) {
        zoneDefs.put(zoneId, def);
    }

    @Nonnull
    public Map<MobClass, Double> getClassWeights() {
        return classWeights;
    }

    public void putClassWeights(@Nonnull Map<MobClass, Double> weights) {
        classWeights.clear();
        classWeights.putAll(weights);
    }

    public boolean isWhitelisted(@Nonnull String canonicalRole) {
        return whitelist.contains(canonicalRole.toLowerCase(Locale.ROOT));
    }

    @Nonnull
    public Set<String> getWhitelist() {
        return whitelist;
    }

    @Nonnull public List<String> getEligibleFragments() { return eligibleFragments; }
    @Nonnull public List<String> getEligibleExcludes() { return eligibleExcludes; }
    @Nonnull public Set<String> getHumanoidRoles() { return humanoidRoles; }

    @Nonnull
    public ScalingDef getScaling(int tier) {
        int t = Math.max(1, Math.min(TIER_COUNT, tier));
        ScalingDef d = scalingByTier[t];
        return d != null ? d : ScalingDef.defaultForTier(t);
    }

    public void putScaling(int tier, @Nonnull ScalingDef def) {
        if (tier >= 1 && tier <= TIER_COUNT) {
            scalingByTier[tier] = def;
        }
    }

    /** True if {@code canonicalRole} may be promoted to an elite (fragment match, exclusions applied). */
    public boolean isEligible(@Nonnull String canonicalRole) {
        String r = canonicalRole.toLowerCase(Locale.ROOT);
        if (whitelist.contains(r)) {
            return true;
        }
        for (String ex : eligibleExcludes) {
            if (!ex.isBlank() && r.contains(ex)) {
                return false;
            }
        }
        for (String frag : eligibleFragments) {
            if (!frag.isBlank() && r.contains(frag)) {
                return true;
            }
        }
        return false;
    }

    /** True if an elite of this role should be given armour (humanoid body). Exact-role whitelist. */
    public boolean isHumanoid(@Nonnull String canonicalRole) {
        return humanoidRoles.contains(canonicalRole.toLowerCase(Locale.ROOT));
    }

    public double getXpMultiplier(int tier) {
        if (tier < 1 || tier > TIER_COUNT) return 1.0;
        return xpMultiplierByTier[tier];
    }

    /** Chance (0..1) that an elite kill of this tier drops exactly 1 Cristal du Vide. */
    public double getVoidCrystalChance(int tier) {
        if (tier < 1 || tier > TIER_COUNT) return 0;
        return voidCrystalChanceByTier[tier];
    }

    /** Chance (0..1) that an elite kill of this tier drops exactly 1 Coeur de Corruption. */
    public double getCorruptionCoreChance(int tier) {
        if (tier < 1 || tier > TIER_COUNT) return 0;
        return corruptionCoreChanceByTier[tier];
    }

    @Nullable
    public NamedElite getNamedElite(long seed) {
        return namedElites.get(seed);
    }

    @Nonnull
    public Map<Long, NamedElite> getNamedElites() {
        return namedElites;
    }

    public static EliteConfig createDefault() {
        EliteConfig cfg = new EliteConfig();
        cfg.enabled = true;

        // Progressive curve: zone 1 already has a small T3 presence (mostly T1, some T2, a
        // little T3), T4 appears from zone 2 on, T5 appears from zone 4 on. Every following zone
        // shifts weight upward a bit more - no zone repeats a neighbour's value on any tier, so
        // the curve keeps moving all the way to zone 10 instead of flattening out.
        double[] chance = {0, 0.10, 0.14, 0.18, 0.22, 0.27, 0.32, 0.37, 0.42, 0.46, 0.50};
        double[][] tierWeights = {
                {},
                {70.0, 25.0, 5.0,  0.0,  0.0},
                {55.0, 33.0, 11.0, 1.0,  0.0},
                {42.0, 36.0, 18.0, 4.0,  0.0},
                {32.0, 35.0, 23.0, 9.0,  1.0},
                {22.0, 32.0, 27.0, 16.0, 3.0},
                {14.0, 27.0, 30.0, 22.0, 7.0},
                {8.0,  21.0, 30.0, 28.0, 13.0},
                {4.5,  15.0, 28.0, 33.0, 19.5},
                {2.5,  10.0, 24.5, 37.0, 26.0},
                {1.0,  5.0,  20.0, 40.0, 34.0},
        };
        for (int zone = 1; zone <= ZONE_COUNT; zone++) {
            cfg.zoneDefs.put(zone, new ZoneEliteDef(chance[zone], tierWeights[zone].clone()));
        }

        // Same class distribution everywhere. BOSS only rolls once a T4+ elite is already picked
        // (MobClass.allowsTier gates it before this weight is even considered), so one flat weight
        // is enough - no need to duplicate this table per zone.
        cfg.classWeights.put(MobClass.BAGARREUR, 1.0);
        cfg.classWeights.put(MobClass.BERSERKER, 1.0);
        cfg.classWeights.put(MobClass.ASSASSIN, 1.0);
        cfg.classWeights.put(MobClass.ARCHER, 1.0);
        cfg.classWeights.put(MobClass.SEIGNEUR_DE_GUERRE, 0.6);
        cfg.classWeights.put(MobClass.SOIGNEUR, 0.5);
        cfg.classWeights.put(MobClass.BOSS, 0.10);

        // Per-tier randomised scaling. hp/dmg are multipliers layered on top of Varyon zone
        // scaling; size is a model scale left at 1.0 by default (enable at your own risk -
        // large models can desync hitboxes / snag pathfinding).
        for (int t = 1; t <= TIER_COUNT; t++) {
            cfg.scalingByTier[t] = ScalingDef.defaultForTier(t);
        }

        cfg.eligibleFragments.addAll(defaultEligibleFragments());
        cfg.eligibleExcludes.addAll(defaultEligibleExcludes());
        cfg.humanoidRoles.addAll(defaultHumanoidRoles());

        cfg.whitelist.addAll(Arrays.asList("cave_dweller"));

        double[] xpMult = {0, 1.5, 2.0, 3.0, 4.5, 7.0};
        System.arraycopy(xpMult, 0, cfg.xpMultiplierByTier, 0, xpMult.length);

        // Cristal du Vide: tiers 1-4 only, drops exactly 1 when the roll hits.
        double[] crystalChance = {0, 0.05, 0.10, 0.25, 0.10, 0.0};
        System.arraycopy(crystalChance, 0, cfg.voidCrystalChanceByTier, 0, crystalChance.length);

        // Coeur de Corruption: tiers 3-5 only, drops exactly 1 when the roll hits.
        double[] coreChance = {0, 0.0, 0.0, 0.02, 0.05, 0.15};
        System.arraycopy(coreChance, 0, cfg.corruptionCoreChanceByTier, 0, coreChance.length);

        cfg.globalAbilityCooldownMinMs = 2000;
        cfg.globalAbilityCooldownMaxMs = 4000;

        // Optional finer gear constraints per mob family. A rule only narrows weapon/armour
        // choices - eligibility itself comes from the fragment lists above. Trork melee has no
        // rule here: it just uses the elite's class weapon + the normal per-tier armour progression.
        // armor_slots: -1 = no override (tier-based piece count), 0 = no armour at all, >0 = forced count.
        cfg.mobRules.add(new MobRuleDef("archers", List.of(), List.of(), List.of("archer", "ranger", "marksman", "gunner"), List.of(),
                List.of("Weapon_Shortbow_", "Weapon_Crossbow_"), List.of(), -1));
        cfg.mobRules.add(new MobRuleDef("mages", List.of(), List.of(), List.of("mage", "archmage", "shaman", "wizard", "sorcerer", "alchemist"), List.of(),
                List.of("Weapon_Staff_"), List.of("Cloth"), -1));
        // skeleton_burnt_praetorian is in defaultHumanoidRoles() (so isHumanoid() is true and it
        // gets a weapon), but this rule forces armor_slots = 0 so it never gets armour.
        cfg.mobRules.add(new MobRuleDef("praetorian", List.of(), List.of(), List.of("praetorian"), List.of(),
                List.of(), List.of(), 0));

        return cfg;
    }

    /**
     * Built-in eligibility fragments. Almost every hostile creature can become an elite;
     * matched by name fragment so new variants (skeleton_frost_*, dungeon_scarak_*,
     * endgame_*, ...) are covered automatically.
     */
    @Nonnull
    public static List<String> defaultEligibleFragments() {
        return List.of(
                // humanoid factions
                "skeleton", "trork", "goblin", "outlander", "scarak", "klops", "bramblekin",
                "grung", "saurian", "slothian", "human_bandit", "human_pirate", "outlaw",
                "bandit", "raider", "kweebec_razorleaf",
                "arcanepower_skeleton", "elite_skeleton",
                // beasts / monsters
                "spider", "golem", "wraith", "ghoul", "zombie", "imp", "wolf", "hyena",
                "bear", "tiger", "leopard", "panther", "raptor", "rex", "trillodon",
                "snapdragon", "yeti", "emberwulf", "werewolf", "broodmother", "megalodon",
                "crocodile", "scorpion", "slime", "toad_rhino", "toad_minion", "toad_frost",
                "lizard_sand", "mosshorn", "armadillo", "bigfoot", "kamikaze", "undead",
                "hound_bleached", "frostgill", "snapjaw", "cow_undead", "boar", "warthog",
                "moose", "wolf", "hyena", "sabertooth",
                // fire family (golem_firesteel already via "golem", wraith already via "wraith",
                // toad_rhino_magma already via "toad_rhino")
                "slug_magma", "snail_magma", "spark_living",
                // void family (larva_void, crawler_void, eye_void, spectre_void, spawn_void,
                // spirit_ember/frost/root/thunder, fen_stalker, endgame_fen_stalker)
                "crawler_void", "larva_void", "eye_void", "spectre_void", "spawn_void",
                "spirit_ember", "spirit_frost", "spirit_root", "spirit_thunder",
                "fen_stalker", "wisp", "phantom",
                // misc isolated mobs not covered by any family fragment above
                "feran_beastmaster", "undead_antelope");
    }

    @Nonnull
    public static List<String> defaultEligibleExcludes() {
        return List.of(
                "merchant", "trader", "prisoner", "civilian", "elder", "gentleman", "king",
                "fisherman", "hermit", "duke", "chief", "chieftain", "warlord", "queen_boss",
                "broodmother_young", "guardian_void", "_boss", "dragon", "champion", "tutorial",
                "unarmed");
    }

    /**
     * Humanoid = wears armour AND carries a weapon. Everything else gets neither (see docs).
     * An explicit whitelist of exact NPC roles, not a fragment match: a fragment like "skeleton"
     * matches ~190 role variants at once, so one-off exceptions (a headless mob, an unarmed
     * reskin, a mounted horse) kept slipping through as false positives. Built from the real
     * server NPC role assets, not from a fragment guess.
     */
    @Nonnull
    public static List<String> defaultHumanoidRoles() {
        return List.of(
                "bramblekin", "bramblekin_shaman", "dungeon_scarak_broodmother", "dungeon_scarak_defender", "dungeon_scarak_defender_patrol",
                "dungeon_scarak_fighter", "dungeon_scarak_fighter_patrol", "dungeon_scarak_louse", "dungeon_scarak_seeker", "dungeon_scarak_seeker_patrol",
                "dungeon_skeleton_sand_archer", "dungeon_skeleton_sand_assassin", "dungeon_skeleton_sand_mage", "dungeon_skeleton_sand_soldier", "goblin_lobber",
                "goblin_lobber_patrol", "goblin_miner", "goblin_miner_patrol", "goblin_ogre", "goblin_scavenger",
                "goblin_scavenger_battleaxe", "goblin_scavenger_sword", "goblin_scrapper", "goblin_scrapper_patrol", "goblin_thief",
                "goblin_thief_patrol", "klops_miner", "klops_miner_patrol", "kweebec_razorleaf", "kweebec_razorleaf_patrol",
                "outlander_berserker", "outlander_brute", "outlander_cultist", "outlander_hunter", "outlander_marauder",
                "outlander_peon", "outlander_priest", "outlander_sorcerer", "outlander_stalker", "risen_gunner",
                "risen_knight", "scarak_broodmother", "scarak_defender", "scarak_defender_patrol", "scarak_fighter",
                "scarak_fighter_patrol", "scarak_fighter_royal_guard", "scarak_louse", "scarak_seeker", "scarak_seeker_patrol",
                "skeleton", "skeleton_archer", "skeleton_archer_patrol", "skeleton_archer_wander", "skeleton_archmage",
                "skeleton_archmage_patrol", "skeleton_archmage_wander", "skeleton_burnt_alchemist", "skeleton_burnt_alchemist_patrol", "skeleton_burnt_alchemist_wander",
                "skeleton_burnt_archer", "skeleton_burnt_archer_patrol", "skeleton_burnt_archer_wander", "skeleton_burnt_gunner", "skeleton_burnt_gunner_patrol",
                "skeleton_burnt_gunner_wander", "skeleton_burnt_knight", "skeleton_burnt_knight_patrol", "skeleton_burnt_knight_wander", "skeleton_burnt_lancer",
                "skeleton_burnt_lancer_patrol", "skeleton_burnt_lancer_wander", "skeleton_burnt_praetorian", "skeleton_burnt_praetorian_patrol", "skeleton_burnt_praetorian_wander",
                "skeleton_burnt_soldier", "skeleton_burnt_soldier_patrol", "skeleton_burnt_soldier_wander",
                "skeleton_burnt_wizard", "skeleton_burnt_wizard_patrol", "skeleton_burnt_wizard_wander", "skeleton_fighter", "skeleton_fighter_patrol",
                "skeleton_fighter_wander", "skeleton_frost_archer", "skeleton_frost_archer_patrol", "skeleton_frost_archer_wander", "skeleton_frost_archmage",
                "skeleton_frost_archmage_patrol", "skeleton_frost_archmage_wander", "skeleton_frost_fighter", "skeleton_frost_fighter_patrol", "skeleton_frost_fighter_wander",
                "skeleton_frost_knight", "skeleton_frost_knight_patrol", "skeleton_frost_knight_wander", "skeleton_frost_mage", "skeleton_frost_mage_patrol",
                "skeleton_frost_mage_wander", "skeleton_frost_ranger", "skeleton_frost_ranger_patrol", "skeleton_frost_ranger_wander", "skeleton_frost_scout",
                "skeleton_frost_scout_patrol", "skeleton_frost_scout_wander", "skeleton_frost_soldier", "skeleton_frost_soldier_patrol", "skeleton_frost_soldier_wander",
                "skeleton_incandescent_fighter", "skeleton_incandescent_fighter_patrol", "skeleton_incandescent_fighter_wander", "skeleton_incandescent_footman", "skeleton_incandescent_footman_patrol",
                "skeleton_incandescent_footman_wander", "skeleton_incandescent_mage", "skeleton_incandescent_mage_patrol", "skeleton_incandescent_mage_wander", "skeleton_knight",
                "skeleton_knight_patrol", "skeleton_knight_wander", "skeleton_mage", "skeleton_mage_patrol", "skeleton_mage_wander",
                "skeleton_pirate_captain", "skeleton_pirate_captain_patrol", "skeleton_pirate_captain_wander", "skeleton_pirate_gunner", "skeleton_pirate_gunner_patrol",
                "skeleton_pirate_gunner_wander", "skeleton_pirate_striker", "skeleton_pirate_striker_patrol", "skeleton_pirate_striker_wander", "skeleton_ranger",
                "skeleton_ranger_patrol", "skeleton_ranger_wander", "skeleton_sand_archer", "skeleton_sand_archer_patrol", "skeleton_sand_archer_wander",
                "skeleton_sand_archmage", "skeleton_sand_archmage_patrol", "skeleton_sand_archmage_wander", "skeleton_sand_assassin", "skeleton_sand_assassin_patrol",
                "skeleton_sand_assassin_wander", "skeleton_sand_guard", "skeleton_sand_guard_patrol", "skeleton_sand_guard_wander", "skeleton_sand_mage",
                "skeleton_sand_mage_patrol", "skeleton_sand_mage_wander", "skeleton_sand_ranger", "skeleton_sand_ranger_patrol", "skeleton_sand_ranger_wander",
                "skeleton_sand_scout", "skeleton_sand_scout_patrol", "skeleton_sand_scout_wander", "skeleton_sand_soldier", "skeleton_sand_soldier_patrol",
                "skeleton_sand_soldier_wander", "skeleton_scout", "skeleton_scout_patrol", "skeleton_scout_wander", "skeleton_soldier",
                "skeleton_soldier_patrol", "skeleton_soldier_wander", "slothian", "spectre_void", "temple_klops",
                "temple_kweebec_razorleaf", "temple_kweebec_razorleaf_patrol", "temple_kweebec_razorleaf_patrol1", "temple_kweebec_razorleaf_patrol2", "temple_kweebec_razorleaf_patrol3",
                "temple_kweebec_razorleaf_patrol4", "temple_kweebec_razorleaf_patrol5", "trork_brawler", "trork_doctor_witch", "trork_guard",
                "trork_hunter", "trork_mauler", "trork_sentry", "trork_sentry_patrol", "trork_shaman",
                "trork_warrior", "trork_warrior_patrol", "zombie", "zombie_burnt", "zombie_frost",
                "zombie_sand"
        );
    }

    public static EliteConfig load(@Nonnull Path dataFolder) {
        File file = dataFolder.resolve(FILENAME).toFile();
        if (!file.exists()) {
            EliteConfig def = createDefault();
            def.save(dataFolder);
            return def;
        }
        try {
            Toml toml = new Toml().read(file);
            EliteConfig cfg = new EliteConfig();
            cfg.enabled = toml.getBoolean("enabled", Boolean.TRUE);

            List<Toml> zoneTables = toml.getTables("zone");
            if (zoneTables != null) {
                for (Toml t : zoneTables) {
                    int id = t.getLong("id", 0L).intValue();
                    if (id < 1 || id > ZONE_COUNT) continue;
                    double chance = t.getDouble("elite_chance", 0.0);
                    List<Number> raw = t.getList("tier_weights");
                    double[] weights = new double[TIER_COUNT];
                    if (raw != null) {
                        for (int i = 0; i < Math.min(TIER_COUNT, raw.size()); i++) {
                            weights[i] = raw.get(i).doubleValue();
                        }
                    }
                    cfg.zoneDefs.put(id, new ZoneEliteDef(chance, weights));
                }
            }

            Toml classWeightsTable = toml.getTable("class_weights");
            if (classWeightsTable != null) {
                for (MobClass mc : MobClass.values()) {
                    Double v = classWeightsTable.getDouble(mc.name(), null);
                    if (v != null) cfg.classWeights.put(mc, v);
                }
            }

            List<String> wl = toml.getList("whitelist");
            if (wl != null) {
                for (String s : wl) cfg.whitelist.add(EliteRoles.canonical(s));
            }
            // Fall back to the built-in defaults when a list is absent from the file, so an
            // older elite.toml still picks up newly added eligible families (wolf, void, ...).
            List<String> ef = lowerList(toml, "eligible_fragments");
            cfg.eligibleFragments.addAll(ef.isEmpty() ? defaultEligibleFragments() : ef);
            List<String> ex = lowerList(toml, "eligible_excludes");
            cfg.eligibleExcludes.addAll(ex.isEmpty() ? defaultEligibleExcludes() : ex);
            // humanoid_roles replaced the old fragment-based humanoid_fragments (a fragment like
            // "skeleton" matched ~190 variants at once and let one-off exceptions slip through).
            // An elite.toml still carrying the old key is ignored here - it can no longer be
            // interpreted as an exact-role whitelist - and falls back to the new default roles.
            List<String> hr = lowerList(toml, "humanoid_roles");
            cfg.humanoidRoles.addAll(hr.isEmpty() ? defaultHumanoidRoles() : hr);

            Toml xp = toml.getTable("xp_multiplier");
            if (xp != null) {
                for (int t = 1; t <= TIER_COUNT; t++) {
                    cfg.xpMultiplierByTier[t] = xp.getDouble("t" + t, 1.0);
                }
            }
            Toml crystalChance = toml.getTable("void_crystal_chance");
            if (crystalChance != null) {
                for (int t = 1; t <= TIER_COUNT; t++) {
                    cfg.voidCrystalChanceByTier[t] = crystalChance.getDouble("t" + t, 0.0);
                }
            }
            Toml coreChance = toml.getTable("corruption_core_chance");
            if (coreChance != null) {
                for (int t = 1; t <= TIER_COUNT; t++) {
                    cfg.corruptionCoreChanceByTier[t] = coreChance.getDouble("t" + t, 0.0);
                }
            }

            List<Toml> namedTables = toml.getTables("named");
            if (namedTables != null) {
                for (Toml t : namedTables) {
                    long seed = t.getLong("seed", 0L);
                    String id = t.getString("id", null);
                    String display = t.getString("display_name", id);
                    String clazz = t.getString("class", "BAGARREUR");
                    int tier = t.getLong("tier", 1L).intValue();
                    double bonus = t.getDouble("stat_bonus", 0.0);
                    if (id == null) continue;
                    try {
                        cfg.namedElites.put(seed,
                                new NamedElite(id, display, MobClass.valueOf(clazz), tier, bonus));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            Toml ability = toml.getTable("ability");
            if (ability != null) {
                cfg.globalAbilityCooldownMinMs = ability.getLong("global_cooldown_min_ms", 2000L);
                cfg.globalAbilityCooldownMaxMs = ability.getLong("global_cooldown_max_ms", 4000L);
            }

            List<Toml> scalingTables = toml.getTables("scaling");
            if (scalingTables != null) {
                for (Toml t : scalingTables) {
                    int tier = t.getLong("tier", 0L).intValue();
                    if (tier < 1 || tier > TIER_COUNT) continue;
                    ScalingDef prev = cfg.getScaling(tier);
                    cfg.scalingByTier[tier] = new ScalingDef(
                            t.getDouble("hp_min", (double) prev.getHpMin()).floatValue(),
                            t.getDouble("hp_max", (double) prev.getHpMax()).floatValue(),
                            t.getDouble("dmg_min", (double) prev.getDmgMin()).floatValue(),
                            t.getDouble("dmg_max", (double) prev.getDmgMax()).floatValue(),
                            t.getDouble("size_min", (double) prev.getSizeMin()).floatValue(),
                            t.getDouble("size_max", (double) prev.getSizeMax()).floatValue());
                }
            }


            List<Toml> ruleTables = toml.getTables("mob_rule");
            if (ruleTables != null) {
                for (Toml t : ruleTables) {
                    cfg.mobRules.add(new MobRuleDef(
                            t.getString("key", "rule"),
                            strList(t, "match_exact"),
                            strList(t, "match_starts_with"),
                            strList(t, "match_contains"),
                            strList(t, "match_excludes"),
                            strList(t, "allowed_weapon_families"),
                            strList(t, "allowed_armor_materials"),
                            t.getLong("armor_slots", -1L).intValue()));
                }
            }

            List<Toml> overlayTables = toml.getTables("world_overlay");
            if (overlayTables != null) {
                for (Toml t : overlayTables) {
                    String prefix = t.getString("world_prefix", null);
                    if (prefix == null || prefix.isBlank()) continue;
                    WorldOverlay o = new WorldOverlay(prefix);
                    List<Toml> ozones = t.getTables("zone");
                    if (ozones != null) {
                        for (Toml zt : ozones) {
                            int id = zt.getLong("id", 0L).intValue();
                            if (id < 1 || id > ZONE_COUNT) continue;
                            List<Number> raw = zt.getList("tier_weights");
                            double[] w = new double[TIER_COUNT];
                            if (raw != null) {
                                for (int i = 0; i < Math.min(TIER_COUNT, raw.size()); i++) {
                                    w[i] = raw.get(i).doubleValue();
                                }
                            }
                            o.zoneDefs.put(id, new ZoneEliteDef(zt.getDouble("elite_chance", 0.0), w));
                            Toml cw = zt.getTable("class_weights");
                            if (cw != null) {
                                Map<MobClass, Double> m = new EnumMap<>(MobClass.class);
                                for (MobClass mc : MobClass.values()) {
                                    Double v = cw.getDouble(mc.name(), null);
                                    if (v != null) m.put(mc, v);
                                }
                                o.classWeights.put(id, m);
                            }
                        }
                    }
                    cfg.worldOverlays.add(o);
                }
            }

            LOGGER.at(Level.INFO).log("Loaded %s: %s zones, %s roles, %s named, %s rules, %s overlays",
                    FILENAME, cfg.zoneDefs.size(), cfg.whitelist.size(), cfg.namedElites.size(),
                    cfg.mobRules.size(), cfg.worldOverlays.size());
            return cfg;
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to load " + FILENAME + ", rewriting defaults");
            EliteConfig def = createDefault();
            def.save(dataFolder);
            return def;
        }
    }

    public void save(@Nonnull Path dataFolder) {
        File file = dataFolder.resolve(FILENAME).toFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try {
            String toml = generateToml();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(toml);
            }
            LOGGER.at(Level.INFO).log("Wrote %s", FILENAME);
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to save " + FILENAME);
        }
    }

    @Nonnull
    private String generateToml() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Varyon EliteMobs - see docs/ELITE_MOBS_DESIGN.md\n");
        sb.append("enabled = ").append(enabled).append("\n\n");

        sb.append("# An NPC role can become elite if its name contains one 'eligible_fragment'\n");
        sb.append("# and no 'eligible_exclude'. 'whitelist' forces exact roles in addition.\n");
        sb.append("# 'humanoid_roles': exact role whitelist - these elites wear armour AND carry a\n");
        sb.append("# weapon (others get neither). Not a fragment match - list every role by its exact name.\n");
        appendStrArray(sb, "whitelist", new ArrayList<>(whitelist));
        appendStrArray(sb, "eligible_fragments", eligibleFragments);
        appendStrArray(sb, "eligible_excludes", eligibleExcludes);
        appendStrArray(sb, "humanoid_roles", new ArrayList<>(humanoidRoles));
        sb.append("\n");

        sb.append("[xp_multiplier]\n");
        for (int t = 1; t <= TIER_COUNT; t++) {
            sb.append("t").append(t).append(" = ").append(xpMultiplierByTier[t]).append("\n");
        }
        sb.append("\n# Chance (0..1) that an elite kill drops exactly 1 Cristal du Vide, by tier.\n");
        sb.append("[void_crystal_chance]\n");
        for (int t = 1; t <= TIER_COUNT; t++) {
            sb.append("t").append(t).append(" = ").append(voidCrystalChanceByTier[t]).append("\n");
        }
        sb.append("\n# Chance (0..1) that an elite kill drops exactly 1 Coeur de Corruption, by tier.\n");
        sb.append("[corruption_core_chance]\n");
        for (int t = 1; t <= TIER_COUNT; t++) {
            sb.append("t").append(t).append(" = ").append(corruptionCoreChanceByTier[t]).append("\n");
        }
        sb.append("\n");

        // Class weights: same distribution for every zone, so this is one flat table rather than
        // a duplicated block per zone. BOSS only ever rolls once a T4+ elite is already picked
        // (see MobClass.allowsTier), so its weight only matters relative to the other classes then.
        if (!classWeights.isEmpty()) {
            sb.append("[class_weights]\n");
            for (Map.Entry<MobClass, Double> w : classWeights.entrySet()) {
                sb.append(w.getKey().name()).append(" = ").append(w.getValue()).append("\n");
            }
            sb.append("\n");
        }

        for (int zone = 1; zone <= ZONE_COUNT; zone++) {
            ZoneEliteDef def = zoneDefs.get(zone);
            if (def == null) continue;
            sb.append("[[zone]]\n");
            sb.append("id = ").append(zone).append("\n");
            sb.append("elite_chance = ").append(def.getEliteChance()).append("\n");
            sb.append("tier_weights = [");
            double[] tw = def.getTierWeights();
            for (int i = 0; i < tw.length; i++) {
                sb.append(tw[i]);
                if (i < tw.length - 1) sb.append(", ");
            }
            sb.append("]\n");
            sb.append("\n");
        }

        for (Map.Entry<Long, NamedElite> e : namedElites.entrySet()) {
            NamedElite n = e.getValue();
            sb.append("[[named]]\n");
            sb.append("seed = ").append(e.getKey()).append("\n");
            sb.append("id = \"").append(n.getId()).append("\"\n");
            sb.append("display_name = \"").append(n.getDisplayName()).append("\"\n");
            sb.append("class = \"").append(n.getMobClass().name()).append("\"\n");
            sb.append("tier = ").append(n.getTier()).append("\n");
            sb.append("stat_bonus = ").append(n.getStatBonus()).append("\n\n");
        }

        sb.append("# Random pause (ms) after any ability completes, before the next may start.\n");
        sb.append("[ability]\n");
        sb.append("global_cooldown_min_ms = ").append(globalAbilityCooldownMinMs).append("\n");
        sb.append("global_cooldown_max_ms = ").append(globalAbilityCooldownMaxMs).append("\n\n");

        sb.append("# Random per-tier scaling. hp/dmg are multipliers layered on top of the zone scaling.\n");
        sb.append("# size = model scale (1.0 = normal). Enlarging can break the hitbox/pathfinding.\n");
        for (int t = 1; t <= TIER_COUNT; t++) {
            ScalingDef s = getScaling(t);
            sb.append("[[scaling]]\n");
            sb.append("tier = ").append(t).append("\n");
            sb.append("hp_min = ").append(s.getHpMin()).append("\n");
            sb.append("hp_max = ").append(s.getHpMax()).append("\n");
            sb.append("dmg_min = ").append(s.getDmgMin()).append("\n");
            sb.append("dmg_max = ").append(s.getDmgMax()).append("\n");
            sb.append("size_min = ").append(s.getSizeMin()).append("\n");
            sb.append("size_max = ").append(s.getSizeMax()).append("\n\n");
        }


        sb.append("# Mob rules: match by role fragments, then constrain the rolled gear.\n");
        for (MobRuleDef r : mobRules) {
            sb.append("[[mob_rule]]\n");
            sb.append("key = \"").append(r.getKey()).append("\"\n");
            appendStrArray(sb, "match_exact", r.getMatchExact());
            appendStrArray(sb, "match_starts_with", r.getMatchStartsWith());
            appendStrArray(sb, "match_contains", r.getMatchContains());
            appendStrArray(sb, "match_excludes", r.getMatchExcludes());
            appendStrArray(sb, "allowed_weapon_families", r.getAllowedWeaponFamilies());
            appendStrArray(sb, "allowed_armor_materials", r.getAllowedArmorMaterials());
            sb.append("armor_slots = ").append(r.getArmorSlotOverride()).append("\n\n");
        }

        for (WorldOverlay o : worldOverlays) {
            sb.append("[[world_overlay]]\n");
            sb.append("world_prefix = \"").append(o.getWorldPrefix()).append("\"\n");
            for (Map.Entry<Integer, ZoneEliteDef> ze : new TreeMap<>(o.zoneDefs).entrySet()) {
                ZoneEliteDef d = ze.getValue();
                sb.append("[[world_overlay.zone]]\n");
                sb.append("id = ").append(ze.getKey()).append("\n");
                sb.append("elite_chance = ").append(d.getEliteChance()).append("\n");
                sb.append("tier_weights = [");
                double[] tw = d.getTierWeights();
                for (int i = 0; i < tw.length; i++) {
                    sb.append(tw[i]);
                    if (i < tw.length - 1) sb.append(", ");
                }
                sb.append("]\n");
                Map<MobClass, Double> cw = o.classWeights.get(ze.getKey());
                if (cw != null && !cw.isEmpty()) {
                    sb.append("[world_overlay.zone.class_weights]\n");
                    for (Map.Entry<MobClass, Double> w : cw.entrySet()) {
                        sb.append(w.getKey().name()).append(" = ").append(w.getValue()).append("\n");
                    }
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private static void appendStrArray(@Nonnull StringBuilder sb, @Nonnull String key, @Nonnull List<String> values) {
        sb.append(key).append(" = [");
        for (int i = 0; i < values.size(); i++) {
            sb.append("\"").append(values.get(i)).append("\"");
            if (i < values.size() - 1) sb.append(", ");
        }
        sb.append("]\n");
    }

    @Nonnull
    private static List<String> strList(@Nonnull Toml t, @Nonnull String key) {
        List<String> raw = t.getList(key);
        return raw != null ? List.copyOf(raw) : List.of();
    }

    @Nonnull
    private static List<String> lowerList(@Nonnull Toml t, @Nonnull String key) {
        List<String> raw = t.getList(key);
        if (raw == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>(raw.size());
        for (String s : raw) {
            if (s != null && !s.isBlank()) {
                out.add(s.trim().toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }

    public static final class ZoneEliteDef {
        private double eliteChance;
        private final double[] tierWeights;

        public ZoneEliteDef(double eliteChance, @Nonnull double[] tierWeights) {
            this.eliteChance = eliteChance;
            this.tierWeights = tierWeights.length == TIER_COUNT ? tierWeights : new double[TIER_COUNT];
        }

        public double getEliteChance() {
            return eliteChance;
        }

        public void setEliteChance(double eliteChance) {
            this.eliteChance = eliteChance;
        }

        @Nonnull
        public double[] getTierWeights() {
            return tierWeights;
        }

        public double totalTierWeight() {
            double total = 0;
            for (double w : tierWeights) total += w;
            return total;
        }
    }

    /** Per-tier randomised stat ranges. A concrete value is rolled per elite from its seed. */
    public static final class ScalingDef {
        private final float hpMin;
        private final float hpMax;
        private final float dmgMin;
        private final float dmgMax;
        private final float sizeMin;
        private final float sizeMax;

        public ScalingDef(float hpMin, float hpMax, float dmgMin, float dmgMax, float sizeMin, float sizeMax) {
            this.hpMin = hpMin;
            this.hpMax = hpMax;
            this.dmgMin = dmgMin;
            this.dmgMax = dmgMax;
            this.sizeMin = sizeMin;
            this.sizeMax = sizeMax;
        }

        public static ScalingDef identity() {
            return new ScalingDef(1f, 1f, 1f, 1f, 1f, 1f);
        }

        /** Built-in per-tier defaults, used when {@code elite.toml} has no {@code [[scaling]]} block for a tier.
         *  Order: hpMin, hpMax, dmgMin, dmgMax, sizeMin, sizeMax. */
        public static ScalingDef defaultForTier(int tier) {
            return switch (Math.max(1, Math.min(5, tier))) {
                case 1 -> new ScalingDef(1.00f, 1.00f, 1.00f, 1.00f, 0.80f, 0.90f);
                case 2 -> new ScalingDef(1.10f, 1.25f, 1.10f, 1.20f, 1.00f, 1.10f);
                case 3 -> new ScalingDef(1.25f, 1.50f, 1.20f, 1.40f, 1.10f, 1.25f);
                case 4 -> new ScalingDef(1.50f, 1.75f, 1.40f, 1.60f, 1.25f, 1.40f);
                default -> new ScalingDef(1.75f, 2.50f, 1.60f, 2.00f, 1.40f, 1.60f);
            };
        }

        public float getHpMin() { return hpMin; }
        public float getHpMax() { return hpMax; }
        public float getDmgMin() { return dmgMin; }
        public float getDmgMax() { return dmgMax; }
        public float getSizeMin() { return sizeMin; }
        public float getSizeMax() { return sizeMax; }

        private static float lerp(float a, float b, double t) {
            return (float) (a + (b - a) * t);
        }

        public float rollHp(double t) { return lerp(Math.min(hpMin, hpMax), Math.max(hpMin, hpMax), t); }
        public float rollDamage(double t) { return lerp(Math.min(dmgMin, dmgMax), Math.max(dmgMin, dmgMax), t); }
        public float rollSize(double t) { return lerp(Math.min(sizeMin, sizeMax), Math.max(sizeMin, sizeMax), t); }
    }

    public static final class NamedElite {
        private final String id;
        private final String displayName;
        private final MobClass mobClass;
        private final int tier;
        private final double statBonus;

        public NamedElite(@Nonnull String id, @Nonnull String displayName,
                          @Nonnull MobClass mobClass, int tier, double statBonus) {
            this.id = id;
            this.displayName = displayName;
            this.mobClass = mobClass;
            this.tier = tier;
            this.statBonus = statBonus;
        }

        @Nonnull public String getId() { return id; }
        @Nonnull public String getDisplayName() { return displayName; }
        @Nonnull public MobClass getMobClass() { return mobClass; }
        public int getTier() { return tier; }
        public double getStatBonus() { return statBonus; }
    }

    /** RPGMobs-style mob rule: match a role by fragments, then constrain rolled gear. */
    public static final class MobRuleDef {
        private final String key;
        private final List<String> matchExact;
        private final List<String> matchStartsWith;
        private final List<String> matchContains;
        private final List<String> matchExcludes;
        private final List<String> allowedWeaponFamilies;
        private final List<String> allowedArmorMaterials;
        private final int armorSlotOverride;

        public MobRuleDef(@Nonnull String key,
                          @Nonnull List<String> matchExact, @Nonnull List<String> matchStartsWith,
                          @Nonnull List<String> matchContains, @Nonnull List<String> matchExcludes,
                          @Nonnull List<String> allowedWeaponFamilies, @Nonnull List<String> allowedArmorMaterials,
                          int armorSlotOverride) {
            this.key = key;
            this.matchExact = List.copyOf(matchExact);
            this.matchStartsWith = List.copyOf(matchStartsWith);
            this.matchContains = List.copyOf(matchContains);
            this.matchExcludes = List.copyOf(matchExcludes);
            this.allowedWeaponFamilies = List.copyOf(allowedWeaponFamilies);
            this.allowedArmorMaterials = List.copyOf(allowedArmorMaterials);
            this.armorSlotOverride = armorSlotOverride;
        }

        public boolean matches(@Nonnull String role) {
            String r = role.toLowerCase(Locale.ROOT);
            for (String ex : matchExcludes) {
                if (!ex.isBlank() && r.contains(ex.toLowerCase(Locale.ROOT))) {
                    return false;
                }
            }
            boolean any = matchExact.isEmpty() && matchStartsWith.isEmpty() && matchContains.isEmpty();
            for (String e : matchExact) {
                if (r.equals(e.toLowerCase(Locale.ROOT))) return true;
            }
            for (String s : matchStartsWith) {
                if (!s.isBlank() && r.startsWith(s.toLowerCase(Locale.ROOT))) return true;
            }
            for (String c : matchContains) {
                if (!c.isBlank() && r.contains(c.toLowerCase(Locale.ROOT))) return true;
            }
            return any;
        }

        @Nonnull public String getKey() { return key; }
        @Nonnull public List<String> getMatchExact() { return matchExact; }
        @Nonnull public List<String> getMatchStartsWith() { return matchStartsWith; }
        @Nonnull public List<String> getMatchContains() { return matchContains; }
        @Nonnull public List<String> getMatchExcludes() { return matchExcludes; }
        @Nonnull public List<String> getAllowedWeaponFamilies() { return allowedWeaponFamilies; }
        @Nonnull public List<String> getAllowedArmorMaterials() { return allowedArmorMaterials; }
        /** -1 (or any negative) = no override, fall back to the tier default. 0 = no armour at
         *  all. &gt;0 = force that many pieces. */
        public int getArmorSlotOverride() { return armorSlotOverride; }
    }

    /** Overrides zone defs / class weights for worlds whose name starts with {@link #worldPrefix}. */
    public static final class WorldOverlay {
        private final String worldPrefix;
        final Map<Integer, ZoneEliteDef> zoneDefs = new LinkedHashMap<>();
        final Map<Integer, Map<MobClass, Double>> classWeights = new LinkedHashMap<>();

        public WorldOverlay(@Nonnull String worldPrefix) {
            this.worldPrefix = worldPrefix;
        }

        public boolean matches(@Nonnull String worldName) {
            return !worldPrefix.isBlank()
                    && worldName.toLowerCase(Locale.ROOT).startsWith(worldPrefix.toLowerCase(Locale.ROOT));
        }

        @Nonnull public String getWorldPrefix() { return worldPrefix; }
        @Nonnull public Map<Integer, ZoneEliteDef> zoneDefs() { return zoneDefs; }
        @Nonnull public Map<Integer, Map<MobClass, Double>> classWeights() { return classWeights; }
    }
}
