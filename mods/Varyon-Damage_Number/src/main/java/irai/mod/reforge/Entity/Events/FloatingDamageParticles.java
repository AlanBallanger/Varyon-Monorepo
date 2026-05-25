package irai.mod.reforge.Entity.Events;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import irai.mod.DynamicFloatingDamageFormatter.DamageNumberMeta;
import irai.mod.DynamicFloatingDamageFormatter.DamageNumbers;

public final class FloatingDamageParticles {

    private static final double HEIGHT_ABOVE_ENTITY = 1.85;
    private static final double DIGIT_SPACING = 0.1;
    private static final double ICON_SLOT_WIDTH = 0.138;
    private static final double ICON_NUDGE_TOWARD_DIGITS = 0.055;
    /** Extra world-space gap between icon and first digit when the number is short (narrow group). */
    private static final double ICON_TO_DIGIT_GAP_PER_MISSING_DIGIT = 0.023;
    /** Scale nudge down for 1–2 digit amounts so the burst sits less into the first digit. */
    private static final double ICON_NUDGE_SHORT_NUMBER_SCALE = 0.28;
    private static final double HORIZONTAL_GROUP_JITTER = 0.26;
    private static final double DISTANCE_SCALE_REFERENCE = 6.0;
    private static final float DISTANCE_SCALE_MIN = 1.0f;
    private static final float DISTANCE_SCALE_MAX = 3.5f;

    private FloatingDamageParticles() {}

    public static boolean trySpawn(Store<EntityStore> store,
                                   @Nullable CommandBuffer<EntityStore> commandBuffer,
                                   Ref<EntityStore> targetRef,
                                   float amount,
                                   @Nullable String kindId,
                                   @Nullable List<Ref<EntityStore>> viewerRefs,
                                   @Nullable Damage damage) {
        String resolved;
        if (damage != null) {
            resolved = DamageNumbers.resolveKindId(damage);
        } else {
            resolved = (kindId == null || kindId.isBlank()) ? "FLAT" : kindId;
        }
        DamageNumbers.KindStyle style = DamageNumbers.getKindStyle(resolved);
        if (style == null || style.particleFontId() == null || style.particleFontId().isBlank()) {
            return false;
        }
        ComponentType<EntityStore, TransformComponent> transformType = TransformComponent.getComponentType();
        TransformComponent transform = null;
        if (commandBuffer != null) {
            transform = commandBuffer.getComponent(targetRef, transformType);
        }
        if (transform == null) {
            transform = store.getComponent(targetRef, transformType);
        }
        if (transform == null) {
            return false;
        }
        int whole = (int) Math.floor(amount);
        if (whole <= 0) {
            return false;
        }
        String digits = Integer.toString(whole);
        String font = style.particleFontId().trim();
        String iconId = style.particleIconId();
        String iconSystem = (iconId != null && !iconId.isBlank()) ? iconId.trim() : null;
        font = resolveCriticalDigitFont(resolved, font, iconSystem, damage);

        Vector3d base = transform.getPosition();
        double y = base.y + HEIGHT_ABOVE_ENTITY;

        int digitCount = digits.length();
        int iconSlots = iconSystem != null ? 1 : 0;
        double iconToDigitGap = iconSlots > 0 ? iconToDigitGapAfterIcon(digitCount) : 0.0;
        double iconNudge = iconSlots > 0 ? iconNudgeTowardDigits() : 0.0;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double groupAlong = rng.nextDouble(-HORIZONTAL_GROUP_JITTER, HORIZONTAL_GROUP_JITTER);

        var accessor = commandBuffer != null ? commandBuffer : store;
        List<Ref<EntityStore>> receivers = sanitizedViewerRefs(viewerRefs);
        if (receivers.isEmpty()) {
            return false;
        }

        int spawnedFor = 0;
        try {
            for (Ref<EntityStore> viewerRef : receivers) {
                TransformComponent vt = commandBuffer != null
                        ? commandBuffer.getComponent(viewerRef, transformType) : null;
                if (vt == null) {
                    vt = store.getComponent(viewerRef, transformType);
                }
                if (vt == null) {
                    continue;
                }
                double[] right = new double[2];
                Vector3d vpos = vt.getPosition();
                resolveHorizontalRight(base, vpos, transform, right);
                float distScale = distanceDisplayScale(vpos.getX(), vpos.getY(), vpos.getZ(), base.x, y, base.z);
                if (!spawnDigitBurst(iconSystem, font, digits, base, y, digitCount,
                        iconSlots, iconToDigitGap, iconNudge,
                        groupAlong, accessor, List.of(viewerRef), right[0], right[1], distScale)) {
                    return false;
                }
                spawnedFor++;
            }
        } catch (Throwable ignored) {
            return false;
        }
        return spawnedFor > 0;
    }

    private static List<Ref<EntityStore>> sanitizedViewerRefs(@Nullable List<Ref<EntityStore>> refs) {
        if (refs == null || refs.isEmpty()) {
            return List.of();
        }
        ArrayList<Ref<EntityStore>> out = new ArrayList<>(refs.size());
        for (Ref<EntityStore> ref : refs) {
            if (ref != null && ref.isValid()) {
                out.add(ref);
            }
        }
        return out;
    }

