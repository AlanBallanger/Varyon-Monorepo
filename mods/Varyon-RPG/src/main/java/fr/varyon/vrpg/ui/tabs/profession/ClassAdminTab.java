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
import fr.varyon.vrpg.classes.ClassProfile;
import fr.varyon.vrpg.classes.ClassProgress;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.ui.RpgUiAdmin;
import fr.varyon.vrpg.ui.profession.RpgProfessionUiState;

import javax.annotation.Nonnull;

public final class ClassAdminTab {

    private ClassAdminTab() {}

    public static void buildPanel(@Nonnull RpgProfessionUiState state,
                                  @Nonnull UICommandBuilder ui,
                                  @Nonnull UIEventBuilder ev) {
        PlayerRef target = RpgUiAdmin.adminTargetRef(state);
        int profileIdx = Math.floorMod(state.adminProfileIndex, ClassProfile.COUNT);

        ClassManager mgr = VaryonRpgPlugin.getInstance().getClassManager();
        if (mgr != null && target != null) {
            mgr.ensureAccount(target.getUuid(), target.getUsername());
            ClassAccount acc = mgr.getOrLoad(target.getUuid());
            if (acc != null) {
                ClassProfile profile = acc.getProfiles()[profileIdx];
                PlayerClass profileClass = acc.resolveProfileActiveClass(profileIdx);

                String profileLabel = profile.getName();
                if (profileClass != null) profileLabel += " : " + profileClass.getDisplayName();
                ui.set("#AdminClassName.TextSpans", Message.raw(profileLabel));

                if (profileClass != null) {
                    ClassProgress prog = acc.resolveProfileProgress(profileIdx, profileClass);
                    ui.set("#AdminClassLevel.TextSpans", Message.raw(String.valueOf(prog.getLevel())));
                    ui.set("#AdminClassXp.TextSpans", Message.raw(prog.getXpInLevel() + " / " + prog.getXpToNextLevel()));
                } else {
                    ui.set("#AdminClassLevel.TextSpans", Message.raw("—"));
                    ui.set("#AdminClassXp.TextSpans", Message.raw("Aucune classe assignée"));
                }

                ui.set("#AdminClassStatsContainer.Visible", true);
                ui.clear("#AdminClassStatsContainer");
                ClassProfile[] profiles = acc.getProfiles();
                for (int i = 0; i < profiles.length; i++) {
                    ClassProfile p = profiles[i];
                    PlayerClass pc = acc.resolveProfileActiveClass(i);
                    String classLabel = pc != null ? pc.getDisplayName() : "Vide";
                    String levelLabel = pc != null
                        ? "Nv " + acc.resolveProfileProgress(i, pc).getLevel() : "—";
                    PlayerSpecialization spec = pc != null ? acc.resolveProfileSpec(i, pc) : null;
                    if (spec != null) classLabel += " — " + spec.getDisplayName();
                    ui.append("#AdminClassStatsContainer", "CharacterTabAdminStatRow.ui");
                    ui.set("#AdminClassStatsContainer[" + i + "] #AdminStatRowName.TextSpans",
                        Message.raw(p.getName() + " : " + classLabel));
                    ui.set("#AdminClassStatsContainer[" + i + "] #AdminStatRowLevel.TextSpans",
                        Message.raw(levelLabel));
                }
            } else {
                ui.set("#AdminClassName.TextSpans", Message.raw("—"));
                ui.set("#AdminClassLevel.TextSpans", Message.raw("—"));
                ui.set("#AdminClassXp.TextSpans", Message.raw("—"));
            }
        } else {
            ui.set("#AdminClassName.TextSpans", Message.raw("—"));
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
