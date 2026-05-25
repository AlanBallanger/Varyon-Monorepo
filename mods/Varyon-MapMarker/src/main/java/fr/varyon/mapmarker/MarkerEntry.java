package fr.varyon.mapmarker;

public record MarkerEntry(
        String id,
        String worldName,
        String imageName,
        String markerName,
        float x,
        float z,
        String createdByName) {}
