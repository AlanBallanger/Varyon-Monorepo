package com.varyon.safezone;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import org.joml.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Level;

public class SafeZonePvpSystem extends DamageEventSystem {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Query<EntityStore> QUERY = Player.getComponentType();
    private static SafeZoneManager safeZoneManager;

    public static void setSafeZoneManager(@Nonnull SafeZoneManager manager) {
        safeZoneManager = manager;
    }

    @Override
    @Nullable
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                      @Nonnull Store<EntityStore> store,
                      @Nonnull CommandBuffer<EntityStore> commandBuffer,
                      @Nonnull Damage damage) {
        
        if (safeZoneManager == null) {
            return;
        }

        Player victimPlayer = archetypeChunk.getComponent(index, Player.getComponentType());
        Ref<EntityStore> victimRef = archetypeChunk.getReferenceTo(index);

        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource)) {
            return;
        }

        Damage.EntitySource entitySource = (Damage.EntitySource)source;
        Ref<EntityStore> attackerRef = entitySource.getRef();

        if (!attackerRef.isValid()) {
            return;
        }

        Player attackerPlayer = commandBuffer.getComponent(attackerRef, Player.getComponentType());
        if (attackerPlayer == null) {
            return;
        }

        Entity victimEntity = EntityUtils.getEntity(index, archetypeChunk);
        Entity attackerEntity = EntityUtils.getEntity(attackerRef, commandBuffer);

        if (victimEntity == null || attackerEntity == null) {
            return;
        }

        TransformComponent victimTransform = commandBuffer.getComponent(victimRef, TransformComponent.getComponentType());
        TransformComponent attackerTransform = commandBuffer.getComponent(attackerRef, TransformComponent.getComponentType());

        if (victimTransform == null || attackerTransform == null) {
            return;
        }

        double victimX = victimTransform.getPosition().x;
        double victimZ = victimTransform.getPosition().z;
        double attackerX = attackerTransform.getPosition().x;
        double attackerZ = attackerTransform.getPosition().z;

        boolean victimInSafeZone = safeZoneManager.isInSafeZone(victimX, victimZ);
        boolean attackerInSafeZone = safeZoneManager.isInSafeZone(attackerX, attackerZ);

        if (victimInSafeZone || attackerInSafeZone) {
            damage.setCancelled(true);

            PlayerRef attackerPlayerRef = commandBuffer.getComponent(attackerRef, PlayerRef.getComponentType());
            if (attackerPlayerRef != null) {
                attackerPlayerRef.sendMessage(Message.raw("[PvP] Vous êtes dans une zone safe !").color(java.awt.Color.RED));
            }

            LOGGER.at(Level.FINE).log("Blocked PvP damage in safe zone");
        }
    }
}
