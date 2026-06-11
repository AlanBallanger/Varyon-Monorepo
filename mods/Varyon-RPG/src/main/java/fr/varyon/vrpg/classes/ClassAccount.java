package fr.varyon.vrpg.classes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClassAccount {

    private final UUID uuid;
    private volatile String playerName;
    private volatile PlayerClass activeClass;

    private final EnumMap<PlayerClass, ClassProgress> progress = new EnumMap<>(PlayerClass.class);
    private final EnumMap<PlayerClass, Map<String, Integer>> talents = new EnumMap<>(PlayerClass.class);
    private final EnumMap<PlayerClass, Map<String, String>> skillSlots = new EnumMap<>(PlayerClass.class);

    private final ClassProfile[] profiles;
    private int activeProfileIndex = 0;

    public ClassAccount(@Nonnull UUID uuid, @Nullable String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
        for (PlayerClass c : PlayerClass.values()) {
            progress.put(c, ClassProgress.freshLevel1(c));
            talents.put(c, new HashMap<>());
            skillSlots.put(c, new HashMap<>());
        }
        this.activeClass = null;
        this.profiles = new ClassProfile[ClassProfile.COUNT];
        for (int i = 0; i < ClassProfile.COUNT; i++) {
            profiles[i] = new ClassProfile("Profil " + (i + 1));
        }
    }

    @Nonnull  public UUID getUuid()           { return uuid; }
    @Nullable public String getPlayerName()   { return playerName; }
    public void setPlayerName(@Nullable String name) { this.playerName = name; }

    @Nullable public PlayerClass getActiveClass()              { return activeClass; }
    public void setActiveClass(@Nullable PlayerClass c)        { this.activeClass = c; }

    @Nonnull
    public ClassProgress getProgress(@Nonnull PlayerClass c) {
        return progress.get(c);
    }

    @Nonnull
    public Map<PlayerClass, ClassProgress> getAllProgress() {
        return progress;
    }

    @Nullable
    public PlayerSpecialization getActiveSpec(@Nonnull PlayerClass c) {
        return progress.get(c).getActiveSpec();
    }

    public void setActiveSpec(@Nonnull PlayerClass c, @Nullable PlayerSpecialization spec) {
        progress.get(c).setActiveSpec(spec);
    }

    @Nonnull
    public Map<String, Integer> getTalents(@Nonnull PlayerClass c) {
        return talents.get(c);
    }

    public int getTalentRank(@Nonnull PlayerClass c, @Nonnull String nodeId) {
        return talents.get(c).getOrDefault(nodeId, 0);
    }

    public void setTalentRank(@Nonnull PlayerClass c, @Nonnull String nodeId, int rank) {
        if (rank <= 0) {
            talents.get(c).remove(nodeId);
        } else {
            talents.get(c).put(nodeId, rank);
        }
    }

    public int totalTalentRanks(@Nonnull PlayerClass c) {
        int sum = 0;
        for (int r : talents.get(c).values()) sum += r;
        return sum;
    }

    public int totalTalentRanksForTree(@Nonnull PlayerClass c, @Nullable PlayerSpecialization spec) {
        String prefix = spec != null ? spec.getId() + "_" : null;
        int sum = 0;
        for (Map.Entry<String, Integer> e : talents.get(c).entrySet()) {
            boolean inTree = prefix != null ? e.getKey().startsWith(prefix) : !e.getKey().contains("_");
            if (inTree) sum += e.getValue();
        }
        return sum;
    }

    public void resetTalents(@Nonnull PlayerClass c) {
        talents.get(c).clear();
    }

    private static String slotKey(@Nullable PlayerSpecialization spec, @Nonnull String slotId) {
        return spec != null ? spec.getId() + ":" + slotId : slotId;
    }

    @Nonnull
    public Map<String, String> getSkillSlots(@Nonnull PlayerClass c) {
        return skillSlots.get(c);
    }

    @Nonnull
    public Map<String, String> getSkillSlotsForSpec(@Nonnull PlayerClass c, @Nullable PlayerSpecialization spec) {
        String prefix = spec != null ? spec.getId() + ":" : "";
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> e : skillSlots.get(c).entrySet()) {
            if (spec != null) {
                if (e.getKey().startsWith(prefix)) {
                    result.put(e.getKey().substring(prefix.length()), e.getValue());
                }
            } else {
                if (!e.getKey().contains(":")) {
                    result.put(e.getKey(), e.getValue());
                }
            }
        }
        return result;
    }

    @Nullable
    public String getSkillSlot(@Nonnull PlayerClass c, @Nonnull String slotId) {
        PlayerSpecialization spec = getActiveSpec(c);
        return skillSlots.get(c).get(slotKey(spec, slotId));
    }

    public void setSkillSlot(@Nonnull PlayerClass c, @Nonnull String slotId, @Nonnull String itemId) {
        PlayerSpecialization spec = getActiveSpec(c);
        skillSlots.get(c).put(slotKey(spec, slotId), itemId);
    }

    public void clearSkillSlot(@Nonnull PlayerClass c, @Nonnull String slotId) {
        PlayerSpecialization spec = getActiveSpec(c);
        skillSlots.get(c).remove(slotKey(spec, slotId));
    }

    public void clearSkillSlots(@Nonnull PlayerClass c) {
        PlayerSpecialization spec = getActiveSpec(c);
        if (spec != null) {
            String prefix = spec.getId() + ":";
            skillSlots.get(c).keySet().removeIf(k -> k.startsWith(prefix));
        } else {
            skillSlots.get(c).keySet().removeIf(k -> !k.contains(":"));
        }
    }

    @Nonnull
    public Map<String, String> copySkillSlots(@Nonnull PlayerClass c) {
        return Collections.unmodifiableMap(getSkillSlotsForSpec(c, getActiveSpec(c)));
    }

    public int availableTalentPoints(@Nonnull PlayerClass c) {
        return availableTalentPoints(c, getActiveSpec(c));
    }

    public int availableTalentPoints(@Nonnull PlayerClass c, @Nullable PlayerSpecialization spec) {
        int earned = ClassXpCurve.talentPointsAtLevel(progress.get(c).getLevel());
        return Math.max(0, earned - totalTalentRanksForTree(c, spec));
    }

    @Nonnull
    public ClassProfile[] getProfiles() { return profiles; }

    public int getActiveProfileIndex() { return activeProfileIndex; }

    public void setActiveProfileIndex(int idx) {
        if (idx >= 0 && idx < ClassProfile.COUNT) activeProfileIndex = idx;
    }

    public void switchProfile(int idx) {
        if (idx < 0 || idx >= ClassProfile.COUNT || idx == activeProfileIndex) return;
        profiles[activeProfileIndex].snapshotFrom(this);
        activeProfileIndex = idx;
        profiles[activeProfileIndex].applyTo(this);
    }
}
