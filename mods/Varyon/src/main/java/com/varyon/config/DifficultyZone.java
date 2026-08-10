package com.varyon.config;

import javax.annotation.Nonnull;
import java.awt.Color;

public class DifficultyZone {
    private final int zoneId;
    private final String color;
    private final double healthMultiplier;
    private final double damageMultiplier;
    private final double lootMultiplier;
    private final double pointsMultiplier;
    private final int radiusStart;
    private final String name;
    private final int teleportCost;

    public DifficultyZone(int zoneId, @Nonnull String color, double healthMultiplier, double damageMultiplier,
                          double lootMultiplier, double pointsMultiplier, int radiusStart, @Nonnull String name) {
        this(zoneId, color, healthMultiplier, damageMultiplier, lootMultiplier, pointsMultiplier, radiusStart, name, zoneId * 100);
    }

    public DifficultyZone(int zoneId, @Nonnull String color, double healthMultiplier, double damageMultiplier,
                          double lootMultiplier, double pointsMultiplier, int radiusStart, @Nonnull String name, int teleportCost) {
        this.zoneId = zoneId;
        this.color = color;
        this.healthMultiplier = healthMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.lootMultiplier = lootMultiplier;
        this.pointsMultiplier = pointsMultiplier;
        this.radiusStart = radiusStart;
        this.name = name;
        this.teleportCost = teleportCost;
    }

    public int getZoneId() {
        return zoneId;
    }

    @Nonnull
    public String getColor() {
        return color;
    }

    public double getHealthMultiplier() {
        return healthMultiplier;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public double getLootMultiplier() {
        return lootMultiplier;
    }

    public double getPointsMultiplier() {
        return pointsMultiplier;
    }

    /**
     * Returns the maximum multiplier for map display purposes.
     */
    public double getMaxMultiplier() {
        return Math.max(healthMultiplier, Math.max(damageMultiplier, lootMultiplier));
    }

    public int getRadiusStart() {
        return radiusStart;
    }

    public int getTeleportCost() {
        return teleportCost;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    /**
     * Parses the color string and returns a java.awt.Color.
     * Supports both named colors (WHITE, RED, etc.) and hex colors (#FF5500, #F50).
     */
    @Nonnull
    public Color getParsedColor() {
        String colorStr = color.trim().toUpperCase();

        // Check if it's a hex color
        if (colorStr.startsWith("#")) {
            return parseHexColor(colorStr);
        }

        // Named colors
        switch (colorStr) {
            case "WHITE":
                return Color.WHITE;
            case "GREEN":
                return new Color(0, 128, 0);
            case "LIME":
                return new Color(50, 205, 50);
            case "YELLOW":
                return Color.YELLOW;
            case "GOLD":
                return new Color(255, 215, 0);
            case "ORANGE":
                return Color.ORANGE;
            case "RED":
                return Color.RED;
            case "DARK_RED":
                return new Color(139, 0, 0);
            case "PURPLE":
                return new Color(128, 0, 128);
            case "BLACK":
                return new Color(30, 30, 30);
            case "BLUE":
                return Color.BLUE;
            case "CYAN":
                return Color.CYAN;
            case "MAGENTA":
                return Color.MAGENTA;
            case "PINK":
                return Color.PINK;
            case "GRAY":
            case "GREY":
                return Color.GRAY;
            case "DARK_GRAY":
            case "DARK_GREY":
                return Color.DARK_GRAY;
            case "LIGHT_GRAY":
            case "LIGHT_GREY":
                return Color.LIGHT_GRAY;
            default:
                // Fallback to white if unknown
                return Color.WHITE;
        }
    }

    @Nonnull
    private Color parseHexColor(@Nonnull String hex) {
        try {
            String cleanHex = hex.substring(1); // Remove #

            if (cleanHex.length() == 3) {
                // Short form #RGB -> #RRGGBB
                char r = cleanHex.charAt(0);
                char g = cleanHex.charAt(1);
                char b = cleanHex.charAt(2);
                cleanHex = "" + r + r + g + g + b + b;
            }

            if (cleanHex.length() == 6) {
                int r = Integer.parseInt(cleanHex.substring(0, 2), 16);
                int g = Integer.parseInt(cleanHex.substring(2, 4), 16);
                int b = Integer.parseInt(cleanHex.substring(4, 6), 16);
                return new Color(r, g, b);
            }
        } catch (Exception e) {
            // Fallback on parse error
        }
        return Color.WHITE;
    }

    @Override
    public String toString() {
        return name + " (HP x" + healthMultiplier + ", DMG x" + damageMultiplier + ", Loot x" + lootMultiplier +
                ", Points de faction x" + pointsMultiplier + ", " + radiusStart + "+ blocks, " + color + ")";
    }
}
