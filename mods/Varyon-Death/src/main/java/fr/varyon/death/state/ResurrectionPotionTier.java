package fr.varyon.death.etat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import fr.varyon.death.config.ConfigDeath;

/**
 * Les niveaux de potion de resurrection, du plus faible au plus fort.
 *
 * <p>L'ordre de declaration EST l'ordre de puissance : {@link #compareTo} s'en sert
 * directement pour determiner « le plus fort prime » entre plusieurs potions detenues par
 * les soigneurs participants.
 */
public enum TierPotionResurrection {

    MINEURE("Varyon_Potion_Resurrection_Mineure", "potion de résurrection mineure", 10) {
        @Override
        public int dureeTicks(@Nonnull ConfigDeath config) {
            return config.getDureeReleveMineureTicks();
        }
    },
    CLASSIQUE("Varyon_Potion_Resurrection_Classique", "potion de résurrection", 30) {
        @Override
        public int dureeTicks(@Nonnull ConfigDeath config) {
            return config.getDureeReleveClassiqueTicks();
        }
    },
    MAJEURE("Varyon_Potion_Resurrection_Majeure", "potion de résurrection majeure", 50) {
        @Override
        public int dureeTicks(@Nonnull ConfigDeath config) {
            return config.getDureeReleveMajeureTicks();
        }
    },
    MYTHIQUE("Varyon_Potion_Resurrection_Mythique", "potion de résurrection mythique", 100) {
        @Override
        public int dureeTicks(@Nonnull ConfigDeath config) {
            return config.getDureeReleveMythiqueTicks();
        }
    };

    private final String itemId;
    private final String nomAffiche;
    private final int pourcentagePv;

    TierPotionResurrection(@Nonnull String itemId, @Nonnull String nomAffiche, int pourcentagePv) {
        this.itemId = itemId;
        this.nomAffiche = nomAffiche;
        this.pourcentagePv = pourcentagePv;
    }

    @Nonnull
    public String itemId() {
        return itemId;
    }

    @Nonnull
    public String nomAffiche() {
        return nomAffiche;
    }

    /** Duree de relevement fixee par ce tier, pour un seul soigneur. */
    public abstract int dureeTicks(@Nonnull ConfigDeath config);

    /** Pourcentage des PV maximum rendus au releve par ce tier : 10/30/50/100 %. */
    public int pourcentagePv() {
        return pourcentagePv;
    }

    @Nullable
    public static TierPotionResurrection depuisItemId(@Nullable String itemId) {
        if (itemId == null) {
            return null;
        }
        for (TierPotionResurrection tier : values()) {
            if (tier.itemId.equals(itemId)) {
                return tier;
            }
        }
        return null;
    }

    /** Le plus fort des deux tiers, {@code null} si aucun n'est fourni. */
    @Nullable
    public static TierPotionResurrection meilleur(@Nullable TierPotionResurrection a,
                                                   @Nullable TierPotionResurrection b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.compareTo(b) >= 0 ? a : b;
    }
}
