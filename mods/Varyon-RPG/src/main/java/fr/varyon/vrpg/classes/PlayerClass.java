package fr.varyon.vrpg.classes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum PlayerClass {
    GUERRIER("guerrier", "Guerrier", "Jobs_Icons/Warrior.png"),
    BARBARE ("barbare",  "Barbare",  "Jobs_Icons/Barbarian.png"),
    MAGE    ("mage",     "Mage",     "Jobs_Icons/Mage.png"),
    TIREUR  ("tireur",   "Tireur",   "Jobs_Icons/Archer.png");

    private final String id;
    private final String displayName;
    private final String iconPath;

    PlayerClass(@Nonnull String id, @Nonnull String displayName, @Nonnull String iconPath) {
        this.id = id;
        this.displayName = displayName;
        this.iconPath = iconPath;
    }

    @Nonnull public String getId()          { return id; }
    @Nonnull public String getDisplayName() { return displayName; }
    @Nonnull public String getIconPath()    { return iconPath; }

    @Nonnull
    public List<PlayerSpecialization> getSpecializations() {
        return Arrays.stream(PlayerSpecialization.values())
            .filter(s -> s.getParentClass() == this)
            .toList();
    }

    @Nonnull
    public String getDescription() {
        return switch (this) {
            case GUERRIER -> "Maitre du combat rapproché, robuste et polyvalent.";
            case BARBARE  -> "Combattant brutal, impitoyable et endurant.";
            case MAGE     -> "Lanceur de sorts, puissant à distance mais fragile.";
            case TIREUR   -> "Expert du combat à distance, précis et mobile.";
        };
    }

    private static final Map<String, PlayerClass> BY_ID =
        Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(PlayerClass::getId, c -> c));

    @Nullable
    public static PlayerClass fromId(@Nullable String id) {
        return id == null ? null : BY_ID.get(id);
    }
}
