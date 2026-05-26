package com.varyon.bossarena.util;

import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;

public final class VecUtil {

    private VecUtil() {}

    public static Vector3d toJoml(Vector3d v) { return v; }
    public static Vector3f toJoml(Vector3f v) { return v; }
    public static Vector3i toJoml(Vector3i v) { return v; }
    public static Vector3d toHytale(Vector3d v) { return v; }
    public static Vector3f toHytale(Vector3f v) { return v; }
    public static Vector3i toHytale(Vector3i v) { return v; }

    public static float getYaw(Vector3f rotation) {
        return rotation != null ? rotation.x : 0f;
    }
}
