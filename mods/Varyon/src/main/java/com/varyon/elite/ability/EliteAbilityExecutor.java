package com.varyon.elite.ability;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.varyon.elite.EliteStats;
import com.varyon.elite.component.EliteComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Level;

/**
 * Applies an elite ability's effect once its telegraph has elapsed. Uses only stable
 * server APIs: {@code DamageSystems.executeDamage}, {@code Selector.selectNearbyEntities},
 * {@code TransformComponent.teleportPosition}, {@code EntityStatMap}, {@code NPCPlugin.spawnNPC}.
 */
public final class EliteAbilityExecutor {

    private static final com.hypixel.hytale.logger.HytaleLogger LOGGER =
            com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private static final String TELEGRAPH_PARTICLE = "Explosion_Small";
    private static final String IMPACT_PARTICLE = "Explosion_Medium";
    private static final String HEAL_PARTICLE = "Heal";

    private EliteAbilityExecutor() {}

    public static void telegraph(@Nonnull Store<EntityStore> store, @Nonnull ComponentAccessor<EntityStore> cb,
                                 @Nonnull org.joml.Vector3d at) {
        try {
            ParticleUtil.spawnParticleEffect(TELEGRAPH_PARTICLE, new org.joml.Vector3d(at), cb);
        } catch (Exception ignored) {
        }
    }

    public static void execute(@Nonnull EliteAbility ability, @Nonnull EliteComponent elite,
                               @Nonnull Ref<EntityStore> eliteRef, @Nonnull Store<EntityStore> store,
                               @Nonnull ComponentAccessor<EntityStore> cb,
                               @Nonnull org.joml.Vector3d elitePos, @Nonnull org.joml.Vector3d targetPos) {
        EliteAbilitySpec spec = EliteAbilitySpec.of(ability);
        int tier = elite.getTier();
        float power = spec.basePower * (1f + 0.15f * (tier - 1))
                * (elite.getStatBonus() > 0 ? (float) (1.0 + elite.getStatBonus()) : 1f);

        switch (ability) {
            case CHARGE_LEAP, LEAP, FORWARD_CHARGE -> {
                leapTo(store, eliteRef, targetPos);
                aoeDamage(store, cb, eliteRef, targetPos, spec.radius, power);
            }
            case DASH -> {
                org.joml.Vector3d dir = new org.joml.Vector3d(targetPos).sub(elitePos);
                if (dir.lengthSquared() > 0.001) {
                    dir.normalize().mul(Math.min(spec.maxRange, elitePos.distance(targetPos)));
                    leapTo(store, eliteRef, new org.joml.Vector3d(elitePos).add(dir));
                }
            }
            case DODGE_ROLL -> {
                org.joml.Vector3d away = new org.joml.Vector3d(elitePos).sub(targetPos);
                if (away.lengthSquared() > 0.001) {
                    away.normalize().mul(4.0);
                    leapTo(store, eliteRef, new org.joml.Vector3d(elitePos).add(away));
                }
            }
            case UPPERCUT, PILEDRIVER, MULTI_SLASH_SHORT, MULTI_SLASH_MEDIUM, MULTI_SLASH_LONG, BLEED ->
                    aoeDamage(store, cb, eliteRef, elitePos, spec.radius, power);
            case WAR_CRY -> {
                elite.setEnraged(true);
                impact(store, cb, elitePos);
            }
            case VOLLEY, ARROW_RAIN -> {
                impact(store, cb, targetPos);
                aoeDamage(store, cb, eliteRef, targetPos, spec.radius, power);
            }
            case HEALING_AURA -> healNearbyElites(store, cb, elitePos, spec.radius, maxHpFraction(0.06f, tier));
            case HEAL_LEAP -> {
                EntityStatMap sm = store.getComponent(eliteRef, EntityStatMap.getComponentType());
                if (sm != null) {
                    EliteStats.heal(sm, hpAmount(sm, 0.12f));
                }
                healParticle(store, cb, elitePos);
            }
            case SHIELD -> {
                EntityStatMap sm = store.getComponent(eliteRef, EntityStatMap.getComponentType());
                if (sm != null) {
                    EliteStats.heal(sm, hpAmount(sm, 0.08f));
                }
            }
            case SUMMON_MINIONS -> summonMinions(store, eliteRef, elitePos, 1 + tier / 2);
            default -> { /* passives handled elsewhere */ }
        }
    }

