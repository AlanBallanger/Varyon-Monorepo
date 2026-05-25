package fr.varyon.playermarker;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class PlayerMarkerControlPageModels {

    enum SurfaceBulkState {
        ALL_ENABLED,
        ALL_DISABLED,
        MIXED,
        EMPTY
    }

    record RowModel(UUID uuid,
                    String playerName,
                    String metaText,
                    String visibilityBadge,
                    String assetPath,
                    boolean self,
                    boolean mapEnabled,
                    boolean minimapEnabled,
                    boolean compassEnabled,
                    PlayerMarkerVisibilityState visibilityState) {
    }

    record PageSlice(List<RowModel> rows, int pageIndex, int pageCount) {
    }

    private PlayerMarkerControlPageModels() {
    }

    static List<RowModel> buildRows(PlayerRef viewerRef,
                                    VaryonPlayerMarkerPlugin plugin,
                                    Runnable onAvatarReady,
                                    Runnable onPendingAssets) {
        List<PlayerRef> players = new ArrayList<>(plugin.getActivePlayers());
        if (plugin.getAvatarService() != null) {
            plugin.getAvatarService().advanceViewerDeliveryPhase(viewerRef);
        }

        UUID viewerUuid = viewerRef != null ? viewerRef.getUuid() : null;
        players.sort(Comparator
                .comparing((PlayerRef ref) -> !isSelf(ref, viewerUuid))
                .thenComparing(ref -> PlayerMarkerPlayerNames.resolveOrLocalizedUnknown(ref, viewerRef).toLowerCase(Locale.ROOT)));

        PlayerMarkerPlayerSettings settings = plugin.resolvePlayerSettings(viewerRef);
        List<RowModel> rows = new ArrayList<>(players.size());
        for (PlayerRef ref : players) {
            UUID playerUuid = ref.getUuid();
            if (playerUuid == null) {
                continue;
            }

            String playerName = PlayerMarkerPlayerNames.resolveOrLocalizedUnknown(ref, viewerRef);
            boolean self = isSelf(ref, viewerUuid);
            PlayerMarkerVisibilityDecision visibility = PlayerMarkerVisibilityService.resolve(viewerRef, viewerUuid, playerUuid);
            PlayerMarkerVisibilityState visibilityState = visibility.state();
            if (!visibility.isVisible()) {
                continue;
            }

            PlayerMarkerVisuals.AvatarVisual visual =
                    PlayerMarkerVisuals.resolveAvatarVisual(viewerRef, playerUuid, playerName, visibilityState, onAvatarReady);
            String assetPath = PlayerMarkerVisuals.toUiAssetPath(visual.markerImage());
            boolean customized = settings.hasAnyOverride(playerUuid);
            boolean mapEnabled = settings.isEnabledFor(PlayerMarkerSurface.MAP, viewerUuid, playerUuid);
            boolean minimapEnabled = settings.isEnabledFor(PlayerMarkerSurface.MINIMAP, viewerUuid, playerUuid);
            boolean compassEnabled = settings.isEnabledFor(PlayerMarkerSurface.COMPASS, viewerUuid, playerUuid);
            rows.add(new RowModel(
                    playerUuid,
                    playerName,
                    describeRowMeta(viewerRef, self, customized, visibilityState),
                    visibilityState.isGhosted() ? PlayerMarkerUiText.choose(viewerRef, "VANISH", "СКРИТІСТЬ") : "",
                    assetPath,
                    self,
                    mapEnabled,
                    minimapEnabled,
                    compassEnabled,
                    visibilityState));
        }

        if (plugin.getAvatarService() != null && plugin.getAvatarService().hasPendingAssets(viewerUuid) && onPendingAssets != null) {
            onPendingAssets.run();
        }
        return rows;
    }

    static PageSlice pageSlice(List<RowModel> rows, int requestedPage, int pageSize) {
        int total = rows.size();
        int pageCount = total == 0 ? 1 : (int) Math.ceil(total / (double) pageSize);
        int pageIndex = total == 0 ? 0 : Math.max(0, Math.min(requestedPage, pageCount - 1));
        if (total == 0) {
            return new PageSlice(List.of(), pageIndex, pageCount);
        }

        int fromIndex = pageIndex * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        return new PageSlice(rows.subList(fromIndex, toIndex), pageIndex, pageCount);
    }

    static int countEnabled(List<RowModel> rows, PlayerMarkerSurface surface, UUID viewerUuid) {
        int enabledCount = 0;
        for (RowModel row : rows) {
            if (isEnabled(row, surface, viewerUuid)) {
                enabledCount++;
            }
        }
        return enabledCount;
    }

    static SurfaceBulkState bulkState(List<RowModel> rows, PlayerMarkerSurface surface, UUID viewerUuid) {
        if (rows.isEmpty()) {
            return SurfaceBulkState.EMPTY;
        }

        int enabledCount = countEnabled(rows, surface, viewerUuid);
        int eligibleCount = countEligible(rows, surface, viewerUuid);
        if (eligibleCount == 0) {
            return SurfaceBulkState.EMPTY;
        }
        if (enabledCount == 0) {
            return SurfaceBulkState.ALL_DISABLED;
        }
        if (enabledCount == eligibleCount) {
            return SurfaceBulkState.ALL_ENABLED;
        }
        return SurfaceBulkState.MIXED;
    }

    static int countEligible(List<RowModel> rows, PlayerMarkerSurface surface, UUID viewerUuid) {
        int eligibleCount = 0;
        for (RowModel row : rows) {
            if (isEligible(row, surface, viewerUuid)) {
                eligibleCount++;
            }
        }
        return eligibleCount;
    }

    static boolean isEnabled(RowModel row, PlayerMarkerSurface surface, UUID viewerUuid) {
        if (!isEligible(row, surface, viewerUuid)) {
            return false;
        }
        return switch (surface) {
            case MAP -> row.mapEnabled();
            case MINIMAP -> row.minimapEnabled();
            case COMPASS -> row.compassEnabled();
        };
    }

    static boolean isEligible(RowModel row, PlayerMarkerSurface surface, UUID viewerUuid) {
        return row != null && !PlayerMarkerPlayerSettings.isSelfForcedHidden(surface, viewerUuid, row.uuid());
    }

    private static boolean isSelf(PlayerRef ref, UUID viewerUuid) {
        return ref != null && viewerUuid != null && viewerUuid.equals(ref.getUuid());
    }

    private static String describeRowMeta(PlayerRef viewerRef,
                                          boolean self,
                                          boolean customized,
                                          PlayerMarkerVisibilityState visibilityState) {
        if (visibilityState != null && visibilityState.isGhosted()) {
            if (self) {
                return PlayerMarkerUiText.choose(viewerRef, "You are in hidden mode", "Ви у режимі скритності");
            }
            return PlayerMarkerUiText.choose(viewerRef, "Visible to you as hidden", "Показується вам як прихований");
        }

        if (self) {
            return customized
                    ? PlayerMarkerUiText.choose(viewerRef, "You · Map only", "Ви · лише мапа")
                    : PlayerMarkerUiText.choose(viewerRef, "You · Hidden on minimap and compass", "Ви · приховано на мінімапі та компасі");
        }

        return customized
                ? PlayerMarkerUiText.choose(viewerRef, "Custom visibility", "Індивідуальна видимість")
                : PlayerMarkerUiText.choose(viewerRef, "Uses bulk defaults", "Використовує загальні налаштування");
    }
}


