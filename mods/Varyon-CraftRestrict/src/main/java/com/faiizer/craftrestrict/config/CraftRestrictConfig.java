package com.faiizer.craftrestrict.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import java.util.LinkedHashMap;
import java.util.Map;

public class CraftRestrictConfig {

    public static final BuilderCodec<CraftRestrictConfig> CODEC = BuilderCodec.builder(CraftRestrictConfig.class, CraftRestrictConfig::new)
            .append(new KeyedCodec<>("RestrictionMode", Codec.STRING),
                    (config, value, extraInfo) -> config.RestrictionMode = value,
                    (config, extraInfo) -> config.RestrictionMode).add()
            .append(new KeyedCodec<>("Debug", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.Debug = value,
                    (config, extraInfo) -> config.Debug).add()
            .append(new KeyedCodec<>("LockedItemsAppearsInCraftingList", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.LockedItemsAppearsInCraftingList = value,
                    (config, extraInfo) -> config.LockedItemsAppearsInCraftingList).add()
            .append(new KeyedCodec<>("SendRecipeDenyMessage", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.SendRecipeDenyMessage = value,
                    (config, extraInfo) -> config.SendRecipeDenyMessage).add()
            .append(new KeyedCodec<>("SendRecipeDenySound", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.SendRecipeDenySound = value,
                    (config, extraInfo) -> config.SendRecipeDenySound).add()
            .append(new KeyedCodec<>("RecipeDenyMessage", Codec.STRING),
                    (config, value, extraInfo) -> config.RecipeDenyMessage = value,
                    (config, extraInfo) -> config.RecipeDenyMessage).add()
            .append(new KeyedCodec<>("RecipeDenySound", Codec.STRING),
                    (config, value, extraInfo) -> config.RecipeDenySound = value,
                    (config, extraInfo) -> config.RecipeDenySound).add()
            .append(new KeyedCodec<>("SendBenchDenyMessage", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.SendBenchDenyMessage = value,
                    (config, extraInfo) -> config.SendBenchDenyMessage).add()
            .append(new KeyedCodec<>("SendBenchDenySound", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.SendBenchDenySound = value,
                    (config, extraInfo) -> config.SendBenchDenySound).add()
            .append(new KeyedCodec<>("BenchDenyMessage", Codec.STRING),
                    (config, value, extraInfo) -> config.BenchDenyMessage = value,
                    (config, extraInfo) -> config.BenchDenyMessage).add()
            .append(new KeyedCodec<>("BenchDenySound", Codec.STRING),
                    (config, value, extraInfo) -> config.BenchDenySound = value,
                    (config, extraInfo) -> config.BenchDenySound).add()
            .append(new KeyedCodec<>("RestrictionRules", new MapCodec<>(RestrictionRule.CODEC, LinkedHashMap::new)),
                    (config, value, extraInfo) -> config.RestrictionRules = value,
                    (config, extraInfo) -> config.RestrictionRules).add()
            .build();

    private String RestrictionMode = "DENY";
    private boolean Debug = false;
    private boolean LockedItemsAppearsInCraftingList = true;
    private boolean SendRecipeDenyMessage = true;
    private boolean SendRecipeDenySound = true;
    private String RecipeDenyMessage = "Tu ne peux pas crafter cet objet !";
    private String RecipeDenySound = "SFX_Antelope_Alerted";
    private boolean SendBenchDenyMessage = true;
    private boolean SendBenchDenySound = true;
    private String BenchDenyMessage = "Tu ne peux pas utiliser cet établi !";
    private String BenchDenySound = "SFX_Antelope_Alerted";
    private Map<String, RestrictionRule> RestrictionRules = new LinkedHashMap<>();

    public String getRestrictionMode() {
        return this.RestrictionMode;
    }

    public boolean isDebug() {
        return this.Debug;
    }

    public boolean isLockedItemsAppearsInCraftingList() {
        return this.LockedItemsAppearsInCraftingList;
    }

    public boolean isSendRecipeDenyMessage() {
        return this.SendRecipeDenyMessage;
    }

    public boolean isSendRecipeDenySound() {
        return this.SendRecipeDenySound;
    }

    public String getRecipeDenyMessage() {
        return this.RecipeDenyMessage;
    }

    public String getRecipeDenySound() {
        return this.RecipeDenySound;
    }

    public boolean isSendBenchDenyMessage() {
        return this.SendBenchDenyMessage;
    }

    public boolean isSendBenchDenySound() {
        return this.SendBenchDenySound;
    }

    public String getBenchDenyMessage() {
        return this.BenchDenyMessage;
    }

    public String getBenchDenySound() {
        return this.BenchDenySound;
    }

    public Map<String, RestrictionRule> getRestrictionRules() {
        return this.RestrictionRules;
    }

    public void setRestrictionRules(Map<String, RestrictionRule> rules) {
        this.RestrictionRules = rules;
    }
}
