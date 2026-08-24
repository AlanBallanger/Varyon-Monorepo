package com.varyon.tptoworld.portal;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.server.core.util.io.BlockingDiskFile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public final class PortalTemplateFile extends BlockingDiskFile {

    private ConcurrentHashMap<String, PortalTemplateEntry> entries = new ConcurrentHashMap<>();

    public PortalTemplateFile(Path path) {
        super(path);
    }

    @Override
    protected void read(BufferedReader reader) throws IOException {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        if (root == null) {
            return;
        }
        ConcurrentHashMap<String, PortalTemplateEntry> loaded = new ConcurrentHashMap<>();
        JsonArray array = root.getAsJsonArray("Portals");
        if (array != null) {
            array.forEach(element -> {
                JsonObject obj = element.getAsJsonObject();
                String dimension = obj.get("Dimension").getAsString();
                int x = obj.get("X").getAsInt();
                int y = obj.get("Y").getAsInt();
                int z = obj.get("Z").getAsInt();
                String type = obj.has("Type") && !obj.get("Type").isJsonNull()
                        ? obj.get("Type").getAsString() : VaryonPortalConfig.DEFAULT_TYPE;
                String background = obj.has("Background") && !obj.get("Background").isJsonNull()
                        ? obj.get("Background").getAsString() : VaryonPortalConfig.NO_BACKGROUND;
                float scale = obj.has("Scale") ? obj.get("Scale").getAsFloat() : VaryonPortalConfig.DEFAULT_SCALE;
                float centerOffsetY = obj.has("CenterOffsetY")
                        ? obj.get("CenterOffsetY").getAsFloat() : VaryonPortalConfig.DEFAULT_CENTER_OFFSET_Y;
                boolean wide = obj.has("Wide") && obj.get("Wide").getAsBoolean();
                float yaw = obj.has("Yaw") ? obj.get("Yaw").getAsFloat() : VaryonPortalConfig.DEFAULT_YAW;
                PortalTemplateEntry entry = new PortalTemplateEntry(
                        dimension, x, y, z, type, background, scale, centerOffsetY, wide, yaw);
                loaded.put(entry.key(), entry);
            });
        }
        this.entries = loaded;
    }

    @Override
    protected void write(BufferedWriter writer) throws IOException {
        JsonObject root = new JsonObject();
        JsonArray array = new JsonArray();
        this.entries.values().forEach(entry -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("Dimension", entry.dimension());
            obj.addProperty("X", entry.x());
            obj.addProperty("Y", entry.y());
            obj.addProperty("Z", entry.z());
            obj.addProperty("Type", entry.type());
            obj.addProperty("Background", entry.background());
            obj.addProperty("Scale", entry.scale());
            obj.addProperty("CenterOffsetY", entry.centerOffsetY());
            obj.addProperty("Wide", entry.wide());
            obj.addProperty("Yaw", entry.yaw());
            array.add(obj);
        });
        root.add("Portals", array);
        writer.write(root.toString());
    }

    @Override
    protected void create(BufferedWriter writer) throws IOException {
        JsonObject root = new JsonObject();
        root.add("Portals", new JsonArray());
        writer.write(root.toString());
    }

    public ConcurrentHashMap<String, PortalTemplateEntry> getEntries() {
        return this.entries;
    }
}
