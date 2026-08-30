package com.varyon.map;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.packets.worldmap.MapImage;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkColumn;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.palette.BitFieldArr;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import com.varyon.config.ConfigManager;
import com.varyon.config.DifficultyZone;
import com.varyon.config.ZoneConfig;
import com.varyon.util.ZoneCalculator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public class ZoneMapImageBuilder {
    private final long index;
    private final World world;
    private final ConfigManager configManager;
    private final int imageWidth;
    private final int imageHeight;
    @Nonnull
    private final int[] rawPixels;
    @Nullable
    private MapImage encodedImage;
    private final int sampleWidth;
    private final int sampleHeight;
    private final int blockStepX;
    private final int blockStepZ;
    @Nonnull
    private final short[] heightSamples;
    @Nonnull
    private final int[] tintSamples;
    @Nonnull
    private final int[] blockSamples;
    @Nonnull
    private final short[] neighborHeightSamples;
    @Nonnull
    private final short[] fluidDepthSamples;
    @Nonnull
    private final int[] environmentSamples;
    @Nonnull
    private final int[] fluidSamples;
    private final RgbColor outColor = new RgbColor();
    @Nullable
    private WorldChunk worldChunk;
    private FluidSection[] fluidSections;

    public ZoneMapImageBuilder(long index, int imageWidth, int imageHeight, World world, ConfigManager configManager) {
        this.index = index;
        this.world = world;
        this.configManager = configManager;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.rawPixels = new int[imageWidth * imageHeight];
        this.sampleWidth = Math.min(32, imageWidth);
        this.sampleHeight = Math.min(32, imageHeight);
        this.blockStepX = Math.max(1, 32 / imageWidth);
        this.blockStepZ = Math.max(1, 32 / imageHeight);
        this.heightSamples = new short[this.sampleWidth * this.sampleHeight];
        this.tintSamples = new int[this.sampleWidth * this.sampleHeight];
        this.blockSamples = new int[this.sampleWidth * this.sampleHeight];
        this.neighborHeightSamples = new short[(this.sampleWidth + 2) * (this.sampleHeight + 2)];
        this.fluidDepthSamples = new short[this.sampleWidth * this.sampleHeight];
        this.environmentSamples = new int[this.sampleWidth * this.sampleHeight];
        this.fluidSamples = new int[this.sampleWidth * this.sampleHeight];
    }

    public long getIndex() {
        return this.index;
    }

    @Nonnull
    public MapImage getImage() {
        if (this.encodedImage == null) {
            this.encodedImage = encodeToPalette();
        }
        return this.encodedImage;
    }

    @Nonnull
    private MapImage encodeToPalette() {
        int pixelCount = this.rawPixels.length;
        IntOpenHashSet uniqueColors = new IntOpenHashSet();
        for (int i = 0; i < pixelCount; ++i) {
            uniqueColors.add(this.rawPixels[i]);
        }
        int[] palette = uniqueColors.toIntArray();
        int bitsPerIndex = calculateBitsRequired(palette.length);
        Int2IntOpenHashMap colorToIndex = new Int2IntOpenHashMap(palette.length);
        for (int i = 0; i < palette.length; ++i) {
            colorToIndex.put(palette[i], i);
        }
        BitFieldArr indices = new BitFieldArr(bitsPerIndex, pixelCount);
        for (int i = 0; i < pixelCount; ++i) {
            indices.set(i, colorToIndex.get(this.rawPixels[i]));
        }
        byte[] packedIndices = indices.get();
        return new MapImage(this.imageWidth, this.imageHeight, palette, (byte) bitsPerIndex, packedIndices);
    }

    private static int calculateBitsRequired(int colorCount) {
        if (colorCount <= 16) {
            return 4;
        }
        if (colorCount <= 256) {
            return 8;
        }
        if (colorCount <= 4096) {
            return 12;
        }
        return 16;
    }

    @Nonnull
    private CompletableFuture<ZoneMapImageBuilder> fetchChunk() {
        return this.world.getChunkStore().getChunkReferenceAsync(this.index).thenApplyAsync(ref -> {
            if (ref != null && ref.isValid()) {
                this.worldChunk = (WorldChunk) ref.getStore().getComponent(ref, WorldChunk.getComponentType());
                ChunkColumn chunkColumn = (ChunkColumn) ref.getStore().getComponent(ref, ChunkColumn.getComponentType());
                this.fluidSections = new FluidSection[10];
                for (int y = 0; y < 10; y++) {
                    Ref sectionRef = chunkColumn.getSection(y);
                    this.fluidSections[y] = (FluidSection) this.world.getChunkStore().getStore().getComponent(sectionRef, FluidSection.getComponentType());
                }
                return this;
            }
            return null;
        }, (Executor) this.world);
    }

    @Nonnull
    private CompletableFuture<ZoneMapImageBuilder> sampleNeighborsSync() {
        CompletionStage<Void> north = this.world.getChunkStore().getChunkReferenceAsync(ChunkUtil.indexChunk(this.worldChunk.getX(), this.worldChunk.getZ() - 1)).thenAcceptAsync(ref -> {
            if (ref != null && ref.isValid()) {
                WorldChunk worldChunk = (WorldChunk) ref.getStore().getComponent(ref, WorldChunk.getComponentType());
                int z = (this.sampleHeight - 1) * this.blockStepZ;
                for (int ix = 0; ix < this.sampleWidth; ix++) {
                    int x = ix * this.blockStepX;
                    this.neighborHeightSamples[1 + ix] = worldChunk.getHeight(x, z);
                }
            }
        }, (Executor) this.world);

        CompletionStage<Void> south = this.world.getChunkStore().getChunkReferenceAsync(ChunkUtil.indexChunk(this.worldChunk.getX(), this.worldChunk.getZ() + 1)).thenAcceptAsync(ref -> {
            if (ref != null && ref.isValid()) {
                WorldChunk worldChunk = (WorldChunk) ref.getStore().getComponent(ref, WorldChunk.getComponentType());
                int z = 0;
                int neighbourStartIndex = (this.sampleHeight + 1) * (this.sampleWidth + 2) + 1;
                for (int ix = 0; ix < this.sampleWidth; ix++) {
                    int x = ix * this.blockStepX;
                    this.neighborHeightSamples[neighbourStartIndex + ix] = worldChunk.getHeight(x, z);
                }
            }
        }, (Executor) this.world);

        CompletionStage<Void> west = this.world.getChunkStore().getChunkReferenceAsync(ChunkUtil.indexChunk(this.worldChunk.getX() - 1, this.worldChunk.getZ())).thenAcceptAsync(ref -> {
            if (ref != null && ref.isValid()) {
                WorldChunk worldChunk = (WorldChunk) ref.getStore().getComponent(ref, WorldChunk.getComponentType());
                int x = (this.sampleWidth - 1) * this.blockStepX;
                for (int iz = 0; iz < this.sampleHeight; iz++) {
                    int z = iz * this.blockStepZ;
                    this.neighborHeightSamples[(iz + 1) * (this.sampleWidth + 2)] = worldChunk.getHeight(x, z);
                }
            }
        }, (Executor) this.world);

        CompletionStage<Void> east = this.world.getChunkStore().getChunkReferenceAsync(ChunkUtil.indexChunk(this.worldChunk.getX() + 1, this.worldChunk.getZ())).thenAcceptAsync(ref -> {
            if (ref != null && ref.isValid()) {
                WorldChunk worldChunk = (WorldChunk) ref.getStore().getComponent(ref, WorldChunk.getComponentType());
                int x = 0;
                for (int iz = 0; iz < this.sampleHeight; iz++) {
                    int z = iz * this.blockStepZ;
                    this.neighborHeightSamples[(iz + 1) * (this.sampleWidth + 2) + this.sampleWidth + 1] = worldChunk.getHeight(x, z);
                }
            }
        }, (Executor) this.world);

        CompletionStage<Void> northeast = this.world.getChunkStore().getChunkReferenceAsync(ChunkUtil.indexChunk(this.worldChunk.getX() + 1, this.worldChunk.getZ() - 1)).thenAcceptAsync(ref -> {
            if (ref != null && ref.isValid()) {
                WorldChunk worldChunk = (WorldChunk) ref.getStore().getComponent(ref, WorldChunk.getComponentType());
                int x = 0;
                int z = (this.sampleHeight - 1) * this.blockStepZ;
                this.neighborHeightSamples[0] = worldChunk.getHeight(x, z);
            }
        }, (Executor) this.world);

        CompletionStage<Void> northwest = this.world.getChunkStore().getChunkReferenceAsync(ChunkUtil.indexChunk(this.worldChunk.getX() - 1, this.worldChunk.getZ() - 1)).thenAcceptAsync(ref -> {
            if (ref != null && ref.isValid()) {
                WorldChunk worldChunk = (WorldChunk) ref.getStore().getComponent(ref, WorldChunk.getComponentType());
                int x = (this.sampleWidth - 1) * this.blockStepX;
                int z = (this.sampleHeight - 1) * this.blockStepZ;
                this.neighborHeightSamples[this.sampleWidth + 1] = worldChunk.getHeight(x, z);
            }
        }, (Executor) this.world);

        CompletionStage<Void> southeast = this.world.getChunkStore().getChunkReferenceAsync(ChunkUtil.indexChunk(this.worldChunk.getX() + 1, this.worldChunk.getZ() + 1)).thenAcceptAsync(ref -> {
            if (ref != null && ref.isValid()) {
                WorldChunk worldChunk = (WorldChunk) ref.getStore().getComponent(ref, WorldChunk.getComponentType());
                int x = 0;
                int z = 0;
                this.neighborHeightSamples[(this.sampleHeight + 1) * (this.sampleWidth + 2) + this.sampleWidth + 1] = worldChunk.getHeight(x, z);
            }
        }, (Executor) this.world);

        CompletionStage<Void> southwest = this.world.getChunkStore().getChunkReferenceAsync(ChunkUtil.indexChunk(this.worldChunk.getX() - 1, this.worldChunk.getZ() + 1)).thenAcceptAsync(ref -> {
            if (ref != null && ref.isValid()) {
                WorldChunk worldChunk = (WorldChunk) ref.getStore().getComponent(ref, WorldChunk.getComponentType());
                int x = (this.sampleWidth - 1) * this.blockStepX;
                int z = 0;
                this.neighborHeightSamples[(this.sampleHeight + 1) * (this.sampleWidth + 2)] = worldChunk.getHeight(x, z);
            }
        }, (Executor) this.world);

        return CompletableFuture.allOf(north.toCompletableFuture(), south.toCompletableFuture(), west.toCompletableFuture(), east.toCompletableFuture(), northeast.toCompletableFuture(), northwest.toCompletableFuture(), southeast.toCompletableFuture(), southwest.toCompletableFuture()).thenApply(v -> this);
    }

    private ZoneMapImageBuilder generateImageAsync() {
        if (this.worldChunk == null) {
            return this;
        }

        // Sample terrain data
        for (int ix = 0; ix < this.sampleWidth; ix++) {
            for (int iz = 0; iz < this.sampleHeight; iz++) {
                int sampleIndex = iz * this.sampleWidth + ix;
                int x = ix * this.blockStepX;
                int z = iz * this.blockStepZ;
                int height = this.worldChunk.getHeight(x, z);
                int tint = this.worldChunk.getBlockChunk().getTint(x, z);
                this.heightSamples[sampleIndex] = (short) height;
                this.tintSamples[sampleIndex] = tint;
                this.blockSamples[sampleIndex] = this.worldChunk.getBlock(x, height, z);

                // Fluid sampling
                int fluidId = 0;
                int fluidTop = 320;
                Fluid fluid = null;
                int chunkYGround = ChunkUtil.chunkCoordinate(height);
                int chunkY = 9;

                block2:
                while (chunkY >= 0 && chunkY >= chunkYGround) {
                    FluidSection fluidSection = this.fluidSections[chunkY];
                    if (fluidSection != null && !fluidSection.isEmpty()) {
                        int minBlockY = Math.max(ChunkUtil.minBlock(chunkY), height);
                        int maxBlockY = ChunkUtil.maxBlock(chunkY);
                        for (int blockY = maxBlockY; blockY >= minBlockY; blockY--) {
                            fluidId = fluidSection.getFluidId(x, blockY, z);
                            if (fluidId != 0) {
                                fluid = (Fluid) Fluid.getAssetMap().getAsset(fluidId);
                                fluidTop = blockY;
                                break block2;
                            }
                        }
                    }
                    chunkY--;
                }

                int fluidBottom = height;
                block4:
                while (chunkY >= 0 && chunkY >= chunkYGround) {
                    FluidSection fluidSection = this.fluidSections[chunkY];
                    if (fluidSection == null || fluidSection.isEmpty()) {
                        fluidBottom = Math.min(ChunkUtil.maxBlock(chunkY) + 1, fluidTop);
                        break;
                    }
                    int minBlockY = Math.max(ChunkUtil.minBlock(chunkY), height);
                    int maxBlockY = Math.min(ChunkUtil.maxBlock(chunkY), fluidTop - 1);
                    for (int blockY = maxBlockY; blockY >= minBlockY; blockY--) {
                        int nextFluidId = fluidSection.getFluidId(x, blockY, z);
                        if (nextFluidId != fluidId) {
                            Fluid nextFluid = (Fluid) Fluid.getAssetMap().getAsset(nextFluidId);
                            if (!Objects.equals(fluid.getParticleColor(), nextFluid.getParticleColor())) {
                                fluidBottom = blockY + 1;
                                break block4;
                            }
                        }
                    }
                    chunkY--;
                }

                short fluidDepth = fluidId != 0 ? (short) (fluidTop - fluidBottom + 1) : 0;
                int environmentId = this.worldChunk.getBlockChunk().getEnvironment(x, fluidTop, z);
                this.fluidDepthSamples[sampleIndex] = fluidDepth;
                this.environmentSamples[sampleIndex] = environmentId;
                this.fluidSamples[sampleIndex] = fluidId;
            }
        }

        float imageToSampleRatioWidth = (float) this.sampleWidth / (float) this.imageWidth;
        float imageToSampleRatioHeight = (float) this.sampleHeight / (float) this.imageHeight;
        int blockPixelWidth = Math.max(1, this.imageWidth / this.sampleWidth);
        int blockPixelHeight = Math.max(1, this.imageHeight / this.sampleHeight);

        for (int iz = 0; iz < this.sampleHeight; iz++) {
            System.arraycopy(this.heightSamples, iz * this.sampleWidth, this.neighborHeightSamples, (iz + 1) * (this.sampleWidth + 2) + 1, this.sampleWidth);
        }

        int chunkX = ChunkUtil.xOfChunkIndex(this.index);
        int chunkZ = ChunkUtil.zOfChunkIndex(this.index);
        int minBlockX = ChunkUtil.minBlock(chunkX);
        int minBlockZ = ChunkUtil.minBlock(chunkZ);

        ZoneConfig zoneConfig = configManager.getZoneConfig();

        // Generate image with zone overlay
        for (int ix = 0; ix < this.imageWidth; ix++) {
            for (int iz = 0; iz < this.imageHeight; iz++) {
                int sampleX = Math.min((int) ((float) ix * imageToSampleRatioWidth), this.sampleWidth - 1);
                int sampleZ = Math.min((int) ((float) iz * imageToSampleRatioHeight), this.sampleHeight - 1);
                int sampleIndex = sampleZ * this.sampleWidth + sampleX;
                int blockPixelX = ix % blockPixelWidth;
                int blockPixelZ = iz % blockPixelHeight;
                short height = this.heightSamples[sampleIndex];
                int tint = this.tintSamples[sampleIndex];
                int blockId = this.blockSamples[sampleIndex];

                // Get base terrain color
                getBlockColor(blockId, tint, this.outColor);

                // Apply shading FIRST (before zone overlay)
                short north = this.neighborHeightSamples[sampleZ * (this.sampleWidth + 2) + sampleX + 1];
                short south = this.neighborHeightSamples[(sampleZ + 2) * (this.sampleWidth + 2) + sampleX + 1];
                short west = this.neighborHeightSamples[(sampleZ + 1) * (this.sampleWidth + 2) + sampleX];
                short east = this.neighborHeightSamples[(sampleZ + 1) * (this.sampleWidth + 2) + sampleX + 2];
                short northWest = this.neighborHeightSamples[sampleZ * (this.sampleWidth + 2) + sampleX];
                short northEast = this.neighborHeightSamples[sampleZ * (this.sampleWidth + 2) + sampleX + 2];
                short southWest = this.neighborHeightSamples[(sampleZ + 2) * (this.sampleWidth + 2) + sampleX];
                short southEast = this.neighborHeightSamples[(sampleZ + 2) * (this.sampleWidth + 2) + sampleX + 2];
                float shade = shadeFromHeights(blockPixelX, blockPixelZ, blockPixelWidth, blockPixelHeight, height, north, south, west, east, northWest, northEast, southWest, southEast);
                this.outColor.multiply(shade);

                // Apply fluid color if present
                int fluidId = this.fluidSamples[sampleIndex];
                if (height < 320 && fluidId != 0) {
                    short fluidDepth = this.fluidDepthSamples[sampleIndex];
                    int environmentId = this.environmentSamples[sampleIndex];
                    getFluidColor(fluidId, environmentId, fluidDepth, this.outColor);
                }

                // Calculate world position for this pixel
                int blockX = minBlockX + ix * 32 / this.imageWidth;
                int blockZ = minBlockZ + iz * 32 / this.imageHeight;

                // Apply zone overlay LAST (on top of everything) — border only, no translucent
                // fill over the whole zone interior (interior left showing plain terrain).
                DifficultyZone zone = ZoneCalculator.getZoneAtPosition(blockX, blockZ, zoneConfig);
                if (zone != null && zone.getMaxMultiplier() > 1.0) {
                    int borderThickness = 3; // Border width in blocks
                    if (isZoneBorder(blockX, blockZ, zone, zoneConfig, borderThickness)) {
                        int zoneColor = getColorForZone(zone);
                        // Draw border with full opacity (no pattern, solid color)
                        blendColor(zoneColor, this.outColor, 1.0f, 0.85f);
                    }
                }

                this.rawPixels[iz * this.imageWidth + ix] = this.outColor.pack();
            }
        }

        return this;
    }

    private int getColorForZone(DifficultyZone zone) {
        java.awt.Color color = zone.getParsedColor();
        return (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
    }

    /**
     * Check if this position is at a zone border (different zone within threshold distance).
     */
    private boolean isZoneBorder(int blockX, int blockZ, DifficultyZone currentZone, ZoneConfig config, int thickness) {
        // Check points around this position to see if they're in a different zone
        for (int dx = -thickness; dx <= thickness; dx++) {
            for (int dz = -thickness; dz <= thickness; dz++) {
                if (dx == 0 && dz == 0) continue;

                DifficultyZone neighborZone = ZoneCalculator.getZoneAtPosition(blockX + dx, blockZ + dz, config);
                if (neighborZone == null || neighborZone.getZoneId() != currentZone.getZoneId()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if a pixel should be colored based on the pattern type.
     * Uses WORLD coordinates (blockX, blockZ) so pattern is seamless across chunks.
     */
    private boolean shouldApplyPattern(int blockX, int blockZ, String pattern, int patternSize) {
        switch (pattern.toUpperCase()) {
            case "SOLID":
                return true;

            case "DOTS":
                // Dots pattern: only color pixels at regular intervals
                return (positiveMod(blockX, patternSize) == 0) && (positiveMod(blockZ, patternSize) == 0);

            case "STRIPES":
                // Diagonal stripes pattern (consistent direction everywhere)
                return positiveMod(blockX + blockZ, patternSize) == 0;

            case "CROSSHATCH":
                // Crosshatch: both diagonal directions
                return positiveMod(blockX + blockZ, patternSize) == 0 || positiveMod(blockX - blockZ, patternSize) == 0;

            case "GRID":
                // Grid pattern: horizontal and vertical lines
                return positiveMod(blockX, patternSize) == 0 || positiveMod(blockZ, patternSize) == 0;

            case "CHECKER":
                // Checkerboard pattern
                int cx = blockX >= 0 ? blockX / patternSize : (blockX - patternSize + 1) / patternSize;
                int cz = blockZ >= 0 ? blockZ / patternSize : (blockZ - patternSize + 1) / patternSize;
                return (cx + cz) % 2 == 0;

            default:
                return true;
        }
    }

    /**
     * Positive modulo that works correctly with negative numbers.
     * Always returns a value in [0, divisor)
     */
    private int positiveMod(int value, int divisor) {
        int mod = value % divisor;
        return mod < 0 ? mod + divisor : mod;
    }

    private void blendColor(int color, RgbColor outColor, float multiplier, float opacity) {
        if (opacity <= 0.0f) {
            return;
        }
        if (opacity >= 1.0f) {
            setDirectColor(color, outColor, multiplier);
            return;
        }
        int regionR = (int) ((float) ((color >> 16) & 0xFF) * multiplier);
        int regionG = (int) ((float) ((color >> 8) & 0xFF) * multiplier);
        int regionB = (int) ((float) (color & 0xFF) * multiplier);
        outColor.r = (int) ((float) outColor.r * (1.0f - opacity) + (float) regionR * opacity);
        outColor.g = (int) ((float) outColor.g * (1.0f - opacity) + (float) regionG * opacity);
        outColor.b = (int) ((float) outColor.b * (1.0f - opacity) + (float) regionB * opacity);
        outColor.a = 255;
    }

    private void setDirectColor(int color, RgbColor outColor, float multiplier) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        outColor.r = (int) ((float) r * multiplier);
        outColor.g = (int) ((float) g * multiplier);
        outColor.b = (int) ((float) b * multiplier);
        outColor.a = 255;
    }

    private static float shadeFromHeights(int blockPixelX, int blockPixelZ, int blockPixelWidth, int blockPixelHeight, short height, short north, short south, short west, short east, short northWest, short northEast, short southWest, short southEast) {
        float u = ((float) blockPixelX + 0.5f) / (float) blockPixelWidth;
        float v = ((float) blockPixelZ + 0.5f) / (float) blockPixelHeight;
        float ud = (u + v) / 2.0f;
        float vd = (1.0f - u + v) / 2.0f;
        float dhdx1 = (float) (height - west) * (1.0f - u) + (float) (east - height) * u;
        float dhdz1 = (float) (height - north) * (1.0f - v) + (float) (south - height) * v;
        float dhdx2 = (float) (height - northWest) * (1.0f - ud) + (float) (southEast - height) * ud;
        float dhdz2 = (float) (height - northEast) * (1.0f - vd) + (float) (southWest - height) * vd;
        float dhdx = dhdx1 * 2.0f + dhdx2;
        float dhdz = dhdz1 * 2.0f + dhdz2;
        float dy = 3.0f;
        float invS = 1.0f / (float) Math.sqrt(dhdx * dhdx + dy * dy + dhdz * dhdz);
        float nx = dhdx * invS;
        float ny = dy * invS;
        float nz = dhdz * invS;
        float lx = -0.2f;
        float ly = 0.8f;
        float lz = 0.5f;
        float invL = 1.0f / (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
        lx *= invL;
        ly *= invL;
        lz *= invL;
        float lambert = Math.max(0.0f, nx * lx + ny * ly + nz * lz);
        float ambient = 0.4f;
        float diffuse = 0.6f;
        return ambient + diffuse * lambert;
    }

    private static void getBlockColor(int blockId, int biomeTintColor, @Nonnull RgbColor outColor) {
        BlockType block = (BlockType) BlockType.getAssetMap().getAsset(blockId);
        int biomeTintR = (biomeTintColor >> 16) & 0xFF;
        int biomeTintG = (biomeTintColor >> 8) & 0xFF;
        int biomeTintB = biomeTintColor & 0xFF;
        Color[] tintUp = block.getTintUp();
        boolean hasTint = tintUp != null && tintUp.length > 0;
        int selfTintR = hasTint ? tintUp[0].red & 0xFF : 255;
        int selfTintG = hasTint ? tintUp[0].green & 0xFF : 255;
        int selfTintB = hasTint ? tintUp[0].blue & 0xFF : 255;
        float biomeTintMultiplier = (float) block.getBiomeTintUp() / 100.0f;
        int tintColorR = (int) ((float) selfTintR + (float) (biomeTintR - selfTintR) * biomeTintMultiplier);
        int tintColorG = (int) ((float) selfTintG + (float) (biomeTintG - selfTintG) * biomeTintMultiplier);
        int tintColorB = (int) ((float) selfTintB + (float) (biomeTintB - selfTintB) * biomeTintMultiplier);
        Color particleColor = block.getParticleColor();
        if (particleColor != null && biomeTintMultiplier < 1.0f) {
            tintColorR = tintColorR * (particleColor.red & 0xFF) / 255;
            tintColorG = tintColorG * (particleColor.green & 0xFF) / 255;
            tintColorB = tintColorB * (particleColor.blue & 0xFF) / 255;
        }
        outColor.r = tintColorR & 0xFF;
        outColor.g = tintColorG & 0xFF;
        outColor.b = tintColorB & 0xFF;
        outColor.a = 255;
    }

    private static void getFluidColor(int fluidId, int environmentId, int fluidDepth, @Nonnull RgbColor outColor) {
        Fluid fluid = (Fluid) Fluid.getAssetMap().getAsset(fluidId);
        int tintColorR = 255;
        int tintColorG = 255;
        int tintColorB = 255;
        Environment environment = (Environment) Environment.getAssetMap().getAsset(environmentId);
        Color waterTint = environment.getWaterTint();
        if (waterTint != null) {
            tintColorR = tintColorR * (waterTint.red & 0xFF) / 255;
            tintColorG = tintColorG * (waterTint.green & 0xFF) / 255;
            tintColorB = tintColorB * (waterTint.blue & 0xFF) / 255;
        }
        Color particleColor = fluid.getParticleColor();
        if (particleColor != null) {
            tintColorR = tintColorR * (particleColor.red & 0xFF) / 255;
            tintColorG = tintColorG * (particleColor.green & 0xFF) / 255;
            tintColorB = tintColorB * (particleColor.blue & 0xFF) / 255;
        }
        float depthMultiplier = Math.min(1.0f, 1.0f / (float) fluidDepth);
        outColor.r = (int) ((float) tintColorR + (float) ((outColor.r & 0xFF) - tintColorR) * depthMultiplier) & 0xFF;
        outColor.g = (int) ((float) tintColorG + (float) ((outColor.g & 0xFF) - tintColorG) * depthMultiplier) & 0xFF;
        outColor.b = (int) ((float) tintColorB + (float) ((outColor.b & 0xFF) - tintColorB) * depthMultiplier) & 0xFF;
    }

    @Nonnull
    public static CompletableFuture<ZoneMapImageBuilder> build(long index, int imageWidth, int imageHeight, World world, ConfigManager configManager) {
        return CompletableFuture.completedFuture(new ZoneMapImageBuilder(index, imageWidth, imageHeight, world, configManager))
                .thenCompose(ZoneMapImageBuilder::fetchChunk)
                .thenCompose(builder -> builder != null ? builder.sampleNeighborsSync() : CompletableFuture.completedFuture(null))
                .thenApplyAsync(builder -> builder != null ? builder.generateImageAsync() : null);
    }

    private static class RgbColor {
        public int r;
        public int g;
        public int b;
        public int a;

        public int pack() {
            // RGBA format (like SimpleClaims): R in high byte, then G, B, A
            return (this.r & 0xFF) << 24 | (this.g & 0xFF) << 16 | (this.b & 0xFF) << 8 | this.a & 0xFF;
        }

        public void multiply(float value) {
            this.r = Math.min(255, Math.max(0, (int) ((float) this.r * value)));
            this.g = Math.min(255, Math.max(0, (int) ((float) this.g * value)));
            this.b = Math.min(255, Math.max(0, (int) ((float) this.b * value)));
        }
    }
}
