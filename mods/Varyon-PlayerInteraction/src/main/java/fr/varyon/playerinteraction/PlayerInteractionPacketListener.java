package fr.varyon.playerinteraction;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketWatcher;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerInteractionPacketListener implements PacketWatcher {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final long COOLDOWN_MS = 500L;

    private final ConcurrentHashMap<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    private PlayerInteractionPacketListener() {
    }

    public static void register() {
        PacketAdapters.registerInbound(new PlayerInteractionPacketListener());
    }

    @Override
    public void accept(PacketHandler handler, Packet packet) {
        if (!(packet instanceof SyncInteractionChains chains)) return;
        if (handler.getAuth() == null) return;

        UUID playerUuid = handler.getAuth().getUuid();
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null || !playerRef.isValid()) return;

        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) return;

        for (SyncInteractionChain chain : chains.updates) {
            world.execute(() -> handleChain(playerRef, playerUuid, world, chain));
        }
    }

    private void handleChain(PlayerRef playerRef, UUID playerUuid, World world, SyncInteractionChain chain) {
        if (chain.interactionType != InteractionType.Use) return;
        if (!chain.initial || chain.forkedId != null) return;
        if (chain.data == null) return;
        if (!checkCooldown(playerUuid)) return;

        Ref<EntityStore> selfRef = playerRef.getReference();
        if (selfRef == null || !selfRef.isValid()) return;
        Store<EntityStore> store = selfRef.getStore();

        Ref<EntityStore> targetRef = world.getEntityStore().getRefFromNetworkId(chain.data.entityId);
        if (targetRef == null || !targetRef.isValid()) return;

        PlayerRef targetPlayerRef = store.getComponent(targetRef, PlayerRef.getComponentType());
        if (targetPlayerRef == null) return;
        if (targetPlayerRef.getUuid().equals(playerUuid)) return;

        Player player = store.getComponent(selfRef, Player.getComponentType());
        if (player == null) return;

        LOGGER.atInfo().log("[PlayerInteraction] Opening menu for %s targeting %s", playerRef.getUsername(), targetPlayerRef.getUsername());

        player.getPageManager().openCustomPage(
            selfRef, store,
            new PlayerInteractionMenuUIPage(playerRef, targetPlayerRef.getUuid(), targetPlayerRef.getUsername())
        );
    }

    private boolean checkCooldown(UUID playerUuid) {
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(playerUuid);
        if (last != null && now - last < COOLDOWN_MS) {
            return false;
        }
        cooldowns.put(playerUuid, now);
        return true;
    }
}
