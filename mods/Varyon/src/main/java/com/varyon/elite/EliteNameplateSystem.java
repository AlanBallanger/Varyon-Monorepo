package com.varyon.elite;

import com.frotty27.nameplatebuilder.api.NameplateData;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.varyon.component.MobScalingComponent;
import com.varyon.elite.component.EliteComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.logging.Level;

/**
 * Marks elites on the nameplate as {@code "[• • •] Assassin Skeleton - Lvl 100"} - a bullet meter
 * counting the elite tier, the mob class, the mob name, then its level. BOSS-class elites read
 * {@code "[Boss] ..."}. The marker is written to NameplateBuilder's {@code monster_level} segment
 * (the same channel that renders "Nv.X" for normal mobs, with NameplateBuilder's own font that
 * carries the bullet glyph). The {@code ComponentType} is resolved lazily on the first tick so
 * plugin load order cannot break the lookup. If NameplateBuilder never resolves the elite simply
 * keeps its default nameplate - no ASCII fallback text is written.
 */
public class EliteNameplateSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final char PIP = '•';

    @Nullable
    private ComponentType<EntityStore, NameplateData> nameplateDataType;
    private boolean resolved;

    public EliteNameplateSystem(@Nullable ComponentType<EntityStore, NameplateData> nameplateDataType) {
        this.nameplateDataType = nameplateDataType;
        this.resolved = nameplateDataType != null;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(EliteComponent.getComponentType());
    }

    @Nullable
    private ComponentType<EntityStore, NameplateData> nameplateType() {
        if (!resolved) {
            resolved = true;
            try {
                nameplateDataType = com.frotty27.nameplatebuilder.api.NameplateAPI.getComponentType();
            } catch (Throwable ignored) {
            }
        }
        return nameplateDataType;
    }

    @Override
    public void tick(float dt, int index,
                     @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        try {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            EliteComponent elite = store.getComponent(ref, EliteComponent.getComponentType());
            if (elite == null) {
                return;
            }

            int level = -1;
            MobScalingComponent scaling = store.getComponent(ref, MobScalingComponent.getComponentType());
            if (scaling != null) {
                level = scaling.getMobLevel();
            }

            boolean boss = elite.getMobClass() == MobClass.BOSS;
            String meter = boss ? "[Boss]" : bulletMeter(elite.getTier());
            String mobName = displayName(store.getComponent(ref, NPCEntity.getComponentType()));
            String text = meter + eliteBody(elite, mobName, level);

            // The elite marker goes on NameplateBuilder's monster_level segment - the same channel
            // that shows "Nv.X" for normal mobs, rendered with NameplateBuilder's own font so the
            // bullet glyphs display.
            ComponentType<EntityStore, NameplateData> type = nameplateType();
            boolean pushedToBuilder = false;
            if (type != null) {
                NameplateData data = store.getComponent(ref, type);
                if (data == null) {
                    data = new NameplateData();
                    data.setText("monster_level", text);
                    data.setText("monster_level.1", text);
                    commandBuffer.putComponent(ref, type, data);
                } else if (!text.equals(data.getText("monster_level"))) {
                    data.setText("monster_level", text);
                    data.setText("monster_level.1", text);
                }
                pushedToBuilder = true;
            }

            // Vanilla plate: this system owns an elite's nameplate. If NameplateBuilder carried the
            // marker, blank the vanilla plate so it does not show a competing "Lvl N"; otherwise
            // write the marker to the vanilla plate directly (bullet glyphs may not render there).
            Nameplate vanilla = store.getComponent(ref, Nameplate.getComponentType());
            String want = pushedToBuilder ? "" : text;
            if (vanilla == null) {
                if (!want.isEmpty()) {
                    commandBuffer.putComponent(ref, Nameplate.getComponentType(), new Nameplate(want));
                }
            } else if (!want.equals(vanilla.getText())) {
                vanilla.setText(want);
            }
        } catch (Exception e) {
            LOGGER.at(Level.FINE).log("EliteNameplateSystem tick error: " + e.getMessage());
        }
    }

    /** {@code " Assassin Skeleton - Lvl 100"} - the part after the tier meter. */
    private static String eliteBody(@Nonnull EliteComponent elite, @Nullable String mobName, int level) {
        StringBuilder sb = new StringBuilder();
        if (elite.isNamed()) {
            sb.append(' ').append(elite.getNamedId());
        } else {
            sb.append(' ').append(elite.getMobClass().displayName());
            if (mobName != null && !mobName.isBlank()) {
                sb.append(' ').append(mobName);
            }
        }
        if (level >= 1) {
            sb.append(" - Lvl ").append(level);
        }
        return sb.toString();
    }

    /** {@code "[• • •]"} for tier 3 - one bullet per tier (RPGMobs style). */
    private static String bulletMeter(int tier) {
        int pips = Math.max(1, Math.min(5, tier));
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < pips; i++) {
            if (i > 0) sb.append(' ');
            sb.append(PIP);
        }
        return sb.append(']').toString();
    }

    /** "skeleton_frost_knight" -> "Skeleton". First role segment, capitalised. */
    @Nullable
    private static String displayName(@Nullable NPCEntity npc) {
        if (npc == null) {
            return null;
        }
        String raw = null;
        try {
            raw = npc.getRoleName();
        } catch (Exception ignored) {
        }
        if (raw == null || raw.isBlank()) {
            try {
                raw = npc.getNPCTypeId();
            } catch (Exception ignored) {
            }
        }
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.toLowerCase(Locale.ROOT);
        int slash = s.lastIndexOf('/');
        if (slash >= 0) s = s.substring(slash + 1);
        int colon = s.lastIndexOf(':');
        if (colon >= 0) s = s.substring(colon + 1);
        int underscore = s.indexOf('_');
        if (underscore > 0) s = s.substring(0, underscore);
        if (s.isEmpty()) {
            return null;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
