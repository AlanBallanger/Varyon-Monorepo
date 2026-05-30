package fr.varyon.vrpg.classes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ClassProgress {

    private final PlayerClass playerClass;
    private int level;
    private long xpInLevel;
    private PlayerSpecialization activeSpec;

    public ClassProgress(@Nonnull PlayerClass playerClass, int level, long xpInLevel,
                         @Nullable PlayerSpecialization activeSpec) {
        this.playerClass = playerClass;
        this.level = Math.max(1, Math.min(ClassXpCurve.MAX_LEVEL, level));
        this.xpInLevel = Math.max(0L, xpInLevel);
        this.activeSpec = activeSpec;
    }

    public static ClassProgress freshLevel1(@Nonnull PlayerClass playerClass) {
        return new ClassProgress(playerClass, 1, 0L, null);
    }

    @Nonnull  public PlayerClass getPlayerClass()          { return playerClass; }
    public int getLevel()                                   { return level; }
    public long getXpInLevel()                              { return xpInLevel; }
    @Nullable public PlayerSpecialization getActiveSpec()  { return activeSpec; }

    public void setActiveSpec(@Nullable PlayerSpecialization spec) {
        if (spec != null && spec.getParentClass() != playerClass) return;
        this.activeSpec = spec;
    }

    public long getXpToNextLevel() {
        return ClassXpCurve.xpForLevel(level);
    }

    public boolean isMaxLevel() {
        return level >= ClassXpCurve.MAX_LEVEL;
    }

    public int addXp(long amount) {
        if (amount <= 0L) return 0;
        int levelsGained = 0;
        long remaining = amount;
        while (remaining > 0L && level < ClassXpCurve.MAX_LEVEL) {
            long needed = ClassXpCurve.xpForLevel(level) - xpInLevel;
            if (remaining < needed) {
                xpInLevel += remaining;
                remaining = 0L;
            } else {
                remaining -= needed;
                xpInLevel = 0L;
                level++;
                levelsGained++;
            }
        }
        if (level >= ClassXpCurve.MAX_LEVEL) {
            xpInLevel = 0L;
        }
        return levelsGained;
    }

    public void setLevel(int newLevel, long newXpInLevel) {
        this.level = Math.max(1, Math.min(ClassXpCurve.MAX_LEVEL, newLevel));
        this.xpInLevel = Math.max(0L, newXpInLevel);
        if (this.level >= ClassXpCurve.MAX_LEVEL) {
            this.xpInLevel = 0L;
        }
    }
}
