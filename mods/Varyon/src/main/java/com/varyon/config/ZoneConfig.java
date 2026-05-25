package com.varyon.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ZoneConfig {
    private final List<String> enabledWorlds;
    private final List<DifficultyZone> zones;
    private final Map<String, Integer> instanceZonePatterns;
    private boolean minimapEnabled;
    private int minimapOpacity;
    private String minimapPattern;
    private int minimapPatternSize;
    private boolean zoneEnterNotification;
    private String zoneEnterTopText;
    private float notificationDuration;
    private boolean zoneSoundEnabled;
    private String zoneSoundId;
    private float zoneSoundVolume;
    private float zoneSoundPitch;
    private boolean zoneHudEnabled;

    public ZoneConfig() {
        this.enabledWorlds = new ArrayList<>();
        this.zones = new ArrayList<>();
        this.instanceZonePatterns = new LinkedHashMap<>();
        this.minimapEnabled = true;
        this.minimapOpacity = 50;
        this.minimapPattern = "SOLID";
        this.minimapPatternSize = 4;
        this.zoneEnterNotification = true;
        this.zoneEnterTopText = "Zone";
        this.notificationDuration = 2.0f;
        this.zoneSoundEnabled = true;
        this.zoneSoundId = "SFX_Axe_Special_Swing";
        this.zoneSoundVolume = 1.0f;
        this.zoneSoundPitch = 1.0f;
        this.zoneHudEnabled = true;
    }

    public ZoneConfig(@Nonnull List<String> enabledWorlds, @Nonnull List<DifficultyZone> zones,
                      @Nullable Map<String, Integer> instanceZonePatterns,
                      boolean minimapEnabled, int minimapOpacity, String minimapPattern, int minimapPatternSize,
                      boolean zoneEnterNotification, String zoneEnterTopText, float notificationDuration,
                      boolean zoneSoundEnabled, String zoneSoundId, float zoneSoundVolume, float zoneSoundPitch,
                      boolean zoneHudEnabled) {
        this.enabledWorlds = new ArrayList<>(enabledWorlds);
        this.zones = new ArrayList<>(zones);
        this.instanceZonePatterns = instanceZonePatterns != null
                ? new LinkedHashMap<>(instanceZonePatterns)
                : new LinkedHashMap<>();
        this.minimapEnabled = minimapEnabled;
        this.minimapOpacity = minimapOpacity;
        this.minimapPattern = minimapPattern != null ? minimapPattern : "SOLID";
        this.minimapPatternSize = minimapPatternSize > 0 ? minimapPatternSize : 4;
        this.zoneEnterNotification = zoneEnterNotification;
        this.zoneEnterTopText = zoneEnterTopText != null ? zoneEnterTopText : "Zone";
        this.notificationDuration = notificationDuration > 0 ? notificationDuration : 2.0f;
        this.zoneSoundEnabled = zoneSoundEnabled;
        this.zoneSoundId = zoneSoundId != null ? zoneSoundId : "SFX_Axe_Special_Swing";
        this.zoneSoundVolume = zoneSoundVolume > 0 ? zoneSoundVolume : 1.0f;
        this.zoneSoundPitch = zoneSoundPitch > 0 ? zoneSoundPitch : 1.0f;
        this.zoneHudEnabled = zoneHudEnabled;
    }

    @Nonnull
    public List<String> getEnabledWorlds() { return Collections.unmodifiableList(enabledWorlds); }

    public boolean isWorldEnabled(@Nonnull String worldName) {
        String w = worldName.trim();
        if (enabledWorlds.stream().anyMatch(e -> e.equalsIgnoreCase(w))) return true;
        return getZoneIdForInstanceWorld(worldName) != null;
    }

    @Nullable
    public Integer getZoneIdForInstanceWorld(@Nonnull String worldName) {
        if (instanceZonePatterns.isEmpty()) return null;
        String w = worldName.trim().toLowerCase(Locale.ROOT);
        return instanceZonePatterns.entrySet().stream()
                .filter(e -> w.startsWith(e.getKey().toLowerCase(Locale.ROOT)))
                .max(Comparator.comparingInt(e -> e.getKey().length()))
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    @Nullable
    public DifficultyZone getZoneById(int zoneId) {
        return zones.stream()
                .filter(z -> z.getZoneId() == zoneId)
                .findFirst()
                .orElse(null);
    }

    @Nonnull
    public Map<String, Integer> getInstanceZonePatterns() { return Collections.unmodifiableMap(instanceZonePatterns); }

    @Nonnull
    public List<DifficultyZone> getZones() { return Collections.unmodifiableList(zones); }

    public void addZone(@Nonnull DifficultyZone zone) { zones.add(zone); }

    public void addInstanceZonePattern(@Nonnull String pattern, int zoneId) {
        instanceZonePatterns.put(pattern, zoneId);
    }

    public boolean isMinimapEnabled()           { return minimapEnabled; }
    public int getMinimapOpacity()              { return minimapOpacity; }
    @Nonnull public String getMinimapPattern()  { return minimapPattern != null ? minimapPattern : "SOLID"; }
    public int getMinimapPatternSize()          { return minimapPatternSize > 0 ? minimapPatternSize : 4; }
    public boolean isZoneEnterNotification()    { return zoneEnterNotification; }
    @Nonnull public String getZoneEnterTopText(){ return zoneEnterTopText != null ? zoneEnterTopText : "Zone"; }
    public float getNotificationDuration()      { return notificationDuration > 0 ? notificationDuration : 2.0f; }
    public boolean isZoneSoundEnabled()         { return zoneSoundEnabled; }
    @Nonnull public String getZoneSoundId()     { return zoneSoundId != null ? zoneSoundId : "SFX_Axe_Special_Swing"; }
    public float getZoneSoundVolume()           { return zoneSoundVolume > 0 ? zoneSoundVolume : 1.0f; }
    public float getZoneSoundPitch()            { return zoneSoundPitch > 0 ? zoneSoundPitch : 1.0f; }
    public boolean isZoneHudEnabled()           { return zoneHudEnabled; }

    @Nonnull
    public static ZoneConfig createDefault() {
        ZoneConfig config = new ZoneConfig();
        config.minimapEnabled = true;
        config.minimapOpacity = 50;
        config.minimapPattern = "SOLID";
        config.minimapPatternSize = 4;
        config.zoneEnterNotification = true;
        config.zoneEnterTopText = "Zone";
        config.notificationDuration = 3.0f;
        config.zoneSoundEnabled = true;
        config.zoneSoundId = "SFX_Axe_Special_Swing";
        config.zoneSoundVolume = 1.0f;
        config.zoneSoundPitch = 1.0f;
        config.zoneHudEnabled = true;

        config.addZone(new DifficultyZone(1,  "#55FF55", 1.0,  1.0,  1.0,  1.0,  0,     "Facile",          50));
        config.addZone(new DifficultyZone(2,  "#55FFAA", 1.5,  1.5,  1.25, 1.2,  5000,  "Normale",         100));
        config.addZone(new DifficultyZone(3,  "#E6FF33", 2.0,  2.25, 1.6,  1.4,  7500,  "Intermédiaire",   150));
        config.addZone(new DifficultyZone(4,  "#FFC533", 3.0,  3.5,  1.8,  1.6,  10000, "Avancée",        200));
        config.addZone(new DifficultyZone(5,  "#FF7A1A", 4.25, 5.0,  2.0,  1.8,  12500, "Difficile",      250));
        config.addZone(new DifficultyZone(6,  "#FF3030", 5.5,  6.5,  2.25, 2.0,  15000, "Très difficile", 300));
        config.addZone(new DifficultyZone(7,  "#C0003A", 7.0,  8.5,  2.5,  2.2,  17500, "Extreme",        350));
        config.addZone(new DifficultyZone(8,  "#8A2BFF", 8.5,  11.0, 2.75, 2.4,  20000, "Cauchemar",      400));
        config.addZone(new DifficultyZone(9,  "#05000A", 11.0, 15.0, 3.0,  2.6,  22500, "Enfer",          450));
        config.addZone(new DifficultyZone(10, "#3D0000", 13.0, 20.0, 3.25, 2.8,  25000, "Abysse",        500));

        config.addInstanceZonePattern("instance-portals_henges", 1);
        config.addInstanceZonePattern("instance-portals_oasis", 1);
        config.addInstanceZonePattern("instance-portals_hedera", 2);
        config.addInstanceZonePattern("instance-portals_jungles", 2);
        config.addInstanceZonePattern("instance-portals_taiga", 2);
        config.addInstanceZonePattern("instance-endgame_frozen_dungeon", 4);
        config.addInstanceZonePattern("instance-endgame_swamp_dungeon", 6);
        config.addInstanceZonePattern("instance-endgame_golem_void", 8);

        return config;
    }
}
