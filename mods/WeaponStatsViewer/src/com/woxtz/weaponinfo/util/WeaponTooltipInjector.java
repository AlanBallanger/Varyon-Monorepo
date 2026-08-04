/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.modules.i18n.I18nModule
 */
package com.woxtz.weaponinfo.util;

import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.woxtz.weaponinfo.WeaponDamageCache;
import com.woxtz.weaponinfo.util.ModLangLoader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class WeaponTooltipInjector {
    private static final Logger LOGGER = Logger.getLogger("WeaponTooltipInjector");

    public static void injectTooltips() {
        try {
            I18nModule i18n = I18nModule.get();
            if (i18n == null) {
                LOGGER.warning("I18nModule instance is null. Cannot inject tooltips.");
                return;
            }
            Field languagesField = I18nModule.class.getDeclaredField("languages");
            languagesField.setAccessible(true);
            Map<String, Map<String, String>> languages = (Map<String, Map<String, String>>)languagesField.get(i18n);
            if (languages == null) {
                LOGGER.warning("Languages map is null. Cannot inject tooltips.");
                return;
            }
            String[] targetLangs = new String[]{"en-US", "es-ES"};
            int injectedCount = 0;
            for (Map.Entry<String, WeaponDamageCache.WeaponDamage> entry : WeaponDamageCache.getAllWeapons().entrySet()) {
                String itemId = entry.getKey();
                WeaponDamageCache.WeaponDamage damage = entry.getValue();
                String descKey = damage.descriptionKey;
                if (descKey == null || descKey.isEmpty()) {
                    descKey = "server.items." + itemId + ".description";
                }
                String[] stringArray = targetLangs;
                int n = targetLangs.length;
                int n2 = 0;
                while (n2 < n) {
                    block9: {
                        String finalTooltip;
                        Map<String, String> langMap;
                        block10: {
                            String statsText;
                            block8: {
                                String markerCheck;
                                String lang = stringArray[n2];
                                langMap = languages.computeIfAbsent(lang, k -> new ConcurrentHashMap());
                                statsText = WeaponTooltipInjector.generateTooltipText(damage, lang);
                                String existingDesc = (String)langMap.get(descKey);
                                if (existingDesc == null || existingDesc.isEmpty()) {
                                    existingDesc = ModLangLoader.get((String)descKey);
                                }
                                if (existingDesc == null || existingDesc.isEmpty()) break block8;
                                boolean isSpanish = lang.startsWith("es");
                                String string = markerCheck = isSpanish ? "Da\u00f1o Prom" : "Avg Damage";
                                if (existingDesc.contains(markerCheck)) break block9;
                                finalTooltip = existingDesc + statsText;
                                break block10;
                            }
                            finalTooltip = statsText;
                        }
                        langMap.put(descKey, finalTooltip);
                    }
                    ++n2;
                }
                ++injectedCount;
            }
            LOGGER.info("Successfully injected dynamic tooltips for " + injectedCount + " weapons.");
        }
        catch (Exception e) {
            LOGGER.severe("Failed to inject weapon tooltips: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String generateTooltipText(WeaponDamageCache.WeaponDamage damage, String lang) {
        StringBuilder sb = new StringBuilder();
        String colorLabel = "#a3906d";
        double minShot = Double.MAX_VALUE;
        double maxShot = 0.0;
        boolean hasShootStrength = false;
        HashMap<String, Double> specificAttacks = new HashMap<String, Double>();
        if (!damage.attackTypes.isEmpty()) {
            for (Map.Entry<String, Double> entry : damage.attackTypes.entrySet()) {
                String key = entry.getKey();
                if (key.contains("Shoot") && key.contains("Strength")) {
                    hasShootStrength = true;
                    double d = entry.getValue();
                    if (d < minShot) {
                        minShot = d;
                    }
                    if (!(d > maxShot)) continue;
                    maxShot = d;
                    continue;
                }
                specificAttacks.put(key, entry.getValue());
            }
        }
        sb.append("\n");
        if (hasShootStrength) {
            String damageLabel = lang.startsWith("es") ? "Da\u00f1o" : "Damage";
            String string = lang.startsWith("es") ? "Da\u00f1o Prom" : "Avg Damage";
            String rangeStr = minShot == maxShot ? String.format("%d", (int)maxShot) : String.format("%d-%d", (int)minShot, (int)maxShot);
            sb.append(String.format("<color is=\"%s\">%s:</color> %s | <color is=\"%s\">%s:</color> %d", colorLabel, damageLabel, rangeStr, colorLabel, string, (int)damage.avgDamage));
        } else {
            String maxLabel = lang.startsWith("es") ? "Da\u00f1o M\u00e1x" : "Max Damage";
            String string = lang.startsWith("es") ? "Da\u00f1o Prom" : "Avg Damage";
            sb.append(String.format("<color is=\"%s\">%s:</color> %d | <color is=\"%s\">%s:</color> %d", colorLabel, maxLabel, (int)damage.maxDamage, colorLabel, string, (int)damage.avgDamage));
        }
        if (!specificAttacks.isEmpty()) {
            sb.append("\nAttack Types:");
            HashMap<Double, List<String>> damageToAttacks = new HashMap<Double, List<String>>();
            for (Map.Entry<String, Double> entry : specificAttacks.entrySet()) {
                damageToAttacks.computeIfAbsent(entry.getValue(), k -> new ArrayList<String>()).add(entry.getKey());
            }
            ArrayList<String> arrayList = new ArrayList<String>();
            ArrayList<Double> sortedDamages = new ArrayList<Double>(damageToAttacks.keySet());
            sortedDamages.sort((d1, d2) -> Double.compare(d2, d1));
            for (Double dmg : sortedDamages) {
                List<String> attacks = damageToAttacks.get(dmg);
                ArrayList<String> simpleNames = new ArrayList<String>();
                for (String rawName : attacks) {
                    simpleNames.add(WeaponTooltipInjector.simplifyAttackName(rawName, lang));
                }
                String joinedNames = String.join((CharSequence)" | ", simpleNames);
                arrayList.add(String.format("<color is=\"%s\"><i>%s:</i></color> %d", colorLabel, joinedNames, dmg.intValue()));
            }
            int itemsPerLine = 3;
            int i = 0;
            while (i < arrayList.size()) {
                if (i % itemsPerLine == 0) {
                    sb.append("\n  ");
                }
                sb.append((String)arrayList.get(i));
                if (i < arrayList.size() - 1 && (i + 1) % itemsPerLine != 0) {
                    sb.append("   ");
                }
                ++i;
            }
        }
        if (damage.signatureEnergy > 0.0) {
            String sigLabel = lang.startsWith("es") ? "Energ\u00eda Firma" : "Signature Energy";
            sb.append("\n\n<color is=\"#a3906d\">" + sigLabel + ": +" + (int)damage.signatureEnergy + "</color>");
        }
        return sb.toString();
    }

    private static String simplifyAttackName(String raw, String lang) {
        String clean = raw.replace("Weapon_", "").replace("_Damage", "");
        String[] parts = clean.split("_");
        ArrayList<String> keywords = new ArrayList<String>();
        boolean startCollecting = false;
        String[] stringArray = parts;
        int n = parts.length;
        int n2 = 0;
        while (n2 < n) {
            String p = stringArray[n2];
            if (p.equals("Primary") || p.equals("Secondary") || p.equals("Signature")) {
                startCollecting = true;
            } else if (startCollecting) {
                keywords.add(p);
            }
            ++n2;
        }
        if (keywords.isEmpty()) {
            return clean.replace("_", " ");
        }
        return String.join((CharSequence)" ", keywords);
    }
}

