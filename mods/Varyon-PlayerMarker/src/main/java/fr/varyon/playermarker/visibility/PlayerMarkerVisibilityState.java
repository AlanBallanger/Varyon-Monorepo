package fr.varyon.playermarker;

enum PlayerMarkerVisibilityState {
    VISIBLE,
    GHOSTED,
    HIDDEN;

    boolean isVisible() {
        return this != HIDDEN;
    }

    boolean isGhosted() {
        return this == GHOSTED;
    }
}

