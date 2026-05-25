package com.varyon.comet.config.defaults;

import com.varyon.comet.config.model.BossEntry;
import com.varyon.comet.config.model.MobEntry;
import com.varyon.comet.config.model.ThemeConfig;
import com.varyon.comet.config.model.TierSettings;
import com.varyon.comet.config.model.WaveEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultThemes {

    public static Map<String, ThemeConfig> generateDefaults() {
        Map<String, ThemeConfig> themes = new LinkedHashMap<>();

        themes.put("skeleton", triple(
                "skeleton", "Horde de squelettes", Arrays.asList(1, 2),
                Arrays.asList(
                        new MobEntry("Skeleton_Soldier", 2, 4),
                        new MobEntry("Skeleton_Archer", 1, 2),
                        new MobEntry("Skeleton_Archmage", 1, 2)),
                Arrays.asList(
                        new MobEntry("Skeleton_Soldier", 3, 5),
                        new MobEntry("Skeleton_Archer", 2, 3),
                        new MobEntry("Skeleton_Archmage", 1, 3)),
                Arrays.asList(
                        new BossEntry("Golem_Crystal_Earth"),
                        new BossEntry("Bear_Grizzly", 3, 4))));

        themes.put("goblin", triple(
                "goblin", "Bande de gobelins", Arrays.asList(1, 2),
                Arrays.asList(
                        new MobEntry("Goblin_Scrapper", 2, 4),
                        new MobEntry("Goblin_Miner", 1, 3),
                        new MobEntry("Goblin_Lobber", 1, 2)),
                Arrays.asList(
                        new MobEntry("Goblin_Scrapper", 3, 5),
                        new MobEntry("Goblin_Hermit", 2, 4),
                        new MobEntry("Goblin_Ogre", 1, 2)),
                Arrays.asList(new BossEntry("Goblin_Duke", 1, 2))));

        themes.put("spider", triple(
                "spider", "Nuée d'araignées", Arrays.asList(1, 2),
                Arrays.asList(new MobEntry("Spider", 4, 6)),
                Arrays.asList(
                        new MobEntry("Spider", 2, 4),
                        new MobEntry("Spider_Cave", 3, 5)),
                Arrays.asList(new BossEntry("Spider_Broodmother"))));

        themes.put("trork", triple(
                "trork", "Troupe trork", Arrays.asList(1, 2, 3),
                Arrays.asList(
                        new MobEntry("Trork_Warrior", 2, 3),
                        new MobEntry("Trork_Hunter", 1, 2),
                        new MobEntry("Trork_Mauler", 1, 2),
                        new MobEntry("Trork_Shaman", 1, 2),
                        new MobEntry("Trork_Brawler", 1, 2)),
                Arrays.asList(
                        new MobEntry("Trork_Warrior", 3, 4),
                        new MobEntry("Trork_Hunter", 1, 3),
                        new MobEntry("Trork_Mauler", 1, 3),
                        new MobEntry("Trork_Shaman", 1, 3),
                        new MobEntry("Trork_Brawler", 1, 3)),
                Arrays.asList(new BossEntry("Trork_Chieftain", 1, 2))));

        themes.put("skeleton_sand", triple(
                "skeleton_sand", "Légion des sables", Arrays.asList(2, 3),
                Arrays.asList(
                        new MobEntry("Skeleton_Sand_Archer", 1, 2),
                        new MobEntry("Skeleton_Sand_Assassin", 1, 2),
                        new MobEntry("Skeleton_Sand_Guard", 2, 3),
                        new MobEntry("Skeleton_Sand_Mage", 1, 2),
                        new MobEntry("Skeleton_Sand_Ranger", 1, 2)),
                Arrays.asList(
                        new MobEntry("Skeleton_Sand_Archer", 1, 3),
                        new MobEntry("Skeleton_Sand_Assassin", 1, 3),
                        new MobEntry("Skeleton_Sand_Guard", 3, 4),
                        new MobEntry("Skeleton_Sand_Mage", 1, 3),
                        new MobEntry("Skeleton_Sand_Ranger", 1, 3)),
                Arrays.asList(
                        new BossEntry("Golem_Crystal_Thunder"),
                        new BossEntry("Skeleton_Sand_Guard", 3, 4),
                        new BossEntry("Scarak_Seeker", 1, 3),
                        new BossEntry("Scarak_Fighter", 2, 4))));

        themes.put("sabertooth", triple(
                "sabertooth", "Meute de tigres", Arrays.asList(2, 3),
                Arrays.asList(new MobEntry("Tiger_Sabertooth", 2, 4)),
                Arrays.asList(new MobEntry("Tiger_Sabertooth", 4, 6)),
                Arrays.asList(
                        new BossEntry("Golem_Crystal_Sand"),
                        new BossEntry("Scarak_Seeker", 1, 3),
                        new BossEntry("Tiger_Sabertooth", 4, 6))));

        themes.put("outlander", triple(
                "outlander", "Horde d'outlander", Arrays.asList(2, 3, 4, 5),
                Arrays.asList(
                        new MobEntry("Outlander_Berserker", 2, 3),
                        new MobEntry("Outlander_Cultist", 1, 2),
                        new MobEntry("Outlander_Hunter", 1, 2),
                        new MobEntry("Outlander_Stalker", 1, 2)),
                Arrays.asList(
                        new MobEntry("Outlander_Berserker", 2, 4),
                        new MobEntry("Outlander_Peon", 1, 2),
                        new MobEntry("Outlander_Sorcerer", 2, 3),
                        new MobEntry("Outlander_Stalker", 2, 3)),
                Arrays.asList(
                        new BossEntry("Outlander_Brute", 2, 3),
                        new BossEntry("Werewolf"))));

        themes.put("leopard", triple(
                "leopard", "Meute de glace", Arrays.asList(3, 4, 5),
                Arrays.asList(new MobEntry("Leopard_Snow", 3, 5)),
                Arrays.asList(new MobEntry("Leopard_Snow", 5, 6)),
                Arrays.asList(
                        new BossEntry("Leopard_Snow", 2, 3),
                        new BossEntry("Yeti", 1, 2),
                        new BossEntry("Werewolf"))));

        themes.put("toad", triple(
                "toad", "Groupe de crapauds", Arrays.asList(3, 4, 5),
                Arrays.asList(
                        new MobEntry("Toad_Rhino", 2, 3),
                        new MobEntry("Toad_Rhino_Magma", 2, 3)),
                Arrays.asList(
                        new MobEntry("Toad_Rhino", 3, 4),
                        new MobEntry("Toad_Rhino_Magma", 3, 4)),
                Arrays.asList(
                        new BossEntry("Shadow_Knight"),
                        new BossEntry("Zombie_Aberrant"))));

        themes.put("skeleton_burnt", triple(
                "skeleton_burnt", "Légion calcinée", Arrays.asList(3, 4, 5),
                Arrays.asList(
                        new MobEntry("Skeleton_Burnt_Archer", 1, 2),
                        new MobEntry("Skeleton_Burnt_Gunner", 1, 2),
                        new MobEntry("Skeleton_Burnt_Knight", 2, 4),
                        new MobEntry("Skeleton_Burnt_Lancer", 2, 3)),
                Arrays.asList(
                        new MobEntry("Skeleton_Burnt_Archer", 2, 3),
                        new MobEntry("Skeleton_Burnt_Gunner", 2, 3),
                        new MobEntry("Skeleton_Burnt_Knight", 3, 5),
                        new MobEntry("Skeleton_Burnt_Lancer", 2, 3)),
                Arrays.asList(new BossEntry("Skeleton_Burnt_Praetorian", 2, 3))));

        themes.put("void", triple(
                "void", "Engeance du vide", Arrays.asList(1, 2, 3),
                Arrays.asList(
                        new MobEntry("Crawler_Void", 1, 3),
                        new MobEntry("Spectre_Void", 1, 3),
                        new MobEntry("Eye_Void", 1, 2),
                        new MobEntry("Larva_Void", 1, 3)),
                Arrays.asList(
                        new MobEntry("Crawler_Void", 2, 4),
                        new MobEntry("Spectre_Void", 2, 3),
                        new MobEntry("Eye_Void", 2, 3)),
                Arrays.asList(
                        new BossEntry("Spawn_Void", 2, 3),
                        new BossEntry("Eye_Void", 1, 2))));

        themes.put("ice", triple(
                "ice", "Attroupement de glace", Arrays.asList(3, 4, 5),
                Arrays.asList(
                        new MobEntry("Yeti", 1, 2),
                        new MobEntry("Bear_Polar", 1, 3),
                        new MobEntry("Leopard_Snow", 1, 3)),
                Arrays.asList(
                        new MobEntry("Yeti", 1, 3),
                        new MobEntry("Bear_Polar", 2, 4),
                        new MobEntry("Leopard_Snow", 2, 5)),
                Arrays.asList(
                        new BossEntry("Yeti", 2, 4),
                        new BossEntry("Leopard_Snow", 2, 5),
                        new BossEntry("Golem_Crystal_Frost"),
                        new BossEntry("Spirit_Frost", 3, 4))));

        themes.put("lava", triple(
                "lava", "Attroupement de lave", Arrays.asList(3, 4, 5),
                Arrays.asList(
                        new MobEntry("Emberwulf", 2, 4),
                        new MobEntry("Golem_Firesteel", 1, 3),
                        new MobEntry("Spirit_Ember", 1, 2)),
                Arrays.asList(
                        new MobEntry("Emberwulf", 3, 5),
                        new MobEntry("Golem_Firesteel", 1, 3),
                        new MobEntry("Spirit_Ember", 2, 3)),
                Arrays.asList(
                        new BossEntry("Golem_Crystal_Flame"),
                        new BossEntry("Toad_Rhino_Magma", 2, 3))));

        themes.put("earth", triple(
                "earth", "Attroupement de terre", Arrays.asList(3, 4, 5),
                Arrays.asList(
                        new MobEntry("Golem_Crystal_Earth", 1, 1),
                        new MobEntry("Bear_Grizzly", 3, 5),
                        new MobEntry("Hyena", 3, 5)),
                Arrays.asList(
                        new MobEntry("Golem_Crystal_Earth", 1, 1),
                        new MobEntry("Bear_Grizzly", 1, 3),
                        new MobEntry("Hyena", 3, 5)),
                Arrays.asList(new BossEntry("Hedera"))));

        themes.put("undead_rare", triple(
                "undead_rare", "Groupe de morts vivants", Arrays.asList(1, 2, 3),
                Arrays.asList(
                        new MobEntry("Pig_Undead", 1, 3),
                        new MobEntry("Cow_Undead", 1, 2),
                        new MobEntry("Chicken_Undead", 1, 3)),
                Arrays.asList(
                        new MobEntry("Pig_Undead", 2, 3),
                        new MobEntry("Cow_Undead", 2, 2),
                        new MobEntry("Chicken_Undead", 1, 3),
                        new MobEntry("Hound_Bleached", 2, 3)),
                Arrays.asList(
                        new BossEntry("Spirit_Thunder", 2, 4),
                        new BossEntry("Golem_Crystal_Thunder"))));

        themes.put("undead_legendary", triple(
                "undead_legendary", "Cohorte de morts vivants", Arrays.asList(3, 4, 5),
                Arrays.asList(
                        new MobEntry("Pig_Undead", 7, 9),
                        new MobEntry("Cow_Undead", 3, 5),
                        new MobEntry("Chicken_Undead", 5, 7),
                        new MobEntry("Hound_Bleached", 2, 4)),
                Arrays.asList(
                        new MobEntry("Pig_Undead", 7, 9),
                        new MobEntry("Cow_Undead", 3, 5),
                        new MobEntry("Chicken_Undead", 5, 7),
                        new MobEntry("Hound_Bleached", 2, 4)),
                Arrays.asList(new BossEntry("Wraith"))));

        themes.put("zombie", triple(
                "zombie", "Aberration zombie", Arrays.asList(3, 4, 5),
                Arrays.asList(new MobEntry("Zombie_Aberrant_Small", 4, 6)),
                Arrays.asList(new MobEntry("Zombie_Aberrant_Small", 4, 6)),
                Arrays.asList(new BossEntry("Zombie_Aberrant"))));

        themes.put("frostbound_pack", triple(
                "frostbound_pack", "Meute du givre", Arrays.asList(2, 3, 4, 5),
                Arrays.asList(
                        new MobEntry("Leopard_Snow", 1, 3),
                        new MobEntry("Yeti", 1, 2),
                        new MobEntry("Skeleton_Archer", 1, 2),
                        new MobEntry("Hyena", 1, 2)),
                Arrays.asList(
                        new MobEntry("Leopard_Snow", 2, 4),
                        new MobEntry("Yeti", 2, 3),
                        new MobEntry("Skeleton_Archer", 2, 3),
                        new MobEntry("Hyena", 2, 3)),
                Arrays.asList(
                        new BossEntry("Spirit_Frost", 4, 6),
                        new BossEntry("Werewolf"))));

        themes.put("ashen_vanguard", triple(
                "ashen_vanguard", "Avant-garde des cendres", Arrays.asList(3, 4, 5),
                Arrays.asList(
                        new MobEntry("Skeleton_Burnt_Knight", 2, 3),
                        new MobEntry("Skeleton_Burnt_Lancer", 1, 3),
                        new MobEntry("Skeleton_Burnt_Gunner", 1, 2),
                        new MobEntry("Emberwulf", 1, 2)),
                Arrays.asList(
                        new MobEntry("Skeleton_Burnt_Knight", 3, 4),
                        new MobEntry("Skeleton_Burnt_Lancer", 2, 3),
                        new MobEntry("Skeleton_Burnt_Gunner", 2, 3),
                        new MobEntry("Emberwulf", 2, 3)),
                Arrays.asList(
                        new BossEntry("Skeleton_Burnt_Praetorian", 2, 3),
                        new BossEntry("Toad_Rhino_Magma", 2, 3))));

        themes.put("dune_stalkers", triple(
                "dune_stalkers", "Rôdeurs des dunes", Arrays.asList(2, 3, 4),
                Arrays.asList(
                        new MobEntry("Skeleton_Sand_Assassin", 1, 3),
                        new MobEntry("Skeleton_Sand_Ranger", 1, 3),
                        new MobEntry("Skeleton_Sand_Guard", 2, 3),
                        new MobEntry("Skeleton_Sand_Mage", 1, 2)),
                Arrays.asList(
                        new MobEntry("Skeleton_Sand_Assassin", 1, 3),
                        new MobEntry("Skeleton_Sand_Ranger", 1, 3),
                        new MobEntry("Skeleton_Sand_Guard", 3, 4),
                        new MobEntry("Skeleton_Sand_Mage", 1, 2),
                        new MobEntry("Tiger_Sabertooth", 2, 3)),
                Arrays.asList(
                        new BossEntry("Skeleton_Sand_Archer", 3, 4),
                        new BossEntry("Skeleton_Burnt_Alchemist", 2, 3))));

        themes.put("void_reavers", triple(
                "void_reavers", "Saccageurs du vide", Arrays.asList(2, 3, 4),
                Arrays.asList(
                        new MobEntry("Crawler_Void", 2, 4),
                        new MobEntry("Spectre_Void", 1, 3),
                        new MobEntry("Eye_Void", 1, 2),
                        new MobEntry("Skeleton_Archmage", 1, 2)),
                Arrays.asList(
                        new MobEntry("Crawler_Void", 3, 5),
                        new MobEntry("Spectre_Void", 2, 3),
                        new MobEntry("Eye_Void", 1, 2),
                        new MobEntry("Skeleton_Archmage", 2, 3)),
                Arrays.asList(
                        new BossEntry("Spawn_Void", 2, 3),
                        new BossEntry("Wraith", 2, 3))));

        themes.put("plague_horde", triple(
                "plague_horde", "Horde pestilentielle", Arrays.asList(2, 3, 4, 5),
                Arrays.asList(
                        new MobEntry("Pig_Undead", 2, 4),
                        new MobEntry("Chicken_Undead", 2, 4),
                        new MobEntry("Cow_Undead", 1, 3),
                        new MobEntry("Hound_Bleached", 1, 3)),
                Arrays.asList(
                        new MobEntry("Pig_Undead", 2, 4),
                        new MobEntry("Chicken_Undead", 2, 4),
                        new MobEntry("Cow_Undead", 1, 3),
                        new MobEntry("Hound_Bleached", 2, 4)),
                Arrays.asList(
                        new BossEntry("Wraith", 2, 4),
                        new BossEntry("Golem_Crystal_Thunder_Comet"))));

        themes.put("trork_siege", triple(
                "trork_siege", "Bataillon trork", Arrays.asList(2, 3, 4),
                Arrays.asList(
                        new MobEntry("Trork_Warrior", 1, 3),
                        new MobEntry("Trork_Brawler", 1, 3),
                        new MobEntry("Trork_Hunter", 1, 2),
                        new MobEntry("Trork_Mauler", 1, 2),
                        new MobEntry("Trork_Shaman", 1, 2)),
                Arrays.asList(
                        new MobEntry("Trork_Warrior", 2, 4),
                        new MobEntry("Trork_Brawler", 2, 3),
                        new MobEntry("Trork_Hunter", 2, 3),
                        new MobEntry("Trork_Mauler", 2, 3),
                        new MobEntry("Trork_Shaman", 2, 3)),
                Arrays.asList(
                        new BossEntry("Trork_Warrior", 2, 4),
                        new BossEntry("Trork_Chieftain", 2, 3))));

        themes.put("ember_hunters", triple(
                "ember_hunters", "Chasseurs des braises", Arrays.asList(3, 4, 5),
                Arrays.asList(
                        new MobEntry("Emberwulf", 1, 3),
                        new MobEntry("Spirit_Ember", 1, 2),
                        new MobEntry("Golem_Firesteel", 1, 2),
                        new MobEntry("Skeleton_Burnt_Archer", 1, 2),
                        new MobEntry("Hyena", 1, 3)),
                Arrays.asList(
                        new MobEntry("Emberwulf", 2, 3),
                        new MobEntry("Spirit_Ember", 2, 3),
                        new MobEntry("Golem_Firesteel", 1, 2),
                        new MobEntry("Skeleton_Burnt_Archer", 2, 3),
                        new MobEntry("Hyena", 2, 3)),
                Arrays.asList(
                        new BossEntry("Toad_Rhino_Magma", 2, 3),
                        new BossEntry("Zombie_Aberrant", 2, 3))));

        themes.put("wraithborn_legion", triple(
                "wraithborn_legion", "Légion des spectres", Arrays.asList(3, 4, 5),
                Arrays.asList(
                        new MobEntry("Skeleton_Archmage", 1, 3),
                        new MobEntry("Spectre_Void", 1, 3),
                        new MobEntry("Outlander_Cultist", 1, 2),
                        new MobEntry("Outlander_Stalker", 1, 2)),
                Arrays.asList(
                        new MobEntry("Skeleton_Archmage", 2, 4),
                        new MobEntry("Spectre_Void", 2, 4),
                        new MobEntry("Outlander_Cultist", 1, 3),
                        new MobEntry("Outlander_Stalker", 1, 3)),
                Arrays.asList(
                        new BossEntry("Wraith", 2, 4),
                        new BossEntry("Outlander_Brute", 2, 4))));

        themes.put("predator_clan", triple(
                "predator_clan", "Clan des prédateurs", Arrays.asList(1, 2, 3, 4),
                Arrays.asList(
                        new MobEntry("Tiger_Sabertooth", 1, 3),
                        new MobEntry("Leopard_Snow", 1, 3),
                        new MobEntry("Hyena", 1, 3),
                        new MobEntry("Goblin_Lobber", 1, 2)),
                Arrays.asList(
                        new MobEntry("Tiger_Sabertooth", 2, 3),
                        new MobEntry("Leopard_Snow", 2, 3),
                        new MobEntry("Hyena", 2, 3),
                        new MobEntry("Goblin_Lobber", 2, 3)),
                Arrays.asList(
                        new BossEntry("Wolf_Black", 3, 12),
                        new BossEntry("Bear_Polar", 2, 4))));

        return themes;
    }

    private static ThemeConfig triple(
            String id,
            String displayName,
            List<Integer> tiers,
            List<MobEntry> wave1Mobs,
            List<MobEntry> wave2Mobs,
            List<BossEntry> bossWave) {
        ThemeConfig theme = new ThemeConfig(id, displayName, tiers, new ArrayList<>(), new ArrayList<>(), true);
        List<WaveEntry> waves = new ArrayList<>();
        WaveEntry w1 = new WaveEntry(WaveEntry.WaveType.NORMAL);
        w1.setMobs(wave1Mobs);
        waves.add(w1);
        WaveEntry w2 = new WaveEntry(WaveEntry.WaveType.NORMAL);
        w2.setMobs(wave2Mobs);
        waves.add(w2);
        WaveEntry w3 = new WaveEntry(WaveEntry.WaveType.BOSS);
        w3.setBosses(bossWave);
        waves.add(w3);
        theme.setWaves(waves);
        theme.setNaturalSpawn(true);
        return theme;
    }

    public static Map<Integer, TierSettings> getDefaultTierSettings() {
        Map<Integer, TierSettings> settings = new LinkedHashMap<>();
        settings.put(1, TierSettings.TIER1_DEFAULTS);
        settings.put(2, TierSettings.TIER2_DEFAULTS);
        settings.put(3, TierSettings.TIER3_DEFAULTS);
        settings.put(4, TierSettings.TIER4_DEFAULTS);
        settings.put(5, TierSettings.TIER5_DEFAULTS);
        return settings;
    }
}
