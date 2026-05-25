package fr.varyon.playermarker;

import java.util.UUID;

interface PlayerMarkerVanishProvider {

    String name();

    boolean isAvailable();

    boolean isVanished(UUID playerUuid);
}

