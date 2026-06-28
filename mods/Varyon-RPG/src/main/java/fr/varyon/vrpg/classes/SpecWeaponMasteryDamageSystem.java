package fr.varyon.vrpg.classes;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import fr.varyon.vrpg.classes.ClassStatDefinition;
import fr.varyon.vrpg.classes.PlayerClass;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class SpecWeaponMasteryDamageSystem extends DamageEventSystem {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private final ClassManager classManager;

    public SpecWeaponMasteryDamageSystem(@Nonnull ClassManager classManager) {
        this.classManager = classManager;
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return NPCEntity.getComponentType();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        try {
            if (damage.isCancelled() || damage.getAmount() <= 0f) return;

            Damage.Source source = damage.getSource();
            if (!(source instanceof Damage.EntitySource entitySource)) return;
            Ref<EntityStore> attackerRef = entitySource.getRef();
            if (attackerRef == null || !attackerRef.isValid()) return;

            Player player = store.getComponent(attackerRef, Player.getComponentType());
            if (player == null) player = commandBuffer.getComponent(attackerRef, Player.getComponentType());
            if (player == null) return;

            PlayerRef playerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());
            if (playerRef == null) playerRef = commandBuffer.getComponent(attackerRef, PlayerRef.getComponentType());
            if (playerRef == null) return;

            UUID uuid = playerRef.getUuid();
            ClassAccount acc = classManager.getOrLoad(uuid);
            PlayerClass activeClass = acc.getActiveClass();
            PlayerSpecialization spec = acc.getActiveSpec(activeClass);

            float base = damage.getAmount();
            float amount = base;

            // Multiplicateur de niveau — sauf mages dont le scaling niveau est dans leur OutgoingDamageSystem
            if (spec != null && activeClass != null
                    && spec != PlayerSpecialization.ARCANISTE
                    && spec != PlayerSpecialization.GARDIEN_DE_GAIA) {
                try {
                    int level = acc.getProgress(activeClass).getLevel();
                    double levelMult = ClassStatDefinition.atkDisplayMultiplier(level, spec);
                    amount *= (float) levelMult;
                    if (fr.varyon.vrpg.config.VrpgConfig.isDebugCombat()) {
                        LOG.atInfo().log(String.format("[LevelMult] spec=%s level=%d mult=x%.2f", spec.getId(), level, levelMult));
                    }
                } catch (Exception ignored2) {}
            }

            // Maîtrise d'armes — multiplicateur selon type d'arme
            if (spec != null) {
                String itemId = getHeldItemId(playerRef);
                if (itemId != null) {
                    WeaponCategory category = WeaponCategory.fromItemId(itemId);
                    if (category != null && category != WeaponCategory.AUTRE) {
                        double weaponMult = category.getMultiplierFor(spec);
                        if (weaponMult != 1.0) {
                            amount *= (float) weaponMult;
                            if (fr.varyon.vrpg.config.VrpgConfig.isDebugCombat()) {
                                LOG.atInfo().log(String.format("[WeaponMastery] spec=%s weapon=%s mult=x%.2f",
                                    spec.getId(), category.name(), weaponMult));
                            }
                        }
                    }
                }
            }

            if (amount != base) {
                damage.setAmount(amount);
                if (fr.varyon.vrpg.config.VrpgConfig.isDebugCombat()) {
                    LOG.atInfo().log(String.format("[Mastery] base=%.1f -> final=%.1f", base, amount));
                }
            }
        } catch (Exception ignored) {}
    }

    private String getHeldItemId(@Nonnull PlayerRef playerRef) {
        try {
            InventoryComponent.Hotbar hotbar = playerRef.getComponent(InventoryComponent.Hotbar.getComponentType());
            if (hotbar == null) return null;
            byte slot = hotbar.getActiveSlot();
            ItemStack held = hotbar.getInventory().getItemStack((short) slot);
            return held != null ? held.getItemId() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
