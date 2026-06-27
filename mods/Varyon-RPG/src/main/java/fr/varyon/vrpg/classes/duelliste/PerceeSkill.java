package fr.varyon.vrpg.classes.duelliste;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;

public final class PerceeSkill {

    public static final String SKILL_ID       = "percee";
    public static final String TALENT_NODE_ID = "duelliste_3";

    private static final float[] BASE_DAMAGE  = {60f, 75f, 90f, 110f, 130f};
    private static final long[]  COOLDOWN_MS  = {20000, 19000, 18000, 17000, 15000};
    private static final float   FULL_HP_BONUS     = 0.30f;
    private static final float   FULL_HP_THRESHOLD = 0.80f;
    private static final float   DASH_SPEED        = 24f;
    private static final double  DASH_DISTANCE     = 5.0;
    private static final double  HIT_RADIUS        = 1.8;

    private static Integer healthIdx = null;

    private PerceeSkill() {}

    public static int maxRank() { return BASE_DAMAGE.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, BASE_DAMAGE.length - 1)); }

    public static float  baseDamageForRank(int rank) { return BASE_DAMAGE[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int base  = (int) baseDamageForRank(rank);
        int bonus = Math.round(FULL_HP_BONUS * 100);
        int cd    = (int) (cooldownMsForRank(rank) / 1000);
        return base + " dégâts, +" + bonus + "% si cible > 80% PV, Délai " + cd + "s";
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

            Velocity velocity = store.getComponent(entityRef, Velocity.getComponentType());
            if (velocity != null) {
                double dx = dir.x, dz = dir.z;
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 1e-6) { dx /= len; dz /= len; }
                velocity.getInstructions().clear();
                velocity.addInstruction(
                    new Vector3d(dx * DASH_SPEED, 3.5, dz * DASH_SPEED),
                    null, ChangeVelocityType.Set);
            }

            float baseDmg = baseDamageForRank(rank);
            long casterIdx = entityRef.getIndex();
            HashSet<Long> hitSet = new HashSet<>();
            DamageCause cause = DamageCause.PHYSICAL;

            for (double t = 0.5; t <= DASH_DISTANCE; t += 0.8) {
                Vector3d sample = new Vector3d(
                    startPos.x + dir.x * t,
                    startPos.y + dir.y * t + 0.8,
                    startPos.z + dir.z * t);
                Selector.selectNearbyEntities(store, sample, HIT_RADIUS, targetRef -> {
                    try {
                        long idx = targetRef.getIndex();
                        if (idx == casterIdx || !hitSet.add(idx)) return;
                        float dmg = baseDmg;
                        float hpPct = getHpPercent(targetRef, store);
                        if (hpPct > FULL_HP_THRESHOLD) dmg *= (1.0f + FULL_HP_BONUS);
                        DamageSystems.executeDamage(targetRef, store,
                            new Damage(new Damage.EntitySource(entityRef), cause, dmg));
                    } catch (Exception ignored) {}
                }, t2 -> t2.getIndex() != casterIdx);
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static float getHpPercent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        try {
            if (healthIdx == null) {
                try { healthIdx = DefaultEntityStatTypes.getHealth(); } catch (Exception e) { healthIdx = -1; }
            }
            if (healthIdx < 0) return 0f;
            EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
            if (stats == null) return 0f;
            var hp = stats.get(healthIdx);
            if (hp == null || hp.getMax() <= 0) return 0f;
            return hp.get() / hp.getMax();
        } catch (Exception e) { return 0f; }
    }
}
