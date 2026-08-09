package fr.varyon.vrpg.combat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.config.VrpgConfig;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Log final du debug combat (voir {@code debug_combat}) : s'execute juste avant que le
 * moteur applique le degat (Order.BEFORE ApplyDamage), donc apres tous les systemes
 * Varyon-RPG. Ferme la trace ouverte par {@link VrpgDamageTrace} pour ce coup.
 *
 * <p>Ne garantit pas de s'executer apres SimpleEnchantments (pas de dependance d'ordre
 * croisee entre les deux mods) : si "brut" semble deja inclure un bonus/malus d'enchantement,
 * c'est que ce mod s'est execute avant.
 */
public final class VrpgDamageLogSystem extends DamageEventSystem {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private static final Set<Dependency<EntityStore>> DEPENDENCIES =
        Set.of(new SystemDependency<>(Order.BEFORE, DamageSystems.ApplyDamage.class));

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        if (!VrpgConfig.isDebugCombat()) return;
        VrpgDamageTrace.logFinal(damage, LOG);
    }
}
