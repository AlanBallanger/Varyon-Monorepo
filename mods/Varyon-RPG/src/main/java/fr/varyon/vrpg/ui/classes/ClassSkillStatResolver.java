package fr.varyon.vrpg.ui.classes;

import fr.varyon.vrpg.classes.ability.AssautEclairSkill;
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
import fr.varyon.vrpg.classes.bagarreur.DirectDuDroitSkill;
import fr.varyon.vrpg.classes.bagarreur.DelugeDeCoups2Skill;
import fr.varyon.vrpg.classes.bagarreur.JeuDeJambesSkill;
import fr.varyon.vrpg.classes.bagarreur.UppercutSkill;
import fr.varyon.vrpg.classes.berserker.AssautBestialSkill;
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
import fr.varyon.vrpg.classes.ombre.PasDeLOmbreSkill;
import fr.varyon.vrpg.classes.ombre.PasDesTenebresSkill;
import fr.varyon.vrpg.classes.ravageur.BondEcrasantSkill;
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
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ClassSkillStatResolver {

    private static final Pattern DMG_PATTERN =
        Pattern.compile("(\\d+)%\\s*d[ée]g[aâ]ts\\s*arme", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern CD_PATTERN =
        Pattern.compile("CD\\s*([\\d.]+s)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STAMINA_PATTERN =
        Pattern.compile("(\\d+)\\s*endurance", Pattern.CASE_INSENSITIVE);
    private static final Pattern MANA_PATTERN =
        Pattern.compile("(\\d+)\\s*mana", Pattern.CASE_INSENSITIVE);

    private ClassSkillStatResolver() {}

    @Nullable
    public static SkillStatDisplay resolve(@Nullable String skillId, int rank, @Nullable String statLine) {
        if (skillId == null || rank < 1 || statLine == null || statLine.isBlank()) return null;

        Integer dmg = matchInt(DMG_PATTERN, statLine);
        String cd = matchString(CD_PATTERN, statLine);
        Integer stamina = matchInt(STAMINA_PATTERN, statLine);
        Integer mana = matchInt(MANA_PATTERN, statLine);

        if (cd == null) {
            Long ms = cooldownMsFromSkill(skillId, rank);
            if (ms != null) cd = formatCooldown(ms);
        }
        if (dmg == null) {
            dmg = damagePctFromSkill(skillId, rank);
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

        SkillStatDisplay display = new SkillStatDisplay(dmg, cd, stamina, mana, null);
        if (display.usesIconLayout()) return display;
        return SkillStatDisplay.fallback(statLine);
    }

    @Nullable
    private static Integer damagePctFromSkill(String skillId, int rank) {
        if (AssautEclairSkill.SKILL_ID.equals(skillId)) {
            return Math.round(AssautEclairSkill.damageFactor(rank) * 100);
        }
        return null;
    }

    @Nullable
    private static Long cooldownMsFromSkill(String skillId, int rank) {
        return null;
    }

    @Nullable
    private static Float staminaCostFromSkill(String skillId, int rank) {
        return switch (skillId) {
            case AssautEclairSkill.SKILL_ID -> AssautEclairSkill.staminaCostForRank(rank);
            case PasDesTenebresSkill.SKILL_ID -> PasDesTenebresSkill.staminaCostForRank(rank);
            case PasDeLOmbreSkill.SKILL_ID -> PasDeLOmbreSkill.staminaCostForRank(rank);
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
            default -> null;
        };
    }

    @Nullable
    private static Float manaCostFromSkill(String skillId, int rank) {
        return switch (skillId) {
            case DistorsionSkill.SKILL_ID -> DistorsionSkill.manaCostForRank(rank);
            case BouleDeFeuSkill.SKILL_ID -> BouleDeFeuSkill.manaCostForRank(rank);
            case MeteoreSkill.SKILL_ID -> MeteoreSkill.manaCostForRank(rank);
            case NovaDeGivreSkill.SKILL_ID -> NovaDeGivreSkill.manaCostForRank(rank);
            case SurchargeSkill.SKILL_ID -> SurchargeSkill.manaCostForRank(rank);
            case SalveDeGivreSkill.SKILL_ID -> SalveDeGivreSkill.manaCostForRank(rank);
            case PassageEthereSkill.SKILL_ID -> PassageEthereSkill.manaCostForRank(rank);
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
            case EvasionSylvestreSkill.SKILL_ID -> EvasionSylvestreSkill.manaCostForRank(rank);
            default -> null;
        };
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
}
