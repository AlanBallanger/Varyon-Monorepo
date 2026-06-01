package fr.varyon.holograms.animation;

import org.joml.Vector3d;
import org.joml.Vector3f;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AnimationData {

    private final String name;
    private final List<Keyframe> keyframes;
    private final boolean loop;

    public AnimationData(@Nonnull String name, boolean loop) {
        this.name = name;
        this.keyframes = new ArrayList<>();
        this.loop = loop;
    }

    @Nonnull public String getName() { return name; }
    public boolean isLoop() { return loop; }

    public float getDuration() {
        return keyframes.isEmpty() ? 0f : keyframes.get(keyframes.size() - 1).getTime();
    }

    public void addKeyframe(@Nonnull Keyframe keyframe) {
        keyframes.add(keyframe);
        keyframes.sort(Comparator.comparingDouble(Keyframe::getTime));
    }

    @Nonnull public List<Keyframe> getKeyframes() { return new ArrayList<>(keyframes); }

    @Nullable
    public Keyframe sample(float time) {
        if (keyframes.isEmpty()) return null;
        if (keyframes.size() == 1) return keyframes.get(0);
        float duration = getDuration();
        if (duration <= 0f) return keyframes.get(0);
        if (loop && time > duration) time %= duration;
        else if (time >= duration) return keyframes.get(keyframes.size() - 1);
        if (time <= 0f) return keyframes.get(0);
        Keyframe prev = keyframes.get(0);
        for (int i = 1; i < keyframes.size(); i++) {
            Keyframe current = keyframes.get(i);
            if (time <= current.getTime()) {
                float segDur = current.getTime() - prev.getTime();
                if (segDur <= 0f) return prev;
                double t = (time - prev.getTime()) / segDur;
                return prev.interpolate(current, t);
            }
            prev = current;
        }
        return keyframes.get(keyframes.size() - 1);
    }

    @Nonnull
    public static AnimationData createFloat(@Nonnull String name, double amplitude, float duration) {
        AnimationData anim = new AnimationData(name, true);
        int steps = 8;
        for (int i = 0; i <= steps; i++) {
            float t = duration / steps * i;
            double y = Math.sin((double) i / steps * Math.PI * 2) * amplitude;
            anim.addKeyframe(new Keyframe(t, new Vector3d(0, y, 0), null, 1f, EasingType.EASE_IN_OUT));
        }
        return anim;
    }

    @Nonnull
    public static AnimationData createSpin(@Nonnull String name, float duration, int axis) {
        AnimationData anim = new AnimationData(name, true);
        float pitch = 0, yaw = 0, roll = 0;
        if (axis == 0) pitch = 360f;
        else if (axis == 1) yaw = 360f;
        else roll = 360f;
        anim.addKeyframe(new Keyframe(0f, null, new Vector3f(0, 0, 0), 1f, EasingType.LINEAR));
        anim.addKeyframe(new Keyframe(duration, null, new Vector3f(pitch, yaw, roll), 1f, EasingType.LINEAR));
        return anim;
    }

    @Nonnull
    public static AnimationData createPulse(@Nonnull String name, float minScale, float maxScale, float duration) {
        AnimationData anim = new AnimationData(name, true);
        anim.addKeyframe(new Keyframe(0f, null, null, minScale, EasingType.EASE_IN_OUT));
        anim.addKeyframe(new Keyframe(duration / 2f, null, null, maxScale, EasingType.EASE_IN_OUT));
        anim.addKeyframe(new Keyframe(duration, null, null, minScale, EasingType.EASE_IN_OUT));
        return anim;
    }

    @Nonnull
    public static AnimationData createBounce(@Nonnull String name, double height, float duration) {
        AnimationData anim = new AnimationData(name, true);
        anim.addKeyframe(new Keyframe(0f, new Vector3d(0, 0, 0), null, 1f, EasingType.EASE_OUT));
        anim.addKeyframe(new Keyframe(duration / 2f, new Vector3d(0, height, 0), null, 1f, EasingType.EASE_IN));
        anim.addKeyframe(new Keyframe(duration, new Vector3d(0, 0, 0), null, 1f, EasingType.BOUNCE));
        return anim;
    }

    @Nonnull
    public static AnimationData createSway(@Nonnull String name, double amplitude, float duration) {
        AnimationData anim = new AnimationData(name, true);
        int steps = 8;
        for (int i = 0; i <= steps; i++) {
            float t = duration / steps * i;
            double x = Math.sin((double) i / steps * Math.PI * 2) * amplitude;
            anim.addKeyframe(new Keyframe(t, new Vector3d(x, 0, 0), null, 1f, EasingType.EASE_IN_OUT));
        }
        return anim;
    }

    @Nonnull
    public static AnimationData createWobble(@Nonnull String name, float angle, float duration) {
        AnimationData anim = new AnimationData(name, true);
        int steps = 8;
        for (int i = 0; i <= steps; i++) {
            float t = duration / steps * i;
            float roll = (float)(Math.sin((double) i / steps * Math.PI * 2) * angle);
            anim.addKeyframe(new Keyframe(t, null, new Vector3f(0, 0, roll), 1f, EasingType.EASE_IN_OUT));
        }
        return anim;
    }

    @Nonnull
    public static AnimationData createShake(@Nonnull String name, double amplitude, float duration) {
        AnimationData anim = new AnimationData(name, true);
        anim.addKeyframe(new Keyframe(0f, new Vector3d(0, 0, 0), null, 1f, EasingType.LINEAR));
        anim.addKeyframe(new Keyframe(duration * 0.1f, new Vector3d(amplitude, 0, 0), null, 1f, EasingType.LINEAR));
        anim.addKeyframe(new Keyframe(duration * 0.2f, new Vector3d(-amplitude, 0, 0), null, 1f, EasingType.LINEAR));
        anim.addKeyframe(new Keyframe(duration * 0.3f, new Vector3d(amplitude, 0, 0), null, 1f, EasingType.LINEAR));
        anim.addKeyframe(new Keyframe(duration * 0.4f, new Vector3d(-amplitude, 0, 0), null, 1f, EasingType.LINEAR));
        anim.addKeyframe(new Keyframe(duration * 0.5f, new Vector3d(0, 0, 0), null, 1f, EasingType.LINEAR));
        anim.addKeyframe(new Keyframe(duration, new Vector3d(0, 0, 0), null, 1f, EasingType.LINEAR));
        return anim;
    }

    @Nonnull
    public static AnimationData createOrbit(@Nonnull String name, double radius, float duration) {
        AnimationData anim = new AnimationData(name, true);
        int steps = 16;
        for (int i = 0; i <= steps; i++) {
            float t = duration / steps * i;
            double angle = (double) i / steps * Math.PI * 2;
            anim.addKeyframe(new Keyframe(t, new Vector3d(Math.cos(angle) * radius, 0, Math.sin(angle) * radius), null, 1f, EasingType.LINEAR));
        }
        return anim;
    }

    @Nonnull
    public static AnimationData createWave(@Nonnull String name, double amplitude, float rollAngle, float duration) {
        AnimationData anim = new AnimationData(name, true);
        int steps = 8;
        for (int i = 0; i <= steps; i++) {
            float t = duration / steps * i;
            double sinVal = Math.sin((double) i / steps * Math.PI * 2);
            anim.addKeyframe(new Keyframe(t, new Vector3d(0, sinVal * amplitude, 0),
                new Vector3f(0, 0, (float)(sinVal * rollAngle)), 1f, EasingType.EASE_IN_OUT));
        }
        return anim;
    }

    @Nonnull
    public static AnimationData createFlip(@Nonnull String name, float duration, boolean fullRotation) {
        AnimationData anim = new AnimationData(name, true);
        if (fullRotation) {
            anim.addKeyframe(new Keyframe(0f, null, new Vector3f(0, 0, 0), 1f, EasingType.LINEAR));
            anim.addKeyframe(new Keyframe(duration, null, new Vector3f(360, 0, 0), 1f, EasingType.LINEAR));
        } else {
            anim.addKeyframe(new Keyframe(0f, null, new Vector3f(0, 0, 0), 1f, EasingType.EASE_IN_OUT));
            anim.addKeyframe(new Keyframe(duration / 2f, null, new Vector3f(180, 0, 0), 1f, EasingType.EASE_IN_OUT));
            anim.addKeyframe(new Keyframe(duration, null, new Vector3f(0, 0, 0), 1f, EasingType.EASE_IN_OUT));
        }
        return anim;
    }
}
