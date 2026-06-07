package fr.varyon.vrpg.classes.ability;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.ui.XpNotifHud;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class AssautEclairSkill {

    public static final String SKILL_ID = "assaut_eclair";
    public static final String TALENT_NODE_ID = "0";

    private static final int[] BLOCKS = {3, 4, 5, 6, 8};
    private static final long[] COOLDOWN_MS = {30_000L, 28_000L, 26_000L, 24_000L, 20_000L};
    private static final float[] STAMINA_COST = {6f, 7f, 8f, 9f, 10f};
    private static final double[] DASH_FORCE = {20.0, 24.0, 28.0, 34.0, 42.0};

    private AssautEclairSkill() {}

    public static int maxRank() {
        return BLOCKS.length;
    }

    public static int blocksForRank(int rank) {
        return BLOCKS[Math.max(0, Math.min(rank - 1, BLOCKS.length - 1))];
    }

    public static long cooldownMsForRank(int rank) {
        return COOLDOWN_MS[Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1))];
    }

    public static float staminaCostForRank(int rank) {
        return STAMINA_COST[Math.max(0, Math.min(rank - 1, STAMINA_COST.length - 1))];
    }

    public static boolean execute(@Nonnull PlayerRef playerRef,
                                  @Nonnull Ref<EntityStore> entityRef,
                                  @Nonnull Store<EntityStore> store,
                                  @Nullable CommandBuffer<EntityStore> commandBuffer,
                                  int rank) {
        HeadRotation headRot = store.getComponent(entityRef, HeadRotation.getComponentType());
        if (headRot == null) return false;

        Velocity velocity = commandBuffer != null
                ? commandBuffer.getComponent(entityRef, Velocity.getComponentType())
                : store.getComponent(entityRef, Velocity.getComponentType());
        if (velocity == null) return false;

        int blocks = blocksForRank(rank);
        double force = dashForceForRank(rank);
        float yaw = headRot.getRotation().y;

        double vx = Math.sin(yaw) * force;
        double vz = -Math.cos(yaw) * force;
        Vector3d dashVelocity = new Vector3d(vx, 0.0, vz);

        velocity.addInstruction(dashVelocity, new VelocityConfig(), ChangeVelocityType.Add);

        XpNotifHud hud = XpNotifHud.get(playerRef.getUuid());
        if (hud != null) hud.showBurst("Assaut Éclair", "Weapon_Sword_Mithril");
        return true;
    }

    private static double dashForceForRank(int rank) {
        return DASH_FORCE[Math.max(0, Math.min(rank - 1, DASH_FORCE.length - 1))];
    }
}
