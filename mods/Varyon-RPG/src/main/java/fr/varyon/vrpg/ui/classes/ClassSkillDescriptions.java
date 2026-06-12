package fr.varyon.vrpg.ui.classes;

import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ability.AssautEclairSkill;
import fr.varyon.vrpg.classes.ability.ClassSkillRegistry;
import fr.varyon.vrpg.classes.ability.ExpertEnDuelSkill;
import fr.varyon.vrpg.classes.duelliste.AssautBretteurSkill;
import fr.varyon.vrpg.classes.duelliste.CoupEstocSkill;
import fr.varyon.vrpg.classes.duelliste.DesarmementSkill;
import fr.varyon.vrpg.classes.duelliste.DuellistePassifs;
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
import java.util.Locale;

public final class ClassSkillDescriptions {

    private ClassSkillDescriptions() {}

    @Nullable
    public static String skillIdForNode(@Nullable ClassAccount acc, int nodeIndex) {
        return ClassSkillRegistry.skillIdForNode(acc, nodeIndex);
    }

    @Nonnull
    public static String effectText(@Nullable String skillId, @Nonnull String baseDescription) {
        return baseDescription.trim();
    }

    @Nullable
    public static String statLineForRank(@Nullable String skillId, int rank) {
        if (rank < 1 || skillId == null) return null;
        return switch (skillId) {
            case AssautEclairSkill.SKILL_ID      -> assautEclairLine(rank);
            case ExpertEnDuelSkill.SKILL_ID      -> ExpertEnDuelSkill.statLineForRank(rank);
            case AssautBretteurSkill.SKILL_ID    -> AssautBretteurSkill.statLineForRank(rank);
            case DesarmementSkill.SKILL_ID       -> DesarmementSkill.statLineForRank(rank);
            case CoupEstocSkill.SKILL_ID         -> CoupEstocSkill.statLineForRank(rank);
            case FeintSkill.SKILL_ID             -> FeintSkill.statLineForRank(rank);
            case RiposteParfaiteSkill.SKILL_ID   -> RiposteParfaiteSkill.statLineForRank(rank);
            case DuellistePassifs.BLESSURE_NODE -> DuellistePassifs.bleedStatLine(rank);
            case DuellistePassifs.FRAPPE_NODE   -> DuellistePassifs.critStatLine(rank);
            case DuellistePassifs.CONTRE_NODE   -> DuellistePassifs.contreStatLine(rank);
            case DuellistePassifs.ESQUIVE_NODE  -> DuellistePassifs.dodgeStatLine(rank);
            case DuellistePassifs.MOMENTUM_NODE -> DuellistePassifs.momentumStatLine(rank);
            case PasDesTenebresSkill.SKILL_ID    -> PasDesTenebresSkill.statLineForRank(rank);
            case EcranDeFumeeSkill.SKILL_ID      -> EcranDeFumeeSkill.statLineForRank(rank);
            case FrappeFataleSkill.SKILL_ID      -> FrappeFataleSkill.statLineForRank(rank);
            case DelugeDeGamesSkill.SKILL_ID     -> DelugeDeGamesSkill.statLineForRank(rank);
            case PasDeLOmbreSkill.SKILL_ID       -> PasDeLOmbreSkill.statLineForRank(rank);
            case ChaseOuverteSkill.SKILL_ID      -> ChaseOuverteSkill.statLineForRank(rank);
            case OmbrePassifs.EXECUTION_RAPIDE_NODE  -> OmbrePassifs.executionStatLine(rank);
            case OmbrePassifs.DANSE_LAMES_NODE       -> OmbrePassifs.danseStatLine(rank);
            case OmbrePassifs.LAMES_EMPOISONNEES_NODE-> OmbrePassifs.poisonStatLine(rank);
            case OmbrePassifs.EMBUSCADE_NODE         -> OmbrePassifs.embuscadeStatLine(rank);
            case OmbrePassifs.OMBRE_INSAISISSABLE_NODE -> OmbrePassifs.ombreInsaisissableStatLine(rank);
            case OmbrePassifs.INSTINCT_SURVIE_NODE   -> OmbrePassifs.instinctSurvieStatLine(rank);
            case fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.SKILL_ID      -> fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.SKILL_ID    -> fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.rempart.ForteresseSkill.SKILL_ID        -> fr.varyon.vrpg.classes.rempart.ForteresseSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.SKILL_ID     -> fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.SKILL_ID    -> fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.rempart.ProvocationSkill.SKILL_ID       -> fr.varyon.vrpg.classes.rempart.ProvocationSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.rempart.RempartPassifs.MAITRE_BOUCLIER_NODE     -> fr.varyon.vrpg.classes.rempart.RempartPassifs.maitreBouclierStatLine(rank);
            case fr.varyon.vrpg.classes.rempart.RempartPassifs.CONSTITUTION_NODE        -> fr.varyon.vrpg.classes.rempart.RempartPassifs.constitutionStatLine(rank);
            case fr.varyon.vrpg.classes.rempart.RempartPassifs.GARDE_IMPENETRABLE_NODE  -> fr.varyon.vrpg.classes.rempart.RempartPassifs.gardeImpenetrableStatLine(rank);
            case fr.varyon.vrpg.classes.rempart.RempartPassifs.INFATIGABLE_NODE         -> fr.varyon.vrpg.classes.rempart.RempartPassifs.infatigableStatLine(rank);
            case fr.varyon.vrpg.classes.rempart.RempartPassifs.CONTRE_OFFENSIF_NODE     -> fr.varyon.vrpg.classes.rempart.RempartPassifs.contreOffensifStatLine(rank);
            case fr.varyon.vrpg.classes.rempart.RempartPassifs.DERNIER_BASTION_NODE     -> fr.varyon.vrpg.classes.rempart.RempartPassifs.dernierBastionStatLine(rank);
            // Berserker
            case fr.varyon.vrpg.classes.berserker.AssautBestialSkill.SKILL_ID        -> fr.varyon.vrpg.classes.berserker.AssautBestialSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.berserker.DechiquetageSkill.SKILL_ID               -> fr.varyon.vrpg.classes.berserker.DechiquetageSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.berserker.CriRalliementSkill.SKILL_ID       -> fr.varyon.vrpg.classes.berserker.CriRalliementSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.berserker.ExecutionSauvageSkill.SKILL_ID    -> fr.varyon.vrpg.classes.berserker.ExecutionSauvageSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.berserker.DixPourSangSkill.SKILL_ID         -> fr.varyon.vrpg.classes.berserker.DixPourSangSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.berserker.CorDeGuerreSkill.SKILL_ID         -> fr.varyon.vrpg.classes.berserker.CorDeGuerreSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.berserker.BerserkerPassifs.DERNIER_SOUFFLE_NODE -> fr.varyon.vrpg.classes.berserker.BerserkerPassifs.dernierSouffleStatLine();
            case fr.varyon.vrpg.classes.berserker.BerserkerPassifs.FERVEUR_NODE     -> fr.varyon.vrpg.classes.berserker.BerserkerPassifs.ferveurStatLine(rank);
            case fr.varyon.vrpg.classes.berserker.BerserkerPassifs.FUREUR_NODE      -> fr.varyon.vrpg.classes.berserker.BerserkerPassifs.fureurStatLine(rank);
            case fr.varyon.vrpg.classes.berserker.BerserkerPassifs.CARNAGE_NODE     -> fr.varyon.vrpg.classes.berserker.BerserkerPassifs.carnageStatLine(rank);
            case fr.varyon.vrpg.classes.berserker.BerserkerPassifs.BLESSURES_NODE   -> fr.varyon.vrpg.classes.berserker.BerserkerPassifs.blessuresStatLine(rank);
            case fr.varyon.vrpg.classes.berserker.BerserkerPassifs.FRENESIE_NODE    -> fr.varyon.vrpg.classes.berserker.BerserkerPassifs.frenesieStatLine(rank);
            // Ravageur
            case fr.varyon.vrpg.classes.ravageur.BondEcrasantSkill.SKILL_ID          -> fr.varyon.vrpg.classes.ravageur.BondEcrasantSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.ravageur.PeauDeFerSkill.SKILL_ID             -> fr.varyon.vrpg.classes.ravageur.PeauDeFerSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.ravageur.DechainementSkill.SKILL_ID          -> fr.varyon.vrpg.classes.ravageur.DechainementSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.ravageur.PremierAssautSkill.SKILL_ID         -> fr.varyon.vrpg.classes.ravageur.PremierAssautSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.ravageur.MarteauPilonSkill.SKILL_ID          -> fr.varyon.vrpg.classes.ravageur.MarteauPilonSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.ravageur.RabattageSkill.SKILL_ID             -> fr.varyon.vrpg.classes.ravageur.RabattageSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.ravageur.RavageurPassifs.MOISSONNEUR_NODE    -> fr.varyon.vrpg.classes.ravageur.RavageurPassifs.moissonneurStatLine(rank);
            case fr.varyon.vrpg.classes.ravageur.RavageurPassifs.ARME_LOURDE_NODE    -> fr.varyon.vrpg.classes.ravageur.RavageurPassifs.armeLourdeStatLine(rank);
            case fr.varyon.vrpg.classes.ravageur.RavageurPassifs.EXECUTEUR_NODE      -> fr.varyon.vrpg.classes.ravageur.RavageurPassifs.executeurStatLine(rank);
            case fr.varyon.vrpg.classes.ravageur.RavageurPassifs.CHASSEUR_GEANT_NODE -> fr.varyon.vrpg.classes.ravageur.RavageurPassifs.chasseurGeantStatLine(rank);
            case fr.varyon.vrpg.classes.ravageur.RavageurPassifs.COMBATTANT_INFATIGABLE_NODE -> fr.varyon.vrpg.classes.ravageur.RavageurPassifs.combattantInfatigableStatLine(rank);
            case fr.varyon.vrpg.classes.ravageur.RavageurPassifs.ELAN_DESTRUCTEUR_NODE -> fr.varyon.vrpg.classes.ravageur.RavageurPassifs.elanDestructeurStatLine(rank);
            default -> null;
        };
    }

    private static String assautEclairLine(int rank) {
        int pct     = Math.round(AssautEclairSkill.damageFactor(rank) * 100);
        String cd   = formatCooldownShort(AssautEclairSkill.cooldownMsForRank(rank));
        int stamina = Math.round(AssautEclairSkill.staminaCostForRank(rank));
        return pct + "% dégâts arme, CD " + cd + " - " + stamina + " endurance";
    }

    private static String formatCooldownShort(long cooldownMs) {
        if (cooldownMs % 1000L == 0L) {
            return (cooldownMs / 1000L) + "s";
        }
        return String.format(Locale.ROOT, "%.1fs", cooldownMs / 1000.0);
    }
}
