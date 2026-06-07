package fr.varyon.vrpg.classes.ability;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Rotation3fc;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class AssautEclairSkill {

    public static final String SKILL_ID = "assaut_eclair";
    public static final String TALENT_NODE_ID = "0";

    private static final int[] BLOCKS = {3, 4, 5, 6, 8};
    private static final long[] COOLDOWN_MS = {30_000L, 28_000L, 26_000L, 24_000L, 20_000L};
    private static final float[] STAMINA_COST = {6f, 7f, 8f, 9f, 10f};

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
        TransformComponent tc = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (tc == null) return false;

        World world = store.getExternalData().getWorld();
        if (world == null) return false;

        int blocks = blocksForRank(rank);
        Vector3d pos = new Vector3d(tc.getPosition());
        HeadRotation headRot = store.getComponent(entityRef, HeadRotation.getComponentType());
        float yaw = headRot != null ? headRot.getRotation().y : 0f;
        Rotation3fc rot = headRot != null ? headRot.getRotation() : Rotation3f.ZERO;

        double dx = -Math.sin(yaw) * blocks;
        double dz = Math.cos(yaw) * blocks;
        Vector3d target = new Vector3d(pos.x + dx, pos.y, pos.z + dz);

        Teleport teleport = Teleport.createForPlayer(world, target, rot);
        if (commandBuffer != null) {
            commandBuffer.addComponent(entityRef, Teleport.getComponentType(), teleport);
        } else {
            store.addComponent(entityRef, Teleport.getComponentType(), teleport);
        }

        playerRef.sendMessage(Message.raw("Assaut éclair — " + blocks + " blocs"));
        return true;
    }
}
