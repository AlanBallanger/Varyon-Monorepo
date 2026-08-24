package com.varyon.tptoworld.portal;

import com.hypixel.hytale.server.core.Constants;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PortalTemplateRegistry {

    private static final Path MAIN_PATH = Constants.UNIVERSE_PATH.resolve("VaryonTpToWorld");
    private static final Path FILE_PATH = MAIN_PATH.resolve("PortalTemplates.json");
    private static final PortalTemplateRegistry INSTANCE = new PortalTemplateRegistry();

    private final PortalTemplateFile file = new PortalTemplateFile(FILE_PATH);

    private PortalTemplateRegistry() {
    }

    public static PortalTemplateRegistry getInstance() {
        return INSTANCE;
    }

    public void load() {
        try {
            java.nio.file.Files.createDirectories(MAIN_PATH);
        } catch (Exception ignored) {
        }
        this.file.syncLoad();
    }

    public void save(@Nonnull String dimension, int x, int y, int z,
            @Nonnull String type, @Nonnull String background, float scale, float centerOffsetY, boolean wide,
            float yaw) {
        PortalTemplateEntry entry = new PortalTemplateEntry(
                dimension, x, y, z, type, background, scale, centerOffsetY, wide, yaw);
        this.file.getEntries().put(entry.key(), entry);
        this.file.syncSave();
    }

    public boolean remove(@Nonnull String dimension, int x, int y, int z) {
        PortalTemplateEntry removed = this.file.getEntries().remove(PortalTemplateEntry.key(dimension, x, y, z));
        if (removed != null) {
            this.file.syncSave();
            return true;
        }
        return false;
    }

    @Nonnull
    public List<PortalTemplateEntry> getInDimension(@Nonnull String dimension) {
        List<PortalTemplateEntry> result = new ArrayList<>();
        this.file.getEntries().values().forEach(entry -> {
            if (entry.dimension().equals(dimension)) {
                result.add(entry);
            }
        });
        return result;
    }
}
