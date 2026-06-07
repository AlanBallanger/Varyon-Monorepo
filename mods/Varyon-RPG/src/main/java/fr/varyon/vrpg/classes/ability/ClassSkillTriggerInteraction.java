package fr.varyon.vrpg.classes.ability;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;

import javax.annotation.Nonnull;

public final class ClassSkillTriggerInteraction extends SimpleInstantInteraction {

    public static final String TYPE_NAME = "vrpg_skill_trigger";
    public static final String INNER_ID = "*Vrpg_Trigger_Inner";

    public static final BuilderCodec<ClassSkillTriggerInteraction> CODEC =
        BuilderCodec.builder(ClassSkillTriggerInteraction.class, ClassSkillTriggerInteraction::new,
            SimpleInstantInteraction.CODEC).build();

    public ClassSkillTriggerInteraction(@Nonnull String id) {
        super(id);
    }

    protected ClassSkillTriggerInteraction() {}

    @Override
    protected void firstRun(@Nonnull InteractionType type,
                            @Nonnull InteractionContext context,
                            @Nonnull CooldownHandler cooldownHandler) {}
}
