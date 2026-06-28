package fr.varyon.vrpg.classes.ability;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.ClassStatDefinition;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.classes.duelliste.AssautBretteurSkill;
import fr.varyon.vrpg.classes.duelliste.DesarmementSkill;
import fr.varyon.vrpg.classes.ability.AssautEclairSkill;
import fr.varyon.vrpg.classes.duelliste.DuellisteState;
import fr.varyon.vrpg.classes.duelliste.CoupEstocSkill;
import fr.varyon.vrpg.classes.duelliste.FeintSkill;
import fr.varyon.vrpg.classes.duelliste.RiposteParfaiteSkill;
import fr.varyon.vrpg.classes.ombre.OmbreState;
import fr.varyon.vrpg.classes.ombre.PasDesTenebresSkill;
import fr.varyon.vrpg.classes.ombre.EcranDeFumeeSkill;
import fr.varyon.vrpg.classes.ombre.FrappeFataleSkill;
import fr.varyon.vrpg.classes.ombre.DelugeDeGamesSkill;
import fr.varyon.vrpg.classes.ombre.PasDeLOmbreSkill;
import fr.varyon.vrpg.classes.ombre.ChaseOuverteSkill;
import fr.varyon.vrpg.classes.ombre.OmbrePassifs;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import fr.varyon.vrpg.audio.ClassSkillSounds;
import fr.varyon.vrpg.ui.RpgUiAdmin;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClassSkillService {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    @FunctionalInterface
    private interface SkillCaster {
        boolean cast(@Nonnull UUID uuid,
                     @Nonnull PlayerRef playerRef,
                     @Nonnull Ref<EntityStore> entityRef,
                     @Nonnull Store<EntityStore> store,
                     @Nullable CommandBuffer<EntityStore> commandBuffer);
    }

    private final ClassManager classManager;
    private final DuellisteState duellisteState;
    private final OmbreState ombreState;
    private final fr.varyon.vrpg.classes.rempart.RempartState rempartState;
    private final fr.varyon.vrpg.classes.berserker.BerserkerState berserkerState;
    private final fr.varyon.vrpg.classes.ravageur.RavageurState ravageurState;
    private final fr.varyon.vrpg.classes.bagarreur.BagarreurState bagarreurState;
    private final fr.varyon.vrpg.classes.arcaniste.ArcanistState arcanistState;
    private final fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaState gardienState;
    private final fr.varyon.vrpg.classes.vaudou.VaudouState vaudouState;
    private fr.varyon.vrpg.classes.vaudou.VaudouPoisonSystem vaudouPoisonSystem;
    private final fr.varyon.vrpg.classes.rodeur.RodeurState rodeurState;
    private final fr.varyon.vrpg.classes.arbaletrier.ArbaietrierState arbaState;
    private fr.varyon.vrpg.classes.rodeur.RodeurPoisonSystem rodeurPoisonSystem;
    @Nullable private fr.varyon.vrpg.classes.arbaletrier.CarreauExplosifGroundSystem carreauExplosifGroundSystem;
    private final fr.varyon.vrpg.classes.lancier.LancierState lancierState;
    private final fr.varyon.vrpg.classes.lancier.LancierBleedSystem lancierBleedSystem;
    @Nullable private fr.varyon.vrpg.classes.lancier.FormationDePiquesZoneSystem formationZoneSystem;
    private final ClassSkillCooldowns cooldowns = new ClassSkillCooldowns();
    private final Map<String, SkillCaster> casters = new HashMap<>();

    public ClassSkillService(@Nonnull ClassManager classManager,
                             @Nonnull DuellisteState duellisteState,
                             @Nonnull OmbreState ombreState,
                             @Nonnull fr.varyon.vrpg.classes.rempart.RempartState rempartState,
                             @Nonnull fr.varyon.vrpg.classes.berserker.BerserkerState berserkerState,
                             @Nonnull fr.varyon.vrpg.classes.ravageur.RavageurState ravageurState,
                             @Nonnull fr.varyon.vrpg.classes.bagarreur.BagarreurState bagarreurState,
                             @Nonnull fr.varyon.vrpg.classes.arcaniste.ArcanistState arcanistState,
                             @Nonnull fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaState gardienState,
                             @Nonnull fr.varyon.vrpg.classes.vaudou.VaudouState vaudouState,
                             @Nonnull fr.varyon.vrpg.classes.rodeur.RodeurState rodeurState,
                             @Nonnull fr.varyon.vrpg.classes.arbaletrier.ArbaietrierState arbaState,
                             @Nonnull fr.varyon.vrpg.classes.lancier.LancierState lancierState,
                             @Nonnull fr.varyon.vrpg.classes.lancier.LancierBleedSystem lancierBleedSystem) {
        this.classManager = classManager;
        this.duellisteState = duellisteState;
        this.ombreState = ombreState;
        this.rempartState = rempartState;
        this.berserkerState = berserkerState;
        this.ravageurState = ravageurState;
        this.bagarreurState = bagarreurState;
        this.arcanistState = arcanistState;
        this.gardienState = gardienState;
        this.vaudouState = vaudouState;
        this.rodeurState = rodeurState;
        this.arbaState = arbaState;
        this.lancierState = lancierState;
        this.lancierBleedSystem = lancierBleedSystem;
        registerCasters();
    }

    public void setVaudouPoisonSystem(@Nonnull fr.varyon.vrpg.classes.vaudou.VaudouPoisonSystem sys) {
        this.vaudouPoisonSystem = sys;
    }

    public void setRodeurPoisonSystem(@Nonnull fr.varyon.vrpg.classes.rodeur.RodeurPoisonSystem sys) {
        this.rodeurPoisonSystem = sys;
    }

    public void setCarreauExplosifGroundSystem(@Nonnull fr.varyon.vrpg.classes.arbaletrier.CarreauExplosifGroundSystem sys) {
        this.carreauExplosifGroundSystem = sys;
    }

    public void setFormationDePiquesZoneSystem(@Nonnull fr.varyon.vrpg.classes.lancier.FormationDePiquesZoneSystem sys) {
        this.formationZoneSystem = sys;
    }

    public ClassSkillCooldowns getCooldowns() {
        return cooldowns;
    }

    private void registerCasters() {
        casters.put(AssautEclairSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastAssautEclair(uuid, pr, er, st, cb));
        casters.put(AssautBretteurSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastAssautBretteur(uuid, pr));
        casters.put(DesarmementSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastDesarmement(uuid, pr, er, st));
        casters.put(CoupEstocSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastCoupEstoc(uuid, pr, er, st));
        casters.put(FeintSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastFeinte(uuid, pr, er, st));
        casters.put(RiposteParfaiteSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastRiposte(uuid, pr, er, st));
        casters.put(PasDesTenebresSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastPasDesTenebres(uuid, pr, er, st, cb));
        casters.put(EcranDeFumeeSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastEcranDeFumee(uuid, pr));
        casters.put(FrappeFataleSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastFrappeFatale(uuid, pr));
        casters.put(DelugeDeGamesSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastDelugeDeGames(uuid, pr, er, st));
        casters.put(PasDeLOmbreSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastPasDeLOmbre(uuid, pr, er, st));
        casters.put(ChaseOuverteSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastChaseOuverte(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastChargeLourde(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastCoupDeBouclier(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.rempart.ForteresseSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastForteresse(uuid, pr));
        casters.put(fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastSecondSouffleRempart(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastGardeRapprochee(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.rempart.ProvocationSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastProvocation(uuid, pr, er, st));
        // Berserker
        casters.put(fr.varyon.vrpg.classes.berserker.AssautBestialSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastAssautBestial(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.berserker.DechiquetageSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastDechiquetage(uuid, pr));
        casters.put(fr.varyon.vrpg.classes.berserker.CriRalliementSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastCriRalliement(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.berserker.ExecutionSauvageSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastExecutionSauvage(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.berserker.DixPourSangSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastDixPourSang(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.berserker.CorDeGuerreSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastCorDeGuerre(uuid, pr, er, st));
        // Bagarreur
        casters.put(fr.varyon.vrpg.classes.bagarreur.JeuDeJambesSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastJeuDeJambes(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.bagarreur.MonteeAdreinalineSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastMonteeAdrenaline(uuid, pr));
        casters.put(fr.varyon.vrpg.classes.bagarreur.DirectDuDroitSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastDirectDuDroit(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.bagarreur.DelugeDeCoups2Skill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastDelugeDeCoups2(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.bagarreur.SecondSouffleSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastSecondSouffleBagarreur(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.bagarreur.UppercutSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastUppercut(uuid, pr, er, st));
        // Ravageur
        casters.put(fr.varyon.vrpg.classes.ravageur.BondEcrasantSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastBondEcrasant(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.ravageur.PeauDeFerSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastPeauDeFer(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.ravageur.DechainementSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastDechainement(uuid, pr));
        casters.put(fr.varyon.vrpg.classes.ravageur.PremierAssautSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastPremierAssaut(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.ravageur.MarteauPilonSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastMarteauPilon(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.ravageur.RabattageSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastRabattage(uuid, pr, er, st));
        // Gardien de Gaïa
        casters.put(fr.varyon.vrpg.classes.gardiendesgaia.EvasionSylvestreSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastEvasionSylvestre(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.gardiendesgaia.BenedictionDeGaiaSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastBenedictionDeGaia(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.gardiendesgaia.MarqueDeRenaissanceSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastMarqueDeRenaissance(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.gardiendesgaia.EtreinteDeGaiaSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastEtreinteDeGaia(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.gardiendesgaia.AppelDuTreantSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastAppelDuTreant(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.gardiendesgaia.EcorceProtectriceSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastEcorceProtectrice(uuid, pr, er, st));
        // Arcaniste
        // Vaudou
        casters.put(fr.varyon.vrpg.classes.vaudou.PassageEthereSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastPassageEthere(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.vaudou.FleauToxiqueSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastFleauToxique(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.vaudou.TotemEntraveSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastTotemEntrave(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.vaudou.AttaquePerfideSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastAttaquePerfide(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.vaudou.TotemVulnerabiliteSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastTotemVulnerabilite(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.vaudou.ExtractionAmeSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastExtractionAme(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.arcaniste.DistorsionSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastDistorsion(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.arcaniste.BouleDeFeuSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastBouleDeFeu(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastMeteore(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.arcaniste.NovaDeGivreSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastNovaDeGivre(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.arcaniste.SurchargeSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastSurcharge(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.arcaniste.SalveDeGivreSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastSalveDeGivre(uuid, pr, er, st, cb));
        // Rôdeur
        casters.put(fr.varyon.vrpg.classes.rodeur.ReculStrategiqueSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastReculStrategique(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.rodeur.PluieDesFlechesSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastPluieDesFleches(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.rodeur.MarqueDuChasseurSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastMarqueDuChasseur(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.rodeur.FlecheDeReculsSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastFlecheDeRecul(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.rodeur.FlecheEntravantSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastFlecheEntravante(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.rodeur.RafaleSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastRafale(uuid, pr, er, st));
        // Arbalétrier
        casters.put(fr.varyon.vrpg.classes.arbaletrier.ReculTactiqueSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastReculTactique(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.arbaletrier.CarreauLourdSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastCarreauLourd(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.arbaletrier.CarreauExplosifSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastCarreauExplosif(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.arbaletrier.CarreauTranspercantSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastCarreauTranspercant(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.arbaletrier.CoupDeBotteSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastCoupDeBotte(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.arbaletrier.MiseEnJouSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastMiseEnJou(uuid, pr));
        // Lancier
        casters.put(fr.varyon.vrpg.classes.lancier.PerceeSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastPercee(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.lancier.ChargeHeroiqueSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastChargeHeroique(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.lancier.GardeDuLancierSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastGardeDuLancier(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.lancier.HarponnageSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastHarponnage(uuid, pr, er, st, cb));
        casters.put(fr.varyon.vrpg.classes.lancier.FormationDePiquesSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastFormationDePiques(uuid, pr, er, st));
        casters.put(fr.varyon.vrpg.classes.lancier.EmpalementSkill.SKILL_ID,
            (uuid, pr, er, st, cb) -> tryCastEmpalement(uuid, pr, er, st));
    }

    public boolean tryCast(@Nonnull String skillId,
                           @Nonnull UUID uuid,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull Ref<EntityStore> entityRef,
                           @Nonnull Store<EntityStore> store,
                           @Nullable CommandBuffer<EntityStore> commandBuffer) {
        SkillCaster caster = casters.get(skillId);
        if (caster == null) return false;
        if (arbaState.isMiseEnJouVisant(uuid) &&
                !fr.varyon.vrpg.classes.arbaletrier.MiseEnJouSkill.SKILL_ID.equals(skillId)) return false;
        if (fr.varyon.vrpg.config.VrpgConfig.isDebugCombat())
            LOG.atInfo().log("[SkillCast] uuid=" + uuid.toString().substring(0, 8) + " skill=" + skillId);
        return caster.cast(uuid, playerRef, entityRef, store, commandBuffer);
    }

    public boolean tryCastAssautEclair(@Nonnull UUID uuid,
                                       @Nonnull PlayerRef playerRef,
                                       @Nonnull Ref<EntityStore> entityRef,
                                       @Nonnull Store<EntityStore> store,
                                       @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        PlayerClass activeClass = acc.getActiveClass();
        if (activeClass != PlayerClass.GUERRIER) return false;
        if (acc.getActiveSpec(activeClass) != PlayerSpecialization.DUELLISTE) return false;
        if (!isHoldingSword(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(activeClass, AssautEclairSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;

        long cooldownMs = AssautEclairSkill.cooldownMsForRank(rank);
        boolean bypassCooldown = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypassCooldown && cooldowns.isOnCooldown(uuid, AssautEclairSkill.SKILL_ID, cooldownMs)) return false;

        float staminaCost = AssautEclairSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        if (!AssautEclairSkill.execute(playerRef, entityRef, store, commandBuffer, rank)) return false;
        ClassSkillStamina.consume(playerRef, staminaCost);
        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                ClassSkillSounds.playSkillSound(ClassSkillSounds.ASSAUT_ECLAIR_SOUND, playerRef, tc.getPosition(), commandBuffer);
            }
        } catch (Exception ignored) {}
        cooldowns.markUsed(uuid, AssautEclairSkill.SKILL_ID);
        return true;
    }

    public boolean tryCastAssautBretteur(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isDuelliste(acc)) return false;
        if (!isHoldingSword(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, AssautBretteurSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, AssautBretteurSkill.SKILL_ID, AssautBretteurSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = AssautBretteurSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;
        ClassSkillStamina.consume(playerRef, staminaCost);
        duellisteState.startAssautBretteur(uuid, AssautBretteurSkill.durationMsForRank(rank));
        if (!bypass) cooldowns.markUsed(uuid, AssautBretteurSkill.SKILL_ID);
        try {
            for (com.hypixel.hytale.server.core.universe.PlayerRef pr :
                    new java.util.ArrayList<>(com.hypixel.hytale.server.core.universe.Universe.get().getPlayers())) {
                if (pr.getUuid().equals(uuid)) {
                    fr.varyon.vrpg.audio.ClassSkillSounds.playSkillSound(
                        fr.varyon.vrpg.audio.ClassSkillSounds.ASSAUT_BRETTEUR_SOUND,
                        pr, new org.joml.Vector3d(), null);
                    ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", pr, new org.joml.Vector3d(), null);
                    break;
                }
            }
        } catch (Exception ignored) {}
        notifySkill(uuid, "Assaut du Bretteur");
        return true;
    }

    public boolean tryCastDesarmement(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef,
                                      @Nonnull Ref<EntityStore> entityRef,
                                      @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isDuelliste(acc)) return false;
        if (!isHoldingSword(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, DesarmementSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, DesarmementSkill.SKILL_ID, DesarmementSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = DesarmementSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;
        Ref<EntityStore> targeted = findTargetedNpcRef(playerRef, entityRef, store);
        if (targeted == null) return false;
        ClassSkillStamina.consume(playerRef, staminaCost);
        try {
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Sword", "SwingRight", true, store);
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                ClassSkillSounds.playSkillSound("SFX_Sword_T2_Swing", playerRef, tc.getPosition(), null);
            }
            int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
            float dmg = weaponDmg > 0 ? (float) weaponDmg : 10f;
            com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(targeted, store,
                new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                    com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, dmg));
        } catch (Exception ignored) {}
        float durationSec = DesarmementSkill.durationMsForRank(rank) / 1000f;
        duellisteState.startDesarmement(uuid, targeted.getIndex(), DesarmementSkill.durationMsForRank(rank));
        try {
            int idx = com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap()
                .getIndex("Vrpg_Desarmement");
            com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect effect =
                (com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect)
                com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getAsset(idx);
            if (effect != null) {
                com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent ec =
                    store.getComponent(targeted,
                        com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
                if (ec != null) {
                    ec.addEffect(targeted, effect, durationSec,
                        com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior.OVERWRITE, store);
                }
            }
        } catch (Exception ignored) {}
        if (!bypass) cooldowns.markUsed(uuid, DesarmementSkill.SKILL_ID);
        notifySkill(uuid, "Désarmement");
        return true;
    }

    public boolean tryCastCoupEstoc(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef,
                                    @Nonnull Ref<EntityStore> entityRef,
                                    @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isDuelliste(acc)) return false;
        if (!isHoldingSword(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, CoupEstocSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        long cd = CoupEstocSkill.cooldownMsForRank(rank);
        if (!bypass && cooldowns.isOnCooldown(uuid, CoupEstocSkill.SKILL_ID, cd)) return false;
        float staminaCost = CoupEstocSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;
        ClassSkillStamina.consume(playerRef, staminaCost);
        // AoE au cast
        try {
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Sword", "Stab", true, store);
            long casterIdx = entityRef.getIndex();
            float dmg = CoupEstocSkill.castDamageForRank(rank, playerRef);
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                ClassSkillSounds.playSkillSound(ClassSkillSounds.COUP_ESTOC_SOUND, playerRef, tc.getPosition(), null);
                com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector.selectNearbyEntities(
                    store, tc.getPosition(), CoupEstocSkill.castRadius(), targetRef -> {
                        try {
                            if (targetRef.getIndex() == casterIdx) return;
                            com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(
                                targetRef, store,
                                new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                                    com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, dmg));
                        } catch (Exception ignored) {}
                    }, t2 -> t2.getIndex() != casterIdx);
            }
        } catch (Exception ignored) {}
        // Arme le prochain coup
        duellisteState.armCoupEstoc(uuid, CoupEstocSkill.nextHitMultForRank(rank), CoupEstocSkill.armedWindowMs());
        if (!bypass) cooldowns.markUsed(uuid, CoupEstocSkill.SKILL_ID);
        notifySkill(uuid, "Coup d'Estoc");
        return true;
    }

    public boolean tryCastFeinte(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef,
                                  @Nonnull Ref<EntityStore> entityRef,
                                  @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isDuelliste(acc)) return false;
        if (!isHoldingSword(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, FeintSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        long cd = FeintSkill.cooldownMsForRank(rank);
        if (!bypass && cooldowns.isOnCooldown(uuid, FeintSkill.SKILL_ID, cd)) return false;
        float staminaCost = FeintSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;
        ClassSkillStamina.consume(playerRef, staminaCost);
        duellisteState.startFeinte(uuid, FeintSkill.windowMs());
        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                ClassSkillSounds.playSkillSound("SFX_Sword_T2_Signature_Part_1", playerRef, tc.getPosition(), null);
            }
        } catch (Exception ignored) {}
        if (!bypass) cooldowns.markUsed(uuid, FeintSkill.SKILL_ID);
        notifySkill(uuid, "Feinte");
        return true;
    }

    public boolean tryCastRiposte(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef,
                                   @Nonnull Ref<EntityStore> entityRef,
                                   @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isDuelliste(acc)) return false;
        if (!isHoldingSword(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, RiposteParfaiteSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        long cd = RiposteParfaiteSkill.cooldownMsForRank(rank);
        if (!bypass && cooldowns.isOnCooldown(uuid, RiposteParfaiteSkill.SKILL_ID, cd)) return false;
        float staminaCost = RiposteParfaiteSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;
        ClassSkillStamina.consume(playerRef, staminaCost);
        duellisteState.startRiposteWindow(uuid, RiposteParfaiteSkill.windowMsForRank(rank), rank);
        try {
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Sword", "Guard", true, store);
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                ClassSkillSounds.playSkillSound("SFX_Sword_T1_Block_Local", playerRef, tc.getPosition(), null);
            }
        } catch (Exception ignored) {}
        if (!bypass) cooldowns.markUsed(uuid, RiposteParfaiteSkill.SKILL_ID);
        notifySkill(uuid, "Riposte Parfaite");
        return true;
    }

    public boolean tryCastPasDesTenebres(@Nonnull UUID uuid,
                                          @Nonnull PlayerRef playerRef,
                                          @Nonnull Ref<EntityStore> entityRef,
                                          @Nonnull Store<EntityStore> store,
                                          @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, PasDesTenebresSkill.TALENT_NODE_ID);
        if (rank <= 0) rank = 1;
        long cdMs = PasDesTenebresSkill.cooldownMsForRank(rank);
        boolean onCd = !bypass && cooldowns.isOnCooldown(uuid, PasDesTenebresSkill.SKILL_ID, cdMs);
        if (onCd) return false;
        float staminaCost = PasDesTenebresSkill.staminaCostForRank(rank);
        boolean hasStamina = ClassSkillStamina.hasEnough(playerRef, staminaCost);
        if (!hasStamina) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            if (tc != null && hr != null) {
                org.joml.Vector3d dir = hr.getDirection();
                double dx = dir.x, dz = dir.z;
                double len = Math.sqrt(dx*dx + dz*dz);
                if (len > 1e-6) { dx /= len; dz /= len; }

                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Daggers", "DashBackward", true,
                    commandBuffer != null ? commandBuffer : store);
                ClassSkillSounds.playSkillSound("SFX_Daggers_T1_Pounce", playerRef, tc.getPosition(), commandBuffer);
                try {
                    final org.joml.Vector3d vanishPos = new org.joml.Vector3d(tc.getPosition());
                    java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread t = new Thread(r, "ombre-vanish-sound"); t.setDaemon(true); return t;
                    }).schedule(() -> ClassSkillSounds.playSkillSound(
                        "SFX_Vrpg_OmbreVanish", playerRef, vanishPos, null),
                        100, java.util.concurrent.TimeUnit.MILLISECONDS);
                } catch (Exception ignored2) {}

                double dist = PasDesTenebresSkill.dashDistanceForRank(rank);
                org.joml.Vector3d newPos = new org.joml.Vector3d(
                    tc.getPosition().x - dx * dist,
                    tc.getPosition().y,
                    tc.getPosition().z - dz * dist);
                com.hypixel.hytale.math.vector.Rotation3fc curRot = hr.getRotation();
                com.hypixel.hytale.math.vector.Rotation3f keepRot = new com.hypixel.hytale.math.vector.Rotation3f(
                    curRot.pitch(), curRot.yaw(), curRot.roll());
                com.hypixel.hytale.server.core.modules.entity.teleport.Teleport tele =
                    com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.createForPlayer(
                        newPos, keepRot);
                tele.withoutVelocityReset();
                (commandBuffer != null ? commandBuffer : store).addComponent(entityRef,
                    com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.getComponentType(), tele);

                com.hypixel.hytale.server.core.modules.physics.component.Velocity vel =
                    commandBuffer != null
                        ? commandBuffer.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType())
                        : store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType());
                if (vel != null) {
                    org.joml.Vector3d dashVel = new org.joml.Vector3d(-dx * 30.0, 0.2, -dz * 30.0);
                    vel.setClient(dashVel);
                    vel.getInstructions().clear();
                    vel.addInstruction(dashVel, null, com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                }
            }
        } catch (Exception ignored) {}

        long stealthMs = PasDesTenebresSkill.stealthDurationMs(rank);
        ombreState.startStealth(uuid, stealthMs);
        fr.varyon.vrpg.classes.ombre.OmbreStealthAggroResetSystem.resetAggroAround(entityRef, store);
        int embRank = acc.getTalentRank(PlayerClass.GUERRIER, OmbrePassifs.EMBUSCADE_NODE);
        if (embRank > 0) ombreState.armEmbuscade(uuid, stealthMs + 2000, embRank);

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, PasDesTenebresSkill.SKILL_ID);
        notifySkill(uuid, "Pas des Ténèbres");
        return true;
    }

    public boolean tryCastEcranDeFumee(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, EcranDeFumeeSkill.TALENT_NODE_ID);
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, EcranDeFumeeSkill.SKILL_ID, EcranDeFumeeSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = EcranDeFumeeSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        long durationMs = EcranDeFumeeSkill.durationMsForRank(rank);
        ombreState.startEcranFumee(uuid, durationMs);
        try {
            Ref<EntityStore> ecranRef = playerRef.getReference();
            if (ecranRef != null && ecranRef.isValid()) {
                fr.varyon.vrpg.classes.ombre.OmbreStealthAggroResetSystem.resetAggroAround(
                    ecranRef, ecranRef.getStore());
            }
        } catch (Exception ignored) {}
        int embRank = acc.getTalentRank(PlayerClass.GUERRIER, OmbrePassifs.EMBUSCADE_NODE);
        if (embRank > 0) ombreState.armEmbuscade(uuid, durationMs + 2000, embRank);
        try {
            for (com.hypixel.hytale.server.core.universe.PlayerRef pr :
                    new java.util.ArrayList<>(com.hypixel.hytale.server.core.universe.Universe.get().getPlayers())) {
                if (pr.getUuid().equals(uuid)) {
                    ClassSkillSounds.playSkillSound("SFX_Vrpg_OmbreVanish", pr, new org.joml.Vector3d(), null);
                    break;
                }
            }
        } catch (Exception ignored) {}
        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, EcranDeFumeeSkill.SKILL_ID);
        notifySkill(uuid, "Écran de Fumée");
        return true;
    }

    public boolean tryCastFrappeFatale(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isOmbre(acc)) return false;
        if (!isHoldingDagger(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, FrappeFataleSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, FrappeFataleSkill.SKILL_ID, FrappeFataleSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = FrappeFataleSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        ombreState.armFrappeFatale(uuid, FrappeFataleSkill.armedWindowMs());
        try {
            for (com.hypixel.hytale.server.core.universe.PlayerRef pr :
                    new java.util.ArrayList<>(com.hypixel.hytale.server.core.universe.Universe.get().getPlayers())) {
                if (pr.getUuid().equals(uuid)) {
                    ClassSkillSounds.playSkillSound(ClassSkillSounds.FRAPPE_FATALE_SOUND, pr, new org.joml.Vector3d(), null);
                    ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", pr, new org.joml.Vector3d(), null);
                    break;
                }
            }
        } catch (Exception ignored) {}
        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, FrappeFataleSkill.SKILL_ID);
        notifySkill(uuid, "Frappe Fatale");
        return true;
    }

    public boolean tryCastDelugeDeGames(@Nonnull UUID uuid,
                                        @Nonnull PlayerRef playerRef,
                                        @Nonnull Ref<EntityStore> entityRef,
                                        @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isOmbre(acc)) return false;
        if (!isHoldingDagger(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, DelugeDeGamesSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, DelugeDeGamesSkill.SKILL_ID, DelugeDeGamesSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = DelugeDeGamesSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            if (tc != null && hr != null) {
                org.joml.Vector3d pos = tc.getPosition();
                org.joml.Vector3d dir = hr.getDirection();
                double searchRadius = 2.5;
                long casterIdx = entityRef.getIndex();
                int strikes = DelugeDeGamesSkill.strikeCountForRank(rank);
                float dmgPct = DelugeDeGamesSkill.damagePctForRank(rank);
                int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
                float baseDmg = weaponDmg > 0 ? weaponDmg : 1f;

                org.joml.Vector3d target = new org.joml.Vector3d(
                    pos.x + dir.x * 2.0, pos.y + dir.y * 2.0 + 0.5, pos.z + dir.z * 2.0);

                com.hypixel.hytale.server.core.universe.world.World world = null;
                try {
                    java.util.UUID worldUuid = playerRef.getWorldUuid();
                    if (worldUuid != null) world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(worldUuid);
                } catch (Exception ignored) {}
                final com.hypixel.hytale.server.core.universe.world.World delugeWorld = world;
                if (delugeWorld == null) return false;

                java.util.concurrent.ScheduledExecutorService delugeExec =
                    java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread t = new Thread(r, "ombre-deluge");
                        t.setDaemon(true);
                        return t;
                    });
                int[] strikesDone = {0};
                delugeExec.scheduleAtFixedRate(() -> {
                    try {
                        if (strikesDone[0] >= strikes) {
                            delugeExec.shutdown();
                            return;
                        }
                        strikesDone[0]++;
                        boolean leftStrike = (strikesDone[0] % 2 == 1);
                        ClassSkillSounds.playSkillSound("SFX_Daggers_T2_Slash_Impact", playerRef, pos, null);
                        delugeWorld.execute(() -> {
                            try {
                                String anim = leftStrike ? "SwingLeft" : "SwingRight";
                                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Daggers", anim, true, store);
                                com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector
                                    .selectNearbyEntities(store, target, searchRadius, targetRef -> {
                                        try {
                                            if (targetRef.getIndex() == casterIdx) return;
                                            float finalDmg = baseDmg * dmgPct;
                                            com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap sm =
                                                store.getComponent(targetRef, com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.getComponentType());
                                            if (sm != null) {
                                                try {
                                                    int hIdx = com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes.getHealth();
                                                    var hpStat = sm.get(hIdx);
                                                    if (hpStat != null && hpStat.getMax() > 0
                                                            && (hpStat.get() / hpStat.getMax()) < DelugeDeGamesSkill.lowHpThreshold()) {
                                                        finalDmg *= (1f + DelugeDeGamesSkill.lowHpBonus());
                                                    }
                                                } catch (Exception ignored2) {}
                                            }
                                            com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(
                                                targetRef, store,
                                                new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                                                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                                                    com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, finalDmg));
                                        } catch (Exception ignored) {}
                                    }, ref -> ref.getIndex() != casterIdx);
                            } catch (Exception ignored) {}
                        });
                    } catch (Exception ignored) {}
                }, 0, DelugeDeGamesSkill.intervalMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, DelugeDeGamesSkill.SKILL_ID);
        notifySkill(uuid, "Déluge de Lames");
        return true;
    }

    public boolean tryCastPasDeLOmbre(@Nonnull UUID uuid,
                                      @Nonnull PlayerRef playerRef,
                                      @Nonnull Ref<EntityStore> entityRef,
                                      @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isOmbre(acc)) return false;
        if (!isHoldingDagger(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, PasDeLOmbreSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, PasDeLOmbreSkill.SKILL_ID, PasDeLOmbreSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = PasDeLOmbreSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        Ref<EntityStore> targeted = findTargetedNpcRef(playerRef, entityRef, store, PasDeLOmbreSkill.rangeForRank(rank));
        if (targeted == null) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            TransformComponent targetTc = store.getComponent(targeted, TransformComponent.getComponentType());

            if (tc != null && targetTc != null) {
                org.joml.Vector3d targetPos = targetTc.getPosition();

                double toPlayerX = tc.getPosition().x - targetPos.x;
                double toPlayerZ = tc.getPosition().z - targetPos.z;
                double toPlayerLen = Math.sqrt(toPlayerX*toPlayerX + toPlayerZ*toPlayerZ);
                if (toPlayerLen > 1e-6) { toPlayerX /= toPlayerLen; toPlayerZ /= toPlayerLen; }

                double behindX = targetPos.x - toPlayerX * 1.5;
                double behindZ = targetPos.z - toPlayerZ * 1.5;

                org.joml.Vector3d behindPos = new org.joml.Vector3d(behindX, targetPos.y, behindZ);
                double facingDx = targetPos.x - behindX;
                double facingDz = targetPos.z - behindZ;
                float facingYaw = (float) Math.atan2(-facingDx, -facingDz);
                com.hypixel.hytale.math.vector.Rotation3f faceRot = new com.hypixel.hytale.math.vector.Rotation3f(0f, facingYaw, 0f);
                com.hypixel.hytale.server.core.modules.entity.teleport.Teleport tele2 =
                    com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.createForPlayer(
                        behindPos, faceRot);
                tele2.withoutVelocityReset();
                store.addComponent(entityRef,
                    com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.getComponentType(), tele2);

                ClassSkillSounds.playSkillSound("SFX_Daggers_T1_Pounce", playerRef, targetPos, null);
                try {
                    com.hypixel.hytale.server.core.universe.world.World stabWorld = null;
                    try {
                        java.util.UUID wUuid = playerRef.getWorldUuid();
                        if (wUuid != null) stabWorld = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(wUuid);
                    } catch (Exception ignored3) {}
                    if (stabWorld != null) {
                        final com.hypixel.hytale.server.core.universe.world.World fw = stabWorld;
                        final Ref<EntityStore> stabRef = entityRef;
                        final org.joml.Vector3d soundPos = new org.joml.Vector3d(targetPos);
                        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                            Thread t = new Thread(r, "ombre-stab"); t.setDaemon(true); return t;
                        }).schedule(() -> fw.execute(() -> {
                            try { AnimationUtils.playAnimation(stabRef, AnimationSlot.Action, "Daggers", "SwingRight", true, store); } catch (Exception ignored3) {}
                            ClassSkillSounds.playSkillSound("SFX_Vrpg_PasDeLOmbre_Strike", playerRef, soundPos, null);
                        }), 100, java.util.concurrent.TimeUnit.MILLISECONDS);
                    }
                } catch (Exception ignored2) {}

                int weaponDmg2 = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
                float baseDmg2 = weaponDmg2 > 0 ? weaponDmg2 : 1f;
                try {
                    com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap sm =
                        store.getComponent(targeted, com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.getComponentType());
                    if (sm != null) {
                        int hIdx = com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes.getHealth();
                        var hpStat = sm.get(hIdx);
                        if (hpStat != null && hpStat.getMax() > 0
                                && (hpStat.get() / hpStat.getMax()) < PasDeLOmbreSkill.lowHpThreshold()) {
                            baseDmg2 *= (1f + PasDeLOmbreSkill.lowHpBonus());
                        }
                    }
                } catch (Exception ignored) {}
                final float skillDmg = baseDmg2 * PasDeLOmbreSkill.damagePctForRank(rank);
                final Ref<EntityStore> skillTarget = targeted;
                final Ref<EntityStore> skillCaster = entityRef;
                com.hypixel.hytale.server.core.universe.world.World dmgWorldRef = null;
                try { java.util.UUID wUuid2 = playerRef.getWorldUuid(); if (wUuid2 != null) dmgWorldRef = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(wUuid2); } catch (Exception ignored2) {}
                if (dmgWorldRef != null) {
                    final com.hypixel.hytale.server.core.universe.world.World dmgWorld = dmgWorldRef;
                    java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread t = new Thread(r, "ombre-skilldmg"); t.setDaemon(true); return t;
                    }).schedule(() -> dmgWorld.execute(() -> {
                        try {
                            com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(
                                skillTarget, store,
                                new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(skillCaster),
                                    com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, skillDmg));
                        } catch (Exception ignored3) {}
                    }), 100, java.util.concurrent.TimeUnit.MILLISECONDS);
                } else {
                    com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(
                        targeted, store,
                        new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                            new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                            com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, skillDmg));
                }
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, PasDeLOmbreSkill.SKILL_ID);
        notifySkill(uuid, "Pas de l'Ombre");
        return true;
    }

    public boolean tryCastChaseOuverte(@Nonnull UUID uuid,
                                       @Nonnull PlayerRef playerRef,
                                       @Nonnull Ref<EntityStore> entityRef,
                                       @Nonnull Store<EntityStore> store,
                                       @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isOmbre(acc)) return false;
        if (!isHoldingDagger(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, ChaseOuverteSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, ChaseOuverteSkill.SKILL_ID, ChaseOuverteSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = ChaseOuverteSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        long durationMs = ChaseOuverteSkill.durationMsForRank(rank);

        Ref<EntityStore> targeted = findTargetedNpcRef(playerRef, entityRef, store, 7.0);
        if (targeted == null) {
            return false;
        }

        ombreState.startChaseOuverte(uuid, durationMs, rank);

        try {
            TransformComponent casterTc = store.getComponent(entityRef, TransformComponent.getComponentType());
            TransformComponent targetTc = store.getComponent(targeted, TransformComponent.getComponentType());
            if (casterTc != null) {
                ClassSkillSounds.playSkillSound("Bow_T2_Shoot_01", playerRef, casterTc.getPosition(), null);

                if (targetTc != null) {
                    try {
                        com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig kunaiConfig =
                            com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig.getAssetMap()
                                .getAsset("Projectile_Config_Kunai");
                        if (kunaiConfig != null) {
                            org.joml.Vector3d casterPos = casterTc.getPosition();
                            org.joml.Vector3d targetPos2 = targetTc.getPosition();
                            double toDx = targetPos2.x - casterPos.x;
                            double toDy = (targetPos2.y + 0.5) - (casterPos.y + 1.5);
                            double toDz = targetPos2.z - casterPos.z;
                            double len = Math.sqrt(toDx*toDx + toDy*toDy + toDz*toDz);
                            if (len > 1e-6) { toDx /= len; toDy /= len; toDz /= len; }
                            org.joml.Vector3d direction = new org.joml.Vector3d(toDx, toDy, toDz);
                            org.joml.Vector3d spawnPos = new org.joml.Vector3d(
                                casterPos.x - toDx * 1.0 - toDz * 0.5,
                                casterPos.y + 1.8,
                                casterPos.z - toDz * 1.0 + toDx * 0.5);
                            if (commandBuffer != null) {
                                com.hypixel.hytale.server.core.modules.projectile.ProjectileModule.get()
                                    .spawnProjectile(entityRef, commandBuffer, kunaiConfig, spawnPos, direction);
                            } else {
                                com.hypixel.hytale.server.core.universe.world.World kunaiWorld = null;
                                try {
                                    java.util.UUID wUuid = playerRef.getWorldUuid();
                                    if (wUuid != null) kunaiWorld = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(wUuid);
                                } catch (Exception ignored2) {}
                                if (kunaiWorld != null) {
                                    final com.hypixel.hytale.server.core.universe.world.World fw = kunaiWorld;
                                    final org.joml.Vector3d fSpawnPos = spawnPos;
                                    final org.joml.Vector3d fDir = direction;
                                    final Ref<EntityStore> fEntityRef = entityRef;
                                    final com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig fConfig = kunaiConfig;
                                    fw.execute(() -> {
                                        try {
                                            Store<EntityStore> ws = fw.getEntityStore().getStore();
                                            java.lang.reflect.Method takeCmd = ws.getClass().getDeclaredMethod("takeCommandBuffer");
                                            takeCmd.setAccessible(true);
                                            @SuppressWarnings("unchecked")
                                            CommandBuffer<EntityStore> cb = (CommandBuffer<EntityStore>) takeCmd.invoke(ws);
                                            Exception spawnEx = null;
                                            try {
                                                com.hypixel.hytale.server.core.modules.projectile.ProjectileModule.get()
                                                    .spawnProjectile(fEntityRef, cb, fConfig, fSpawnPos, fDir);
                                            } catch (Exception e3) {
                                                spawnEx = e3;
                                            }
                                            try {
                                                java.lang.reflect.Method consume = cb.getClass().getDeclaredMethod("consume");
                                                consume.setAccessible(true);
                                                consume.invoke(cb);
                                            } catch (Exception e4) {
                                            }
                                        } catch (Exception e2) {
                                        }
                                    });
                                }
                            }
                        }
                    } catch (Exception ignoredProj) {
                    }
                }
            }

            float durationSec = durationMs / 1000f;
            int idx = com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap()
                .getIndex("Vrpg_Chasse_Ouverte");
            com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect effect =
                (com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect)
                com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getAsset(idx);
            if (effect != null) {
                com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent ec =
                    store.getComponent(targeted,
                        com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
                if (ec != null) {
                    ec.addEffect(targeted, effect, durationSec,
                        com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior.OVERWRITE, store);
                }
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, ChaseOuverteSkill.SKILL_ID);
        notifySkill(uuid, "Chasse Ouverte");
        return true;
    }

    private boolean isDuelliste(@Nonnull ClassAccount acc) {
        return acc.getActiveClass() == PlayerClass.GUERRIER
            && acc.getActiveSpec(PlayerClass.GUERRIER) == PlayerSpecialization.DUELLISTE;
    }

    private boolean isOmbre(@Nonnull ClassAccount acc) {
        return acc.getActiveClass() == PlayerClass.GUERRIER
            && acc.getActiveSpec(PlayerClass.GUERRIER) == PlayerSpecialization.OMBRE;
    }

    private boolean isHoldingDagger(@Nonnull PlayerRef playerRef) {
        return fr.varyon.vrpg.classes.WeaponCategory.fromItemId(getHeldItemId(playerRef))
            == fr.varyon.vrpg.classes.WeaponCategory.DAGUE;
    }

    private String getHeldItemId(@Nonnull PlayerRef playerRef) {
        try {
            var hotbar = playerRef.getComponent(InventoryComponent.Hotbar.getComponentType());
            if (hotbar == null) return null;
            ItemStack held = hotbar.getInventory().getItemStack((short) hotbar.getActiveSlot());
            if (held == null || held.isEmpty()) return null;
            return held.getItemId();
        } catch (Exception e) { return null; }
    }

    @Nullable
    private Ref<EntityStore> findTargetedNpcRef(
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store) {
        return findTargetedNpcRef(playerRef, entityRef, store, 5.0);
    }

    @Nullable
    private Ref<EntityStore> findTargetedNpcRef(
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            double maxRange) {
        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc == null) return null;
            org.joml.Vector3d pos = tc.getPosition();

            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            org.joml.Vector3d dir = hr != null ? hr.getDirection() : new org.joml.Vector3d(0, 0, 1);

            long casterIdx = entityRef.getIndex();
            double searchRadius = maxRange;
            double[] bestDist = {searchRadius * searchRadius + 1};
            Ref<EntityStore>[] bestRef = new Ref[]{null};

            org.joml.Vector3d searchCenter = new org.joml.Vector3d(
                pos.x + dir.x * 2.5, pos.y + dir.y * 2.5 + 1.0, pos.z + dir.z * 2.5);

            com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector.selectNearbyEntities(
                store, searchCenter, searchRadius, targetRef -> {
                    try {
                        if (targetRef.getIndex() == casterIdx) return;
                        com.hypixel.hytale.server.npc.entities.NPCEntity npc =
                            store.getComponent(targetRef, com.hypixel.hytale.server.npc.entities.NPCEntity.getComponentType());
                        if (npc == null) return;
                        TransformComponent ttc = store.getComponent(targetRef, TransformComponent.getComponentType());
                        if (ttc == null) return;
                        org.joml.Vector3d tp = ttc.getPosition();
                        double dx = tp.x - pos.x, dy = tp.y - pos.y, dz = tp.z - pos.z;
                        double dot = dx * dir.x + dy * dir.y + dz * dir.z;
                        if (dot <= 0) return;
                        double distSq = dx * dx + dy * dy + dz * dz;
                        if (distSq < bestDist[0]) {
                            bestDist[0] = distSq;
                            bestRef[0] = targetRef;
                        }
                    } catch (Exception ignored) {}
                }, t2 -> t2.getIndex() != casterIdx);

            return bestRef[0];
        } catch (Exception e) {
            return null;
        }
    }

    @Nonnull
    private java.util.List<Ref<EntityStore>> findPiercingNpcRefs(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            double maxRange,
            double hitRadius,
            int maxCount) {
        java.util.List<Ref<EntityStore>> results = new java.util.ArrayList<>();
        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc == null) return results;
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            if (hr == null) return results;
            org.joml.Vector3d origin = new org.joml.Vector3d(tc.getPosition().x, tc.getPosition().y + 1.2, tc.getPosition().z);
            org.joml.Vector3d dir = new org.joml.Vector3d(hr.getDirection()).normalize();
            long casterIdx = entityRef.getIndex();
            java.util.List<long[]> hits = new java.util.ArrayList<>();
            double searchR = maxRange + hitRadius;
            org.joml.Vector3d rayCenter = new org.joml.Vector3d(origin).add(new org.joml.Vector3d(dir).mul(maxRange * 0.5));
            com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector.selectNearbyEntities(
                store, rayCenter, searchR, targetRef -> {
                    try {
                        if (targetRef.getIndex() == casterIdx) return;
                        if (store.getComponent(targetRef, com.hypixel.hytale.server.npc.entities.NPCEntity.getComponentType()) == null) return;
                        TransformComponent ttc = store.getComponent(targetRef, TransformComponent.getComponentType());
                        if (ttc == null) return;
                        org.joml.Vector3d tp = new org.joml.Vector3d(ttc.getPosition().x, ttc.getPosition().y + 1.0, ttc.getPosition().z);
                        org.joml.Vector3d toTarget = new org.joml.Vector3d(tp).sub(origin);
                        double along = toTarget.dot(dir);
                        if (along < 0 || along > maxRange) return;
                        org.joml.Vector3d proj = new org.joml.Vector3d(dir).mul(along);
                        double perpDist = new org.joml.Vector3d(toTarget).sub(proj).length();
                        if (perpDist > hitRadius) return;
                        hits.add(new long[]{targetRef.getIndex(), Double.doubleToLongBits(along)});
                        results.add(targetRef);
                    } catch (Exception ignored) {}
                }, t2 -> t2.getIndex() != casterIdx);
            hits.sort((a, b) -> Double.compare(Double.longBitsToDouble(a[1]), Double.longBitsToDouble(b[1])));
            if (results.size() > maxCount) results.subList(maxCount, results.size()).clear();
        } catch (Exception ignored) {}
        return results;
    }

    @Nullable
    private PlayerRef findTargetedAllyRef(
            @Nonnull PlayerRef casterRef,
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            double maxRange) {
        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc == null) return null;
            org.joml.Vector3d pos = tc.getPosition();
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            org.joml.Vector3d dir = hr != null ? hr.getDirection() : new org.joml.Vector3d(0, 0, 1);
            UUID casterUuid = casterRef.getUuid();

            double[] bestDist = {maxRange * maxRange + 1};
            PlayerRef[] bestRef = new PlayerRef[]{null};

            // Chercher parmi les joueurs
            for (PlayerRef ally : com.hypixel.hytale.server.core.universe.Universe.get().getPlayers()) {
                try {
                    if (ally.getUuid().equals(casterUuid)) continue;
                    Ref<EntityStore> allyEntityRef = ally.getReference();
                    if (allyEntityRef == null || !allyEntityRef.isValid()) continue;
                    Store<EntityStore> allyStore = allyEntityRef.getStore();
                    if (allyStore == null) continue;
                    TransformComponent allyTc = allyStore.getComponent(allyEntityRef, TransformComponent.getComponentType());
                    if (allyTc == null) continue;
                    org.joml.Vector3d tp = allyTc.getPosition();
                    double dx = tp.x - pos.x, dy = tp.y + 0.8 - pos.y, dz = tp.z - pos.z;
                    double dot = dx * dir.x + dy * dir.y + dz * dir.z;
                    if (dot <= 0) continue;
                    double distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq <= maxRange * maxRange && distSq < bestDist[0]) {
                        bestDist[0] = distSq;
                        bestRef[0] = ally;
                    }
                } catch (Exception ignored2) {}
            }
            return bestRef[0];
        } catch (Exception e) {
            return null;
        }
    }

    // Cherche l'entité alliée (joueur OU NPC non-hostile) la plus proche dans le regard
    @Nullable
    private Ref<EntityStore> findTargetedAllyEntityRef(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull Store<EntityStore> store,
            double maxRange) {
        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc == null) return null;
            org.joml.Vector3d pos = tc.getPosition();
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            org.joml.Vector3d dir = hr != null ? hr.getDirection() : new org.joml.Vector3d(0, 0, 1);
            long casterIdx = entityRef.getIndex();

            double[] bestDist = {maxRange * maxRange + 1};
            Ref<EntityStore>[] bestRef = new Ref[]{null};

            org.joml.Vector3d searchCenter = new org.joml.Vector3d(
                pos.x + dir.x * 3.0, pos.y + dir.y * 3.0 + 1.0, pos.z + dir.z * 3.0);

            com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector
                .selectNearbyEntities(store, searchCenter, maxRange, targetRef -> {
                    try {
                        if (targetRef.getIndex() == casterIdx) return;
                        TransformComponent ttc = store.getComponent(targetRef, TransformComponent.getComponentType());
                        if (ttc == null) return;
                        org.joml.Vector3d tp = ttc.getPosition();
                        double dx = tp.x - pos.x, dy = tp.y + 0.8 - pos.y, dz = tp.z - pos.z;
                        double dot = dx * dir.x + dy * dir.y + dz * dir.z;
                        if (dot <= 0) return;
                        double distSq = dx * dx + dy * dy + dz * dz;
                        if (distSq < bestDist[0]) {
                            bestDist[0] = distSq;
                            bestRef[0] = targetRef;
                        }
                    } catch (Exception ignored2) {}
                }, t -> t.getIndex() != casterIdx);

            return bestRef[0];
        } catch (Exception e) {
            return null;
        }
    }

    private void notifySkill(@Nonnull UUID uuid, @Nonnull String name) {
        try {
            ClassAccount acc = classManager.getOrLoad(uuid);
            PlayerClass cls = acc.getActiveClass();
            PlayerSpecialization spec = cls != null ? acc.getActiveSpec(cls) : null;
            String icon = spec != null ? spec.getItemId() : "Weapon_Sword_Mithril";
            for (com.hypixel.hytale.server.core.universe.PlayerRef pr :
                    new java.util.ArrayList<>(com.hypixel.hytale.server.core.universe.Universe.get().getPlayers())) {
                if (pr.getUuid().equals(uuid)) {
                    NotificationUtil.sendNotification(pr.getPacketHandler(),
                        com.hypixel.hytale.server.core.Message.raw(name), icon, NotificationStyle.Success);
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    private void notifyNoWeapon(@Nonnull PlayerRef playerRef) {
        try {
            NotificationUtil.sendNotification(playerRef.getPacketHandler(),
                com.hypixel.hytale.server.core.Message.raw("Arme requise"),
                (String) null, NotificationStyle.Danger);
        } catch (Exception ignored) {}
    }

    private boolean isHoldingSword(@Nonnull PlayerRef playerRef) {
        try {
            var hotbar = playerRef.getComponent(
                com.hypixel.hytale.server.core.inventory.InventoryComponent.Hotbar.getComponentType());
            if (hotbar == null) return false;
            byte slot = hotbar.getActiveSlot();
            com.hypixel.hytale.server.core.inventory.ItemStack held = hotbar.getInventory().getItemStack((short) slot);
            if (held == null || held.isEmpty()) return false;
            String id = held.getItemId();
            if (id == null) return false;
            return fr.varyon.vrpg.classes.WeaponCategory.fromItemId(id)
                == fr.varyon.vrpg.classes.WeaponCategory.EPEE;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isRempart(@Nonnull ClassAccount acc) {
        return acc.getActiveClass() == PlayerClass.GUERRIER
            && acc.getActiveSpec(PlayerClass.GUERRIER) == PlayerSpecialization.REMPART;
    }

    private boolean isHoldingOneHanded(@Nonnull PlayerRef playerRef) {
        String id = getHeldItemId(playerRef);
        if (id == null) return false;
        fr.varyon.vrpg.classes.WeaponCategory cat = fr.varyon.vrpg.classes.WeaponCategory.fromItemId(id);
        return cat == fr.varyon.vrpg.classes.WeaponCategory.EPEE
            || cat == fr.varyon.vrpg.classes.WeaponCategory.DAGUE
            || cat == fr.varyon.vrpg.classes.WeaponCategory.HACHE;
    }

    public boolean tryCastChargeLourde(@Nonnull UUID uuid,
                                        @Nonnull PlayerRef playerRef,
                                        @Nonnull Ref<EntityStore> entityRef,
                                        @Nonnull Store<EntityStore> store,
                                        @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRempart(acc)) return false;
        if (!isHoldingOneHanded(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.SKILL_ID, fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            if (tc != null && hr != null) {
                org.joml.Vector3d dir = hr.getDirection();
                double dx = dir.x, dz = dir.z;
                double len = Math.sqrt(dx*dx + dz*dz);
                if (len > 1e-6) { dx /= len; dz /= len; }

                com.hypixel.hytale.server.core.modules.physics.component.Velocity vel =
                    commandBuffer != null
                        ? commandBuffer.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType())
                        : store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType());
                if (vel != null) {
                    vel.getInstructions().clear();
                    vel.addInstruction(
                        new org.joml.Vector3d(dx * 22.0, 0.5, dz * 22.0),
                        null, com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                }

                int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
                float dmg = (weaponDmg > 0 ? weaponDmg : 1f) * fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.damagePctForRank(rank);
                long casterIdx = entityRef.getIndex();
                double dist = fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.dashDistanceForRank(rank);
                org.joml.Vector3d startPos = tc.getPosition();
                java.util.HashSet<Long> hitSet = new java.util.HashSet<>();

                for (double t = 0.5; t <= dist; t += 0.8) {
                    org.joml.Vector3d sample = new org.joml.Vector3d(
                        startPos.x + dx * t,
                        startPos.y + 0.5,
                        startPos.z + dz * t);
                    final double fx = dx, fz = dz;
                    com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector.selectNearbyEntities(
                        store, sample, 1.8, targetRef -> {
                            try {
                                long tidx = targetRef.getIndex();
                                if (tidx == casterIdx || !hitSet.add(tidx)) return;
                                com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(targetRef, store,
                                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                                        new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                                        com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, dmg));
                                com.hypixel.hytale.server.core.modules.physics.component.Velocity targetVel =
                                    store.getComponent(targetRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType());
                                if (targetVel != null) {
                                    targetVel.getInstructions().clear();
                                    targetVel.addInstruction(
                                        new org.joml.Vector3d(fx * 12.0, 6.0, fz * 12.0),
                                        null, com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                                }
                            } catch (Exception ignored) {}
                        }, ref -> ref.getIndex() != casterIdx);
                }

                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Sword", "StabDashCharged", true, commandBuffer != null ? commandBuffer : store);
                ClassSkillSounds.playSkillSound("SFX_Sword_T2_Lunge_Local", playerRef, tc.getPosition(), commandBuffer);
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.SKILL_ID);
        notifySkill(uuid, "Charge Lourde");
        return true;
    }

    public boolean tryCastCoupDeBouclier(@Nonnull UUID uuid,
                                          @Nonnull PlayerRef playerRef,
                                          @Nonnull Ref<EntityStore> entityRef,
                                          @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRempart(acc)) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.SKILL_ID, fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Status, "Sword", "Guard", true, store);
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Sword", "SwingRight", true, store);
            if (tc != null) ClassSkillSounds.playSkillSound("Shield_T1_Impact_01", playerRef, tc.getPosition(), null);

            Ref<EntityStore> targeted = findTargetedNpcRef(playerRef, entityRef, store);
            if (targeted != null) {
                int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
                float dmg = (weaponDmg > 0 ? weaponDmg : 1f) * fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.damagePctForRank(rank);
                com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(targeted, store,
                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                        new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                        com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, dmg));

                float stunSec = fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.stunMsForRank(rank) / 1000f;
                int stunIdx = com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getIndex("Vrpg_Stun");
                com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect stunEffect =
                    (com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect)
                    com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getAsset(stunIdx);
                if (stunEffect != null) {
                    com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent ec =
                        store.getComponent(targeted, com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
                    if (ec != null) ec.addEffect(targeted, stunEffect, stunSec,
                        com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior.OVERWRITE, store);
                }
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.SKILL_ID);
        notifySkill(uuid, "Coup de Bouclier");
        return true;
    }

    public boolean tryCastForteresse(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRempart(acc)) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, fr.varyon.vrpg.classes.rempart.ForteresseSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.rempart.ForteresseSkill.SKILL_ID, fr.varyon.vrpg.classes.rempart.ForteresseSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.rempart.ForteresseSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        rempartState.startForteresse(uuid, fr.varyon.vrpg.classes.rempart.ForteresseSkill.durationMsForRank(rank), fr.varyon.vrpg.classes.rempart.ForteresseSkill.damageReductionForRank(rank));
        try {
            ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", playerRef, new org.joml.Vector3d(), null);
        } catch (Exception ignored) {}
        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.rempart.ForteresseSkill.SKILL_ID);
        notifySkill(uuid, "Forteresse");
        return true;
    }

    public boolean tryCastSecondSouffleRempart(@Nonnull UUID uuid,
                                                @Nonnull PlayerRef playerRef,
                                                @Nonnull Ref<EntityStore> entityRef,
                                                @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRempart(acc)) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.SKILL_ID, fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            Ref<EntityStore> ref = entityRef;
            com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap stats =
                store.getComponent(ref, com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.getComponentType());
            if (stats != null) {
                int hIdx = com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes.getHealth();
                var hp = stats.get(hIdx);
                if (hp != null && hp.getMax() > 0) {
                    float heal = hp.getMax() * fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.healPctForRank(rank);
                    stats.setStatValue(hIdx, Math.min(hp.getMax(), hp.get() + heal));
                }
            }
            ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", playerRef, new org.joml.Vector3d(), null);
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.SKILL_ID);
        notifySkill(uuid, "Second Souffle");
        return true;
    }

    public boolean tryCastGardeRapprochee(@Nonnull UUID uuid,
                                           @Nonnull PlayerRef playerRef,
                                           @Nonnull Ref<EntityStore> entityRef,
                                           @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRempart(acc)) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.SKILL_ID, fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        float reduction = fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.damageReductionForRank(rank);
        long durationMs = fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.durationMsForRank(rank);
        rempartState.startGardeRapprochee(uuid, durationMs, reduction);

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                double radius = fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.allyRadius();
                long casterIdx = entityRef.getIndex();
                com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector.selectNearbyEntities(
                    store, tc.getPosition(), radius, nearRef -> {
                        try {
                            if (nearRef.getIndex() == casterIdx) return;
                            PlayerRef nearPlayer = store.getComponent(nearRef, PlayerRef.getComponentType());
                            if (nearPlayer == null) return;
                            rempartState.startGardeRapprochee(nearPlayer.getUuid(), durationMs, reduction);
                        } catch (Exception ignored) {}
                    }, ref -> ref.getIndex() != casterIdx);
                ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", playerRef, tc.getPosition(), null);
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.SKILL_ID);
        notifySkill(uuid, "Garde Rapprochée");
        return true;
    }

    public boolean tryCastProvocation(@Nonnull UUID uuid,
                                       @Nonnull PlayerRef playerRef,
                                       @Nonnull Ref<EntityStore> entityRef,
                                       @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRempart(acc)) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.GUERRIER, fr.varyon.vrpg.classes.rempart.ProvocationSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.rempart.ProvocationSkill.SKILL_ID, fr.varyon.vrpg.classes.rempart.ProvocationSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.rempart.ProvocationSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        rempartState.startProvocation(uuid, fr.varyon.vrpg.classes.rempart.ProvocationSkill.durationMsForRank(rank));
        fr.varyon.vrpg.classes.ombre.OmbreStealthAggroResetSystem.resetAggroAround(entityRef, store);

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                double radius = fr.varyon.vrpg.classes.rempart.ProvocationSkill.tauntRadius();
                long casterIdx = entityRef.getIndex();
                final Ref<EntityStore> casterRef = entityRef;
                com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector.selectNearbyEntities(
                    store, tc.getPosition(), radius, npcRef -> {
                        try {
                            if (npcRef.getIndex() == casterIdx) return;
                            com.hypixel.hytale.server.npc.entities.NPCEntity npc =
                                store.getComponent(npcRef, com.hypixel.hytale.server.npc.entities.NPCEntity.getComponentType());
                            if (npc == null) return;
                            com.hypixel.hytale.server.npc.role.Role role = npc.getRole();
                            if (role == null) return;
                            fr.varyon.vrpg.classes.ombre.OmbreStealthAggroResetSystem.forceTarget(role, casterRef);
                        } catch (Exception ignored) {}
                    }, ref -> ref.getIndex() != casterIdx);
                ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", playerRef, tc.getPosition(), null);
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.rempart.ProvocationSkill.SKILL_ID);
        notifySkill(uuid, "Provocation");
        return true;
    }

    private interface CooldownResolver {
        long resolve(@Nonnull ClassAccount acc, @Nonnull PlayerClass cls);
    }

    private static final Map<String, CooldownResolver> COOLDOWN_RESOLVERS = new HashMap<>();

    static {
        COOLDOWN_RESOLVERS.put(AssautEclairSkill.SKILL_ID,
            (acc, cls) -> AssautEclairSkill.cooldownMsForRank(acc.getTalentRank(cls, AssautEclairSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(AssautBretteurSkill.SKILL_ID,
            (acc, cls) -> AssautBretteurSkill.cooldownMsForRank(acc.getTalentRank(cls, AssautBretteurSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(DesarmementSkill.SKILL_ID,
            (acc, cls) -> DesarmementSkill.cooldownMsForRank(acc.getTalentRank(cls, DesarmementSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(CoupEstocSkill.SKILL_ID,
            (acc, cls) -> CoupEstocSkill.cooldownMsForRank(acc.getTalentRank(cls, CoupEstocSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(FeintSkill.SKILL_ID,
            (acc, cls) -> FeintSkill.cooldownMsForRank(acc.getTalentRank(cls, FeintSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(RiposteParfaiteSkill.SKILL_ID,
            (acc, cls) -> RiposteParfaiteSkill.cooldownMsForRank(acc.getTalentRank(cls, RiposteParfaiteSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(PasDesTenebresSkill.SKILL_ID,
            (acc, cls) -> PasDesTenebresSkill.cooldownMsForRank(acc.getTalentRank(cls, PasDesTenebresSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(EcranDeFumeeSkill.SKILL_ID,
            (acc, cls) -> EcranDeFumeeSkill.cooldownMsForRank(acc.getTalentRank(cls, EcranDeFumeeSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(FrappeFataleSkill.SKILL_ID,
            (acc, cls) -> FrappeFataleSkill.cooldownMsForRank(acc.getTalentRank(cls, FrappeFataleSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(DelugeDeGamesSkill.SKILL_ID,
            (acc, cls) -> DelugeDeGamesSkill.cooldownMsForRank(acc.getTalentRank(cls, DelugeDeGamesSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(PasDeLOmbreSkill.SKILL_ID,
            (acc, cls) -> PasDeLOmbreSkill.cooldownMsForRank(acc.getTalentRank(cls, PasDeLOmbreSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(ChaseOuverteSkill.SKILL_ID,
            (acc, cls) -> ChaseOuverteSkill.cooldownMsForRank(acc.getTalentRank(cls, ChaseOuverteSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.rempart.ChargeLourdeSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.rempart.CoupDeBouclierSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.rempart.ForteresseSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.rempart.ForteresseSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.rempart.ForteresseSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.rempart.SecondSouffleSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.rempart.GardeRapprocheSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.rempart.ProvocationSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.rempart.ProvocationSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.rempart.ProvocationSkill.TALENT_NODE_ID)));
        // Berserker
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.berserker.AssautBestialSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.berserker.AssautBestialSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.berserker.AssautBestialSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.berserker.DechiquetageSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.berserker.DechiquetageSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.berserker.DechiquetageSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.berserker.CriRalliementSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.berserker.CriRalliementSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.berserker.CriRalliementSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.berserker.ExecutionSauvageSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.berserker.ExecutionSauvageSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.berserker.ExecutionSauvageSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.berserker.DixPourSangSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.berserker.DixPourSangSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.berserker.DixPourSangSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.berserker.CorDeGuerreSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.berserker.CorDeGuerreSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.berserker.CorDeGuerreSkill.TALENT_NODE_ID)));
        // Arcaniste
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.arcaniste.DistorsionSkill.SKILL_ID,
            (acc, cls) -> echoTemporelCd(acc, cls, fr.varyon.vrpg.classes.arcaniste.DistorsionSkill.TALENT_NODE_ID, fr.varyon.vrpg.classes.arcaniste.DistorsionSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.arcaniste.DistorsionSkill.TALENT_NODE_ID))));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.arcaniste.BouleDeFeuSkill.SKILL_ID,
            (acc, cls) -> echoTemporelCd(acc, cls, fr.varyon.vrpg.classes.arcaniste.BouleDeFeuSkill.TALENT_NODE_ID, fr.varyon.vrpg.classes.arcaniste.BouleDeFeuSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.arcaniste.BouleDeFeuSkill.TALENT_NODE_ID))));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.SKILL_ID,
            (acc, cls) -> echoTemporelCd(acc, cls, fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.TALENT_NODE_ID, fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.TALENT_NODE_ID))));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.arcaniste.NovaDeGivreSkill.SKILL_ID,
            (acc, cls) -> echoTemporelCd(acc, cls, fr.varyon.vrpg.classes.arcaniste.NovaDeGivreSkill.TALENT_NODE_ID, fr.varyon.vrpg.classes.arcaniste.NovaDeGivreSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.arcaniste.NovaDeGivreSkill.TALENT_NODE_ID))));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.arcaniste.SurchargeSkill.SKILL_ID,
            (acc, cls) -> echoTemporelCd(acc, cls, fr.varyon.vrpg.classes.arcaniste.SurchargeSkill.TALENT_NODE_ID, fr.varyon.vrpg.classes.arcaniste.SurchargeSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.arcaniste.SurchargeSkill.TALENT_NODE_ID))));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.arcaniste.SalveDeGivreSkill.SKILL_ID,
            (acc, cls) -> echoTemporelCd(acc, cls, fr.varyon.vrpg.classes.arcaniste.SalveDeGivreSkill.TALENT_NODE_ID, fr.varyon.vrpg.classes.arcaniste.SalveDeGivreSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.arcaniste.SalveDeGivreSkill.TALENT_NODE_ID))));
        // Gardien de Gaïa
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.gardiendesgaia.EvasionSylvestreSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.gardiendesgaia.EvasionSylvestreSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.gardiendesgaia.EvasionSylvestreSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.gardiendesgaia.BenedictionDeGaiaSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.gardiendesgaia.BenedictionDeGaiaSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.gardiendesgaia.BenedictionDeGaiaSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.gardiendesgaia.EcorceProtectriceSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.gardiendesgaia.EcorceProtectriceSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.gardiendesgaia.EcorceProtectriceSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.gardiendesgaia.AppelDuTreantSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.gardiendesgaia.AppelDuTreantSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.gardiendesgaia.AppelDuTreantSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.gardiendesgaia.EtreinteDeGaiaSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.gardiendesgaia.EtreinteDeGaiaSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.gardiendesgaia.EtreinteDeGaiaSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.gardiendesgaia.MarqueDeRenaissanceSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.gardiendesgaia.MarqueDeRenaissanceSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.gardiendesgaia.MarqueDeRenaissanceSkill.TALENT_NODE_ID)));
        // Vaudou
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.vaudou.PassageEthereSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.vaudou.PassageEthereSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.vaudou.PassageEthereSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.vaudou.FleauToxiqueSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.vaudou.FleauToxiqueSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.vaudou.FleauToxiqueSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.vaudou.TotemEntraveSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.vaudou.TotemEntraveSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.vaudou.TotemEntraveSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.vaudou.AttaquePerfideSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.vaudou.AttaquePerfideSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.vaudou.AttaquePerfideSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.vaudou.TotemVulnerabiliteSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.vaudou.TotemVulnerabiliteSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.vaudou.TotemVulnerabiliteSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.vaudou.ExtractionAmeSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.vaudou.ExtractionAmeSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.vaudou.ExtractionAmeSkill.TALENT_NODE_ID)));
        // Rôdeur
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.rodeur.ReculStrategiqueSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.rodeur.ReculStrategiqueSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.rodeur.ReculStrategiqueSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.rodeur.PluieDesFlechesSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.rodeur.PluieDesFlechesSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.rodeur.PluieDesFlechesSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.rodeur.MarqueDuChasseurSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.rodeur.MarqueDuChasseurSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.rodeur.MarqueDuChasseurSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.rodeur.FlecheDeReculsSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.rodeur.FlecheDeReculsSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.rodeur.FlecheDeReculsSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.rodeur.FlecheEntravantSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.rodeur.FlecheEntravantSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.rodeur.FlecheEntravantSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.rodeur.RafaleSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.rodeur.RafaleSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.rodeur.RafaleSkill.TALENT_NODE_ID)));
        // Arbalétrier
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.arbaletrier.ReculTactiqueSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.arbaletrier.ReculTactiqueSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.arbaletrier.ReculTactiqueSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.arbaletrier.CarreauLourdSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.arbaletrier.CarreauLourdSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.arbaletrier.CarreauLourdSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.arbaletrier.CarreauExplosifSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.arbaletrier.CarreauExplosifSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.arbaletrier.CarreauExplosifSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.arbaletrier.CarreauTranspercantSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.arbaletrier.CarreauTranspercantSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.arbaletrier.CarreauTranspercantSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.arbaletrier.CoupDeBotteSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.arbaletrier.CoupDeBotteSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.arbaletrier.CoupDeBotteSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.arbaletrier.MiseEnJouSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.arbaletrier.MiseEnJouSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.arbaletrier.MiseEnJouSkill.TALENT_NODE_ID)));

        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.lancier.PerceeSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.lancier.PerceeSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.lancier.PerceeSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.lancier.ChargeHeroiqueSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.lancier.ChargeHeroiqueSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.lancier.ChargeHeroiqueSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.lancier.GardeDuLancierSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.lancier.GardeDuLancierSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.lancier.GardeDuLancierSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.lancier.HarponnageSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.lancier.HarponnageSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.lancier.HarponnageSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.lancier.FormationDePiquesSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.lancier.FormationDePiquesSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.lancier.FormationDePiquesSkill.TALENT_NODE_ID)));
        COOLDOWN_RESOLVERS.put(fr.varyon.vrpg.classes.lancier.EmpalementSkill.SKILL_ID,
            (acc, cls) -> fr.varyon.vrpg.classes.lancier.EmpalementSkill.cooldownMsForRank(acc.getTalentRank(cls, fr.varyon.vrpg.classes.lancier.EmpalementSkill.TALENT_NODE_ID)));
    }

    private static long echoTemporelCd(@Nonnull ClassAccount acc, @Nonnull PlayerClass cls,
                                        @Nonnull String nodeId, long baseMs) {
        int rank = acc.getTalentRank(cls, fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.ECHO_TEMPOREL_NODE);
        if (rank <= 0) return baseMs;
        float reduc = fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.echoTemporelReducForRank(rank);
        return Math.round(baseMs * (1.0 - reduc));
    }

    public long getCooldownTotalMs(@Nonnull String skillId, @Nonnull ClassAccount acc, @Nonnull PlayerClass cls) {
        CooldownResolver resolver = COOLDOWN_RESOLVERS.get(skillId);
        return resolver != null ? resolver.resolve(acc, cls) : 0L;
    }

    public long getCooldownRemainingMs(@Nonnull UUID uuid, @Nonnull String skillId,
                                       @Nonnull ClassAccount acc, @Nonnull PlayerClass cls) {
        long total = getCooldownTotalMs(skillId, acc, cls);
        if (total <= 0L) return 0L;
        return cooldowns.remainingMs(uuid, skillId, total);
    }

    // ========== BERSERKER ==========

    private boolean isBerserker(@Nonnull ClassAccount acc) {
        return acc.getActiveClass() == PlayerClass.BARBARE
            && acc.getActiveSpec(PlayerClass.BARBARE) == fr.varyon.vrpg.classes.PlayerSpecialization.BERSERKER;
    }

    private boolean isHoldingAxe(@Nonnull PlayerRef playerRef) {
        return fr.varyon.vrpg.classes.WeaponCategory.fromItemId(getHeldItemId(playerRef))
            == fr.varyon.vrpg.classes.WeaponCategory.HACHE;
    }

    public boolean tryCastAssautBestial(@Nonnull UUID uuid,
                                         @Nonnull PlayerRef playerRef,
                                         @Nonnull Ref<EntityStore> entityRef,
                                         @Nonnull Store<EntityStore> store,
                                         @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isBerserker(acc)) return false;
        if (!isHoldingAxe(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.BARBARE, fr.varyon.vrpg.classes.berserker.AssautBestialSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.berserker.AssautBestialSkill.SKILL_ID, fr.varyon.vrpg.classes.berserker.AssautBestialSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.berserker.AssautBestialSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            if (tc != null && hr != null) {
                org.joml.Vector3d dir = hr.getDirection();
                double dx = dir.x, dz = dir.z;
                double hlen = Math.sqrt(dx*dx + dz*dz);
                if (hlen > 1e-6) { dx /= hlen; dz /= hlen; }

                com.hypixel.hytale.server.core.modules.physics.component.Velocity vel =
                    commandBuffer != null
                        ? commandBuffer.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType())
                        : store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType());
                if (vel != null) {
                    double hSpeed = fr.varyon.vrpg.classes.berserker.AssautBestialSkill.dashDistanceForRank(rank) * 2.4;
                    org.joml.Vector3d leapVel = new org.joml.Vector3d(dx * hSpeed, 16.0, dz * hSpeed);
                    vel.setClient(leapVel);
                    vel.getInstructions().clear();
                    vel.addInstruction(leapVel, null, com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                }

                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Battleaxe", "DownstrikeLeap", true,
                    commandBuffer != null ? commandBuffer : store);
                ClassSkillSounds.playSkillSound("SFX_Battleaxe_T1_Launch", playerRef, tc.getPosition(), commandBuffer);

                final org.joml.Vector3d landPos = new org.joml.Vector3d(
                    tc.getPosition().x + dx * fr.varyon.vrpg.classes.berserker.AssautBestialSkill.dashDistanceForRank(rank),
                    tc.getPosition().y,
                    tc.getPosition().z + dz * fr.varyon.vrpg.classes.berserker.AssautBestialSkill.dashDistanceForRank(rank));
                final double fdx = dx, fdz = dz;
                final int fRank = rank;
                final PlayerRef fPlayerRef = playerRef;
                final Ref<EntityStore> fEntityRef = entityRef;

                com.hypixel.hytale.server.core.universe.world.World world = null;
                try {
                    java.util.UUID wUuid = playerRef.getWorldUuid();
                    if (wUuid != null) world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(wUuid);
                } catch (Exception ignored2) {}
                if (world != null) {
                    final com.hypixel.hytale.server.core.universe.world.World fw = world;
                    final Store<EntityStore> fStore = store;
                    java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread t = new Thread(r, "assaut-bestial-strike"); t.setDaemon(true); return t;
                    }).schedule(() -> fw.execute(() -> {
                        try {
                            AnimationUtils.playAnimation(fEntityRef, AnimationSlot.Action, "Battleaxe", "DownstrikeCharged", true, fStore);
                            ClassSkillSounds.playSkillSound("SFX_Battleaxe_T2_Swing_Charged", fPlayerRef, landPos, null);

                            float dmg = fr.varyon.vrpg.classes.berserker.AssautBestialSkill.damagePctForRank(fRank)
                                * fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(fPlayerRef);
                            if (dmg < 1f) dmg = 1f;
                            final float finalDmg = dmg;
                            long casterIdx = fEntityRef.getIndex();
                            java.util.HashSet<Long> hitSet = new java.util.HashSet<>();

                            double sweepRadius = 3.5;
                            double sweepAngle = Math.PI * 2.0 / 3.0;
                            double casterYaw = Math.atan2(-fdx, -fdz);
                            int steps = 16;
                            for (int i = 0; i <= steps; i++) {
                                double angle = casterYaw - sweepAngle / 2.0 + sweepAngle * i / steps;
                                org.joml.Vector3d sample = new org.joml.Vector3d(
                                    landPos.x + Math.sin(angle) * sweepRadius * 0.5,
                                    landPos.y + 0.8,
                                    landPos.z - Math.cos(angle) * sweepRadius * 0.5);
                                com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector
                                    .selectNearbyEntities(fStore, sample, 2.2, targetRef -> {
                                        try {
                                            long tidx = targetRef.getIndex();
                                            if (tidx == casterIdx || !hitSet.add(tidx)) return;
                                            com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(
                                                targetRef, fStore,
                                                new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                                                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(fEntityRef),
                                                    com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL,
                                                    finalDmg));
                                        } catch (Exception ignored3) {}
                                    }, t2 -> t2.getIndex() != casterIdx);
                            }
                        } catch (Exception ignored2) {}
                    }), 650, java.util.concurrent.TimeUnit.MILLISECONDS);
                }
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.berserker.AssautBestialSkill.SKILL_ID);
        notifySkill(uuid, "Assaut Bestial");
        return true;
    }

    public boolean tryCastDechiquetage(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isBerserker(acc)) return false;
        if (!isHoldingAxe(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.BARBARE, fr.varyon.vrpg.classes.berserker.DechiquetageSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.berserker.DechiquetageSkill.SKILL_ID, fr.varyon.vrpg.classes.berserker.DechiquetageSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.berserker.DechiquetageSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        berserkerState.armEvisc(uuid, fr.varyon.vrpg.classes.berserker.DechiquetageSkill.armedWindowMs(), rank);
        try {
            ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", playerRef, new org.joml.Vector3d(), null);
        } catch (Exception ignored) {}
        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.berserker.DechiquetageSkill.SKILL_ID);
        notifySkill(uuid, "Déchiquetage");
        return true;
    }

    public boolean tryCastCriRalliement(@Nonnull UUID uuid,
                                         @Nonnull PlayerRef playerRef,
                                         @Nonnull Ref<EntityStore> entityRef,
                                         @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isBerserker(acc)) return false;
        if (!isHoldingAxe(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.BARBARE, fr.varyon.vrpg.classes.berserker.CriRalliementSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.berserker.CriRalliementSkill.SKILL_ID, fr.varyon.vrpg.classes.berserker.CriRalliementSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.berserker.CriRalliementSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        float bonus = fr.varyon.vrpg.classes.berserker.CriRalliementSkill.damageBonusForRank(rank);
        long duration = fr.varyon.vrpg.classes.berserker.CriRalliementSkill.durationMsForRank(rank);
        berserkerState.startCriRalliement(uuid, duration, bonus);

        // Appliquer aux alliés proches
        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                double radius = fr.varyon.vrpg.classes.berserker.CriRalliementSkill.allyRadius();
                long casterIdx = entityRef.getIndex();
                com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector.selectNearbyEntities(
                    store, tc.getPosition(), radius, nearRef -> {
                        try {
                            if (nearRef.getIndex() == casterIdx) return;
                            com.hypixel.hytale.server.core.universe.PlayerRef nearPlayer =
                                store.getComponent(nearRef, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
                            if (nearPlayer == null) return;
                            berserkerState.startCriRalliement(nearPlayer.getUuid(), duration, bonus);
                        } catch (Exception ignored) {}
                    }, ref -> ref.getIndex() != casterIdx);
                ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", playerRef, tc.getPosition(), null);
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.berserker.CriRalliementSkill.SKILL_ID);
        notifySkill(uuid, "Cri de Ralliement");
        return true;
    }

    public boolean tryCastExecutionSauvage(@Nonnull UUID uuid,
                                            @Nonnull PlayerRef playerRef,
                                            @Nonnull Ref<EntityStore> entityRef,
                                            @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isBerserker(acc)) return false;
        if (!isHoldingAxe(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.BARBARE, fr.varyon.vrpg.classes.berserker.ExecutionSauvageSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.berserker.ExecutionSauvageSkill.SKILL_ID, fr.varyon.vrpg.classes.berserker.ExecutionSauvageSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.berserker.ExecutionSauvageSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            Ref<EntityStore> targeted = findTargetedNpcRef(playerRef, entityRef, store, 2.0);
            if (targeted == null) return false;

            int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
            float axeMult = (float) fr.varyon.vrpg.classes.WeaponCategory.HACHE.getMultiplierFor(fr.varyon.vrpg.classes.PlayerSpecialization.BERSERKER);
            float base = (weaponDmg > 0 ? (float) weaponDmg : 1f) * axeMult;
            float mult = fr.varyon.vrpg.classes.berserker.ExecutionSauvageSkill.baseDamagePctForRank(rank);

            com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap sm =
                store.getComponent(targeted, com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.getComponentType());
            if (sm != null) {
                try {
                    int hIdx = com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes.getHealth();
                    var hpStat = sm.get(hIdx);
                    if (hpStat != null && hpStat.getMax() > 0
                            && (hpStat.get() / hpStat.getMax()) < fr.varyon.vrpg.classes.berserker.ExecutionSauvageSkill.lowHpThreshold()) {
                        mult += fr.varyon.vrpg.classes.berserker.ExecutionSauvageSkill.lowHpBonusPctForRank(rank);
                    }
                } catch (Exception ignored2) {}
            }

            com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(targeted, store,
                new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                    com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, base * mult));

            TransformComponent targetTc = store.getComponent(targeted, TransformComponent.getComponentType());
            if (targetTc != null) {
                final org.joml.Vector3d impactPos = new org.joml.Vector3d(targetTc.getPosition());
                final PlayerRef fpr2 = playerRef;
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "berserker-axe-impact"); t.setDaemon(true); return t; })
                    .schedule(() -> ClassSkillSounds.playSkillSound("SFX_Vrpg_AxeImpact", fpr2, impactPos, null),
                        250, java.util.concurrent.TimeUnit.MILLISECONDS);
            }

            AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Sword", "SwingRight", true, store);
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                final org.joml.Vector3d swingPos = new org.joml.Vector3d(tc.getPosition());
                final PlayerRef fpr = playerRef;
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "berserker-axe-sound"); t.setDaemon(true); return t; })
                    .schedule(() -> ClassSkillSounds.playSkillSound("SFX_Vrpg_AxeSwing", fpr, swingPos, null),
                        150, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.berserker.ExecutionSauvageSkill.SKILL_ID);
        notifySkill(uuid, "Exécution Sauvage");
        return true;
    }

    public boolean tryCastDixPourSang(@Nonnull UUID uuid,
                                       @Nonnull PlayerRef playerRef,
                                       @Nonnull Ref<EntityStore> entityRef,
                                       @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isBerserker(acc)) return false;
        if (!isHoldingAxe(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.BARBARE, fr.varyon.vrpg.classes.berserker.DixPourSangSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.berserker.DixPourSangSkill.SKILL_ID, fr.varyon.vrpg.classes.berserker.DixPourSangSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.berserker.DixPourSangSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            // Consommer 10% HP
            Integer hIdx = null;
            try { hIdx = com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes.getHealth(); } catch (Exception e) { hIdx = -1; }
            if (hIdx >= 0) {
                com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap selfStats =
                    store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.getComponentType());
                if (selfStats != null) {
                    var hp = selfStats.get(hIdx);
                    if (hp != null && hp.get() <= hp.getMax() * fr.varyon.vrpg.classes.berserker.DixPourSangSkill.selfHpCostPct() + 1f) return false;
                    if (hp != null) selfStats.addStatValue(hIdx, -hp.getMax() * fr.varyon.vrpg.classes.berserker.DixPourSangSkill.selfHpCostPct());
                }
            }

            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
                float dmg = (weaponDmg > 0 ? (float) weaponDmg : 1f) * fr.varyon.vrpg.classes.berserker.DixPourSangSkill.damagePctForRank(rank);
                org.joml.Vector3d casterPos = tc.getPosition();
                long casterIdx = entityRef.getIndex();
                com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector.selectNearbyEntities(
                    store, casterPos, fr.varyon.vrpg.classes.berserker.DixPourSangSkill.hitRadius(), targetRef -> {
                        try {
                            if (targetRef.getIndex() == casterIdx) return;
                            com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(targetRef, store,
                                new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                                    com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, dmg));
                            TransformComponent targetTc = store.getComponent(targetRef, TransformComponent.getComponentType());
                            com.hypixel.hytale.server.core.modules.physics.component.Velocity targetVel =
                                store.getComponent(targetRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType());
                            if (targetTc != null && targetVel != null) {
                                org.joml.Vector3d diff = new org.joml.Vector3d(
                                    targetTc.getPosition().x - casterPos.x,
                                    0,
                                    targetTc.getPosition().z - casterPos.z);
                                double len = diff.length();
                                if (len > 1e-6) diff.div(len); else diff.set(0, 0, 1);
                                targetVel.getInstructions().clear();
                                targetVel.addInstruction(
                                    new org.joml.Vector3d(diff.x * 8.0, 5.0, diff.z * 8.0),
                                    null, com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                            }
                        } catch (Exception ignored2) {}
                    }, ref -> ref.getIndex() != casterIdx);
                ClassSkillSounds.playSkillSound("SFX_Club_Steel_Impact", playerRef, casterPos, null);
                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Club", "SwingDownCharged", true, store);
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.berserker.DixPourSangSkill.SKILL_ID);
        notifySkill(uuid, "Dix pour Sang");
        return true;
    }

    public boolean tryCastCorDeGuerre(@Nonnull UUID uuid,
                                       @Nonnull PlayerRef playerRef,
                                       @Nonnull Ref<EntityStore> entityRef,
                                       @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isBerserker(acc)) return false;
        int rank = acc.getTalentRank(PlayerClass.BARBARE, fr.varyon.vrpg.classes.berserker.CorDeGuerreSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.berserker.CorDeGuerreSkill.SKILL_ID, fr.varyon.vrpg.classes.berserker.CorDeGuerreSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.berserker.CorDeGuerreSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        float spdBoost = fr.varyon.vrpg.classes.berserker.CorDeGuerreSkill.speedBonusForRank(rank);
        long durationMs = fr.varyon.vrpg.classes.berserker.CorDeGuerreSkill.durationMsForRank(rank);

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                double radius = fr.varyon.vrpg.classes.berserker.CorDeGuerreSkill.allyRadius();
                com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector.selectNearbyEntities(
                    store, tc.getPosition(), radius, nearRef -> {
                        try {
                            com.hypixel.hytale.server.core.universe.PlayerRef nearPlayer =
                                store.getComponent(nearRef, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
                            if (nearPlayer == null) return;
                            berserkerState.startCor(nearPlayer.getUuid(), durationMs, spdBoost);
                        } catch (Exception ignored2) {}
                    }, ref -> store.getComponent(ref, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType()) != null);
                ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", playerRef, tc.getPosition(), null);
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.berserker.CorDeGuerreSkill.SKILL_ID);
        notifySkill(uuid, "Cor de Guerre");
        return true;
    }

    public void cleanup(@Nonnull UUID uuid) {
        cooldowns.cleanup(uuid);
    }

    // ========== RAVAGEUR ==========

    private boolean isRavageur(@Nonnull ClassAccount acc) {
        return acc.getActiveClass() == PlayerClass.BARBARE
            && acc.getActiveSpec(PlayerClass.BARBARE) == fr.varyon.vrpg.classes.PlayerSpecialization.RAVAGEUR;
    }

    private boolean isHoldingTwoHanded(@Nonnull PlayerRef playerRef) {
        fr.varyon.vrpg.classes.WeaponCategory cat =
            fr.varyon.vrpg.classes.WeaponCategory.fromItemId(getHeldItemId(playerRef));
        return cat == fr.varyon.vrpg.classes.WeaponCategory.DEUX_MAINS
            || cat == fr.varyon.vrpg.classes.WeaponCategory.HACHE;
    }

    public boolean tryCastBondEcrasant(@Nonnull UUID uuid,
                                       @Nonnull PlayerRef playerRef,
                                       @Nonnull Ref<EntityStore> entityRef,
                                       @Nonnull Store<EntityStore> store,
                                       @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRavageur(acc)) return false;
        if (!isHoldingTwoHanded(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.BARBARE, fr.varyon.vrpg.classes.ravageur.BondEcrasantSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.ravageur.BondEcrasantSkill.SKILL_ID,
                fr.varyon.vrpg.classes.ravageur.BondEcrasantSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.ravageur.BondEcrasantSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            if (tc != null && hr != null) {
                org.joml.Vector3d dir = hr.getDirection();
                double dx = dir.x, dz = dir.z;
                double hlen = Math.sqrt(dx * dx + dz * dz);
                if (hlen > 1e-6) { dx /= hlen; dz /= hlen; }

                com.hypixel.hytale.server.core.modules.physics.component.Velocity vel =
                    commandBuffer != null
                        ? commandBuffer.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType())
                        : store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType());
                if (vel != null) {
                    double hSpeed = fr.varyon.vrpg.classes.ravageur.BondEcrasantSkill.dashDistanceForRank(rank) * 1.2;
                    org.joml.Vector3d leapVel = new org.joml.Vector3d(dx * hSpeed, 16.0, dz * hSpeed);
                    vel.setClient(leapVel);
                    vel.getInstructions().clear();
                    vel.addInstruction(leapVel, null, com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                }

                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Battleaxe", "DownstrikeLeap", true,
                    commandBuffer != null ? commandBuffer : store);
                ClassSkillSounds.playSkillSound("SFX_Battleaxe_T1_Launch", playerRef, tc.getPosition(), commandBuffer);

                final org.joml.Vector3d landPos = new org.joml.Vector3d(
                    tc.getPosition().x + dx * fr.varyon.vrpg.classes.ravageur.BondEcrasantSkill.dashDistanceForRank(rank),
                    tc.getPosition().y,
                    tc.getPosition().z + dz * fr.varyon.vrpg.classes.ravageur.BondEcrasantSkill.dashDistanceForRank(rank));
                final double fdx = dx, fdz = dz;
                final int fRank = rank;
                final PlayerRef fPlayerRef = playerRef;
                final Ref<EntityStore> fEntityRef = entityRef;
                final Store<EntityStore> fStore = store;

                com.hypixel.hytale.server.core.universe.world.World world = null;
                try {
                    java.util.UUID wUuid = playerRef.getWorldUuid();
                    if (wUuid != null) world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(wUuid);
                } catch (Exception ignored2) {}
                if (world != null) {
                    final com.hypixel.hytale.server.core.universe.world.World fw = world;
                    java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread t = new Thread(r, "bond-ecrasant-strike"); t.setDaemon(true); return t;
                    }).schedule(() -> fw.execute(() -> {
                        try {
                            AnimationUtils.playAnimation(fEntityRef, AnimationSlot.Action, "Battleaxe", "DownstrikeCharged", true, fStore);
                            ClassSkillSounds.playSkillSound("SFX_Battleaxe_T2_Swing_Charged", fPlayerRef, landPos, null);

                            float dmg = fr.varyon.vrpg.classes.ravageur.BondEcrasantSkill.damagePctForRank(fRank)
                                * fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(fPlayerRef);
                            if (dmg < 1f) dmg = 1f;
                            final float finalDmg = dmg;
                            long casterIdx = fEntityRef.getIndex();
                            java.util.HashSet<Long> hitSet = new java.util.HashSet<>();
                            double sweepAngle = Math.PI * 2.0 / 3.0;
                            double casterYaw = Math.atan2(-fdx, -fdz);
                            for (int i = 0; i <= 16; i++) {
                                double angle = casterYaw - sweepAngle / 2.0 + sweepAngle * i / 16.0;
                                org.joml.Vector3d sample = new org.joml.Vector3d(
                                    landPos.x + Math.sin(angle) * 1.75,
                                    landPos.y + 0.8,
                                    landPos.z - Math.cos(angle) * 1.75);
                                com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector
                                    .selectNearbyEntities(fStore, sample, 2.2, targetRef -> {
                                        try {
                                            long tidx = targetRef.getIndex();
                                            if (tidx == casterIdx || !hitSet.add(tidx)) return;
                                            com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(
                                                targetRef, fStore,
                                                new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                                                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(fEntityRef),
                                                    com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, finalDmg));
                                        } catch (Exception ignored3) {}
                                    }, t2 -> t2.getIndex() != casterIdx);
                            }
                        } catch (Exception ignored2) {}
                    }), 650, java.util.concurrent.TimeUnit.MILLISECONDS);
                }
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.ravageur.BondEcrasantSkill.SKILL_ID);
        notifySkill(uuid, "Bond Écrasant");
        return true;
    }

    public boolean tryCastPeauDeFer(@Nonnull UUID uuid,
                                    @Nonnull PlayerRef playerRef,
                                    @Nonnull Ref<EntityStore> entityRef,
                                    @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRavageur(acc)) return false;
        int rank = acc.getTalentRank(PlayerClass.BARBARE, fr.varyon.vrpg.classes.ravageur.PeauDeFerSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.ravageur.PeauDeFerSkill.SKILL_ID,
                fr.varyon.vrpg.classes.ravageur.PeauDeFerSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.ravageur.PeauDeFerSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        ravageurState.startPeauDeFer(uuid,
            fr.varyon.vrpg.classes.ravageur.PeauDeFerSkill.durationMsForRank(rank),
            fr.varyon.vrpg.classes.ravageur.PeauDeFerSkill.damageReductionForRank(rank));

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) ClassSkillSounds.playSkillSound("SFX_Vrpg_ShieldImpact", playerRef, tc.getPosition(), null);
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Club", "Guard", true, store);
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.ravageur.PeauDeFerSkill.SKILL_ID);
        notifySkill(uuid, "Peau de Fer");
        return true;
    }

    public boolean tryCastDechainement(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRavageur(acc)) return false;
        int rank = acc.getTalentRank(PlayerClass.BARBARE, fr.varyon.vrpg.classes.ravageur.DechainementSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.ravageur.DechainementSkill.SKILL_ID,
                fr.varyon.vrpg.classes.ravageur.DechainementSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.ravageur.DechainementSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        ravageurState.startDechainement(uuid,
            fr.varyon.vrpg.classes.ravageur.DechainementSkill.durationMsForRank(rank),
            fr.varyon.vrpg.classes.ravageur.DechainementSkill.damageBonusForRank(rank));

        try {
            com.hypixel.hytale.server.core.modules.entity.component.TransformComponent tc2 =
                playerRef.getComponent(com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
            if (tc2 != null) ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", playerRef, tc2.getPosition(), null);
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.ravageur.DechainementSkill.SKILL_ID);
        notifySkill(uuid, "Déchaînement");
        return true;
    }

    public boolean tryCastPremierAssaut(@Nonnull UUID uuid,
                                        @Nonnull PlayerRef playerRef,
                                        @Nonnull Ref<EntityStore> entityRef,
                                        @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRavageur(acc)) return false;
        if (!isHoldingTwoHanded(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.BARBARE, fr.varyon.vrpg.classes.ravageur.PremierAssautSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.ravageur.PremierAssautSkill.SKILL_ID,
                fr.varyon.vrpg.classes.ravageur.PremierAssautSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.ravageur.PremierAssautSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        Ref<EntityStore> targeted = findTargetedNpcRef(playerRef, entityRef, store, 4.0);
        if (targeted == null) return false;

        try {
            int hIdx;
            try { hIdx = com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes.getHealth(); }
            catch (Exception e) { hIdx = -1; }

            boolean targetHealthy = false;
            if (hIdx >= 0) {
                com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap targetStats =
                    store.getComponent(targeted, com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.getComponentType());
                if (targetStats != null) {
                    var hp = targetStats.get(hIdx);
                    if (hp != null && hp.getMax() > 0)
                        targetHealthy = (hp.get() / hp.getMax()) >= fr.varyon.vrpg.classes.ravageur.PremierAssautSkill.HEALTHY_THRESHOLD;
                }
            }
            if (!targetHealthy) return false;

            int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
            float dmg = (weaponDmg > 0 ? weaponDmg : 1f) * fr.varyon.vrpg.classes.ravageur.PremierAssautSkill.damagePctForRank(rank);
            com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(targeted, store,
                new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                    com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, dmg));

            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) ClassSkillSounds.playSkillSound("SFX_Battleaxe_T2_Swing_Charged", playerRef, tc.getPosition(), null);
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Longsword", "StabCharged", true, store);
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.ravageur.PremierAssautSkill.SKILL_ID);
        notifySkill(uuid, "Premier Assaut");
        return true;
    }

    public boolean tryCastMarteauPilon(@Nonnull UUID uuid,
                                       @Nonnull PlayerRef playerRef,
                                       @Nonnull Ref<EntityStore> entityRef,
                                       @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRavageur(acc)) return false;
        if (!isHoldingTwoHanded(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.BARBARE, fr.varyon.vrpg.classes.ravageur.MarteauPilonSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.ravageur.MarteauPilonSkill.SKILL_ID,
                fr.varyon.vrpg.classes.ravageur.MarteauPilonSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.ravageur.MarteauPilonSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        Ref<EntityStore> targeted = findTargetedNpcRef(playerRef, entityRef, store, 4.0);
        if (targeted == null) return false;

        try {
            int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
            float dmg1 = (weaponDmg > 0 ? weaponDmg : 1f) * fr.varyon.vrpg.classes.ravageur.MarteauPilonSkill.damagePct1ForRank(rank);
            com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(targeted, store,
                new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                    com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, dmg1));

            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Club", "SwingDown", true, store);

            final int fRank = rank;
            final Ref<EntityStore> fTargeted = targeted;
            final Ref<EntityStore> fEntityRef = entityRef;
            final PlayerRef fPlayerRef = playerRef;
            final org.joml.Vector3d fPos1 = tc != null ? new org.joml.Vector3d(tc.getPosition()) : new org.joml.Vector3d();

            com.hypixel.hytale.server.core.universe.world.World world = null;
            try {
                java.util.UUID wUuid = playerRef.getWorldUuid();
                if (wUuid != null) world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(wUuid);
            } catch (Exception ignored2) {}
            if (world != null) {
                final com.hypixel.hytale.server.core.universe.world.World fw = world;
                final Store<EntityStore> fStore = store;
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "marteau-pilon-snd1"); t.setDaemon(true); return t;
                }).schedule(() -> ClassSkillSounds.playSkillSound("SFX_Club_Steel_Impact", fPlayerRef, fPos1, null),
                    100, java.util.concurrent.TimeUnit.MILLISECONDS);
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "marteau-pilon-2"); t.setDaemon(true); return t;
                }).schedule(() -> fw.execute(() -> {
                    try {
                        float dmg2 = (fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(fPlayerRef) > 0
                            ? fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(fPlayerRef) : 1f)
                            * fr.varyon.vrpg.classes.ravageur.MarteauPilonSkill.damagePct2ForRank(fRank);
                        com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(
                            fTargeted, fStore,
                            new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                                new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(fEntityRef),
                                com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, dmg2));

                        TransformComponent targetTc = fStore.getComponent(fTargeted, TransformComponent.getComponentType());
                        if (targetTc != null) ClassSkillSounds.playSkillSound("SFX_Club_Steel_Impact", fPlayerRef, targetTc.getPosition(), null);
                        AnimationUtils.playAnimation(fEntityRef, AnimationSlot.Action, "Club", "SwingDownCharged", true, fStore);

                        float stunSec = fr.varyon.vrpg.classes.ravageur.MarteauPilonSkill.stunMsForRank(fRank) / 1000f;
                        int stunIdx = com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getIndex("Vrpg_Stun");
                        com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect stunEffect =
                            (com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect)
                            com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getAsset(stunIdx);
                        if (stunEffect != null) {
                            com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent ec =
                                fStore.getComponent(fTargeted, com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
                            if (ec != null) ec.addEffect(fTargeted, stunEffect, stunSec,
                                com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior.OVERWRITE, fStore);
                        }
                    } catch (Exception ignored2) {}
                }), fr.varyon.vrpg.classes.ravageur.MarteauPilonSkill.SECOND_HIT_DELAY_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.ravageur.MarteauPilonSkill.SKILL_ID);
        notifySkill(uuid, "Marteau-Pilon");
        return true;
    }

    public boolean tryCastRabattage(@Nonnull UUID uuid,
                                    @Nonnull PlayerRef playerRef,
                                    @Nonnull Ref<EntityStore> entityRef,
                                    @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRavageur(acc)) return false;
        if (!isHoldingTwoHanded(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.BARBARE, fr.varyon.vrpg.classes.ravageur.RabattageSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.ravageur.RabattageSkill.SKILL_ID,
                fr.varyon.vrpg.classes.ravageur.RabattageSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.ravageur.RabattageSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                org.joml.Vector3d casterPos = tc.getPosition();

                float stunSec = fr.varyon.vrpg.classes.ravageur.RabattageSkill.stunMsForRank(rank) / 1000f;
                long casterIdx = entityRef.getIndex();
                java.util.HashSet<Long> hitSet = new java.util.HashSet<>();

                double radius = fr.varyon.vrpg.classes.ravageur.RabattageSkill.sweepRadius();
                int stunIdx = com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getIndex("Vrpg_Stun");
                com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect stunEffect =
                    (com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect)
                    com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getAsset(stunIdx);

                for (int i = 0; i < 16; i++) {
                    double angle = 2.0 * Math.PI * i / 16.0;
                    org.joml.Vector3d sample = new org.joml.Vector3d(
                        casterPos.x + Math.sin(angle) * radius * 0.5,
                        casterPos.y + 0.8,
                        casterPos.z - Math.cos(angle) * radius * 0.5);
                    final float fStunSec = stunSec;
                    final com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect fStunEffect = stunEffect;
                    com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector
                        .selectNearbyEntities(store, sample, 2.0, targetRef -> {
                            try {
                                long tidx = targetRef.getIndex();
                                if (tidx == casterIdx || !hitSet.add(tidx)) return;


                                com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(
                                    targetRef, store,
                                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                                        new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                                        com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, 0.01f));

                                TransformComponent targetTc = store.getComponent(targetRef, TransformComponent.getComponentType());
                                if (targetTc != null) {
                                    final org.joml.Vector3d startPos = new org.joml.Vector3d(targetTc.getPosition());
                                    final double offsetX = (Math.random() - 0.5) * 2.0;
                                    final double offsetZ = (Math.random() - 0.5) * 2.0;
                                    final org.joml.Vector3d endPos = new org.joml.Vector3d(
                                        casterPos.x + offsetX, casterPos.y, casterPos.z + offsetZ);
                                    final Ref<EntityStore> fRef = targetRef;
                                    final Store<EntityStore> fStore = store;
                                    com.hypixel.hytale.server.core.universe.world.World pullWorld = null;
                                    try {
                                        java.util.UUID wUuid2 = playerRef.getWorldUuid();
                                        if (wUuid2 != null) pullWorld = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(wUuid2);
                                    } catch (Exception ignored4) {}
                                    if (pullWorld != null) {
                                        final com.hypixel.hytale.server.core.universe.world.World fw = pullWorld;
                                        final int STEPS = 6;
                                        final long STEP_MS = 40L;
                                        java.util.concurrent.ScheduledExecutorService exec =
                                            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                                                Thread t = new Thread(r, "rabattage-move"); t.setDaemon(true); return t;
                                            });
                                        java.util.concurrent.atomic.AtomicInteger step = new java.util.concurrent.atomic.AtomicInteger(0);
                                        exec.scheduleAtFixedRate(() -> fw.execute(() -> {
                                            int s = step.incrementAndGet();
                                            if (s > STEPS) { exec.shutdown(); return; }
                                            double t = (double) s / STEPS;
                                            try {
                                                TransformComponent tc2 = fStore.getComponent(fRef, TransformComponent.getComponentType());
                                                if (tc2 != null) {
                                                    tc2.getPosition().set(
                                                        startPos.x + (endPos.x - startPos.x) * t,
                                                        startPos.y + (endPos.y - startPos.y) * t,
                                                        startPos.z + (endPos.z - startPos.z) * t);
                                                }
                                            } catch (Exception ignored5) { exec.shutdown(); }
                                        }), 0, STEP_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
                                    }
                                }

                                if (fStunEffect != null) {
                                    final Ref<EntityStore> fTargetRef = targetRef;
                                    final Store<EntityStore> fStore = store;
                                    com.hypixel.hytale.server.core.universe.world.World stunWorld = null;
                                    try {
                                        java.util.UUID wUuid = playerRef.getWorldUuid();
                                        if (wUuid != null) stunWorld = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(wUuid);
                                    } catch (Exception ignored3) {}
                                    if (stunWorld != null) {
                                        final com.hypixel.hytale.server.core.universe.world.World fw = stunWorld;
                                        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                                            Thread t = new Thread(r, "rabattage-stun"); t.setDaemon(true); return t;
                                        }).schedule(() -> fw.execute(() -> {
                                            try {
                                                com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent ec =
                                                    fStore.getComponent(fTargetRef, com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
                                                if (ec != null) {
                                                    ec.addEffect(fTargetRef, fStunEffect, fStunSec,
                                                        com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior.OVERWRITE, fStore);
                                                }
                                            } catch (Exception ignored4) {}
                                        }), 280, java.util.concurrent.TimeUnit.MILLISECONDS);
                                    }
                                }
                            } catch (Exception e) {
                                LOG.atWarning().log("[Rabattage] exception: %s", e.getMessage());
                            }
                        }, t2 -> t2.getIndex() != casterIdx);
                }

                ClassSkillSounds.playSkillSound("SFX_Vrpg_Rabattage", playerRef, casterPos, null);
                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Battleaxe", "Sweep", true, store);
            }
        } catch (Exception e) {
            LOG.atWarning().log("[Rabattage] outer exception: %s", e.getMessage());
        }

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.ravageur.RabattageSkill.SKILL_ID);
        notifySkill(uuid, "Rabattage");
        return true;
    }

    // ========== BAGARREUR ==========

    private boolean isBagarreur(@Nonnull ClassAccount acc) {
        return acc.getActiveClass() == PlayerClass.BARBARE
            && acc.getActiveSpec(PlayerClass.BARBARE) == fr.varyon.vrpg.classes.PlayerSpecialization.BAGARREUR;
    }

    private boolean isHoldingNothing(@Nonnull PlayerRef playerRef) {
        String held = getHeldItemId(playerRef);
        return held == null || held.isBlank();
    }

    private float getBaseDamage(@Nonnull PlayerRef playerRef) {
        int w = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
        return w > 0 ? (float) w : 1f;
    }

    public boolean tryCastJeuDeJambes(@Nonnull UUID uuid,
                                       @Nonnull PlayerRef playerRef,
                                       @Nonnull Ref<EntityStore> entityRef,
                                       @Nonnull Store<EntityStore> store,
                                       @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isBagarreur(acc)) return false;
        if (!isHoldingNothing(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.BARBARE, fr.varyon.vrpg.classes.bagarreur.JeuDeJambesSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.bagarreur.JeuDeJambesSkill.SKILL_ID,
                fr.varyon.vrpg.classes.bagarreur.JeuDeJambesSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.bagarreur.JeuDeJambesSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            if (tc != null && hr != null) {
                org.joml.Vector3d dir = hr.getDirection();
                double dx = dir.x, dz = dir.z;
                double len = Math.sqrt(dx*dx + dz*dz);
                if (len > 1e-6) { dx /= len; dz /= len; }
                double rightX = dz, rightZ = -dx;

                double dist = fr.varyon.vrpg.classes.bagarreur.JeuDeJambesSkill.dashDistanceForRank(rank);
                double dashSpeed = dist * 3.0;

                com.hypixel.hytale.server.core.modules.physics.component.Velocity vel =
                    commandBuffer != null
                        ? commandBuffer.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType())
                        : store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType());
                if (vel != null) {
                    org.joml.Vector3d dashVel = new org.joml.Vector3d(rightX * dashSpeed, 2.0, rightZ * dashSpeed);
                    vel.setClient(dashVel);
                    vel.getInstructions().clear();
                    vel.addInstruction(dashVel, null, com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                }
                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Daggers", "DashBackward", true,
                    commandBuffer != null ? commandBuffer : store);
                ClassSkillSounds.playSkillSound("SFX_Daggers_T1_Pounce", playerRef, tc.getPosition(), commandBuffer);
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.bagarreur.JeuDeJambesSkill.SKILL_ID);
        notifySkill(uuid, "Jeu de Jambes");
        return true;
    }

    public boolean tryCastMonteeAdrenaline(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isBagarreur(acc)) return false;
        if (!isHoldingNothing(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.BARBARE, fr.varyon.vrpg.classes.bagarreur.MonteeAdreinalineSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.bagarreur.MonteeAdreinalineSkill.SKILL_ID,
                fr.varyon.vrpg.classes.bagarreur.MonteeAdreinalineSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.bagarreur.MonteeAdreinalineSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        bagarreurState.startMonteeAdrenaline(uuid,
            fr.varyon.vrpg.classes.bagarreur.MonteeAdreinalineSkill.durationMsForRank(rank),
            fr.varyon.vrpg.classes.bagarreur.MonteeAdreinalineSkill.damageBonusForRank(rank));

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.bagarreur.MonteeAdreinalineSkill.SKILL_ID);
        notifySkill(uuid, "Montée d'Adrénaline");
        return true;
    }

    public boolean tryCastDirectDuDroit(@Nonnull UUID uuid,
                                         @Nonnull PlayerRef playerRef,
                                         @Nonnull Ref<EntityStore> entityRef,
                                         @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isBagarreur(acc)) return false;
        if (!isHoldingNothing(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.BARBARE, fr.varyon.vrpg.classes.bagarreur.DirectDuDroitSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.bagarreur.DirectDuDroitSkill.SKILL_ID,
                fr.varyon.vrpg.classes.bagarreur.DirectDuDroitSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.bagarreur.DirectDuDroitSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        Ref<EntityStore> targeted = findTargetedNpcRef(playerRef, entityRef, store, 3.5);
        if (targeted == null) return false;

        try {
            float dmg = getBaseDamage(playerRef) * fr.varyon.vrpg.classes.bagarreur.DirectDuDroitSkill.damagePctForRank(rank);
            com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(targeted, store,
                new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                    com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, dmg));

            float stunSec = fr.varyon.vrpg.classes.bagarreur.DirectDuDroitSkill.stunMsForRank(rank) / 1000f;
            int stunIdx = com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getIndex("Vrpg_Stun");
            com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect stunEffect =
                (com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect)
                com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getAsset(stunIdx);
            if (stunEffect != null) {
                com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent ec =
                    store.getComponent(targeted, com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
                if (ec != null) ec.addEffect(targeted, stunEffect, stunSec,
                    com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior.OVERWRITE, store);
            }

            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) ClassSkillSounds.playSkillSound("SFX_Vrpg_Punch", playerRef, tc.getPosition(), null);
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Club", "SwingRight", true, store);
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.bagarreur.DirectDuDroitSkill.SKILL_ID);
        notifySkill(uuid, "Direct du Droit");
        return true;
    }

    public boolean tryCastDelugeDeCoups2(@Nonnull UUID uuid,
                                          @Nonnull PlayerRef playerRef,
                                          @Nonnull Ref<EntityStore> entityRef,
                                          @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isBagarreur(acc)) return false;
        if (!isHoldingNothing(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.BARBARE, fr.varyon.vrpg.classes.bagarreur.DelugeDeCoups2Skill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.bagarreur.DelugeDeCoups2Skill.SKILL_ID,
                fr.varyon.vrpg.classes.bagarreur.DelugeDeCoups2Skill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.bagarreur.DelugeDeCoups2Skill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        Ref<EntityStore> targeted = findTargetedNpcRef(playerRef, entityRef, store, 3.5);
        if (targeted == null) return false;

        try {
            float dmgPerHit = getBaseDamage(playerRef) * fr.varyon.vrpg.classes.bagarreur.DelugeDeCoups2Skill.damagePerHitForRank(rank);
            final int fRank = rank;
            final Ref<EntityStore> fTargeted = targeted;
            final Ref<EntityStore> fEntityRef = entityRef;
            final PlayerRef fPlayerRef = playerRef;

            com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(targeted, store,
                new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                    com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, dmgPerHit));
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Club", "SwingLeft", true, store);
            TransformComponent tcDeluge = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tcDeluge != null) ClassSkillSounds.playSkillSound("SFX_Vrpg_Punch", playerRef, tcDeluge.getPosition(), null);

            int totalHits = fr.varyon.vrpg.classes.bagarreur.DelugeDeCoups2Skill.hitCountForRank(rank);
            com.hypixel.hytale.server.core.universe.world.World world = null;
            try {
                java.util.UUID wUuid = playerRef.getWorldUuid();
                if (wUuid != null) world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(wUuid);
            } catch (Exception ignored2) {}
            if (world != null) {
                final com.hypixel.hytale.server.core.universe.world.World fw = world;
                final Store<EntityStore> fStore = store;
                final float fDmg = dmgPerHit;
                final int fTotalHits = totalHits;
                java.util.concurrent.ScheduledExecutorService exec =
                    java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread t = new Thread(r, "deluge-bagarreur"); t.setDaemon(true); return t;
                    });
                java.util.concurrent.atomic.AtomicInteger hitNum = new java.util.concurrent.atomic.AtomicInteger(1);
                String[] anims = {"SwingRight", "SwingLeft", "SwingRight", "SwingLeft", "SwingRight", "SwingLeft"};
                exec.scheduleAtFixedRate(() -> fw.execute(() -> {
                    int h = hitNum.incrementAndGet();
                    if (h > fTotalHits) { exec.shutdown(); return; }
                    try {
                        com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(
                            fTargeted, fStore,
                            new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                                new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(fEntityRef),
                                com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, fDmg));
                        AnimationUtils.playAnimation(fEntityRef, AnimationSlot.Action, "Club",
                            anims[(h - 2) % anims.length], true, fStore);
                        TransformComponent tc2 = fStore.getComponent(fEntityRef, TransformComponent.getComponentType());
                        if (tc2 != null) ClassSkillSounds.playSkillSound("SFX_Vrpg_Punch", fPlayerRef, tc2.getPosition(), null);
                    } catch (Exception ignored3) { exec.shutdown(); }
                }), fr.varyon.vrpg.classes.bagarreur.DelugeDeCoups2Skill.HIT_DELAY_MS,
                   fr.varyon.vrpg.classes.bagarreur.DelugeDeCoups2Skill.HIT_DELAY_MS,
                   java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.bagarreur.DelugeDeCoups2Skill.SKILL_ID);
        notifySkill(uuid, "Déluge de Coups");
        return true;
    }

    public boolean tryCastSecondSouffleBagarreur(@Nonnull UUID uuid,
                                                   @Nonnull PlayerRef playerRef,
                                                   @Nonnull Ref<EntityStore> entityRef,
                                                   @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isBagarreur(acc)) return false;
        int rank = acc.getTalentRank(PlayerClass.BARBARE, fr.varyon.vrpg.classes.bagarreur.SecondSouffleSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.bagarreur.SecondSouffleSkill.SKILL_ID,
                fr.varyon.vrpg.classes.bagarreur.SecondSouffleSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.bagarreur.SecondSouffleSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            Integer hIdx = null;
            try { hIdx = com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes.getHealth(); }
            catch (Exception e) { hIdx = -1; }
            Integer sIdx = null;
            try { sIdx = com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes.getStamina(); }
            catch (Exception e) { sIdx = -1; }

            com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap stats =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.getComponentType());
            if (stats != null) {
                if (hIdx != null && hIdx >= 0) {
                    var hp = stats.get(hIdx);
                    if (hp != null) {
                        float heal = hp.getMax() * fr.varyon.vrpg.classes.bagarreur.SecondSouffleSkill.hpRestorePctForRank(rank);
                        stats.setStatValue(hIdx, Math.min(hp.getMax(), hp.get() + heal));
                    }
                }
                if (sIdx != null && sIdx >= 0) {
                    var sta = stats.get(sIdx);
                    if (sta != null) {
                        float staminaRestore = sta.getMax() * fr.varyon.vrpg.classes.bagarreur.SecondSouffleSkill.staminaRestorePctForRank(rank);
                        stats.setStatValue(sIdx, Math.min(sta.getMax(), sta.get() + staminaRestore));
                    }
                }
            }
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", playerRef, tc.getPosition(), null);
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Status, "Club", "Guard", true, store);
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.bagarreur.SecondSouffleSkill.SKILL_ID);
        notifySkill(uuid, "Second Souffle");
        return true;
    }

    public boolean tryCastUppercut(@Nonnull UUID uuid,
                                    @Nonnull PlayerRef playerRef,
                                    @Nonnull Ref<EntityStore> entityRef,
                                    @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isBagarreur(acc)) return false;
        if (!isHoldingNothing(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.BARBARE, fr.varyon.vrpg.classes.bagarreur.UppercutSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.bagarreur.UppercutSkill.SKILL_ID,
                fr.varyon.vrpg.classes.bagarreur.UppercutSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.bagarreur.UppercutSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        Ref<EntityStore> targeted = findTargetedNpcRef(playerRef, entityRef, store, 3.0);
        if (targeted == null) return false;

        try {
            float dmg = getBaseDamage(playerRef) * fr.varyon.vrpg.classes.bagarreur.UppercutSkill.damagePctForRank(rank);

            TransformComponent tcCasterUp = store.getComponent(entityRef, TransformComponent.getComponentType());
            TransformComponent tcTargetUp = store.getComponent(targeted, TransformComponent.getComponentType());

            com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(targeted, store,
                new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                    com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, dmg));

            double kbH = fr.varyon.vrpg.classes.bagarreur.UppercutSkill.knockbackH();
            double vx = 0, vz = 0;
            if (tcCasterUp != null && tcTargetUp != null) {
                org.joml.Vector3d diff = new org.joml.Vector3d(tcTargetUp.getPosition()).sub(tcCasterUp.getPosition());
                diff.y = 0;
                double len = diff.length();
                if (len > 1e-6) { vx = diff.x / len * kbH; vz = diff.z / len * kbH; }
            }
            com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent kbComp =
                store.ensureAndGetComponent(targeted, com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent.getComponentType());
            kbComp.setVelocity(new org.joml.Vector3d(vx, fr.varyon.vrpg.classes.bagarreur.UppercutSkill.launchY(), vz));
            kbComp.setVelocityType(com.hypixel.hytale.protocol.ChangeVelocityType.Set);
            kbComp.setDuration(0.0f);

            if (tcCasterUp != null) ClassSkillSounds.playSkillSound("SFX_Vrpg_Punch", playerRef, tcCasterUp.getPosition(), null);
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Club", "SwingRight", true, store);
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Club", "Swing_Up_Left", true, store);
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.bagarreur.UppercutSkill.SKILL_ID);
        notifySkill(uuid, "Uppercut");
        return true;
    }

    public boolean tryCastDistorsion(@Nonnull UUID uuid,
                                      @Nonnull PlayerRef playerRef,
                                      @Nonnull Ref<EntityStore> entityRef,
                                      @Nonnull Store<EntityStore> store,
                                      @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isArcaniste(acc)) return false;
        int rank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.arcaniste.DistorsionSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        long cd = applyEchoTemporel(acc, fr.varyon.vrpg.classes.arcaniste.DistorsionSkill.cooldownMsForRank(rank));
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.arcaniste.DistorsionSkill.SKILL_ID, cd)) return false;
        float manaCost = fr.varyon.vrpg.classes.arcaniste.DistorsionSkill.manaCostForRank(rank);
        if (!fr.varyon.vrpg.classes.ability.ClassSkillMana.hasEnough(playerRef, manaCost)) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef,
                    com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            if (tc != null && hr != null) {
                org.joml.Vector3d lookDir = hr.getDirection();
                double fwdX = lookDir.x, fwdZ = lookDir.z;
                double fwdLen = Math.sqrt(fwdX * fwdX + fwdZ * fwdZ);
                if (fwdLen > 1e-6) { fwdX /= fwdLen; fwdZ /= fwdLen; }

                double dashX = -fwdX, dashZ = -fwdZ;

                double baseForce = 12.0 + fr.varyon.vrpg.classes.arcaniste.DistorsionSkill.dashDistanceForRank(rank) * 0.875;
                com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig dashConfig =
                    new com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig();
                dashConfig.setAirResistance(0.97f);
                dashConfig.setAirResistanceMax(0.96f);
                dashConfig.setGroundResistance(0.94f);
                dashConfig.setGroundResistanceMax(0.82f);
                dashConfig.setThreshold(5.0f);
                dashConfig.setStyle(com.hypixel.hytale.protocol.VelocityThresholdStyle.Exp);

                com.hypixel.hytale.server.core.modules.physics.component.Velocity vel =
                    commandBuffer != null
                        ? commandBuffer.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType())
                        : store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType());
                if (vel != null) {
                    org.joml.Vector3d dashVel = new org.joml.Vector3d(dashX * baseForce, 0.2, dashZ * baseForce);
                    vel.setClient(dashVel);
                    vel.getInstructions().clear();
                    vel.addInstruction(dashVel, dashConfig, com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                }

                try {
                    String animName = "DashBackward";
                    com.hypixel.hytale.server.core.asset.type.itemanimation.config.ItemPlayerAnimations dashAnims =
                        com.hypixel.hytale.server.core.asset.type.itemanimation.config.ItemPlayerAnimations
                            .getAssetMap().getAsset("Vrpg_Arcaniste_Dash");
                    if (dashAnims != null) {
                        AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, dashAnims, animName,
                            commandBuffer != null ? commandBuffer : store);
                    }
                } catch (Exception ignored2) {}

                try {
                    int effIdx = com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect
                        .getAssetMap().getIndex("Vrpg_Distorsion_Dash");
                    com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect dashEff =
                        (com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect)
                        com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect
                            .getAssetMap().getAsset(effIdx);
                    if (dashEff != null) {
                        com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent ec =
                            store.getComponent(entityRef,
                                com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
                        if (ec == null && commandBuffer != null)
                            ec = commandBuffer.getComponent(entityRef,
                                com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
                        if (ec != null) ec.addEffect(entityRef, dashEff, 0.3f,
                            com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior.OVERWRITE, store);
                    }
                } catch (Exception ignored3) {}

                ClassSkillSounds.playSkillSound("SFX_Vrpg_OmbreVanish", playerRef, tc.getPosition(), commandBuffer);

                try {
                    int rootIdx = com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction
                        .getAssetMap().getIndex("Root_Distorsion_Trail");
                    if (rootIdx >= 0) {
                        String heldItemId = getHeldItemId(playerRef);
                        com.hypixel.hytale.protocol.packets.interaction.PlayInteractionFor trailPacket =
                            new com.hypixel.hytale.protocol.packets.interaction.PlayInteractionFor(
                                (int) entityRef.getIndex(), 0, null, 0, rootIdx,
                                heldItemId, com.hypixel.hytale.protocol.InteractionType.Primary, false);
                        playerRef.getPacketHandler().write(trailPacket);
                    }
                } catch (Exception ignored4) {}
            }
        } catch (Exception ignored) {}

        fr.varyon.vrpg.classes.ability.ClassSkillMana.consume(playerRef, manaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.arcaniste.DistorsionSkill.SKILL_ID);
        maybeEchoArcanique(uuid, acc, fr.varyon.vrpg.classes.arcaniste.DistorsionSkill.SKILL_ID, bypass);
        notifySkill(uuid, "Distorsion");
        return true;
    }

    private boolean isArcaniste(@Nonnull ClassAccount acc) {
        return acc.getActiveClass() == PlayerClass.MAGE
            && acc.getActiveSpec(PlayerClass.MAGE) == fr.varyon.vrpg.classes.PlayerSpecialization.ARCANISTE;
    }

    private boolean isHoldingStaff(@Nonnull PlayerRef playerRef) {
        fr.varyon.vrpg.classes.WeaponCategory cat =
            fr.varyon.vrpg.classes.WeaponCategory.fromItemId(getHeldItemId(playerRef));
        return cat == fr.varyon.vrpg.classes.WeaponCategory.MAGIE;
    }

    private long applyEchoTemporel(@Nonnull ClassAccount acc, long cooldownMs) {
        int rank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.ECHO_TEMPOREL_NODE);
        if (rank <= 0) return cooldownMs;
        float reduc = fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.echoTemporelReducForRank(rank);
        return Math.round(cooldownMs * (1.0 - reduc));
    }

    private void maybeEchoArcanique(@Nonnull UUID uuid, @Nonnull ClassAccount acc,
                                     @Nonnull String skillId, boolean bypass) {
        if (bypass) return;
        int rank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.ECHO_ARCANIQUE_NODE);
        if (rank <= 0) return;
        if (Math.random() < fr.varyon.vrpg.classes.arcaniste.ArcanistPassifs.echoArcanicChanceForRank(rank)) {
            cooldowns.clearCooldown(uuid, skillId);
            notifySkill(uuid, "Écho Arcanique !");
        }
    }

    public boolean tryCastBouleDeFeu(@Nonnull UUID uuid,
                                      @Nonnull PlayerRef playerRef,
                                      @Nonnull Ref<EntityStore> entityRef,
                                      @Nonnull Store<EntityStore> store,
                                      @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isArcaniste(acc)) return false;
        if (!isHoldingStaff(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.arcaniste.BouleDeFeuSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        long cd = applyEchoTemporel(acc, fr.varyon.vrpg.classes.arcaniste.BouleDeFeuSkill.cooldownMsForRank(rank));
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.arcaniste.BouleDeFeuSkill.SKILL_ID, cd)) return false;
        float manaCost = fr.varyon.vrpg.classes.arcaniste.BouleDeFeuSkill.manaCostForRank(rank);
        if (!ClassSkillMana.hasEnough(playerRef, manaCost)) return false;
        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            if (tc != null && hr != null) {
                float dmg = getBaseDamage(playerRef) * fr.varyon.vrpg.classes.arcaniste.BouleDeFeuSkill.damagePctForRank(rank);
                org.joml.Vector3d spawnPos = new org.joml.Vector3d(tc.getPosition().x, tc.getPosition().y + 1.2, tc.getPosition().z);
                arcanistState.setLastCastFire(uuid, true);
                arcanistState.setPendingProjectileDmg(uuid, dmg, 1);
                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Staff", "SwingRight", true, store);
                ClassSkillSounds.playSkillSound("SFX_Staff_Flame_Fireball_Launch", playerRef, tc.getPosition(), null);
                spawnMagicProjectile(fr.varyon.vrpg.classes.arcaniste.BouleDeFeuSkill.PROJECTILE_CONFIG, spawnPos, hr.getDirection(), entityRef, playerRef, store, commandBuffer, dmg, fr.varyon.vrpg.classes.arcaniste.BouleDeFeuSkill.damageRadius(), null, 0f);
            }
        } catch (Exception ignored) {}
        ClassSkillMana.consume(playerRef, manaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.arcaniste.BouleDeFeuSkill.SKILL_ID);
        maybeEchoArcanique(uuid, acc, fr.varyon.vrpg.classes.arcaniste.BouleDeFeuSkill.SKILL_ID, bypass);
        notifySkill(uuid, "Boule de Feu");
        return true;
    }

    public boolean tryCastMeteore(@Nonnull UUID uuid,
                                   @Nonnull PlayerRef playerRef,
                                   @Nonnull Ref<EntityStore> entityRef,
                                   @Nonnull Store<EntityStore> store,
                                   @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isArcaniste(acc)) return false;
        if (!isHoldingStaff(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        long cd = applyEchoTemporel(acc, fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.cooldownMsForRank(rank));
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.SKILL_ID, cd)) return false;
        float manaCost = fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.manaCostForRank(rank);
        if (!ClassSkillMana.hasEnough(playerRef, manaCost)) return false;
        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            com.hypixel.hytale.server.core.universe.world.World world = store.getExternalData().getWorld();
            if (tc == null || hr == null || world == null) return false;

            org.joml.Vector3d casterPos = tc.getPosition();
            org.joml.Vector3d direction = new org.joml.Vector3d(hr.getDirection()).normalize();
            org.joml.Vector3d eyePos = new org.joml.Vector3d(casterPos.x, casterPos.y + 1.6, casterPos.z);
            org.joml.Vector3d zoneCenter = BlockRaystep.hitPosition(
                world, eyePos, direction, fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.maxTargetDistance(), 0.5);
            zoneCenter.y += fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.groundYOffset();

            final float dmg = getBaseDamage(playerRef) * fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.damagePctForRank(rank);
            final float radius = fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.impactRadius();
            final long dropDelay = fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.dropDelayMs();
            final org.joml.Vector3d fImpact = new org.joml.Vector3d(zoneCenter);
            final Ref<EntityStore> fRef = entityRef;
            final PlayerRef fPr = playerRef;
            final com.hypixel.hytale.server.core.universe.world.World fw = world;
            final java.util.concurrent.atomic.AtomicReference<java.util.UUID> meteorProjectileId =
                new java.util.concurrent.atomic.AtomicReference<>();

            spawnMeteorParticle(fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.TELEGRAPH_PARTICLE, fImpact, store);
            spawnMeteorFalling(fRef, fImpact, fw, meteorProjectileId);
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "meteore"); t.setDaemon(true); return t;
            }).schedule(() -> fw.execute(() -> {
                try {
                    Store<EntityStore> ws = fw.getEntityStore().getStore();
                    removeMeteorProjectile(ws, meteorProjectileId.get(), fImpact);
                    if (!fRef.isValid()) return;
                    spawnMeteorParticle(fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.IMPACT_PARTICLE, fImpact, ws);
                    ClassSkillSounds.playSkillSound(fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.IMPACT_SOUND, fPr, fImpact, null);
                    damageNearby(fImpact, radius, fRef, ws, dmg, resolveFireDamageCause());
                } catch (Exception e) {
                    LOG.atWarning().log("[Meteore] impact failed: " + e.getMessage());
                }
            }), dropDelay, java.util.concurrent.TimeUnit.MILLISECONDS);

            AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Staff", "SwingRight", true, store);
            ClassSkillSounds.playSkillSound("SFX_Staff_Flame_Fireball_Launch", playerRef, casterPos, commandBuffer);
        } catch (Exception e) {
            LOG.atWarning().log("[Meteore] cast failed: " + e.getMessage());
            return false;
        }
        ClassSkillMana.consume(playerRef, manaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.SKILL_ID);
        maybeEchoArcanique(uuid, acc, fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.SKILL_ID, bypass);
        notifySkill(uuid, "Meteore");
        return true;
    }

    public boolean tryCastNovaDeGivre(@Nonnull UUID uuid,
                                       @Nonnull PlayerRef playerRef,
                                       @Nonnull Ref<EntityStore> entityRef,
                                       @Nonnull Store<EntityStore> store,
                                       @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isArcaniste(acc)) return false;
        if (!isHoldingStaff(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.arcaniste.NovaDeGivreSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        long cd = applyEchoTemporel(acc, fr.varyon.vrpg.classes.arcaniste.NovaDeGivreSkill.cooldownMsForRank(rank));
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.arcaniste.NovaDeGivreSkill.SKILL_ID, cd)) return false;
        float manaCost = fr.varyon.vrpg.classes.arcaniste.NovaDeGivreSkill.manaCostForRank(rank);
        if (!ClassSkillMana.hasEnough(playerRef, manaCost)) return false;
        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                org.joml.Vector3d center = tc.getPosition();
                float dmg = getBaseDamage(playerRef) * fr.varyon.vrpg.classes.arcaniste.NovaDeGivreSkill.damagePctForRank(rank);
                float radius = fr.varyon.vrpg.classes.arcaniste.NovaDeGivreSkill.radius();
                float slowSec = fr.varyon.vrpg.classes.arcaniste.NovaDeGivreSkill.slowMsForRank(rank) / 1000f;
                int slowIdx = com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getIndex(fr.varyon.vrpg.classes.arcaniste.NovaDeGivreSkill.SLOW_EFFECT);
                com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect slowEff = slowIdx >= 0
                    ? (com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect) com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getAsset(slowIdx) : null;
                final com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect fSlowEff = slowEff;
                final float fSlowSec = slowSec;
                damageNearby(center, radius, entityRef, store, dmg, resolveIceDamageCause());
                // Slow séparé
                long casterIdx = entityRef.getIndex();
                com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector
                    .selectNearbyEntities(store, center, radius, targetRef -> {
                        try {
                            if (targetRef.getIndex() == casterIdx) return;
                            com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent ec =
                                store.getComponent(targetRef, com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
                            if (ec != null && fSlowEff != null) ec.addEffect(targetRef, fSlowEff, fSlowSec,
                                com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior.OVERWRITE, store);
                        } catch (Exception ignored2) {}
                    }, t -> t.getIndex() != casterIdx);
                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Staff", "SwingLeft", true, store);
                ClassSkillSounds.playSkillSound("SFX_Vrpg_Punch", playerRef, center, null);
            }
        } catch (Exception ignored) {}
        ClassSkillMana.consume(playerRef, manaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.arcaniste.NovaDeGivreSkill.SKILL_ID);
        maybeEchoArcanique(uuid, acc, fr.varyon.vrpg.classes.arcaniste.NovaDeGivreSkill.SKILL_ID, bypass);
        notifySkill(uuid, "Nova de Givre");
        return true;
    }

    public boolean tryCastSurcharge(@Nonnull UUID uuid,
                                     @Nonnull PlayerRef playerRef,
                                     @Nonnull Ref<EntityStore> entityRef,
                                     @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isArcaniste(acc)) return false;
        int rank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.arcaniste.SurchargeSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        long cd = applyEchoTemporel(acc, fr.varyon.vrpg.classes.arcaniste.SurchargeSkill.cooldownMsForRank(rank));
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.arcaniste.SurchargeSkill.SKILL_ID, cd)) return false;
        float manaCost = fr.varyon.vrpg.classes.arcaniste.SurchargeSkill.manaCostForRank(rank);
        if (!ClassSkillMana.hasEnough(playerRef, manaCost)) return false;
        arcanistState.startSurcharge(uuid, fr.varyon.vrpg.classes.arcaniste.SurchargeSkill.durationMsForRank(rank),
            fr.varyon.vrpg.classes.arcaniste.SurchargeSkill.damageBonusForRank(rank));
        ClassSkillMana.restore(playerRef, ClassSkillMana.getMaxMana(playerRef) * fr.varyon.vrpg.classes.arcaniste.SurchargeSkill.manaRestorePctForRank(rank));
        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) ClassSkillSounds.playSkillSound("SFX_Vrpg_Combo_3", playerRef, tc.getPosition(), null);
        } catch (Exception ignored) {}
        ClassSkillMana.consume(playerRef, manaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.arcaniste.SurchargeSkill.SKILL_ID);
        maybeEchoArcanique(uuid, acc, fr.varyon.vrpg.classes.arcaniste.SurchargeSkill.SKILL_ID, bypass);
        notifySkill(uuid, "Surcharge");
        return true;
    }

    public boolean tryCastSalveDeGivre(@Nonnull UUID uuid,
                                        @Nonnull PlayerRef playerRef,
                                        @Nonnull Ref<EntityStore> entityRef,
                                        @Nonnull Store<EntityStore> store,
                                        @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isArcaniste(acc)) return false;
        if (!isHoldingStaff(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.arcaniste.SalveDeGivreSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        long cd = applyEchoTemporel(acc, fr.varyon.vrpg.classes.arcaniste.SalveDeGivreSkill.cooldownMsForRank(rank));
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.arcaniste.SalveDeGivreSkill.SKILL_ID, cd)) return false;
        float manaCost = fr.varyon.vrpg.classes.arcaniste.SalveDeGivreSkill.manaCostForRank(rank);
        if (!ClassSkillMana.hasEnough(playerRef, manaCost)) return false;
        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            if (tc != null && hr != null) {
                int bolts = fr.varyon.vrpg.classes.arcaniste.SalveDeGivreSkill.boltCountForRank(rank);
                float dmg = getBaseDamage(playerRef) * fr.varyon.vrpg.classes.arcaniste.SalveDeGivreSkill.damagePctPerHitForRank(rank);
                float slowSec = fr.varyon.vrpg.classes.arcaniste.SalveDeGivreSkill.slowMsForRank(rank) / 1000f;
                long delayMs = fr.varyon.vrpg.classes.arcaniste.SalveDeGivreSkill.boltDelayMs();
                org.joml.Vector3d chestPos = new org.joml.Vector3d(tc.getPosition().x, tc.getPosition().y + 1.2, tc.getPosition().z);
                int slowIdx = com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getIndex(fr.varyon.vrpg.classes.arcaniste.SalveDeGivreSkill.SLOW_EFFECT);
                com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect slowEff = slowIdx >= 0
                    ? (com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect) com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getAsset(slowIdx) : null;
                org.joml.Vector3d baseDir = hr.getDirection();
                arcanistState.setLastCastFire(uuid, false);
                arcanistState.setPendingProjectileDmg(uuid, dmg, bolts);
                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Staff", "SwingLeft", true, store);
                final org.joml.Vector3d fDir = new org.joml.Vector3d(baseDir).normalize();
                final float fDmg = dmg; final float fSlowSec = slowSec;
                final com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect fSlowEff = slowEff;
                final Ref<EntityStore> fRef = entityRef;
                final Store<EntityStore> fStore = store;
                final org.joml.Vector3d fPos = new org.joml.Vector3d(chestPos);
                final PlayerRef fPlayerRef = playerRef;
                for (int i = 0; i < bolts; i++) {
                    if (i == 0) {
                        ClassSkillSounds.playSkillSound("SFX_Vrpg_Salve_Launch", playerRef, tc.getPosition(), null);
                        spawnMagicProjectile("Projectile_Config_Ice_Bolt", fPos, fDir, fRef, playerRef, fStore, commandBuffer, fDmg, 1.5f, fSlowEff, fSlowSec);
                    } else {
                        final long fDelay = delayMs * i;
                        final int fi = i;
                        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "salve-" + fi); t.setDaemon(true); return t; })
                            .schedule(() -> {
                                ClassSkillSounds.playSkillSound("SFX_Vrpg_Salve_Launch", fPlayerRef, fPos, null);
                                spawnMagicProjectile("Projectile_Config_Ice_Bolt", fPos, fDir, fRef, fPlayerRef, fStore, null, fDmg, 1.5f, fSlowEff, fSlowSec);
                            }, fDelay, java.util.concurrent.TimeUnit.MILLISECONDS);
                    }
                }
            }
        } catch (Exception ignored) {}
        ClassSkillMana.consume(playerRef, manaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.arcaniste.SalveDeGivreSkill.SKILL_ID);
        maybeEchoArcanique(uuid, acc, fr.varyon.vrpg.classes.arcaniste.SalveDeGivreSkill.SKILL_ID, bypass);
        notifySkill(uuid, "Salve de Givre");
        return true;
    }

    private void spawnMagicProjectile(@Nonnull String configId,
                                       @Nonnull org.joml.Vector3d spawnPos,
                                       @Nonnull org.joml.Vector3d dir,
                                       @Nonnull Ref<EntityStore> casterRef,
                                       @Nonnull PlayerRef playerRef,
                                       @Nonnull Store<EntityStore> store,
                                       @Nullable CommandBuffer<EntityStore> commandBuffer,
                                       float dmg, float impactRadius,
                                       @Nullable com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect slowEff,
                                       float slowSec) {
        try {
            com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig cfg =
                com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig.getAssetMap().getAsset(configId);
            if (cfg == null) return;
            if (commandBuffer != null) {
                com.hypixel.hytale.server.core.modules.projectile.ProjectileModule.get()
                    .spawnProjectile(casterRef, commandBuffer, cfg, spawnPos, dir);
            } else {
                com.hypixel.hytale.server.core.universe.world.World world = null;
                try { java.util.UUID wUuid = playerRef.getWorldUuid(); if (wUuid != null) world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(wUuid); } catch (Exception ignored2) {}
                if (world != null) {
                    final com.hypixel.hytale.server.core.universe.world.World fw = world;
                    final com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig fCfg = cfg;
                    final org.joml.Vector3d fPos = spawnPos, fDir = dir;
                    final Ref<EntityStore> fRef = casterRef;
                    fw.execute(() -> {
                        try {
                            Store<EntityStore> ws = fw.getEntityStore().getStore();
                            java.lang.reflect.Method takeCmd = ws.getClass().getDeclaredMethod("takeCommandBuffer");
                            takeCmd.setAccessible(true);
                            @SuppressWarnings("unchecked") CommandBuffer<EntityStore> cb = (CommandBuffer<EntityStore>) takeCmd.invoke(ws);
                            try {
                                com.hypixel.hytale.server.core.modules.projectile.ProjectileModule.get().spawnProjectile(fRef, cb, fCfg, fPos, fDir);
                            } finally {
                                try { java.lang.reflect.Method c = cb.getClass().getDeclaredMethod("consume"); c.setAccessible(true); c.invoke(cb); } catch (Exception ignored3) {}
                            }
                        } catch (Exception ignored2) {}
                    });
                }
            }
        } catch (Exception ignored) {}
    }

    private static com.hypixel.hytale.server.core.modules.entity.damage.DamageCause resolveFireDamageCause() {
        com.hypixel.hytale.server.core.modules.entity.damage.DamageCause fire =
            (com.hypixel.hytale.server.core.modules.entity.damage.DamageCause)
            com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.getAssetMap().getAsset("Fire");
        return fire != null ? fire : com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL;
    }

    private static com.hypixel.hytale.server.core.modules.entity.damage.DamageCause resolveIceDamageCause() {
        com.hypixel.hytale.server.core.modules.entity.damage.DamageCause ice =
            (com.hypixel.hytale.server.core.modules.entity.damage.DamageCause)
            com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.getAssetMap().getAsset("Ice");
        return ice != null ? ice : com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL;
    }

    private void spawnMeteorParticle(@Nonnull String particleId,
                                     @Nonnull org.joml.Vector3d at,
                                     @Nonnull Store<EntityStore> store) {
        try {
            com.hypixel.hytale.server.core.universe.world.ParticleUtil.spawnParticleEffect(particleId, at, store);
        } catch (Exception e) {
            LOG.atFine().log("[Meteore] particle " + particleId + " failed: " + e.getMessage());
        }
    }

    private void spawnMeteorFalling(@Nonnull Ref<EntityStore> casterRef,
                                    @Nonnull org.joml.Vector3d zoneCenter,
                                    @Nonnull com.hypixel.hytale.server.core.universe.world.World world,
                                    @Nonnull java.util.concurrent.atomic.AtomicReference<java.util.UUID> outProjectileId) {
        try {
            com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig cfg =
                com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig.getAssetMap()
                    .getAsset(fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.FALLING_PROJECTILE);
            if (cfg == null) return;
            double dropHeight = fr.varyon.vrpg.classes.arcaniste.MeteoreSkill.dropHeight();
            org.joml.Vector3d spawnPos = new org.joml.Vector3d(zoneCenter.x, zoneCenter.y + dropHeight, zoneCenter.z);
            org.joml.Vector3d downDir = new org.joml.Vector3d(0.0, -1.0, 0.0);
            final com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig fCfg = cfg;
            final org.joml.Vector3d fPos = spawnPos;
            final org.joml.Vector3d fDir = downDir;
            final Ref<EntityStore> fRef = casterRef;
            world.execute(() -> {
                try {
                    if (!fRef.isValid()) return;
                    Store<EntityStore> ws = world.getEntityStore().getStore();
                    java.lang.reflect.Method takeCmd = ws.getClass().getDeclaredMethod("takeCommandBuffer");
                    takeCmd.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    CommandBuffer<EntityStore> cb = (CommandBuffer<EntityStore>) takeCmd.invoke(ws);
                    if (cb == null) return;
                    try {
                        java.util.UUID projectileId = java.util.UUID.randomUUID();
                        Ref<EntityStore> projectileRef = com.hypixel.hytale.server.core.modules.projectile.ProjectileModule.get()
                            .spawnProjectile(projectileId, fRef, cb, fCfg, fPos, fDir);
                        if (projectileRef != null) {
                            outProjectileId.set(projectileId);
                        }
                    } finally {
                        try {
                            java.lang.reflect.Method c = cb.getClass().getDeclaredMethod("consume");
                            c.setAccessible(true);
                            c.invoke(cb);
                        } catch (Exception ignored) {}
                    }
                } catch (Exception e) {
                    LOG.atWarning().log("[Meteore] falling projectile failed: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            LOG.atFine().log("[Meteore] falling projectile failed: " + e.getMessage());
        }
    }

    @Nullable
    private Ref<EntityStore> findMeteorProjectileRef(@Nonnull Store<EntityStore> store,
                                                     @Nullable java.util.UUID projectileId,
                                                     @Nonnull org.joml.Vector3d impactCenter) {
        final Ref<EntityStore>[] found = new Ref[1];
        if (projectileId != null) {
            try {
                store.forEachChunk(com.hypixel.hytale.component.query.Query.any(), (chunk, commandBuffer) -> {
                    if (found[0] != null) return;
                    if (!chunk.getArchetype().contains(com.hypixel.hytale.server.core.entity.UUIDComponent.getComponentType())) return;
                    for (int i = 0; i < chunk.size(); i++) {
                        Ref<EntityStore> ref = chunk.getReferenceTo(i);
                        if (ref == null || !ref.isValid()) continue;
                        com.hypixel.hytale.server.core.entity.UUIDComponent uuidComponent =
                            store.getComponent(ref, com.hypixel.hytale.server.core.entity.UUIDComponent.getComponentType());
                        if (uuidComponent != null && projectileId.equals(uuidComponent.getUuid())) {
                            found[0] = ref;
                            return;
                        }
                    }
                });
            } catch (Exception ignored) {}
        }
        if (found[0] != null) return found[0];
        try {
            com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector
                .selectNearbyEntities(store, impactCenter, 4.0, ref -> {
                    if (found[0] != null) return;
                    if (store.getComponent(ref, com.hypixel.hytale.server.core.modules.projectile.component.Projectile.getComponentType()) == null) return;
                    found[0] = ref;
                }, ref -> store.getComponent(ref, com.hypixel.hytale.server.core.modules.projectile.component.Projectile.getComponentType()) != null);
        } catch (Exception ignored) {}
        return found[0];
    }

    private void removeMeteorProjectile(@Nonnull Store<EntityStore> store,
                                        @Nullable java.util.UUID projectileId,
                                        @Nonnull org.joml.Vector3d impactCenter) {
        Ref<EntityStore> projectileRef = findMeteorProjectileRef(store, projectileId, impactCenter);
        if (projectileRef == null || !projectileRef.isValid()) return;
        try {
            java.lang.reflect.Method takeCmd = store.getClass().getDeclaredMethod("takeCommandBuffer");
            takeCmd.setAccessible(true);
            @SuppressWarnings("unchecked")
            CommandBuffer<EntityStore> cb = (CommandBuffer<EntityStore>) takeCmd.invoke(store);
            if (cb != null) {
                try {
                    cb.removeEntity(projectileRef, com.hypixel.hytale.component.RemoveReason.REMOVE);
                } finally {
                    try {
                        java.lang.reflect.Method c = cb.getClass().getDeclaredMethod("consume");
                        c.setAccessible(true);
                        c.invoke(cb);
                    } catch (Exception ignored) {}
                }
                return;
            }
        } catch (Exception ignored) {}
        try {
            store.removeEntity(projectileRef, com.hypixel.hytale.component.RemoveReason.REMOVE);
        } catch (Exception e) {
            LOG.atFine().log("[Meteore] projectile cleanup failed: " + e.getMessage());
        }
    }

    // =====================================================================
    // Gardien de Gaïa
    // =====================================================================

    private boolean isGardienDeGaia(@Nonnull ClassAccount acc) {
        return acc.getActiveClass() == PlayerClass.MAGE
            && acc.getActiveSpec(PlayerClass.MAGE) == PlayerSpecialization.GARDIEN_DE_GAIA;
    }

    public boolean tryCastEvasionSylvestre(@Nonnull UUID uuid,
                                            @Nonnull PlayerRef playerRef,
                                            @Nonnull Ref<EntityStore> entityRef,
                                            @Nonnull Store<EntityStore> store,
                                            @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isGardienDeGaia(acc)) return false;
        if (!isHoldingStaff(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.gardiendesgaia.EvasionSylvestreSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        long cd = fr.varyon.vrpg.classes.gardiendesgaia.EvasionSylvestreSkill.cooldownMsForRank(rank);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.gardiendesgaia.EvasionSylvestreSkill.SKILL_ID, cd)) return false;
        float manaCost = fr.varyon.vrpg.classes.gardiendesgaia.EvasionSylvestreSkill.manaCostForRank(rank);
        if (!ClassSkillMana.hasEnough(playerRef, manaCost)) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            if (tc != null && hr != null) {
                org.joml.Vector3d lookDir = hr.getDirection();
                double fwdX = lookDir.x, fwdZ = lookDir.z;
                double fwdLen = Math.sqrt(fwdX * fwdX + fwdZ * fwdZ);
                if (fwdLen > 1e-6) { fwdX /= fwdLen; fwdZ /= fwdLen; }
                double dashX = -fwdX, dashZ = -fwdZ;
                double baseForce = 12.0 + fr.varyon.vrpg.classes.gardiendesgaia.EvasionSylvestreSkill.dashDistanceForRank(rank) * 0.875;
                com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig dashConfig =
                    new com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig();
                dashConfig.setAirResistance(0.97f);
                dashConfig.setAirResistanceMax(0.96f);
                dashConfig.setGroundResistance(0.94f);
                dashConfig.setGroundResistanceMax(0.82f);
                dashConfig.setThreshold(5.0f);
                dashConfig.setStyle(com.hypixel.hytale.protocol.VelocityThresholdStyle.Exp);
                com.hypixel.hytale.server.core.modules.physics.component.Velocity vel =
                    commandBuffer != null
                        ? commandBuffer.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType())
                        : store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType());
                if (vel != null) {
                    org.joml.Vector3d dashVel = new org.joml.Vector3d(dashX * baseForce, 0.2, dashZ * baseForce);
                    vel.setClient(dashVel);
                    vel.getInstructions().clear();
                    vel.addInstruction(dashVel, dashConfig, com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                }
                ClassSkillSounds.playSkillSound("SFX_Vrpg_OmbreVanish", playerRef, tc.getPosition(), commandBuffer);
            }
        } catch (Exception ignored) {}

        ClassSkillMana.consume(playerRef, manaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.gardiendesgaia.EvasionSylvestreSkill.SKILL_ID);
        notifySkill(uuid, "Évasion Sylvestre");
        return true;
    }

    public boolean tryCastBenedictionDeGaia(@Nonnull UUID uuid,
                                             @Nonnull PlayerRef playerRef,
                                             @Nonnull Ref<EntityStore> entityRef,
                                             @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isGardienDeGaia(acc)) return false;
        if (!isHoldingStaff(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.gardiendesgaia.BenedictionDeGaiaSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        long cd = fr.varyon.vrpg.classes.gardiendesgaia.BenedictionDeGaiaSkill.cooldownMsForRank(rank);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.gardiendesgaia.BenedictionDeGaiaSkill.SKILL_ID, cd)) return false;
        float manaCost = fr.varyon.vrpg.classes.gardiendesgaia.BenedictionDeGaiaSkill.manaCostForRank(rank);
        if (!ClassSkillMana.hasEnough(playerRef, manaCost)) return false;

        float baseHeal = fr.varyon.vrpg.classes.gardiendesgaia.BenedictionDeGaiaSkill.healAmountForRank(rank);
        double radius  = fr.varyon.vrpg.classes.gardiendesgaia.BenedictionDeGaiaSkill.HEAL_RADIUS;
        int graceRank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaPassifs.GRACE_NODE);
        float graceBonus = graceRank > 0 ? fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaPassifs.graceBonusForRank(graceRank) : 0f;
        float graceSeuil = fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaPassifs.GRACE_HP_SEUIL;
        boolean doCycle = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaPassifs.CYCLE_NODE) > 0;
        int hIdx;
        try { hIdx = com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes.getHealth(); }
        catch (Exception e) { hIdx = -1; }

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null && hIdx >= 0) {
                org.joml.Vector3d center = tc.getPosition();
                final int fHIdx = hIdx;
                final float fBaseHeal = baseHeal;
                final float fGraceBonus = graceBonus;
                final float fGraceSeuil = graceSeuil;
                boolean[] healed = {false};

                // Healer toutes les entités proches (joueurs + NPCs alliés + dummies)
                com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector
                    .selectNearbyEntities(store, center, (float) radius, ref -> {
                        try {
                            com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap sm =
                                store.getComponent(ref, com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.getComponentType());
                            if (sm == null) return;
                            var hp = sm.get(fHIdx);
                            if (hp == null || hp.getMax() <= 0) return;
                            float heal = fBaseHeal;
                            if (fGraceBonus > 0f && hp.get() / hp.getMax() < fGraceSeuil)
                                heal *= (1f + fGraceBonus);
                            sm.setStatValue(fHIdx, Math.min(hp.getMax(), hp.get() + heal));
                            healed[0] = true;
                        } catch (Exception ignored2) {}
                    }, ref -> true);

                // Aussi healer les joueurs du serveur dans le rayon (ils peuvent ne pas être dans le même Store)
                for (PlayerRef ally : com.hypixel.hytale.server.core.universe.Universe.get().getPlayers()) {
                    try {
                        Ref<EntityStore> allyRef = ally.getReference();
                        if (allyRef == null || !allyRef.isValid()) continue;
                        Store<EntityStore> allyStore = allyRef.getStore();
                        if (allyStore == null || allyStore == store) continue; // déjà traité par selectNearbyEntities
                        TransformComponent allyTc = allyStore.getComponent(allyRef, TransformComponent.getComponentType());
                        if (allyTc == null) continue;
                        double dx = allyTc.getPosition().x - center.x;
                        double dy = allyTc.getPosition().y - center.y;
                        double dz = allyTc.getPosition().z - center.z;
                        if (dx * dx + dy * dy + dz * dz > radius * radius) continue;
                        com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap sm =
                            ally.getComponent(com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.getComponentType());
                        if (sm == null) continue;
                        var hp = sm.get(fHIdx);
                        if (hp == null || hp.getMax() <= 0) continue;
                        float heal = fBaseHeal;
                        if (fGraceBonus > 0f && hp.get() / hp.getMax() < fGraceSeuil)
                            heal *= (1f + fGraceBonus);
                        sm.setStatValue(fHIdx, Math.min(hp.getMax(), hp.get() + heal));
                        healed[0] = true;
                    } catch (Exception ignored2) {}
                }

                if (healed[0] && doCycle) gardienState.notifyHeal(uuid);
                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Staff", "SwingLeft", true, store);
                ClassSkillSounds.playSkillSound("SFX_Vrpg_TreeRegrowth", playerRef, center, null);
            }
        } catch (Exception ignored) {}

        ClassSkillMana.consume(playerRef, manaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.gardiendesgaia.BenedictionDeGaiaSkill.SKILL_ID);
        notifySkill(uuid, "Bénédiction de Gaïa");
        return true;
    }

    public boolean tryCastMarqueDeRenaissance(@Nonnull UUID uuid,
                                               @Nonnull PlayerRef playerRef,
                                               @Nonnull Ref<EntityStore> entityRef,
                                               @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isGardienDeGaia(acc)) return false;
        if (!isHoldingStaff(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.gardiendesgaia.MarqueDeRenaissanceSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        long cd = fr.varyon.vrpg.classes.gardiendesgaia.MarqueDeRenaissanceSkill.cooldownMsForRank(rank);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.gardiendesgaia.MarqueDeRenaissanceSkill.SKILL_ID, cd)) return false;
        float manaCost = fr.varyon.vrpg.classes.gardiendesgaia.MarqueDeRenaissanceSkill.manaCostForRank(rank);
        if (!ClassSkillMana.hasEnough(playerRef, manaCost)) return false;

        // Ciblage : joueur dans le regard dans les 8 blocs, sinon soi-même
        Ref<EntityStore> targetEntityRef = findTargetedAllyEntityRef(entityRef, store, 8.0);
        UUID targetUuid = uuid;
        Ref<EntityStore> visualTargetRef = entityRef;
        Store<EntityStore> visualTargetStore = store;
        if (targetEntityRef != null) {
            PlayerRef targetPlayer = store.getComponent(targetEntityRef, PlayerRef.getComponentType());
            if (targetPlayer != null) {
                targetUuid = targetPlayer.getUuid();
                visualTargetRef = targetEntityRef;
                visualTargetStore = targetEntityRef.getStore() != null ? targetEntityRef.getStore() : store;
            }
        }

        gardienState.armMarque(targetUuid, fr.varyon.vrpg.classes.gardiendesgaia.MarqueDeRenaissanceSkill.durationMsForRank(rank));

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                int effIdx = com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect
                    .getAssetMap().getIndex("Vrpg_Marque_Renaissance");
                com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect eff = effIdx >= 0
                    ? (com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect)
                      com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getAsset(effIdx)
                    : null;
                if (eff != null) {
                    Store<EntityStore> vs = visualTargetStore;
                    Ref<EntityStore> vr = visualTargetRef;
                    com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent ec =
                        vs.getComponent(vr, com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
                    if (ec != null)
                        ec.addEffect(vr, eff,
                            fr.varyon.vrpg.classes.gardiendesgaia.MarqueDeRenaissanceSkill.durationMsForRank(rank) / 1000f,
                            com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior.OVERWRITE, vs);
                }
                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Staff", "SwingLeft", true, store);
                ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", playerRef, tc.getPosition(), null);
            }
        } catch (Exception ignored) {}

        ClassSkillMana.consume(playerRef, manaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.gardiendesgaia.MarqueDeRenaissanceSkill.SKILL_ID);
        notifySkill(uuid, "Marque de Renaissance");
        return true;
    }

    public boolean tryCastEtreinteDeGaia(@Nonnull UUID uuid,
                                          @Nonnull PlayerRef playerRef,
                                          @Nonnull Ref<EntityStore> entityRef,
                                          @Nonnull Store<EntityStore> store,
                                          @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isGardienDeGaia(acc)) return false;
        if (!isHoldingStaff(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.gardiendesgaia.EtreinteDeGaiaSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        long cd = fr.varyon.vrpg.classes.gardiendesgaia.EtreinteDeGaiaSkill.cooldownMsForRank(rank);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.gardiendesgaia.EtreinteDeGaiaSkill.SKILL_ID, cd)) return false;
        float manaCost = fr.varyon.vrpg.classes.gardiendesgaia.EtreinteDeGaiaSkill.manaCostForRank(rank);
        if (!ClassSkillMana.hasEnough(playerRef, manaCost)) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            com.hypixel.hytale.server.core.universe.world.World world = null;
            try {
                java.util.UUID wUuid = playerRef.getWorldUuid();
                if (wUuid != null) world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(wUuid);
            } catch (Exception ignored2) {}
            if (tc == null || hr == null || world == null) return false;

            org.joml.Vector3d eyePos = new org.joml.Vector3d(
                tc.getPosition().x, tc.getPosition().y + 1.2, tc.getPosition().z);
            org.joml.Vector3d dir = new org.joml.Vector3d(hr.getDirection()).normalize();
            org.joml.Vector3d impactPos = BlockRaystep.hitPosition(
                world, eyePos, dir, fr.varyon.vrpg.classes.gardiendesgaia.EtreinteDeGaiaSkill.CAST_RANGE, 0.3);

            double dist = impactPos.distance(eyePos);
            long delayMs = (long) (dist / fr.varyon.vrpg.classes.gardiendesgaia.EtreinteDeGaiaSkill.PROJECTILE_SPEED * 1000.0);

            spawnMagicProjectile(fr.varyon.vrpg.classes.gardiendesgaia.EtreinteDeGaiaSkill.PROJECTILE_CONFIG,
                eyePos, dir, entityRef, playerRef, store, commandBuffer, 0f, 0f, null, 0f);
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Staff", "SwingRight", true, store);

            final int fRank = rank;
            final org.joml.Vector3d fImpact = new org.joml.Vector3d(impactPos);
            final Ref<EntityStore> fCasterRef = entityRef;
            final com.hypixel.hytale.server.core.universe.world.World fw = world;

            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "etreinte"); t.setDaemon(true); return t;
            }).schedule(() -> fw.execute(() -> {
                try {
                    if (!fCasterRef.isValid()) return;
                    Store<EntityStore> ws = fw.getEntityStore().getStore();
                    float rootRadius = (float) fr.varyon.vrpg.classes.gardiendesgaia.EtreinteDeGaiaSkill.rootRadius();
                    float rootSec    = fr.varyon.vrpg.classes.gardiendesgaia.EtreinteDeGaiaSkill.rootDurationMs(fRank) / 1000f;
                    int rootEffIdx = com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect
                        .getAssetMap().getIndex("Vrpg_Etreinte_Root");
                    com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect rootEff = rootEffIdx >= 0
                        ? (com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect)
                          com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getAsset(rootEffIdx)
                        : null;
                    if (rootEff == null) return;
                    spawnMeteorParticle(fr.varyon.vrpg.classes.gardiendesgaia.EtreinteDeGaiaSkill.IMPACT_PARTICLE, fImpact, ws);
                    long casterIdx = fCasterRef.getIndex();
                    final com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect fRootEff = rootEff;
                    com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector
                        .selectNearbyEntities(ws, fImpact, rootRadius, targetRef -> {
                            try {
                                if (targetRef.getIndex() == casterIdx) return;
                                com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent ec =
                                    ws.getComponent(targetRef,
                                        com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
                                if (ec != null)
                                    ec.addEffect(targetRef, fRootEff, rootSec,
                                        com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior.OVERWRITE, ws);
                            } catch (Exception ignored2) {}
                        }, t -> t.getIndex() != casterIdx);
                } catch (Exception ignored) {}
            }), delayMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        } catch (Exception ignored) {}

        ClassSkillMana.consume(playerRef, manaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.gardiendesgaia.EtreinteDeGaiaSkill.SKILL_ID);
        notifySkill(uuid, "Étreinte de Gaïa");
        return true;
    }

    public boolean tryCastAppelDuTreant(@Nonnull UUID uuid,
                                         @Nonnull PlayerRef playerRef,
                                         @Nonnull Ref<EntityStore> entityRef,
                                         @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isGardienDeGaia(acc)) return false;
        if (!isHoldingStaff(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.gardiendesgaia.AppelDuTreantSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        long cd = fr.varyon.vrpg.classes.gardiendesgaia.AppelDuTreantSkill.cooldownMsForRank(rank);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.gardiendesgaia.AppelDuTreantSkill.SKILL_ID, cd)) return false;
        float manaCost = fr.varyon.vrpg.classes.gardiendesgaia.AppelDuTreantSkill.manaCostForRank(rank);
        if (!ClassSkillMana.hasEnough(playerRef, manaCost)) return false;

        int playerLevel = acc.getProgress(PlayerClass.MAGE).getLevel();
        float skillFactor = fr.varyon.vrpg.classes.gardiendesgaia.AppelDuTreantSkill.hpFactorForRank(rank);

        // HP : hpLevelMult × specHpMult × skillFactor
        float hpFactor = (float)(fr.varyon.vrpg.classes.ClassStatDefinition.hpLevelMult(playerLevel)
            * PlayerSpecialization.GARDIEN_DE_GAIA.getHpMult()) * skillFactor;
        int gardienRank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaPassifs.GARDIEN_NODE);
        if (gardienRank > 0) {
            hpFactor *= (1f + fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaPassifs.gardienHpBonusForRank(gardienRank));
        }
        final float finalHpFactor = hpFactor;

        // DMG : atkLevelMult × specAtkMult × skillFactor × 5 (base JSON = 10, on vise ×5)
        float dmgFactor = (float)(fr.varyon.vrpg.classes.ClassStatDefinition.atkLevelMultiplier(playerLevel)
            * PlayerSpecialization.GARDIEN_DE_GAIA.getAtkMult()) * skillFactor * 5f;
        final float finalDmgFactor = dmgFactor;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                com.hypixel.hytale.server.core.entity.entities.Player player =
                    store.getComponent(entityRef, com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
                com.hypixel.hytale.server.core.universe.world.World world =
                    player != null ? player.getWorld() : null;
                if (world != null) {
                    org.joml.Vector3d spawnPos = new org.joml.Vector3d(
                        tc.getPosition().x + 1.5, tc.getPosition().y, tc.getPosition().z + 1.5);
                    fr.varyon.vrpg.classes.gardiendesgaia.TreantSpawner.scheduleSpawn(
                        world, spawnPos, finalHpFactor, finalDmgFactor, uuid, gardienState);
                }
            }
        } catch (Exception ignored) {}

        ClassSkillMana.consume(playerRef, manaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.gardiendesgaia.AppelDuTreantSkill.SKILL_ID);
        notifySkill(uuid, "Appel du Tréant");
        return true;
    }

    public boolean tryCastEcorceProtectrice(@Nonnull UUID uuid,
                                             @Nonnull PlayerRef playerRef,
                                             @Nonnull Ref<EntityStore> entityRef,
                                             @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isGardienDeGaia(acc)) return false;
        if (!isHoldingStaff(playerRef)) { notifyNoWeapon(playerRef); return false; }
        int rank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.gardiendesgaia.EcorceProtectriceSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        long cd = fr.varyon.vrpg.classes.gardiendesgaia.EcorceProtectriceSkill.cooldownMsForRank(rank);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.gardiendesgaia.EcorceProtectriceSkill.SKILL_ID, cd)) return false;
        float manaCost = fr.varyon.vrpg.classes.gardiendesgaia.EcorceProtectriceSkill.manaCostForRank(rank);
        if (!ClassSkillMana.hasEnough(playerRef, manaCost)) return false;

        // Ciblage : entité dans le regard (joueur ou NPC allié/dummy) dans les 8 blocs, sinon soi-même
        Ref<EntityStore> targetEntityRef = findTargetedAllyEntityRef(entityRef, store, 8.0);
        UUID targetUuid = uuid;
        if (targetEntityRef != null) {
            PlayerRef targetPlayer = store.getComponent(targetEntityRef, PlayerRef.getComponentType());
            if (targetPlayer != null) targetUuid = targetPlayer.getUuid();
        }

        gardienState.startEcorce(uuid, targetUuid, targetEntityRef,
            fr.varyon.vrpg.classes.gardiendesgaia.EcorceProtectriceSkill.durationMsForRank(rank),
            fr.varyon.vrpg.classes.gardiendesgaia.EcorceProtectriceSkill.damageReductionForRank(rank),
            fr.varyon.vrpg.classes.gardiendesgaia.EcorceProtectriceSkill.healPerSecForRank(rank));

        // Cycle de Vie
        int cycleRank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.gardiendesgaia.GardienDeGaiaPassifs.CYCLE_NODE);
        if (cycleRank > 0) gardienState.notifyHeal(uuid);

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Staff", "SwingRight", true, store);
                ClassSkillSounds.playSkillSound("SFX_Vrpg_TreeRegrowth", playerRef, tc.getPosition(), null);
            }
        } catch (Exception ignored) {}

        ClassSkillMana.consume(playerRef, manaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.gardiendesgaia.EcorceProtectriceSkill.SKILL_ID);
        notifySkill(uuid, "Écorce Protectrice");
        return true;
    }

    private boolean isVaudou(@Nonnull ClassAccount acc) {
        return acc.getActiveClass() == PlayerClass.MAGE
            && acc.getActiveSpec(PlayerClass.MAGE) == PlayerSpecialization.VAUDOU;
    }


    public boolean tryCastPassageEthere(@Nonnull UUID uuid,
                                        @Nonnull PlayerRef playerRef,
                                        @Nonnull Ref<EntityStore> entityRef,
                                        @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isVaudou(acc)) return false;
        if (!isHoldingStaff(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.vaudou.PassageEthereSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.vaudou.PassageEthereSkill.SKILL_ID,
                fr.varyon.vrpg.classes.vaudou.PassageEthereSkill.cooldownMsForRank(rank))) return false;
        float manaCost = fr.varyon.vrpg.classes.vaudou.PassageEthereSkill.manaCostForRank(rank);
        if (!ClassSkillMana.hasEnough(playerRef, manaCost)) return false;

        Ref<EntityStore> targeted = findTargetedNpcRef(playerRef, entityRef, store,
            fr.varyon.vrpg.classes.vaudou.PassageEthereSkill.rangeForRank(rank));
        if (targeted == null) return false;

        try {
            TransformComponent tc    = store.getComponent(entityRef, TransformComponent.getComponentType());
            TransformComponent ttc   = store.getComponent(targeted, TransformComponent.getComponentType());
            if (tc != null && ttc != null) {
                org.joml.Vector3d targetPos = ttc.getPosition();
                org.joml.Vector3d playerPos = tc.getPosition();
                org.joml.Vector3d destPos = new org.joml.Vector3d(
                    2.0 * targetPos.x - playerPos.x,
                    targetPos.y,
                    2.0 * targetPos.z - playerPos.z);
                double facingDx = targetPos.x - destPos.x;
                double facingDz = targetPos.z - destPos.z;
                float facingYaw = (float) Math.atan2(-facingDx, -facingDz);
                com.hypixel.hytale.math.vector.Rotation3f faceRot = new com.hypixel.hytale.math.vector.Rotation3f(0f, facingYaw, 0f);
                com.hypixel.hytale.server.core.modules.entity.teleport.Teleport tele =
                    com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.createForPlayer(destPos, faceRot);
                tele.withoutVelocityReset();
                store.addComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.getComponentType(), tele);
                ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", playerRef, targetPos, null);
            }
        } catch (Exception ignored) {}

        ClassSkillMana.consume(playerRef, manaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.vaudou.PassageEthereSkill.SKILL_ID);
        notifySkill(uuid, "Passage Éthéré");
        return true;
    }

    public boolean tryCastFleauToxique(@Nonnull UUID uuid,
                                       @Nonnull PlayerRef playerRef,
                                       @Nonnull Ref<EntityStore> entityRef,
                                       @Nonnull Store<EntityStore> store,
                                       @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isVaudou(acc)) return false;
        if (!isHoldingStaff(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.vaudou.FleauToxiqueSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.vaudou.FleauToxiqueSkill.SKILL_ID,
                fr.varyon.vrpg.classes.vaudou.FleauToxiqueSkill.cooldownMsForRank(rank))) return false;
        float manaCost = fr.varyon.vrpg.classes.vaudou.FleauToxiqueSkill.manaCostForRank(rank);
        if (!ClassSkillMana.hasEnough(playerRef, manaCost)) return false;

        int toxinesRank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.vaudou.VaudouPassifs.TOXINES_NODE);
        float durationBonus = toxinesRank > 0
            ? fr.varyon.vrpg.classes.vaudou.VaudouPassifs.toxinesDurationBonusForRank(toxinesRank)
            : 0f;
        long durationMs = (long)(fr.varyon.vrpg.classes.vaudou.FleauToxiqueSkill.BASE_POISON_DURATION_MS * (1f + durationBonus));

        float weaponPct = fr.varyon.vrpg.classes.vaudou.FleauToxiqueSkill.poisonWeaponPctForRank(rank);
        int level = acc.getProgress(PlayerClass.MAGE).getLevel();
        float vaudouMageMult = (float) (ClassStatDefinition.atkDisplayMultiplier(level, PlayerSpecialization.VAUDOU)
            * PlayerSpecialization.VAUDOU.getWeaponMagieMult());
        float dpt = 50f * vaudouMageMult * weaponPct;

        vaudouState.setPendingFleau(uuid, dpt, durationMs, rank);

        try {
            TransformComponent casterTc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (casterTc != null) {
                com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                    store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
                org.joml.Vector3d dir = hr != null ? hr.getDirection() : new org.joml.Vector3d(0, 0, 1);
                org.joml.Vector3d casterPos = casterTc.getPosition();
                org.joml.Vector3d spawnPos = new org.joml.Vector3d(
                    casterPos.x + dir.x * 0.5,
                    casterPos.y + 1.5,
                    casterPos.z + dir.z * 0.5);
                try {
                    com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig cfg =
                        com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig.getAssetMap().getAsset("Vrpg_Fleau_Toxique");
                    if (cfg != null && commandBuffer != null) {
                        com.hypixel.hytale.server.core.modules.projectile.ProjectileModule.get()
                            .spawnProjectile(entityRef, commandBuffer, cfg, spawnPos, dir);
                    } else if (cfg != null) {
                        com.hypixel.hytale.server.core.universe.world.World w = null;
                        try { java.util.UUID wId = playerRef.getWorldUuid(); if (wId != null) w = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(wId); } catch (Exception ignored2) {}
                        if (w != null) {
                            final com.hypixel.hytale.server.core.universe.world.World fw = w;
                            final com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig fc = cfg;
                            final org.joml.Vector3d fPos = spawnPos, fDir = dir;
                            final Ref<EntityStore> fRef = entityRef;
                            fw.execute(() -> {
                                try {
                                    Store<EntityStore> ws = fw.getEntityStore().getStore();
                                    java.lang.reflect.Method takeCmd = ws.getClass().getDeclaredMethod("takeCommandBuffer");
                                    takeCmd.setAccessible(true);
                                    @SuppressWarnings("unchecked") CommandBuffer<EntityStore> cb = (CommandBuffer<EntityStore>) takeCmd.invoke(ws);
                                    try { com.hypixel.hytale.server.core.modules.projectile.ProjectileModule.get().spawnProjectile(fRef, cb, fc, fPos, fDir); }
                                    finally { try { java.lang.reflect.Method c = cb.getClass().getDeclaredMethod("consume"); c.setAccessible(true); c.invoke(cb); } catch (Exception ignored3) {} }
                                } catch (Exception ignored2) {}
                            });
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        ClassSkillMana.consume(playerRef, manaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.vaudou.FleauToxiqueSkill.SKILL_ID);
        notifySkill(uuid, "Fléau Toxique");
        return true;
    }

    public boolean tryCastTotemEntrave(@Nonnull UUID uuid,
                                       @Nonnull PlayerRef playerRef,
                                       @Nonnull Ref<EntityStore> entityRef,
                                       @Nonnull Store<EntityStore> store,
                                       @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isVaudou(acc)) return false;
        if (!isHoldingStaff(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.vaudou.TotemEntraveSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.vaudou.TotemEntraveSkill.SKILL_ID,
                fr.varyon.vrpg.classes.vaudou.TotemEntraveSkill.cooldownMsForRank(rank))) return false;
        float manaCost = fr.varyon.vrpg.classes.vaudou.TotemEntraveSkill.manaCostForRank(rank);
        if (!ClassSkillMana.hasEnough(playerRef, manaCost)) return false;

        int ancrageRank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.vaudou.VaudouPassifs.ANCRAGE_NODE);
        float durBonus = ancrageRank > 0 ? fr.varyon.vrpg.classes.vaudou.VaudouPassifs.ancrageTotemBonusForRank(ancrageRank) : 0f;
        long durationMs = (long)(fr.varyon.vrpg.classes.vaudou.TotemEntraveSkill.baseDurationMsForRank(rank) * (1f + durBonus));

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            if (tc != null && hr != null) {
                org.joml.Vector3d pos = tc.getPosition();
                org.joml.Vector3d dir = hr.getDirection();
                org.joml.Vector3d spawnPos = new org.joml.Vector3d(
                    pos.x + dir.x * 1.2,
                    pos.y + 1.4,
                    pos.z + dir.z * 1.2
                );
                fr.varyon.vrpg.classes.vaudou.VaudouTotemHelper.deploySlownessTotem(pos, durationMs);
                final long fDurEntrave = durationMs;
                final org.joml.Vector3d fSpawnPosEntrave = spawnPos, fDirEntrave = dir;
                final com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> fRefEntrave = entityRef;
                final com.hypixel.hytale.server.core.universe.PlayerRef fPrEntrave = playerRef;
                final com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> fStoreEntrave = store;
                final com.hypixel.hytale.component.CommandBuffer<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> fCbEntrave = commandBuffer;
                fr.varyon.vrpg.classes.vaudou.VaudouTotemHelper.patchAndRun("Slowness_Totem", fDurEntrave,
                    () -> spawnMagicProjectile("Vrpg_Totem_Throw", fSpawnPosEntrave, fDirEntrave, fRefEntrave, fPrEntrave, fStoreEntrave, fCbEntrave, 0f, 0f, null, 0f));
                ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", playerRef, pos, null);
            }
        } catch (Exception ignored) {}

        ClassSkillMana.consume(playerRef, manaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.vaudou.TotemEntraveSkill.SKILL_ID);
        notifySkill(uuid, "Totem d'Entrave");
        return true;
    }

    public boolean tryCastAttaquePerfide(@Nonnull UUID uuid,
                                         @Nonnull PlayerRef playerRef,
                                         @Nonnull Ref<EntityStore> entityRef,
                                         @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isVaudou(acc)) return false;
        if (!isHoldingStaff(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.vaudou.AttaquePerfideSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.vaudou.AttaquePerfideSkill.SKILL_ID,
                fr.varyon.vrpg.classes.vaudou.AttaquePerfideSkill.cooldownMsForRank(rank))) return false;
        float manaCost = fr.varyon.vrpg.classes.vaudou.AttaquePerfideSkill.manaCostForRank(rank);
        if (!ClassSkillMana.hasEnough(playerRef, manaCost)) return false;

        Ref<EntityStore> targeted = findTargetedNpcRef(playerRef, entityRef, store, 4.0);
        if (targeted == null) return false;

        try {
            TransformComponent tc   = store.getComponent(entityRef, TransformComponent.getComponentType());
            TransformComponent ttc  = store.getComponent(targeted, TransformComponent.getComponentType());
            if (tc != null && ttc != null) {
                float skillDmg = 50f * fr.varyon.vrpg.classes.vaudou.AttaquePerfideSkill.damagePctForRank(rank);

                org.joml.Vector3d playerPos = tc.getPosition();
                org.joml.Vector3d targetPos = ttc.getPosition();
                com.hypixel.hytale.server.core.modules.entity.component.HeadRotation targetHR =
                    store.getComponent(targeted, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
                if (targetHR != null) {
                    org.joml.Vector3d targetFacing = targetHR.getDirection();
                    double dx = playerPos.x - targetPos.x;
                    double dz = playerPos.z - targetPos.z;
                    double dot = dx * targetFacing.x + dz * targetFacing.z;
                    if (dot < 0) skillDmg *= (1f + fr.varyon.vrpg.classes.vaudou.AttaquePerfideSkill.behindBonus());
                }

                int rituelRank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.vaudou.VaudouPassifs.RITUEL_NODE);
                if (rituelRank > 0 && vaudouState.isNpcCursed(targeted.getIndex())) {
                    skillDmg *= (1f + fr.varyon.vrpg.classes.vaudou.VaudouPassifs.rituelBonusForRank(rituelRank));
                }

                float hpCostPct = fr.varyon.vrpg.classes.vaudou.AttaquePerfideSkill.hpCostPctForRank(rank);
                try {
                    com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap sm =
                        store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.getComponentType());
                    if (sm != null) {
                        int hIdx = com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes.getHealth();
                        var hpStat = sm.get(hIdx);
                        if (hpStat != null) sm.addStatValue(hIdx, -(hpStat.getMax() * hpCostPct));
                    }
                } catch (Exception ignored2) {}

                final float finalDmg = skillDmg;
                final Ref<EntityStore> skillTarget = targeted;
                final Ref<EntityStore> skillCaster = entityRef;
                final UUID fUuid = uuid;
                com.hypixel.hytale.server.core.universe.world.World dmgWorld = null;
                try { java.util.UUID wId = playerRef.getWorldUuid(); if (wId != null) dmgWorld = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(wId); } catch (Exception ignored2) {}
                if (dmgWorld != null) {
                    final com.hypixel.hytale.server.core.universe.world.World fw = dmgWorld;
                    java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "vaudou-perfide"); t.setDaemon(true); return t; })
                        .schedule(() -> fw.execute(() -> {
                            try {
                                vaudouState.setPendingSkillDmg(fUuid, finalDmg);
                                com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(
                                    skillTarget, fw.getEntityStore().getStore(),
                                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                                        new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(skillCaster),
                                        com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, finalDmg));
                            } catch (Exception ignored3) { vaudouState.consumePendingSkillDmg(fUuid); }
                        }), 50, java.util.concurrent.TimeUnit.MILLISECONDS);
                }

                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Staff", "SwingRight", true, store);
                ClassSkillSounds.playSkillSound("SFX_Daggers_T1_Pounce", playerRef, targetPos, null);
            }
        } catch (Exception ignored) {}

        ClassSkillMana.consume(playerRef, manaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.vaudou.AttaquePerfideSkill.SKILL_ID);
        notifySkill(uuid, "Attaque Perfide");
        return true;
    }

    public boolean tryCastTotemVulnerabilite(@Nonnull UUID uuid,
                                             @Nonnull PlayerRef playerRef,
                                             @Nonnull Ref<EntityStore> entityRef,
                                             @Nonnull Store<EntityStore> store,
                                             @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isVaudou(acc)) return false;
        if (!isHoldingStaff(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.vaudou.TotemVulnerabiliteSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.vaudou.TotemVulnerabiliteSkill.SKILL_ID,
                fr.varyon.vrpg.classes.vaudou.TotemVulnerabiliteSkill.cooldownMsForRank(rank))) return false;
        float manaCost = fr.varyon.vrpg.classes.vaudou.TotemVulnerabiliteSkill.manaCostForRank(rank);
        if (!ClassSkillMana.hasEnough(playerRef, manaCost)) return false;

        int ancrageRank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.vaudou.VaudouPassifs.ANCRAGE_NODE);
        float durBonus = ancrageRank > 0 ? fr.varyon.vrpg.classes.vaudou.VaudouPassifs.ancrageTotemBonusForRank(ancrageRank) : 0f;
        long durationMs = (long)(fr.varyon.vrpg.classes.vaudou.TotemVulnerabiliteSkill.baseDurationMsForRank(rank) * (1f + durBonus));

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            if (tc != null && hr != null) {
                org.joml.Vector3d pos = tc.getPosition();
                org.joml.Vector3d dir = hr.getDirection();
                org.joml.Vector3d spawnPos = new org.joml.Vector3d(
                    pos.x + dir.x * 1.2,
                    pos.y + 1.4,
                    pos.z + dir.z * 1.2
                );
                float mag = fr.varyon.vrpg.classes.vaudou.TotemVulnerabiliteSkill.damageTakenBonusForRank(rank);
                fr.varyon.vrpg.classes.vaudou.VaudouTotemHelper.deployVulnerabilityTotem(pos, durationMs, mag);
                final long fDurVuln = durationMs;
                final org.joml.Vector3d fSpawnPosVuln = spawnPos, fDirVuln = dir;
                final com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> fRefVuln = entityRef;
                final com.hypixel.hytale.server.core.universe.PlayerRef fPrVuln = playerRef;
                final com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> fStoreVuln = store;
                final com.hypixel.hytale.component.CommandBuffer<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> fCbVuln = commandBuffer;
                fr.varyon.vrpg.classes.vaudou.VaudouTotemHelper.patchAndRun("Vulnerability_Totem", fDurVuln,
                    () -> spawnMagicProjectile("Vrpg_Totem_Vulnerabilite_Throw", fSpawnPosVuln, fDirVuln, fRefVuln, fPrVuln, fStoreVuln, fCbVuln, 0f, 0f, null, 0f));
                ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", playerRef, pos, null);
            }
        } catch (Exception ignored) {}

        ClassSkillMana.consume(playerRef, manaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.vaudou.TotemVulnerabiliteSkill.SKILL_ID);
        notifySkill(uuid, "Totem de Vulnérabilité");
        return true;
    }

    public boolean tryCastExtractionAme(@Nonnull UUID uuid,
                                        @Nonnull PlayerRef playerRef,
                                        @Nonnull Ref<EntityStore> entityRef,
                                        @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isVaudou(acc)) return false;
        if (!isHoldingStaff(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.vaudou.ExtractionAmeSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.vaudou.ExtractionAmeSkill.SKILL_ID,
                fr.varyon.vrpg.classes.vaudou.ExtractionAmeSkill.cooldownMsForRank(rank))) return false;
        float manaCost = fr.varyon.vrpg.classes.vaudou.ExtractionAmeSkill.manaCostForRank(rank);
        if (!ClassSkillMana.hasEnough(playerRef, manaCost)) return false;

        Ref<EntityStore> targeted = findTargetedNpcRef(playerRef, entityRef, store, 4.0);
        if (targeted == null) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                float skillDmg = 50f * fr.varyon.vrpg.classes.vaudou.ExtractionAmeSkill.damagePctForRank(rank);

                int rituelRank = acc.getTalentRank(PlayerClass.MAGE, fr.varyon.vrpg.classes.vaudou.VaudouPassifs.RITUEL_NODE);
                if (rituelRank > 0 && vaudouState.isNpcCursed(targeted.getIndex())) {
                    skillDmg *= (1f + fr.varyon.vrpg.classes.vaudou.VaudouPassifs.rituelBonusForRank(rituelRank));
                }

                final float healAmt = skillDmg * fr.varyon.vrpg.classes.vaudou.ExtractionAmeSkill.lifestealPctForRank(rank);
                final Ref<EntityStore> skillTarget = targeted;
                final Ref<EntityStore> skillCaster = entityRef;
                final UUID fUuid = uuid;
                com.hypixel.hytale.server.core.universe.world.World dmgWorld = null;
                try { java.util.UUID wId = playerRef.getWorldUuid(); if (wId != null) dmgWorld = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(wId); } catch (Exception ignored2) {}
                if (dmgWorld != null) {
                    final com.hypixel.hytale.server.core.universe.world.World fw = dmgWorld;
                    final float finalDmg = skillDmg;
                    java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "vaudou-ame"); t.setDaemon(true); return t; })
                        .schedule(() -> fw.execute(() -> {
                            try {
                                vaudouState.setPendingSkillDmg(fUuid, finalDmg);
                                Store<EntityStore> liveStore = fw.getEntityStore().getStore();
                                com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(
                                    skillTarget, liveStore,
                                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                                        new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(skillCaster),
                                        com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, finalDmg));
                                com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap casterSm =
                                    liveStore.getComponent(skillCaster, com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.getComponentType());
                                if (casterSm != null) {
                                    int hIdx = com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes.getHealth();
                                    casterSm.addStatValue(hIdx, healAmt);
                                }
                            } catch (Exception ignored3) { vaudouState.consumePendingSkillDmg(fUuid); }
                        }), 50, java.util.concurrent.TimeUnit.MILLISECONDS);
                }

                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Staff", "SwingRight", true, store);
                ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", playerRef, tc.getPosition(), null);
            }
        } catch (Exception ignored) {}

        ClassSkillMana.consume(playerRef, manaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.vaudou.ExtractionAmeSkill.SKILL_ID);
        notifySkill(uuid, "Extraction d'Âme");
        return true;
    }

    // ========== RÔDEUR ==========

    private boolean isRodeur(@Nonnull ClassAccount acc) {
        return acc.getActiveClass() == PlayerClass.TIREUR
            && acc.getActiveSpec(PlayerClass.TIREUR) == PlayerSpecialization.RODEUR;
    }

    private boolean isArbaletrier(@Nonnull ClassAccount acc) {
        return acc.getActiveClass() == PlayerClass.TIREUR
            && acc.getActiveSpec(PlayerClass.TIREUR) == PlayerSpecialization.ARBALETRIER;
    }

    private boolean isHoldingDistance(@Nonnull PlayerRef playerRef) {
        return fr.varyon.vrpg.classes.WeaponCategory.fromItemId(getHeldItemId(playerRef))
            == fr.varyon.vrpg.classes.WeaponCategory.DISTANCE;
    }

    public boolean tryCastReculStrategique(@Nonnull UUID uuid,
                                           @Nonnull PlayerRef playerRef,
                                           @Nonnull Ref<EntityStore> entityRef,
                                           @Nonnull Store<EntityStore> store,
                                           @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRodeur(acc)) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.TIREUR, fr.varyon.vrpg.classes.rodeur.ReculStrategiqueSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.rodeur.ReculStrategiqueSkill.SKILL_ID,
                fr.varyon.vrpg.classes.rodeur.ReculStrategiqueSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.rodeur.ReculStrategiqueSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            if (tc != null && hr != null) {
                org.joml.Vector3d dir = hr.getDirection();
                double dx = dir.x, dz = dir.z;
                double len = Math.sqrt(dx*dx + dz*dz);
                if (len > 1e-6) { dx /= len; dz /= len; }

                double dist = fr.varyon.vrpg.classes.rodeur.ReculStrategiqueSkill.dashDistanceForRank(rank);
                org.joml.Vector3d newPos = new org.joml.Vector3d(
                    tc.getPosition().x - dx * dist,
                    tc.getPosition().y,
                    tc.getPosition().z - dz * dist);
                com.hypixel.hytale.math.vector.Rotation3fc curRot = hr.getRotation();
                com.hypixel.hytale.math.vector.Rotation3f keepRot = new com.hypixel.hytale.math.vector.Rotation3f(
                    curRot.pitch(), curRot.yaw(), curRot.roll());
                com.hypixel.hytale.server.core.modules.entity.teleport.Teleport tele =
                    com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.createForPlayer(newPos, keepRot);
                tele.withoutVelocityReset();
                (commandBuffer != null ? commandBuffer : store).addComponent(entityRef,
                    com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.getComponentType(), tele);

                com.hypixel.hytale.server.core.modules.physics.component.Velocity vel =
                    commandBuffer != null
                        ? commandBuffer.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType())
                        : store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType());
                if (vel != null) {
                    org.joml.Vector3d dashVel = new org.joml.Vector3d(-dx * 30.0, 0.2, -dz * 30.0);
                    vel.setClient(dashVel);
                    vel.getInstructions().clear();
                    vel.addInstruction(dashVel, null, com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                }
                ClassSkillSounds.playSkillSound("SFX_Vrpg_PasDeLOmbre_Dash", playerRef, tc.getPosition(), commandBuffer);
            }
        } catch (Exception ignored) {}

        long speedMs = fr.varyon.vrpg.classes.rodeur.ReculStrategiqueSkill.speedDurationMsForRank(rank);
        float speedBonus = fr.varyon.vrpg.classes.rodeur.ReculStrategiqueSkill.speedBonusForRank(rank);
        rodeurState.startSpeedBoost(uuid, speedBonus, speedMs);

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.rodeur.ReculStrategiqueSkill.SKILL_ID);
        notifySkill(uuid, "Recul Stratégique");
        return true;
    }

    public boolean tryCastPluieDesFleches(@Nonnull UUID uuid,
                                          @Nonnull PlayerRef playerRef,
                                          @Nonnull Ref<EntityStore> entityRef,
                                          @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRodeur(acc)) return false;
        if (!isHoldingDistance(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.TIREUR, fr.varyon.vrpg.classes.rodeur.PluieDesFlechesSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.rodeur.PluieDesFlechesSkill.SKILL_ID,
                fr.varyon.vrpg.classes.rodeur.PluieDesFlechesSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.rodeur.PluieDesFlechesSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) {
                float weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
                float dmgPerArrow = weaponDmg * fr.varyon.vrpg.classes.rodeur.PluieDesFlechesSkill.damagePctForRank(rank);
                if (dmgPerArrow < 1f) dmgPerArrow = 1f;
                int arrows = fr.varyon.vrpg.classes.rodeur.PluieDesFlechesSkill.arrowCountForRank(rank);
                long durationMs = fr.varyon.vrpg.classes.rodeur.PluieDesFlechesSkill.durationMs();
                double radius = fr.varyon.vrpg.classes.rodeur.PluieDesFlechesSkill.radius();
                com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hrPluie =
                    store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
                org.joml.Vector3d dirPluie = hrPluie != null ? hrPluie.getDirection() : new org.joml.Vector3d(0, 0, 1);
                org.joml.Vector3d pluieBase = tc.getPosition();
                org.joml.Vector3d center;
                {
                    Ref<EntityStore> pluieTarget = findTargetedNpcRef(playerRef, entityRef, store, 20.0);
                    org.joml.Vector3d aimed = null;
                    if (pluieTarget != null) {
                        TransformComponent tcPluieTarget = store.getComponent(pluieTarget, TransformComponent.getComponentType());
                        if (tcPluieTarget != null) {
                            org.joml.Vector3d tp = tcPluieTarget.getPosition();
                            org.joml.Vector3d toTarget = new org.joml.Vector3d(tp).sub(pluieBase).normalize();
                            double cosAngle = toTarget.dot(dirPluie);
                            if (cosAngle > Math.cos(Math.toRadians(15.0))) aimed = new org.joml.Vector3d(tp);
                        }
                    }
                    if (aimed == null) {
                        double eyeY = pluieBase.y + 1.6;
                        double t;
                        if (dirPluie.y < -1e-4) {
                            t = Math.min(20.0 / Math.max(1e-6, Math.sqrt(dirPluie.x*dirPluie.x + dirPluie.z*dirPluie.z)),
                                         -eyeY / dirPluie.y);
                        } else {
                            t = 20.0 / Math.max(1e-6, Math.sqrt(dirPluie.x*dirPluie.x + dirPluie.z*dirPluie.z));
                        }
                        aimed = new org.joml.Vector3d(
                            pluieBase.x + dirPluie.x * t,
                            pluieBase.y,
                            pluieBase.z + dirPluie.z * t);
                    }
                    center = aimed;
                }

                com.hypixel.hytale.server.core.universe.world.World world = null;
                try {
                    java.util.UUID wUuid = playerRef.getWorldUuid();
                    if (wUuid != null) world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(wUuid);
                } catch (Exception ignored2) {}
                ClassSkillSounds.playSkillSound("SFX_Bow_T2_Shoot", playerRef, tc.getPosition(), null);
                if (world != null) {
                    final com.hypixel.hytale.server.core.universe.world.World fw = world;
                    final Ref<EntityStore> fEntityRef = entityRef;
                    final float fDmg = dmgPerArrow;
                    final int fArrows = arrows;
                    final double fRadius = radius;
                    final org.joml.Vector3d fCenter = center;
                    long tickInterval = durationMs / Math.max(1, fArrows - 1);
                    java.util.concurrent.ScheduledExecutorService exec =
                        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                            Thread t = new Thread(r, "pluie-fleches"); t.setDaemon(true); return t;
                        });
                    int[] ticks = {0};
                    exec.scheduleAtFixedRate(() -> {
                        try {
                            if (ticks[0] >= fArrows) { exec.shutdown(); return; }
                            ticks[0]++;
                            double rndX = fCenter.x + (Math.random() * 2 - 1) * 1.0;
                            double rndZ = fCenter.z + (Math.random() * 2 - 1) * 1.0;
                            org.joml.Vector3d spawnAbove = new org.joml.Vector3d(rndX, fCenter.y + 10.0, rndZ);
                            org.joml.Vector3d landPos    = new org.joml.Vector3d(rndX, fCenter.y,        rndZ);
                            spawnProjectileToward("Vrpg_Fleche_Pluie", spawnAbove, landPos, fEntityRef, playerRef, null);
                            fw.execute(() -> {
                                try {
                                    Store<EntityStore> liveStore = fw.getEntityStore().getStore();
                                    damageNearby(landPos, 1.5f, fEntityRef, liveStore, fDmg,
                                        com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL);
                                    ClassSkillSounds.playSkillSound("SFX_Arrow_Fire_Miss", playerRef, landPos, null);
                                } catch (Exception ignored3) {}
                            });
                        } catch (Exception ignored4) { exec.shutdown(); }
                    }, 0L, tickInterval, java.util.concurrent.TimeUnit.MILLISECONDS);
                }
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.rodeur.PluieDesFlechesSkill.SKILL_ID);
        notifySkill(uuid, "Pluie de Flèches");
        return true;
    }

    public boolean tryCastMarqueDuChasseur(@Nonnull UUID uuid,
                                           @Nonnull PlayerRef playerRef,
                                           @Nonnull Ref<EntityStore> entityRef,
                                           @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRodeur(acc)) return false;
        if (!isHoldingDistance(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.TIREUR, fr.varyon.vrpg.classes.rodeur.MarqueDuChasseurSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.rodeur.MarqueDuChasseurSkill.SKILL_ID,
                fr.varyon.vrpg.classes.rodeur.MarqueDuChasseurSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.rodeur.MarqueDuChasseurSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        long durationMs = fr.varyon.vrpg.classes.rodeur.MarqueDuChasseurSkill.durationMsForRank(rank);
        int maxConsumes = fr.varyon.vrpg.classes.rodeur.MarqueDuChasseurSkill.maxConsumesForRank(rank);
        try {
            TransformComponent tcCaster = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tcCaster == null) return false;
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hrMarque =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            org.joml.Vector3d dirMarque = hrMarque != null ? hrMarque.getDirection() : new org.joml.Vector3d(0, 0, 1);
            org.joml.Vector3d casterPos = tcCaster.getPosition();
            org.joml.Vector3d aimFar = new org.joml.Vector3d(
                casterPos.x + dirMarque.x * 20.0,
                casterPos.y + dirMarque.y * 20.0,
                casterPos.z + dirMarque.z * 20.0);
            rodeurState.setPendingMarquage(uuid, rank, durationMs, maxConsumes);
            spawnProjectileToward("Vrpg_Fleche_Marquage", casterPos, aimFar, entityRef, playerRef, null);
            ClassSkillSounds.playSkillSound("SFX_Bow_T2_Shoot", playerRef, casterPos, null);
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.rodeur.MarqueDuChasseurSkill.SKILL_ID);
        notifySkill(uuid, "Marque du Chasseur");
        return true;
    }

    public boolean tryCastFlecheDeRecul(@Nonnull UUID uuid,
                                        @Nonnull PlayerRef playerRef,
                                        @Nonnull Ref<EntityStore> entityRef,
                                        @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRodeur(acc)) return false;
        if (!isHoldingDistance(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.TIREUR, fr.varyon.vrpg.classes.rodeur.FlecheDeReculsSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.rodeur.FlecheDeReculsSkill.SKILL_ID,
                fr.varyon.vrpg.classes.rodeur.FlecheDeReculsSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.rodeur.FlecheDeReculsSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            float weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
            float dmg = weaponDmg * fr.varyon.vrpg.classes.rodeur.FlecheDeReculsSkill.damagePctForRank(rank);
            if (dmg < 1f) dmg = 1f;
            double kb = fr.varyon.vrpg.classes.rodeur.FlecheDeReculsSkill.knockbackForRank(rank);

            TransformComponent tcCaster = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tcCaster == null) return false;

            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hrRecul =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            org.joml.Vector3d dirRecul = hrRecul != null ? hrRecul.getDirection() : new org.joml.Vector3d(0, 0, 1);
            org.joml.Vector3d casterPos = tcCaster.getPosition();
            org.joml.Vector3d aimFar = new org.joml.Vector3d(
                casterPos.x + dirRecul.x * 20.0,
                casterPos.y + dirRecul.y * 20.0,
                casterPos.z + dirRecul.z * 20.0);
            rodeurState.setPendingArrow(uuid, fr.varyon.vrpg.classes.rodeur.RodeurState.ARROW_TYPE_RECUL, dmg, kb, 0L);
            spawnProjectileToward("Vrpg_Fleche_Recul", casterPos, aimFar, entityRef, playerRef, null);
            ClassSkillSounds.playSkillSound("SFX_Bow_T2_Shoot", playerRef, casterPos, null);
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.rodeur.FlecheDeReculsSkill.SKILL_ID);
        notifySkill(uuid, "Flèche de Recul");
        return true;
    }

    public boolean tryCastFlecheEntravante(@Nonnull UUID uuid,
                                           @Nonnull PlayerRef playerRef,
                                           @Nonnull Ref<EntityStore> entityRef,
                                           @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRodeur(acc)) return false;
        if (!isHoldingDistance(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.TIREUR, fr.varyon.vrpg.classes.rodeur.FlecheEntravantSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.rodeur.FlecheEntravantSkill.SKILL_ID,
                fr.varyon.vrpg.classes.rodeur.FlecheEntravantSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.rodeur.FlecheEntravantSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            float weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
            float dmg = weaponDmg * fr.varyon.vrpg.classes.rodeur.FlecheEntravantSkill.damagePctForRank(rank);
            if (dmg < 1f) dmg = 1f;
            long rootMs = fr.varyon.vrpg.classes.rodeur.FlecheEntravantSkill.rootMsForRank(rank);

            TransformComponent tcCaster = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tcCaster == null) return false;
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hrEnt =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            org.joml.Vector3d dirEnt = hrEnt != null ? hrEnt.getDirection() : new org.joml.Vector3d(0, 0, 1);
            org.joml.Vector3d casterPosEnt = tcCaster.getPosition();
            org.joml.Vector3d aimFarEnt = new org.joml.Vector3d(
                casterPosEnt.x + dirEnt.x * 20.0,
                casterPosEnt.y + dirEnt.y * 20.0,
                casterPosEnt.z + dirEnt.z * 20.0);
            rodeurState.setPendingArrow(uuid, fr.varyon.vrpg.classes.rodeur.RodeurState.ARROW_TYPE_ENTRAVANTE, dmg, 0.0, rootMs);
            spawnProjectileToward("Vrpg_Fleche_Recul", casterPosEnt, aimFarEnt, entityRef, playerRef, null);
            ClassSkillSounds.playSkillSound("SFX_Bow_T2_Shoot", playerRef, casterPosEnt, null);
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.rodeur.FlecheEntravantSkill.SKILL_ID);
        notifySkill(uuid, "Flèche Entravante");
        return true;
    }

    public boolean tryCastRafale(@Nonnull UUID uuid,
                                 @Nonnull PlayerRef playerRef,
                                 @Nonnull Ref<EntityStore> entityRef,
                                 @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isRodeur(acc)) return false;
        if (!isHoldingDistance(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.TIREUR, fr.varyon.vrpg.classes.rodeur.RafaleSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.rodeur.RafaleSkill.SKILL_ID,
                fr.varyon.vrpg.classes.rodeur.RafaleSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.rodeur.RafaleSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            float weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
            float dmgPerArrow = weaponDmg * fr.varyon.vrpg.classes.rodeur.RafaleSkill.damagePctForRank(rank);
            if (dmgPerArrow < 1f) dmgPerArrow = 1f;
            int arrowCount = fr.varyon.vrpg.classes.rodeur.RafaleSkill.arrowCountForRank(rank);
            long delayMs = fr.varyon.vrpg.classes.rodeur.RafaleSkill.delayMsBetween(rank);

            TransformComponent tcRafale = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tcRafale == null) return false;
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hrRafale =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            org.joml.Vector3d dirRafale = hrRafale != null ? hrRafale.getDirection() : new org.joml.Vector3d(0, 0, 1);
            org.joml.Vector3d casterPosRafale = tcRafale.getPosition();
            org.joml.Vector3d aimFarRafale = new org.joml.Vector3d(
                casterPosRafale.x + dirRafale.x * 25.0,
                casterPosRafale.y + dirRafale.y * 25.0,
                casterPosRafale.z + dirRafale.z * 25.0);

            ClassSkillSounds.playSkillSound("SFX_Bow_T2_Shoot", playerRef, casterPosRafale, null);

            final Ref<EntityStore> fEntityRef = entityRef;
            final float fDmg = dmgPerArrow;
            final int fCount = arrowCount;
            java.util.concurrent.ScheduledExecutorService rafaleExec =
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "rafale-arrow"); t.setDaemon(true); return t;
                });
            int[] shots = {0};
            rafaleExec.scheduleAtFixedRate(() -> {
                try {
                    if (shots[0] >= fCount) { rafaleExec.shutdown(); return; }
                    shots[0]++;
                    rodeurState.setPendingArrow(uuid, fr.varyon.vrpg.classes.rodeur.RodeurState.ARROW_TYPE_RAFALE, fDmg, 0.0, 0L);
                    spawnProjectileToward("Vrpg_Fleche_Recul", casterPosRafale, aimFarRafale, fEntityRef, playerRef, null);
                } catch (Exception ignored4) { rafaleExec.shutdown(); }
            }, 0L, delayMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.rodeur.RafaleSkill.SKILL_ID);
        notifySkill(uuid, "Rafale");
        return true;
    }

    // ========== ARBALÉTRIER ==========

    public boolean tryCastReculTactique(@Nonnull UUID uuid,
                                        @Nonnull PlayerRef playerRef,
                                        @Nonnull Ref<EntityStore> entityRef,
                                        @Nonnull Store<EntityStore> store,
                                        @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isArbaletrier(acc)) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.TIREUR, fr.varyon.vrpg.classes.arbaletrier.ReculTactiqueSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.arbaletrier.ReculTactiqueSkill.SKILL_ID,
                fr.varyon.vrpg.classes.arbaletrier.ReculTactiqueSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.arbaletrier.ReculTactiqueSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            if (tc != null && hr != null) {
                org.joml.Vector3d dir = hr.getDirection();
                double dx = dir.x, dz = dir.z;
                double len = Math.sqrt(dx*dx + dz*dz);
                if (len > 1e-6) { dx /= len; dz /= len; }

                double dist = fr.varyon.vrpg.classes.arbaletrier.ReculTactiqueSkill.dashDistanceForRank(rank);
                org.joml.Vector3d newPos = new org.joml.Vector3d(
                    tc.getPosition().x - dx * dist,
                    tc.getPosition().y,
                    tc.getPosition().z - dz * dist);
                com.hypixel.hytale.math.vector.Rotation3fc curRot = hr.getRotation();
                com.hypixel.hytale.math.vector.Rotation3f keepRot = new com.hypixel.hytale.math.vector.Rotation3f(
                    curRot.pitch(), curRot.yaw(), curRot.roll());
                com.hypixel.hytale.server.core.modules.entity.teleport.Teleport tele =
                    com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.createForPlayer(newPos, keepRot);
                tele.withoutVelocityReset();
                (commandBuffer != null ? commandBuffer : store).addComponent(entityRef,
                    com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.getComponentType(), tele);

                com.hypixel.hytale.server.core.modules.physics.component.Velocity vel =
                    commandBuffer != null
                        ? commandBuffer.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType())
                        : store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType());
                if (vel != null) {
                    org.joml.Vector3d dashVel = new org.joml.Vector3d(-dx * 30.0, 0.2, -dz * 30.0);
                    vel.setClient(dashVel);
                    vel.getInstructions().clear();
                    vel.addInstruction(dashVel, null, com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                }
                ClassSkillSounds.playSkillSound("SFX_Vrpg_PasDeLOmbre_Dash", playerRef, tc.getPosition(), commandBuffer);
            }
        } catch (Exception ignored) {}

        float dmgBonus = fr.varyon.vrpg.classes.arbaletrier.ReculTactiqueSkill.damageBonusForRank(rank);
        arbaState.armReculBonus(uuid, dmgBonus, fr.varyon.vrpg.classes.arbaletrier.ReculTactiqueSkill.armedWindowMs());

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.arbaletrier.ReculTactiqueSkill.SKILL_ID);
        notifySkill(uuid, "Recul Tactique");
        return true;
    }

    public boolean tryCastCarreauLourd(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef,
                                       @Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isArbaletrier(acc)) return false;
        if (!isHoldingDistance(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.TIREUR, fr.varyon.vrpg.classes.arbaletrier.CarreauLourdSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.arbaletrier.CarreauLourdSkill.SKILL_ID,
                fr.varyon.vrpg.classes.arbaletrier.CarreauLourdSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.arbaletrier.CarreauLourdSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        arbaState.armCarreauLourd(uuid, rank, fr.varyon.vrpg.classes.arbaletrier.CarreauLourdSkill.armedWindowMs());
        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            org.joml.Vector3d pos = tc != null ? tc.getPosition() : new org.joml.Vector3d();
            ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", playerRef, pos, null);
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.arbaletrier.CarreauLourdSkill.SKILL_ID);
        notifySkill(uuid, "Carreau Lourd");
        return true;
    }

    public boolean tryCastCarreauExplosif(@Nonnull UUID uuid,
                                          @Nonnull PlayerRef playerRef,
                                          @Nonnull Ref<EntityStore> entityRef,
                                          @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isArbaletrier(acc)) return false;
        if (!isHoldingDistance(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.TIREUR, fr.varyon.vrpg.classes.arbaletrier.CarreauExplosifSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.arbaletrier.CarreauExplosifSkill.SKILL_ID,
                fr.varyon.vrpg.classes.arbaletrier.CarreauExplosifSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.arbaletrier.CarreauExplosifSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (tc == null) return false;
        com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hrExplosif =
            store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
        org.joml.Vector3d dirExplosif = hrExplosif != null ? hrExplosif.getDirection() : new org.joml.Vector3d(0, 0, 1);
        org.joml.Vector3d casterPos = tc.getPosition();
        org.joml.Vector3d aimFar = new org.joml.Vector3d(casterPos.x + dirExplosif.x * 25, casterPos.y + dirExplosif.y * 25, casterPos.z + dirExplosif.z * 25);

        float weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
        float dmg = weaponDmg * fr.varyon.vrpg.classes.arbaletrier.CarreauExplosifSkill.damagePctForRank(rank);
        if (dmg < 1f) dmg = 1f;

        arbaState.setPendingCarreau(uuid, fr.varyon.vrpg.classes.arbaletrier.ArbaietrierState.CARREAU_TYPE_EXPLOSIF, dmg, rank, false);
        if (carreauExplosifGroundSystem != null) {
            spawnProjectileTowardTracked("Vrpg_Carreau_Explosif", casterPos, aimFar, entityRef, playerRef, uuid, carreauExplosifGroundSystem);
        } else {
            spawnProjectileToward("Vrpg_Carreau_Explosif", casterPos, aimFar, entityRef, playerRef, null);
            LOG.atWarning().log("[CarreauExplosif] groundSystem null, no ground explosion");
        }
        ClassSkillSounds.playSkillSound("SFX_Bow_T2_Shoot", playerRef, casterPos, null);

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.arbaletrier.CarreauExplosifSkill.SKILL_ID);
        notifySkill(uuid, "Carreau Explosif");
        return true;
    }

    public boolean tryCastCarreauTranspercant(@Nonnull UUID uuid,
                                              @Nonnull PlayerRef playerRef,
                                              @Nonnull Ref<EntityStore> entityRef,
                                              @Nonnull Store<EntityStore> store,
                                              @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isArbaletrier(acc)) return false;
        if (!isHoldingDistance(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.TIREUR, fr.varyon.vrpg.classes.arbaletrier.CarreauTranspercantSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.arbaletrier.CarreauTranspercantSkill.SKILL_ID,
                fr.varyon.vrpg.classes.arbaletrier.CarreauTranspercantSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.arbaletrier.CarreauTranspercantSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (tc == null) return false;
        com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hrTransp =
            store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
        org.joml.Vector3d dirTransp = hrTransp != null ? hrTransp.getDirection() : new org.joml.Vector3d(0, 0, 1);
        org.joml.Vector3d casterPos = tc.getPosition();

        float weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
        float dmg = weaponDmg * fr.varyon.vrpg.classes.arbaletrier.CarreauTranspercantSkill.damagePctForRank(rank);
        if (dmg < 1f) dmg = 1f;

        org.joml.Vector3d aimFar = new org.joml.Vector3d(
            casterPos.x + dirTransp.x * 25, casterPos.y + dirTransp.y * 25, casterPos.z + dirTransp.z * 25);

        arbaState.setPendingCarreau(uuid, fr.varyon.vrpg.classes.arbaletrier.ArbaietrierState.CARREAU_TYPE_TRANSPERCANT, dmg, rank, false);
        spawnProjectileToward("Vrpg_Carreau_Transpercant", casterPos, aimFar, entityRef, playerRef, commandBuffer);
        ClassSkillSounds.playSkillSound("SFX_Bow_T2_Shoot", playerRef, tc.getPosition(), null);

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.arbaletrier.CarreauTranspercantSkill.SKILL_ID);
        notifySkill(uuid, "Carreau Transpercant");
        return true;
    }

    public boolean tryCastCoupDeBotte(@Nonnull UUID uuid,
                                      @Nonnull PlayerRef playerRef,
                                      @Nonnull Ref<EntityStore> entityRef,
                                      @Nonnull Store<EntityStore> store,
                                      @Nonnull com.hypixel.hytale.component.CommandBuffer<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isArbaletrier(acc)) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.TIREUR, fr.varyon.vrpg.classes.arbaletrier.CoupDeBotteSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.arbaletrier.CoupDeBotteSkill.SKILL_ID,
                fr.varyon.vrpg.classes.arbaletrier.CoupDeBotteSkill.cooldownMsForRank(rank))) return false;
        float staminaCost = fr.varyon.vrpg.classes.arbaletrier.CoupDeBotteSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        Ref<EntityStore> targeted = findTargetedNpcRef(playerRef, entityRef, store, 3.0);
        LOG.atInfo().log("[CoupDeBotte] targeted=" + (targeted != null ? targeted.getIndex() : "null") + " rank=" + rank);
        if (targeted == null) return false;

        try {
            float weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
            float dmg = weaponDmg * fr.varyon.vrpg.classes.arbaletrier.CoupDeBotteSkill.damagePctForRank(rank);
            if (dmg < 1f) dmg = 1f;
            double kb = fr.varyon.vrpg.classes.arbaletrier.CoupDeBotteSkill.knockbackForRank(rank);
            LOG.atInfo().log("[CoupDeBotte] weaponDmg=" + weaponDmg + " dmg=" + dmg + " kb=" + kb);

            TransformComponent tcCaster = store.getComponent(entityRef, TransformComponent.getComponentType());
            TransformComponent tcTarget = store.getComponent(targeted, TransformComponent.getComponentType());

            com.hypixel.hytale.server.core.modules.entity.damage.Damage dmgEvent =
                new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                    com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, dmg);

            if (tcCaster != null && tcTarget != null && kb > 0) {
                org.joml.Vector3d diff = new org.joml.Vector3d(tcTarget.getPosition()).sub(tcCaster.getPosition());
                diff.y = 0;
                double len = diff.length();
                if (len > 1e-6) diff.mul(1.0 / len);
                LOG.atInfo().log("[CoupDeBotte] pending KB vx=" + (diff.x * kb) + " vz=" + (diff.z * kb));
                arbaState.setPendingCoupDeBotteKb(uuid, diff.x * kb, diff.z * kb);
            }

            com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(targeted, store, dmgEvent);

            if (tcTarget != null)
                ClassSkillSounds.playSkillSound("SFX_Vrpg_Punch", playerRef, tcTarget.getPosition(), null);
        } catch (Exception e) {
            LOG.atWarning().log("[CoupDeBotte] EXCEPTION: " + e.getMessage());
        }

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.arbaletrier.CoupDeBotteSkill.SKILL_ID);
        notifySkill(uuid, "Coup de Botte");
        return true;
    }

    public boolean tryCastMiseEnJou(@Nonnull UUID uuid, @Nonnull PlayerRef playerRef) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isArbaletrier(acc)) return false;
        if (!isHoldingDistance(playerRef)) { notifyNoWeapon(playerRef); return false; }
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.TIREUR, fr.varyon.vrpg.classes.arbaletrier.MiseEnJouSkill.TALENT_NODE_ID);
        if (rank <= 0 && !bypass) return false;
        if (rank <= 0) rank = 1;
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.arbaletrier.MiseEnJouSkill.SKILL_ID,
                fr.varyon.vrpg.classes.arbaletrier.MiseEnJouSkill.cooldownMsForRank(rank))) return false;
        if (arbaState.isMiseEnJouVisant(uuid)) return false;
        float staminaCost = fr.varyon.vrpg.classes.arbaletrier.MiseEnJouSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        long viseeMs = fr.varyon.vrpg.classes.arbaletrier.MiseEnJouSkill.viseeDurationMs();
        arbaState.startMiseEnJouVisee(uuid, viseeMs);

        try {
            ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", playerRef, new org.joml.Vector3d(), null);
        } catch (Exception ignored) {}

        final int fRank = rank;
        java.util.concurrent.ScheduledExecutorService mejExec = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        mejExec.schedule(() -> {
            try {
                arbaState.clearMiseEnJouVisee(uuid);
                com.hypixel.hytale.server.core.universe.PlayerRef liveRef =
                    com.hypixel.hytale.server.core.universe.Universe.get().getPlayer(uuid);
                if (liveRef == null) return;
                Ref<EntityStore> liveEntityRef = liveRef.getReference();
                if (liveEntityRef == null || !liveEntityRef.isValid()) return;
                java.util.UUID wUuidMej = liveRef.getWorldUuid();
                if (wUuidMej == null) return;
                com.hypixel.hytale.server.core.universe.world.World world =
                    com.hypixel.hytale.server.core.universe.Universe.get().getWorld(wUuidMej);
                if (world == null) return;

                float weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(liveRef);
                float dmg = weaponDmg * (1f + fr.varyon.vrpg.classes.arbaletrier.MiseEnJouSkill.damageBonusForRank(fRank));
                if (dmg < 1f) dmg = 1f;
                final float fDmg = dmg;

                arbaState.setPendingCarreau(uuid,
                    fr.varyon.vrpg.classes.arbaletrier.ArbaietrierState.CARREAU_TYPE_LOURD, fDmg, fRank, true);

                world.execute(() -> {
                    try {
                        com.hypixel.hytale.server.core.universe.PlayerRef wr =
                            com.hypixel.hytale.server.core.universe.Universe.get().getPlayer(uuid);
                        if (wr == null) return;
                        Ref<EntityStore> wRef = wr.getReference();
                        if (wRef == null || !wRef.isValid()) return;
                        Store<EntityStore> wStore = wRef.getStore();
                        if (wStore == null) return;
                        TransformComponent tcMej = wStore.getComponent(wRef, TransformComponent.getComponentType());
                        if (tcMej == null) return;
                        com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hrMej =
                            wStore.getComponent(wRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
                        org.joml.Vector3d dirMej = hrMej != null ? hrMej.getDirection() : new org.joml.Vector3d(0, 0, 1);
                        org.joml.Vector3d posMej = tcMej.getPosition();
                        org.joml.Vector3d aimFarMej = new org.joml.Vector3d(
                            posMej.x + dirMej.x * 25, posMej.y + dirMej.y * 25, posMej.z + dirMej.z * 25);
                        spawnProjectileToward("Vrpg_Carreau_Lourd", posMej, aimFarMej, wRef, wr, null);
                        ClassSkillSounds.playSkillSound("SFX_Bow_T2_Shoot", wr, posMej, null);
                        try { com.hypixel.hytale.server.core.entity.AnimationUtils.playAnimation(wRef, com.hypixel.hytale.protocol.AnimationSlot.Action, "Bow", "ShootCharged", true, wStore); } catch (Exception ignored3) {}
                    } catch (Exception ignored2) {}
                });
            } catch (Exception ignored) {}
            finally { mejExec.shutdown(); }
        }, viseeMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.arbaletrier.MiseEnJouSkill.SKILL_ID);
        notifySkill(uuid, "Mise en Joue");
        return true;
    }

    // =====================================================================
    // Lancier
    // =====================================================================

    private boolean isLancier(@Nonnull ClassAccount acc) {
        return acc.getActiveClass() == PlayerClass.TIREUR
            && acc.getActiveSpec(PlayerClass.TIREUR) == PlayerSpecialization.LANCIER;
    }

    public boolean tryCastPercee(@Nonnull UUID uuid,
                                  @Nonnull PlayerRef playerRef,
                                  @Nonnull Ref<EntityStore> entityRef,
                                  @Nonnull Store<EntityStore> store,
                                  @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.TIREUR, fr.varyon.vrpg.classes.lancier.PerceeSkill.TALENT_NODE_ID);
        if (rank <= 0) rank = 1;
        long cdMs = fr.varyon.vrpg.classes.lancier.PerceeSkill.cooldownMsForRank(rank);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.lancier.PerceeSkill.SKILL_ID, cdMs)) return false;
        float staminaCost = fr.varyon.vrpg.classes.lancier.PerceeSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            if (tc != null && hr != null) {
                org.joml.Vector3d dir = hr.getDirection();
                double dx = dir.x, dz = dir.z;
                double len = Math.sqrt(dx*dx + dz*dz);
                if (len > 1e-6) { dx /= len; dz /= len; }

                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Spear", "DashBackward", true,
                    commandBuffer != null ? commandBuffer : store);
                ClassSkillSounds.playSkillSound("SFX_Daggers_T1_Pounce", playerRef, tc.getPosition(), commandBuffer);

                double dist = fr.varyon.vrpg.classes.lancier.PerceeSkill.dashDistanceForRank(rank);
                org.joml.Vector3d newPos = new org.joml.Vector3d(
                    tc.getPosition().x - dx * dist,
                    tc.getPosition().y,
                    tc.getPosition().z - dz * dist);
                com.hypixel.hytale.math.vector.Rotation3fc curRot = hr.getRotation();
                com.hypixel.hytale.math.vector.Rotation3f keepRot = new com.hypixel.hytale.math.vector.Rotation3f(
                    curRot.pitch(), curRot.yaw(), curRot.roll());
                com.hypixel.hytale.server.core.modules.entity.teleport.Teleport tele =
                    com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.createForPlayer(newPos, keepRot);
                tele.withoutVelocityReset();
                (commandBuffer != null ? commandBuffer : store).addComponent(entityRef,
                    com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.getComponentType(), tele);

                com.hypixel.hytale.server.core.modules.physics.component.Velocity vel =
                    commandBuffer != null
                        ? commandBuffer.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType())
                        : store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType());
                if (vel != null) {
                    org.joml.Vector3d dashVel = new org.joml.Vector3d(-dx * 30.0, 0.2, -dz * 30.0);
                    vel.setClient(dashVel);
                    vel.getInstructions().clear();
                    vel.addInstruction(dashVel, null, com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                }
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.lancier.PerceeSkill.SKILL_ID);
        notifySkill(uuid, "Percée");
        return true;
    }

    public boolean tryCastChargeHeroique(@Nonnull UUID uuid,
                                          @Nonnull PlayerRef playerRef,
                                          @Nonnull Ref<EntityStore> entityRef,
                                          @Nonnull Store<EntityStore> store,
                                          @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isLancier(acc)) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.TIREUR, fr.varyon.vrpg.classes.lancier.ChargeHeroiqueSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        long cdMs = fr.varyon.vrpg.classes.lancier.ChargeHeroiqueSkill.cooldownMsForRank(rank);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.lancier.ChargeHeroiqueSkill.SKILL_ID, cdMs)) return false;
        float staminaCost = fr.varyon.vrpg.classes.lancier.ChargeHeroiqueSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            if (tc != null && hr != null) {
                org.joml.Vector3d dir = hr.getDirection();
                double dx = dir.x, dz = dir.z;
                double len = Math.sqrt(dx*dx + dz*dz);
                if (len > 1e-6) { dx /= len; dz /= len; }

                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Spear", "DashForward", true,
                    commandBuffer != null ? commandBuffer : store);
                ClassSkillSounds.playSkillSound("SFX_Vrpg_Punch", playerRef, tc.getPosition(), commandBuffer);

                double dist = fr.varyon.vrpg.classes.lancier.ChargeHeroiqueSkill.chargeDistance();
                org.joml.Vector3d startPos = tc.getPosition();
                float dmg = fr.varyon.vrpg.classes.lancier.ChargeHeroiqueSkill.computeDamage(rank, playerRef);
                long stunMs = fr.varyon.vrpg.classes.lancier.ChargeHeroiqueSkill.stunMsForRank(rank);
                double hitRadius = fr.varyon.vrpg.classes.lancier.ChargeHeroiqueSkill.hitRadius();
                long casterIdx = entityRef.getIndex();
                java.util.HashSet<Long> hitSet = new java.util.HashSet<>();

                double teleportDist = 2.0;
                org.joml.Vector3d newPos = new org.joml.Vector3d(
                    startPos.x + dx * teleportDist, startPos.y, startPos.z + dz * teleportDist);
                com.hypixel.hytale.math.vector.Rotation3fc curRot = hr.getRotation();
                com.hypixel.hytale.math.vector.Rotation3f keepRot = new com.hypixel.hytale.math.vector.Rotation3f(
                    curRot.pitch(), curRot.yaw(), curRot.roll());
                com.hypixel.hytale.server.core.modules.entity.teleport.Teleport tele =
                    com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.createForPlayer(newPos, keepRot);
                tele.withoutVelocityReset();
                (commandBuffer != null ? commandBuffer : store).addComponent(entityRef,
                    com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.getComponentType(), tele);

                for (double t = 0.5; t <= dist; t += 0.8) {
                    org.joml.Vector3d sample = new org.joml.Vector3d(
                        startPos.x + dx * t, startPos.y + 0.8, startPos.z + dz * t);
                    com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector
                        .selectNearbyEntities(store, sample, hitRadius, targetRef -> {
                            try {
                                long tidx = targetRef.getIndex();
                                if (tidx == casterIdx || !hitSet.add(tidx)) return;
                                com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(
                                    targetRef, store,
                                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                                        new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                                        com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, dmg));
                                applyEntityEffect("Vrpg_Stun", stunMs / 1000f, targetRef, store);
                                lancierState.armCcDamage(uuid, acc.getTalentRank(PlayerClass.TIREUR, fr.varyon.vrpg.classes.lancier.LancierPassifs.CONTROLE_NODE),
                                    fr.varyon.vrpg.classes.lancier.LancierPassifs.CONTROLE_DURATION_MS);
                            } catch (Exception ignored) {}
                        }, ref -> ref.getIndex() != casterIdx);
                }

                com.hypixel.hytale.server.core.modules.physics.component.Velocity vel =
                    commandBuffer != null
                        ? commandBuffer.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType())
                        : store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType());
                if (vel != null) {
                    org.joml.Vector3d dashVel = new org.joml.Vector3d(dx * 36.0, 0.5, dz * 36.0);
                    vel.setClient(dashVel);
                    vel.getInstructions().clear();
                    vel.addInstruction(dashVel, null, com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                }
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.lancier.ChargeHeroiqueSkill.SKILL_ID);
        notifySkill(uuid, "Charge Héroïque");
        return true;
    }

    public boolean tryCastGardeDuLancier(@Nonnull UUID uuid,
                                          @Nonnull PlayerRef playerRef,
                                          @Nonnull Ref<EntityStore> entityRef,
                                          @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isLancier(acc)) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.TIREUR, fr.varyon.vrpg.classes.lancier.GardeDuLancierSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        long cdMs = fr.varyon.vrpg.classes.lancier.GardeDuLancierSkill.cooldownMsForRank(rank);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.lancier.GardeDuLancierSkill.SKILL_ID, cdMs)) return false;
        float staminaCost = fr.varyon.vrpg.classes.lancier.GardeDuLancierSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        lancierState.armGarde(uuid, rank, fr.varyon.vrpg.classes.lancier.GardeDuLancierSkill.windowMsForRank(rank));
        try {
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Spear", "Guard", true, store);
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) ClassSkillSounds.playSkillSound("SFX_Sword_T1_Block_Local", playerRef, tc.getPosition(), null);
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.lancier.GardeDuLancierSkill.SKILL_ID);
        notifySkill(uuid, "Garde du Lancier");
        return true;
    }

    public boolean tryCastHarponnage(@Nonnull UUID uuid,
                                      @Nonnull PlayerRef playerRef,
                                      @Nonnull Ref<EntityStore> entityRef,
                                      @Nonnull Store<EntityStore> store,
                                      @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isLancier(acc)) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.TIREUR, fr.varyon.vrpg.classes.lancier.HarponnageSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        long cdMs = fr.varyon.vrpg.classes.lancier.HarponnageSkill.cooldownMsForRank(rank);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.lancier.HarponnageSkill.SKILL_ID, cdMs)) return false;
        float staminaCost = fr.varyon.vrpg.classes.lancier.HarponnageSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        Ref<EntityStore> targeted = findTargetedNpcRef(playerRef, entityRef, store,
            fr.varyon.vrpg.classes.lancier.HarponnageSkill.range());
        if (targeted == null) return false;

        try {
            TransformComponent tcCaster = store.getComponent(entityRef, TransformComponent.getComponentType());
            TransformComponent tcTarget = store.getComponent(targeted, TransformComponent.getComponentType());
            if (tcCaster != null && tcTarget != null) {
                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Spear", "Throw", true,
                    commandBuffer != null ? commandBuffer : store);
                ClassSkillSounds.playSkillSound("SFX_Bow_T2_Shoot", playerRef, tcCaster.getPosition(), commandBuffer);

                float dmg = fr.varyon.vrpg.classes.lancier.HarponnageSkill.computeDamage(rank, playerRef);
                com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(targeted, store,
                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                        new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                        com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, dmg));

                org.joml.Vector3d toPlayer = new org.joml.Vector3d(tcCaster.getPosition()).sub(tcTarget.getPosition());
                double pullLen = toPlayer.length();
                if (pullLen > 1e-6) toPlayer.mul(1.0 / pullLen);
                double pullStrength = fr.varyon.vrpg.classes.lancier.HarponnageSkill.pullStrength();
                com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent kbComp =
                    new com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent();
                kbComp.setVelocity(new org.joml.Vector3d(toPlayer.x * pullStrength, 0.3, toPlayer.z * pullStrength));
                kbComp.setVelocityType(com.hypixel.hytale.protocol.ChangeVelocityType.Set);
                kbComp.setDuration(0.0f);
                (commandBuffer != null ? commandBuffer : store).putComponent(targeted,
                    com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent.getComponentType(), kbComp);

                int controleRank = acc.getTalentRank(PlayerClass.TIREUR, fr.varyon.vrpg.classes.lancier.LancierPassifs.CONTROLE_NODE);
                if (controleRank > 0) lancierState.armCcDamage(uuid, controleRank, fr.varyon.vrpg.classes.lancier.LancierPassifs.CONTROLE_DURATION_MS);
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.lancier.HarponnageSkill.SKILL_ID);
        notifySkill(uuid, "Harponnage");
        return true;
    }

    public boolean tryCastFormationDePiques(@Nonnull UUID uuid,
                                             @Nonnull PlayerRef playerRef,
                                             @Nonnull Ref<EntityStore> entityRef,
                                             @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isLancier(acc)) return false;
        if (formationZoneSystem == null) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.TIREUR, fr.varyon.vrpg.classes.lancier.FormationDePiquesSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        long cdMs = fr.varyon.vrpg.classes.lancier.FormationDePiquesSkill.cooldownMsForRank(rank);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.lancier.FormationDePiquesSkill.SKILL_ID, cdMs)) return false;
        float staminaCost = fr.varyon.vrpg.classes.lancier.FormationDePiquesSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        try {
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            com.hypixel.hytale.server.core.modules.entity.component.HeadRotation hr =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.modules.entity.component.HeadRotation.getComponentType());
            if (tc != null && hr != null) {
                org.joml.Vector3d dir = new org.joml.Vector3d(hr.getDirection());
                dir.y = 0;
                double len = dir.length();
                if (len > 1e-6) dir.mul(1.0 / len);
                org.joml.Vector3d zoneStart = new org.joml.Vector3d(tc.getPosition()).add(new org.joml.Vector3d(dir).mul(1.5));
                float dmgPerTick = fr.varyon.vrpg.classes.lancier.FormationDePiquesSkill.computeDamagePerTick(rank, playerRef);
                formationZoneSystem.createZone(uuid, entityRef, zoneStart, dir, rank, dmgPerTick);
                AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Spear", "Guard", true, store);
                ClassSkillSounds.playSkillSound("SFX_Vrpg_SkillActivate", playerRef, tc.getPosition(), null);
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.lancier.FormationDePiquesSkill.SKILL_ID);
        notifySkill(uuid, "Formation de Piques");
        return true;
    }

    public boolean tryCastEmpalement(@Nonnull UUID uuid,
                                      @Nonnull PlayerRef playerRef,
                                      @Nonnull Ref<EntityStore> entityRef,
                                      @Nonnull Store<EntityStore> store) {
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (!isLancier(acc)) return false;
        boolean bypass = RpgUiAdmin.isAdmin(playerRef) && RpgUiAdmin.isCreative(playerRef);
        int rank = acc.getTalentRank(PlayerClass.TIREUR, fr.varyon.vrpg.classes.lancier.EmpalementSkill.TALENT_NODE_ID);
        if (rank <= 0) return false;
        long cdMs = fr.varyon.vrpg.classes.lancier.EmpalementSkill.cooldownMsForRank(rank);
        if (!bypass && cooldowns.isOnCooldown(uuid, fr.varyon.vrpg.classes.lancier.EmpalementSkill.SKILL_ID, cdMs)) return false;
        float staminaCost = fr.varyon.vrpg.classes.lancier.EmpalementSkill.staminaCostForRank(rank);
        if (!ClassSkillStamina.hasEnough(playerRef, staminaCost)) return false;

        Ref<EntityStore> targeted = findTargetedNpcRef(playerRef, entityRef, store,
            fr.varyon.vrpg.classes.lancier.EmpalementSkill.range());

        try {
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Spear", "DashForward", true, store);
            TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (tc != null) ClassSkillSounds.playSkillSound("SFX_Sword_T2_Swing", playerRef, tc.getPosition(), null);

            if (targeted != null) {
                float dmg = fr.varyon.vrpg.classes.lancier.EmpalementSkill.computeDamage(rank, playerRef);
                com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(targeted, store,
                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                        new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(entityRef),
                        com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL, dmg));

                long rootMs = fr.varyon.vrpg.classes.lancier.EmpalementSkill.rootDurationMsForRank(rank);
                long bleedMs = fr.varyon.vrpg.classes.lancier.EmpalementSkill.bleedDurationMsForRank(rank);
                float bleedPct = fr.varyon.vrpg.classes.lancier.EmpalementSkill.bleedPctPerSForRank(rank);
                int ticks = (int)(bleedMs / 1000L);
                float dpt = dmg * bleedPct;
                lancierBleedSystem.applyBleed(targeted, dpt, ticks, store);
                lancierState.rootEntity(targeted.getIndex(), rootMs);

                int controleRank = acc.getTalentRank(PlayerClass.TIREUR, fr.varyon.vrpg.classes.lancier.LancierPassifs.CONTROLE_NODE);
                if (controleRank > 0) lancierState.armCcDamage(uuid, controleRank, fr.varyon.vrpg.classes.lancier.LancierPassifs.CONTROLE_DURATION_MS);
            }
        } catch (Exception ignored) {}

        ClassSkillStamina.consume(playerRef, staminaCost);
        if (!bypass) cooldowns.markUsed(uuid, fr.varyon.vrpg.classes.lancier.EmpalementSkill.SKILL_ID);
        notifySkill(uuid, "Empalement");
        return true;
    }

    private void damageNearby(@Nonnull org.joml.Vector3d center, float radius,
                               @Nonnull Ref<EntityStore> casterRef, @Nonnull Store<EntityStore> store,
                               float dmg, @Nonnull com.hypixel.hytale.server.core.modules.entity.damage.DamageCause cause) {
        long ci = casterRef.getIndex();
        java.util.HashSet<Long> hit = new java.util.HashSet<>();
        com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector
            .selectNearbyEntities(store, center, radius, t -> {
                try {
                    long tidx = t.getIndex();
                    if (tidx == ci || !hit.add(tidx)) return;
                    com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems.executeDamage(t, store,
                        new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                            new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(casterRef), cause, dmg));
                } catch (Exception ignored2) {}
            }, t -> t.getIndex() != ci);
    }

    private void spawnProjectileToward(
            @Nonnull String configId,
            @Nonnull org.joml.Vector3d fromPos,
            @Nonnull org.joml.Vector3d toPos,
            @Nonnull Ref<EntityStore> casterRef,
            @Nonnull PlayerRef playerRef,
            @Nullable CommandBuffer<EntityStore> commandBuffer) {
        try {
            com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig cfg =
                com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig.getAssetMap().getAsset(configId);
            if (cfg == null) return;
            double dx = toPos.x - fromPos.x;
            double dy = (toPos.y + 0.5) - (fromPos.y + 1.5);
            double dz = toPos.z - fromPos.z;
            double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (len < 1e-6) return;
            org.joml.Vector3d dir = new org.joml.Vector3d(dx/len, dy/len, dz/len);
            org.joml.Vector3d spawnPos = new org.joml.Vector3d(
                fromPos.x + dir.x * 0.5,
                fromPos.y + 1.5,
                fromPos.z + dir.z * 0.5);
            if (commandBuffer != null) {
                com.hypixel.hytale.server.core.modules.projectile.ProjectileModule.get()
                    .spawnProjectile(casterRef, commandBuffer, cfg, spawnPos, dir);
            } else {
                java.util.UUID wUuid = playerRef.getWorldUuid();
                if (wUuid == null) return;
                com.hypixel.hytale.server.core.universe.world.World world =
                    com.hypixel.hytale.server.core.universe.Universe.get().getWorld(wUuid);
                if (world == null) return;
                final com.hypixel.hytale.server.core.universe.world.World fw = world;
                final org.joml.Vector3d fSpawnPos = spawnPos;
                final org.joml.Vector3d fDir = dir;
                final Ref<EntityStore> fRef = casterRef;
                final com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig fCfg = cfg;
                fw.execute(() -> {
                    try {
                        Store<EntityStore> ws = fw.getEntityStore().getStore();
                        java.lang.reflect.Method takeCmd = ws.getClass().getDeclaredMethod("takeCommandBuffer");
                        takeCmd.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        CommandBuffer<EntityStore> cb = (CommandBuffer<EntityStore>) takeCmd.invoke(ws);
                        try {
                            com.hypixel.hytale.server.core.modules.projectile.ProjectileModule.get()
                                .spawnProjectile(fRef, cb, fCfg, fSpawnPos, fDir);
                        } finally {
                            try {
                                java.lang.reflect.Method consume = cb.getClass().getDeclaredMethod("consume");
                                consume.setAccessible(true);
                                consume.invoke(cb);
                            } catch (Exception ignored3) {}
                        }
                    } catch (Exception ignored2) {}
                });
            }
        } catch (Exception ignored) {}
    }

    private void spawnProjectileTowardTracked(
            @Nonnull String configId,
            @Nonnull org.joml.Vector3d fromPos,
            @Nonnull org.joml.Vector3d toPos,
            @Nonnull Ref<EntityStore> casterRef,
            @Nonnull PlayerRef playerRef,
            @Nonnull UUID creatorUuid,
            @Nonnull fr.varyon.vrpg.classes.arbaletrier.CarreauExplosifGroundSystem groundSystem) {
        try {
            com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig cfg =
                com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig.getAssetMap().getAsset(configId);
            if (cfg == null) return;
            double dx = toPos.x - fromPos.x;
            double dy = (toPos.y + 0.5) - (fromPos.y + 1.5);
            double dz = toPos.z - fromPos.z;
            double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (len < 1e-6) return;
            org.joml.Vector3d dir = new org.joml.Vector3d(dx/len, dy/len, dz/len);
            org.joml.Vector3d spawnPos = new org.joml.Vector3d(
                fromPos.x + dir.x * 0.5,
                fromPos.y + 1.5,
                fromPos.z + dir.z * 0.5);
            java.util.UUID wUuid = playerRef.getWorldUuid();
            if (wUuid == null) return;
            com.hypixel.hytale.server.core.universe.world.World world =
                com.hypixel.hytale.server.core.universe.Universe.get().getWorld(wUuid);
            if (world == null) return;
            final com.hypixel.hytale.server.core.universe.world.World fw = world;
            final org.joml.Vector3d fSpawnPos = spawnPos;
            final org.joml.Vector3d fDir = dir;
            final Ref<EntityStore> fRef = casterRef;
            final com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig fCfg = cfg;
            fw.execute(() -> {
                try {
                    Store<EntityStore> ws = fw.getEntityStore().getStore();
                    java.lang.reflect.Method takeCmd = ws.getClass().getDeclaredMethod("takeCommandBuffer");
                    takeCmd.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    CommandBuffer<EntityStore> cb = (CommandBuffer<EntityStore>) takeCmd.invoke(ws);
                    Ref<EntityStore> projRef = com.hypixel.hytale.server.core.modules.projectile.ProjectileModule.get()
                        .spawnProjectile(fRef, cb, fCfg, fSpawnPos, fDir);
                    try {
                        java.lang.reflect.Method consume = cb.getClass().getDeclaredMethod("consume");
                        consume.setAccessible(true);
                        consume.invoke(cb);
                    } catch (Exception ignored3) {}
                    groundSystem.trackProjectile(projRef, creatorUuid);
                } catch (Exception ignored2) {}
            });
        } catch (Exception ignored) {}
    }

    private void applyEntityEffect(
            @Nonnull String effectId,
            float durationSec,
            @Nonnull Ref<EntityStore> targetRef,
            @Nonnull Store<EntityStore> store) {
        try {
            int idx = com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getIndex(effectId);
            if (idx < 0) return;
            com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect effect =
                (com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect)
                com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect.getAssetMap().getAsset(idx);
            if (effect == null) return;
            com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent ec =
                store.getComponent(targetRef, com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
            if (ec != null) ec.addEffect(targetRef, effect, durationSec,
                com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior.OVERWRITE, store);
        } catch (Exception ignored) {}
    }
}
