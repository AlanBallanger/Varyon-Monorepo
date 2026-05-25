package com.varyon.teleport;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import org.joml.Vector3d;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.chunk.ZoneBiomeResult;
import com.hypixel.hytale.server.worldgen.zone.Zone;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiPredicate;
import java.util.logging.Level;

public class RtpService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int MAX_SEARCH_RADIUS = 10000;
    private static final int START_Y = 320;
    private static final int MIN_Y = 0;
    /** Tentatives par défaut pour rtpz / rtps (zone + sol + chunk). */
    public static final int DEFAULT_RTP_MAX_ATTEMPTS = 150;
    private static final int RTP_CHUNK_SIZE = 16;
    private static final int RTP_FINE_CHUNK_RADIUS = 6;
    private static final int RTP_COARSE_CHUNK_STEP = 8;
    private static final int RTP_COARSE_RADIUS_STEPS = 12;
    private static final int RTP_MAX_ASYNC_LOADS_PER_ANCHOR = 5;
    private static final int RTP_MAX_COLUMN_CHUNK_LOADS = 8;
    private final Random random = new Random();

    private static final int[][] COLUMN_NEIGHBOR_OFFSETS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1},
        {1, 1}, {1, -1}, {-1, 1}, {-1, -1},
        {2, 0}, {0, 2}
    };

    private static final int[][] COLUMN_NEIGHBOR_OFFSETS_FAR = {
        {3, 0}, {-3, 0}, {0, 3}, {0, -3},
        {2, 2}, {2, -2}, {-2, 2}, {-2, -2}
    };

    /**
     * Résout une zone Hytale (worldgen) par préfixe de nom, comme {@code /rtpz zone1}.
     */
    @Nullable
    public Zone resolveZoneByPrefix(@Nonnull ChunkGenerator generator, @Nonnull String prefixRaw) {
        Zone[] all = resolveAllZonesByPrefix(generator, prefixRaw);
        if (all == null || all.length == 0) {
            return null;
        }
        return all[random.nextInt(all.length)];
    }

    @Nullable
    public Zone[] resolveAllZonesByPrefix(@Nonnull ChunkGenerator generator, @Nonnull String prefixRaw) {
        String prefix = prefixRaw.trim().toLowerCase();
        if (prefix.isEmpty()) {
            return null;
        }
        Zone[] zones = generator.getZonePatternProvider().getZones();
        List<Zone> matching = new ArrayList<>();
        for (Zone zone : zones) {
            String zoneName = zone.name().toLowerCase();
            if (zoneName.startsWith(prefix) && !zoneName.contains("ocean")) {
                matching.add(zone);
            }
        }
        if (matching.isEmpty()) {
            for (Zone zone : zones) {
                if (zone.name().toLowerCase().startsWith(prefix)) {
                    matching.add(zone);
                }
            }
        }
        return matching.toArray(new Zone[0]);
    }

    @Nonnull
    public Zone[] shuffleZones(@Nonnull Zone[] zones) {
        Zone[] copy = zones.clone();
        for (int i = copy.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Zone tmp = copy[i];
            copy[i] = copy[j];
            copy[j] = tmp;
        }
        return copy;
    }

    @Nullable
    public Vector3d findSafePosition(@Nonnull World world, @Nonnull ChunkGenerator generator, 
                                     @Nullable Zone targetZone, int maxAttempts) {
        return findSafePosition(world, generator, targetZone, maxAttempts, null, null);
    }

    @Nullable
    public Vector3d findSafePosition(@Nonnull World world, @Nonnull ChunkGenerator generator, 
                                     @Nullable Zone targetZone, int maxAttempts, Double targetX, Double targetZ) {
        int seed = (int) world.getWorldConfig().getSeed();
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int x, z;
            
            if (targetX != null && targetZ != null) {
                double radiusVariation = 50.0;
                double angleVariation = random.nextDouble() * 2 * Math.PI;
                double distVariation = random.nextDouble() * radiusVariation;
                x = (int) (targetX + Math.cos(angleVariation) * distVariation);
                z = (int) (targetZ + Math.sin(angleVariation) * distVariation);
            } else if (targetZone != null) {
                int w = MAX_SEARCH_RADIUS * 2;
                int bx = random.nextInt(w) - MAX_SEARCH_RADIUS;
                int bz = random.nextInt(w) - MAX_SEARCH_RADIUS;
                Vector3d found = tryRtpZoneWithAnchor(world, generator, seed, bx, bz, targetZone, (nx, nz) -> true);
                if (found != null) {
                    ZoneBiomeResult result = generator.getZoneBiomeResultAt(seed, (int) Math.floor(found.x), (int) Math.floor(found.z));
                    LOGGER.at(Level.INFO).log("Position trouvée après " + (attempt + 1) + " tentatives dans: " + result.getZoneResult().getZone().name());
                    return found;
                }
                continue;
            } else {
                x = random.nextInt(MAX_SEARCH_RADIUS * 2) - MAX_SEARCH_RADIUS;
                z = random.nextInt(MAX_SEARCH_RADIUS * 2) - MAX_SEARCH_RADIUS;
            }

            ZoneBiomeResult result = generator.getZoneBiomeResultAt(seed, x, z);
            Zone foundZone = result.getZoneResult().getZone();

            if (targetZone != null && foundZone.id() != targetZone.id()) {
                continue;
            }

            Double safeY = tryColumnY(world, x, z);
            if (safeY != null) {
                LOGGER.at(Level.INFO).log("Position trouvée après " + (attempt + 1) + " tentatives dans: " + foundZone.name());
                return new Vector3d(x + 0.5, safeY, z + 0.5);
            }
        }
        
        LOGGER.at(Level.FINE).log("Aucune position sûre trouvée après " + maxAttempts + " tentatives");
        return null;
    }

    private boolean zoneMatches(@Nonnull ChunkGenerator generator, int seed, int x, int z, @Nonnull Zone targetZone) {
        ZoneBiomeResult result = generator.getZoneBiomeResultAt(seed, x, z);
        return result.getZoneResult().getZone().id() == targetZone.id();
    }

    private boolean zoneMatchesAny(@Nonnull ChunkGenerator generator, int seed, int x, int z, @Nonnull int[] zoneIds) {
        int id = generator.getZoneBiomeResultAt(seed, x, z).getZoneResult().getZone().id();
        for (int accepted : zoneIds) {
            if (accepted == id) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    private static int[] toZoneIds(@Nonnull Zone[] zones) {
        int[] ids = new int[zones.length];
        for (int i = 0; i < zones.length; i++) {
            ids[i] = zones[i].id();
        }
        return ids;
    }

    private boolean chunkTouchesAllowed(int chunkX, int chunkZ, @Nonnull BiPredicate<Integer, Integer> allowed) {
        int x0 = chunkX * RTP_CHUNK_SIZE;
        int z0 = chunkZ * RTP_CHUNK_SIZE;
        int x1 = x0 + RTP_CHUNK_SIZE - 1;
        int z1 = z0 + RTP_CHUNK_SIZE - 1;
        int xm = x0 + RTP_CHUNK_SIZE / 2;
        int zm = z0 + RTP_CHUNK_SIZE / 2;
        return allowed.test(xm, zm)
            || allowed.test(x0, z0) || allowed.test(x1, z1)
            || allowed.test(x0, z1) || allowed.test(x1, z0);
    }

    @Nullable
    private Vector3d scanChunkSpiral(@Nonnull World world, @Nonnull ChunkGenerator generator, int seed,
                                     int acx, int acz, int radiusSteps, int chunkStep,
                                     @Nonnull int[] acceptedZoneIds,
                                     @Nonnull BiPredicate<Integer, Integer> xzAllowed,
                                     int[] loads) {
        outer:
        for (int r = 0; r <= radiusSteps; r++) {
            for (int dcx = -r; dcx <= r; dcx++) {
                for (int dcz = -r; dcz <= r; dcz++) {
                    if (Math.max(Math.abs(dcx), Math.abs(dcz)) != r) {
                        continue;
                    }
                    int cx = acx + dcx * chunkStep;
                    int cz = acz + dcz * chunkStep;

                    if (!chunkTouchesAllowed(cx, cz, xzAllowed)) {
                        continue;
                    }
                    int centerX = cx * RTP_CHUNK_SIZE + RTP_CHUNK_SIZE / 2;
                    int centerZ = cz * RTP_CHUNK_SIZE + RTP_CHUNK_SIZE / 2;
                    if (!zoneMatchesAny(generator, seed, centerX, centerZ, acceptedZoneIds)) {
                        continue;
                    }
                    if (loads[0] >= RTP_MAX_ASYNC_LOADS_PER_ANCHOR) {
                        break outer;
                    }
                    long ci = ChunkUtil.indexChunkFromBlock(centerX, centerZ);
                    WorldChunk chunk = resolveChunk(world, ci);
                    loads[0]++;
                    if (chunk == null) {
                        continue;
                    }
                    for (int dx = 0; dx < RTP_CHUNK_SIZE; dx++) {
                        for (int dz = 0; dz < RTP_CHUNK_SIZE; dz++) {
                            int x = cx * RTP_CHUNK_SIZE + dx;
                            int z = cz * RTP_CHUNK_SIZE + dz;
                            if (!xzAllowed.test(x, z)) {
                                continue;
                            }
                            Double y = findSafeSurfaceYFromChunk(chunk, x, z);
                            if (y != null) {
                                return new Vector3d(x + 0.5, y, z + 0.5);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private Vector3d tryRtpZoneWithAnchor(@Nonnull World world, @Nonnull ChunkGenerator generator, int seed,
                                          int bx, int bz, @Nonnull int[] acceptedZoneIds,
                                          @Nonnull BiPredicate<Integer, Integer> xzAllowed) {
        int acx = Math.floorDiv(bx, RTP_CHUNK_SIZE);
        int acz = Math.floorDiv(bz, RTP_CHUNK_SIZE);
        int[] loads = {0};

        Vector3d fine = scanChunkSpiral(world, generator, seed, acx, acz,
            RTP_FINE_CHUNK_RADIUS, 1, acceptedZoneIds, xzAllowed, loads);
        if (fine != null) {
            return fine;
        }
        if (loads[0] >= RTP_MAX_ASYNC_LOADS_PER_ANCHOR) {
            return null;
        }
        return scanChunkSpiral(world, generator, seed, acx, acz,
            RTP_COARSE_RADIUS_STEPS, RTP_COARSE_CHUNK_STEP, acceptedZoneIds, xzAllowed, loads);
    }

    @Nullable
    private Vector3d tryRtpZoneWithAnchor(@Nonnull World world, @Nonnull ChunkGenerator generator, int seed,
                                          int bx, int bz, @Nonnull Zone targetZone,
                                          @Nonnull BiPredicate<Integer, Integer> xzAllowed) {
        return tryRtpZoneWithAnchor(world, generator, seed, bx, bz, new int[]{targetZone.id()}, xzAllowed);
    }

    @Nullable
    private Vector3d tryRtpColumnAt(@Nonnull World world, @Nonnull ChunkGenerator generator, int seed,
                                    int x, int z, @Nullable Zone targetZone,
                                    @Nonnull BiPredicate<Integer, Integer> xzAllowed) {
        if (!xzAllowed.test(x, z)) {
            return null;
        }
        if (targetZone != null && !zoneMatches(generator, seed, x, z, targetZone)) {
            return null;
        }
        Map<Long, WorldChunk> chunkCache = new HashMap<>();
        Set<Long> chunkMiss = new HashSet<>();
        Double columnY = tryColumnY(world, x, z, chunkCache, chunkMiss);
        if (columnY != null) {
            return new Vector3d(x + 0.5, columnY, z + 0.5);
        }
        for (int[] off : COLUMN_NEIGHBOR_OFFSETS) {
            int nx = x + off[0];
            int nz = z + off[1];
            if (!xzAllowed.test(nx, nz)) {
                continue;
            }
            if (targetZone != null && !zoneMatches(generator, seed, nx, nz, targetZone)) {
                continue;
            }
            Double neighborY = tryColumnY(world, nx, nz, chunkCache, chunkMiss);
            if (neighborY != null) {
                return new Vector3d(nx + 0.5, neighborY, nz + 0.5);
            }
        }
        for (int[] off : COLUMN_NEIGHBOR_OFFSETS_FAR) {
            int nx = x + off[0];
            int nz = z + off[1];
            if (!xzAllowed.test(nx, nz)) {
                continue;
            }
            if (targetZone != null && !zoneMatches(generator, seed, nx, nz, targetZone)) {
                continue;
            }
            Double farY = tryColumnY(world, nx, nz, chunkCache, chunkMiss);
            if (farY != null) {
                return new Vector3d(nx + 0.5, farY, nz + 0.5);
            }
        }
        return null;
    }

    @Nullable
    private Double tryColumnY(@Nonnull World world, int x, int z) {
        return tryColumnY(world, x, z, new HashMap<>(), new HashSet<>());
    }

    @Nullable
    private Double tryColumnY(@Nonnull World world, int x, int z,
                              @Nonnull Map<Long, WorldChunk> chunkCache, @Nonnull Set<Long> chunkMiss) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        if (chunkMiss.contains(chunkIndex)) {
            return null;
        }
        WorldChunk chunk = chunkCache.get(chunkIndex);
        if (chunk == null) {
            if (chunkCache.size() + chunkMiss.size() >= RTP_MAX_COLUMN_CHUNK_LOADS) {
                return null;
            }
            chunk = resolveChunk(world, chunkIndex);
            if (chunk == null) {
                chunkMiss.add(chunkIndex);
                return null;
            }
            chunkCache.put(chunkIndex, chunk);
        }
        return findSafeSurfaceYFromChunk(chunk, x, z);
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private int[] pickZoneSearchRect(int minX, int maxX, int minZ, int maxZ) {
        return pickRandomRect(minX, maxX, minZ, maxZ);
    }

    @Nullable
    private int[] pickZoneSearchFrame(int innerHalf, int outerHalf, int span) {
        return pickRandomFrame(innerHalf, outerHalf, span);
    }

    @Nullable
    private int[] pickRandomFrame(int innerHalf, int outerHalf, int span) {
        for (int tries = 0; tries < 96; tries++) {
            int x = -outerHalf + random.nextInt(span);
            int z = -outerHalf + random.nextInt(span);
            if (Math.abs(x) <= innerHalf && Math.abs(z) <= innerHalf) {
                continue;
            }
            return new int[]{x, z};
        }
        return null;
    }

    @Nullable
    private int[] pickHashInFrame(int innerHalf, int outerHalf, int span, int attempt, int worldSeed) {
        long h = mix64((long) worldSeed * 31L + (long) attempt * 0x9E3779B1L);
        for (int perturb = 0; perturb < 48; perturb++) {
            long t = h + (long) perturb * 0xD6E8FEBFL;
            int x = -outerHalf + (int) (Math.floorMod(t, span));
            int z = -outerHalf + (int) (Math.floorMod(t >>> 20, span));
            if (Math.abs(x) <= innerHalf && Math.abs(z) <= innerHalf) {
                continue;
            }
            return new int[]{x, z};
        }
        return null;
    }

    private int[] pickRandomRect(int minX, int maxX, int minZ, int maxZ) {
        int rangeX = maxX - minX + 1;
        int rangeZ = maxZ - minZ + 1;
        return new int[]{minX + random.nextInt(rangeX), minZ + random.nextInt(rangeZ)};
    }

    private int[] pickHashInRect(int minX, int maxX, int minZ, int maxZ, int attempt, int worldSeed) {
        int rangeX = maxX - minX + 1;
        int rangeZ = maxZ - minZ + 1;
        long h = mix64((long) worldSeed * 47L + (long) attempt * 0xC2B2AE3DL);
        int x = minX + (int) (Math.floorMod(h, rangeX));
        int z = minZ + (int) (Math.floorMod(h >>> 24, rangeZ));
        return new int[]{x, z};
    }

    private int[] pickRingPoint(double distSqMin, double distSqMax, double minAngle, double maxAngle) {
        double t = random.nextDouble();
        double dist = Math.sqrt(t * (distSqMax - distSqMin) + distSqMin);
        double angle = minAngle + random.nextDouble() * (maxAngle - minAngle);
        int x = (int) (Math.cos(angle) * dist);
        int z = (int) (Math.sin(angle) * dist);
        return new int[]{x, z};
    }

    @Nullable
    public Vector3d findSafePositionInRing(@Nonnull World world, @Nonnull ChunkGenerator generator,
                                           double minDist, double maxDist,
                                           double minAngle, double maxAngle,
                                           int maxAttempts) {
        return findSafePositionInRing(world, generator, minDist, maxDist, minAngle, maxAngle, maxAttempts, null);
    }

    @Nullable
    public Vector3d findSafePositionInRect(@Nonnull World world, @Nonnull ChunkGenerator generator,
                                           int minX, int maxX, int minZ, int maxZ,
                                           int maxAttempts) {
        return findSafePositionInRect(world, generator, minX, maxX, minZ, maxZ, maxAttempts, null);
    }

    @Nullable
    public Vector3d findSafePositionInRect(@Nonnull World world, @Nonnull ChunkGenerator generator,
                                           int minX, int maxX, int minZ, int maxZ,
                                           int maxAttempts, @Nullable Zone targetZone) {
        int seed = (int) world.getWorldConfig().getSeed();

        BiPredicate<Integer, Integer> inRect = (nx, nz) -> nx >= minX && nx <= maxX && nz >= minZ && nz <= maxZ;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int x;
            int z;
            if (targetZone != null) {
                int[] base = pickZoneSearchRect(minX, maxX, minZ, maxZ);
                Vector3d pos = tryRtpZoneWithAnchor(world, generator, seed, base[0], base[1], targetZone, inRect);
                if (pos != null) {
                    ZoneBiomeResult result = generator.getZoneBiomeResultAt(seed, (int) Math.floor(pos.x), (int) Math.floor(pos.z));
                    LOGGER.at(Level.INFO).log("RTP rect: position trouvée après " + (attempt + 1) + " tentative(s) — zone: " + result.getZoneResult().getZone().name());
                    return pos;
                }
                continue;
            } else {
                int[] c = pickRandomRect(minX, maxX, minZ, maxZ);
                x = c[0];
                z = c[1];
            }

            Vector3d pos = tryRtpColumnAt(world, generator, seed, x, z, targetZone, inRect);
            if (pos != null) {
                ZoneBiomeResult result = generator.getZoneBiomeResultAt(seed, (int) Math.floor(pos.x), (int) Math.floor(pos.z));
                LOGGER.at(Level.INFO).log("RTP rect: position trouvée après " + (attempt + 1) + " tentative(s) — zone: " + result.getZoneResult().getZone().name());
                return pos;
            }
        }

        LOGGER.at(Level.FINE).log("RTP rect: aucune position sûre après " + maxAttempts + " tentatives");
        return null;
    }

    @Nullable
    public Vector3d findSafePositionOutsideInnerSquare(@Nonnull World world, @Nonnull ChunkGenerator generator,
                                                        int innerHalf, int outerHalf, int maxAttempts) {
        return findSafePositionOutsideInnerSquare(world, generator, innerHalf, outerHalf, maxAttempts, (Zone) null);
    }

    @Nullable
    public Vector3d findSafePositionOutsideInnerSquare(@Nonnull World world, @Nonnull ChunkGenerator generator,
                                                        int innerHalf, int outerHalf, int maxAttempts,
                                                        @Nullable Zone targetZone) {
        if (outerHalf <= innerHalf) {
            LOGGER.at(Level.WARNING).log("RTP cadre: outerHalf doit être > innerHalf");
            return null;
        }
        int seed = (int) world.getWorldConfig().getSeed();
        int span = 2 * outerHalf + 1;

        BiPredicate<Integer, Integer> inFrame = (nx, nz) -> {
            if (Math.abs(nx) > outerHalf || Math.abs(nz) > outerHalf) {
                return false;
            }
            return !(Math.abs(nx) <= innerHalf && Math.abs(nz) <= innerHalf);
        };

        int[] zoneIds = targetZone != null ? new int[]{targetZone.id()} : null;

        LOGGER.at(Level.INFO).log("RTP cadre: recherche zone=" + (targetZone != null ? targetZone.name() : "any")
            + " cadre=[" + (-outerHalf) + ".." + outerHalf + "] excl=[" + (-innerHalf) + ".." + innerHalf + "]");

        return runFrameSearch(world, generator, seed, innerHalf, outerHalf, span, maxAttempts, zoneIds, inFrame);
    }

    @Nullable
    public Vector3d findSafePositionOutsideInnerSquare(@Nonnull World world, @Nonnull ChunkGenerator generator,
                                                        int innerHalf, int outerHalf, int maxAttempts,
                                                        @Nonnull Zone[] targetZones) {
        if (outerHalf <= innerHalf) {
            LOGGER.at(Level.WARNING).log("RTP cadre: outerHalf doit être > innerHalf");
            return null;
        }
        int seed = (int) world.getWorldConfig().getSeed();
        int span = 2 * outerHalf + 1;

        BiPredicate<Integer, Integer> inFrame = (nx, nz) -> {
            if (Math.abs(nx) > outerHalf || Math.abs(nz) > outerHalf) {
                return false;
            }
            return !(Math.abs(nx) <= innerHalf && Math.abs(nz) <= innerHalf);
        };

        int[] zoneIds = toZoneIds(targetZones);
        LOGGER.at(Level.INFO).log("RTP cadre: recherche zone=any_zone1 (" + targetZones.length + " sous-zones)"
            + " cadre=[" + (-outerHalf) + ".." + outerHalf + "] excl=[" + (-innerHalf) + ".." + innerHalf + "]");

        return runFrameSearch(world, generator, seed, innerHalf, outerHalf, span, maxAttempts, zoneIds, inFrame);
    }

    @Nullable
    private Vector3d runFrameSearch(@Nonnull World world, @Nonnull ChunkGenerator generator, int seed,
                                    int innerHalf, int outerHalf, int span, int maxAttempts,
                                    @Nullable int[] zoneIds,
                                    @Nonnull BiPredicate<Integer, Integer> inFrame) {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int x;
            int z;
            if (zoneIds != null) {
                int[] base = pickZoneSearchFrame(innerHalf, outerHalf, span);
                if (base == null) {
                    continue;
                }
                Vector3d posZone = tryRtpZoneWithAnchor(world, generator, seed, base[0], base[1], zoneIds, inFrame);
                if (posZone != null) {
                    ZoneBiomeResult result = generator.getZoneBiomeResultAt(seed, (int) Math.floor(posZone.x), (int) Math.floor(posZone.z));
                    LOGGER.at(Level.INFO).log("RTP cadre: position trouvée après " + (attempt + 1) + " tentative(s) — zone: " + result.getZoneResult().getZone().name());
                    return posZone;
                }
                continue;
            } else {
                int[] c = pickRandomFrame(innerHalf, outerHalf, span);
                if (c == null) {
                    continue;
                }
                x = c[0];
                z = c[1];
            }

            Vector3d pos = tryRtpColumnAt(world, generator, seed, x, z, null, inFrame);
            if (pos != null) {
                ZoneBiomeResult result = generator.getZoneBiomeResultAt(seed, (int) Math.floor(pos.x), (int) Math.floor(pos.z));
                LOGGER.at(Level.INFO).log("RTP cadre: position trouvée après " + (attempt + 1) + " tentative(s) — zone: " + result.getZoneResult().getZone().name());
                return pos;
            }
        }

        LOGGER.at(Level.INFO).log("RTP cadre: aucune position sûre après " + maxAttempts + " tentatives");
        return null;
    }

    @Nullable
    public Vector3d findSafePositionInRing(@Nonnull World world, @Nonnull ChunkGenerator generator,
                                           double minDist, double maxDist,
                                           double minAngle, double maxAngle,
                                           int maxAttempts, @Nullable Zone targetZone) {
        int seed = (int) world.getWorldConfig().getSeed();
        double distSqMin = minDist * minDist;
        double distSqMax = maxDist * maxDist;
        boolean fullCircle = (maxAngle - minAngle) >= 2 * Math.PI - 1e-6;

        BiPredicate<Integer, Integer> inRing = (nx, nz) -> {
            double d = Math.sqrt(nx * nx + nz * nz);
            if (d < minDist || d > maxDist) {
                return false;
            }
            if (fullCircle) {
                return true;
            }
            double ang = Math.atan2(nz, nx);
            if (ang < 0) {
                ang += 2 * Math.PI;
            }
            return ang >= minAngle && ang <= maxAngle;
        };

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int[] c = pickRingPoint(distSqMin, distSqMax, minAngle, maxAngle);
            int x;
            int z;
            if (targetZone != null) {
                Vector3d posZone = tryRtpZoneWithAnchor(world, generator, seed, c[0], c[1], targetZone, inRing);
                if (posZone != null) {
                    ZoneBiomeResult result = generator.getZoneBiomeResultAt(seed, (int) Math.floor(posZone.x), (int) Math.floor(posZone.z));
                    LOGGER.at(Level.INFO).log("RTP: position trouvée après " + (attempt + 1) + " tentative(s) — zone: " + result.getZoneResult().getZone().name());
                    return posZone;
                }
                continue;
            } else {
                if (!inRing.test(c[0], c[1])) {
                    continue;
                }
                x = c[0];
                z = c[1];
            }
            Vector3d pos = tryRtpColumnAt(world, generator, seed, x, z, targetZone, inRing);
            if (pos != null) {
                ZoneBiomeResult result = generator.getZoneBiomeResultAt(seed, (int) Math.floor(pos.x), (int) Math.floor(pos.z));
                LOGGER.at(Level.INFO).log("RTP: position trouvée après " + (attempt + 1) + " tentative(s) — zone: " + result.getZoneResult().getZone().name());
                return pos;
            }
        }

        LOGGER.at(Level.FINE).log("RTP: aucune position sûre après " + maxAttempts + " tentatives");
        return null;
    }

    @Nullable
    private WorldChunk resolveChunk(@Nonnull World world, long chunkIndex) {
        WorldChunk c = world.getChunk(chunkIndex);
        if (c != null) {
            return c;
        }
        try {
            return world.getChunkAsync(chunkIndex).toCompletableFuture().get(2500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException | TimeoutException e) {
            LOGGER.at(Level.FINE).log("RTP: chunk " + chunkIndex + " non chargé à temps");
            return null;
        }
    }

    @Nullable
    private Double findSafeSurfaceYFromChunk(@Nonnull WorldChunk chunk, int blockX, int blockZ) {
        for (int checkY = START_Y; checkY >= MIN_Y; checkY--) {
            try {
                if (hasFluid(chunk, blockX, checkY, blockZ)) {
                    continue;
                }
                if (!isSolidBlock(chunk, blockX, checkY, blockZ)) {
                    continue;
                }
                int spawnY = checkY + 1;
                if (!isAcceptableOutdoorDrySpawn(chunk, blockX, spawnY, blockZ)) {
                    continue;
                }
                if (passesTierStrictFeet(chunk, blockX, spawnY, blockZ)) {
                    return (double) spawnY;
                }
                if (passesTierRelaxedFeet(chunk, blockX, spawnY, blockZ)) {
                    return (double) spawnY;
                }
                if (passesTierMinimalFeet(chunk, blockX, spawnY, blockZ)) {
                    return (double) spawnY;
                }
            } catch (Exception e) {
                continue;
            }
        }
        return null;
    }

    private boolean passesTierStrictFeet(@Nonnull WorldChunk chunk, int blockX, int spawnY, int blockZ) {
        if (hasFluid(chunk, blockX, spawnY, blockZ) || hasFluid(chunk, blockX, spawnY + 1, blockZ)) {
            return false;
        }
        return !isSolidBlock(chunk, blockX, spawnY + 1, blockZ);
    }

    private boolean passesTierRelaxedFeet(@Nonnull WorldChunk chunk, int blockX, int spawnY, int blockZ) {
        return !hasFluid(chunk, blockX, spawnY, blockZ) && !hasFluid(chunk, blockX, spawnY + 1, blockZ);
    }

    private boolean passesTierMinimalFeet(@Nonnull WorldChunk chunk, int blockX, int spawnY, int blockZ) {
        return !hasFluid(chunk, blockX, spawnY, blockZ);
    }

    private boolean isAcceptableOutdoorDrySpawn(@Nonnull WorldChunk chunk, int blockX, int spawnY, int blockZ) {
        for (int dy = 0; dy <= 2; dy++) {
            if (hasFluid(chunk, blockX, spawnY + dy, blockZ)) {
                return false;
            }
        }
        for (int y = spawnY + 2; y <= START_Y; y++) {
            if (hasFluid(chunk, blockX, y, blockZ)) {
                return false;
            }
            if (blocksSkyOcclusion(chunk, blockX, y, blockZ)) {
                return false;
            }
        }
        return true;
    }

    private boolean blocksSkyOcclusion(@Nonnull WorldChunk chunk, int x, int y, int z) {
        try {
            BlockType blockType = chunk.getBlockType(x, y, z);
            if (blockType == null) {
                return false;
            }
            if (blockType.getMaterial() != BlockMaterial.Solid) {
                return false;
            }
            String id = blockType.getId();
            if (id != null) {
                String s = id.toLowerCase();
                if (s.contains("leaf") || s.contains("leaves") || s.contains("vine") || s.contains("sapling")
                    || s.contains("flower")) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSolidBlock(@Nonnull WorldChunk chunk, int x, int y, int z) {
        try {
            BlockType blockType = chunk.getBlockType(x, y, z);
            if (blockType == null) {
                return false;
            }
            return blockType.getMaterial() == BlockMaterial.Solid;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasFluid(@Nonnull WorldChunk chunk, int x, int y, int z) {
        try {
            return chunk.getFluidId(x, y, z) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
