package fr.varyon.vrpg.ui.tabs.profession;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.vrpg.VaryonRpgPlugin;
import fr.varyon.vrpg.rpg.PlayerAccount;
import fr.varyon.vrpg.rpg.Profession;
import fr.varyon.vrpg.rpg.ProfessionManager;
import fr.varyon.vrpg.rpg.ProfessionProgress;
import fr.varyon.vrpg.ui.RpgUiAdmin;
import fr.varyon.vrpg.ui.profession.ProfessionSkillTrees;
import fr.varyon.vrpg.ui.profession.RpgProfessionUiState;

import javax.annotation.Nonnull;

public final class ProfessionAdminTab {

    private ProfessionAdminTab() {}

    public static void buildPanel(@Nonnull RpgProfessionUiState state,
                                  @Nonnull UICommandBuilder ui,
                                  @Nonnull UIEventBuilder ev) {
        PlayerRef target = RpgUiAdmin.adminTargetRef(state);

        Profession prof = ProfessionSkillTrees.CATALOG_ORDER[state.adminProfIndex];
        ui.set("#AdminProfName.TextSpans", Message.raw(prof.getDisplayName()));

        ProfessionManager mgr = VaryonRpgPlugin.getInstance().getProfessionManager();
        if (mgr != null && target != null) {
            mgr.ensureAccount(target.getUuid(), target.getUsername());
            PlayerAccount acc = mgr.getAccount(target.getUuid());
            if (acc != null) {
                ProfessionProgress prog = acc.getProgress(prof);
                ui.set("#AdminProfLevel.TextSpans", Message.raw(String.valueOf(prog.getLevel())));
                ui.set("#AdminProfXp.TextSpans", Message.raw(prog.getXpInLevel() + " / " + prog.getXpToNextLevel()));

                ui.set("#AdminStatsContainer.Visible", true);
                ui.clear("#AdminStatsContainer");
                for (int i = 0; i < ProfessionSkillTrees.CATALOG_ORDER.length; i++) {
                    Profession p = ProfessionSkillTrees.CATALOG_ORDER[i];
                    ProfessionProgress pp = acc.getProgress(p);
                    ui.append("#AdminStatsContainer", "CharacterTabAdminStatRow.ui");
                    ui.set("#AdminStatsContainer[" + i + "] #AdminStatRowName.TextSpans", Message.raw(p.getDisplayName()));
                    ui.set("#AdminStatsContainer[" + i + "] #AdminStatRowLevel.TextSpans",
                        Message.raw("Nv " + pp.getLevel()));
                }
            } else {
                ui.set("#AdminProfLevel.TextSpans", Message.raw("—"));
                ui.set("#AdminProfXp.TextSpans", Message.raw("—"));
            }
        } else {
            ui.set("#AdminProfLevel.TextSpans", Message.raw("—"));
            ui.set("#AdminProfXp.TextSpans", Message.raw("—"));
        }

        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminProfPrev",
            EventData.of("Action", "adminProfNav").append("Dir", "-1"), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminProfNext",
            EventData.of("Action", "adminProfNav").append("Dir", "1"), false);

        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminXp100",
            EventData.of("Action", "adminXp").append("Amount", "100"), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminXp1k",
            EventData.of("Action", "adminXp").append("Amount", "1000"), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminXp5k",
            EventData.of("Action", "adminXp").append("Amount", "5000"), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminXp10k",
            EventData.of("Action", "adminXp").append("Amount", "10000"), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminXp50k",
            EventData.of("Action", "adminXp").append("Amount", "50000"), false);

        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminLvlMinus5",
            EventData.of("Action", "adminLevel").append("Delta", "-5"), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminLvlMinus1",
            EventData.of("Action", "adminLevel").append("Delta", "-1"), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminLvlPlus1",
            EventData.of("Action", "adminLevel").append("Delta", "1"), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminLvlPlus5",
            EventData.of("Action", "adminLevel").append("Delta", "5"), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminLvlMax",
            EventData.of("Action", "adminLevel").append("Delta", "max"), false);

        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminResetTalents",
            EventData.of("Action", "adminResetTalents"), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminResetAll",
            EventData.of("Action", "adminResetAll"), false);

        ui.set("#AdminFeedback.Visible", false);
    }
}
