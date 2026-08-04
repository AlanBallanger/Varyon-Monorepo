/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.codec.Codec
 *  com.hypixel.hytale.codec.KeyedCodec
 *  com.hypixel.hytale.codec.builder.BuilderCodec
 *  com.hypixel.hytale.codec.builder.BuilderCodec$Builder
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
 *  com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.asset.type.item.config.Item
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
 *  com.hypixel.hytale.server.core.modules.i18n.I18nModule
 *  com.hypixel.hytale.server.core.ui.builder.EventData
 *  com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
 *  com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 */
package com.woxtz.weaponinfo.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.woxtz.weaponinfo.SpanishSearchTranslator;
import com.woxtz.weaponinfo.WeaponDamageCache;
import com.woxtz.weaponinfo.WeaponInfoTranslations;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

public class WeaponInfoPage
extends InteractiveCustomUIPage<WeaponInfoPage.SearchData> {
    private String searchQuery;
    private final Map<String, List<Map.Entry<String, Item>>> weaponsByType;
    private final Map<String, Message> tooltipCache;
    private final Map<String, Message> nameCache;

    public WeaponInfoPage(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, String defaultSearchQuery) {
        super(playerRef, lifetime, SearchData.CODEC);
        this.searchQuery = defaultSearchQuery;
        this.weaponsByType = new LinkedHashMap<String, List<Map.Entry<String, Item>>>();
        this.tooltipCache = new HashMap<String, Message>();
        this.nameCache = new HashMap<String, Message>();
    }

    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/WeaponInfo_Main.ui");
        uiCommandBuilder.set("#TitleText.TextSpans", Message.raw((String)WeaponInfoTranslations.get(this.playerRef.getLanguage(), "title")));
        uiCommandBuilder.set("#SearchInput.Value", this.searchQuery);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput", EventData.of((String)"@SearchQuery", (String)"#SearchInput.Value"), false);
        this.buildWeaponList(ref, uiCommandBuilder, uiEventBuilder, store);
    }

    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull SearchData data) {
        super.handleDataEvent(ref, store, data);
        if (data.searchQuery != null) {
            this.searchQuery = data.searchQuery.trim().toLowerCase();
            UICommandBuilder commandBuilder = new UICommandBuilder();
            UIEventBuilder eventBuilder = new UIEventBuilder();
            this.buildWeaponList(ref, commandBuilder, eventBuilder, store);
            this.sendUpdate(commandBuilder, eventBuilder, false);
        }
    }

    private void buildWeaponList(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        this.weaponsByType.clear();
        Map<String, Item> allItems = Item.getAssetMap().getAssetMap();
        String language = this.playerRef.getLanguage();
        String[] searchTerms = this.searchQuery.isEmpty() ? new String[]{} : this.searchQuery.split(" ");
        for (Map.Entry<String, Item> entry : allItems.entrySet()) {
            String weaponType;
            boolean includeItem;
            Item item = (Item)entry.getValue();
            String itemId = (String)entry.getKey();
            if (item.getWeapon() == null) continue;
            boolean bl = includeItem = searchTerms.length == 0;
            if (!includeItem) {
                includeItem = true;
                String[] stringArray = searchTerms;
                int n = searchTerms.length;
                int n2 = 0;
                while (n2 < n) {
                    String[] possibleTerms;
                    String term = stringArray[n2];
                    boolean termFound = false;
                    String[] stringArray2 = possibleTerms = SpanishSearchTranslator.getAllPossibleTranslations(term);
                    int n3 = possibleTerms.length;
                    int n4 = 0;
                    while (n4 < n3) {
                        String englishName;
                        String searchTerm = stringArray2[n4];
                        if (itemId.toLowerCase().contains(searchTerm.toLowerCase())) {
                            termFound = true;
                            break;
                        }
                        String translatedName = I18nModule.get().getMessage(language, item.getTranslationKey());
                        if (translatedName != null && translatedName.toLowerCase().contains(searchTerm.toLowerCase())) {
                            termFound = true;
                            break;
                        }
                        if (!language.equals("en-US") && (englishName = I18nModule.get().getMessage("en-US", item.getTranslationKey())) != null && englishName.toLowerCase().contains(searchTerm.toLowerCase())) {
                            termFound = true;
                            break;
                        }
                        ++n4;
                    }
                    if (!termFound) {
                        includeItem = false;
                        break;
                    }
                    ++n2;
                }
            }
            if (!includeItem) continue;
            if (itemId.contains("Arrow")) {
                weaponType = "Arrow";
            } else {
                weaponType = item.getPlayerAnimationsId();
                if (weaponType == null || weaponType.isEmpty()) {
                    weaponType = "Other";
                }
            }
            this.weaponsByType.computeIfAbsent(weaponType, k -> new ArrayList()).add(entry);
        }
        this.buildWeaponCards(commandBuilder, eventBuilder);
    }

    private void buildWeaponCards(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        commandBuilder.clear("#SubcommandCards");
        commandBuilder.set("#SubcommandSection.Visible", true);
        int componentIndex = 0;
        String language = this.playerRef.getLanguage();
        for (Map.Entry<String, List<Map.Entry<String, Item>>> typeEntry : this.weaponsByType.entrySet()) {
            String weaponType = typeEntry.getKey();
            List<Map.Entry<String, Item>> weapons = typeEntry.getValue();
            weapons.sort((e1, e2) -> {
                WeaponDamageCache.WeaponDamage d1 = WeaponDamageCache.getWeaponDamage((String)e1.getKey());
                WeaponDamageCache.WeaponDamage d2 = WeaponDamageCache.getWeaponDamage((String)e2.getKey());
                int priority1 = this.getQualityPriority(d1 != null ? d1.quality : "Common");
                int priority2 = this.getQualityPriority(d2 != null ? d2.quality : "Common");
                return Integer.compare(priority2, priority1);
            });
            commandBuilder.append("#SubcommandCards", "Pages/WeaponInfo_SectionHeader.ui");
            String sectionTitle = WeaponInfoTranslations.get(language, weaponType);
            commandBuilder.set("#SubcommandCards[" + componentIndex + "] #SectionTitle.TextSpans", Message.raw((String)sectionTitle));
            int cardsInCurrentRow = 0;
            int rowIndex = ++componentIndex;
            for (Map.Entry<String, Item> weaponEntry : weapons) {
                Item item = weaponEntry.getValue();
                if (cardsInCurrentRow == 0) {
                    commandBuilder.append("#SubcommandCards", "Pages/WeaponInfo_Row.ui");
                }
                commandBuilder.append("#SubcommandCards[" + rowIndex + "]", "Pages/WeaponInfo_Card.ui");
                String itemId = weaponEntry.getKey();
                Message tooltip = this.tooltipCache.computeIfAbsent(itemId, id -> this.buildWeaponTooltip((String)id, item));
                commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "].TooltipTextSpans", tooltip);
                commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemIcon.ItemId", weaponEntry.getKey());
                Message weaponName = this.nameCache.computeIfAbsent(itemId, id -> Message.translation((String)item.getTranslationKey()));
                commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #ItemName.TextSpans", weaponName);
                WeaponDamageCache.WeaponDamage damage = WeaponDamageCache.getWeaponDamage(itemId);
                String baseColor = "#2d2d2d";
                if (itemId.startsWith("WanMine_")) {
                    baseColor = "#380c42";
                } else if (damage != null && damage.quality != null) {
                    baseColor = this.getQualityBackgroundColor(damage.quality);
                }
                commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "].Background", baseColor);
                String hoverColor = this.lightenColor(baseColor, 0.15f);
                commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "].Style.Hovered.Background", hoverColor);
                if (damage != null) {
                    String damageText;
                    String damageHeader = language.equals("es") ? "Da\u00f1o:" : "Damage:";
                    commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #DamageHeader.TextSpans", Message.raw((String)damageHeader));
                    if (damage.hasRandomDamage && damage.randomModifier > 0.0) {
                        int minDamage = (int)Math.round(damage.maxDamage * (1.0 - damage.randomModifier));
                        int maxDamage = (int)Math.round(damage.maxDamage * (1.0 + damage.randomModifier));
                        damageText = minDamage + "-" + maxDamage;
                    } else {
                        damageText = "Max: " + (int)damage.maxDamage + " / Avg: " + (int)damage.avgDamage;
                    }
                    commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #DamageLabel.TextSpans", Message.raw((String)damageText));
                } else {
                    commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #DamageHeader.TextSpans", Message.raw((String)""));
                    commandBuilder.set("#SubcommandCards[" + rowIndex + "][" + cardsInCurrentRow + "] #DamageLabel.TextSpans", Message.raw((String)""));
                }
                if (++cardsInCurrentRow < 6) continue;
                cardsInCurrentRow = 0;
                ++rowIndex;
                ++componentIndex;
            }
            componentIndex = cardsInCurrentRow > 0 ? rowIndex + 1 : rowIndex;
        }
    }

    private Message buildWeaponTooltip(String itemId, Item item) {
        String titleColor;
        WeaponDamageCache.WeaponDamage damage;
        String language;
        ArrayList<Message> messages;
        block32: {
            block33: {
                block31: {
                    messages = new ArrayList<Message>();
                    language = this.playerRef.getLanguage();
                    damage = WeaponDamageCache.getWeaponDamage(itemId);
                    if (!itemId.startsWith("WanMine_")) break block31;
                    titleColor = "#ff1493";
                    break block32;
                }
                if (damage == null || damage.quality == null) break block33;
                switch (damage.quality) {
                    case "Common": {
                        titleColor = "#ffffff";
                        break;
                    }
                    case "Uncommon": {
                        titleColor = "#77b35c";
                        break;
                    }
                    case "Rare": {
                        titleColor = "#4a9cd4";
                        break;
                    }
                    case "Epic": {
                        titleColor = "#a040d4";
                        break;
                    }
                    case "Legendary": {
                        titleColor = "#d4a040";
                        break;
                    }
                    default: {
                        titleColor = "#ffffff";
                        break;
                    }
                }
                break block32;
            }
            titleColor = "#ffffff";
        }
        Message weaponName = this.nameCache.computeIfAbsent(itemId, id -> Message.translation((String)item.getTranslationKey()));
        messages.add(weaponName.bold(true).color(titleColor));
        messages.add(Message.raw((String)"\n"));
        messages.add(Message.raw((String)"--------------------------").color("#4a5a52"));
        messages.add(Message.raw((String)"\n"));
        if (damage != null) {
            if (damage.hasRandomDamage && damage.randomModifier > 0.0) {
                int minDamage = (int)Math.round(damage.maxDamage * (1.0 - damage.randomModifier));
                int maxDamage = (int)Math.round(damage.maxDamage * (1.0 + damage.randomModifier));
                messages.add(Message.raw((String)WeaponInfoTranslations.get(language, "maxDamage")).color("#93844c").bold(true));
                messages.add(Message.raw((String)(": " + minDamage + "-" + maxDamage)).color("#ffffff"));
                messages.add(Message.raw((String)"\n"));
            } else {
                messages.add(Message.raw((String)WeaponInfoTranslations.get(language, "maxDamage")).color("#93844c").bold(true));
                messages.add(Message.raw((String)(": " + (int)damage.maxDamage)).color("#ffffff"));
                messages.add(Message.raw((String)"\n"));
                messages.add(Message.raw((String)WeaponInfoTranslations.get(language, "avgDamage")).color("#93844c").bold(true));
                messages.add(Message.raw((String)(": " + (int)damage.avgDamage)).color("#ffffff"));
                messages.add(Message.raw((String)"\n"));
            }
            if (!damage.attackTypes.isEmpty()) {
                messages.add(Message.raw((String)"\n"));
                messages.add(Message.raw((String)WeaponInfoTranslations.get(language, "attackTypes")).color("#93844c").bold(true));
                messages.add(Message.raw((String)"\n"));
                for (Map.Entry<String, Double> attackEntry : damage.attackTypes.entrySet()) {
                    Object damageDisplay;
                    String attackName = this.translateAttackType(attackEntry.getKey(), language);
                    double baseAttackDamage = attackEntry.getValue();
                    if (damage.hasRandomDamage && damage.randomModifier > 0.0) {
                        int minAttackDamage = (int)Math.round(baseAttackDamage * (1.0 - damage.randomModifier));
                        int maxAttackDamage = (int)Math.round(baseAttackDamage * (1.0 + damage.randomModifier));
                        damageDisplay = minAttackDamage + "-" + maxAttackDamage;
                    } else {
                        damageDisplay = String.valueOf((int)baseAttackDamage);
                    }
                    messages.add(Message.raw((String)("  \u2022 " + attackName + ": ")).color("#b0b0b0"));
                    messages.add(Message.raw((String)damageDisplay).color("#ff6b6b"));
                    messages.add(Message.raw((String)"\n"));
                }
            }
            messages.add(Message.raw((String)"\n"));
        }
        messages.add(Message.raw((String)WeaponInfoTranslations.get(language, "level")).color("#93844c").bold(true));
        messages.add(Message.raw((String)(": " + item.getItemLevel())));
        messages.add(Message.raw((String)"\n"));
        if (item.getQualityIndex() > 0) {
            messages.add(Message.raw((String)WeaponInfoTranslations.get(language, "quality")).color("#93844c").bold(true));
            messages.add(Message.raw((String)(": " + item.getQualityIndex())));
            messages.add(Message.raw((String)"\n"));
        }
        if (item.getMaxDurability() > 0.0) {
            messages.add(Message.raw((String)WeaponInfoTranslations.get(language, "durability")).color("#93844c").bold(true));
            messages.add(Message.raw((String)(": " + (int)item.getMaxDurability())));
            messages.add(Message.raw((String)"\n"));
        }
        if (item.getCategories() != null && item.getCategories().length > 0) {
            messages.add(Message.raw((String)WeaponInfoTranslations.get(language, "type")).color("#93844c").bold(true));
            StringBuilder categories = new StringBuilder();
            int i = 0;
            while (i < item.getCategories().length) {
                String cat;
                int dotIndex;
                if (i > 0) {
                    categories.append(", ");
                }
                categories.append((dotIndex = (cat = item.getCategories()[i]).lastIndexOf(46)) > 0 ? cat.substring(dotIndex + 1) : cat);
                ++i;
            }
            messages.add(Message.raw((String)(": " + categories.toString())));
            messages.add(Message.raw((String)"\n"));
        }
        messages.add(Message.raw((String)"--------------------------").color("#4a5a52"));
        messages.add(Message.raw((String)"\n"));
        messages.add(Message.raw((String)WeaponInfoTranslations.get(language, "id")).color("#5a6a62").italic(true));
        messages.add(Message.raw((String)(": " + itemId)).color("#6a7a72").italic(true));
        return Message.join((Message[])messages.toArray(new Message[0]));
    }

    private int getQualityPriority(String quality) {
        if (quality == null) {
            return 0;
        }
        switch (quality) {
            case "Legendary": {
                return 5;
            }
            case "Epic": {
                return 4;
            }
            case "Rare": {
                return 3;
            }
            case "Uncommon": {
                return 2;
            }
            case "Common": {
                return 1;
            }
        }
        return 0;
    }

    private String getQualityBackgroundColor(String quality) {
        if (quality == null) {
            return "#2d2d2d";
        }
        switch (quality) {
            case "Legendary": {
                return "#3d2e1a";
            }
            case "Epic": {
                return "#2d1f3d";
            }
            case "Rare": {
                return "#1a2d3d";
            }
            case "Uncommon": {
                return "#1f3d2a";
            }
            case "Common": {
                return "#2d2d2d";
            }
        }
        return "#2d2d2d";
    }

    private String lightenColor(String hexColor, float amount) {
        if (hexColor == null || !hexColor.startsWith("#")) {
            return hexColor;
        }
        try {
            int r = Integer.parseInt(hexColor.substring(1, 3), 16);
            int g = Integer.parseInt(hexColor.substring(3, 5), 16);
            int b = Integer.parseInt(hexColor.substring(5, 7), 16);
            r = (int)Math.min(255.0f, (float)r + (float)(255 - r) * amount);
            g = (int)Math.min(255.0f, (float)g + (float)(255 - g) * amount);
            b = (int)Math.min(255.0f, (float)b + (float)(255 - b) * amount);
            return String.format("#%02x%02x%02x", r, g, b);
        }
        catch (NumberFormatException e) {
            return hexColor;
        }
    }

    private String getQualityHoverColor(String quality) {
        if (quality == null) {
            return "#3a3a3a";
        }
        switch (quality) {
            case "Legendary": {
                return "#4d3e2a";
            }
            case "Epic": {
                return "#3d2f4d";
            }
            case "Rare": {
                return "#2a3d4d";
            }
            case "Uncommon": {
                return "#2f4d3a";
            }
            case "Common": {
                return "#3a3a3a";
            }
        }
        return "#3a3a3a";
    }

    private String translateAttackType(String attackType, String language) {
        String translationKey;
        String translated;
        String cleanName = attackType.replace("_Damage", "");
        if (cleanName.contains("_Signature_")) {
            cleanName = cleanName.replace("_Signature_", language.equals("es") ? "_Especial_" : "_Special_");
        }
        if ((translated = WeaponInfoTranslations.get(language, translationKey = "attack_" + cleanName)) != null && !translated.equals(translationKey)) {
            return translated;
        }
        if (cleanName.startsWith("Knife_")) {
            String suffix = cleanName.replace("Knife_", "").replace("_", " ");
            return language.equals("es") ? "Cuchillo: " + suffix : "Knife: " + suffix;
        }
        return cleanName.replace("_", " ");
    }

    public static class SearchData {
        private String searchQuery;
        public static final BuilderCodec<SearchData> CODEC = ((BuilderCodec.Builder)BuilderCodec.builder(SearchData.class, SearchData::new).addField(new KeyedCodec("@SearchQuery", (Codec)Codec.STRING), (data, s) -> {
            data.searchQuery = s;
        }, data -> data.searchQuery)).build();
    }
}

