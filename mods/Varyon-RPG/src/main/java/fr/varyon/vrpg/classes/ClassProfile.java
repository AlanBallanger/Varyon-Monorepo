package fr.varyon.vrpg.classes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class ClassProfile {

    public static final int COUNT = 5;

    private String name;
    @Nullable private PlayerClass activeClass;
    private final EnumMap<PlayerClass, ClassProgress> progress = new EnumMap<>(PlayerClass.class);
    private final EnumMap<PlayerClass, PlayerSpecialization> specs = new EnumMap<>(PlayerClass.class);
    private final EnumMap<PlayerClass, Map<String, Integer>> talents = new EnumMap<>(PlayerClass.class);
    private final EnumMap<PlayerClass, Map<String, String>> skillSlots = new EnumMap<>(PlayerClass.class);

    public ClassProfile(@Nonnull String name) {
        this.name = name;
        for (PlayerClass c : PlayerClass.values()) {
            progress.put(c, ClassProgress.freshLevel1(c));
            talents.put(c, new HashMap<>());
            skillSlots.put(c, new HashMap<>());
        }
    }

    @Nonnull
    public ClassProgress getProgress(@Nonnull PlayerClass c) {
        return progress.get(c);
    }

    @Nonnull  public String getName()                               { return name; }
    public void setName(@Nonnull String n)                         { this.name = n; }
    @Nullable public PlayerClass getActiveClass()                   { return activeClass; }
    public void setActiveClass(@Nullable PlayerClass c)             { this.activeClass = c; }

    @Nullable public PlayerSpecialization getSpec(@Nonnull PlayerClass c) { return specs.get(c); }
    public void setSpec(@Nonnull PlayerClass c, @Nullable PlayerSpecialization spec) {
        if (spec != null && spec.getParentClass() != c) return;
        if (spec == null) specs.remove(c); else specs.put(c, spec);
    }

    @Nonnull public Map<String, Integer> getTalents(@Nonnull PlayerClass c) {
        return talents.computeIfAbsent(c, k -> new HashMap<>());
    }

    public int getTalentRank(@Nonnull PlayerClass c, @Nonnull String nodeId) {
        return getTalents(c).getOrDefault(nodeId, 0);
    }

    public void setTalentRank(@Nonnull PlayerClass c, @Nonnull String nodeId, int rank) {
        if (rank <= 0) getTalents(c).remove(nodeId);
        else getTalents(c).put(nodeId, rank);
    }

    public void resetTalents(@Nonnull PlayerClass c) { getTalents(c).clear(); }

    @Nonnull
    public Map<String, String> getSkillSlots(@Nonnull PlayerClass c) {
        return skillSlots.computeIfAbsent(c, k -> new HashMap<>());
    }

    public void setSkillSlot(@Nonnull PlayerClass c, @Nonnull String slotId, @Nonnull String itemId) {
        getSkillSlots(c).put(slotId, itemId);
    }

    public void clearSkillSlots(@Nonnull PlayerClass c) {
        getSkillSlots(c).clear();
    }

    public int totalTalentRanks(@Nonnull PlayerClass c) {
        int s = 0;
        for (int r : getTalents(c).values()) s += r;
        return s;
    }

    public void snapshotFrom(@Nonnull ClassAccount acc) {
        this.activeClass = acc.getActiveClass();
        for (PlayerClass c : PlayerClass.values()) {
            ClassProgress live = acc.getProgress(c);
            getProgress(c).setLevel(live.getLevel(), live.getXpInLevel());
            setSpec(c, live.getActiveSpec());
            getTalents(c).clear();
            getTalents(c).putAll(acc.getTalents(c));
            getSkillSlots(c).clear();
            getSkillSlots(c).putAll(acc.getSkillSlots(c));
        }
    }

    public void applyTo(@Nonnull ClassAccount acc) {
        acc.setActiveClass(activeClass);
        for (PlayerClass c : PlayerClass.values()) {
            ClassProgress stored = getProgress(c);
            acc.getProgress(c).setLevel(stored.getLevel(), stored.getXpInLevel());
            acc.getProgress(c).setActiveSpec(specs.get(c));
            acc.getTalents(c).clear();
            acc.getTalents(c).putAll(getTalents(c));
            acc.getSkillSlots(c).clear();
            acc.getSkillSlots(c).putAll(getSkillSlots(c));
        }
    }
}
