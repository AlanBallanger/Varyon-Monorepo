/*
 * Decompiled with CFR 0.152.
 */
package com.woxtz.weaponinfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SpanishSearchTranslator {
    private static final Map<String, String> translations = new HashMap<String, String>();

    static {
        translations.put("espada", "sword");
        translations.put("hacha", "axe");
        translations.put("alabarda", "halberd");
        translations.put("daga", "dagger");
        translations.put("lanza", "spear");
        translations.put("baculo", "staff");
        translations.put("b\u00e1culo", "staff");
        translations.put("vara", "wand");
        translations.put("varita", "wand");
        translations.put("arco", "bow");
        translations.put("flecha", "arrow");
        translations.put("ballesta", "crossbow");
        translations.put("escudo", "shield");
        translations.put("garras", "claws");
        translations.put("garra", "claws");
        translations.put("maza", "mace");
        translations.put("mazo", "club");
        translations.put("libro", "spellbook");
        translations.put("cerbatana", "blowgun");
        translations.put("pistola", "gun");
        translations.put("kunai", "kunai");
        translations.put("madera", "wood");
        translations.put("piedra", "stone");
        translations.put("hierro", "iron");
        translations.put("acero", "steel");
        translations.put("bronce", "bronze");
        translations.put("cobre", "copper");
        translations.put("cobalto", "cobalt");
        translations.put("mitril", "mithril");
        translations.put("mithril", "mithril");
        translations.put("adamantita", "adamantite");
        translations.put("adamantite", "adamantite");
        translations.put("torio", "thorium");
        translations.put("thorium", "thorium");
        translations.put("\u00f3nix", "onyxium");
        translations.put("onix", "onyxium");
        translations.put("onyxium", "onyxium");
        translations.put("hueso", "bone");
        translations.put("tosco", "crude");
        translations.put("tosca", "crude");
        translations.put("crudo", "crude");
        translations.put("cruda", "crude");
        translations.put("tribal", "tribal");
        translations.put("condenado", "doomed");
        translations.put("condenada", "doomed");
        translations.put("escarabajo", "scarab");
        translations.put("plateado", "silversteel");
        translations.put("plateada", "silversteel");
        translations.put("incandescente", "incandescent");
        translations.put("podrido", "rotten");
        translations.put("podrida", "rotten");
        translations.put("oxidado", "rusty");
        translations.put("oxidada", "rusty");
        translations.put("antiguo", "ancient");
        translations.put("antigua", "ancient");
        translations.put("encantado", "enchanted");
        translations.put("encantada", "enchanted");
        translations.put("roto", "broken");
        translations.put("rota", "broken");
        translations.put("pesado", "heavy");
        translations.put("pesada", "heavy");
        translations.put("ligero", "light");
        translations.put("ligera", "light");
        translations.put("depuracion", "debug");
        translations.put("depuraci\u00f3n", "debug");
        translations.put("prueba", "debug");
    }

    public static String translate(String term) {
        String lowerTerm = term.toLowerCase();
        if (translations.containsKey(lowerTerm)) {
            return translations.get(lowerTerm);
        }
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            if (!entry.getKey().contains(lowerTerm)) continue;
            return entry.getValue();
        }
        return term;
    }

    public static String[] getAllPossibleTranslations(String term) {
        String lowerTerm = term.toLowerCase();
        ArrayList<String> results = new ArrayList<String>();
        results.add(term);
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            if (!entry.getKey().contains(lowerTerm) && !lowerTerm.contains(entry.getKey()) || results.contains(entry.getValue())) continue;
            results.add(entry.getValue());
        }
        return results.toArray(new String[0]);
    }

    public static String[] translateTerms(String[] terms) {
        String[] translated = new String[terms.length];
        int i = 0;
        while (i < terms.length) {
            translated[i] = SpanishSearchTranslator.translate(terms[i]);
            ++i;
        }
        return translated;
    }
}

