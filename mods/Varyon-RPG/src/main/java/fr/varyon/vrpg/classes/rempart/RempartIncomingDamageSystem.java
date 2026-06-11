package fr.varyon.vrpg.classes.rempart;

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
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.classes.WeaponCategory;
import fr.varyon.vrpg.config.VrpgConfig;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class RempartIncomingDamageSystem extends DamageEventSystem {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private final ClassManager classManager;
    private final RempartState rempartState;
    private int healthIdx = Integer.MIN_VALUE;

    public RempartIncomingDamageSystem(@Nonnull ClassManager classManager,
                                       @Nonnull RempartState rempartState) {
        this.classManager = classManager;
        this.rempartState = rempartState;
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
        try {
            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null) return;

            UUID uuid = playerRef.getUuid();
            ClassAccount acc = classManager.getOrLoad(uuid);
            if (acc.getActiveClass() != PlayerClass.GUERRIER) return;
            if (acc.getActiveSpec(PlayerClass.GUERRIER) != PlayerSpecialization.REMPART) return;

            boolean debug = VrpgConfig.isDebugCombat();

            // Détection de blocage bouclier : damage already cancelled + bouclier en main
            if (damage.isCancelled() && hasShield(playerRef)) {
                onBlock(uuid, acc, playerRef, chunk, index, store, debug);
                return;
            }

            if (damage.isCancelled() || damage.getAmount() <= 0f) return;

            float incoming = damage.getAmount();
            float amount = incoming;

            // Forteresse
            float forteresseReduc = rempartState.getForteresseReduction(uuid);
            if (forteresseReduc > 0f) {
                amount *= (1f - forteresseReduc);
                if (debug) LOG.atInfo().log(String.format("[RempartRecu] Forteresse -%.0f%% %.1f->%.1f", forteresseReduc * 100, incoming, amount));
            }

            // Garde Rapprochée (auto-réduction)
            float gardeReduc = rempartState.getGardeRapprocheReduction(uuid);
            if (gardeReduc > 0f) {
                amount *= (1f - gardeReduc);
                if (debug) LOG.atInfo().log(String.format("[RempartRecu] GardeRapprochee -%.0f%%", gardeReduc * 100));
            }

            // Garde Impénétrable (après blocage)
            int gardeImpRank = rempartState.getGardeImpenetrableRank(uuid);
            if (gardeImpRank > 0) {
                float gardeImpReduc = RempartPassifs.gardeReductionForRank(gardeImpRank);
                amount *= (1f - gardeImpReduc);
                if (debug) LOG.atInfo().log(String.format("[RempartRecu] GardeImpenetrable -%.0f%%", gardeImpReduc * 100));
            }

            // Dernier Bastion (sous 30% HP)
            int bastionRank = acc.getTalentRank(PlayerClass.GUERRIER, RempartPassifs.DERNIER_BASTION_NODE);
            if (bastionRank > 0) {
                float hpRatio = getHpRatio(chunk, index, store);
                if (hpRatio < RempartPassifs.bastionThreshold()) {
                    float bastionReduc = RempartPassifs.bastionReductionForRank(bastionRank);
                    amount *= (1f - bastionReduc);
                    if (debug) LOG.atInfo().log(String.format("[RempartRecu] DernierBastion -%.0f%%", bastionReduc * 100));
                }
            }

            if (amount != incoming) damage.setAmount(amount);

        } catch (Exception ignored) {}
    }

    private void onBlock(@Nonnull UUID uuid, @Nonnull ClassAccount acc,
                         @Nonnull PlayerRef playerRef,
                         @Nonnull ArchetypeChunk<EntityStore> chunk, int index,
                         @Nonnull Store<EntityStore> store, boolean debug) {
        // Contre Offensif
        int contreRank = acc.getTalentRank(PlayerClass.GUERRIER, RempartPassifs.CONTRE_OFFENSIF_NODE);
        if (contreRank > 0) {
            rempartState.armContreOffensif(uuid, RempartPassifs.contreWindowMs(), contreRank);
            if (debug) LOG.atInfo().log("[RempartRecu] ContreOffensif armé");
        }

        // Garde Impénétrable
        int gardeImpRank = acc.getTalentRank(PlayerClass.GUERRIER, RempartPassifs.GARDE_IMPENETRABLE_NODE);
        if (gardeImpRank > 0) {
            rempartState.armGardeImpenetrable(uuid, RempartPassifs.gardeDurationMs(), gardeImpRank);
            if (debug) LOG.atInfo().log("[RempartRecu] GardeImpenetrable armé");
        }

        // Infatigable — soin sur blocage
        int infatigableRank = acc.getTalentRank(PlayerClass.GUERRIER, RempartPassifs.INFATIGABLE_NODE);
        if (infatigableRank > 0) {
            healPlayer(playerRef, chunk, index, store, RempartPassifs.infatigableHealPctForRank(infatigableRank));
            if (debug) LOG.atInfo().log("[RempartRecu] Infatigable soin");
        }
    }

    private void healPlayer(@Nonnull PlayerRef playerRef,
                            @Nonnull ArchetypeChunk<EntityStore> chunk, int index,
                            @Nonnull Store<EntityStore> store, float pct) {
        try {
            if (healthIdx == Integer.MIN_VALUE) {
                try { healthIdx = DefaultEntityStatTypes.getHealth(); } catch (Exception e) { healthIdx = -1; }
            }
            if (healthIdx < 0) return;
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
            if (stats == null) return;
            var hp = stats.get(healthIdx);
            if (hp == null || hp.getMax() <= 0) return;
            float heal = hp.getMax() * pct;
            stats.setStatValue(healthIdx, Math.min(hp.getMax(), hp.get() + heal));
        } catch (Exception ignored) {}
    }

    private float getHpRatio(@Nonnull ArchetypeChunk<EntityStore> chunk, int index,
                             @Nonnull Store<EntityStore> store) {
        try {
            if (healthIdx == Integer.MIN_VALUE) {
                try { healthIdx = DefaultEntityStatTypes.getHealth(); } catch (Exception e) { healthIdx = -1; }
            }
            if (healthIdx < 0) return 1f;
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
            if (stats == null) return 1f;
            var hp = stats.get(healthIdx);
            if (hp == null || hp.getMax() <= 0) return 1f;
            return hp.get() / hp.getMax();
        } catch (Exception e) { return 1f; }
    }

    public boolean hasShield(@Nonnull PlayerRef playerRef) {
        try {
            var hotbar = playerRef.getComponent(InventoryComponent.Hotbar.getComponentType());
            if (hotbar == null) return false;
            var inv = hotbar.getInventory();
            for (short s = 0; s < inv.getCapacity(); s++) {
                ItemStack stack = inv.getItemStack(s);
                if (stack == null || stack.isEmpty()) continue;
                if (WeaponCategory.fromItemId(stack.getItemId()) == WeaponCategory.BOUCLIER) return true;
            }
            return false;
        } catch (Exception e) { return false; }
    }
}
