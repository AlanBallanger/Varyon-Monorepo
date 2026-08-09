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
 * Sonde de debug (voir {@code debug_combat}) : fixe le "base=" du log consolide au plus tot
 * parmi les systemes de degats Varyon-RPG (avant crit, maitrise, armure). Ne reference aucun
 * mod tiers, donc fonctionne que SimpleEnchantments (ou autre) soit installe ou non.
 *
 * <p>Observe en jeu : SimpleEnchantments s'execute avant cette sonde malgre l'absence de
 * dependance d'ordre le concernant — son effet (ex. Protection) est donc deja inclus dans
 * "base=", pas visible comme etape separee. Pour mesurer l'effet d'un enchantement precis,
 * comparer "base=" d'un meme coup avec/sans l'objet enchante equipe.
 */
public final class VrpgDamageProbeSystem extends DamageEventSystem {

    private static final Set<Dependency<EntityStore>> DEPENDENCIES = Set.of(
        new SystemDependency<>(Order.BEFORE, VrpgCritDamageSystem.class),
        new SystemDependency<>(Order.BEFORE, VrpgArmorDamageSystem.class),
        new SystemDependency<>(Order.BEFORE, fr.varyon.vrpg.classes.SpecWeaponMasteryDamageSystem.class),
        new SystemDependency<>(Order.BEFORE, DamageSystems.ArmorDamageReduction.class)
    );

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
        if (damage.isCancelled() || damage.getAmount() <= 0f) return;
        VrpgDamageTrace.markBase(damage, damage.getAmount());
    }
}
