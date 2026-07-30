package com.varyon.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.VaryonPlugin;
import com.varyon.config.MessagesConfig;
import com.varyon.extraction.ExtractionPortalManager;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.awt.Color;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ExtractionPortalTickSystem extends EntityTickingSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final double PORTAL_RADIUS_XZ = 1.5;
    private static final double PORTAL_HEIGHT = 3.5;
    private static final long DENY_MESSAGE_COOLDOWN_MS = 3000;

    private final Map<UUID, Long> lastDenyMessage = new ConcurrentHashMap<>();

    @Override
    public void tick(float deltaTime, int index, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                     @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {

        ExtractionPortalManager manager = ExtractionPortalManager.getInstance();
        if (manager == null) {
            return;
        }

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        PlayerRef playerRef = (PlayerRef) store.getComponent(ref, PlayerRef.getComponentType());
        Player player = (Player) store.getComponent(ref, Player.getComponentType());

        if (playerRef == null || player == null) {
            return;
        }

        UUID playerId = playerRef.getUuid();
        double px = playerRef.getTransform().getPosition().x;
        double py = playerRef.getTransform().getPosition().y;
        double pz = playerRef.getTransform().getPosition().z;

        for (Map.Entry<UUID, ExtractionPortalManager.PortalData> entry : manager.getActivePortals().entrySet()) {
            ExtractionPortalManager.PortalData portal = entry.getValue();
            UUID ownerId = entry.getKey();

            double dx = px - (portal.x() + 0.5);
            double dz = pz - (portal.z() + 0.5);
            double distXZ = Math.sqrt(dx * dx + dz * dz);
            double dy = py - portal.y();

            if (distXZ > PORTAL_RADIUS_XZ || dy < 0 || dy > PORTAL_HEIGHT) {
                continue;
            }

            if (!ownerId.equals(playerId)) {
                long now = System.currentTimeMillis();
                Long lastDeny = lastDenyMessage.get(playerId);
                if (lastDeny == null || now - lastDeny > DENY_MESSAGE_COOLDOWN_MS) {
                    lastDenyMessage.put(playerId, now);
                    MessagesConfig.ExtractionMessages msg = VaryonPlugin.getStaticConfigManager().getMessagesConfig().getExtraction();
                    playerRef.sendMessage(Message.raw(msg.notYourPortal).color(Color.RED));
                }
                return;
            }

            World world = ((EntityStore) store.getExternalData()).getWorld();
            ISpawnProvider spawnProvider = world.getWorldConfig().getSpawnProvider();
            if (spawnProvider == null) {
                return;
            }

            Transform spawnPoint = spawnProvider.getSpawnPoint(world, playerId);
            org.joml.Vector3d spawnPos = spawnPoint.getPosition();

            // Jouer le son de téléportation
            try {
                com.hypixel.hytale.protocol.SoundCategory soundCategory = com.hypixel.hytale.protocol.SoundCategory.UI;
                com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent soundEvent = 
                    com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent.class.cast(null);
                
                // Utiliser le son du téléporteur vanilla ou un son de portail
                String soundId = "SFX_Teleporter_Arrival"; // Son de téléportation
                int soundIndex = com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent.getAssetMap().getIndex(soundId);
                
                if (soundIndex != 0) {
                    com.hypixel.hytale.server.core.universe.world.SoundUtil.playSoundEvent2dToPlayer(
                        playerRef, soundIndex, soundCategory, 1.0f, 1.0f);
                }
            } catch (Exception e) {
                // Si le son échoue, on continue quand même
            }

            Teleport teleport = Teleport.createForPlayer(world, spawnPos, com.hypixel.hytale.math.vector.Rotation3f.ZERO);
            commandBuffer.addComponent(ref, Teleport.getComponentType(), teleport);

            MessagesConfig.ExtractionMessages msg = VaryonPlugin.getStaticConfigManager().getMessagesConfig().getExtraction();
            playerRef.sendMessage(Message.raw(msg.teleporting).color(Color.GREEN));

            manager.consumePortal(ownerId);

            LOGGER.at(Level.INFO).log("Player " + playerId + " used extraction portal at " + portal.x() + "," + portal.y() + "," + portal.z());
            return;
        }
    }

    public void removePlayer(UUID playerId) {
        lastDenyMessage.remove(playerId);
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
