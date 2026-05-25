package com.varyon.comet.util;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;

public final class VecUtil {

    private VecUtil() {}

    public static org.joml.Vector3d toJoml(Vector3d v) {
        return new org.joml.Vector3d(v.x, v.y, v.z);
    }

    public static org.joml.Vector3f toJoml(Vector3f v) {
        return new org.joml.Vector3f(v.x, v.y, v.z);
    }

    public static org.joml.Vector3i toJoml(Vector3i v) {
        return new org.joml.Vector3i(v.x, v.y, v.z);
    }

    public static Vector3d toHytale(org.joml.Vector3d v) {
        return new Vector3d(v.x, v.y, v.z);
    }

    public static Vector3f toHytale(org.joml.Vector3f v) {
        return new Vector3f(v.x, v.y, v.z);
    }

    public static Vector3i toHytale(org.joml.Vector3i v) {
        return new Vector3i(v.x, v.y, v.z);
    }
}
