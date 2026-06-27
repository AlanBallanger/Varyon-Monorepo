package fr.varyon.vrpg.ui.classes;

import javax.annotation.Nullable;
import java.util.List;

public record SkillStatDisplay(
    List<SkillStatEntry> entries,
    @Nullable String fallbackText
) {
    public static final int MAX_SLOTS = 6;

    public static SkillStatDisplay of(List<SkillStatEntry> entries) {
        return new SkillStatDisplay(entries, null);
    }

    public static SkillStatDisplay fallback(@Nullable String text) {
        return new SkillStatDisplay(List.of(), text);
    }

    public boolean usesIconLayout() {
        return entries != null && !entries.isEmpty();
    }
}