    private static float distanceDisplayScale(double vx, double vy, double vz, double tx, double ty, double tz) {
        double dx = tx - vx;
        double dy = ty - vy;
        double dz = tz - vz;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.25) {
            dist = 0.25;
        }
        double s = dist / DISTANCE_SCALE_REFERENCE;
        if (s < DISTANCE_SCALE_MIN) {
            s = DISTANCE_SCALE_MIN;
        }
        if (s > DISTANCE_SCALE_MAX) {
            s = DISTANCE_SCALE_MAX;
        }
        return (float) s;
    }

    private static boolean spawnDigitBurst(@Nullable String iconSystem,
                                           String digitFont,
                                           String digits,
                                           Vector3d base,
                                           double y,
                                           int digitCount,
                                           int iconSlots,
                                           double iconToDigitGap,
                                           double iconNudge,
                                           double groupAlong,
                                           ComponentAccessor<EntityStore> accessor,
                                           List<Ref<EntityStore>> playerRefs,
                                           double rightX,
                                           double rightZ,
                                           float particleScale) {
        double layout = particleScale <= 1e-4f ? 1.0 : particleScale;
        double iconSlotW = ICON_SLOT_WIDTH * layout;
        double digitSpacing = DIGIT_SPACING * layout;
        double gapAfterIcon = iconToDigitGap * layout;
        double iconNudgeScaled = iconNudge * layout;
        double groupJitterScaled = groupAlong * layout;

        double totalWidth = iconSlots * iconSlotW + gapAfterIcon + digitCount * digitSpacing;

        double cursor = -totalWidth / 2.0;
        try {
            if (iconSystem != null) {
                double along = cursor + iconSlotW / 2.0 + iconNudgeScaled + groupJitterScaled;
                double px = base.x + rightX * along;
                double pz = base.z + rightZ * along;
                spawnParticleScaled(iconSystem, px, y, pz, particleScale, playerRefs, accessor);
                cursor += iconSlotW + gapAfterIcon;
            }
            for (int i = 0; i < digits.length(); i++) {
                char c = digits.charAt(i);
                if (c < '0' || c > '9') {
                    continue;
                }
                String systemName = digitFont + "_Digit_" + c;
                double along = cursor + digitSpacing / 2.0 + groupJitterScaled;
                double px = base.x + rightX * along;
                double pz = base.z + rightZ * along;
                spawnParticleScaled(systemName, px, y, pz, particleScale, playerRefs, accessor);
                cursor += digitSpacing;
            }
        } catch (Throwable t) {
            return false;
        }
        return true;
    }

    private static void spawnParticleScaled(String systemName,
                                            double px,
                                            double py,
                                            double pz,
                                            float scale,
                                            List<Ref<EntityStore>> playerRefs,
                                            ComponentAccessor<EntityStore> accessor) {
        ParticleUtil.spawnParticleEffect(systemName, px, py, pz, 0f, 0f, 0f, scale, null, null,
                playerRefs, accessor);
    }

    private static final String FONT_FLAT = "FloatingDamage_FLAT";
    private static final String FONT_CRITICAL = "FloatingDamage_CRITICAL";

    private static String resolveCriticalDigitFont(String resolvedKind,
                                                 String font,
                                                 @Nullable String iconSystem,
                                                 @Nullable Damage damage) {
        String upper = resolvedKind == null ? "" : resolvedKind.toUpperCase(Locale.ROOT);
        if ("CRITICAL".equals(upper)) {
            return FONT_CRITICAL;
        }
        if (font == null || font.isBlank()) {
            return font;
        }
        if (!font.contains(FONT_FLAT)) {
            return font;
        }
        if (damage != null) {
            if (DamageNumberMeta.isCritical(damage)
                    || DamageNumberMeta.inferCriticalFromImpactVfx(damage)
                    || DamageNumberMeta.inferCriticalFromMetaStringScan(damage)) {
                return FONT_CRITICAL;
            }
        }
        if (iconSystem != null && iconSystem.contains("Icon_Critical")) {
            return FONT_CRITICAL;
        }
        return font;
    }

    private static double iconToDigitGapAfterIcon(int digitCount) {
        double gap = Math.max(0, 3 - digitCount) * ICON_TO_DIGIT_GAP_PER_MISSING_DIGIT;
        if (digitCount >= 3) {
            gap = Math.max(gap, ICON_TO_DIGIT_GAP_PER_MISSING_DIGIT);
        }
        return gap;
    }

    private static double iconNudgeTowardDigits() {
        return ICON_NUDGE_TOWARD_DIGITS * ICON_NUDGE_SHORT_NUMBER_SCALE;
    }

    private static void resolveHorizontalRight(Vector3d base,
                                               @Nullable Vector3d viewerPosition,
                                               TransformComponent targetTransform,
                                               double[] outRightXZ) {
        if (viewerPosition != null) {
            double fx = base.getX() - viewerPosition.getX();
            double fz = base.getZ() - viewerPosition.getZ();
            double len = Math.hypot(fx, fz);
            if (len > 1e-4) {
                fx /= len;
                fz /= len;
                outRightXZ[0] = -fz;
                outRightXZ[1] = fx;
                return;
            }
        }
        Vector3f rot = targetTransform.getRotation();
        double yawRad = Math.toRadians(rot.getYaw());
        outRightXZ[0] = -Math.cos(yawRad);
        outRightXZ[1] = -Math.sin(yawRad);
    }
}
