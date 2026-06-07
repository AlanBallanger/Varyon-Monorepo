package fr.varyon.vrpg.ui.tabs.classes;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import javax.annotation.Nonnull;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.vrpg.VaryonRpgPlugin;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.ClassPlayerStats;
import fr.varyon.vrpg.classes.ClassProgress;
import fr.varyon.vrpg.classes.ClassStatDefinition;
import fr.varyon.vrpg.classes.ClassTalentTree;
import fr.varyon.vrpg.classes.ClassXpCurve;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.ui.RpgUiStyles;
import fr.varyon.vrpg.ui.classes.ClassUnlockedActiveSkills;
import fr.varyon.vrpg.ui.classes.RpgClassUiState;
import fr.varyon.vrpg.ui.classes.layout.ClassTalentTreeLayouts;

import javax.annotation.Nonnull;
import java.util.List;

public final class ClassesTab {

    public static final String TAB_CLASSES = ClassTalentsTab.TAB_CLASSES;

    private ClassesTab() {}

    public static void build(@Nonnull PlayerRef playerRef,
                             @Nonnull RpgClassUiState state,
                             @Nonnull UICommandBuilder uiBuilder,
                             @Nonnull UIEventBuilder eventBuilder) {
        ClassManager classManager = VaryonRpgPlugin.getInstance().getClassManager();
        if (classManager == null) return;
        ClassAccount acc = classManager.getAccount(playerRef.getUuid());
        if (acc == null) return;

        PlayerClass activeClass = acc.getActiveClass();

        if (activeClass == null) {
            uiBuilder.set("#ClassesNoClassCard.Visible", true);
            uiBuilder.set("#ClassesHasClassCard.Visible", false);
            uiBuilder.set("#ClassesSpec0Card.Visible", false);
            uiBuilder.set("#ClassesSpec1Card.Visible", false);
            uiBuilder.set("#ClassesSpec2Card.Visible", false);
            uiBuilder.set("#ClassesSpecSectionTitle.Visible", false);
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ClassesSelectClassButton",
                EventData.of("Action", "tab").append("Tab", "classselect"),
                false
            );
            return;
        }

        uiBuilder.set("#ClassesSpec0Card.Visible", true);
        uiBuilder.set("#ClassesSpec1Card.Visible", true);
        uiBuilder.set("#ClassesSpec2Card.Visible", true);
        uiBuilder.set("#ClassesSpecSectionTitle.Visible", true);
        uiBuilder.set("#ClassesNoClassCard.Visible", false);
        uiBuilder.set("#ClassesHasClassCard.Visible", true);

        ClassProgress activeProgress = acc.getProgress(activeClass);
        PlayerSpecialization activeSpec = activeProgress.getActiveSpec();

        uiBuilder.set("#ClassesActiveClassName.TextSpans", Message.raw(activeClass.getDisplayName()));
        uiBuilder.set("#ClassesActiveClassLevel.TextSpans",
            Message.raw("NIVEAU " + activeProgress.getLevel()));
        long xpIn = activeProgress.getXpInLevel();
        long xpNeeded = ClassXpCurve.xpForLevel(activeProgress.getLevel());
        String xpText = xpIn + " / " + (activeProgress.isMaxLevel() ? "MAX" : xpNeeded + " XP");
        uiBuilder.set("#ClassesActiveClassXp.TextSpans", Message.raw(xpText));
        float xpRatio = xpNeeded > 0 ? (float) xpIn / xpNeeded : 1f;
        uiBuilder.set("#ClassesActiveClassXpBar.Value", xpRatio);
        uiBuilder.set("#ClassesActiveClassIcon.ItemId", activeClass.getItemId());

        if (activeSpec != null) {
            uiBuilder.set("#ClassesActiveSpecName.TextSpans", Message.raw(activeSpec.getDisplayName()));
            uiBuilder.set("#ClassesActiveSpecKeywords.TextSpans", Message.raw(activeSpec.getKeywords()));
            uiBuilder.set("#ClassesActiveSpecIcon.ItemId", activeSpec.getItemId());
        } else {
            uiBuilder.set("#ClassesActiveSpecName.TextSpans", Message.raw("Aucune"));
            uiBuilder.set("#ClassesActiveSpecKeywords.TextSpans", Message.raw(""));
        }

        ClassPlayerStats activeStats = classManager.getStatEngine().getStats(playerRef.getUuid());
        if (activeStats == null) {
            activeStats = ClassStatDefinition.compute(activeProgress.getLevel(), activeSpec);
        }

