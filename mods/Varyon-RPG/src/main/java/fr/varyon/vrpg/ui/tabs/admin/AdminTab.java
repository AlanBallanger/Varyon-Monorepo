package fr.varyon.vrpg.ui.tabs.admin;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.vrpg.ui.RpgUiAdmin;
import fr.varyon.vrpg.ui.profession.RpgProfessionUiState;
import fr.varyon.vrpg.ui.tabs.profession.ClassAdminTab;
import fr.varyon.vrpg.ui.tabs.profession.ProfessionAdminTab;

import javax.annotation.Nonnull;
import java.util.List;

public final class AdminTab {

    public static final String SUB_PROFESSIONS = "professions";
    public static final String SUB_CLASSES = "classes";

    private AdminTab() {}

    public static void build(@Nonnull RpgProfessionUiState state,
                             @Nonnull UICommandBuilder ui,
                             @Nonnull UIEventBuilder ev) {
        List<PlayerRef> players = RpgUiAdmin.getOnlinePlayers();
        PlayerRef target = RpgUiAdmin.adminTargetRef(state);

        String targetName = target != null ? target.getUsername() : "—";
        ui.set("#AdminTargetName.TextSpans", Message.raw(targetName));
        ui.set("#AdminPlayerCount.TextSpans", Message.raw("(" + players.size() + " en ligne)"));

        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminPlayerPrev",
            EventData.of("Action", "adminPlayerNav").append("Dir", "-1"), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminPlayerNext",
            EventData.of("Action", "adminPlayerNav").append("Dir", "1"), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminPlayerSelf",
            EventData.of("Action", "adminPlayerSelf"), false);

        boolean isProfessions = SUB_PROFESSIONS.equals(state.adminSubTab);
        ui.set("#AdminSubTabProfessionsUnderline.Visible", isProfessions);
        ui.set("#AdminSubTabClassesUnderline.Visible", !isProfessions);

        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminSubTabProfessions",
            EventData.of("Action", "adminSubTab").append("Sub", SUB_PROFESSIONS), false);
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#AdminSubTabClasses",
            EventData.of("Action", "adminSubTab").append("Sub", SUB_CLASSES), false);

        ui.clear("#AdminContentMount");
        if (isProfessions) {
            ui.append("#AdminContentMount", "CharacterTabAdminProfessions.ui");
            ProfessionAdminTab.buildPanel(state, ui, ev);
        } else {
            ui.append("#AdminContentMount", "CharacterTabAdminClasses.ui");
            ClassAdminTab.buildPanel(state, ui, ev);
        }
    }
}
