package fr.varyon.mapmarker;

import javax.annotation.Nullable;

public record MarkerEntry(
        String id,
        String worldName,
        String imageName,
        String markerName,
        float x,
        float z,
        String createdByName,
        @Nullable String group) {}
