package fr.varyon.vrpg.ui.tabs.profession;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.vrpg.VaryonRpgPlugin;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.ClassProgress;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.ui.RpgUiAdmin;
import fr.varyon.vrpg.ui.profession.RpgProfessionUiState;

import javax.annotation.Nonnull;

public final class ClassAdminTab {

    private static final PlayerClass[] CLASS_ORDER = PlayerClass.values();

    private ClassAdminTab() {}

    public static void buildPanel(@Nonnull RpgProfessionUiState state,
                                  @Nonnull UICommandBuilder ui,
                                  @Nonnull UIEventBuilder ev) {
        PlayerRef target = RpgUiAdmin.adminTargetRef(state);
        PlayerClass playerClass = CLASS_ORDER[state.adminClassIndex];
        ui.set("#AdminClassName.TextSpans", Message.raw(playerClass.getDisplayName()));

        ClassManager mgr = VaryonRpgPlugin.getInstance().getClassManager();
        if (mgr != null && target != null) {
            mgr.ensureAccount(target.getUuid(), target.getUsername());
            ClassAccount acc = mgr.getAccount(target.getUuid());
            if (acc != null) {
                ClassProgress prog = acc.getProgress(playerClass);
                ui.set("#AdminClassLevel.TextSpans", Message.raw(String.valueOf(prog.getLevel())));
                ui.set("#AdminClassXp.TextSpans", Message.raw(prog.getXpInLevel() + " / " + prog.getXpToNextLevel()));

                ui.set("#AdminClassStatsContainer.Visible", true);
                ui.clear("#AdminClassStatsContainer");
                for (int i = 0; i < CLASS_ORDER.length; i++) {
                    PlayerClass c = CLASS_ORDER[i];
                    ClassProgress cp = acc.getProgress(c);
                    PlayerSpecialization spec = cp.getActiveSpec();
                    String specLabel = spec != null ? " — " + spec.getDisplayName() : "";
                    ui.append("#AdminClassStatsContainer", "CharacterTabAdminStatRow.ui");
                    ui.set("#AdminClassStatsContainer[" + i + "] #AdminStatRowName.TextSpans",
                        Message.raw(c.getDisplayName() + specLabel));
                    ui.set("#AdminClassStatsContainer[" + i + "] #AdminStatRowLevel.TextSpans",
                        Message.raw("Nv " + cp.getLevel()));
                }
            } else {
                ui.set("#AdminClassLevel.TextSpans", Message.raw("—"));
                ui.set("#AdminClassXp.TextSpans", Message.raw("—"));
            }
        } else {
            ui.set("#AdminClassLevel.TextSpans", Message.raw("—"));
            ui.set("#AdminClassXp.TextSpans", Message.raw("—"));
        }

        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminClassPrev",
            EventData.of("Action", "adminClassNav").append("Dir", "-1"), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminClassNext",
            EventData.of("Action", "adminClassNav").append("Dir", "1"), false);

        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminClassXp100",
            EventData.of("Action", "adminClassXp").append("Amount", "100"), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminClassXp1k",
            EventData.of("Action", "adminClassXp").append("Amount", "1000"), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminClassXp5k",
            EventData.of("Action", "adminClassXp").append("Amount", "5000"), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminClassXp10k",
            EventData.of("Action", "adminClassXp").append("Amount", "10000"), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminClassXp50k",
            EventData.of("Action", "adminClassXp").append("Amount", "50000"), false);

        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminClassLvlMinus5",
            EventData.of("Action", "adminClassLevel").append("Delta", "-5"), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminClassLvlMinus1",
            EventData.of("Action", "adminClassLevel").append("Delta", "-1"), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminClassLvlPlus1",
            EventData.of("Action", "adminClassLevel").append("Delta", "1"), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminClassLvlPlus5",
            EventData.of("Action", "adminClassLevel").append("Delta", "5"), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminClassLvlMax",
            EventData.of("Action", "adminClassLevel").append("Delta", "max"), false);

        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminClassResetTalents",
            EventData.of("Action", "adminClassResetTalents"), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminClassResetAll",
            EventData.of("Action", "adminClassResetAll"), false);

        ui.set("#AdminClassFeedback.Visible", false);
    }
}
