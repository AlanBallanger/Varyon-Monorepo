package fr.varyon.vrpg.ui.classes;

import javax.annotation.Nullable;

public record SkillStatEntry(
    SkillStatKind kind,
    String value,
    @Nullable String labelOverride
) {
    public SkillStatEntry(SkillStatKind kind, String value) {
        this(kind, value, null);
    }

    public String label() {
        return labelOverride != null ? labelOverride : kind.defaultLabel();
    }
}