        int displayHp = readLiveStatMax(playerRef, true, activeStats.maxHp());
        int displayStamina = readLiveStatMax(playerRef, false, activeStats.maxStamina());
        int displayAtk = readLiveAtk(playerRef, activeStats.atk());

        uiBuilder.set("#ClassesStatPv.TextSpans",    Message.raw(String.valueOf(displayHp)));
        uiBuilder.set("#ClassesStatDef.TextSpans",   Message.raw(activeStats.armorPct() + "%"));
        uiBuilder.set("#ClassesStatEnd.TextSpans",   Message.raw(String.valueOf(displayStamina)));
        uiBuilder.set("#ClassesStatAtk.TextSpans",   Message.raw(String.valueOf(displayAtk)));
        uiBuilder.set("#ClassesStatCrit.TextSpans",  Message.raw(activeStats.critChancePct() + "%"));
        uiBuilder.set("#ClassesStatDcrit.TextSpans", Message.raw("+" + activeStats.critDamagePct() + "%"));

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ClassesChangeProfileButton",
            EventData.of("Action", "tab").append("Tab", "classprofiles"),
            false
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ClassesViewTalentsButton",
            EventData.of("Action", "tab").append("Tab", "classtree"),
            false
        );

        java.util.List<PlayerSpecialization> specs = activeClass.getSpecializations();
        for (int i = 0; i < 3; i++) {
            String suffix = String.valueOf(i);
            if (i < specs.size()) {
                PlayerSpecialization spec = specs.get(i);
                boolean isActive = spec == activeSpec;
                uiBuilder.set("#ClassesSpec" + suffix + "Name.TextSpans", Message.raw(spec.getDisplayName()));
                uiBuilder.set("#ClassesSpec" + suffix + "Keywords.TextSpans", Message.raw(spec.getKeywords()));
                uiBuilder.set("#ClassesSpec" + suffix + "Icon.ItemId", spec.getItemId());
                uiBuilder.setObject("#ClassesSpec" + suffix + "Card.Background",
                    isActive ? RpgUiStyles.CARD_BG_ACTIVE_STYLE : RpgUiStyles.CARD_BG_INACTIVE_STYLE);
                uiBuilder.set("#ClassesSpec" + suffix + "StatPv.TextSpans",    specDiffMsg(spec.getHpMult()));
                uiBuilder.set("#ClassesSpec" + suffix + "StatDef.TextSpans",   specDiffMsg(spec.getArmorMult()));
                uiBuilder.set("#ClassesSpec" + suffix + "StatEnd.TextSpans",   specDiffMsg(spec.getStaminaMult()));
                uiBuilder.set("#ClassesSpec" + suffix + "StatAtk.TextSpans",   specDiffMsg(spec.getAtkMult()));
                uiBuilder.set("#ClassesSpec" + suffix + "StatCrit.TextSpans",  specDiffMsg(spec.getCritChanceMult()));
                uiBuilder.set("#ClassesSpec" + suffix + "StatDcrit.TextSpans", specDiffMsg(spec.getCritDamageMult()));

                boolean isPending = spec.getId().equals(state.pendingSpecId);
                uiBuilder.set("#ClassesSpec" + suffix + "ConfirmPanel.Visible", isPending);
                uiBuilder.set("#ClassesSpec" + suffix + "HoverButton.Visible", !isActive && !isPending);
                if (!isActive && !isPending) {
                    eventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#ClassesSpec" + suffix + "HoverButton",
                        EventData.of("Action", "pendingSpec").append("SpecId", spec.getId()),
                        false
                    );
                }
                if (isPending) {
                    eventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#ClassesSpec" + suffix + "ConfirmButton",
                        EventData.of("Action", "selectSpec").append("SpecId", spec.getId()),
                        false
                    );
                }
            }
        }

        ClassTalentTree.Node[] talentNodes = ClassTalentTreeLayouts.talentNodes(acc);
        List<ClassUnlockedActiveSkills.Entry> unlockedActives = ClassUnlockedActiveSkills.list(acc);

        String[] slotIds = {"E", "R", "CrouchA", "CrouchE", "CrouchR", "A"};
        for (String slotId : slotIds) {
            String assigned = state.skillSlotAssignments.get(slotId);
            boolean shown = false;
            if (assigned != null) {
                for (ClassTalentTree.Node n : talentNodes) {
                    if (n.itemId().equals(assigned)) {
                        uiBuilder.set("#SkillSlot" + slotId + "Icon.ItemId", n.itemId());
                        uiBuilder.set("#SkillSlot" + slotId + "Icon.Visible", true);
                        shown = true;
                        break;
                    }
                }
            }
            if (!shown) {
                uiBuilder.set("#SkillSlot" + slotId + "Icon.Visible", false);
            }
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                "#SkillSlot" + slotId,
                EventData.of("Action", "skillSlotClick").append("Slot", slotId), false);
        }

        uiBuilder.clear("#ClassesSkillPickerList");
        if (activeSpec == null) {
            uiBuilder.set("#ClassesSkillPickerLabel.TextSpans",
                Message.raw("Choisissez une spécialisation pour débloquer des compétences actives"));
        } else if (unlockedActives.isEmpty()) {
            uiBuilder.set("#ClassesSkillPickerLabel.TextSpans",
                Message.raw("Aucune compétence active débloquée"));
        } else {
            uiBuilder.set("#ClassesSkillPickerLabel.TextSpans",
                Message.raw("Compétences disponibles"));
            for (int i = 0; i < unlockedActives.size(); i++) {
                ClassTalentTree.Node n = unlockedActives.get(i).node();
                uiBuilder.append("#ClassesSkillPickerList", "CharacterTabClassTalents_SkillEntry.ui");
                uiBuilder.set("#ClassesSkillPickerList[" + i + "] #SkillEntryIcon.ItemId", n.itemId());
                uiBuilder.set("#ClassesSkillPickerList[" + i + "] #SkillEntryName.TextSpans", Message.raw(n.name()));
                uiBuilder.set("#ClassesSkillPickerList[" + i + "] #SkillEntryAssign.Visible", state.selectedSkillSlot != null);
                if (state.selectedSkillSlot != null) {
                    EventData assignEvent = EventData.of("Action", "skillSlotAssign")
                        .append("Slot", state.selectedSkillSlot)
                        .append("Node", n.itemId());
                    eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                        "#ClassesSkillPickerList[" + i + "]", assignEvent, false);
                    eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                        "#ClassesSkillPickerList[" + i + "] #SkillEntryAssign", assignEvent, false);
                }
            }
        }
    }

    private static int hIdx = Integer.MIN_VALUE;
    private static int sIdx = Integer.MIN_VALUE;

    private static int healthIndex() {
        if (hIdx == Integer.MIN_VALUE) {
            try { hIdx = DefaultEntityStatTypes.getHealth(); } catch (Exception e) { hIdx = -1; }
        }
        return hIdx;
    }

    private static int staminaIndex() {
        if (sIdx == Integer.MIN_VALUE) {
            try { sIdx = DefaultEntityStatTypes.getStamina(); } catch (Exception e) { sIdx = -1; }
        }
        return sIdx;
    }

    private static int readLiveStatMax(@Nonnull PlayerRef playerRef, boolean hp, int fallback) {
        try {
            EntityStatMap statMap = playerRef.getComponent(EntityStatMap.getComponentType());
            if (statMap == null) return fallback;
            int idx = hp ? healthIndex() : staminaIndex();
            if (idx < 0) return fallback;
            var stat = statMap.get(idx);
            if (stat == null) return fallback;
            double max = stat.getMax();
            return max > 0.5 ? (int) Math.round(max) : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static int readLiveAtk(@Nonnull PlayerRef playerRef, int vrpgAtk) {
        try {
            EntityStatMap statMap = playerRef.getComponent(EntityStatMap.getComponentType());
            if (statMap == null) return vrpgAtk;
            var dmgStat = statMap.get("damage");
            if (dmgStat != null && dmgStat.getMax() > 0.5) {
                double weaponBase = dmgStat.getMax();
                double atkMult = vrpgAtk / 10.0;
                return (int) Math.max(1, Math.round(weaponBase * atkMult));
            }
        } catch (Exception ignored) {}
        return vrpgAtk;
    }

    static Message specDiffMsg(double mult) {
        int pct = (int) Math.round((mult - 1.0) * 100);
        String text = (pct > 0 ? "+" : "") + pct + "%";
        java.awt.Color color = pct > 0
            ? new java.awt.Color(0x6B, 0xCB, 0x7A)
            : pct < 0
                ? new java.awt.Color(0xFF, 0x66, 0x66)
                : new java.awt.Color(0xC8, 0xBE, 0xB0);
        return Message.raw(text).color(color);
    }
}
