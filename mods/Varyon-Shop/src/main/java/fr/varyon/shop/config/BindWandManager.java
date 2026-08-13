package fr.varyon.shop.config;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players who ran "/vshop bind <type> [category|shopId]" (or "/vshop unbind") and are
 * now expected to interact with (right click) the Denizen NPC they want to bind or unbind.
 * Mirrors QuestLinesDenizens' own SelectionManager wand pattern: armed state with a TTL,
 * consumed on the next interaction.
 */
public final class BindWandManager {
    private static final long WAND_TTL_MS = 60_000L;

    /** Empty binding means "unbind on next interact"; present binding means "bind to this". */
    private record ArmedRequest(Optional<MerchantRegistry.Binding> binding, long armedAtMillis) {
    }

    private final Map<UUID, ArmedRequest> armed = new ConcurrentHashMap<>();

    public void arm(UUID playerUuid, MerchantRegistry.MerchantType type) {
        armBind(playerUuid, type, null, null);
    }

    public void arm(UUID playerUuid, MerchantRegistry.MerchantType type, String category) {
        armBind(playerUuid, type, category, null);
    }

    public void armBind(UUID playerUuid, MerchantRegistry.MerchantType type, String category, String shopId) {
        MerchantRegistry.Binding binding = type == null ? null : new MerchantRegistry.Binding(type, category, shopId);
        armed.put(playerUuid, new ArmedRequest(Optional.ofNullable(binding), System.currentTimeMillis()));
    }

    public void armUnbind(UUID playerUuid) {
        armed.put(playerUuid, new ArmedRequest(Optional.empty(), System.currentTimeMillis()));
    }

    public void disarm(UUID playerUuid) {
        armed.remove(playerUuid);
    }

    public boolean isArmed(UUID playerUuid) {
        return peek(playerUuid) != null;
    }

    /**
     * Consumes the armed request for this player, if any and not expired.
     * Returns null if nothing is armed; an empty Optional if armed for unbind; a present
     * Optional with the merchant binding if armed for bind.
     */
    public Optional<MerchantRegistry.Binding> consume(UUID playerUuid) {
        ArmedRequest request = armed.remove(playerUuid);
        if (request == null || isExpired(request)) {
            return null;
        }
        return request.binding();
    }

    private ArmedRequest peek(UUID playerUuid) {
        ArmedRequest request = armed.get(playerUuid);
        if (request == null) {
            return null;
        }
        if (isExpired(request)) {
            armed.remove(playerUuid);
            return null;
        }
        return request;
    }

    private boolean isExpired(ArmedRequest request) {
        return System.currentTimeMillis() - request.armedAtMillis() > WAND_TTL_MS;
    }
}
