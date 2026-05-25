package fr.varyon.playermarker;

final class PlayerMarkerVisibilityResolver {

    private PlayerMarkerVisibilityResolver() {}

    static PlayerMarkerVisibilityDecision resolve(PlayerMarkerVisibilityInputs inputs) {
        if (inputs == null) {
            return new PlayerMarkerVisibilityDecision(
                    PlayerMarkerVisibilityState.HIDDEN,
                    false,
                    false,
                    false,
                    false,
                    true,
                    false);
        }

        boolean viewerVanished = inputs.viewerVanished();
        boolean targetVanished = inputs.targetVanished();
        boolean hiddenByViewerManager = inputs.hiddenByViewerManager();
        boolean hiddenByCollector = inputs.hiddenByCollector();
        boolean hiddenByVanishCollector = inputs.hiddenByVanishCollector();
        boolean effectiveTargetVanished = targetVanished
                || (viewerVanished && (hiddenByViewerManager || hiddenByVanishCollector));
        boolean selfGhosted = inputs.self()
                && (viewerVanished || targetVanished || hiddenByViewerManager || hiddenByCollector || hiddenByVanishCollector);

        PlayerMarkerVisibilityState state;
        if (inputs.self()) {
            state = selfGhosted ? PlayerMarkerVisibilityState.GHOSTED : PlayerMarkerVisibilityState.VISIBLE;
        } else if (hiddenByCollector || hiddenByViewerManager) {
            state = viewerVanished && effectiveTargetVanished
                    ? PlayerMarkerVisibilityState.GHOSTED
                    : PlayerMarkerVisibilityState.HIDDEN;
        } else if (effectiveTargetVanished) {
            state = viewerVanished ? PlayerMarkerVisibilityState.GHOSTED : PlayerMarkerVisibilityState.HIDDEN;
        } else {
            state = PlayerMarkerVisibilityState.VISIBLE;
        }

        return new PlayerMarkerVisibilityDecision(
                state,
                viewerVanished,
                targetVanished,
                effectiveTargetVanished,
                hiddenByViewerManager,
                hiddenByCollector,
                hiddenByVanishCollector);
    }
}

