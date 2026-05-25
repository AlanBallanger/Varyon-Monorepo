package com.varyon.comet.config.model;

import java.util.Random;

public class BossEntry {

    private String id;
    private Integer explicitMinCount;
    private Integer explicitMaxCount;
    private java.util.Map<Integer, float[]> multipliers = new java.util.HashMap<>();

    public BossEntry() {
        this.id = "";
    }

    public BossEntry(String id) {
        this.id = id;
    }

    public BossEntry(String id, int minCount, int maxCount) {
        this.id = id;
        int mn = Math.max(1, minCount);
        int mx = Math.max(mn, maxCount);
        this.explicitMinCount = mn;
        this.explicitMaxCount = mx;
    }

    // Getters
    public String getId() {
        return id;
    }

    public Integer getExplicitMinCount() {
        return explicitMinCount;
    }

    public Integer getExplicitMaxCount() {
        return explicitMaxCount;
    }

    public int rollSpawnCount(Random random) {
        if (explicitMinCount != null && explicitMaxCount != null) {
            int lo = explicitMinCount;
            int hi = explicitMaxCount;
            if (lo > hi) {
                int t = lo;
                lo = hi;
                hi = t;
            }
            return lo + random.nextInt(hi - lo + 1);
        }
        if (explicitMinCount != null) {
            return Math.max(1, explicitMinCount);
        }
        return 1;
    }

    public java.util.Map<Integer, float[]> getMultipliers() {
        return multipliers;
    }

    public float[] getMultipliersForTier(int tier) {
        return multipliers.get(tier);
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setExplicitMinCount(Integer explicitMinCount) {
        this.explicitMinCount = explicitMinCount;
    }

    public void setExplicitMaxCount(Integer explicitMaxCount) {
        this.explicitMaxCount = explicitMaxCount;
    }

    public void setMultipliers(java.util.Map<Integer, float[]> multipliers) {
        this.multipliers = multipliers;
    }

    public void addMultiplier(int tier, float hp, float damage, float scale, float speed) {
        this.multipliers.put(tier, new float[] { hp, damage, scale, speed });
    }

    @Override
    public String toString() {
        return "BossEntry{id='" + id + "', multipliers=" + multipliers.size() + "}";
    }
}
