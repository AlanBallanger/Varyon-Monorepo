package fr.varyon.vrpg;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;
import javax.annotation.Nonnull;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.vrpg.commands.VpaBlastCommand;
import fr.varyon.vrpg.combat.MobParticipantsTracker;
import fr.varyon.vrpg.config.ClassXpConfig;
import fr.varyon.vrpg.config.MobCategoriesConfig;
import fr.varyon.vrpg.config.TierMappingConfig;
import fr.varyon.vrpg.config.VrpgConfig;
import fr.varyon.vrpg.config.XpTableConfig;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import fr.varyon.vrpg.classes.ability.ClassSkillInteractionInjector;
import fr.varyon.vrpg.classes.ability.ClassSkillKeyFilter;
import fr.varyon.vrpg.classes.ability.ClassSkillService;
import fr.varyon.vrpg.classes.ability.ClassSkillTriggerInteraction;
import fr.varyon.vrpg.classes.ClassKillXpSystem;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.SpecWeaponMasteryDamageSystem;
import fr.varyon.vrpg.classes.duelliste.DuellisteBleedSystem;
import fr.varyon.vrpg.classes.duelliste.DuellisteDesarmementDamageSystem;
import fr.varyon.vrpg.classes.duelliste.DuellisteIncomingDamageSystem;
import fr.varyon.vrpg.classes.duelliste.DuellisteOutgoingDamageSystem;
import fr.varyon.vrpg.classes.duelliste.DuellisteSpeedSystem;
import fr.varyon.vrpg.classes.duelliste.DuellisteState;
import fr.varyon.vrpg.classes.ombre.OmbreState;
import fr.varyon.vrpg.classes.ombre.OmbrePoisonSystem;
import fr.varyon.vrpg.classes.ombre.OmbreOutgoingDamageSystem;
import fr.varyon.vrpg.classes.ombre.OmbreIncomingDamageSystem;
import fr.varyon.vrpg.classes.ombre.OmbreSpeedSystem;
import fr.varyon.vrpg.classes.ombre.OmbreStealthAttitudeProvider;
import fr.varyon.vrpg.commands.VpaCommand;
import fr.varyon.vrpg.commands.VpaAdminCommand;
import fr.varyon.vrpg.commands.VpaSurfaceCommand;
import fr.varyon.vrpg.commands.VrpgCommand;
import fr.varyon.vrpg.restriction.TalentItemPlaceRestrictionSystem;
import fr.varyon.vrpg.restriction.TalentItemRestrictionSystem;
import fr.varyon.vrpg.profession.fermier.FarmerAnimalDropSystem;
import fr.varyon.vrpg.profession.fermier.FarmerBlockBreakSystem;
import fr.varyon.vrpg.profession.fermier.FarmerComboTracker;
import fr.varyon.vrpg.profession.fermier.FarmerPlaceCropSystem;
import fr.varyon.vrpg.profession.fermier.FarmerPickupHarvestSystem;
import fr.varyon.vrpg.profession.fermier.GuardianCropManager;
import fr.varyon.vrpg.profession.fermier.GuardianCropTickSystem;
import fr.varyon.vrpg.profession.forestier.ForestierBlockBreakSystem;
import fr.varyon.vrpg.profession.forestier.ForestierComboTracker;
import fr.varyon.vrpg.profession.forestier.ForestierPickupSystem;
import fr.varyon.vrpg.profession.forestier.PoumonLoutreTickSystem;
import fr.varyon.vrpg.profession.forestier.RodeurSylvestreTickSystem;
import fr.varyon.vrpg.profession.forestier.RodeurSylvestreFallSystem;
import fr.varyon.vrpg.profession.chasseur.ChasseurComboTracker;
import fr.varyon.vrpg.profession.chasseur.ChasseurGuardianManager;
import fr.varyon.vrpg.profession.chasseur.ChasseurGuardianTickSystem;
import fr.varyon.vrpg.profession.chasseur.ChasseurKillSystem;
import fr.varyon.vrpg.profession.chasseur.ChasseurPickupSystem;
import fr.varyon.vrpg.profession.chasseur.RodeurDunesTickSystem;
import fr.varyon.vrpg.profession.chasseur.RodeurDunesFallSystem;
import fr.varyon.vrpg.profession.chasseur.SecondSouffleTickSystem;
import fr.varyon.vrpg.profession.chasseur.YeuxLynxTickSystem;
import fr.varyon.vrpg.profession.forestier.GuardianWoodManager;
import fr.varyon.vrpg.profession.forestier.GuardianWoodTickSystem;
import fr.varyon.vrpg.profession.forestier.LitDeFortuneTickSystem;
import fr.varyon.vrpg.profession.forestier.YeuxHibouTickSystem;
import fr.varyon.vrpg.profession.chasseur.MaitriseChasseurDamageBoostSystem;
import fr.varyon.vrpg.profession.chasseur.MaitriseChasseurDamageSystem;
import fr.varyon.vrpg.profession.chasseur.MaitriseChasseurSpeedSystem;
import fr.varyon.vrpg.profession.chasseur.MaitriseChasseurTracker;
import fr.varyon.vrpg.profession.fermier.MaitriseFermierDamageSystem;
import fr.varyon.vrpg.profession.fermier.MaitriseFermierRegenSystem;
import fr.varyon.vrpg.profession.fermier.MaitriseFermierSpeedSystem;
import fr.varyon.vrpg.profession.fermier.MaitriseFermierTracker;
import fr.varyon.vrpg.profession.forestier.MaitriseForestierDamageSystem;
import fr.varyon.vrpg.profession.forestier.MaitriseForestierSpeedSystem;
import fr.varyon.vrpg.profession.forestier.MaitriseForestierStaminaSystem;
import fr.varyon.vrpg.profession.forestier.MaitriseForestierTracker;
import fr.varyon.vrpg.profession.mineur.MaitriseMineurDamageSystem;
import fr.varyon.vrpg.profession.mineur.MaitriseMineurSpeedSystem;
import fr.varyon.vrpg.profession.mineur.BagCraftRestrictionSystem;
import fr.varyon.vrpg.profession.mineur.ExplosionTalentSystem;
import fr.varyon.vrpg.profession.mineur.MinerComboTracker;
import fr.varyon.vrpg.profession.mineur.VeinCooldownTracker;
import fr.varyon.vrpg.profession.mineur.GuardianOreTickSystem;
import fr.varyon.vrpg.profession.mineur.GuardianStoneManager;
import fr.varyon.vrpg.profession.mineur.MinerBlockBreakSystem;
import fr.varyon.vrpg.profession.mineur.MinerGuardianOreHardnessSystem;
import fr.varyon.vrpg.profession.mineur.MiningHelmet;
import fr.varyon.vrpg.profession.mineur.MiningHelmetTickSystem;
import fr.varyon.vrpg.rpg.PlayerAccount;
import fr.varyon.vrpg.rpg.ProfessionManager;
import fr.varyon.vrpg.item.ProfessionXpBoostInteraction;
import fr.varyon.vrpg.item.ProfessionXpPotionInteraction;
import fr.varyon.vrpg.ui.AbilitySlotsHud;
import fr.varyon.vrpg.ui.ProfessionXpHud;
import fr.varyon.vrpg.ui.XpNotifHud;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VaryonRpgPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static volatile VaryonRpgPlugin instance;

    private ProfessionManager professionManager;
    private ClassManager classManager;
    private MobParticipantsTracker mobParticipantsTracker;
    private ClassKillXpSystem classKillXpSystem;
    private ClassSkillService classSkillService;
    private ClassSkillKeyFilter classSkillKeyFilter;
    private PacketFilter classSkillPacketFilter;
    private DuellisteState duellisteState;
    private DuellisteBleedSystem duellisteBleedSystem;
    private DuellisteSpeedSystem duellisteSpeedSystem;
    private fr.varyon.vrpg.classes.berserker.BerserkerState berserkerState;
    private fr.varyon.vrpg.classes.berserker.BerserkerCombatTracker berserkerCombatTracker;
    private fr.varyon.vrpg.classes.berserker.BerserkerTickSystem berserkerTickSystem;
    private fr.varyon.vrpg.classes.berserker.BerserkerOutgoingDamageSystem berserkerOutgoingDamageSystem;
    private fr.varyon.vrpg.classes.berserker.BerserkerIncomingDamageSystem berserkerIncomingDamageSystem;
    private fr.varyon.vrpg.classes.ravageur.RavageurState ravageurState;
    private fr.varyon.vrpg.classes.ravageur.RavageurOutgoingDamageSystem ravageurOutgoingDamageSystem;
    private fr.varyon.vrpg.classes.ravageur.RavageurIncomingDamageSystem ravageurIncomingDamageSystem;
    private OmbreState ombreState;
    private OmbrePoisonSystem ombrePoisonSystem;
    private OmbreSpeedSystem ombreSpeedSystem;
    private fr.varyon.vrpg.classes.rempart.RempartState rempartState;
    private MiningHelmet miningHelmet;
    private GuardianStoneManager guardianManager;
    private MinerComboTracker comboTracker;
    private FarmerComboTracker farmerComboTracker;
    private FarmerAnimalDropSystem farmerAnimalDropSystem;
    private FarmerPickupHarvestSystem farmerPickupHarvestSystem;
    private GuardianCropManager guardianCropManager;
    private VeinCooldownTracker veinCooldownTracker;
    private VpaSurfaceCommand surfaceCommand;
    private VpaBlastCommand blastCommand;
    private ExplosionTalentSystem explosionTalentSystem;
    private ForestierComboTracker forestierComboTracker;
    private GuardianWoodManager guardianWoodManager;
    private PoumonLoutreTickSystem poumonLoutreTickSystem;
    private RodeurSylvestreTickSystem rodeurSylvestreTickSystem;
    private ChasseurComboTracker chasseurComboTracker;
    private ChasseurGuardianManager chasseurGuardianManager;
    private ChasseurKillSystem chasseurKillSystem;
    private RodeurDunesTickSystem rodeurDunesTickSystem;
    private SecondSouffleTickSystem secondSouffleTickSystem;
    private YeuxLynxTickSystem yeuxLynxTickSystem;
    private YeuxHibouTickSystem yeuxHibouTickSystem;
    private LitDeFortuneTickSystem litDeFortuneTickSystem;
    private MaitriseMineurSpeedSystem maitriseMineurSpeedSystem;
    private MaitriseForestierTracker maitriseForestierTracker;
    private MaitriseForestierSpeedSystem maitriseForestierSpeedSystem;
    private MaitriseForestierStaminaSystem maitriseForestierStaminaSystem;
    private MaitriseFermierTracker maitriseFermierTracker;
    private MaitriseFermierSpeedSystem maitriseFermierSpeedSystem;
    private MaitriseFermierRegenSystem maitriseFermierRegenSystem;
    private MaitriseChasseurTracker maitriseChasseurTracker;
    private MaitriseChasseurSpeedSystem maitriseChasseurSpeedSystem;
    private ForestierBlockBreakSystem forestierBlockBreakSystem;

    private final ConcurrentHashMap<UUID, PlayerRef> pendingProfessionHudInit = new ConcurrentHashMap<>();

    public VaryonRpgPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    public static VaryonRpgPlugin getInstance() {
        return instance;
    }

    public ProfessionManager getProfessionManager() {
        return professionManager;
    }

    public ClassManager getClassManager() {
        return classManager;
    }

    public ClassSkillService getClassSkillService() {
        return classSkillService;
    }

    public java.nio.file.Path getPluginDataDirectory() {
        return getDataDirectory();
    }

    @Override
    protected void setup() {
        instance = this;

        VrpgConfig.load(getDataDirectory());
        XpTableConfig.load(getDataDirectory());
        MobCategoriesConfig.load(getDataDirectory());
        TierMappingConfig.load(getDataDirectory());
        ClassXpConfig.load(getDataDirectory());

        try {
            getCodecRegistry(Interaction.CODEC)
                .register(ProfessionXpPotionInteraction.TYPE_NAME,
                    ProfessionXpPotionInteraction.class,
                    ProfessionXpPotionInteraction.CODEC);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register ProfessionXpPotionInteraction");
        }

        try {
            getCodecRegistry(Interaction.CODEC)
                .register(ProfessionXpBoostInteraction.TYPE_NAME,
                    ProfessionXpBoostInteraction.class,
                    ProfessionXpBoostInteraction.CODEC);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register ProfessionXpBoostInteraction");
        }

        try {
            getCodecRegistry(Interaction.CODEC)
                .register(ClassSkillTriggerInteraction.TYPE_NAME,
                    ClassSkillTriggerInteraction.class,
                    ClassSkillTriggerInteraction.CODEC);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register ClassSkillTriggerInteraction");
        }

        try {
            this.classManager = new ClassManager(getDataDirectory());
            this.duellisteState = new DuellisteState();
            this.duellisteBleedSystem = new DuellisteBleedSystem();
            this.duellisteSpeedSystem = new DuellisteSpeedSystem(classManager, duellisteState);
            this.ombreState = new OmbreState();
            this.ombrePoisonSystem = new OmbrePoisonSystem();
            this.ombreSpeedSystem = new OmbreSpeedSystem(classManager, ombreState);
            this.rempartState = new fr.varyon.vrpg.classes.rempart.RempartState();
            this.berserkerState = new fr.varyon.vrpg.classes.berserker.BerserkerState();
            this.berserkerCombatTracker = new fr.varyon.vrpg.classes.berserker.BerserkerCombatTracker();
            this.berserkerTickSystem = new fr.varyon.vrpg.classes.berserker.BerserkerTickSystem(classManager, berserkerState, berserkerCombatTracker);
            this.berserkerOutgoingDamageSystem = new fr.varyon.vrpg.classes.berserker.BerserkerOutgoingDamageSystem(classManager, berserkerState, berserkerCombatTracker, duellisteBleedSystem);
            this.berserkerIncomingDamageSystem = new fr.varyon.vrpg.classes.berserker.BerserkerIncomingDamageSystem(classManager, berserkerState, berserkerCombatTracker, new fr.varyon.vrpg.classes.ability.ClassSkillCooldowns());
            this.ravageurState = new fr.varyon.vrpg.classes.ravageur.RavageurState();
            this.ravageurOutgoingDamageSystem = new fr.varyon.vrpg.classes.ravageur.RavageurOutgoingDamageSystem(classManager, ravageurState);
            this.ravageurIncomingDamageSystem = new fr.varyon.vrpg.classes.ravageur.RavageurIncomingDamageSystem(classManager, ravageurState);
            this.classSkillService = new ClassSkillService(classManager, duellisteState, ombreState, rempartState, berserkerState, ravageurState);
            this.classSkillKeyFilter = new ClassSkillKeyFilter(classManager, rempartState);
            this.classSkillPacketFilter = PacketAdapters.registerInbound(classSkillKeyFilter);
            ClassSkillInteractionInjector.register();
            this.mobParticipantsTracker = new MobParticipantsTracker();
            this.classKillXpSystem = new ClassKillXpSystem(classManager, mobParticipantsTracker, berserkerState, ravageurState);
        } catch (Exception e) {
            LOGGER.atSevere().withCause(e).log("[VaryonRPG] Failed to initialize ClassManager");
        }

        try {
            this.professionManager = new ProfessionManager(getDataDirectory());
            this.miningHelmet = new MiningHelmet(professionManager);
            this.guardianManager = new GuardianStoneManager();
            this.comboTracker = new MinerComboTracker();
            this.farmerComboTracker = new FarmerComboTracker();
            this.farmerAnimalDropSystem = new FarmerAnimalDropSystem(professionManager);
            this.guardianCropManager = new GuardianCropManager();
            this.farmerPickupHarvestSystem = new FarmerPickupHarvestSystem(professionManager, farmerComboTracker, guardianCropManager);
            this.veinCooldownTracker = new VeinCooldownTracker();
            this.explosionTalentSystem = new ExplosionTalentSystem();
            this.forestierComboTracker = new ForestierComboTracker();
            this.guardianWoodManager = new GuardianWoodManager();
            this.poumonLoutreTickSystem = new PoumonLoutreTickSystem(professionManager);
            this.rodeurSylvestreTickSystem = new RodeurSylvestreTickSystem(professionManager);
            this.chasseurComboTracker = new ChasseurComboTracker();
            this.chasseurGuardianManager = new ChasseurGuardianManager();
            this.chasseurKillSystem = new ChasseurKillSystem(professionManager, chasseurComboTracker, chasseurGuardianManager);
            this.rodeurDunesTickSystem = new RodeurDunesTickSystem(professionManager);
            this.secondSouffleTickSystem = new SecondSouffleTickSystem(professionManager);
            this.yeuxLynxTickSystem = new YeuxLynxTickSystem(professionManager);
            this.yeuxHibouTickSystem = new YeuxHibouTickSystem(professionManager);
            this.litDeFortuneTickSystem = new LitDeFortuneTickSystem(professionManager);
            this.maitriseMineurSpeedSystem = new MaitriseMineurSpeedSystem(professionManager);
            this.maitriseForestierTracker = new MaitriseForestierTracker();
            this.maitriseForestierSpeedSystem = new MaitriseForestierSpeedSystem(professionManager, maitriseForestierTracker);
            this.maitriseForestierStaminaSystem = new MaitriseForestierStaminaSystem(professionManager);
            this.maitriseFermierTracker = new MaitriseFermierTracker();
            this.maitriseFermierSpeedSystem = new MaitriseFermierSpeedSystem(professionManager, maitriseFermierTracker);
            this.maitriseFermierRegenSystem = new MaitriseFermierRegenSystem(professionManager);
            this.maitriseChasseurTracker = new MaitriseChasseurTracker();
            this.maitriseChasseurSpeedSystem = new MaitriseChasseurSpeedSystem(professionManager, maitriseChasseurTracker);
            this.chasseurKillSystem.setMaitriseTracker(maitriseChasseurTracker);
            this.farmerPickupHarvestSystem.setMaitriseTracker(maitriseFermierTracker);
            this.forestierBlockBreakSystem = new ForestierBlockBreakSystem(professionManager, forestierComboTracker, guardianWoodManager);
            this.forestierBlockBreakSystem.setMaitriseTracker(maitriseForestierTracker);
            this.surfaceCommand = new VpaSurfaceCommand();
            this.blastCommand = new VpaBlastCommand(explosionTalentSystem);
        } catch (Exception e) {
            LOGGER.atSevere().withCause(e).log("[VaryonRPG] Failed to initialize ProfessionManager");
        }

        try {
            getCommandRegistry().registerCommand(new VpaCommand(this));
            getCommandRegistry().registerCommand(new VrpgCommand(this));
            getCommandRegistry().registerCommand(new VpaAdminCommand());
            getCommandRegistry().registerCommand(surfaceCommand);
            getCommandRegistry().registerCommand(blastCommand);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] enregistrement commandes");
        }

        try {
            getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, event -> {
                Player player = event.getHolder().getComponent(Player.getComponentType());
                PlayerRef ref = event.getHolder().getComponent(PlayerRef.getComponentType());
                if (player == null || ref == null || professionManager == null) return;
                professionManager.ensureAccount(ref.getUuid(), ref.getUsername());
                if (classManager != null) classManager.ensureAccount(ref.getUuid(), ref.getUsername());
                pendingProfessionHudInit.put(ref.getUuid(), ref);
            });
            getEventRegistry().registerGlobal(PlayerReadyEvent.class, event -> {
                if (professionManager == null) return;
                @SuppressWarnings("rawtypes") Ref entityRef = event.getPlayerRef();
                if (entityRef == null) return;
                @SuppressWarnings("rawtypes") Store store = entityRef.getStore();
                if (store == null) return;
                @SuppressWarnings("unchecked") PlayerRef connectRefLookup = (PlayerRef) store.getComponent(entityRef, PlayerRef.getComponentType());
                UUID uid = connectRefLookup != null ? connectRefLookup.getUuid() : null;
                if (uid == null) return;
                PlayerRef connectRef = pendingProfessionHudInit.remove(uid);
                if (connectRef == null) return;
                Player player = event.getPlayer();
                if (player == null) return;
                World world = ((EntityStore) store.getExternalData()).getWorld();
                if (world == null) return;

                final Player readyPlayer = player;
                final PlayerRef readyRef = connectRef;
                world.execute(() -> {
                    try {
                        if (!readyRef.isValid()) return;
                        ProfessionXpHud.getOrCreate(readyPlayer, readyRef);
                        XpNotifHud.getOrCreate(readyPlayer, readyRef);
                        AbilitySlotsHud.getOrCreate(readyPlayer, readyRef).refreshSlots();
                    } catch (Exception e) {
                        LOGGER.atWarning().withCause(e).log("[VaryonRPG] init ProfessionXpHud");
                    }
                    try {
                        if (classManager != null && readyRef.isValid()) {
                            classManager.applyStats(readyRef.getUuid(), readyRef);
                        }
                    } catch (Exception e) {
                        LOGGER.atWarning().withCause(e).log("[VaryonRPG] applyStats classes");
                    }
                });
            });
            getEventRegistry().registerGlobal(PlayerInteractEvent.class, event -> {
                ItemStack held = event.getItemInHand();
                if (held == null) return;
                String itemId = held.getItemId();
                if (itemId == null) return;
                boolean isOreBag = BagCraftRestrictionSystem.ORE_BAG_IDS.contains(itemId);
                boolean isCropBag = BagCraftRestrictionSystem.CROP_BAG_IDS.contains(itemId);
                boolean isWoodBag = BagCraftRestrictionSystem.WOOD_BAG_IDS.contains(itemId);
                if (!isOreBag && !isCropBag && !isWoodBag) return;
                Player player = event.getPlayer();
                if (player == null) return;
                Ref<EntityStore> playerEntityRef = event.getPlayerRef();
                PlayerRef ref = playerEntityRef != null ? playerEntityRef.getStore().getComponent(playerEntityRef, PlayerRef.getComponentType()) : null;
                if (ref == null || professionManager == null) { event.setCancelled(true); return; }
                PlayerAccount acc = professionManager.getAccount(ref.getUuid());
                boolean ok;
                String msg;
                if (isOreBag) {
                    ok = acc != null && acc.isActive(fr.varyon.vrpg.rpg.Profession.MINEUR)
                        && acc.getTalentRank(fr.varyon.vrpg.rpg.Profession.MINEUR, "13") > 0;
                    msg = "Besace du Foreur — talent Mineur (nœud 13) requis.";
                } else if (isWoodBag) {
                    ok = acc != null && acc.isActive(fr.varyon.vrpg.rpg.Profession.FORESTIER)
                        && acc.getTalentRank(fr.varyon.vrpg.rpg.Profession.FORESTIER, "13") > 0;
                    msg = "Besace du Forestier — talent Forestier (nœud 13) requis.";
                } else {
                    ok = acc != null && acc.isActive(fr.varyon.vrpg.rpg.Profession.FERMIER)
                        && acc.getTalentRank(fr.varyon.vrpg.rpg.Profession.FERMIER, "16") > 0;
                    msg = "Besace du Paysan — talent Fermier (nœud 16) requis.";
                }
                if (!ok) {
                    event.setCancelled(true);
                    ref.sendMessage(com.hypixel.hytale.server.core.Message.raw(msg)
                        .color(new java.awt.Color(200, 50, 50)));
                }
            });
            getEventRegistry().registerGlobal(PlayerInteractEvent.class, event -> {
                ItemStack heldRestrict = event.getItemInHand();
                if (heldRestrict != null) {
                    String restrictId = heldRestrict.getItemId();
                    if (restrictId != null && TalentItemRestrictionSystem.getMessage(restrictId) != null) {
                        Player restrictPlayer = event.getPlayer();
                        if (restrictPlayer != null) {
                            Ref<EntityStore> restrictEntityRef = event.getPlayerRef();
                            PlayerRef restrictRef = restrictEntityRef != null ? restrictEntityRef.getStore().getComponent(restrictEntityRef, PlayerRef.getComponentType()) : null;
                            PlayerAccount restrictAcc = restrictRef != null && professionManager != null
                                ? professionManager.getAccount(restrictRef.getUuid()) : null;
                            if (!TalentItemRestrictionSystem.isAllowed(restrictId, restrictAcc)) {
                                event.setCancelled(true);
                                restrictRef.sendMessage(com.hypixel.hytale.server.core.Message.raw(
                                    TalentItemRestrictionSystem.getMessage(restrictId))
                                    .color(new java.awt.Color(200, 50, 50)));
                                return;
                            }
                        }
                    }
                }
            });
            getEventRegistry().registerGlobal(PlayerInteractEvent.class, event -> {
                InteractionType action = event.getActionType();
                if (action != InteractionType.Secondary && action != InteractionType.Use) return;
                Entity targetEnt = event.getTargetEntity();
                if (!(targetEnt instanceof NPCEntity npc)) return;
                String role = npc.getRoleName();
                if (role == null) return;
                String roleLower = role.toLowerCase(java.util.Locale.ROOT);
                Player player = event.getPlayer();
                if (player == null) return;
                Ref<EntityStore> milkEntityRef = event.getPlayerRef();
                PlayerRef milkRef = milkEntityRef != null ? milkEntityRef.getStore().getComponent(milkEntityRef, PlayerRef.getComponentType()) : null;
                if (milkRef == null || farmerAnimalDropSystem == null) return;
                farmerAnimalDropSystem.onMilkInteract(milkRef, roleLower);
            });
            getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event -> {
                PlayerRef ref = event.getPlayerRef();
                if (ref != null) {
                    pendingProfessionHudInit.remove(ref.getUuid());
                    ProfessionXpHud.cleanup(ref.getUuid());
                    XpNotifHud.cleanup(ref.getUuid());
                    AbilitySlotsHud.cleanup(ref.getUuid());
                }
                if (ref != null && professionManager != null) {
                    professionManager.onPlayerDisconnect(ref.getUuid());
                    if (comboTracker != null) comboTracker.remove(ref.getUuid());
                    if (farmerComboTracker != null) farmerComboTracker.remove(ref.getUuid());
                    if (forestierComboTracker != null) forestierComboTracker.remove(ref.getUuid());
                    if (poumonLoutreTickSystem != null) poumonLoutreTickSystem.removePlayer(ref.getUuid());
                    if (rodeurSylvestreTickSystem != null) rodeurSylvestreTickSystem.removePlayer(ref.getUuid());
                    if (yeuxLynxTickSystem != null) yeuxLynxTickSystem.removePlayer(ref.getUuid());
                    if (yeuxHibouTickSystem != null) yeuxHibouTickSystem.removePlayer(ref.getUuid());
                    if (litDeFortuneTickSystem != null) litDeFortuneTickSystem.removePlayer(ref.getUuid());
                    if (chasseurComboTracker != null) chasseurComboTracker.remove(ref.getUuid());
                    if (rodeurDunesTickSystem != null) rodeurDunesTickSystem.removePlayer(ref.getUuid());
                    if (secondSouffleTickSystem != null) secondSouffleTickSystem.removePlayer(ref.getUuid());
                    if (farmerPickupHarvestSystem != null) farmerPickupHarvestSystem.removePlayer(ref.getUuid());
                    if (farmerAnimalDropSystem != null) farmerAnimalDropSystem.removePlayer(ref.getUuid());
                    if (veinCooldownTracker != null) veinCooldownTracker.remove(ref.getUuid());
                    if (miningHelmet != null) miningHelmet.removePlayer(ref.getUuid());
                    if (surfaceCommand != null) surfaceCommand.clearCooldown(ref.getUuid());
                    if (blastCommand != null) blastCommand.clearCooldown(ref.getUuid());
                    if (maitriseMineurSpeedSystem != null) maitriseMineurSpeedSystem.removePlayer(ref.getUuid());
                    if (maitriseForestierTracker != null) maitriseForestierTracker.remove(ref.getUuid());
                    if (maitriseForestierSpeedSystem != null) maitriseForestierSpeedSystem.removePlayer(ref.getUuid());
                    if (maitriseForestierStaminaSystem != null) maitriseForestierStaminaSystem.removePlayer(ref.getUuid());
                    if (maitriseFermierTracker != null) maitriseFermierTracker.remove(ref.getUuid());
                    if (maitriseFermierSpeedSystem != null) maitriseFermierSpeedSystem.removePlayer(ref.getUuid());
                    if (maitriseFermierRegenSystem != null) maitriseFermierRegenSystem.removePlayer(ref.getUuid());
                    if (maitriseChasseurTracker != null) maitriseChasseurTracker.remove(ref.getUuid());
                    if (maitriseChasseurSpeedSystem != null) maitriseChasseurSpeedSystem.removePlayer(ref.getUuid());
                }
                if (ref != null && duellisteState != null) {
                    duellisteState.cleanup(ref.getUuid());
                }
                if (ref != null && duellisteSpeedSystem != null) {
                    duellisteSpeedSystem.removePlayer(ref.getUuid());
                }
                if (ref != null && ombreState != null) {
                    ombreState.cleanup(ref.getUuid());
                }
                if (ref != null && rempartState != null) {
                    rempartState.cleanup(ref.getUuid());
                }
                if (ref != null && berserkerState != null) {
                    berserkerState.cleanup(ref.getUuid());
                }
                if (ref != null && berserkerCombatTracker != null) {
                    berserkerCombatTracker.remove(ref.getUuid());
                }
                if (ref != null && berserkerTickSystem != null) {
                    berserkerTickSystem.removePlayer(ref.getUuid());
                }
                if (ref != null && ombreSpeedSystem != null) {
                    ombreSpeedSystem.removePlayer(ref.getUuid());
                }
                if (ref != null && classKillXpSystem != null) {
                    classKillXpSystem.cleanup(ref.getUuid());
                }
                if (ref != null && classSkillService != null) {
                    classSkillService.cleanup(ref.getUuid());
                }
                if (ref != null && classManager != null) {
                    classManager.onPlayerDisconnect(ref.getUuid());
                }
            });
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] enregistrement events joueur");
        }
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("[VaryonRPG] démarré v1.0.0");

        try {
            getEntityStoreRegistry().registerSystem(new MinerGuardianOreHardnessSystem());
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register MinerGuardianOreHardnessSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(new MinerBlockBreakSystem(professionManager, guardianManager, comboTracker, veinCooldownTracker));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register MinerBlockBreakSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(new MiningHelmetTickSystem(professionManager, miningHelmet));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register MiningHelmetTickSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(new GuardianOreTickSystem(guardianManager));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register GuardianOreTickSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(explosionTalentSystem);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register ExplosionTalentSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(forestierBlockBreakSystem);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register ForestierBlockBreakSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(new ForestierPickupSystem(professionManager));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register ForestierPickupSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(new GuardianWoodTickSystem(guardianWoodManager));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register GuardianWoodTickSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(poumonLoutreTickSystem);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register PoumonLoutreTickSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(rodeurSylvestreTickSystem);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register RodeurSylvestreTickSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(new RodeurSylvestreFallSystem(rodeurSylvestreTickSystem));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register RodeurSylvestreFallSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(yeuxLynxTickSystem);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register YeuxLynxTickSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(yeuxHibouTickSystem);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register YeuxHibouTickSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(litDeFortuneTickSystem);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register LitDeFortuneTickSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(new FarmerBlockBreakSystem(professionManager, farmerComboTracker, guardianCropManager));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register FarmerBlockBreakSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(new GuardianCropTickSystem(guardianCropManager));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register GuardianCropTickSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(farmerPickupHarvestSystem);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register FarmerPickupHarvestSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(new FarmerPlaceCropSystem(professionManager));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register FarmerPlaceCropSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(new BagCraftRestrictionSystem(professionManager));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register BagCraftRestrictionSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(new TalentItemRestrictionSystem(professionManager));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register TalentItemRestrictionSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(new TalentItemPlaceRestrictionSystem(professionManager));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register TalentItemPlaceRestrictionSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(farmerAnimalDropSystem.new AttackTagger());
            getEntityStoreRegistry().registerSystem(farmerAnimalDropSystem.new DropOnDeath());
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register FarmerAnimalDropSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(chasseurKillSystem.new AttackTagger());
            getEntityStoreRegistry().registerSystem(chasseurKillSystem.new DropOnDeath());
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register ChasseurKillSystem");
        }

        if (mobParticipantsTracker != null) {
            try {
                getEntityStoreRegistry().registerSystem(mobParticipantsTracker.createRecorderSystem());
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] register MobParticipantsTracker");
            }
        }

        if (classKillXpSystem != null) {
            try {
                getEntityStoreRegistry().registerSystem(classKillXpSystem.new KillPredictor());
                getEntityStoreRegistry().registerSystem(classKillXpSystem.new ParticipantDeathXp());
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] register ClassKillXpSystem");
            }
        }

        try {
            getEntityStoreRegistry().registerSystem(new ChasseurGuardianTickSystem(chasseurGuardianManager));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register ChasseurGuardianTickSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(new ChasseurPickupSystem(professionManager, chasseurComboTracker));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register ChasseurPickupSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(rodeurDunesTickSystem);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register RodeurDunesTickSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(new RodeurDunesFallSystem(rodeurDunesTickSystem));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register RodeurDunesFallSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(secondSouffleTickSystem);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register SecondSouffleTickSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(new MaitriseMineurDamageSystem(professionManager));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register MaitriseMineurDamageSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(maitriseMineurSpeedSystem);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register MaitriseMineurSpeedSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(maitriseForestierSpeedSystem);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register MaitriseForestierSpeedSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(new MaitriseForestierDamageSystem(professionManager));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register MaitriseForestierDamageSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(maitriseForestierStaminaSystem);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register MaitriseForestierStaminaSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(maitriseFermierSpeedSystem);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register MaitriseFermierSpeedSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(new MaitriseFermierDamageSystem(professionManager));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register MaitriseFermierDamageSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(maitriseFermierRegenSystem);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register MaitriseFermierRegenSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(new MaitriseChasseurDamageSystem(professionManager));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register MaitriseChasseurDamageSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(maitriseChasseurSpeedSystem);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register MaitriseChasseurSpeedSystem");
        }

        try {
            getEntityStoreRegistry().registerSystem(new MaitriseChasseurDamageBoostSystem(professionManager));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] register MaitriseChasseurDamageBoostSystem");
        }

        if (duellisteBleedSystem != null && duellisteState != null && classManager != null) {
            try {
                getEntityStoreRegistry().registerSystem(
                    new DuellisteOutgoingDamageSystem(classManager, duellisteState, duellisteBleedSystem));
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] register DuellisteOutgoingDamageSystem");
            }
            try {
                getEntityStoreRegistry().registerSystem(
                    new DuellisteIncomingDamageSystem(classManager, duellisteState));
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] register DuellisteIncomingDamageSystem");
            }
            try {
                getEntityStoreRegistry().registerSystem(
                    new DuellisteDesarmementDamageSystem(duellisteState));
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] register DuellisteDesarmementDamageSystem");
            }
            try {
                getEntityStoreRegistry().registerSystem(duellisteBleedSystem);
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] register DuellisteBleedSystem");
            }
            try {
                getEntityStoreRegistry().registerSystem(duellisteSpeedSystem);
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] register DuellisteSpeedSystem");
            }
        }

        if (ombreState != null && ombrePoisonSystem != null && classManager != null) {
            try {
                getEntityStoreRegistry().registerSystem(
                    new OmbreOutgoingDamageSystem(classManager, ombreState, ombrePoisonSystem));
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] register OmbreOutgoingDamageSystem");
            }
            try {
                getEntityStoreRegistry().registerSystem(
                    new OmbreIncomingDamageSystem(classManager, ombreState));
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] register OmbreIncomingDamageSystem");
            }
            try {
                getEntityStoreRegistry().registerSystem(ombrePoisonSystem);
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] register OmbrePoisonSystem");
            }
            try {
                getEntityStoreRegistry().registerSystem(ombreSpeedSystem);
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] register OmbreSpeedSystem");
            }
            // Injecter l'attitude provider stealth Ombre dans tous les mondes actifs et futurs
            final fr.varyon.vrpg.classes.ombre.OmbreStealthAttitudeProvider stealthProvider =
                new OmbreStealthAttitudeProvider(ombreState);
            try {
                for (World w : com.hypixel.hytale.server.core.universe.Universe.get().getWorlds().values()) {
                    injectOmbreStealthProvider(w, stealthProvider);
                }
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] inject OmbreStealthAttitudeProvider existing worlds");
            }
            try {
                getEventRegistry().registerGlobal(
                    com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent.class,
                    event -> injectOmbreStealthProvider(event.getWorld(), stealthProvider));
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] register StartWorldEvent for OmbreStealthAttitudeProvider");
            }
        }

        if (rempartState != null && classManager != null) {
            try {
                getEntityStoreRegistry().registerSystem(
                    new fr.varyon.vrpg.classes.rempart.RempartIncomingDamageSystem(classManager, rempartState));
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] register RempartIncomingDamageSystem");
            }
            try {
                getEntityStoreRegistry().registerSystem(
                    new fr.varyon.vrpg.classes.rempart.RempartOutgoingDamageSystem(classManager, rempartState));
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] register RempartOutgoingDamageSystem");
            }
        }

        if (berserkerState != null && classManager != null) {
            try {
                getEntityStoreRegistry().registerSystem(berserkerOutgoingDamageSystem);
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] register BerserkerOutgoingDamageSystem");
            }
            try {
                getEntityStoreRegistry().registerSystem(berserkerIncomingDamageSystem);
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] register BerserkerIncomingDamageSystem");
            }
            try {
                getEntityStoreRegistry().registerSystem(berserkerTickSystem);
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] register BerserkerTickSystem");
            }
        }

        if (ravageurState != null && classManager != null) {
            try {
                getEntityStoreRegistry().registerSystem(ravageurOutgoingDamageSystem);
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] register RavageurOutgoingDamageSystem");
            }
            try {
                getEntityStoreRegistry().registerSystem(ravageurIncomingDamageSystem);
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] register RavageurIncomingDamageSystem");
            }
        }

        if (classManager != null) {
            try {
                getEntityStoreRegistry().registerSystem(new SpecWeaponMasteryDamageSystem(classManager));
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] register SpecWeaponMasteryDamageSystem");
            }
        }
    }

    private static void injectOmbreStealthProvider(@Nonnull World world,
                                                    @Nonnull OmbreStealthAttitudeProvider provider) {
        try {
            world.execute(() -> {
                try {
                    com.hypixel.hytale.server.npc.NPCPlugin npcPlugin =
                        com.hypixel.hytale.server.npc.NPCPlugin.get();
                    if (npcPlugin == null) { LOGGER.atWarning().log("[StealthProvider] NPCPlugin null"); return; }
                    var store = world.getEntityStore().getStore();
                    com.hypixel.hytale.server.npc.blackboard.Blackboard blackboard =
                        store.getResource(npcPlugin.getBlackboardResourceType());
                    if (blackboard == null) { LOGGER.atWarning().log("[StealthProvider] Blackboard null world=" + world.getName()); return; }
                    int[] count = {0};
                    blackboard.forEachView(
                        com.hypixel.hytale.server.npc.blackboard.view.attitude.AttitudeView.class,
                        attitudeView -> { attitudeView.registerProvider(-1, provider); count[0]++; });
                    LOGGER.atInfo().log("[StealthProvider] injected into " + count[0] + " AttitudeViews in world=" + world.getName());
                } catch (Exception e) {
                    LOGGER.atWarning().withCause(e).log("[VaryonRPG] OmbreStealthAttitudeProvider inject world=" + world.getName());
                }
            });
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] OmbreStealthAttitudeProvider schedule world=" + world.getName());
        }
    }

    @Override
    protected void shutdown() {
        if (classSkillPacketFilter != null) {
            try {
                PacketAdapters.deregisterInbound(classSkillPacketFilter);
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] deregister ClassSkillKeyFilter");
            }
            classSkillPacketFilter = null;
        }
        if (classManager != null) {
            try {
                classManager.shutdown();
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("[VaryonRPG] shutdown ClassManager");
            }
        }
        if (professionManager != null) {
            try {
                professionManager.shutdown();
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("[VaryonRPG] shutdown ProfessionManager");
            }
        }
        if (guardianManager != null) guardianManager.clear();
        if (guardianCropManager != null) guardianCropManager.clear();
        if (guardianWoodManager != null) guardianWoodManager.clear();
        if (chasseurGuardianManager != null) chasseurGuardianManager.clear();
        if (veinCooldownTracker != null) veinCooldownTracker.clear();
        if (comboTracker != null) comboTracker.clear();
        if (farmerComboTracker != null) farmerComboTracker.clear();
        if (forestierComboTracker != null) forestierComboTracker.clear();
        if (chasseurComboTracker != null) chasseurComboTracker.clear();
        instance = null;
    }
}
