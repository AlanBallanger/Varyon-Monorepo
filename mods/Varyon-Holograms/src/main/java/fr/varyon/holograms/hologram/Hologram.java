package fr.varyon.holograms.hologram;

import org.joml.Vector3d;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Hologram {

    @Nonnull private final UUID id;
    @Nonnull private String name;
    @Nonnull private Vector3d position;
    @Nonnull private final UUID worldId;
    @Nonnull private List<String> lines;
    @Nullable private UUID creatorId;
    private boolean visible;
    private double lineSpacing;
    @Nonnull private final List<UUID> lineEntityIds;

    public Hologram(@Nonnull String name, @Nonnull Vector3d position, @Nonnull UUID worldId) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.position = new Vector3d(position);
        this.worldId = worldId;
        this.lines = new ArrayList<>();
        this.lineEntityIds = new ArrayList<>();
        this.visible = true;
        this.lineSpacing = 0.35;
    }

    public Hologram(@Nonnull UUID id, @Nonnull String name, @Nonnull Vector3d position, @Nonnull UUID worldId,
                    @Nonnull List<String> lines, double lineSpacing, boolean visible, @Nullable UUID creatorId) {
        this.id = id;
        this.name = name;
        this.position = new Vector3d(position);
        this.worldId = worldId;
        this.lines = new ArrayList<>(lines);
        this.lineEntityIds = new ArrayList<>();
        this.lineSpacing = lineSpacing;
        this.visible = visible;
        this.creatorId = creatorId;
    }

    @Nonnull public UUID getId() { return id; }
    @Nonnull public String getName() { return name; }
    @Nonnull public Vector3d getPosition() { return position; }
    @Nonnull public UUID getWorldId() { return worldId; }
    @Nullable public UUID getCreatorId() { return creatorId; }
    public boolean isVisible() { return visible; }
    public double getLineSpacing() { return lineSpacing; }

    public void setPosition(@Nonnull Vector3d position) { this.position = new Vector3d(position); }
    public void setVisible(boolean visible) { this.visible = visible; }
    public void setCreatorId(@Nullable UUID creatorId) { this.creatorId = creatorId; }
    public void setLineSpacing(double lineSpacing) { this.lineSpacing = lineSpacing; }

    @Nonnull
    public List<String> getLines() { return new ArrayList<>(lines); }

    public void setLines(@Nonnull List<String> lines) { this.lines = new ArrayList<>(lines); }

    public void addLine(@Nonnull String line) { lines.add(line); }

    public void setLine(int index, @Nonnull String line) {
        if (index >= 0 && index < lines.size()) lines.set(index, line);
    }

    public void removeLine(int index) {
        if (index >= 0 && index < lines.size()) lines.remove(index);
    }

    public int getLineCount() { return lines.size(); }

    @Nonnull
    public Vector3d getLinePosition(int lineIndex) {
        return new Vector3d(position.x, position.y - lineIndex * lineSpacing, position.z);
    }

    @Nonnull public List<UUID> getLineEntityIds() { return lineEntityIds; }

    public void clearLineEntityIds() { lineEntityIds.clear(); }

    public void addLineEntityId(@Nonnull UUID entityId) { lineEntityIds.add(entityId); }

    @Override
    public String toString() {
        return "Hologram{id=" + id + ", name='" + name + "', lines=" + lines.size() + ", visible=" + visible + "}";
    }
}
