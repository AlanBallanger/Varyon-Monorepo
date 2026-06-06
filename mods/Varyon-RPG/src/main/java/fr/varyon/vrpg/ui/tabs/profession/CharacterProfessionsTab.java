package fr.varyon.vrpg.ui.tabs.profession;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.vrpg.rpg.PlayerAccount;
import fr.varyon.vrpg.rpg.Profession;
import fr.varyon.vrpg.rpg.ProfessionProgress;
import fr.varyon.vrpg.rpg.XpBoost;
import fr.varyon.vrpg.ui.RpgUiStyles;
import fr.varyon.vrpg.ui.profession.ProfessionAccounts;
import fr.varyon.vrpg.ui.profession.ProfessionSkillTrees;
import fr.varyon.vrpg.ui.profession.ProfessionUiUtil;
import fr.varyon.vrpg.ui.profession.RpgProfessionUiState;

import javax.annotation.Nonnull;
import java.util.Locale;

public final class CharacterProfessionsTab {

    private CharacterProfessionsTab() {}

    public static void build(@Nonnull PlayerRef playerRef,
                             @Nonnull RpgProfessionUiState state,
                             @Nonnull UICommandBuilder uiBuilder,
                             @Nonnull UIEventBuilder eventBuilder) {
        PlayerAccount acc = ProfessionAccounts.get(playerRef);
        Profession[] activeSlots = new Profession[2];
        if (acc != null) {
            activeSlots[0] = acc.getActiveSlot0();
            activeSlots[1] = acc.getActiveSlot1();
        }
        for (int i = 0; i < 2; i++) {
            String p = "#ProfessionActiveCard" + i;
            Profession active = activeSlots[i];
            if (active != null && acc != null) {
                ProfessionProgress prog = acc.getProgress(active);
                uiBuilder.set(p + ".Visible", true);
                uiBuilder.set(p + "Name.TextSpans", Message.raw(active.getDisplayName().toUpperCase(Locale.FRENCH)));
                uiBuilder.set(p + "Icon.ItemId", active.getIconItemId());
                String levelBadge = "Niveau " + prog.getLevel();
                if (acc.availableTalentPoints(active) > 0) levelBadge += " *";
                uiBuilder.set(p + "LevelBadge.TextSpans", Message.raw(levelBadge.toUpperCase(Locale.FRENCH)));
                if (prog.isMaxLevel()) {
                    uiBuilder.set(p + "LevelXp.TextSpans", Message.raw("MAX"));
                    uiBuilder.set(p + "ProgBarFill.Value", 1.0);
                } else {
                    uiBuilder.set(p + "LevelXp.TextSpans",
                        Message.raw(prog.getXpInLevel() + " / " + prog.getXpToNextLevel() + " XP"));
                    ProfessionUiUtil.applyGaugeBar(uiBuilder, p + "ProgBarFill", prog.getXpInLevel(), prog.getXpToNextLevel());
                }
                uiBuilder.set(p + "Reconvert.Visible", true);
                uiBuilder.set(p + "ViewTalents.Visible", true);
                eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    p + "Reconvert",
                    EventData.of("Action", "professionReconvert")
                        .append("ProfessionId", active.getId())
                        .append("Node", Integer.toString(i)),
                    false
                );
                eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    p + "ViewTalents",
                    EventData.of("Action", "tab").append("Tab", "skills").append("TalentSlot", Integer.toString(i)),
                    false
                );
            } else {
                uiBuilder.set(p + ".Visible", false);
                uiBuilder.set(p + "Reconvert.Visible", false);
                uiBuilder.set(p + "ViewTalents.Visible", false);
            }
        }

        boolean selectMode = state.reconvertSourceId != null
            && Profession.fromId(state.reconvertSourceId) != null;
        if (!selectMode) state.reconvertSourceId = null;

        for (int i = 0; i < ProfessionSkillTrees.PROFESSION_CATALOG_SLOTS; i++) {
            String id = "#ProfessionCatalogCard" + i;
            Profession p = ProfessionSkillTrees.CATALOG_ORDER[i];
            uiBuilder.set(id + "Name.TextSpans", Message.raw(p.getDisplayName()));
            uiBuilder.set(id + "Icon.ItemId", p.getIconItemId());
            ProfessionProgress catProg = acc == null ? null : acc.getProgress(p);
            int level = catProg == null ? 1 : catProg.getLevel();
            boolean catMax = catProg != null && catProg.isMaxLevel();
            uiBuilder.set(id + "Level.TextSpans", Message.raw((catMax ? "MAX" : "Niveau " + level).toUpperCase(Locale.FRENCH)));
            if (catMax) {
                uiBuilder.set(id + "ProgBarFill.Value", 1.0);
            } else {
                long catXpInLevel = catProg == null ? 0L : catProg.getXpInLevel();
                long catXpToNext = catProg == null ? 0L : catProg.getXpToNextLevel();
                ProfessionUiUtil.applyGaugeBar(uiBuilder, id + "ProgBarFill", catXpInLevel, catXpToNext);
            }

            boolean selectable = selectMode
                && (!p.isSpecialized() || (acc != null && acc.isUnlocked(p)))
                && p != activeSlots[0]
                && p != activeSlots[1];
            uiBuilder.set(id + "Select.Visible", selectable);
            if (selectable) {
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                    id + "Select",
                    EventData.of("Action", "professionReconvertSelect")
                        .append("ProfessionId", p.getId()),
                    false);
            }

            boolean isActive = p == activeSlots[0] || p == activeSlots[1];
            uiBuilder.set(id + "Active.Visible", isActive);
            uiBuilder.set(id + "Inactive.Visible", !isActive);
            uiBuilder.setObject(id + ".Background", isActive ? RpgUiStyles.CARD_BG_ACTIVE_STYLE : RpgUiStyles.CARD_BG_INACTIVE_STYLE);

            if (p.isSpecialized()) {
                Profession parent = p.getPrereq();
                int need = p.getPrereqLevel();
                String parentName = parent == null ? "?" : parent.getDisplayName();
                boolean unlocked = acc != null && acc.isUnlocked(p);
                uiBuilder.set(id + "PrereqText.Visible", !unlocked);
                if (!unlocked) {
                    uiBuilder.set(id + "PrereqText.TextSpans",
                        Message.raw("Pr\u00e9requis : niveau " + need + " " + parentName));
                }
                uiBuilder.set(id + "Desc.Visible", unlocked);
                if (unlocked) {
                    uiBuilder.set(id + "Desc.TextSpans", Message.raw(p.getDescription()));
                }
            } else {
                uiBuilder.set(id + "PrereqText.Visible", false);
                uiBuilder.set(id + "Desc.Visible", true);
                uiBuilder.set(id + "Desc.TextSpans", Message.raw(p.getDescription()));
            }
        }

        for (int i = 0; i < ProfessionUiUtil.BOOST_PROFESSION_ORDER.length; i++) {
            Profession bp = ProfessionUiUtil.BOOST_PROFESSION_ORDER[i];
            String bid = "#BoostCard" + i;
            XpBoost boost = acc != null ? acc.getBoost(bp) : null;
            uiBuilder.set(bid + "Icon.ItemId", bp.getIconItemId());
            uiBuilder.set(bid + "Name.TextSpans", Message.raw(bp.getDisplayName()));
            uiBuilder.set(bid + "Info.Visible", boost != null);
            if (boost != null) {
                uiBuilder.set(bid + "Multiplier.TextSpans", Message.raw(ProfessionUiUtil.formatBoostMultiplier(boost.getBonus())));
                uiBuilder.set(bid + "Timer.TextSpans", Message.raw(ProfessionUiUtil.formatBoostTime(boost.getRemainingMs())));
            }
        }
    }
}
