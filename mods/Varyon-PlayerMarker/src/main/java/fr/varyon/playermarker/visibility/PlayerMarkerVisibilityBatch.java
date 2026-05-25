package fr.varyon.playermarker;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class PlayerMarkerVisibilityBatch {

    private final Set<UUID> hiddenByAnyOther;
    private final Set<UUID> viewerHiddenTargets;
    private final Map<UUID, Boolean> modVanish;

    private PlayerMarkerVisibilityBatch(
            Set<UUID> hiddenByAnyOther,
            Set<UUID> viewerHiddenTargets,
            Map<UUID, Boolean> modVanish) {
        this.hiddenByAnyOther = hiddenByAnyOther;
        this.viewerHiddenTargets = viewerHiddenTargets;
        this.modVanish = modVanish;
    }

    static PlayerMarkerVisibilityBatch build(Collection<PlayerRef> playerRefs, PlayerRef viewerRef, UUID viewerUuid) {
        if (playerRefs == null || playerRefs.isEmpty()) {
            return new PlayerMarkerVisibilityBatch(
                    Collections.emptySet(),
                    Collections.emptySet(),
                    Collections.emptyMap());
        }

        HiddenSnapshots hidden = collectHiddenSnapshots(playerRefs, viewerRef, viewerUuid);
        Map<UUID, Boolean> modVanish = computeModVanish(playerRefs, viewerUuid);

        return new PlayerMarkerVisibilityBatch(
                hidden.hiddenByAnyOther(),
                hidden.viewerHidden(),
                Map.copyOf(modVanish));
    }

    private static HiddenSnapshots collectHiddenSnapshots(
            Collection<PlayerRef> playerRefs,
            PlayerRef viewerRef,
            UUID viewerUuid) {

        Map<UUID, Set<UUID>> hiddenByPlayer = new HashMap<>();
        for (PlayerRef o : playerRefs) {
            if (o == null) {
                continue;
            }
            UUID ou = o.getUuid();
            if (ou == null) {
                continue;
            }
            Set<UUID> snap = PlayerMarkerHiddenSnapshot.snapshot(o.getHiddenPlayersManager());
            hiddenByPlayer.put(ou, snap);
        }

        if (viewerUuid != null && viewerRef != null && !hiddenByPlayer.containsKey(viewerUuid)) {
            Set<UUID> snap = PlayerMarkerHiddenSnapshot.snapshot(viewerRef.getHiddenPlayersManager());
            hiddenByPlayer.put(viewerUuid, snap);
        }

        Set<UUID> hiddenByAnyOther = new HashSet<>();
        for (Map.Entry<UUID, Set<UUID>> e : hiddenByPlayer.entrySet()) {
            UUID ou = e.getKey();
            for (UUID t : e.getValue()) {
                if (!t.equals(ou)) {
                    hiddenByAnyOther.add(t);
                }
            }
        }

        Set<UUID> viewerHidden =
                viewerUuid == null
                        ? Collections.emptySet()
                        : hiddenByPlayer.getOrDefault(viewerUuid, Collections.emptySet());

        return new HiddenSnapshots(Set.copyOf(hiddenByAnyOther), Set.copyOf(viewerHidden));
    }

    private static Map<UUID, Boolean> computeModVanish(Collection<PlayerRef> playerRefs, UUID viewerUuid) {
        if (!PlayerMarkerVanishProviders.hasActiveProvider()) {
            return Collections.emptyMap();
        }
        Map<UUID, Boolean> modVanish = new HashMap<>();
        for (PlayerRef ref : playerRefs) {
            if (ref == null) {
                continue;
            }
            UUID u = ref.getUuid();
            if (u != null) {
                modVanish.put(u, PlayerMarkerVanishProviders.isVanished(u));
            }
        }
        if (viewerUuid != null && !modVanish.containsKey(viewerUuid)) {
            modVanish.put(viewerUuid, PlayerMarkerVanishProviders.isVanished(viewerUuid));
        }
        return modVanish;
    }

    boolean effectiveVanished(UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }
        return Boolean.TRUE.equals(modVanish.get(playerUuid)) || hiddenByAnyOther.contains(playerUuid);
    }

    boolean isHiddenByViewer(UUID targetUuid) {
        return targetUuid != null && viewerHiddenTargets.contains(targetUuid);
    }

    private record HiddenSnapshots(Set<UUID> hiddenByAnyOther, Set<UUID> viewerHidden) {}
}
