package fr.varyon.holograms.hologram;

import org.joml.Vector3d;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Hologram {

    public static final int MAX_PAGES = 8;

    @Nonnull private final UUID id;
    @Nonnull private String name;
    @Nonnull private Vector3d position;
    @Nonnull private final UUID worldId;
    @Nonnull private final List<List<String>> pages;
    @Nullable private UUID creatorId;
    private boolean visible;
    private double lineSpacing;
    @Nullable private String group;
    @Nullable private String animation;
    @Nonnull private HologramLayout layout;
    @Nonnull private HologramFacing facing;
    private boolean billboard;
    private boolean carouselEnabled;
    private float carouselIntervalSeconds;
    @Nonnull private CarouselTransition carouselTransition;
    private boolean glow;
    @Nonnull private final List<UUID> lineEntityIds;

    public Hologram(@Nonnull String name, @Nonnull Vector3d position, @Nonnull UUID worldId) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.position = new Vector3d(position);
        this.worldId = worldId;
        this.pages = new ArrayList<>();
        this.pages.add(new ArrayList<>());
        this.lineEntityIds = new ArrayList<>();
        this.visible = true;
        this.lineSpacing = 0.35;
        this.layout = HologramLayout.WALL;
        this.facing = HologramFacing.NORTH;
        this.billboard = false;
        this.carouselEnabled = false;
        this.carouselIntervalSeconds = 5f;
        this.carouselTransition = CarouselTransition.SLIDE_LEFT;
        this.glow = false;
    }

    public Hologram(@Nonnull UUID id, @Nonnull String name, @Nonnull Vector3d position, @Nonnull UUID worldId,
                    @Nonnull List<List<String>> pages, double lineSpacing, boolean visible, @Nullable UUID creatorId,
                    @Nullable String group, @Nullable String animation, @Nonnull HologramLayout layout,
                    @Nonnull HologramFacing facing, boolean billboard,
                    boolean carouselEnabled, float carouselIntervalSeconds,
                    @Nonnull CarouselTransition carouselTransition, boolean glow) {
        this.id = id;
        this.name = name;
        this.position = new Vector3d(position);
        this.worldId = worldId;
        this.pages = deepCopyPages(pages);
        if (this.pages.isEmpty()) {
            this.pages.add(new ArrayList<>());
        }
        this.lineEntityIds = new ArrayList<>();
        this.lineSpacing = lineSpacing;
        this.visible = visible;
        this.creatorId = creatorId;
        this.group = group;
        this.animation = animation;
        this.layout = layout;
        this.facing = facing;
        this.billboard = billboard;
        this.carouselEnabled = carouselEnabled;
        this.carouselIntervalSeconds = carouselIntervalSeconds;
        this.carouselTransition = carouselTransition;
        this.glow = glow;
    }

    @Nonnull public UUID getId() { return id; }
    @Nonnull public String getName() { return name; }
    @Nonnull public Vector3d getPosition() { return position; }
    @Nonnull public UUID getWorldId() { return worldId; }
    @Nullable public UUID getCreatorId() { return creatorId; }
    public boolean isVisible() { return visible; }
    public double getLineSpacing() { return lineSpacing; }
    @Nullable public String getGroup() { return group; }
    @Nullable public String getAnimation() { return animation; }
    @Nonnull public HologramLayout getLayout() { return layout; }
    @Nonnull public HologramFacing getFacing() { return facing; }
    public boolean isBillboard() { return billboard; }
    public boolean isCarouselEnabled() { return carouselEnabled; }
    public float getCarouselIntervalSeconds() { return carouselIntervalSeconds; }
    @Nonnull public CarouselTransition getCarouselTransition() { return carouselTransition; }
    public boolean isGlow() { return glow; }

    public void setPosition(@Nonnull Vector3d position) { this.position = new Vector3d(position); }
    public void setName(@Nonnull String name) { this.name = name; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public void setCreatorId(@Nullable UUID creatorId) { this.creatorId = creatorId; }
    public void setLineSpacing(double lineSpacing) { this.lineSpacing = lineSpacing; }
    public void setGroup(@Nullable String group) { this.group = group; }
    public void setAnimation(@Nullable String animation) { this.animation = animation; }
    public void setLayout(@Nonnull HologramLayout layout) { this.layout = layout; }
    public void setFacing(@Nonnull HologramFacing facing) { this.facing = facing; }
    public void setBillboard(boolean billboard) { this.billboard = billboard; }
    public void setCarouselEnabled(boolean carouselEnabled) { this.carouselEnabled = carouselEnabled; }
    public void setCarouselIntervalSeconds(float carouselIntervalSeconds) {
        this.carouselIntervalSeconds = Math.max(1f, Math.min(120f, carouselIntervalSeconds));
    }
    public void setCarouselTransition(@Nonnull CarouselTransition carouselTransition) {
        this.carouselTransition = carouselTransition;
    }
    public void setGlow(boolean glow) { this.glow = glow; }

    public boolean isCarouselActive() {
        return carouselEnabled && getPageCount() > 1;
    }

    public int getPageCount() { return pages.size(); }

    @Nonnull
    public List<List<String>> getPagesCopy() { return deepCopyPages(pages); }

    @Nonnull
    public List<String> getPageLines(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= pages.size()) return List.of();
        return new ArrayList<>(pages.get(pageIndex));
    }

    public void setPageLines(int pageIndex, @Nonnull List<String> lines) {
        ensurePageIndex(pageIndex);
        pages.set(pageIndex, new ArrayList<>(lines));
    }

    public void addPageLine(int pageIndex, @Nonnull String line) {
        ensurePageIndex(pageIndex);
        pages.get(pageIndex).add(line);
    }

    public void setPageLine(int pageIndex, int lineIndex, @Nonnull String line) {
        ensurePageIndex(pageIndex);
        List<String> page = pages.get(pageIndex);
        if (lineIndex >= 0 && lineIndex < page.size()) {
            page.set(lineIndex, line);
        }
    }

    public void removePageLine(int pageIndex, int lineIndex) {
        ensurePageIndex(pageIndex);
        List<String> page = pages.get(pageIndex);
        if (lineIndex >= 0 && lineIndex < page.size()) {
            page.remove(lineIndex);
        }
    }

    public int getPageLineCount(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= pages.size()) return 0;
        return pages.get(pageIndex).size();
    }

    public boolean addPage() {
        if (pages.size() >= MAX_PAGES) return false;
        pages.add(new ArrayList<>());
        return true;
    }

    public void removePage(int pageIndex) {
        if (pages.size() <= 1 || pageIndex < 0 || pageIndex >= pages.size()) return;
        pages.remove(pageIndex);
    }

    @Nonnull
    public List<String> getLines() { return getPageLines(0); }

    public void setLines(@Nonnull List<String> lines) { setPageLines(0, lines); }

    public void addLine(@Nonnull String line) { addPageLine(0, line); }

    public void setLine(int index, @Nonnull String line) { setPageLine(0, index, line); }

    public void removeLine(int index) { removePageLine(0, index); }

    public int getLineCount() { return getPageLineCount(0); }

    @Nonnull
    public Vector3d getLinePosition(int lineIndex) {
        Vector3d off = layout.lineOffset(lineIndex, lineSpacing, HologramLineType.TEXT);
        return new Vector3d(position.x + off.x, position.y + off.y, position.z + off.z);
    }

    @Nonnull public List<UUID> getLineEntityIds() { return lineEntityIds; }

    public void clearLineEntityIds() { lineEntityIds.clear(); }

    public void addLineEntityId(@Nonnull UUID entityId) { lineEntityIds.add(entityId); }

    private void ensurePageIndex(int pageIndex) {
        while (pages.size() <= pageIndex && pages.size() < MAX_PAGES) {
            pages.add(new ArrayList<>());
        }
    }

    @Nonnull
    private static List<List<String>> deepCopyPages(@Nonnull List<List<String>> source) {
        List<List<String>> copy = new ArrayList<>();
        for (List<String> page : source) {
            copy.add(new ArrayList<>(page));
        }
        return copy;
    }

    @Override
    public String toString() {
        return "Hologram{id=" + id + ", name='" + name + "', pages=" + pages.size() + ", visible=" + visible + "}";
    }
}
