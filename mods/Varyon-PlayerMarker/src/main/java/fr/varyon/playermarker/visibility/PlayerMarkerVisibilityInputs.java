package fr.varyon.playermarker;

record PlayerMarkerVisibilityInputs(boolean self,
                                    boolean viewerVanished,
                                    boolean targetVanished,
                                    boolean hiddenByViewerManager,
                                    boolean hiddenByCollector,
                                    boolean hiddenByVanishCollector) {

    static PlayerMarkerVisibilityInputs hiddenTarget() {
        return new PlayerMarkerVisibilityInputs(false, false, false, false, true, false);
    }
}

