package irai.mod.DynamicFloatingDamageFormatter;

import java.lang.reflect.Method;
import java.util.Locale;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelParticle;
import com.hypixel.hytale.server.core.asset.type.particle.config.WorldParticle;
import com.hypixel.hytale.server.core.meta.IMetaStore;
import com.hypixel.hytale.server.core.meta.MetaKey;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;

import irai.mod.reforge.Util.DamageNumberFormatter;

public final class DamageNumberMeta {
    public static final MetaKey<String> META_DAMAGE_KIND =
            Damage.META_REGISTRY.registerMetaObject(d -> "", false, "socketreforge:damage_kind", Codec.STRING);
    public static final MetaKey<Boolean> META_CRITICAL =
            Damage.META_REGISTRY.registerMetaObject(d -> Boolean.FALSE, false, "socketreforge:damage_crit", Codec.BOOLEAN);
    public static final MetaKey<Boolean> META_SKIP_COMBAT_TEXT =
            Damage.META_REGISTRY.registerMetaObject(d -> Boolean.FALSE, false, "socketreforge:damage_skip_text", Codec.BOOLEAN);

    private DamageNumberMeta() {}

    public static void markKind(Damage damage, String kindId) {
        if (damage == null || kindId == null || kindId.isBlank()) {
            return;
        }
        damage.putMetaObject(META_DAMAGE_KIND, kindId);
    }

    public static void markKind(Damage damage, DamageNumberFormatter.DamageKind kind) {
        if (damage == null || kind == null) {
            return;
        }
        markKind(damage, kind.name());
    }

    public static String readKindId(Damage damage) {
        if (damage == null) {
            return null;
        }
        Object raw = damage.getIfPresentMetaObject(META_DAMAGE_KIND);
        if (!(raw instanceof String value) || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public static DamageNumberFormatter.DamageKind readKind(Damage damage) {
        String value = readKindId(damage);
        if (value == null) {
            return null;
        }
        try {
            return DamageNumberFormatter.DamageKind.valueOf(value.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static void markCritical(Damage damage) {
        if (damage == null) {
            return;
        }
        damage.putMetaObject(META_CRITICAL, Boolean.TRUE);
    }

    public static boolean isCritical(Damage damage) {
        if (damage == null) {
            return false;
        }
        return Boolean.TRUE.equals(damage.getIfPresentMetaObject(META_CRITICAL));
    }

    public static boolean inferCriticalFromImpactVfx(Damage damage) {
        if (damage == null) {
            return false;
        }
        try {
            Object raw = damage.getIfPresentMetaObject(Damage.IMPACT_PARTICLES);
            if (raw == null) {
                return false;
            }
            if (raw instanceof Damage.Particles particles) {
                return scanParticleArrays(particles.getModelParticles(), particles.getWorldParticles());
            }
            return scanParticlesReflective(raw);
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean inferCriticalFromMetaStringScan(Damage damage) {
        if (damage == null) {
            return false;
        }
        try {
            boolean[] hit = {false};
            damage.forEachMetaObject(new IMetaStore.MetaEntryConsumer() {
                @Override
                public <T> void accept(int metaId, T value) {
                    if (hit[0] || value == null) {
                        return;
                    }
                    if (valueMentionsImpactCritical(value)) {
                        hit[0] = true;
                    }
                }
            });
            return hit[0];
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean valueMentionsImpactCritical(Object value) {
        String s = String.valueOf(value).toLowerCase(Locale.ROOT);
        if (s.contains("impact_critical")) {
            return true;
        }
        return s.contains("critical")
                && (s.contains("systemid") || s.contains("worldparticle") || s.contains("modelparticle"));
    }

    private static boolean scanParticlesReflective(Object raw) {
        try {
            Method gm = raw.getClass().getMethod("getModelParticles");
            Method gw = raw.getClass().getMethod("getWorldParticles");
            Object models = gm.invoke(raw);
            Object worlds = gw.invoke(raw);
            return scanParticleArrays(
                    models instanceof ModelParticle[] mp ? mp : null,
                    worlds instanceof WorldParticle[] wp ? wp : null);
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean scanParticleArrays(ModelParticle[] models, WorldParticle[] worlds) {
        if (models != null) {
            for (ModelParticle m : models) {
                if (m != null && systemIdImpliesCritical(m.getSystemId())) {
                    return true;
                }
            }
        }
        if (worlds != null) {
            for (WorldParticle w : worlds) {
                if (w != null && systemIdImpliesCritical(w.getSystemId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean systemIdImpliesCritical(String systemId) {
        if (systemId == null || systemId.isBlank()) {
            return false;
        }
        return systemId.toLowerCase(Locale.ROOT).contains("critical");
    }

    public static void markSkipCombatText(Damage damage) {
        if (damage == null) {
            return;
        }
        damage.putMetaObject(META_SKIP_COMBAT_TEXT, Boolean.TRUE);
    }

    public static boolean shouldSkipCombatText(Damage damage) {
        if (damage == null) {
            return false;
        }
        return Boolean.TRUE.equals(damage.getIfPresentMetaObject(META_SKIP_COMBAT_TEXT));
    }
}
