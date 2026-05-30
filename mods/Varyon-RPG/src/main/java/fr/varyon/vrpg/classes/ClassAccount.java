package fr.varyon.vrpg.classes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    public ClassAccount(@Nonnull UUID uuid, @Nullable String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
        for (PlayerClass c : PlayerClass.values()) {
            progress.put(c, ClassProgress.freshLevel1(c));
            talents.put(c, new HashMap<>());
        }
        this.activeClass = null;
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

    public void resetTalents(@Nonnull PlayerClass c) {
        talents.get(c).clear();
    }

    public int availableTalentPoints(@Nonnull PlayerClass c) {
        int earned = ClassXpCurve.talentPointsAtLevel(progress.get(c).getLevel());
        return Math.max(0, earned - totalTalentRanks(c));
    }
}
