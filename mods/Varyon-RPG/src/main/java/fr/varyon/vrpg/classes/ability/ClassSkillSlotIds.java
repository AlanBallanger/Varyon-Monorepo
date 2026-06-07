package fr.varyon.vrpg.classes.ability;

import com.hypixel.hytale.protocol.InteractionType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ClassSkillSlotIds {

    public static final String E = "E";
    public static final String R = "R";
    public static final String A = "A";
    public static final String CROUCH_A = "CrouchA";
    public static final String CROUCH_E = "CrouchE";
    public static final String CROUCH_R = "CrouchR";

    public static final String[] ALL = {E, R, A, CROUCH_A, CROUCH_E, CROUCH_R};

    private ClassSkillSlotIds() {}

    @Nullable
    public static String forAbility(@Nonnull InteractionType type, boolean crouching) {
        return switch (type) {
            case Ability1 -> crouching ? CROUCH_A : A;
            case Ability2 -> crouching ? CROUCH_E : E;
            case Ability3 -> crouching ? CROUCH_R : R;
            default -> null;
        };
    }
}
