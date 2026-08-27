package fr.varyon.vrpg.classes.rempart;

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
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;

public final class ChargeLourdeSkill {

    public static final String SKILL_ID       = "charge_lourde";
    public static final String TALENT_NODE_ID = "rempart_0";

    private static final double[] DASH_DISTANCE = {4, 5, 6, 7, 8, 9};
    private static final float[]  DAMAGE_PCT    = {1.2f, 1.5f, 1.8f, 2.1f, 2.4f, 3.0f};
    private static final long[]   COOLDOWN_MS   = {20000, 18000, 16000, 14000, 12000};
    private static final float[]  STAMINA_COST  = {6f, 7f, 8f, 9f, 10f, 10f};
    private static final float    DASH_SPEED    = 22f;
    private static final double   HIT_RADIUS    = 1.8;
    private static final float    KNOCKBACK_SPEED = 12f;
    private static final float    KNOCKBACK_UP    = 6f;

    private ChargeLourdeSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }
    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static double dashDistanceForRank(int rank) { return DASH_DISTANCE[idx(rank)]; }
    public static float  damagePctForRank(int rank)    { return DAMAGE_PCT[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)   { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank)  { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dist = (int) dashDistanceForRank(rank);
        int pct = Math.round(damagePctForRank(rank) * 100);
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        return "Charge " + dist + " blocs, " + pct + "% dégâts arme, Délai " + cd + "s";
    }

    public static float computeDamage(int rank, @Nonnull PlayerRef playerRef) {
        int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
        int base = weaponDmg > 0 ? weaponDmg : 1;
        return base * damagePctForRank(rank);
    }

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

            ComponentAccessor<EntityStore> accessor = commandBuffer != null ? commandBuffer : store;
            AnimationUtils.playAnimation(entityRef, AnimationSlot.Action, "Sword", "StabDashCharged", true, accessor);

            double dx = dir.x, dz = dir.z;
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 1e-6) { dx /= len; dz /= len; }

            Velocity velocity = commandBuffer != null
                ? commandBuffer.getComponent(entityRef, Velocity.getComponentType())
                : store.getComponent(entityRef, Velocity.getComponentType());
            if (velocity != null) {
                velocity.getInstructions().clear();
                velocity.addInstruction(
                    new Vector3d(dx * DASH_SPEED, 0.5, dz * DASH_SPEED),
                    null, ChangeVelocityType.Set);
            }

            float dmg = computeDamage(rank, playerRef);
            long casterIdx = entityRef.getIndex();
            double dist = dashDistanceForRank(rank);
            HashSet<Long> hitSet = new HashSet<>();
            final double fx = dx, fz = dz;

            for (double t = 0.5; t <= dist; t += 0.8) {
                Vector3d sample = new Vector3d(
                    startPos.x + fx * t,
                    startPos.y + 0.5,
                    startPos.z + fz * t);
                Selector.selectNearbyEntities(store, sample, HIT_RADIUS, targetRef -> {
                    try {
                        long tidx = targetRef.getIndex();
                        if (tidx == casterIdx || !hitSet.add(tidx)) return;
                        DamageSystems.executeDamage(targetRef, store,
                            new Damage(new Damage.EntitySource(entityRef), DamageCause.PHYSICAL, dmg));
                        Velocity targetVel = store.getComponent(targetRef, Velocity.getComponentType());
                        if (targetVel != null) {
                            targetVel.getInstructions().clear();
                            targetVel.addInstruction(
                                new Vector3d(fx * KNOCKBACK_SPEED, KNOCKBACK_UP, fz * KNOCKBACK_SPEED),
                                null, ChangeVelocityType.Set);
                        }
                    } catch (Exception ignored) {}
                }, t2 -> t2.getIndex() != casterIdx);
            }

            fr.varyon.vrpg.audio.ClassSkillSounds.playSkillSound(
                "SFX_Sword_T2_Lunge_Local", playerRef, startPos, commandBuffer);

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
