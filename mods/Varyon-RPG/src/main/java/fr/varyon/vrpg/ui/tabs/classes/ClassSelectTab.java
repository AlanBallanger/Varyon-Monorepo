package fr.varyon.vrpg.ui.tabs.classes;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;

import javax.annotation.Nonnull;

public final class ClassSelectTab {

    private ClassSelectTab() {}

    public static void build(@Nonnull UICommandBuilder uiBuilder,
                             @Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ClassSelectBackButton",
            EventData.of("Action", "tab").append("Tab", ClassTalentsTab.TAB_CLASSES),
            false
        );

        PlayerClass[] classes = PlayerClass.values();
        for (int i = 0; i < classes.length; i++) {
            PlayerClass cls = classes[i];
            String suffix = String.valueOf(i);

            uiBuilder.set("#ClassSelectCard" + suffix + "Name.TextSpans", Message.raw(cls.getDisplayName()));
            uiBuilder.set("#ClassSelectCard" + suffix + "Icon.ItemId", cls.getItemId());
            uiBuilder.set("#ClassSelectCard" + suffix + "Desc.TextSpans", Message.raw(cls.getDescription()));

            java.util.List<PlayerSpecialization> specs = cls.getSpecializations();
            for (int j = 0; j < 3; j++) {
                String specSuffix = suffix + "Spec" + j;
                if (j < specs.size()) {
                    PlayerSpecialization spec = specs.get(j);
                    uiBuilder.set("#ClassSelectCard" + specSuffix + "Name.TextSpans",
                        Message.raw(spec.getDisplayName()));
                    uiBuilder.set("#ClassSelectCard" + specSuffix + "Keys.TextSpans",
                        Message.raw(spec.getKeywords()));
                    uiBuilder.set("#ClassSelectCard" + specSuffix + "Icon.ItemId", spec.getItemId());
                }
            }

            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ClassSelectCard" + suffix + "Button",
                EventData.of("Action", "selectClass").append("ClassId", cls.getId()),
                false
            );
        }
    }
}
