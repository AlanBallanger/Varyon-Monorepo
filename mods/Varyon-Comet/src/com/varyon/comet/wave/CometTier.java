package com.varyon.comet.wave;

import java.util.Locale;

public enum CometTier {
    UNCOMMON("Common", "Common"),
    EPIC("Epic", "Epic"),
    RARE("Rare", "Rare"),
    LEGENDARY("Legendary", "Legendary"),
    MYTHIC("Mythic", "Mythic");

    private final String displayName;
    private final String assetToken;

    CometTier(String displayName, String assetToken) {
        this.displayName = displayName;
        this.assetToken = assetToken;
    }

    public String getName() {
        return displayName;
    }

    public String getAssetToken() {
        return assetToken;
    }

    public String getDisplayName() {
        switch (this) {
            case UNCOMMON:
                return "Commune";
            case RARE:
                return "Rare";
            case EPIC:
                return "Épique";
            case LEGENDARY:
                return "Légendaire";
            case MYTHIC:
                return "Mythique";
            default:
                return displayName;
        }
    }

    public String getTierAdjectiveForCometeAnnouncement() {
        switch (this) {
            case UNCOMMON:
                return "commune";
            case RARE:
                return "rare";
            case EPIC:
                return "épique";
            case LEGENDARY:
                return "légendaire";
            case MYTHIC:
                return "mythique";
            default:
                return "commune";
        }
    }

    public String applyTierPlaceholders(String template) {
        if (template == null) {
            return null;
        }
        String adj = getTierAdjectiveForCometeAnnouncement();
        return template.replace("%tier_lc%", adj).replace("%tier%", adj);
    }

    public static CometTier fromString(String tierName) {
        if (tierName == null) {
            return UNCOMMON;
        }

        String n = tierName.trim().toLowerCase(Locale.ROOT);
        for (CometTier tier : values()) {
            if (tier.displayName.toLowerCase(Locale.ROOT).equals(n)) {
                return tier;
            }
            if (tier.assetToken.toLowerCase(Locale.ROOT).equals(n)) {
                return tier;
            }
        }

        switch (n) {
            case "peu commun":
            case "peu-commun":
            case "commun":
            case "commune":
            case "uncommon":
                return UNCOMMON;
            case "épique":
            case "epique":
                return EPIC;
            case "légendaire":
            case "legendaire":
                return LEGENDARY;
            case "mythique":
                return MYTHIC;
            default:
                return UNCOMMON;
        }
    }

    public String getAssetSuffix() {
        return "_" + assetToken;
    }

    public String getBlockId(String baseName) {
        return baseName + getAssetSuffix();
    }

    public String getShardId() {
        return getBlockId("Comet_Shard");
    }

    public String getLootTableName() {
        return "Comet_Rewards" + getAssetSuffix();
    }

    public String getFallingProjectileConfig() {
        return "Comet_Falling" + getAssetSuffix();
    }

    public String getExplosionParticleSystem() {
        return "Comet_Explosion_Large" + getAssetSuffix();
    }

    public String getBeamParticleSystem() {
        return "Comet_Beam" + getAssetSuffix();
    }
}
