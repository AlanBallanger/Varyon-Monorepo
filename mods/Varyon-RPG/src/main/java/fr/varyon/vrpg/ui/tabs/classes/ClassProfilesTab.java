package fr.varyon.vrpg.ui.tabs.classes;

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
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.ui.classes.RpgClassUiState;

import javax.annotation.Nonnull;

public final class ClassProfilesTab {

    private ClassProfilesTab() {}

    public static void build(@Nonnull PlayerRef playerRef,
                             @Nonnull RpgClassUiState state,
                             @Nonnull UICommandBuilder uiBuilder,
                             @Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ClassProfilesBackButton",
            EventData.of("Action", "tab").append("Tab", ClassTalentsTab.TAB_CLASSES),
            false
        );

        ClassManager classManager = VaryonRpgPlugin.getInstance().getClassManager();
        if (classManager == null) return;
        ClassAccount acc = classManager.getAccount(playerRef.getUuid());
        if (acc == null) return;

        int activeIdx = acc.getActiveProfileIndex();
        ClassProfile[] profiles = acc.getProfiles();
        for (int i = 0; i < ClassProfile.COUNT; i++) {
            ClassProfile p = profiles[i];
            boolean isActive = i == activeIdx;
            String s = String.valueOf(i);

            uiBuilder.set("#ClassProfile" + s + "Name.TextSpans", Message.raw(p.getName()));
            uiBuilder.set("#ClassProfile" + s + "ActiveBadge.Visible", isActive);
            uiBuilder.set("#ClassProfile" + s + "InactiveBadge.Visible", !isActive);

            PlayerClass cls = p.getActiveClass();
            if (cls != null) {
                uiBuilder.set("#ClassProfile" + s + "ClassName.TextSpans", Message.raw(cls.getDisplayName()));
                uiBuilder.set("#ClassProfile" + s + "ClassIcon.ItemId", cls.getItemId());
                PlayerSpecialization spec = p.getSpec(cls);
                if (spec != null) {
                    uiBuilder.set("#ClassProfile" + s + "SpecName.TextSpans", Message.raw(spec.getDisplayName()));
                } else {
                    uiBuilder.set("#ClassProfile" + s + "SpecName.TextSpans", Message.raw(""));
                }
            } else {
                uiBuilder.set("#ClassProfile" + s + "ClassName.TextSpans", Message.raw("Aucune classe"));
                uiBuilder.set("#ClassProfile" + s + "SpecName.TextSpans", Message.raw(""));
            }

            boolean isPending = state.pendingProfileIndex != null && state.pendingProfileIndex == i;
            uiBuilder.set("#ClassProfile" + s + "ConfirmPanel.Visible", isPending);
            uiBuilder.set("#ClassProfile" + s + "HoverButton.Visible", !isActive && !isPending);
            if (!isActive && !isPending) {
                eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ClassProfile" + s + "HoverButton",
                    EventData.of("Action", "pendingProfile").append("Index", s),
                    false
                );
            }
            if (isPending) {
                eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ClassProfile" + s + "ConfirmButton",
                    EventData.of("Action", "selectProfile").append("Index", s),
                    false
                );
            }
        }
    }
}
