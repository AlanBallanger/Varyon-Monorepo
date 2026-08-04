package fr.varyon.vrpg.classes.rodeur;

import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RodeurTrapHelper {

    public static final class ActiveTrap {
        final int x, y, z;
        final long rootMs;
        final AtomicBoolean triggered = new AtomicBoolean(false);
        volatile ScheduledFuture<?> despawnTask;

        ActiveTrap(int x, int y, int z, long rootMs) {
            this.x = x; this.y = y; this.z = z;
            this.rootMs = rootMs;
        }

        public int x() { return x; }
        public int y() { return y; }
        public int z() { return z; }
        public long rootMs() { return rootMs; }
    }

    private static final CopyOnWriteArrayList<ActiveTrap> GLOBAL_TRAPS = new CopyOnWriteArrayList<>();
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rodeur-trap-scheduler");
        t.setDaemon(true);
        return t;
    });

    public static final String TRAP_BLOCK_ID = "Survival_Trap_Snapjaw";

    private RodeurTrapHelper() {}

    public static void activateTrap(@Nonnull World world,
                                    @Nonnull Vector3d aimPos,
                                    long durationMs,
                                    long rootMs) {
        int x = (int) Math.floor(aimPos.x);
        int z = (int) Math.floor(aimPos.z);
        int y = findGroundY(world, x, z, (int) Math.floor(aimPos.y) + 2);

        try {
            world.setBlock(x, y, z, TRAP_BLOCK_ID);
        } catch (Exception ignored) {
            return;
        }

        ActiveTrap trap = new ActiveTrap(x, y, z, rootMs);
        GLOBAL_TRAPS.add(trap);
        trap.despawnTask = SCHEDULER.schedule(() -> world.execute(() -> removeTrap(world, trap)),
            durationMs, TimeUnit.MILLISECONDS);
    }

    /** Appelé quand un ennemi déclenche le piège : le piège disparaît dès la fin du root plutôt qu'au bout des 10s. */
    public static void onTriggered(@Nonnull World world, @Nonnull ActiveTrap trap) {
        if (!trap.triggered.compareAndSet(false, true)) return;
        if (trap.despawnTask != null) trap.despawnTask.cancel(false);
        SCHEDULER.schedule(() -> world.execute(() -> removeTrap(world, trap)),
            trap.rootMs(), TimeUnit.MILLISECONDS);
    }

    private static void removeTrap(@Nonnull World world, @Nonnull ActiveTrap trap) {
        try {
            String currentId = String.valueOf(world.getBlockType(trap.x, trap.y, trap.z).getId());
            if (currentId.equalsIgnoreCase(TRAP_BLOCK_ID)) {
                world.setBlock(trap.x, trap.y, trap.z, "Empty");
            }
        } catch (Exception ignored) {}
        GLOBAL_TRAPS.remove(trap);
    }

    public static boolean hasActiveTrap() {
        return !GLOBAL_TRAPS.isEmpty();
    }

    @Nullable
    public static ActiveTrap trapAt(@Nonnull Vector3d pos) {
        int px = (int) Math.floor(pos.x);
        int py = (int) Math.floor(pos.y);
        int pz = (int) Math.floor(pos.z);
        for (ActiveTrap t : GLOBAL_TRAPS) {
            if (t.x() == px && (t.y() == py || t.y() == py - 1) && t.z() == pz) return t;
        }
        return null;
    }

    private static int findGroundY(@Nonnull World world, int x, int z, int searchTopY) {
        for (int y = searchTopY; y >= searchTopY - 64; y--) {
            if (!isSolidBlock(world, x, y, z)) continue;
            if (isSolidBlock(world, x, y + 1, z)) continue;
            return y + 1;
        }
        return searchTopY;
    }

    private static boolean isSolidBlock(@Nonnull World world, int x, int y, int z) {
        try {
            int blockId = world.getBlock(x, y, z);
            if (blockId == 0) return false;
            BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
            return blockType != null && blockType.getMaterial() == BlockMaterial.Solid;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
