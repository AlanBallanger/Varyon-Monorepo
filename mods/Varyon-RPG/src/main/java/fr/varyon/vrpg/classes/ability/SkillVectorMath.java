package fr.varyon.vrpg.classes.ability;

import org.joml.Vector3d;

public final class SkillVectorMath {

    private SkillVectorMath() {}

    public static boolean isFinite(Vector3d v) {
        return v != null
            && Double.isFinite(v.x)
            && Double.isFinite(v.y)
            && Double.isFinite(v.z);
    }

    public static Vector3d sanitize(Vector3d v, Vector3d fallback) {
        if (isFinite(v)) return v;
        return new Vector3d(fallback);
    }

    public static Vector3d horizontalDir(Vector3d dir, double fallbackX, double fallbackZ) {
        double dx = (dir != null) ? dir.x : 0.0;
        double dz = (dir != null) ? dir.z : 0.0;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (!Double.isFinite(len) || len < 1e-6) {
            return new Vector3d(fallbackX, 0.0, fallbackZ);
        }
        return new Vector3d(dx / len, 0.0, dz / len);
    }
}
