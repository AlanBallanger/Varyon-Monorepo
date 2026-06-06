package fr.varyon.vrpg.ui.tabs.profession;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import fr.varyon.vrpg.VaryonRpgPlugin;
import fr.varyon.vrpg.rpg.LeaderboardEntry;
import fr.varyon.vrpg.rpg.Profession;
import fr.varyon.vrpg.rpg.ProfessionManager;
import fr.varyon.vrpg.rpg.XpCurve;
import fr.varyon.vrpg.ui.profession.ProfessionUiUtil;
import fr.varyon.vrpg.ui.profession.RpgProfessionUiState;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClassementTab {

    private ClassementTab() {}

    public static void build(@Nonnull RpgProfessionUiState state,
                             @Nonnull UICommandBuilder uiBuilder,
                             @Nonnull UIEventBuilder eventBuilder) {
        for (Profession p : Profession.values()) {
            String capId = ProfessionUiUtil.capitalize(p.getId());
            uiBuilder.set("#ClassementUnderline" + capId + ".Visible", p == state.classementFilter);
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ClassementBtn" + capId,
                EventData.of("Action", "classementFilter").append("ProfessionId", p.getId()), false);
        }

        uiBuilder.set("#ClassementOrderLabel.Text", state.classementAsc ? "Ordre : ASC" : "Ordre : DESC");
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ClassementOrderBtn",
            EventData.of("Action", "classementOrder"), false);

        ProfessionManager mgr = VaryonRpgPlugin.getInstance().getProfessionManager();
        List<LeaderboardEntry> entries = mgr != null ? mgr.getLeaderboard(state.classementFilter) : Collections.emptyList();
        if (state.classementAsc) {
            entries = new ArrayList<>(entries);
            Collections.reverse(entries);
        }

        uiBuilder.clear("#ClassementListContainer");
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntry entry = entries.get(i);
            uiBuilder.append("#ClassementListContainer", "CharacterTabClassementEntry.ui");
            String eid = "#ClassementListContainer[" + i + "]";
            uiBuilder.set(eid + " #BgOdd.Visible", i % 2 == 1);
            if (i == 0) {
                uiBuilder.set(eid + " #Rank.Visible", false);
                uiBuilder.set(eid + " #RankGold.Text", "1");
                uiBuilder.set(eid + " #RankGold.Visible", true);
            } else if (i == 1) {
                uiBuilder.set(eid + " #Rank.Visible", false);
                uiBuilder.set(eid + " #RankSilver.Text", "2");
                uiBuilder.set(eid + " #RankSilver.Visible", true);
            } else if (i == 2) {
                uiBuilder.set(eid + " #Rank.Visible", false);
                uiBuilder.set(eid + " #RankBronze.Text", "3");
                uiBuilder.set(eid + " #RankBronze.Visible", true);
            } else {
                uiBuilder.set(eid + " #Rank.Text", String.valueOf(i + 1));
            }
            uiBuilder.set(eid + " #PlayerName.Text", entry.playerName());
            uiBuilder.set(eid + " #Level.Text", "Niv. " + entry.level());
            uiBuilder.set(eid + " #XpTotal.Text", ProfessionUiUtil.formatXp(XpCurve.cumulativeXp(entry.level(), entry.xpInLevel())));
        }
    }
}
