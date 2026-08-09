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
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Trace de debug (voir {@code debug_combat}) pour la reduction de degats appliquee par
 * l'armure physique EQUIPEE via le systeme natif du moteur Hytale
 * ({@link DamageSystems.ArmorDamageReduction}, cf. InventoryComponent.Armor + ItemArmor sur
 * chaque piece). Independant de Varyon-RPG (armorPct de la classe RPG, deja trace ailleurs
 * comme "red dgt classe") et de SimpleEnchantments (deja fondu dans "base=" faute d'ordre
 * garanti avec ce mod tiers).
 *
 * <p>Deux sondes : {@link Before} capture le montant juste avant le systeme moteur,
 * {@link After} capture juste apres et pousse l'etape dans {@link VrpgDamageTrace}.
 */
public final class VrpgNativeArmorTraceSystem {

    private static final Map<Damage, Float> BEFORE = new IdentityHashMap<>();

    private VrpgNativeArmorTraceSystem() {}

    public static final class Before extends DamageEventSystem {
        private static final Set<Dependency<EntityStore>> DEPENDENCIES = Set.of(
            new SystemDependency<>(Order.BEFORE, DamageSystems.ArmorDamageReduction.class)
        );

        @Override
        public Set<Dependency<EntityStore>> getDependencies() { return DEPENDENCIES; }

        @Override
        public SystemGroup<EntityStore> getGroup() { return DamageModule.get().getFilterDamageGroup(); }

        @Override
        public Query<EntityStore> getQuery() { return Player.getComponentType(); }

        @Override
        public void handle(int index,
                           @Nonnull ArchetypeChunk<EntityStore> chunk,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull CommandBuffer<EntityStore> commandBuffer,
                           @Nonnull Damage damage) {
            if (!VrpgConfig.isDebugCombat()) return;
            if (damage.isCancelled() || damage.getAmount() <= 0f) return;
            BEFORE.put(damage, damage.getAmount());
        }
    }

    public static final class After extends DamageEventSystem {
        private static final Set<Dependency<EntityStore>> DEPENDENCIES = Set.of(
            new SystemDependency<>(Order.AFTER, DamageSystems.ArmorDamageReduction.class)
        );

        @Override
        public Set<Dependency<EntityStore>> getDependencies() { return DEPENDENCIES; }

        @Override
        public SystemGroup<EntityStore> getGroup() { return DamageModule.get().getFilterDamageGroup(); }

        @Override
        public Query<EntityStore> getQuery() { return Player.getComponentType(); }

        @Override
        public void handle(int index,
                           @Nonnull ArchetypeChunk<EntityStore> chunk,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull CommandBuffer<EntityStore> commandBuffer,
                           @Nonnull Damage damage) {
            if (!VrpgConfig.isDebugCombat()) return;
            Float before = BEFORE.remove(damage);
            if (before == null) return;
            float after = damage.getAmount();
            if (Math.abs(after - before) < 0.001f) return;
            float reducPct = before != 0f ? (1f - after / before) * 100f : 0f;
            VrpgDamageTrace.step(damage, "armure Hytale", reducPct, before, after);
        }
    }
}
