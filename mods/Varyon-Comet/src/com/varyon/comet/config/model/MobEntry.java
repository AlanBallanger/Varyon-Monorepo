package com.varyon.comet.config.model;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class MobEntry {

    private String id;
    private int count;
    private Map<Integer, Integer> tierCounts = null;
    private Integer explicitMinCount = null;
    private Integer explicitMaxCount = null;
    private Map<Integer, Integer> tierMinCounts = null;
    private Map<Integer, Integer> tierMaxCounts = null;
    private Map<Integer, float[]> multipliers = new java.util.HashMap<>();

    public MobEntry() {
        this.id = "";
        this.count = 1;
    }

    public MobEntry(String id, int count) {
        this.id = id;
        this.count = count;
    }

    public MobEntry(String id, int minCount, int maxCount) {
        this.id = id;
        int mn = Math.max(1, minCount);
        int mx = Math.max(mn, maxCount);
        this.explicitMinCount = mn;
        this.explicitMaxCount = mx;
        this.count = (mn + mx + 1) / 2;
    }

    public String getId() {
        return id;
    }

    public int getCount() {
        return count;
    }

    public Integer getExplicitMinCount() {
        return explicitMinCount;
    }

    public Integer getExplicitMaxCount() {
        return explicitMaxCount;
    }

    public Map<Integer, Integer> getTierMinCounts() {
        return tierMinCounts;
    }

    public Map<Integer, Integer> getTierMaxCounts() {
        return tierMaxCounts;
    }

    public int getCountForTier(int tier) {
        int mn = getMinCountForTier(tier);
        int mx = getMaxCountForTier(tier);
        if (mn <= 0 && mx <= 0) {
            return 0;
        }
        if (mn <= 0) {
            return mx;
        }
        if (mx <= 0) {
            return mn;
        }
        return (mn + mx) / 2;
    }

    public int getMinCountForTier(int tier) {
        if (tierCounts != null) {
            int c = tierCounts.getOrDefault(tier, 0);
            if (c <= 0) {
                return 0;
            }
            if (tierMinCounts != null && tierMinCounts.containsKey(tier)) {
                return Math.max(1, tierMinCounts.get(tier));
            }
            return Math.max(1, c - 1);
        }
        if (explicitMinCount != null && explicitMaxCount != null) {
            return Math.max(1, explicitMinCount);
        }
        return Math.max(1, count - 1);
    }

    public int getMaxCountForTier(int tier) {
        if (tierCounts != null) {
            int c = tierCounts.getOrDefault(tier, 0);
            if (c <= 0) {
                return 0;
            }
            if (tierMaxCounts != null && tierMaxCounts.containsKey(tier)) {
                int lo = getMinCountForTier(tier);
                return Math.max(lo, tierMaxCounts.get(tier));
            }
            return c + 1;
        }
        if (explicitMinCount != null && explicitMaxCount != null) {
            return Math.max(explicitMinCount, explicitMaxCount);
        }
        return count + 1;
    }

    public int rollCountForTier(int tier, Random random) {
        int lo = getMinCountForTier(tier);
        int hi = getMaxCountForTier(tier);
        if (lo <= 0 || hi <= 0) {
            return 0;
        }
        if (lo > hi) {
            int t = lo;
            lo = hi;
            hi = t;
        }
        return lo + random.nextInt(hi - lo + 1);
    }

    public Map<Integer, Integer> getTierCounts() {
        return tierCounts;
    }

    public Map<Integer, float[]> getMultipliers() {
        return multipliers;
    }

    public float[] getMultipliersForTier(int tier) {
        return multipliers.get(tier);
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCount(int count) {
        this.count = Math.max(1, count);
    }

    public void setExplicitMinCount(Integer explicitMinCount) {
        this.explicitMinCount = explicitMinCount;
    }

    public void setExplicitMaxCount(Integer explicitMaxCount) {
        this.explicitMaxCount = explicitMaxCount;
    }

    public void setTierMinCounts(Map<Integer, Integer> tierMinCounts) {
        this.tierMinCounts = tierMinCounts != null && !tierMinCounts.isEmpty() ? new LinkedHashMap<>(tierMinCounts) : null;
    }

    public void setTierMaxCounts(Map<Integer, Integer> tierMaxCounts) {
        this.tierMaxCounts = tierMaxCounts != null && !tierMaxCounts.isEmpty() ? new LinkedHashMap<>(tierMaxCounts) : null;
    }

    public void setTierCounts(java.util.Map<Integer, Integer> tierCounts) {
        this.tierCounts = tierCounts != null ? new LinkedHashMap<>(tierCounts) : null;
    }

    public void setMultipliers(java.util.Map<Integer, float[]> multipliers) {
        this.multipliers = multipliers;
    }

    public void addMultiplier(int tier, float hp, float damage, float scale, float speed) {
        this.multipliers.put(tier, new float[] { hp, damage, scale, speed });
    }

    public MobEntry copy() {
        MobEntry o = new MobEntry();
        o.setId(id);
        o.setCount(count);
        if (tierCounts != null) {
            o.setTierCounts(new LinkedHashMap<>(tierCounts));
        }
        o.setExplicitMinCount(explicitMinCount);
        o.setExplicitMaxCount(explicitMaxCount);
        if (tierMinCounts != null) {
            o.setTierMinCounts(new LinkedHashMap<>(tierMinCounts));
        }
        if (tierMaxCounts != null) {
            o.setTierMaxCounts(new LinkedHashMap<>(tierMaxCounts));
        }
        for (Map.Entry<Integer, float[]> e : multipliers.entrySet()) {
            float[] arr = e.getValue();
            o.multipliers.put(e.getKey(), arr != null ? Arrays.copyOf(arr, arr.length) : null);
        }
        return o;
    }

    @Override
    public String toString() {
        String countStr = tierCounts != null ? "tierCounts=" + tierCounts : "count=" + count;
        return "MobEntry{id='" + id + "', " + countStr + ", multipliers=" + multipliers.size() + "}";
    }
}
