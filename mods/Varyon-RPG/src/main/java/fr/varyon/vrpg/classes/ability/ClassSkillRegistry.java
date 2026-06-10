package fr.varyon.vrpg.classes.ability;

import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.classes.duelliste.AssautBretteurSkill;
import fr.varyon.vrpg.classes.duelliste.DesarmementSkill;
import fr.varyon.vrpg.classes.duelliste.DuellistePassifs;
import fr.varyon.vrpg.classes.duelliste.CoupEstocSkill;
import fr.varyon.vrpg.classes.duelliste.FeintSkill;
import fr.varyon.vrpg.classes.duelliste.RiposteParfaiteSkill;
import fr.varyon.vrpg.classes.ombre.PasDesTenebresSkill;
import fr.varyon.vrpg.classes.ombre.EcranDeFumeeSkill;
import fr.varyon.vrpg.classes.ombre.FrappeFataleSkill;
import fr.varyon.vrpg.classes.ombre.DelugeDeGamesSkill;
import fr.varyon.vrpg.classes.ombre.PasDeLOmbreSkill;
import fr.varyon.vrpg.classes.ombre.ChaseOuverteSkill;
import fr.varyon.vrpg.classes.ombre.OmbrePassifs;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ClassSkillRegistry {

    private ClassSkillRegistry() {}

    @Nullable
    public static String resolveSkillId(@Nullable ClassAccount acc, @Nullable String itemId) {
        return ClassSkillSlots.resolveSkillId(acc, itemId);
    }

    @Nullable
    public static String skillIdForNode(@Nullable ClassAccount acc, int nodeIndex) {
        if (acc == null) return null;
        PlayerClass activeClass = acc.getActiveClass();
        if (activeClass == null) return null;
        PlayerSpecialization spec = acc.getActiveSpec(activeClass);
        if (activeClass == PlayerClass.GUERRIER && spec == PlayerSpecialization.DUELLISTE) {
            return switch (nodeIndex) {
                case 0  -> AssautEclairSkill.SKILL_ID;
                case 1  -> ExpertEnDuelSkill.SKILL_ID;
                case 2  -> DuellistePassifs.BLESSURE_NODE;
                case 3  -> CoupEstocSkill.SKILL_ID;
                case 4  -> RiposteParfaiteSkill.SKILL_ID;
                case 5  -> DuellistePassifs.CONTRE_NODE;
                case 6  -> FeintSkill.SKILL_ID;
                case 7  -> DuellistePassifs.ESQUIVE_NODE;
                case 8  -> DuellistePassifs.FRAPPE_NODE;
                case 9  -> DesarmementSkill.SKILL_ID;
                case 10 -> DuellistePassifs.MOMENTUM_NODE;
                case 11 -> AssautBretteurSkill.SKILL_ID;
                default -> null;
            };
        }
        if (activeClass == PlayerClass.GUERRIER && spec == PlayerSpecialization.OMBRE) {
            return switch (nodeIndex) {
                case 0  -> PasDesTenebresSkill.SKILL_ID;         // Actif
                case 1  -> OmbrePassifs.EXECUTION_RAPIDE_NODE;   // Passif
                case 2  -> OmbrePassifs.LAMES_EMPOISONNEES_NODE; // Passif
                case 3  -> DelugeDeGamesSkill.SKILL_ID;          // Actif
                case 4  -> EcranDeFumeeSkill.SKILL_ID;           // Actif
                case 5  -> OmbrePassifs.EMBUSCADE_NODE;          // Passif
                case 6  -> FrappeFataleSkill.SKILL_ID;           // Actif
                case 7  -> OmbrePassifs.OMBRE_INSAISISSABLE_NODE;// Passif
                case 8  -> OmbrePassifs.DANSE_LAMES_NODE;        // Passif
                case 9  -> PasDeLOmbreSkill.SKILL_ID;            // Actif
                case 10 -> OmbrePassifs.INSTINCT_SURVIE_NODE;    // Passif
                case 11 -> ChaseOuverteSkill.SKILL_ID;           // Actif
                default -> null;
            };
        }
        return null;
    }
}
