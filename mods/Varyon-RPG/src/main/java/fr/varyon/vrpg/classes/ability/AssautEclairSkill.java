package fr.varyon.vrpg.classes.ability;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;

public final class AssautEclairSkill {

    public static final String SKILL_ID       = "assaut_eclair";
    public static final String TALENT_NODE_ID = "duelliste_0";

    private static final float[] DAMAGE_FACTOR = {1.8f, 2.3f, 2.8f, 3.5f, 4.2f};
    private static final long[]  COOLDOWN_MS = {28000, 26000, 24000, 22000, 18000};
    private static final float[] STAMINA_COST = {6f, 7f, 8f, 9f, 10f};
    private static final float   DASH_SPEED   = 24f;
    private static final double  DASH_DISTANCE = 5.0;
    private static final double  HIT_RADIUS    = 1.8;

    private AssautEclairSkill() {}

    public static int   maxRank()                  { return DAMAGE_FACTOR.length; }
    public static float staminaCostForRank(int rank){ return STAMINA_COST[idx(rank)]; }
    public static long  cooldownMsForRank(int rank) { return COOLDOWN_MS[idx(rank)]; }
    public static float damageFactor(int rank)      { return DAMAGE_FACTOR[idx(rank)]; }

    public static float computeDamage(int rank, @Nonnull PlayerRef playerRef) {
        int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
        int base = weaponDmg > 0 ? weaponDmg : 1;
        return base * damageFactor(rank);
    }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, DAMAGE_FACTOR.length - 1)); }

    public static boolean execute(@Nonnull PlayerRef playerRef,
                                  @Nonnull Ref<EntityStore> entityRef,
                                  @Nonnull Store<EntityStore> store,
                                  @Nullable CommandBuffer<EntityStore> commandBuffer,
                                  int rank) {
        try {
            HeadRotation headRot = store.getComponent(entityRef, HeadRotation.getComponentType());
            TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (headRot == null || transform == null) return false;

            Vector3d dir = headRot.getDirection();
            Vector3d startPos = transform.getPosition();
            final PlayerRef finalPlayerRef = playerRef;

            ComponentAccessor<EntityStore> accessor = commandBuffer != null ? commandBuffer : store;
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Sword", "StabDashCharged", true, accessor);

            Velocity velocity = commandBuffer != null
                ? commandBuffer.getComponent(entityRef, Velocity.getComponentType())
                : store.getComponent(entityRef, Velocity.getComponentType());
            if (velocity != null) {
                double dx = dir.x, dz = dir.z;
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 1e-6) { dx /= len; dz /= len; }
                velocity.getInstructions().clear();
                velocity.addInstruction(
                    new Vector3d(dx * DASH_SPEED, 3.5, dz * DASH_SPEED),
                    null, ChangeVelocityType.Set);
            }

            float dmg = computeDamage(rank, playerRef);
            long casterIdx = entityRef.getIndex();
            HashSet<Long> hitSet = new HashSet<>();

            for (double t = 0.5; t <= DASH_DISTANCE; t += 0.8) {
                Vector3d sample = new Vector3d(
                    startPos.x + dir.x * t,
                    startPos.y + dir.y * t + 0.8,
                    startPos.z + dir.z * t);
                Selector.selectNearbyEntities(store, sample, HIT_RADIUS, targetRef -> {
                    try {
                        long tidx = targetRef.getIndex();
                        if (tidx == casterIdx || !hitSet.add(tidx)) return;
                        DamageSystems.executeDamage(targetRef, store,
                            new Damage(new Damage.EntitySource(entityRef), DamageCause.PHYSICAL, dmg));
                        if (commandBuffer != null) {
                            fr.varyon.vrpg.audio.ClassSkillSounds.playSkillSound(
                                fr.varyon.vrpg.audio.ClassSkillSounds.ASSAUT_ECLAIR_IMPACT,
                                finalPlayerRef, sample, commandBuffer);
                        }
                    } catch (Exception ignored) {}
                }, t2 -> t2.getIndex() != casterIdx);
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
