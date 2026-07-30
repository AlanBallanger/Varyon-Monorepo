package com.faiizer.craftrestrict.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class CraftRestrictEventData {

    public static final BuilderCodec<CraftRestrictEventData> CODEC =
            BuilderCodec.builder(CraftRestrictEventData.class, CraftRestrictEventData::new)
                    .addField(new KeyedCodec<>("Action", Codec.STRING),
                            (d, v) -> d.action = v, d -> d.action)
                    .addField(new KeyedCodec<>("ItemId", Codec.STRING),
                            (d, v) -> d.itemId = v, d -> d.itemId)
                    .addField(new KeyedCodec<>("RuleId", Codec.STRING),
                            (d, v) -> d.ruleId = v, d -> d.ruleId)
                    .addField(new KeyedCodec<>("@SearchQuery", Codec.STRING),
                            (d, v) -> d.searchQuery = v, d -> d.searchQuery)
                    .addField(new KeyedCodec<>("@EditPermission", Codec.STRING),
                            (d, v) -> d.editPermission = v, d -> d.editPermission)
                    .build();

    String action;
    String itemId;
    String ruleId;
    String searchQuery;
    String editPermission;

    public String getAction() {
        return action;
    }

    public String getItemId() {
        return itemId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public String getEditPermission() {
        return editPermission;
    }
}
