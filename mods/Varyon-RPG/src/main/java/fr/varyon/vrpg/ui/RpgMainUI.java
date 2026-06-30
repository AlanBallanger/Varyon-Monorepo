package fr.varyon.vrpg.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.ui.classes.ClassSkillUiSync;
import fr.varyon.vrpg.ui.classes.ClassTalentTreeLogic;
import fr.varyon.vrpg.ui.classes.RpgClassUiState;
import fr.varyon.vrpg.ui.events.AdminUiEvents;
import fr.varyon.vrpg.ui.events.ClassUiEvents;
import fr.varyon.vrpg.ui.events.ProfessionUiEvents;
import fr.varyon.vrpg.ui.tabs.admin.AdminTab;
import fr.varyon.vrpg.ui.events.UiEventResult;
import fr.varyon.vrpg.ui.profession.ProfessionSkillTreeLogic;
import fr.varyon.vrpg.ui.profession.RpgProfessionUiState;
import fr.varyon.vrpg.ui.tabs.classes.ClassProfilesTab;
import fr.varyon.vrpg.ui.tabs.classes.ClassSelectTab;
import fr.varyon.vrpg.ui.tabs.classes.ClassTalentsTab;
import fr.varyon.vrpg.ui.tabs.classes.ClassesTab;
import fr.varyon.vrpg.ui.tabs.profession.CharacterProfessionsTab;
import fr.varyon.vrpg.ui.tabs.profession.ClassementTab;
import fr.varyon.vrpg.ui.tabs.profession.ProfessionSkillsTab;
import fr.varyon.vrpg.ui.events.SettingsUiEvents;
import fr.varyon.vrpg.ui.tabs.SettingsTab;
import fr.varyon.vrpg.VaryonRpgPlugin;

import javax.annotation.Nonnull;

public final class RpgMainUI extends InteractiveCustomUIPage<RpgMainUI.Data> {

    private static final String TAB_CLASSES = ClassTalentsTab.TAB_CLASSES;

    private final PlayerRef playerRef;
    private final RpgProfessionUiState professionUi = new RpgProfessionUiState();
    private final RpgClassUiState classUi = new RpgClassUiState();
    private String activeTab = "character";

