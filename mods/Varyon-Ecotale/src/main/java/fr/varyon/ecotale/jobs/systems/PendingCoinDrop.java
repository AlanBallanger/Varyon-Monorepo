package fr.varyon.ecotale.jobs.systems;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Carries the coin amount a dead NPC still owes as a physical world drop.
 *
 * The reward pipeline in {@link MobRewardSystem} runs at the moment of death (killer, anti-farm
 * and economy-cap only make sense then) but, when physical coins are enabled, it does not spawn
 * them immediately. It attaches this component instead, and {@link DeferredCoinDropSystem} spawns
 * the coins once the corpse is ready to disappear — matching vanilla loot timing.
 */
public class PendingCoinDrop implements Component<EntityStore> {

    private static ComponentType<EntityStore, PendingCoinDrop> COMPONENT_TYPE;

    private long amount;

    public PendingCoinDrop() {
    }

    public PendingCoinDrop(long amount) {
        this.amount = amount;
    }

    public long getAmount() {
        return amount;
    }

    @Override
    @Nullable
    public Component<EntityStore> clone() {
        return new PendingCoinDrop(amount);
    }

    @Nonnull
    public static ComponentType<EntityStore, PendingCoinDrop> getComponentType() {
        return COMPONENT_TYPE;
    }

    public static void setComponentType(@Nonnull ComponentType<EntityStore, PendingCoinDrop> componentType) {
        COMPONENT_TYPE = componentType;
    }
}
