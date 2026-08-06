/*
 * Decompiled with CFR 0.152.
 */
package fr.varyon.weaponstats;

import java.util.HashMap;
import java.util.Map;

public class WeaponInfoTranslations {
    private static final Map<String, Map<String, String>> translations = new HashMap<String, Map<String, String>>();

    static {
        HashMap<String, String> en = new HashMap<String, String>();
        en.put("title", "Informations sur les Armes");
        en.put("maxDamage", "Max Damage");
        en.put("avgDamage", "Avg Damage");
        en.put("level", "Level");
        en.put("quality", "Quality");
        en.put("durability", "Durability");
        en.put("type", "Type");
        en.put("id", "ID");
        en.put("attackTypes", "Attack Types:");
        en.put("Sword", "Swords");
        en.put("Longsword", "Longswords");
        en.put("Axe", "Axes");
        en.put("Battleaxe", "Battleaxes");
        en.put("Daggers", "Daggers");
        en.put("Spear", "Spears");
        en.put("Club", "Clubs");
        en.put("Mace", "Maces");
        en.put("Staff", "Staves");
        en.put("Wand", "Wands");
        en.put("Shortbow", "Shortbows");
        en.put("Crossbow", "Crossbows");
        en.put("Shield", "Shields");
        en.put("Claws", "Claws");
        en.put("Blowgun", "Blowguns");
        en.put("Gun", "Guns");
        en.put("Dart", "Darts");
        en.put("Arrow", "Arrows");
        en.put("Other", "Other Weapons");
        en.put("Firearms", "Firearms");
        en.put("Bow", "Bows");
        en.put("Bomb", "Bombs");
        en.put("Sword_Shield", "Sword & Shield");
        en.put("Dagger", "Daggers");
        en.put("Spellbook", "Spellbooks");
        en.put("Clubs", "Clubs");
        en.put("Longswords", "Longswords");
        en.put("attack_Swing_Left", "Left Swing");
        en.put("attack_Swing_Right", "Right Swing");
        en.put("attack_Swing_Down", "Down Swing");
        en.put("attack_Swing_Down_Left", "Down Left Swing");
        en.put("attack_Swing_Down_Right", "Down Right Swing");
        en.put("attack_Swing_Up", "Up Swing");
        en.put("attack_Thrust", "Thrust");
        en.put("attack_Vortexstrike_Spin", "Vortex (Special)");
        en.put("attack_Vortexstrike_Stab", "Stab (Special)");
        en.put("attack_Downstrike", "Down Strike");
        en.put("attack_Spear_Stab", "Stab (Spear)");
        en.put("attack_Longsword_Swing_Left", "Left Swing");
        en.put("attack_Longsword_Swing_Right", "Right Swing");
        en.put("attack_Longsword_Swing_Up_Left", "Up Left Swing");
        en.put("attack_Longsword_Stab_Charged", "Charged Stab");
        en.put("attack_Axe_Swing_Up_Right", "Up Right Swing");
        en.put("attack_Axe_Swing_Down_Left", "Down Left Swing");
        en.put("attack_Axe_Swing_Left_Charged", "Charged Left Swing");
        en.put("attack_Swing_Right_Charged", "Charged Right Swing");
        en.put("attack_Swing_Left_Charged", "Charged Left Swing");
        en.put("attack_Swing_Up_Left_Charged", "Charged Up Left Swing");
        en.put("attack_Swing_Up_Left", "Up Left Swing");
        en.put("attack_Groundslam", "Ground Slam");
        translations.put("en-US", en);
        translations.put("en", en);
    }

    public static String get(String language, String key) {
        String translation;
        Map<String, String> langMap;
        if (language == null || language.isEmpty()) {
            language = "en-US";
        }
        if ((langMap = translations.get(language)) == null && language.length() > 2) {
            langMap = translations.get(language.substring(0, 2));
        }
        if (langMap == null) {
            langMap = translations.get("en-US");
        }
        return (translation = langMap.get(key)) != null ? translation : key;
    }
}
