package fr.varyon.shop.config;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players who ran "/vshop bind <type>" (or "/vshop unbind") and are now expected to
 * interact with (right click) the Denizen NPC they want to bind or unbind. Mirrors
 * QuestLinesDenizens' own SelectionManager wand pattern: armed state with a TTL, consumed on
 * the next interaction.
 */
public final class BindWandManager {
    private static final long WAND_TTL_MS = 60_000L;

    /** Empty type means "unbind on next interact"; present type means "bind to this type". */
    private record ArmedRequest(Optional<MerchantRegistry.MerchantType> type, long armedAtMillis) {
    }

    private final Map<UUID, ArmedRequest> armed = new ConcurrentHashMap<>();

    public void arm(UUID playerUuid, MerchantRegistry.MerchantType type) {
        armed.put(playerUuid, new ArmedRequest(Optional.ofNullable(type), System.currentTimeMillis()));
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
     * Optional with the merchant type if armed for bind.
     */
    public Optional<MerchantRegistry.MerchantType> consume(UUID playerUuid) {
        ArmedRequest request = armed.remove(playerUuid);
        if (request == null || isExpired(request)) {
            return null;
        }
        return request.type();
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
