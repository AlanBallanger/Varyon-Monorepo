package fr.varyon.vrpg.classes.ability;

import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.classes.ability.AssautEclairSkill;
import fr.varyon.vrpg.classes.ability.ExpertEnDuelSkill;
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
import fr.varyon.vrpg.classes.gardiendesgaia.EvasionSylvestreSkill;
import fr.varyon.vrpg.classes.gardiendesgaia.BenedictionDeGaiaSkill;
import fr.varyon.vrpg.classes.gardiendesgaia.MarqueDeRenaissanceSkill;
import fr.varyon.vrpg.classes.gardiendesgaia.EtreinteDeGaiaSkill;
import fr.varyon.vrpg.classes.gardiendesgaia.AppelDuTreantSkill;
import fr.varyon.vrpg.classes.gardiendesgaia.EcorceProtectriceSkill;
import fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaPassifs;
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
        if (activeClass == PlayerClass.BARBARE && spec == PlayerSpecialization.BERSERKER) {
            return switch (nodeIndex) {
                case 0  -> fr.varyon.vrpg.classes.berserker.AssautBestialSkill.SKILL_ID;
                case 1  -> fr.varyon.vrpg.classes.berserker.BerserkerPassifs.CARNAGE_NODE;
                case 2  -> fr.varyon.vrpg.classes.berserker.BerserkerPassifs.FUREUR_NODE;
                case 3  -> fr.varyon.vrpg.classes.berserker.BerserkerPassifs.FRENESIE_NODE;
                case 4  -> fr.varyon.vrpg.classes.berserker.DixPourSangSkill.SKILL_ID;
                case 5  -> fr.varyon.vrpg.classes.berserker.CorDeGuerreSkill.SKILL_ID;
                case 6  -> fr.varyon.vrpg.classes.berserker.BerserkerPassifs.BLESSURES_NODE;
                case 7  -> fr.varyon.vrpg.classes.berserker.BerserkerPassifs.FERVEUR_NODE;
                case 8  -> fr.varyon.vrpg.classes.berserker.CriRalliementSkill.SKILL_ID;
                case 9  -> fr.varyon.vrpg.classes.berserker.DechiquetageSkill.SKILL_ID;
                case 10 -> fr.varyon.vrpg.classes.berserker.BerserkerPassifs.DERNIER_SOUFFLE_NODE;
                case 11 -> fr.varyon.vrpg.classes.berserker.ExecutionSauvageSkill.SKILL_ID;
                default -> null;
            };
        }
        if (activeClass == PlayerClass.GUERRIER && spec == PlayerSpecialization.REMPART) {
            return switch (nodeIndex) {
                case 0  -> fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.SKILL_ID;
                case 1  -> fr.varyon.vrpg.classes.rempart.RempartPassifs.MAITRE_BOUCLIER_NODE;
                case 2  -> fr.varyon.vrpg.classes.rempart.RempartPassifs.CONSTITUTION_NODE;
                case 3  -> fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.SKILL_ID;
                case 4  -> fr.varyon.vrpg.classes.rempart.ForteresseSkill.SKILL_ID;
                case 5  -> fr.varyon.vrpg.classes.rempart.RempartPassifs.GARDE_IMPENETRABLE_NODE;
                case 6  -> fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.SKILL_ID;
                case 7  -> fr.varyon.vrpg.classes.rempart.RempartPassifs.INFATIGABLE_NODE;
                case 8  -> fr.varyon.vrpg.classes.rempart.RempartPassifs.CONTRE_OFFENSIF_NODE;
                case 9  -> fr.varyon.vrpg.classes.rempart.ProvocationSkill.SKILL_ID;
                case 10 -> fr.varyon.vrpg.classes.rempart.RempartPassifs.DERNIER_BASTION_NODE;
                case 11 -> fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.SKILL_ID;
                default -> null;
            };
        }
        if (activeClass == PlayerClass.BARBARE && spec == PlayerSpecialization.RAVAGEUR) {
            return switch (nodeIndex) {
                case 0  -> fr.varyon.vrpg.classes.ravageur.BondEcrasantSkill.SKILL_ID;
                case 1  -> fr.varyon.vrpg.classes.ravageur.RavageurPassifs.MOISSONNEUR_NODE;
                case 2  -> fr.varyon.vrpg.classes.ravageur.RavageurPassifs.ARME_LOURDE_NODE;
                case 3  -> fr.varyon.vrpg.classes.ravageur.PeauDeFerSkill.SKILL_ID;
                case 4  -> fr.varyon.vrpg.classes.ravageur.RavageurPassifs.EXECUTEUR_NODE;
                case 5  -> fr.varyon.vrpg.classes.ravageur.DechainementSkill.SKILL_ID;
                case 6  -> fr.varyon.vrpg.classes.ravageur.RavageurPassifs.CHASSEUR_GEANT_NODE;
                case 7  -> fr.varyon.vrpg.classes.ravageur.RavageurPassifs.ELAN_DESTRUCTEUR_NODE;
                case 8  -> fr.varyon.vrpg.classes.ravageur.PremierAssautSkill.SKILL_ID;
                case 9  -> fr.varyon.vrpg.classes.ravageur.MarteauPilonSkill.SKILL_ID;
                case 10 -> fr.varyon.vrpg.classes.ravageur.RavageurPassifs.COMBATTANT_INFATIGABLE_NODE;
                case 11 -> fr.varyon.vrpg.classes.ravageur.RabattageSkill.SKILL_ID;
                default -> null;
            };
        }
        if (activeClass == PlayerClass.BARBARE && spec == PlayerSpecialization.BAGARREUR) {
            return switch (nodeIndex) {
                case 0  -> fr.varyon.vrpg.classes.bagarreur.JeuDeJambesSkill.SKILL_ID;
                case 1  -> fr.varyon.vrpg.classes.bagarreur.BagarreurPassifs.JUSQUAU_BOUT_NODE;
                case 2  -> fr.varyon.vrpg.classes.bagarreur.DirectDuDroitSkill.SKILL_ID;
                case 3  -> fr.varyon.vrpg.classes.bagarreur.MonteeAdreinalineSkill.SKILL_ID;
                case 4  -> fr.varyon.vrpg.classes.bagarreur.BagarreurPassifs.GARDE_BOXEUR_NODE;
                case 5  -> fr.varyon.vrpg.classes.bagarreur.BagarreurPassifs.ADRENALINE_NODE;
                case 6  -> fr.varyon.vrpg.classes.bagarreur.BagarreurPassifs.ACHARNEMENT_NODE;
                case 7  -> fr.varyon.vrpg.classes.bagarreur.BagarreurPassifs.ESPRIT_COMBATIF_NODE;
                case 8  -> fr.varyon.vrpg.classes.bagarreur.BagarreurPassifs.POINGS_ACIER_NODE;
                case 9  -> fr.varyon.vrpg.classes.bagarreur.DelugeDeCoups2Skill.SKILL_ID;
                case 10 -> fr.varyon.vrpg.classes.bagarreur.SecondSouffleSkill.SKILL_ID;
                case 11 -> fr.varyon.vrpg.classes.bagarreur.UppercutSkill.SKILL_ID;
                default -> null;
            };
        }
        if (activeClass == PlayerClass.MAGE && spec == PlayerSpecialization.GARDIEN_DE_GAIA) {
            return switch (nodeIndex) {
                case 0  -> EvasionSylvestreSkill.SKILL_ID;
                case 1  -> GardienDeGaiaPassifs.HARMONIE_NODE;
                case 2  -> BenedictionDeGaiaSkill.SKILL_ID;
                case 3  -> EcorceProtectriceSkill.SKILL_ID;
                case 4  -> AppelDuTreantSkill.SKILL_ID;
                case 5  -> GardienDeGaiaPassifs.LIEN_NODE;
                case 6  -> EtreinteDeGaiaSkill.SKILL_ID;
                case 7  -> GardienDeGaiaPassifs.GARDIEN_NODE;
                case 8  -> GardienDeGaiaPassifs.CYCLE_NODE;
                case 9  -> GardienDeGaiaPassifs.GRACE_NODE;
                case 10 -> GardienDeGaiaPassifs.SOUFFLE_NODE;
                case 11 -> MarqueDeRenaissanceSkill.SKILL_ID;
                default -> null;
            };
        }
        if (activeClass == PlayerClass.MAGE && spec == PlayerSpecialization.VAUDOU) {
            return switch (nodeIndex) {
                case 0  -> fr.varyon.vrpg.classes.vaudou.PassageEthereSkill.SKILL_ID;
                case 1  -> fr.varyon.vrpg.classes.vaudou.VaudouPassifs.FETICHEUR_NODE;
                case 2  -> fr.varyon.vrpg.classes.vaudou.FleauToxiqueSkill.SKILL_ID;
                case 3  -> fr.varyon.vrpg.classes.vaudou.TotemEntraveSkill.SKILL_ID;
                case 4  -> fr.varyon.vrpg.classes.vaudou.AttaquePerfideSkill.SKILL_ID;
                case 5  -> fr.varyon.vrpg.classes.vaudou.VaudouPassifs.TOXINES_NODE;
                case 6  -> fr.varyon.vrpg.classes.vaudou.TotemVulnerabiliteSkill.SKILL_ID;
                case 7  -> fr.varyon.vrpg.classes.vaudou.VaudouPassifs.PARASITE_NODE;
                case 8  -> fr.varyon.vrpg.classes.vaudou.VaudouPassifs.ANCRAGE_NODE;
                case 9  -> fr.varyon.vrpg.classes.vaudou.VaudouPassifs.RITUEL_NODE;
                case 10 -> fr.varyon.vrpg.classes.vaudou.ExtractionAmeSkill.SKILL_ID;
                case 11 -> fr.varyon.vrpg.classes.vaudou.VaudouPassifs.PRESENCE_NODE;
                default -> null;
            };
        }
        if (activeClass == PlayerClass.TIREUR && spec == PlayerSpecialization.RODEUR) {
            return switch (nodeIndex) {
                case 0  -> fr.varyon.vrpg.classes.rodeur.ReculStrategiqueSkill.SKILL_ID;
                case 1  -> fr.varyon.vrpg.classes.rodeur.RodeurPassifs.OEIL_CHASSEUR_NODE;
                case 2  -> fr.varyon.vrpg.classes.rodeur.PluieDesFlechesSkill.SKILL_ID;
                case 3  -> fr.varyon.vrpg.classes.rodeur.MarqueDuChasseurSkill.SKILL_ID;
                case 4  -> fr.varyon.vrpg.classes.rodeur.RodeurPassifs.RICOCHET_NODE;
                case 5  -> fr.varyon.vrpg.classes.rodeur.RodeurPassifs.PRECISION_MORTELLE_NODE;
                case 6  -> fr.varyon.vrpg.classes.rodeur.RodeurPassifs.FLECHES_TOXIQUES_NODE;
                case 7  -> fr.varyon.vrpg.classes.rodeur.FlecheDeReculsSkill.SKILL_ID;
                case 8  -> fr.varyon.vrpg.classes.rodeur.FlecheEntravantSkill.SKILL_ID;
                case 9  -> fr.varyon.vrpg.classes.rodeur.RodeurPassifs.FOULEE_RODEUR_NODE;
                case 10 -> fr.varyon.vrpg.classes.rodeur.RodeurPassifs.TRAQUE_SANS_FIN_NODE;
                case 11 -> fr.varyon.vrpg.classes.rodeur.RafaleSkill.SKILL_ID;
                default -> null;
            };
        }
        if (activeClass == PlayerClass.TIREUR && spec == PlayerSpecialization.ARBALETRIER) {
            return switch (nodeIndex) {
                case 0  -> fr.varyon.vrpg.classes.arbaletrier.ReculTactiqueSkill.SKILL_ID;
                case 1  -> fr.varyon.vrpg.classes.arbaletrier.ArbaietrierPassifs.TIREUR_ELITE_NODE;
                case 2  -> fr.varyon.vrpg.classes.arbaletrier.CarreauLourdSkill.SKILL_ID;
                case 3  -> fr.varyon.vrpg.classes.arbaletrier.CarreauExplosifSkill.SKILL_ID;
                case 4  -> fr.varyon.vrpg.classes.arbaletrier.CarreauTranspercantSkill.SKILL_ID;
                case 5  -> fr.varyon.vrpg.classes.arbaletrier.CoupDeBotteSkill.SKILL_ID;
                case 6  -> fr.varyon.vrpg.classes.arbaletrier.ArbaietrierPassifs.CHASSEUR_COLOSSES_NODE;
                case 7  -> fr.varyon.vrpg.classes.arbaletrier.ArbaietrierPassifs.TIREUR_EMBUSQUE_NODE;
                case 8  -> fr.varyon.vrpg.classes.arbaletrier.ArbaietrierPassifs.VISEUR_NODE;
                case 9  -> fr.varyon.vrpg.classes.arbaletrier.ArbaietrierPassifs.CARREAUX_LACERANTS_NODE;
                case 10 -> fr.varyon.vrpg.classes.arbaletrier.ArbaietrierPassifs.REFLEXES_AFFUTES_NODE;
                case 11 -> fr.varyon.vrpg.classes.arbaletrier.MiseEnJouSkill.SKILL_ID;
                default -> null;
            };
        }
        if (activeClass == PlayerClass.TIREUR && spec == PlayerSpecialization.LANCIER) {
            return switch (nodeIndex) {
                case 0  -> fr.varyon.vrpg.classes.lancier.PerceeSkill.SKILL_ID;
                case 1  -> fr.varyon.vrpg.classes.lancier.LancierPassifs.DISCIPLINE_NODE;
                case 2  -> fr.varyon.vrpg.classes.lancier.ChargeHeroiqueSkill.SKILL_ID;
                case 3  -> fr.varyon.vrpg.classes.lancier.GardeDuLancierSkill.SKILL_ID;
                case 4  -> fr.varyon.vrpg.classes.lancier.HarponnageSkill.SKILL_ID;
                case 5  -> fr.varyon.vrpg.classes.lancier.FormationDePiquesSkill.SKILL_ID;
                case 6  -> fr.varyon.vrpg.classes.lancier.LancierPassifs.POSTURE_NODE;
                case 7  -> fr.varyon.vrpg.classes.lancier.LancierPassifs.PERCE_COEUR_NODE;
                case 8  -> fr.varyon.vrpg.classes.lancier.LancierPassifs.CHASSEUR_GEANTS_NODE;
                case 9  -> fr.varyon.vrpg.classes.lancier.LancierPassifs.CONTROLE_NODE;
                case 10 -> fr.varyon.vrpg.classes.lancier.LancierPassifs.PORTEE_NODE;
                case 11 -> fr.varyon.vrpg.classes.lancier.EmpalementSkill.SKILL_ID;
                default -> null;
            };
        }
        if (activeClass == PlayerClass.MAGE && spec == PlayerSpecialization.ARCANISTE) {
            return switch (nodeIndex) {
                case 0  -> fr.varyon.vrpg.classes.arcaniste.DistorsionSkill.SKILL_ID;
                case 1  -> fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.TALENT_INNE_NODE;
                case 2  -> fr.varyon.vrpg.classes.arcaniste.BouleDeFeuSkill.SKILL_ID;
                case 3  -> fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.ECHO_ARCANIQUE_NODE;
                case 4  -> fr.varyon.vrpg.classes.arcaniste.SalveDeGivreSkill.SKILL_ID;
                case 5  -> fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.PUITS_MANA_NODE;
                case 6  -> fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.ECHO_TEMPOREL_NODE;
                case 7  -> fr.varyon.vrpg.classes.arcaniste.NovaDeGivreSkill.SKILL_ID;
                case 8  -> fr.varyon.vrpg.classes.arcaniste.SurchargeSkill.SKILL_ID;
                case 9  -> fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.DRAIN_MYSTIQUE_NODE;
                case 10 -> fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.SKILL_ID;
                case 11 -> fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.POUVOIR_GRANDISSANT_NODE;
                default -> null;
            };
        }
        return null;
    }
}
