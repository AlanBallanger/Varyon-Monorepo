package fr.varyon.vrpg.ui.classes;

import fr.varyon.vrpg.classes.ability.AssautEclairSkill;
import fr.varyon.vrpg.classes.ability.ExpertEnDuelSkill;
import fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs;
import fr.varyon.vrpg.classes.arbaletrier.ArbaietrierPassifs;
import fr.varyon.vrpg.classes.arcaniste.BouleDeFeuSkill;
import fr.varyon.vrpg.classes.arcaniste.DistorsionSkill;
import fr.varyon.vrpg.classes.arcaniste.MeteoreSkill;
import fr.varyon.vrpg.classes.arcaniste.NovaDeGivreSkill;
import fr.varyon.vrpg.classes.arcaniste.SalveDeGivreSkill;
import fr.varyon.vrpg.classes.arcaniste.SurchargeSkill;
import fr.varyon.vrpg.classes.arbaletrier.CarreauExplosifSkill;
import fr.varyon.vrpg.classes.arbaletrier.CarreauLourdSkill;
import fr.varyon.vrpg.classes.arbaletrier.CarreauTranspercantSkill;
import fr.varyon.vrpg.classes.arbaletrier.CoupDeBotteSkill;
import fr.varyon.vrpg.classes.arbaletrier.MiseEnJouSkill;
import fr.varyon.vrpg.classes.arbaletrier.ReculTactiqueSkill;
import fr.varyon.vrpg.classes.bagarreur.BagarreurPassifs;
import fr.varyon.vrpg.classes.bagarreur.DirectDuDroitSkill;
import fr.varyon.vrpg.classes.bagarreur.DelugeDeCoups2Skill;
import fr.varyon.vrpg.classes.bagarreur.JeuDeJambesSkill;
import fr.varyon.vrpg.classes.bagarreur.UppercutSkill;
import fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaPassifs;
import fr.varyon.vrpg.classes.lancier.LancierPassifs;
import fr.varyon.vrpg.classes.ombre.OmbrePassifs;
import fr.varyon.vrpg.classes.ravageur.BondEcrasantSkill;
import fr.varyon.vrpg.classes.ravageur.DechainementSkill;
import fr.varyon.vrpg.classes.ravageur.MarteauPilonSkill;
import fr.varyon.vrpg.classes.ravageur.PeauDeFerSkill;
import fr.varyon.vrpg.classes.ravageur.PremierAssautSkill;
import fr.varyon.vrpg.classes.ravageur.RabattageSkill;
import fr.varyon.vrpg.classes.ravageur.RavageurPassifs;
import fr.varyon.vrpg.classes.rempart.RempartPassifs;
import fr.varyon.vrpg.classes.rodeur.MarqueDuChasseurSkill;
import fr.varyon.vrpg.classes.rodeur.RodeurPassifs;
import fr.varyon.vrpg.classes.vaudou.VaudouPassifs;
import fr.varyon.vrpg.classes.bagarreur.MonteeAdreinalineSkill;
import fr.varyon.vrpg.classes.berserker.CorDeGuerreSkill;
import fr.varyon.vrpg.classes.berserker.CriRalliementSkill;
import fr.varyon.vrpg.classes.berserker.DechiquetageSkill;
import fr.varyon.vrpg.classes.berserker.DixPourSangSkill;
import fr.varyon.vrpg.classes.berserker.ExecutionSauvageSkill;
import fr.varyon.vrpg.classes.duelliste.AssautBretteurSkill;
import fr.varyon.vrpg.classes.duelliste.CoupEstocSkill;
import fr.varyon.vrpg.classes.duelliste.DesarmementSkill;
import fr.varyon.vrpg.classes.duelliste.DuellistePassifs;
import fr.varyon.vrpg.classes.duelliste.FeintSkill;
import fr.varyon.vrpg.classes.duelliste.RiposteParfaiteSkill;
import fr.varyon.vrpg.classes.gardiendesgaia.AppelDuTreantSkill;
import fr.varyon.vrpg.classes.gardiendesgaia.BenedictionDeGaiaSkill;
import fr.varyon.vrpg.classes.gardiendesgaia.EcorceProtectriceSkill;
import fr.varyon.vrpg.classes.gardiendesgaia.EtreinteDeGaiaSkill;
import fr.varyon.vrpg.classes.gardiendesgaia.EvasionSylvestreSkill;
import fr.varyon.vrpg.classes.gardiendesgaia.MarqueDeRenaissanceSkill;
import fr.varyon.vrpg.classes.lancier.ChargeHeroiqueSkill;
import fr.varyon.vrpg.classes.lancier.EmpalementSkill;
import fr.varyon.vrpg.classes.lancier.FormationDePiquesSkill;
import fr.varyon.vrpg.classes.lancier.GardeDuLancierSkill;
import fr.varyon.vrpg.classes.lancier.HarponnageSkill;
import fr.varyon.vrpg.classes.lancier.PerceeSkill;
import fr.varyon.vrpg.classes.ombre.ChaseOuverteSkill;
import fr.varyon.vrpg.classes.ombre.DelugeDeGamesSkill;
import fr.varyon.vrpg.classes.ombre.EcranDeFumeeSkill;
import fr.varyon.vrpg.classes.ombre.FrappeFataleSkill;
import fr.varyon.vrpg.classes.ombre.PasDeLOmbreSkill;
import fr.varyon.vrpg.classes.ombre.PasDesTenebresSkill;
import fr.varyon.vrpg.classes.berserker.AssautBestialSkill;
import fr.varyon.vrpg.classes.berserker.BerserkerPassifs;
import fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill;
import fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill;
import fr.varyon.vrpg.classes.rempart.ForteresseSkill;
import fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill;
import fr.varyon.vrpg.classes.rempart.ProvocationSkill;
import fr.varyon.vrpg.classes.rempart.SecondSouffleSkill;
import fr.varyon.vrpg.classes.rodeur.FlecheDeReculsSkill;
import fr.varyon.vrpg.classes.rodeur.FlecheEntravantSkill;
import fr.varyon.vrpg.classes.rodeur.PluieDesFlechesSkill;
import fr.varyon.vrpg.classes.rodeur.RafaleSkill;
import fr.varyon.vrpg.classes.rodeur.ReculStrategiqueSkill;
import fr.varyon.vrpg.classes.vaudou.AttaquePerfideSkill;
import fr.varyon.vrpg.classes.vaudou.ExtractionAmeSkill;
import fr.varyon.vrpg.classes.vaudou.FleauToxiqueSkill;
import fr.varyon.vrpg.classes.vaudou.PassageEthereSkill;
import fr.varyon.vrpg.classes.vaudou.TotemEntraveSkill;
import fr.varyon.vrpg.classes.vaudou.TotemVulnerabiliteSkill;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ClassSkillStatResolver {

    private static final Pattern DMG_PATTERN =
        Pattern.compile("(\\d+)%\\s*d[ée]g[aâ]ts\\s*arme", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern[] WEAPON_DMG_PATTERNS = {
        DMG_PATTERN,
        Pattern.compile("(\\d+)%\\s*d[ée]g[aâ]ts/", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
        Pattern.compile("(\\d+)%\\s*d[ée]g[aâ]ts/s", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
        Pattern.compile("(\\d+)%\\s*d[ée]g[aâ]ts,", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
        Pattern.compile("à (\\d+)%\\s*d[ée]g[aâ]ts", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
        Pattern.compile("(\\d+)%\\s*arme/s", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
    };
    private static final Pattern CD_PATTERN =
        Pattern.compile("(?:CD|D[eé]lai)\\s*([\\d.]+s)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern STAMINA_PATTERN =
        Pattern.compile("(\\d+)\\s*endurance", Pattern.CASE_INSENSITIVE);
    private static final Pattern MANA_PATTERN =
        Pattern.compile("(\\d+)\\s*mana", Pattern.CASE_INSENSITIVE);

    private ClassSkillStatResolver() {}

    @Nullable
    public static SkillStatDisplay resolve(@Nullable String skillId, int rank, @Nullable String statLine) {
        if (skillId == null || rank < 1) return null;

        List<SkillStatEntry> specific = buildSpecific(skillId, rank);
        if (specific != null && !specific.isEmpty()) {
            return SkillStatDisplay.of(specific);
        }

        if (statLine == null || statLine.isBlank()) return null;

        List<SkillStatEntry> generic = parseGeneric(skillId, rank, statLine);
        if (!generic.isEmpty()) {
            return SkillStatDisplay.of(generic);
        }
        return SkillStatDisplay.fallback(statLine);
    }

    @Nullable
    private static List<SkillStatEntry> buildSpecific(String skillId, int rank) {
        return switch (skillId) {
            case CoupEstocSkill.SKILL_ID -> coupEstocStats(rank);
            case AssautBretteurSkill.SKILL_ID -> assautBretteurStats(rank);
            case FeintSkill.SKILL_ID -> feintStats(rank);
            case DesarmementSkill.SKILL_ID -> desarmementStats(rank);
            case RiposteParfaiteSkill.SKILL_ID -> riposteStats(rank);
            case DuellistePassifs.BLESSURE_NODE -> entailleStats(rank);
            case DuellistePassifs.CONTRE_NODE -> ascendantStats(rank);
            case DuellistePassifs.FRAPPE_NODE -> frappePreciseStats(rank);
            case DuellistePassifs.MOMENTUM_NODE -> momentumStats(rank);
            case DuellistePassifs.ESQUIVE_NODE -> esquiveStats(rank);
            case ExpertEnDuelSkill.SKILL_ID -> xpOnlyStats(
                (int) Math.round(ExpertEnDuelSkill.xpBonusForRank(rank) * 100));
            case OmbrePassifs.EXECUTION_RAPIDE_NODE -> List.of(
                xpEntry(Math.round(OmbrePassifs.executionXpBonusForRank(rank) * 100)),
                new SkillStatEntry(SkillStatKind.DURATION,
                    formatDurationMs(OmbrePassifs.executionWindowMs())));
            case BerserkerPassifs.CARNAGE_NODE -> xpStackStats(
                Math.round(BerserkerPassifs.carnageXpBonusPerStack(rank) * 100));
            case BerserkerPassifs.FUREUR_NODE -> fureurSanguinaireStats(rank);
            case BerserkerPassifs.FRENESIE_NODE -> frenesieStats(rank);
            case BerserkerPassifs.FERVEUR_NODE -> ferveurStats(rank);
            case BerserkerPassifs.BLESSURES_NODE -> blessuresProfondesStats(rank);
            case BerserkerPassifs.DERNIER_SOUFFLE_NODE -> dernierSouffleStats(rank);
            case CorDeGuerreSkill.SKILL_ID -> corDeGuerreStats(rank);
            case CriRalliementSkill.SKILL_ID -> criRalliementStats(rank);
            case RempartPassifs.MAITRE_BOUCLIER_NODE -> xpOnlyStats(
                Math.round(RempartPassifs.bouclierXpBonusForRank(rank) * 100));
            case RempartPassifs.CONSTITUTION_NODE -> constitutionStats(rank);
            case RempartPassifs.GARDE_IMPENETRABLE_NODE -> gardeImpenetrableStats(rank);
            case RempartPassifs.INFATIGABLE_NODE -> infatigableStats(rank);
            case RempartPassifs.CONTRE_OFFENSIF_NODE -> contreOffensifStats(rank);
            case RempartPassifs.DERNIER_BASTION_NODE -> dernierBastionStats(rank);
            case CoupDeBouclierSkill.SKILL_ID -> coupDeBouclierStats(rank);
            case ForteresseSkill.SKILL_ID -> forteresseStats(rank);
            case SecondSouffleSkill.SKILL_ID -> secondSouffleRempartStats(rank);
            case ProvocationSkill.SKILL_ID -> provocationStats(rank);
            case GardeRapprocheSkill.SKILL_ID -> gardeRapprocheStats(rank);
            case RavageurPassifs.MOISSONNEUR_NODE -> xpStackStats(
                Math.round(RavageurPassifs.moissonneurXpBonusPerStack(rank) * 100));
            case RavageurPassifs.ARME_LOURDE_NODE -> armeLourdeStats(rank);
            case PeauDeFerSkill.SKILL_ID -> peauDeFerStats(rank);
            case RavageurPassifs.EXECUTEUR_NODE -> executeurStats(rank);
            case DechainementSkill.SKILL_ID -> dechainementStats(rank);
            case RavageurPassifs.CHASSEUR_GEANT_NODE -> chasseurGeantStats(rank);
            case RavageurPassifs.ELAN_DESTRUCTEUR_NODE -> elanDestructeurStats(rank);
            case RavageurPassifs.COMBATTANT_INFATIGABLE_NODE -> combattantInfatigableStats(rank);
            case BagarreurPassifs.JUSQUAU_BOUT_NODE -> xpOnlyStats(
                Math.round(BagarreurPassifs.jusquAuBoutBonusForRank(rank) * 100));
            case DirectDuDroitSkill.SKILL_ID -> directDuDroitStats(rank);
            case MonteeAdreinalineSkill.SKILL_ID -> monteeAdrenalineStats(rank);
            case BagarreurPassifs.GARDE_BOXEUR_NODE -> gardeBoxeurStats(rank);
            case BagarreurPassifs.ADRENALINE_NODE -> adrenalineBagarreurStats(rank);
            case BagarreurPassifs.ACHARNEMENT_NODE -> acharnementStats(rank);
            case BagarreurPassifs.ESPRIT_COMBATIF_NODE -> espritCombatifStats(rank);
            case BagarreurPassifs.POINGS_ACIER_NODE -> poingsAcierStats(rank);
            case fr.varyon.vrpg.classes.bagarreur.SecondSouffleSkill.SKILL_ID ->
                secondSouffleBagarreurStats(rank);
            case GardienDeGaiaPassifs.HARMONIE_NODE -> xpOnlyStats(
                Math.round(GardienDeGaiaPassifs.harmonieBonusForRank(rank) * 100));
            case EvasionSylvestreSkill.SKILL_ID -> evasionSylvestreStats(rank);
            case BenedictionDeGaiaSkill.SKILL_ID -> benedictionDeGaiaStats(rank);
            case EcorceProtectriceSkill.SKILL_ID -> ecorceProtectriceStats(rank);
            case AppelDuTreantSkill.SKILL_ID -> appelDuTreantStats(rank);
            case EtreinteDeGaiaSkill.SKILL_ID -> etreinteDeGaiaStats(rank);
            case MarqueDeRenaissanceSkill.SKILL_ID -> marqueDeRenaissanceStats(rank);
            case GardienDeGaiaPassifs.LIEN_NODE -> lienSpirituelStats(rank);
            case GardienDeGaiaPassifs.GARDIEN_NODE -> gardienNatureStats(rank);
            case GardienDeGaiaPassifs.GRACE_NODE -> graceDeGaiaStats(rank);
            case GardienDeGaiaPassifs.CYCLE_NODE -> cycleDeVieStats(rank);
            case GardienDeGaiaPassifs.SOUFFLE_NODE -> souffleNatureStats(rank);
            case VaudouPassifs.FETICHEUR_NODE -> xpOnlyStats(
                Math.round(VaudouPassifs.feticheurXpBonusForRank(rank) * 100));
            case PassageEthereSkill.SKILL_ID -> passageEthereStats(rank);
            case FleauToxiqueSkill.SKILL_ID -> fleauToxiqueStats(rank);
            case TotemEntraveSkill.SKILL_ID -> totemEntraveStats(rank);
            case TotemVulnerabiliteSkill.SKILL_ID -> totemVulnerabiliteStats(rank);
            case VaudouPassifs.PARASITE_NODE -> parasiteSpirituelStats(rank);
            case VaudouPassifs.TOXINES_NODE -> toxinesStats(rank);
            case VaudouPassifs.ANCRAGE_NODE -> ancrageRituelStats(rank);
            case VaudouPassifs.RITUEL_NODE -> rituelInterditStats(rank);
            case VaudouPassifs.PRESENCE_NODE -> presenceOppressanteStats(rank);
            case RodeurPassifs.OEIL_CHASSEUR_NODE -> xpOnlyStats(
                Math.round(RodeurPassifs.oeilXpBonusForRank(rank) * 100));
            case RodeurPassifs.FLECHES_TOXIQUES_NODE -> rodeurFlechesToxiquesStats(rank);
            case RodeurPassifs.INSTINCT_SURVIE_NODE -> rodeurInstinctSurvieStats(rank);
            case RodeurPassifs.PRECISION_MORTELLE_NODE -> rodeurPrecisionMortelleStats(rank);
            case RodeurPassifs.TRAQUE_MOBILE_NODE -> rodeurTraqueMobileStats(rank);
            case RodeurPassifs.TRAQUE_SANS_FIN_NODE -> rodeurTraqueSansFinStats(rank);
            case ReculStrategiqueSkill.SKILL_ID -> reculStrategiqueStats(rank);
            case PluieDesFlechesSkill.SKILL_ID -> pluieDesFlechesStats(rank);
            case MarqueDuChasseurSkill.SKILL_ID -> marqueDuChasseurStats(rank);
            case FlecheDeReculsSkill.SKILL_ID -> flecheDeReculStats(rank);
            case FlecheEntravantSkill.SKILL_ID -> flecheEntravanteStats(rank);
            case RafaleSkill.SKILL_ID -> rafaleStats(rank);
            case ArbaietrierPassifs.TIREUR_ELITE_NODE -> xpOnlyStats(
                Math.round(ArbaietrierPassifs.eliteXpBonusForRank(rank) * 100));
            case CarreauExplosifSkill.SKILL_ID -> carreauExplosifStats(rank);
            case CarreauTranspercantSkill.SKILL_ID -> carreauTranspercantStats(rank);
            case CoupDeBotteSkill.SKILL_ID -> coupDeBotteStats(rank);
            case MiseEnJouSkill.SKILL_ID -> miseEnJouStats(rank);
            case ArbaietrierPassifs.CHASSEUR_COLOSSES_NODE -> chasseurColossesStats(rank);
            case ArbaietrierPassifs.TIREUR_EMBUSQUE_NODE -> tireurEmbusqueStats(rank);
            case ArbaietrierPassifs.VISEUR_NODE -> viseurExperimenteStats(rank);
            case ArbaietrierPassifs.CARREAUX_LACERANTS_NODE -> carreauxLacerantsStats(rank);
            case ArbaietrierPassifs.REFLEXES_AFFUTES_NODE -> reflexesAffutesStats(rank);
            case LancierPassifs.DISCIPLINE_NODE -> xpOnlyStats(
                Math.round(LancierPassifs.disciplineBonusForRank(rank) * 100));
            case ArcanistPassifs.TALENT_INNE_NODE -> xpOnlyStats(
                Math.round(ArcanistPassifs.talentInneBonusForRank(rank) * 100));
            case DistorsionSkill.SKILL_ID -> distorsionStats(rank);
            case MeteoreSkill.SKILL_ID -> meteoreStats(rank);
            case NovaDeGivreSkill.SKILL_ID -> novaDeGivreStats(rank);
            case SurchargeSkill.SKILL_ID -> surchargeStats(rank);
            case SalveDeGivreSkill.SKILL_ID -> salveDeGivreStats(rank);
            case ArcanistPassifs.PUITS_MANA_NODE -> puitsManaStats(rank);
            case ArcanistPassifs.ECHO_ARCANIQUE_NODE -> echoArcanicStats(rank);
            case ArcanistPassifs.ECHO_TEMPOREL_NODE -> echoTemporelStats(rank);
            case ArcanistPassifs.DRAIN_MYSTIQUE_NODE -> drainMystiqueStats(rank);
            case ArcanistPassifs.POUVOIR_GRANDISSANT_NODE -> pouvoirGrandissantStats(rank);
            case OmbrePassifs.LAMES_EMPOISONNEES_NODE -> lamesEmpoisonneesStats(rank);
            case OmbrePassifs.EMBUSCADE_NODE -> embuscadeStats(rank);
            case OmbrePassifs.DANSE_LAMES_NODE -> danseLamesStats(rank);
            case OmbrePassifs.OMBRE_INSAISISSABLE_NODE -> ombreInsaisissableStats(rank);
            case OmbrePassifs.INSTINCT_SURVIE_NODE -> instinctSurvieStats(rank);
            case DelugeDeGamesSkill.SKILL_ID -> delugeStats(rank);
            case FrappeFataleSkill.SKILL_ID -> frappeFataleStats(rank);
            case EcranDeFumeeSkill.SKILL_ID -> ecranStats(rank);
            case ChaseOuverteSkill.SKILL_ID -> chaseOuverteStats(rank);
            case MarteauPilonSkill.SKILL_ID -> marteauPilonStats(rank);
            default -> null;
        };
    }

    private static List<SkillStatEntry> entailleStats(int rank) {
        int chance = Math.round(DuellistePassifs.bleedChanceForRank(rank) * 100);
        int bleed = Math.round(DuellistePassifs.bleedWeaponPctForRank(rank) * 100);
        return List.of(
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, bleed + "%"),
            new SkillStatEntry(SkillStatKind.RATE, chance + "%"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(DuellistePassifs.BLEED_DURATION_MS))
        );
    }

    private static List<SkillStatEntry> lamesEmpoisonneesStats(int rank) {
        int chance = Math.round(OmbrePassifs.poisonChanceForRank(rank) * 100);
        int poison = Math.round(OmbrePassifs.poisonWeaponPctForRank(rank) * 100);
        return List.of(
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, poison + "%"),
            new SkillStatEntry(SkillStatKind.RATE, chance + "%"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(OmbrePassifs.POISON_DURATION_MS))
        );
    }

    private static List<SkillStatEntry> embuscadeStats(int rank) {
        return List.of(new SkillStatEntry(SkillStatKind.DURATION,
            formatDurationMs(OmbrePassifs.embuscadeDurationMs(rank))));
    }

    private static List<SkillStatEntry> danseLamesStats(int rank) {
        int spd = Math.round(OmbrePassifs.danseSpeedBonusForRank(rank) * 100);
        return List.of(
            new SkillStatEntry(SkillStatKind.MOVE_SPEED, "+" + spd + "%"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(OmbrePassifs.danseDurationMs()))
        );
    }

    private static List<SkillStatEntry> ombreInsaisissableStats(int rank) {
        int pct = Math.round(OmbrePassifs.dodgeBonusForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.DODGE, "+" + pct + "%"));
    }

    private static List<SkillStatEntry> instinctSurvieStats(int rank) {
        int pct = Math.round(OmbrePassifs.survieDodgeBonusForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.DODGE, "+" + pct + "%", "Sous 30%"));
    }

    private static List<SkillStatEntry> ascendantStats(int rank) {
        int pct = Math.round(DuellistePassifs.contreBonusForRank(rank) * 100);
        return List.of(
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + pct + "%"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(DuellistePassifs.CONTRE_WINDOW_MS))
        );
    }

    private static List<SkillStatEntry> frappePreciseStats(int rank) {
        int pct = Math.round(DuellistePassifs.critBonusForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + pct + "%"));
    }

    private static List<SkillStatEntry> momentumStats(int rank) {
        float raw = DuellistePassifs.momentumBonusPerStack(rank) * 100f;
        String pctStr = raw == Math.floor(raw) ? String.valueOf((int) raw) : String.valueOf(raw);
        return List.of(new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + pctStr + "%", "Cumul"));
    }

    private static List<SkillStatEntry> esquiveStats(int rank) {
        int pct = Math.round(DuellistePassifs.dodgeChanceForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.DODGE, pct + "%"));
    }

    private static List<SkillStatEntry> xpOnlyStats(int pct) {
        return List.of(xpEntry(pct));
    }

    private static List<SkillStatEntry> xpStackStats(int pct) {
        return List.of(xpEntry(pct, "Cumul"));
    }

    private static SkillStatEntry xpEntry(int pct) {
        return new SkillStatEntry(SkillStatKind.XP, "+" + pct + "%");
    }

    private static SkillStatEntry xpEntry(int pct, String label) {
        return new SkillStatEntry(SkillStatKind.XP, "+" + pct + "%", label);
    }

    private static List<SkillStatEntry> coupEstocStats(int rank) {
        int pct = Math.round(CoupEstocSkill.castDamageFactorForRank(rank) * 100);
        int next = Math.round((CoupEstocSkill.nextHitMultForRank(rank) - 1f) * 100);
        int stamina = Math.round(CoupEstocSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, pct + "%"),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(CoupEstocSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina)),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(CoupEstocSkill.armedWindowMs())),
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + next + "%", "Proch.")
        );
    }

    private static List<SkillStatEntry> assautBretteurStats(int rank) {
        int dmg = Math.round(AssautBretteurSkill.damageBonusForRank(rank) * 100);
        int spd = Math.round(AssautBretteurSkill.speedBonusForRank(rank) * 100);
        int stamina = Math.round(AssautBretteurSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + dmg + "%"),
            new SkillStatEntry(SkillStatKind.MOVE_SPEED, "+" + spd + "%"),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(AssautBretteurSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina)),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(AssautBretteurSkill.durationMsForRank(rank)))
        );
    }

    private static List<SkillStatEntry> feintStats(int rank) {
        int stamina = Math.round(FeintSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.DURATION, formatDurationMs(FeintSkill.windowMs())),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(FeintSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> riposteStats(int rank) {
        int dmg = Math.round(RiposteParfaiteSkill.dmgBonusForRank(rank) * 100);
        int stamina = Math.round(RiposteParfaiteSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + dmg + "%", "Contre"),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(RiposteParfaiteSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina)),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(RiposteParfaiteSkill.windowMsForRank(rank)), "Posture")
        );
    }

    private static List<SkillStatEntry> desarmementStats(int rank) {
        int red = Math.round(DesarmementSkill.reductionForRank(rank) * 100);
        int stamina = Math.round(DesarmementSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "-" + red + "%"),
            new SkillStatEntry(SkillStatKind.MOVE_SPEED, "-50%"),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(DesarmementSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina)),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(DesarmementSkill.durationMsForRank(rank)))
        );
    }

    private static List<SkillStatEntry> delugeStats(int rank) {
        int pct = Math.round(DelugeDeGamesSkill.damagePctForRank(rank) * 100);
        int stamina = Math.round(DelugeDeGamesSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, pct + "%"),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(DelugeDeGamesSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina)),
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "×" + DelugeDeGamesSkill.strikeCountForRank(rank), "Frappes")
        );
    }

    private static List<SkillStatEntry> frappeFataleStats(int rank) {
        int pct = Math.round(FrappeFataleSkill.damageBonusForRank(rank) * 100);
        int stamina = Math.round(FrappeFataleSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + pct + "%"),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(FrappeFataleSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina)),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(FrappeFataleSkill.armedWindowMs()), "Proch.")
        );
    }

    private static List<SkillStatEntry> ecranStats(int rank) {
        int stamina = Math.round(EcranDeFumeeSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(EcranDeFumeeSkill.durationMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(EcranDeFumeeSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> chaseOuverteStats(int rank) {
        int stamina = Math.round(ChaseOuverteSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(ChaseOuverteSkill.durationMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(ChaseOuverteSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> marteauPilonStats(int rank) {
        int p1 = Math.round(MarteauPilonSkill.damagePct1ForRank(rank) * 100);
        int p2 = Math.round(MarteauPilonSkill.damagePct2ForRank(rank) * 100);
        int stamina = Math.round(MarteauPilonSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, p1 + "%", "1"),
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, p2 + "%", "2"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(MarteauPilonSkill.stunMsForRank(rank)), "Étour."),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(MarteauPilonSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> armeLourdeStats(int rank) {
        int slow = Math.round(RavageurPassifs.armeLourdeSlowForRank(rank) * 100);
        return List.of(
            new SkillStatEntry(SkillStatKind.MOVE_SPEED, "-" + slow + "%", "Vitesse"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(RavageurPassifs.armeLourdeDurationMs()))
        );
    }

    private static List<SkillStatEntry> directDuDroitStats(int rank) {
        int pct = Math.round(DirectDuDroitSkill.damagePctForRank(rank) * 100);
        int stamina = Math.round(DirectDuDroitSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, pct + "%"),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(DirectDuDroitSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> monteeAdrenalineStats(int rank) {
        int dmg = Math.round(MonteeAdreinalineSkill.damageBonusForRank(rank) * 100);
        int stamina = Math.round(MonteeAdreinalineSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + dmg + "%"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(MonteeAdreinalineSkill.durationMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(MonteeAdreinalineSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> gardeBoxeurStats(int rank) {
        int red = Math.round(BagarreurPassifs.gardeBoxeurReducForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.DEFENSE, "-" + red + "%"));
    }

    private static List<SkillStatEntry> adrenalineBagarreurStats(int rank) {
        int spd = Math.round(BagarreurPassifs.adrenalineSpeedPerStackForRank(rank) * 100);
        return List.of(
            new SkillStatEntry(SkillStatKind.MOVE_SPEED, "+" + spd + "%", "Cumul"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(BagarreurPassifs.adrenalineDurationMsForRank(rank)))
        );
    }

    private static List<SkillStatEntry> acharnementStats(int rank) {
        int pct = Math.round(BagarreurPassifs.acharnementBonusPerStack(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + pct + "%"));
    }

    private static List<SkillStatEntry> espritCombatifStats(int rank) {
        int pct = Math.round(BagarreurPassifs.espritCombatifBonusForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + pct + "%"));
    }

    private static List<SkillStatEntry> poingsAcierStats(int rank) {
        int chance = Math.round(BagarreurPassifs.poingsAcierChanceForRank(rank) * 100);
        return List.of(
            new SkillStatEntry(SkillStatKind.RATE, chance + "%"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(BagarreurPassifs.poingsAcierStunMsForRank(rank)), "Étour.")
        );
    }

    private static List<SkillStatEntry> distorsionStats(int rank) {
        int dist = (int) DistorsionSkill.dashDistanceForRank(rank);
        int stamina = Math.round(DistorsionSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.MOVE_SPEED, String.valueOf(dist), "Dist."),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(DistorsionSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> evasionSylvestreStats(int rank) {
        int dist = (int) EvasionSylvestreSkill.dashDistanceForRank(rank);
        int stamina = Math.round(EvasionSylvestreSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.MOVE_SPEED, String.valueOf(dist), "Dist."),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(EvasionSylvestreSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> benedictionDeGaiaStats(int rank) {
        int heal = Math.round(BenedictionDeGaiaSkill.healAmountForRank(rank));
        int mana = Math.round(BenedictionDeGaiaSkill.manaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.HEAL, "+" + heal),
            new SkillStatEntry(SkillStatKind.MANA, String.valueOf(mana)),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(BenedictionDeGaiaSkill.cooldownMsForRank(rank)))
        );
    }

    private static List<SkillStatEntry> ecorceProtectriceStats(int rank) {
        int reduc = Math.round(EcorceProtectriceSkill.damageReductionForRank(rank) * 100);
        float heal = EcorceProtectriceSkill.healPerSecForRank(rank);
        int mana = Math.round(EcorceProtectriceSkill.manaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.DEFENSE, "-" + reduc + "%"),
            new SkillStatEntry(SkillStatKind.HEAL, heal + "/s", "Regen."),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(EcorceProtectriceSkill.durationMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.MANA, String.valueOf(mana)),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(EcorceProtectriceSkill.cooldownMsForRank(rank)))
        );
    }

    private static List<SkillStatEntry> appelDuTreantStats(int rank) {
        int pct = Math.round(AppelDuTreantSkill.damageFactor(rank) * 100);
        int mana = Math.round(AppelDuTreantSkill.manaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(AppelDuTreantSkill.durationMsForRank(rank)), "Durée"),
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, pct + "%", "Stats"),
            new SkillStatEntry(SkillStatKind.MANA, String.valueOf(mana)),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(AppelDuTreantSkill.cooldownMsForRank(rank)))
        );
    }

    private static List<SkillStatEntry> etreinteDeGaiaStats(int rank) {
        int mana = Math.round(EtreinteDeGaiaSkill.manaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(EtreinteDeGaiaSkill.rootDurationMs(rank)), "Immob."),
            new SkillStatEntry(SkillStatKind.MANA, String.valueOf(mana)),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(EtreinteDeGaiaSkill.cooldownMsForRank(rank)))
        );
    }

    private static List<SkillStatEntry> marqueDeRenaissanceStats(int rank) {
        int gauge = Math.round(MarqueDeRenaissanceSkill.specialGaugeBonusForRank(rank));
        int mana = Math.round(MarqueDeRenaissanceSkill.manaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(MarqueDeRenaissanceSkill.durationMsForRank(rank)), "Durée"),
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, "+" + gauge, "Jauge"),
            new SkillStatEntry(SkillStatKind.MANA, String.valueOf(mana)),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(MarqueDeRenaissanceSkill.cooldownMsForRank(rank)))
        );
    }

    private static List<SkillStatEntry> lienSpirituelStats(int rank) {
        int pct = Math.round(GardienDeGaiaPassifs.lienRatioForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.HEAL, pct + "%", "Soin"));
    }

    private static List<SkillStatEntry> gardienNatureStats(int rank) {
        int pct = Math.round(GardienDeGaiaPassifs.gardienHpBonusForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.HEAL, "+" + pct + "%", "PV"));
    }

    private static List<SkillStatEntry> graceDeGaiaStats(int rank) {
        int pct = Math.round(GardienDeGaiaPassifs.graceBonusForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.HEAL, "+" + pct + "%", "Soins <40% PV"));
    }

    private static List<SkillStatEntry> cycleDeVieStats(int rank) {
        float mana = GardienDeGaiaPassifs.cycleManaForRank(rank);
        String manaStr = mana == Math.floor(mana) ? String.valueOf((int) mana) : String.valueOf(mana);
        return List.of(new SkillStatEntry(SkillStatKind.MANA, "+" + manaStr, "Mana"));
    }

    private static List<SkillStatEntry> souffleNatureStats(int rank) {
        float hp = GardienDeGaiaPassifs.souffleHpRegenForRank(rank);
        float sta = GardienDeGaiaPassifs.souffleStaRegenForRank(rank);
        String hpStr = hp == Math.floor(hp) ? String.valueOf((int) hp) : String.valueOf(hp);
        String staStr = sta == Math.floor(sta) ? String.valueOf((int) sta) : String.valueOf(sta);
        return List.of(
            new SkillStatEntry(SkillStatKind.HEAL, "+" + hpStr + "/s", "PV"),
            new SkillStatEntry(SkillStatKind.STAMINA, "+" + staStr + "/s", "Endu.")
        );
    }

    private static List<SkillStatEntry> passageEthereStats(int rank) {
        int range = (int) PassageEthereSkill.rangeForRank(rank);
        int stamina = Math.round(PassageEthereSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.MOVE_SPEED, String.valueOf(range), "Portée"),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(PassageEthereSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> fleauToxiqueStats(int rank) {
        int pct = Math.round(FleauToxiqueSkill.poisonWeaponPctForRank(rank) * 100);
        int red = Math.round(FleauToxiqueSkill.damageReduceForRank(rank) * 100);
        int mana = Math.round(FleauToxiqueSkill.manaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, pct + "%", "Poison"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(FleauToxiqueSkill.poisonDurationMsForRank(rank)), "Durée"),
            new SkillStatEntry(SkillStatKind.DEFENSE, "-" + red + "%", "Redu."),
            new SkillStatEntry(SkillStatKind.MANA, String.valueOf(mana)),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(FleauToxiqueSkill.cooldownMsForRank(rank)))
        );
    }

    private static List<SkillStatEntry> totemEntraveStats(int rank) {
        int mana = Math.round(TotemEntraveSkill.manaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.MOVE_SPEED,
                "-" + TotemEntraveSkill.slowReductionPct() + "%", "Vitesse"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(TotemEntraveSkill.baseDurationMsForRank(rank)), "Durée"),
            new SkillStatEntry(SkillStatKind.MANA, String.valueOf(mana)),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(TotemEntraveSkill.cooldownMsForRank(rank)))
        );
    }

    private static List<SkillStatEntry> totemVulnerabiliteStats(int rank) {
        int bonus = Math.round(TotemVulnerabiliteSkill.damageTakenBonusForRank(rank) * 100);
        int mana = Math.round(TotemVulnerabiliteSkill.manaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + bonus + "%", "Dégâts"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(TotemVulnerabiliteSkill.baseDurationMsForRank(rank)), "Durée"),
            new SkillStatEntry(SkillStatKind.MANA, String.valueOf(mana)),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(TotemVulnerabiliteSkill.cooldownMsForRank(rank)))
        );
    }

    private static List<SkillStatEntry> parasiteSpirituelStats(int rank) {
        int pct = Math.round(VaudouPassifs.parasiteHealPctForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.HEAL, pct + "%", "PV"));
    }

    private static List<SkillStatEntry> toxinesStats(int rank) {
        int pct = Math.round(VaudouPassifs.toxinesDurationBonusForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.DURATION, "+" + pct + "%", "Durée"));
    }

    private static List<SkillStatEntry> ancrageRituelStats(int rank) {
        int pct = Math.round(VaudouPassifs.ancrageTotemBonusForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.DURATION, "+" + pct + "%", "Durée"));
    }

    private static List<SkillStatEntry> rituelInterditStats(int rank) {
        int pct = Math.round(VaudouPassifs.rituelBonusForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + pct + "%", "Dégâts"));
    }

    private static List<SkillStatEntry> presenceOppressanteStats(int rank) {
        int pct = Math.round(VaudouPassifs.presenceReductionForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.DEFENSE, "-" + pct + "%", "Redu."));
    }

    private static List<SkillStatEntry> puitsManaStats(int rank) {
        int pct = Math.round(ArcanistPassifs.puitsManaBonus(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.MANA, "+" + pct + "%", "Max"));
    }

    private static List<SkillStatEntry> echoArcanicStats(int rank) {
        int pct = Math.round(ArcanistPassifs.echoArcanicChanceForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.RATE, pct + "%"));
    }

    private static List<SkillStatEntry> echoTemporelStats(int rank) {
        int pct = Math.round(ArcanistPassifs.echoTemporelReducForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.COOLDOWN, "-" + pct + "%"));
    }

    private static List<SkillStatEntry> drainMystiqueStats(int rank) {
        int pct = Math.round(ArcanistPassifs.drainMystiqueBonusForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.MANA, "+" + pct + "%"));
    }

    private static List<SkillStatEntry> pouvoirGrandissantStats(int rank) {
        int pct = Math.round(ArcanistPassifs.pouvoirGrandissantBonusForRank(rank) * 100);
        int sec = (int) (ArcanistPassifs.pouvoirGrandissantDelayMs() / 1000);
        return List.of(
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + pct + "%", "Sorts"),
            new SkillStatEntry(SkillStatKind.DURATION, sec + "s", "Sans dégât")
        );
    }

    private static List<SkillStatEntry> salveDeGivreStats(int rank) {
        int bolts = SalveDeGivreSkill.boltCountForRank(rank);
        int pct = Math.round(SalveDeGivreSkill.damagePctPerHitForRank(rank) * 100);
        int mana = Math.round(SalveDeGivreSkill.manaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, String.valueOf(bolts), "Proj."),
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, pct + "%"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(SalveDeGivreSkill.slowMsForRank(rank)), "Ralent."),
            new SkillStatEntry(SkillStatKind.MANA, String.valueOf(mana)),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(SalveDeGivreSkill.cooldownMsForRank(rank)))
        );
    }

    private static List<SkillStatEntry> novaDeGivreStats(int rank) {
        int pct = Math.round(NovaDeGivreSkill.damagePctForRank(rank) * 100);
        int mana = Math.round(NovaDeGivreSkill.manaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, pct + "%"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(NovaDeGivreSkill.slowMsForRank(rank)), "Ralent."),
            new SkillStatEntry(SkillStatKind.MOVE_SPEED,
                String.valueOf((int) NovaDeGivreSkill.radiusForRank(rank)), "Rayon"),
            new SkillStatEntry(SkillStatKind.MANA, String.valueOf(mana)),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(NovaDeGivreSkill.cooldownMsForRank(rank)))
        );
    }

    private static List<SkillStatEntry> surchargeStats(int rank) {
        int dmg = Math.round(SurchargeSkill.damageBonusForRank(rank) * 100);
        int manaRst = Math.round(SurchargeSkill.manaRestorePctForRank(rank) * 100);
        return List.of(
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + dmg + "%"),
            new SkillStatEntry(SkillStatKind.MANA, "+" + manaRst + "%"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(SurchargeSkill.durationMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(SurchargeSkill.cooldownMsForRank(rank)))
        );
    }

    private static List<SkillStatEntry> meteoreStats(int rank) {
        int pct = Math.round(MeteoreSkill.damagePctForRank(rank) * 100);
        int mana = Math.round(MeteoreSkill.manaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, pct + "%"),
            new SkillStatEntry(SkillStatKind.MOVE_SPEED,
                String.valueOf((int) MeteoreSkill.impactRadiusForRank(rank)), "Rayon"),
            new SkillStatEntry(SkillStatKind.MANA, String.valueOf(mana)),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(MeteoreSkill.cooldownMsForRank(rank)))
        );
    }

    private static List<SkillStatEntry> secondSouffleBagarreurStats(int rank) {
        int hp = Math.round(
            fr.varyon.vrpg.classes.bagarreur.SecondSouffleSkill.hpRestorePctForRank(rank) * 100);
        int staRestore = Math.round(
            fr.varyon.vrpg.classes.bagarreur.SecondSouffleSkill.staminaRestorePctForRank(rank) * 100);
        return List.of(
            new SkillStatEntry(SkillStatKind.HEAL, hp + "%"),
            new SkillStatEntry(SkillStatKind.STAMINA, staRestore + "%"),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(
                    fr.varyon.vrpg.classes.bagarreur.SecondSouffleSkill.cooldownMsForRank(rank)))
        );
    }

    private static List<SkillStatEntry> peauDeFerStats(int rank) {
        int red = Math.round(PeauDeFerSkill.damageReductionForRank(rank) * 100);
        int stamina = Math.round(PeauDeFerSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.DEFENSE, "-" + red + "%"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(PeauDeFerSkill.durationMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(PeauDeFerSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> executeurStats(int rank) {
        int pct = Math.round(RavageurPassifs.executeurBonusForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + pct + "%", "Attaque"));
    }

    private static List<SkillStatEntry> dechainementStats(int rank) {
        int dmg = Math.round(DechainementSkill.damageBonusForRank(rank) * 100);
        int stamina = Math.round(DechainementSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + dmg + "%"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(DechainementSkill.durationMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(DechainementSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> chasseurGeantStats(int rank) {
        int pct = Math.round(RavageurPassifs.chasseurGeantBonusForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + pct + "%", "Attaque"));
    }

    private static List<SkillStatEntry> carreauExplosifStats(int rank) {
        int dmg = Math.round(CarreauExplosifSkill.damagePctForRank(rank) * 100);
        int stamina = Math.round(CarreauExplosifSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, dmg + "%"),
            new SkillStatEntry(SkillStatKind.MOVE_SPEED,
                String.valueOf((int) CarreauExplosifSkill.radius()), "Portée"),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(CarreauExplosifSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> carreauTranspercantStats(int rank) {
        int dmg = Math.round(CarreauTranspercantSkill.damagePctForRank(rank) * 100);
        int slow = Math.round(CarreauTranspercantSkill.slowFactorForRank(rank) * 100);
        int stamina = Math.round(CarreauTranspercantSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, dmg + "%"),
            new SkillStatEntry(SkillStatKind.MOVE_SPEED,
                String.valueOf(CarreauTranspercantSkill.pierceCountForRank(rank)), "Cibles"),
            new SkillStatEntry(SkillStatKind.DEFENSE, "-" + slow + "%", "Ralent."),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(CarreauTranspercantSkill.slowMsForRank(rank)), "Durée"),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(CarreauTranspercantSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> coupDeBotteStats(int rank) {
        int dmg = Math.round(CoupDeBotteSkill.damagePctForRank(rank) * 100);
        int kb = (int) CoupDeBotteSkill.knockbackForRank(rank);
        int stamina = Math.round(CoupDeBotteSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, dmg + "%"),
            new SkillStatEntry(SkillStatKind.MOVE_SPEED, String.valueOf(kb), "Recul"),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(CoupDeBotteSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> miseEnJouStats(int rank) {
        int mult = (int) MiseEnJouSkill.damageMultiplierForRank(rank);
        int stamina = Math.round(MiseEnJouSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "×" + mult, "Carreau"),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(MiseEnJouSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> chasseurColossesStats(int rank) {
        int pct = Math.round(ArbaietrierPassifs.colossesBonusForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + pct + "%", "Attaque"));
    }

    private static List<SkillStatEntry> tireurEmbusqueStats(int rank) {
        int pct = Math.round(ArbaietrierPassifs.embusqueBonusForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + pct + "%", "Attaque"));
    }

    private static List<SkillStatEntry> viseurExperimenteStats(int rank) {
        int bonusPer5m = Math.round(ArbaietrierPassifs.viseurBonusPerMeterForRank(rank) * 5 * 100);
        return List.of(new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + bonusPer5m + "%", "/5m"));
    }

    private static List<SkillStatEntry> carreauxLacerantsStats(int rank) {
        int chance = Math.round(ArbaietrierPassifs.bleedChanceForRank(rank) * 100);
        int bleed = Math.round(ArbaietrierPassifs.BLEED_WEAPON_PCT * 100);
        return List.of(
            new SkillStatEntry(SkillStatKind.RATE, chance + "%"),
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, bleed + "%"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(ArbaietrierPassifs.BLEED_DURATION_MS))
        );
    }

    private static List<SkillStatEntry> reflexesAffutesStats(int rank) {
        int pct = Math.round(ArbaietrierPassifs.dodgeChanceForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.DODGE, "+" + pct + "%"));
    }

    private static List<SkillStatEntry> rodeurFlechesToxiquesStats(int rank) {
        int chance = Math.round(RodeurPassifs.poisonChanceForRank(rank) * 100);
        int poison = Math.round(RodeurPassifs.POISON_WEAPON_PCT * 100);
        return List.of(
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, poison + "%"),
            new SkillStatEntry(SkillStatKind.RATE, chance + "%"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(RodeurPassifs.POISON_DURATION_MS))
        );
    }

    private static List<SkillStatEntry> rodeurInstinctSurvieStats(int rank) {
        int pct = Math.round(RodeurPassifs.dodgeChanceForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.DODGE, "+" + pct + "%"));
    }

    private static List<SkillStatEntry> rodeurPrecisionMortelleStats(int rank) {
        int pct = Math.round(RodeurPassifs.precisionBonusForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + pct + "%"));
    }

    private static List<SkillStatEntry> rodeurTraqueMobileStats(int rank) {
        int pct = Math.round(RodeurPassifs.traqueBonusForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + pct + "%"));
    }

    private static List<SkillStatEntry> rodeurTraqueSansFinStats(int rank) {
        int pct = Math.round(RodeurPassifs.traqueCdReductionForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.COOLDOWN, "-" + pct + "%", "Délai"));
    }

    private static List<SkillStatEntry> reculStrategiqueStats(int rank) {
        int dist = (int) ReculStrategiqueSkill.dashDistanceForRank(rank);
        int spd = Math.round(ReculStrategiqueSkill.speedBonusForRank(rank) * 100);
        int stamina = Math.round(ReculStrategiqueSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.MOVE_SPEED, String.valueOf(dist), "Dist."),
            new SkillStatEntry(SkillStatKind.MOVE_SPEED, "+" + spd + "%", "Vitesse"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(ReculStrategiqueSkill.speedDurationMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(ReculStrategiqueSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> pluieDesFlechesStats(int rank) {
        int dmg = Math.round(PluieDesFlechesSkill.damagePctForRank(rank) * 100);
        int arrows = PluieDesFlechesSkill.arrowCountForRank(rank);
        int stamina = Math.round(PluieDesFlechesSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, dmg + "%"),
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, String.valueOf(arrows), "Flèches"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(PluieDesFlechesSkill.durationMs())),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(PluieDesFlechesSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> marqueDuChasseurStats(int rank) {
        int dmg = Math.round(MarqueDuChasseurSkill.damageBonusForRank(rank) * 100);
        int stamina = Math.round(MarqueDuChasseurSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + dmg + "%", "Cible"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(MarqueDuChasseurSkill.durationMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(MarqueDuChasseurSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> flecheDeReculStats(int rank) {
        int dmg = Math.round(FlecheDeReculsSkill.damagePctForRank(rank) * 100);
        int kb = (int) FlecheDeReculsSkill.knockbackForRank(rank);
        int stamina = Math.round(FlecheDeReculsSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, dmg + "%"),
            new SkillStatEntry(SkillStatKind.MOVE_SPEED, String.valueOf(kb), "Recul"),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(FlecheDeReculsSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> flecheEntravanteStats(int rank) {
        int dmg = Math.round(FlecheEntravantSkill.damagePctForRank(rank) * 100);
        int stamina = Math.round(FlecheEntravantSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, dmg + "%"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(FlecheEntravantSkill.rootMsForRank(rank)), "Immob."),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(FlecheEntravantSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> rafaleStats(int rank) {
        int dmg = Math.round(RafaleSkill.damagePctForRank(rank) * 100);
        int arrows = RafaleSkill.arrowCountForRank(rank);
        int stamina = Math.round(RafaleSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, dmg + "%"),
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, String.valueOf(arrows), "Tirs"),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(RafaleSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> elanDestructeurStats(int rank) {
        int pct = Math.round(RavageurPassifs.elanBonusForRank(rank) * 100);
        return List.of(
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + pct + "%", "Proch."),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(RavageurPassifs.ELAN_WINDOW_MS))
        );
    }

    private static List<SkillStatEntry> combattantInfatigableStats(int rank) {
        float raw = RavageurPassifs.combattantBonusPer10PctForRank(rank) * 100f;
        String pctStr = raw == Math.floor(raw)
            ? String.valueOf((int) raw)
            : String.format(Locale.FRENCH, "%.1f", raw);
        return List.of(new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + pctStr + "%/10% PV"));
    }

    private static List<SkillStatEntry> fureurSanguinaireStats(int rank) {
        int pct = Math.round(BerserkerPassifs.fureurLifestealPct(rank) * 100);
        return List.of(
            new SkillStatEntry(SkillStatKind.HEAL, pct + "%", "Vol vie"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(BerserkerPassifs.fureurDurationMs()))
        );
    }

    private static List<SkillStatEntry> frenesieStats(int rank) {
        int dmg = Math.round(BerserkerPassifs.frenesieDmgBonusPerStack(rank) * 100);
        int spd = Math.round(BerserkerPassifs.frenesieSpdBonusPerStack(rank) * 100);
        return List.of(
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + dmg + "%", "Attaque"),
            new SkillStatEntry(SkillStatKind.MOVE_SPEED, "+" + spd + "%", "Vitesse"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(BerserkerPassifs.frenesieDurationMs()))
        );
    }

    private static List<SkillStatEntry> ferveurStats(int rank) {
        return List.of(
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS,
                "+" + BerserkerPassifs.ferveurBonusPctDisplay(rank) + "%", "Cumul"),
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS,
                String.valueOf(BerserkerPassifs.FERVEUR_MAX_STACKS), "Max")
        );
    }

    private static List<SkillStatEntry> blessuresProfondesStats(int rank) {
        int chance = Math.round(BerserkerPassifs.bleedChanceForRank(rank) * 100);
        int bleed = Math.round(BerserkerPassifs.bleedWeaponPctForRank(rank) * 100);
        return List.of(
            new SkillStatEntry(SkillStatKind.RATE, chance + "%"),
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, bleed + "%"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(BerserkerPassifs.BLEED_DURATION_MS))
        );
    }

    private static List<SkillStatEntry> dernierSouffleStats(int rank) {
        return List.of(
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(BerserkerPassifs.DERNIER_SOUFFLE_DURATION_MS)),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(BerserkerPassifs.dernierSouffleCooldownMsForRank(rank)))
        );
    }

    private static List<SkillStatEntry> corDeGuerreStats(int rank) {
        int spd = Math.round(CorDeGuerreSkill.speedBonusForRank(rank) * 100);
        int stamina = Math.round(CorDeGuerreSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.MOVE_SPEED, "+" + spd + "%"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(CorDeGuerreSkill.durationMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(CorDeGuerreSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> criRalliementStats(int rank) {
        int dmg = Math.round(CriRalliementSkill.damageBonusForRank(rank) * 100);
        int stamina = Math.round(CriRalliementSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + dmg + "%"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(CriRalliementSkill.durationMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(CriRalliementSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> constitutionStats(int rank) {
        int pct = Math.round(RempartPassifs.constitutionHpBonusForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.HEAL, "+" + pct + "%", "Max"));
    }

    private static List<SkillStatEntry> gardeImpenetrableStats(int rank) {
        int red = Math.round(RempartPassifs.gardeReductionForRank(rank) * 100);
        return List.of(
            new SkillStatEntry(SkillStatKind.DEFENSE, "-" + red + "%"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(RempartPassifs.gardeDurationMs()))
        );
    }

    private static List<SkillStatEntry> infatigableStats(int rank) {
        int pct = Math.round(RempartPassifs.infatigableHealPctForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.DEFENSE, pct + "%", "Blocage"));
    }

    private static List<SkillStatEntry> contreOffensifStats(int rank) {
        int pct = Math.round(RempartPassifs.contreDamageBonusForRank(rank) * 100);
        return List.of(
            new SkillStatEntry(SkillStatKind.DAMAGE_BONUS, "+" + pct + "%", "Proch."),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(RempartPassifs.contreWindowMs()))
        );
    }

    private static List<SkillStatEntry> dernierBastionStats(int rank) {
        int pct = Math.round(RempartPassifs.bastionReductionForRank(rank) * 100);
        return List.of(new SkillStatEntry(SkillStatKind.DEFENSE, "-" + pct + "%"));
    }

    private static List<SkillStatEntry> coupDeBouclierStats(int rank) {
        int pct = Math.round(CoupDeBouclierSkill.damagePctForRank(rank) * 100);
        int stamina = Math.round(CoupDeBouclierSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, pct + "%"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(CoupDeBouclierSkill.stunMsForRank(rank)), "Étour."),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(CoupDeBouclierSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> secondSouffleRempartStats(int rank) {
        int pct = Math.round(SecondSouffleSkill.healPctForRank(rank) * 100);
        int stamina = Math.round(SecondSouffleSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.HEAL, pct + "%"),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(SecondSouffleSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> provocationStats(int rank) {
        int stamina = Math.round(ProvocationSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(ProvocationSkill.durationMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(ProvocationSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> gardeRapprocheStats(int rank) {
        int red = Math.round(GardeRapprocheSkill.damageReductionForRank(rank) * 100);
        int stamina = Math.round(GardeRapprocheSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.DEFENSE, "-" + red + "%"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(GardeRapprocheSkill.durationMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(GardeRapprocheSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> forteresseStats(int rank) {
        int red = Math.round(ForteresseSkill.damageReductionForRank(rank) * 100);
        int stamina = Math.round(ForteresseSkill.staminaCostForRank(rank));
        return List.of(
            new SkillStatEntry(SkillStatKind.DEFENSE, "-" + red + "%"),
            new SkillStatEntry(SkillStatKind.DURATION,
                formatDurationMs(ForteresseSkill.durationMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.COOLDOWN,
                formatCooldown(ForteresseSkill.cooldownMsForRank(rank))),
            new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina))
        );
    }

    private static List<SkillStatEntry> parseGeneric(String skillId, int rank, String statLine) {
        Integer dmg = extractWeaponDamagePct(statLine);
        String cd = matchString(CD_PATTERN, statLine);
        Integer stamina = matchInt(STAMINA_PATTERN, statLine);
        Integer mana = matchInt(MANA_PATTERN, statLine);

        if (cd == null) {
            Long ms = cooldownMsFromSkill(skillId, rank);
            if (ms != null) cd = formatCooldown(ms);
        }
        if (stamina == null && mana == null) {
            Float s = staminaCostFromSkill(skillId, rank);
            if (s != null) {
                stamina = Math.round(s);
            } else {
                Float m = manaCostFromSkill(skillId, rank);
                if (m != null) mana = Math.round(m);
            }
        }
        if (mana != null) stamina = null;

        List<SkillStatEntry> entries = new ArrayList<>(3);
        if (dmg != null) entries.add(new SkillStatEntry(SkillStatKind.WEAPON_DAMAGE, dmg + "%"));
        if (cd != null) entries.add(new SkillStatEntry(SkillStatKind.COOLDOWN, cd));
        if (stamina != null) entries.add(new SkillStatEntry(SkillStatKind.STAMINA, String.valueOf(stamina)));
        else if (mana != null) entries.add(new SkillStatEntry(SkillStatKind.MANA, String.valueOf(mana)));
        return entries;
    }

    @Nullable
    private static Long cooldownMsFromSkill(String skillId, int rank) {
        return null;
    }

    @Nullable
    private static Float staminaCostFromSkill(String skillId, int rank) {
        return switch (skillId) {
            case AssautEclairSkill.SKILL_ID -> AssautEclairSkill.staminaCostForRank(rank);
            case CoupEstocSkill.SKILL_ID -> CoupEstocSkill.staminaCostForRank(rank);
            case FeintSkill.SKILL_ID -> FeintSkill.staminaCostForRank(rank);
            case DesarmementSkill.SKILL_ID -> DesarmementSkill.staminaCostForRank(rank);
            case AssautBretteurSkill.SKILL_ID -> AssautBretteurSkill.staminaCostForRank(rank);
            case RiposteParfaiteSkill.SKILL_ID -> RiposteParfaiteSkill.staminaCostForRank(rank);
            case PasDesTenebresSkill.SKILL_ID -> PasDesTenebresSkill.staminaCostForRank(rank);
            case PasDeLOmbreSkill.SKILL_ID -> PasDeLOmbreSkill.staminaCostForRank(rank);
            case DelugeDeGamesSkill.SKILL_ID -> DelugeDeGamesSkill.staminaCostForRank(rank);
            case FrappeFataleSkill.SKILL_ID -> FrappeFataleSkill.staminaCostForRank(rank);
            case EcranDeFumeeSkill.SKILL_ID -> EcranDeFumeeSkill.staminaCostForRank(rank);
            case ChaseOuverteSkill.SKILL_ID -> ChaseOuverteSkill.staminaCostForRank(rank);
            case ChargeLourdeSkill.SKILL_ID -> ChargeLourdeSkill.staminaCostForRank(rank);
            case CoupDeBouclierSkill.SKILL_ID -> CoupDeBouclierSkill.staminaCostForRank(rank);
            case ForteresseSkill.SKILL_ID -> ForteresseSkill.staminaCostForRank(rank);
            case SecondSouffleSkill.SKILL_ID -> SecondSouffleSkill.staminaCostForRank(rank);
            case GardeRapprocheSkill.SKILL_ID -> GardeRapprocheSkill.staminaCostForRank(rank);
            case ProvocationSkill.SKILL_ID -> ProvocationSkill.staminaCostForRank(rank);
            case AssautBestialSkill.SKILL_ID -> AssautBestialSkill.staminaCostForRank(rank);
            case BondEcrasantSkill.SKILL_ID -> BondEcrasantSkill.staminaCostForRank(rank);
            case JeuDeJambesSkill.SKILL_ID -> JeuDeJambesSkill.staminaCostForRank(rank);
            case DirectDuDroitSkill.SKILL_ID -> DirectDuDroitSkill.staminaCostForRank(rank);
            case DelugeDeCoups2Skill.SKILL_ID -> DelugeDeCoups2Skill.staminaCostForRank(rank);
            case UppercutSkill.SKILL_ID -> UppercutSkill.staminaCostForRank(rank);
            case ReculStrategiqueSkill.SKILL_ID -> ReculStrategiqueSkill.staminaCostForRank(rank);
            case PluieDesFlechesSkill.SKILL_ID -> PluieDesFlechesSkill.staminaCostForRank(rank);
            case FlecheDeReculsSkill.SKILL_ID -> FlecheDeReculsSkill.staminaCostForRank(rank);
            case FlecheEntravantSkill.SKILL_ID -> FlecheEntravantSkill.staminaCostForRank(rank);
            case RafaleSkill.SKILL_ID -> RafaleSkill.staminaCostForRank(rank);
            case ReculTactiqueSkill.SKILL_ID -> ReculTactiqueSkill.staminaCostForRank(rank);
            case CarreauLourdSkill.SKILL_ID -> CarreauLourdSkill.staminaCostForRank(rank);
            case CarreauExplosifSkill.SKILL_ID -> CarreauExplosifSkill.staminaCostForRank(rank);
            case CarreauTranspercantSkill.SKILL_ID -> CarreauTranspercantSkill.staminaCostForRank(rank);
            case CoupDeBotteSkill.SKILL_ID -> CoupDeBotteSkill.staminaCostForRank(rank);
            case MiseEnJouSkill.SKILL_ID -> MiseEnJouSkill.staminaCostForRank(rank);
            case PerceeSkill.SKILL_ID -> PerceeSkill.staminaCostForRank(rank);
            case ChargeHeroiqueSkill.SKILL_ID -> ChargeHeroiqueSkill.staminaCostForRank(rank);
            case GardeDuLancierSkill.SKILL_ID -> GardeDuLancierSkill.staminaCostForRank(rank);
            case HarponnageSkill.SKILL_ID -> HarponnageSkill.staminaCostForRank(rank);
            case FormationDePiquesSkill.SKILL_ID -> FormationDePiquesSkill.staminaCostForRank(rank);
            case EmpalementSkill.SKILL_ID -> EmpalementSkill.staminaCostForRank(rank);
            case MonteeAdreinalineSkill.SKILL_ID -> MonteeAdreinalineSkill.staminaCostForRank(rank);
            case fr.varyon.vrpg.classes.bagarreur.SecondSouffleSkill.SKILL_ID ->
                fr.varyon.vrpg.classes.bagarreur.SecondSouffleSkill.staminaCostForRank(rank);
            case DechiquetageSkill.SKILL_ID -> DechiquetageSkill.staminaCostForRank(rank);
            case CriRalliementSkill.SKILL_ID -> CriRalliementSkill.staminaCostForRank(rank);
            case ExecutionSauvageSkill.SKILL_ID -> ExecutionSauvageSkill.staminaCostForRank(rank);
            case CorDeGuerreSkill.SKILL_ID -> CorDeGuerreSkill.staminaCostForRank(rank);
            case DixPourSangSkill.SKILL_ID -> DixPourSangSkill.staminaCostForRank(rank);
            case PeauDeFerSkill.SKILL_ID -> PeauDeFerSkill.staminaCostForRank(rank);
            case DechainementSkill.SKILL_ID -> DechainementSkill.staminaCostForRank(rank);
            case PremierAssautSkill.SKILL_ID -> PremierAssautSkill.staminaCostForRank(rank);
            case MarteauPilonSkill.SKILL_ID -> MarteauPilonSkill.staminaCostForRank(rank);
            case RabattageSkill.SKILL_ID -> RabattageSkill.staminaCostForRank(rank);
            case MarqueDuChasseurSkill.SKILL_ID -> MarqueDuChasseurSkill.staminaCostForRank(rank);
            case DistorsionSkill.SKILL_ID -> DistorsionSkill.staminaCostForRank(rank);
            case EvasionSylvestreSkill.SKILL_ID -> EvasionSylvestreSkill.staminaCostForRank(rank);
            case PassageEthereSkill.SKILL_ID -> PassageEthereSkill.staminaCostForRank(rank);
            default -> null;
        };
    }

    @Nullable
    private static Float manaCostFromSkill(String skillId, int rank) {
        return switch (skillId) {
            case BouleDeFeuSkill.SKILL_ID -> BouleDeFeuSkill.manaCostForRank(rank);
            case MeteoreSkill.SKILL_ID -> MeteoreSkill.manaCostForRank(rank);
            case NovaDeGivreSkill.SKILL_ID -> NovaDeGivreSkill.manaCostForRank(rank);
            case SalveDeGivreSkill.SKILL_ID -> SalveDeGivreSkill.manaCostForRank(rank);
            case FleauToxiqueSkill.SKILL_ID -> FleauToxiqueSkill.manaCostForRank(rank);
            case TotemEntraveSkill.SKILL_ID -> TotemEntraveSkill.manaCostForRank(rank);
            case AttaquePerfideSkill.SKILL_ID -> AttaquePerfideSkill.manaCostForRank(rank);
            case TotemVulnerabiliteSkill.SKILL_ID -> TotemVulnerabiliteSkill.manaCostForRank(rank);
            case ExtractionAmeSkill.SKILL_ID -> ExtractionAmeSkill.manaCostForRank(rank);
            case BenedictionDeGaiaSkill.SKILL_ID -> BenedictionDeGaiaSkill.manaCostForRank(rank);
            case EcorceProtectriceSkill.SKILL_ID -> EcorceProtectriceSkill.manaCostForRank(rank);
            case AppelDuTreantSkill.SKILL_ID -> AppelDuTreantSkill.manaCostForRank(rank);
            case EtreinteDeGaiaSkill.SKILL_ID -> EtreinteDeGaiaSkill.manaCostForRank(rank);
            case MarqueDeRenaissanceSkill.SKILL_ID -> MarqueDeRenaissanceSkill.manaCostForRank(rank);
            default -> null;
        };
    }

    @Nullable
    private static Integer extractWeaponDamagePct(@Nullable String statLine) {
        if (statLine == null || statLine.isBlank()) return null;
        for (Pattern pattern : WEAPON_DMG_PATTERNS) {
            Integer value = matchInt(pattern, statLine);
            if (value != null) return value;
        }
        return null;
    }

    @Nullable
    private static Integer matchInt(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    @Nullable
    private static String matchString(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    static String formatCooldown(long cooldownMs) {
        if (cooldownMs % 1000L == 0L) {
            return (cooldownMs / 1000L) + "s";
        }
        return String.format(Locale.ROOT, "%.1fs", cooldownMs / 1000.0);
    }

    static String formatDurationMs(long durationMs) {
        return formatCooldown(durationMs);
    }
}
