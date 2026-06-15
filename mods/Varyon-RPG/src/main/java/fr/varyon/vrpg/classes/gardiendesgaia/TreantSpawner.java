package fr.varyon.vrpg.classes.gardiendesgaia;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class TreantSpawner {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String TREANT_NPC_ID   = "Treant";
    public static final long   SPAWN_DELAY_MS  = 500L;
    private static final String HP_MODIFIER_KEY = "vrpg_treant_hp";

    private TreantSpawner() {}

    public static void scheduleSpawn(@Nonnull World world,
                                     @Nonnull Vector3d pos,
                                     float hpFactor,
                                     float dmgFactor,
                                     @Nonnull UUID playerUuid,
                                     @Nonnull GardienDeGaiaState state) {
        final double x = pos.x, y = pos.y, z = pos.z;
        world.execute(() -> {
            try {
                Store<EntityStore> entityStore = world.getEntityStore().getStore();
                spawnTreant(entityStore, new Vector3d(x, y, z), hpFactor, dmgFactor, playerUuid, state);
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[AppelDuTreant] spawn ERREUR");
            }
        });
    }

    public static void spawnTreant(@Nonnull Store<EntityStore> store,
                                   @Nonnull Vector3d pos,
                                   float hpFactor,
                                   float dmgFactor,
                                   @Nonnull UUID playerUuid,
                                   @Nonnull GardienDeGaiaState state) {
        LOGGER.atInfo().log(String.format("[AppelDuTreant] spawnNPC Treant - pos=(%.0f, %.0f, %.0f)",
            pos.x, pos.y, pos.z));
        try {
            var pair = NPCPlugin.get().spawnNPC(store, TREANT_NPC_ID, null, pos, Rotation3f.ZERO);
            if (pair == null) {
                LOGGER.atWarning().log("[AppelDuTreant] spawnNPC null - verifier le role 'Treant'");
                return;
            }
            Ref<EntityStore> treantEntityRef = pair.left();

            EntityStatMap statMap = store.getComponent(treantEntityRef, EntityStatMap.getComponentType());
            if (statMap != null) {
                int healthIdx = DefaultEntityStatTypes.getHealth();
                if (hpFactor != 1.0f) {
                    statMap.putModifier(healthIdx, HP_MODIFIER_KEY,
                        new StaticModifier(Modifier.ModifierTarget.MAX,
                            StaticModifier.CalculationType.MULTIPLICATIVE, hpFactor));
                }
                statMap.maximizeStatValue(healthIdx);

                var hp = statMap.get(healthIdx);
                int hpMax = hp != null ? Math.round(hp.getMax()) : -1;
                int hpCur = hp != null ? Math.round(hp.get())    : -1;
                int estDmg = Math.round(10f * dmgFactor);
                LOGGER.atInfo().log(String.format("[AppelDuTreant] HP=%d  ATK≈%d/coup (factor=%.2f)", hpMax, estDmg, dmgFactor));
            } else {
                LOGGER.atWarning().log("[AppelDuTreant] EntityStatMap null sur le tréant");
            }

            state.setTreant(playerUuid, treantEntityRef, dmgFactor);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[AppelDuTreant] spawnNPC ERREUR pos=" + pos);
        }
    }
}
