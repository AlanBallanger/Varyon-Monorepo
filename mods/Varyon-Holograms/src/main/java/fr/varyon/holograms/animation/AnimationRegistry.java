package fr.varyon.holograms.animation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AnimationRegistry {

    private final Map<String, AnimationData> animations = new HashMap<>();

    public AnimationRegistry() {
        register(AnimationData.createFloat("float", 0.15, 2.0f));
        register(AnimationData.createFloat("float_slow", 0.1, 3.0f));
        register(AnimationData.createFloat("float_fast", 0.2, 1.0f));
        register(AnimationData.createSpin("spin", 4.0f, 1));
        register(AnimationData.createSpin("spin_fast", 2.0f, 1));
        register(AnimationData.createSpin("spin_slow", 8.0f, 1));
        register(AnimationData.createSpin("tumble", 3.0f, 0));
        register(AnimationData.createPulse("pulse", 0.9f, 1.1f, 1.5f));
        register(AnimationData.createPulse("pulse_big", 0.8f, 1.2f, 2.0f));
        register(AnimationData.createPulse("heartbeat", 1.0f, 1.15f, 0.8f));
        register(AnimationData.createPulse("breathe", 0.95f, 1.05f, 3.0f));
        register(AnimationData.createBounce("bounce", 0.3, 1.0f));
        register(AnimationData.createBounce("bounce_small", 0.15, 0.8f));
        register(AnimationData.createSway("sway", 0.1, 2.0f));
        register(AnimationData.createSway("sway_big", 0.2, 2.5f));
        register(AnimationData.createWobble("wobble", 15.0f, 1.5f));
        register(AnimationData.createWobble("wobble_slow", 10.0f, 2.5f));
        register(AnimationData.createWobble("wobble_fast", 20.0f, 0.8f));
        register(AnimationData.createShake("shake", 0.03, 0.15f));
        register(AnimationData.createShake("shake_big", 0.15, 0.25f));
        register(AnimationData.createOrbit("orbit", 0.3, 3.0f));
        register(AnimationData.createOrbit("orbit_small", 0.15, 2.0f));
        register(AnimationData.createOrbit("orbit_fast", 0.3, 1.5f));
        register(AnimationData.createWave("wave", 0.1, 10.0f, 2.0f));
        register(AnimationData.createFlip("flip", 2.0f, false));
        register(AnimationData.createFlip("flip_full", 2.0f, true));
    }

    private void register(@Nonnull AnimationData anim) {
        animations.put(anim.getName().toLowerCase(), anim);
    }

    @Nullable
    public AnimationData getAnimation(@Nonnull String name) {
        return animations.get(name.toLowerCase());
    }

    public boolean hasAnimation(@Nonnull String name) {
        return animations.containsKey(name.toLowerCase());
    }

    @Nonnull
    public Map<String, AnimationData> getAllAnimations() {
        return Collections.unmodifiableMap(animations);
    }
}
