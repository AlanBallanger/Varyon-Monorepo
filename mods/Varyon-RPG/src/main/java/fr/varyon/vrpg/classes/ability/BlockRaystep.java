package fr.varyon.vrpg.classes.ability;

import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

public final class BlockRaystep {

    private BlockRaystep() {}

    @Nonnull
    public static Vector3d hitPosition(@Nullable World world,
                                       @Nonnull Vector3d origin,
                                       @Nonnull Vector3d direction,
                                       double maxDistance,
                                       double stepIncrement) {
        if (world == null || maxDistance <= 0.0 || stepIncrement <= 0.0) {
            return new Vector3d(origin);
        }
        for (double t = stepIncrement; t <= maxDistance; t += stepIncrement) {
            double px = origin.x + direction.x * t;
            double py = origin.y + direction.y * t;
            double pz = origin.z + direction.z * t;
            int bx = (int) Math.floor(px);
            int by = (int) Math.floor(py);
            int bz = (int) Math.floor(pz);
            int blockId;
            try {
                blockId = world.getBlock(bx, by, bz);
            } catch (Throwable ignored) {
                continue;
            }
            if (blockId == 0) continue;
            BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
            if (blockType == null || blockType.getMaterial() != BlockMaterial.Solid) continue;
            return new Vector3d(px, by + 1.0, pz);
        }
        return new Vector3d(
            origin.x + direction.x * maxDistance,
            origin.y + direction.y * maxDistance,
            origin.z + direction.z * maxDistance);
    }
}