    public RpgMainUI(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, Data.CODEC);
        this.playerRef = playerRef;
    }

    public RpgMainUI(@Nonnull PlayerRef playerRef, @Nonnull String initialTab) {
        super(playerRef, CustomPageLifetime.CanDismiss, Data.CODEC);
        this.playerRef = playerRef;
        this.activeTab = initialTab;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder uiBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        boolean isAdmin = RpgUiAdmin.isAdmin(playerRef);
        ClassSkillUiSync.hydrate(playerRef.getUuid(), classUi);

        uiBuilder.append("CharacterPage.ui");
        uiBuilder.append("#ClassesTabMount", "CharacterTabClasses.ui");
        uiBuilder.append("#CharacterTabMount", "CharacterTabProfession.ui");
        uiBuilder.append("#SkillsTabMount", "CharacterTabSkills.ui");
        uiBuilder.append("#ClassTalentsTabMount", "CharacterTabClassTalents.ui");
        uiBuilder.append("#ClassSelectTabMount", "CharacterTabClassSelect.ui");
        uiBuilder.append("#ClassProfilesTabMount", "CharacterTabClassProfiles.ui");
        uiBuilder.append("#ArtisansTabMount", "CharacterTabArtisans.ui");
        uiBuilder.append("#ClassementTabMount", "CharacterTabClassement.ui");
        uiBuilder.append("#ParametresTabMount", "CharacterTabParametres.ui");
        if (isAdmin) {
            uiBuilder.append("#AdminTabMount", "CharacterTabAdmin.ui");
        }

        uiBuilder.set("#ClassesTabContent.Visible", TAB_CLASSES.equals(activeTab));
        uiBuilder.set("#CharacterTabContent.Visible", "character".equals(activeTab));
        uiBuilder.set("#SkillsTabContent.Visible", "skills".equals(activeTab));
        uiBuilder.set("#ClassTalentsTabContent.Visible", "classtree".equals(activeTab));
        uiBuilder.set("#ClassSelectTabContent.Visible", "classselect".equals(activeTab));
        uiBuilder.set("#ClassProfilesTabContent.Visible", "classprofiles".equals(activeTab));
        uiBuilder.set("#ArtisansTabContent.Visible", "artisans".equals(activeTab));
        uiBuilder.set("#ClassementTabContent.Visible", "classement".equals(activeTab));
        uiBuilder.set("#ParametresTabContent.Visible", "parametres".equals(activeTab));
        uiBuilder.set("#AdminTabContent.Visible", "admin".equals(activeTab));

        boolean classesActive = TAB_CLASSES.equals(activeTab) || "classtree".equals(activeTab)
            || "classselect".equals(activeTab) || "classprofiles".equals(activeTab);
        boolean characterActive = "character".equals(activeTab) || "skills".equals(activeTab);
        uiBuilder.set("#TabClassesUnderline.Visible", classesActive);
        uiBuilder.set("#TabCharacterUnderline.Visible", characterActive);
        uiBuilder.set("#TabArtisansUnderline.Visible", "artisans".equals(activeTab));
        uiBuilder.set("#TabClassementUnderline.Visible", "classement".equals(activeTab));
        uiBuilder.set("#TabParametresUnderline.Visible", "parametres".equals(activeTab));
        uiBuilder.set("#TabAdminUnderline.Visible", "admin".equals(activeTab));
        uiBuilder.set("#TabAdminButton.Visible", isAdmin);

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabClassesButton",
            EventData.of("Action", "tab").append("Tab", TAB_CLASSES), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabCharacterButton",
            EventData.of("Action", "tab").append("Tab", "character"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabArtisansButton",
            EventData.of("Action", "tab").append("Tab", "artisans"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabClassementButton",
            EventData.of("Action", "tab").append("Tab", "classement"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabParametresButton",
            EventData.of("Action", "tab").append("Tab", "parametres"), false);
        if (isAdmin) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabAdminButton",
                EventData.of("Action", "tab").append("Tab", "admin"), false);
        }

        if (TAB_CLASSES.equals(activeTab)) {
            ClassesTab.build(playerRef, classUi, uiBuilder, eventBuilder);
        } else if ("character".equals(activeTab)) {
            CharacterProfessionsTab.build(playerRef, professionUi, uiBuilder, eventBuilder);
        } else if ("skills".equals(activeTab)) {
            ProfessionSkillsTab.build(playerRef, professionUi, uiBuilder, eventBuilder);
        } else if ("classtree".equals(activeTab)) {
            ClassTalentsTab.build(playerRef, classUi, uiBuilder, eventBuilder);
        } else if ("classselect".equals(activeTab)) {
            ClassSelectTab.build(uiBuilder, eventBuilder);
        } else if ("classprofiles".equals(activeTab)) {
            ClassProfilesTab.build(playerRef, classUi, uiBuilder, eventBuilder);
        } else if ("classement".equals(activeTab)) {
            ClassementTab.build(professionUi, uiBuilder, eventBuilder);
        } else if ("parametres".equals(activeTab)) {
            SettingsTab.build(playerRef, uiBuilder, eventBuilder);
        } else if ("admin".equals(activeTab) && isAdmin) {
            AdminTab.build(professionUi, uiBuilder, eventBuilder);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull Data data) {
        super.handleDataEvent(ref, store, data);
        if (data.action == null) return;

        if ("tab".equals(data.action) && data.tab != null) {
            activeTab = data.tab;
            if ("skills".equals(data.tab) && data.talentSlot != null) {
                professionUi.talentTreeSlotIndex = "1".equals(data.talentSlot) ? 1 : 0;
            }
            professionUi.hoveredNode = -1;
            professionUi.selectedNode = 0;
            classUi.hoveredClassNode = -1;
            classUi.selectedClassNode = 0;
            ClassTalentTreeLogic.exitEditMode(classUi);
            ProfessionSkillTreeLogic.exitEditMode(professionUi);
            rebuild();
            return;
        }

        UiEventResult result = SettingsUiEvents.handle(playerRef, data);
        if (result == UiEventResult.NONE) {
            result = ProfessionUiEvents.handle(playerRef, professionUi, data);
        }
        if (result == UiEventResult.NONE) {
            result = AdminUiEvents.handle(playerRef, professionUi, data);
        }
        if (result == UiEventResult.NONE) {
            result = ClassUiEvents.handle(playerRef, classUi, tab -> activeTab = tab, data);
        }

        switch (result) {
            case REBUILD -> rebuild();
            case SETTINGS_UPDATE -> {
                UICommandBuilder cmd = new UICommandBuilder();
                if ("parametres".equals(activeTab)) {
                    VaryonRpgPlugin plugin = VaryonRpgPlugin.getInstance();
                    if (plugin != null && plugin.getUiPreferencesManager() != null) {
                        SettingsTab.applyOffsetDisplays(cmd,
                            plugin.getUiPreferencesManager().get(playerRef.getUuid()));
                    }
                }
                sendUpdate(cmd, null, false);
            }
            case HOVER_UPDATE -> {
                UICommandBuilder cmd = new UICommandBuilder();
                if ("skills".equals(activeTab)) {
                    ProfessionSkillsTab.applyHoverChrome(playerRef, professionUi, cmd);
                } else if ("classtree".equals(activeTab)) {
                    ClassTalentsTab.applyHoverChrome(playerRef, classUi, cmd);
                }
                sendUpdate(cmd, null, false);
            }
            case NONE -> {}
        }
    }

    public static final class Data {
        public static final BuilderCodec<Data> CODEC =
            BuilderCodec.builder(Data.class, Data::new)
                .addField(new KeyedCodec<>("Action", Codec.STRING),
                    (d, v) -> d.action = v,
                    d -> d.action)
                .addField(new KeyedCodec<>("Tab", Codec.STRING),
                    (d, v) -> d.tab = v,
                    d -> d.tab)
                .addField(new KeyedCodec<>("Node", Codec.STRING),
                    (d, v) -> d.node = v,
                    d -> d.node)
                .addField(new KeyedCodec<>("ProfessionId", Codec.STRING),
                    (d, v) -> d.professionId = v,
                    d -> d.professionId)
                .addField(new KeyedCodec<>("TalentSlot", Codec.STRING),
                    (d, v) -> d.talentSlot = v,
                    d -> d.talentSlot)
                .addField(new KeyedCodec<>("Dir", Codec.STRING),
                    (d, v) -> d.dir = v,
                    d -> d.dir)
                .addField(new KeyedCodec<>("Amount", Codec.STRING),
                    (d, v) -> d.amount = v,
                    d -> d.amount)
                .addField(new KeyedCodec<>("Delta", Codec.STRING),
                    (d, v) -> d.delta = v,
                    d -> d.delta)
                .addField(new KeyedCodec<>("Index", Codec.STRING),
                    (d, v) -> d.index = v,
                    d -> d.index)
                .addField(new KeyedCodec<>("ClassId", Codec.STRING),
                    (d, v) -> d.classId = v,
                    d -> d.classId)
                .addField(new KeyedCodec<>("SpecId", Codec.STRING),
                    (d, v) -> d.specId = v,
                    d -> d.specId)
                .addField(new KeyedCodec<>("Sub", Codec.STRING),
                    (d, v) -> d.sub = v,
                    d -> d.sub)
                .addField(new KeyedCodec<>("Slot", Codec.STRING),
                    (d, v) -> d.slot = v,
                    d -> d.slot)
                .addField(new KeyedCodec<>("Filter", Codec.STRING),
                    (d, v) -> d.filter = v,
                    d -> d.filter)
                .addField(new KeyedCodec<>("Hovered", Codec.STRING),
                    (d, v) -> d.hovered = v,
                    d -> d.hovered)
                .addField(new KeyedCodec<>("Setting", Codec.STRING),
                    (d, v) -> d.setting = v,
                    d -> d.setting)
                .addField(new KeyedCodec<>("Corner", Codec.STRING),
                    (d, v) -> d.corner = v,
                    d -> d.corner)
                .addField(new KeyedCodec<>("@SliderValue", Codec.INTEGER),
                    (d, v) -> d.sliderValue = v,
                    d -> d.sliderValue)
                .build();

        public String action;
        public String tab;
        public String node;
        public String professionId;
        public String talentSlot;
        public String dir;
        public String amount;
        public String delta;
        public String index;
        public String classId;
        public String specId;
        public String sub;
        public String slot;
        public String filter;
        public String hovered;
        public String setting;
        public String corner;
        public Integer sliderValue;

        public Data() {}
    }
}