    private static void leapTo(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> eliteRef,
                               @Nonnull org.joml.Vector3d dest) {
        try {
            TransformComponent tc = store.getComponent(eliteRef, TransformComponent.getComponentType());
            if (tc != null) {
                tc.teleportPosition(dest);
            }
        } catch (Exception e) {
            LOGGER.at(Level.FINE).log("elite leap failed: " + e.getMessage());
        }
    }

    private static void aoeDamage(@Nonnull Store<EntityStore> store, @Nonnull ComponentAccessor<EntityStore> cb,
                                  @Nonnull Ref<EntityStore> eliteRef, @Nonnull org.joml.Vector3d center,
                                  double radius, float amount) {
        if (amount <= 0f || radius <= 0) {
            return;
        }
        impact(store, cb, center);
        final int eliteIdx = eliteRef.getIndex();
        try {
            Selector.selectNearbyEntities(store, new org.joml.Vector3d(center), radius, t -> {
                try {
                    if (t.getIndex() == eliteIdx) {
                        return;
                    }
                    var player = store.getComponent(t, com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
                    if (player == null && store.getComponent(t, NPCEntity.getComponentType()) == null) {
                        return;
                    }
                    if (player != null && player.getGameMode() == com.hypixel.hytale.protocol.GameMode.Creative) {
                        return;
                    }
                    if (store.getComponent(t, EliteComponent.getComponentType()) != null) {
                        return;
                    }
                    DamageSystems.executeDamage(t, store,
                            new Damage(new Damage.EntitySource(eliteRef), DamageCause.PHYSICAL, amount));
                } catch (Exception ignored) {
                }
            }, t -> t.getIndex() != eliteIdx);
        } catch (Exception e) {
            LOGGER.at(Level.FINE).log("elite AoE failed: " + e.getMessage());
        }
    }

    private static void healNearbyElites(@Nonnull Store<EntityStore> store, @Nonnull ComponentAccessor<EntityStore> cb,
                                         @Nonnull org.joml.Vector3d center, double radius, float fraction) {
        healParticle(store, cb, center);
        try {
            Selector.selectNearbyEntities(store, new org.joml.Vector3d(center), radius, t -> {
                try {
                    if (store.getComponent(t, EliteComponent.getComponentType()) == null) {
                        return;
                    }
                    EntityStatMap sm = store.getComponent(t, EntityStatMap.getComponentType());
                    if (sm != null) {
                        EliteStats.heal(sm, hpAmount(sm, fraction));
                    }
                } catch (Exception ignored) {
                }
            }, t -> true);
        } catch (Exception ignored) {
        }
    }

    private static void summonMinions(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> eliteRef,
                                      @Nonnull org.joml.Vector3d center, int count) {
        NPCEntity npc = store.getComponent(eliteRef, NPCEntity.getComponentType());
        if (npc == null) {
            return;
        }
        String typeId = npc.getNPCTypeId();
        String roleName = npc.getRoleName();
        if (typeId == null || roleName == null) {
            return;
        }
        for (int i = 0; i < count; i++) {
            double ang = (2 * Math.PI * i) / Math.max(1, count);
            org.joml.Vector3d pos = new org.joml.Vector3d(
                    center.x + Math.cos(ang) * 2.0, center.y, center.z + Math.sin(ang) * 2.0);
            try {
                NPCPlugin.get().spawnNPC(store, typeId, roleName, pos,
                        com.hypixel.hytale.math.vector.Rotation3f.ZERO);
            } catch (Exception e) {
                LOGGER.at(Level.FINE).log("elite summon failed: " + e.getMessage());
                return;
            }
        }
    }

    private static void impact(@Nonnull Store<EntityStore> store, @Nonnull ComponentAccessor<EntityStore> cb,
                               @Nonnull org.joml.Vector3d at) {
        try {
            ParticleUtil.spawnParticleEffect(IMPACT_PARTICLE, new org.joml.Vector3d(at), cb);
        } catch (Exception ignored) {
        }
    }

    private static void healParticle(@Nonnull Store<EntityStore> store, @Nonnull ComponentAccessor<EntityStore> cb,
                                     @Nonnull org.joml.Vector3d at) {
        try {
            ParticleUtil.spawnParticleEffect(HEAL_PARTICLE, new org.joml.Vector3d(at), cb);
        } catch (Exception ignored) {
        }
    }

    private static float hpAmount(@Nonnull EntityStatMap sm, float fraction) {
        var v = sm.get(com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes.getHealth());
        return v != null ? v.getMax() * fraction : 0f;
    }

    private static float maxHpFraction(float base, int tier) {
        return base + 0.01f * (tier - 1);
    }
}
