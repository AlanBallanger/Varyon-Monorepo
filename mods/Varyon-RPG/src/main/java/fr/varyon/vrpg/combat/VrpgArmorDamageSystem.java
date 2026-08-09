package fr.varyon.vrpg.combat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.ClassPlayerStats;
import fr.varyon.vrpg.config.VrpgConfig;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Applique la réduction de dégâts (armure) affichée sur la fiche de classe.
 *
 * La valeur de {@code armorPct} etait calculee et affichee dans l'UI mais n'etait lue par
 * aucun systeme de degats : elle n'avait donc aucun effet en jeu, pour les 12 specialisations.
 *
 * <p>{@code armorPct} est utilise comme pourcentage de reduction direct (66 => 66% de
 * reduction), plafonne a {@link #MAX_REDUCTION} pour ne jamais rendre un joueur invulnerable.
 */
public final class VrpgArmorDamageSystem extends DamageEventSystem {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    /** Plafond de securite pour ne jamais rendre un joueur invulnerable. */
    private static final float MAX_REDUCTION = 0.75f;

    private final ClassManager classManager;

    public VrpgArmorDamageSystem(@Nonnull ClassManager classManager) {
        this.classManager = classManager;
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    /**
     * Reduction directe : armorPct => armorPct% de reduction, plafonnee a {@link #MAX_REDUCTION}.
     */
    public static float reductionForArmorScore(float armorScore) {
        if (armorScore <= 0f) return 0f;
        return Math.min(armorScore / 100f, MAX_REDUCTION);
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        try {
            if (damage.isCancelled() || damage.getAmount() <= 0f) return;

            // L'armure protege des coups recus, pas des degats de survie/environnement :
            // sinon un joueur bien equipe devient quasi insensible aux chutes et a la noyade.
            DamageCause cause = damage.getCause();
            if (cause == DamageCause.FALL
                || cause == DamageCause.DROWNING
                || cause == DamageCause.SUFFOCATION
                || cause == DamageCause.OUT_OF_WORLD
                || cause == DamageCause.COMMAND) {
                return;
            }

            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null) return;
            UUID uuid = playerRef.getUuid();

            ClassPlayerStats stats = classManager.getStatEngine().getStats(uuid);
            float reduc = (stats != null && stats.armorPct() > 0) ? reductionForArmorScore(stats.armorPct()) : 0f;

            float before = damage.getAmount();
            float after = reduc > 0f ? before * (1f - reduc) : before;
            if (reduc > 0f) damage.setAmount(after);

            if (VrpgConfig.isDebugCombat()) {
                VrpgDamageTrace.step(damage, "red dgt classe", reduc * 100f, before, after);
            }
        } catch (Exception e) {
            LOG.atWarning().withCause(e).log("[VrpgArmor] handle ERREUR");
        }
    }
}
