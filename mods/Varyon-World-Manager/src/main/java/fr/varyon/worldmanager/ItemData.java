package fr.varyon.worldmanager;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import org.bson.BsonDocument;

public class ItemData {

    private int sectionId;
    private int slot;
    private String itemId;
    private int quantity;
    private double durability;
    private double maxDurability;
    private String metadata;

    public ItemData() {}

    public ItemData(int sectionId, int slot, ItemStack item) {
        this(sectionId, slot, item, null);
    }

    /** {@code profilerMeta} is null in normal operation; non-null only while JoinProfiler is timing a scan. */
    public ItemData(int sectionId, int slot, ItemStack item, JoinProfiler.Metadata profilerMeta) {
        this.sectionId = sectionId;
        this.slot = slot;
        this.itemId = item.getItemId();
        this.quantity = item.getQuantity();
        this.durability = item.getDurability();
        this.maxDurability = item.getMaxDurability();

        long t0 = profilerMeta != null ? System.nanoTime() : 0L;
        BsonDocument meta = item.getMetadata();
        long t1 = profilerMeta != null ? System.nanoTime() : 0L;
        int jsonLength = 0;
        if (meta != null && !meta.isEmpty()) {
            this.metadata = meta.toJson();
            jsonLength = this.metadata.length();
        }
        long t2 = profilerMeta != null ? System.nanoTime() : 0L;

        if (profilerMeta != null) {
            profilerMeta.recordItem(this.itemId, t1 - t0, t2 - t1, jsonLength);
        }
    }

    public ItemStack toItemStack() {
        try {
            BsonDocument metaDoc = (metadata != null && !metadata.isEmpty())
                    ? BsonDocument.parse(metadata) : null;
            return new ItemStack(itemId, quantity, durability, maxDurability, metaDoc);
        } catch (Exception e) {
            return null;
        }
    }

    public int getSectionId() { return sectionId; }
    public void setSectionId(int sectionId) { this.sectionId = sectionId; }
    public int getSlot() { return slot; }
    public void setSlot(int slot) { this.slot = slot; }
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getDurability() { return durability; }
    public void setDurability(double durability) { this.durability = durability; }
    public double getMaxDurability() { return maxDurability; }
    public void setMaxDurability(double maxDurability) { this.maxDurability = maxDurability; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
