package fr.varyon.vrpg.profession;

import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import org.joml.Vector3d;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class BlockUtil {

    private BlockUtil() {}

    @Nullable
    public static Vector3d blockCenter(@Nonnull BreakBlockEvent event) {
        Vector3i t = event.getTargetBlock();
        if (t == null) return null;
        return new Vector3d(t.x + 0.5, t.y + 0.5, t.z + 0.5);
    }

    @Nonnull
    public static Vector3d blockCenter(int x, int y, int z) {
        return new Vector3d(x + 0.5, y + 0.5, z + 0.5);
    }
}
