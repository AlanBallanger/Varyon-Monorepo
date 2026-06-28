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

import com.hypixel.hytale.server.core.Message;

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

    @Nonnull
    public static String tooltipPlainText(@Nonnull String name, @Nonnull String baseDescription) {
        return name.trim() + "\n\n" + baseDescription.trim();
    }

    @Nonnull
    public static Message tooltipMessage(@Nonnull String name,
                                         @Nullable String skillId,
                                         @Nonnull String baseDescription) {
        return Message.join(
            Message.raw(name.trim()).bold(true),
            Message.raw("\n\n"),
            Message.raw(effectText(skillId, baseDescription))
        );
    }

    @Nullable
    public static SkillStatDisplay statDisplayForRank(@Nullable String skillId, int rank) {
        return ClassSkillStatResolver.resolve(skillId, rank, statLineForRank(skillId, rank));
    }

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
            case fr.varyon.vrpg.classes.berserker.BerserkerPassifs.DERNIER_SOUFFLE_NODE -> fr.varyon.vrpg.classes.berserker.BerserkerPassifs.dernierSouffleStatLine(rank);
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
            // Bagarreur
            case fr.varyon.vrpg.classes.bagarreur.JeuDeJambesSkill.SKILL_ID           -> fr.varyon.vrpg.classes.bagarreur.JeuDeJambesSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.bagarreur.MonteeAdreinalineSkill.SKILL_ID     -> fr.varyon.vrpg.classes.bagarreur.MonteeAdreinalineSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.bagarreur.DirectDuDroitSkill.SKILL_ID         -> fr.varyon.vrpg.classes.bagarreur.DirectDuDroitSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.bagarreur.DelugeDeCoups2Skill.SKILL_ID        -> fr.varyon.vrpg.classes.bagarreur.DelugeDeCoups2Skill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.bagarreur.SecondSouffleSkill.SKILL_ID         -> fr.varyon.vrpg.classes.bagarreur.SecondSouffleSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.bagarreur.UppercutSkill.SKILL_ID              -> fr.varyon.vrpg.classes.bagarreur.UppercutSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.bagarreur.BagarreurPassifs.JUSQUAU_BOUT_NODE  -> fr.varyon.vrpg.classes.bagarreur.BagarreurPassifs.jusquAuBoutStatLine(rank);
            case fr.varyon.vrpg.classes.bagarreur.BagarreurPassifs.ADRENALINE_NODE    -> fr.varyon.vrpg.classes.bagarreur.BagarreurPassifs.adrenalineStatLine(rank);
            case fr.varyon.vrpg.classes.bagarreur.BagarreurPassifs.GARDE_BOXEUR_NODE  -> fr.varyon.vrpg.classes.bagarreur.BagarreurPassifs.gardeBoxeurStatLine(rank);
            case fr.varyon.vrpg.classes.bagarreur.BagarreurPassifs.ACHARNEMENT_NODE    -> fr.varyon.vrpg.classes.bagarreur.BagarreurPassifs.acharnementStatLine(rank);
            case fr.varyon.vrpg.classes.bagarreur.BagarreurPassifs.ESPRIT_COMBATIF_NODE -> fr.varyon.vrpg.classes.bagarreur.BagarreurPassifs.espritCombatifStatLine(rank);
            case fr.varyon.vrpg.classes.bagarreur.BagarreurPassifs.POINGS_ACIER_NODE  -> fr.varyon.vrpg.classes.bagarreur.BagarreurPassifs.poingsAcierStatLine(rank);
            // Arcaniste
            case fr.varyon.vrpg.classes.arcaniste.DistorsionSkill.SKILL_ID       -> fr.varyon.vrpg.classes.arcaniste.DistorsionSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.arcaniste.BouleDeFeuSkill.SKILL_ID       -> fr.varyon.vrpg.classes.arcaniste.BouleDeFeuSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.SKILL_ID          -> fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.arcaniste.NovaDeGivreSkill.SKILL_ID      -> fr.varyon.vrpg.classes.arcaniste.NovaDeGivreSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.arcaniste.SurchargeSkill.SKILL_ID        -> fr.varyon.vrpg.classes.arcaniste.SurchargeSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.arcaniste.SalveDeGivreSkill.SKILL_ID     -> fr.varyon.vrpg.classes.arcaniste.SalveDeGivreSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.TALENT_INNE_NODE         -> fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.talentInneStatLine(rank);
            case fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.ECHO_TEMPOREL_NODE       -> fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.echoTemporelStatLine(rank);
            case fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.PUITS_MANA_NODE          -> fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.puitsManaStatLine(rank);
            case fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.DRAIN_MYSTIQUE_NODE      -> fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.drainMystiqueStatLine(rank);
            case fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.ECHO_ARCANIQUE_NODE      -> fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.echoArcanicStatLine(rank);
            case fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.POUVOIR_GRANDISSANT_NODE -> fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.pouvoirGrandissantStatLine(rank);
            // Vaudou
            case fr.varyon.vrpg.classes.vaudou.PassageEthereSkill.SKILL_ID      -> fr.varyon.vrpg.classes.vaudou.PassageEthereSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.vaudou.FleauToxiqueSkill.SKILL_ID       -> fr.varyon.vrpg.classes.vaudou.FleauToxiqueSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.vaudou.TotemEntraveSkill.SKILL_ID       -> fr.varyon.vrpg.classes.vaudou.TotemEntraveSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.vaudou.AttaquePerfideSkill.SKILL_ID     -> fr.varyon.vrpg.classes.vaudou.AttaquePerfideSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.vaudou.TotemVulnerabiliteSkill.SKILL_ID -> fr.varyon.vrpg.classes.vaudou.TotemVulnerabiliteSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.vaudou.ExtractionAmeSkill.SKILL_ID      -> fr.varyon.vrpg.classes.vaudou.ExtractionAmeSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.vaudou.VaudouPassifs.FETICHEUR_NODE     -> fr.varyon.vrpg.classes.vaudou.VaudouPassifs.feticheurStatLine(rank);
            case fr.varyon.vrpg.classes.vaudou.VaudouPassifs.PRESENCE_NODE      -> fr.varyon.vrpg.classes.vaudou.VaudouPassifs.presenceStatLine(rank);
            case fr.varyon.vrpg.classes.vaudou.VaudouPassifs.TOXINES_NODE       -> fr.varyon.vrpg.classes.vaudou.VaudouPassifs.toxinesStatLine(rank);
            case fr.varyon.vrpg.classes.vaudou.VaudouPassifs.ANCRAGE_NODE       -> fr.varyon.vrpg.classes.vaudou.VaudouPassifs.ancrageStatLine(rank);
            case fr.varyon.vrpg.classes.vaudou.VaudouPassifs.RITUEL_NODE        -> fr.varyon.vrpg.classes.vaudou.VaudouPassifs.rituelStatLine(rank);
            case fr.varyon.vrpg.classes.vaudou.VaudouPassifs.PARASITE_NODE      -> fr.varyon.vrpg.classes.vaudou.VaudouPassifs.parasiteStatLine(rank);
            // Rôdeur
            case fr.varyon.vrpg.classes.rodeur.ReculStrategiqueSkill.SKILL_ID    -> fr.varyon.vrpg.classes.rodeur.ReculStrategiqueSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.rodeur.PluieDesFlechesSkill.SKILL_ID     -> fr.varyon.vrpg.classes.rodeur.PluieDesFlechesSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.rodeur.MarqueDuChasseurSkill.SKILL_ID    -> fr.varyon.vrpg.classes.rodeur.MarqueDuChasseurSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.rodeur.FlecheDeReculsSkill.SKILL_ID      -> fr.varyon.vrpg.classes.rodeur.FlecheDeReculsSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.rodeur.FlecheEntravantSkill.SKILL_ID     -> fr.varyon.vrpg.classes.rodeur.FlecheEntravantSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.rodeur.RafaleSkill.SKILL_ID              -> fr.varyon.vrpg.classes.rodeur.RafaleSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.rodeur.RodeurPassifs.OEIL_CHASSEUR_NODE  -> fr.varyon.vrpg.classes.rodeur.RodeurPassifs.oeilStatLine(rank);
            case fr.varyon.vrpg.classes.rodeur.RodeurPassifs.FLECHES_TOXIQUES_NODE -> fr.varyon.vrpg.classes.rodeur.RodeurPassifs.poisonStatLine(rank);
            case fr.varyon.vrpg.classes.rodeur.RodeurPassifs.INSTINCT_SURVIE_NODE  -> fr.varyon.vrpg.classes.rodeur.RodeurPassifs.instinctStatLine(rank);
            case fr.varyon.vrpg.classes.rodeur.RodeurPassifs.PRECISION_MORTELLE_NODE -> fr.varyon.vrpg.classes.rodeur.RodeurPassifs.precisionStatLine(rank);
            case fr.varyon.vrpg.classes.rodeur.RodeurPassifs.TRAQUE_MOBILE_NODE    -> fr.varyon.vrpg.classes.rodeur.RodeurPassifs.traqueMobileStatLine(rank);
            case fr.varyon.vrpg.classes.rodeur.RodeurPassifs.TRAQUE_SANS_FIN_NODE  -> fr.varyon.vrpg.classes.rodeur.RodeurPassifs.traqueSansFinStatLine(rank);
            // Arbalétrier
            case fr.varyon.vrpg.classes.arbaletrier.ReculTactiqueSkill.SKILL_ID      -> fr.varyon.vrpg.classes.arbaletrier.ReculTactiqueSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.arbaletrier.CarreauLourdSkill.SKILL_ID       -> fr.varyon.vrpg.classes.arbaletrier.CarreauLourdSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.arbaletrier.CarreauExplosifSkill.SKILL_ID    -> fr.varyon.vrpg.classes.arbaletrier.CarreauExplosifSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.arbaletrier.CarreauTranspercantSkill.SKILL_ID -> fr.varyon.vrpg.classes.arbaletrier.CarreauTranspercantSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.arbaletrier.CoupDeBotteSkill.SKILL_ID        -> fr.varyon.vrpg.classes.arbaletrier.CoupDeBotteSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.arbaletrier.MiseEnJouSkill.SKILL_ID          -> fr.varyon.vrpg.classes.arbaletrier.MiseEnJouSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.arbaletrier.ArbaietrierPassifs.TIREUR_ELITE_NODE      -> fr.varyon.vrpg.classes.arbaletrier.ArbaietrierPassifs.eliteStatLine(rank);
            case fr.varyon.vrpg.classes.arbaletrier.ArbaietrierPassifs.CHASSEUR_COLOSSES_NODE -> fr.varyon.vrpg.classes.arbaletrier.ArbaietrierPassifs.colossesStatLine(rank);
            case fr.varyon.vrpg.classes.arbaletrier.ArbaietrierPassifs.TIREUR_EMBUSQUE_NODE   -> fr.varyon.vrpg.classes.arbaletrier.ArbaietrierPassifs.embusqueStatLine(rank);
            case fr.varyon.vrpg.classes.arbaletrier.ArbaietrierPassifs.VISEUR_NODE             -> fr.varyon.vrpg.classes.arbaletrier.ArbaietrierPassifs.viseurStatLine(rank);
            case fr.varyon.vrpg.classes.arbaletrier.ArbaietrierPassifs.CARREAUX_LACERANTS_NODE -> fr.varyon.vrpg.classes.arbaletrier.ArbaietrierPassifs.bleedStatLine(rank);
            case fr.varyon.vrpg.classes.arbaletrier.ArbaietrierPassifs.REFLEXES_AFFUTES_NODE  -> fr.varyon.vrpg.classes.arbaletrier.ArbaietrierPassifs.reflexesStatLine(rank);
            // Gardien de Gaïa
            case fr.varyon.vrpg.classes.gardiendesgaia.EvasionSylvestreSkill.SKILL_ID      -> fr.varyon.vrpg.classes.gardiendesgaia.EvasionSylvestreSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.gardiendesgaia.BenedictionDeGaiaSkill.SKILL_ID     -> fr.varyon.vrpg.classes.gardiendesgaia.BenedictionDeGaiaSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.gardiendesgaia.MarqueDeRenaissanceSkill.SKILL_ID   -> fr.varyon.vrpg.classes.gardiendesgaia.MarqueDeRenaissanceSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.gardiendesgaia.EtreinteDeGaiaSkill.SKILL_ID        -> fr.varyon.vrpg.classes.gardiendesgaia.EtreinteDeGaiaSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.gardiendesgaia.AppelDuTreantSkill.SKILL_ID         -> fr.varyon.vrpg.classes.gardiendesgaia.AppelDuTreantSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.gardiendesgaia.EcorceProtectriceSkill.SKILL_ID     -> fr.varyon.vrpg.classes.gardiendesgaia.EcorceProtectriceSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaPassifs.HARMONIE_NODE  -> fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaPassifs.harmonieStatLine(rank);
            case fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaPassifs.SOUFFLE_NODE   -> fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaPassifs.souffleStatLine(rank);
            case fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaPassifs.LIEN_NODE      -> fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaPassifs.lienStatLine(rank);
            case fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaPassifs.GARDIEN_NODE   -> fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaPassifs.gardienStatLine(rank);
            case fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaPassifs.GRACE_NODE     -> fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaPassifs.graceStatLine(rank);
            case fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaPassifs.CYCLE_NODE     -> fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaPassifs.cycleStatLine(rank);
            // Lancier
            case fr.varyon.vrpg.classes.lancier.PerceeSkill.SKILL_ID              -> fr.varyon.vrpg.classes.lancier.PerceeSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.lancier.ChargeHeroiqueSkill.SKILL_ID      -> fr.varyon.vrpg.classes.lancier.ChargeHeroiqueSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.lancier.GardeDuLancierSkill.SKILL_ID      -> fr.varyon.vrpg.classes.lancier.GardeDuLancierSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.lancier.HarponnageSkill.SKILL_ID          -> fr.varyon.vrpg.classes.lancier.HarponnageSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.lancier.FormationDePiquesSkill.SKILL_ID   -> fr.varyon.vrpg.classes.lancier.FormationDePiquesSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.lancier.EmpalementSkill.SKILL_ID          -> fr.varyon.vrpg.classes.lancier.EmpalementSkill.statLineForRank(rank);
            case fr.varyon.vrpg.classes.lancier.LancierPassifs.DISCIPLINE_NODE    -> fr.varyon.vrpg.classes.lancier.LancierPassifs.disciplineStatLine(rank);
            case fr.varyon.vrpg.classes.lancier.LancierPassifs.POSTURE_NODE       -> fr.varyon.vrpg.classes.lancier.LancierPassifs.postureStatLine(rank);
            case fr.varyon.vrpg.classes.lancier.LancierPassifs.PERCE_COEUR_NODE   -> fr.varyon.vrpg.classes.lancier.LancierPassifs.perceCoeurStatLine(rank);
            case fr.varyon.vrpg.classes.lancier.LancierPassifs.CHASSEUR_GEANTS_NODE -> fr.varyon.vrpg.classes.lancier.LancierPassifs.geantsStatLine(rank);
            case fr.varyon.vrpg.classes.lancier.LancierPassifs.CONTROLE_NODE      -> fr.varyon.vrpg.classes.lancier.LancierPassifs.controleStatLine(rank);
            case fr.varyon.vrpg.classes.lancier.LancierPassifs.PORTEE_NODE        -> fr.varyon.vrpg.classes.lancier.LancierPassifs.porteeStatLine(rank);
            default -> null;
        };
    }

    private static String assautEclairLine(int rank) {
        int pct     = Math.round(AssautEclairSkill.damageFactor(rank) * 100);
        String cd   = formatCooldownShort(AssautEclairSkill.cooldownMsForRank(rank));
        int stamina = Math.round(AssautEclairSkill.staminaCostForRank(rank));
        return pct + "% dégâts arme, Délai " + cd + " - " + stamina + " endurance";
    }

    private static String formatCooldownShort(long cooldownMs) {
        if (cooldownMs % 1000L == 0L) {
            return (cooldownMs / 1000L) + "s";
        }
        return String.format(Locale.ROOT, "%.1fs", cooldownMs / 1000.0);
    }
}
