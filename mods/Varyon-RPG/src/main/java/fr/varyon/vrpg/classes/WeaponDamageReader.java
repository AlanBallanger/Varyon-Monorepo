package fr.varyon.vrpg.classes;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemWeapon;
import com.hypixel.hytale.server.core.asset.type.projectile.config.Projectile;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.DamageEntityInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.combat.DamageCalculator;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WeaponDamageReader {

    private static final ConcurrentHashMap<String, Integer> CACHE = new ConcurrentHashMap<>();

    private static final InteractionType[] INTERACTION_TYPES = {
        InteractionType.Primary,
        InteractionType.Secondary,
        InteractionType.ProjectileSpawn,
        InteractionType.ProjectileHit,
        InteractionType.Use,
        InteractionType.Ability1,
    };

    private static final String[] VAR_DAMAGE_KEYS = {
        "Swing_Left_Damage", "Primary_Swing_Left_Damage", "Melee_Swing_Left_Damage",
        "Shoot_Left_Damage", "Standard_Projectile_Damage",
        "Swing_Right_Damage", "Primary_Swing_Right_Damage", "Melee_Swing_Right_Damage",
        "Shoot_Right_Damage", "Primary_Shot_Damage",
        "Thrust_Left_Damage", "Thrust_Right_Damage", "Thrust_Damage",
        "Swing_Down_Damage",
    };

    private WeaponDamageReader() {}

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    public static int readHeldWeaponDamage(@Nullable PlayerRef playerRef) {
        if (playerRef == null) return -1;
        try {
            var hotbar = playerRef.getComponent(
                com.hypixel.hytale.server.core.inventory.InventoryComponent.Hotbar.getComponentType());
            if (hotbar == null) return -1;
            byte slot = hotbar.getActiveSlot();
            ItemStack held = hotbar.getInventory().getItemStack((short) slot);
            if (held == null || held.isEmpty()) return -1;
            String itemId = held.getItemId();
            if (itemId == null || itemId.isEmpty()) return -1;
            int dmg = CACHE.computeIfAbsent(itemId, id -> {
                int v = readDamageFromItem(held.getItem());
                if (v < 0) v = readFromAsset(id);
                LOG.atInfo().log("[WeaponDamageReader] itemId=" + id + " dmg=" + v
                    + " itemNull=" + (held.getItem() == null));
                return v;
            });
            return dmg;
        } catch (Exception e) {
            LOG.atWarning().log("[WeaponDamageReader] exception: " + e.getMessage());
            return -1;
        }
    }

    private static int readDamageFromItem(@Nullable Item item) {
        if (item == null) return -1;
        int v = fromInteractionVars(item);
        if (v >= 0) return v;
        v = fromPrimaryInteractions(item);
        if (v >= 0) return v;
        ItemWeapon w = item.getWeapon();
        if (w != null) {
            v = fromStatModifiers(w);
            if (v > 0) return v;
        }
        return -1;
    }

    private static int fromInteractionVars(Item item) {
        Map<String, String> vars = item.getInteractionVars();
        if (vars == null || vars.isEmpty()) return -1;
        for (String key : VAR_DAMAGE_KEYS) {
            String raw = vars.get(key);
            if (raw == null) continue;
            int v = fromVarValue(raw);
            if (v >= 0) return v;
        }
        for (Map.Entry<String, String> e : vars.entrySet()) {
            String k = e.getKey();
            if (k == null || !k.contains("Damage")) continue;
            int v = fromVarValue(e.getValue());
            if (v >= 0) return v;
        }
        return -1;
    }

    private static int fromVarValue(String raw) {
        if (raw == null) return -1;
        return fromInteractionChain(raw.trim(), 0);
    }

    private static int fromPrimaryInteractions(Item item) {
        Map<InteractionType, String> map = item.getInteractions();
        if (map == null || map.isEmpty()) return -1;
        for (InteractionType t : INTERACTION_TYPES) {
            String iid = map.get(t);
            if (iid == null || iid.isEmpty()) continue;
            int v = fromInteractionChain(iid, 0);
            if (v >= 0) return v;
        }
        return -1;
    }

    private static int fromInteractionChain(String id, int depth) {
        if (depth > 12 || id == null || id.isEmpty() || id.indexOf('{') >= 0) return -1;
        try {
            Interaction inter = Interaction.getAssetMap().getAsset(id.trim());
            if (inter == null || inter.isUnknown()) return -1;
            int direct = extractDamage(inter);
            if (direct >= 0) return direct;
            String next = reflectField(inter, "next");
            if (next != null && !next.isBlank()) return fromInteractionChain(next.trim(), depth + 1);
        } catch (Exception ignored) {}
        return -1;
    }

    private static int extractDamage(Interaction inter) {
        int fromProj = damageFromProjectile(inter);
        if (fromProj >= 0) return fromProj;
        if (inter instanceof DamageEntityInteraction dei) {
            try {
                Field f = DamageEntityInteraction.class.getDeclaredField("damageCalculator");
                f.setAccessible(true);
                DamageCalculator dc = (DamageCalculator) f.get(dei);
                float v = physicalFromDc(dc);
                return v >= 0 ? Math.round(v) : -1;
            } catch (ReflectiveOperationException ignored) {}
        }
        return -1;
    }

    private static float physicalFromDc(@Nullable DamageCalculator dc) {
        if (dc == null) return -1f;
        try {
            @SuppressWarnings("unchecked")
            Object2FloatMap<DamageCause> res = (Object2FloatMap<DamageCause>) dc.getClass()
                .getMethod("calculateDamage", double.class).invoke(dc, 1.0);
            if (res != null && res.containsKey(DamageCause.PHYSICAL)) {
                float p = res.getFloat(DamageCause.PHYSICAL);
                if (p > 0f) return p;
            }
        } catch (Exception ignored) {}
        try {
            Field f = DamageCalculator.class.getDeclaredField("baseDamageRaw");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            Object2FloatMap<String> map = (Object2FloatMap<String>) f.get(dc);
            if (map != null && map.containsKey("Physical")) return map.getFloat("Physical");
            if (map != null) { float s = 0f; for (var e : map.object2FloatEntrySet()) s += e.getFloatValue(); return s > 0f ? s : -1f; }
        } catch (ReflectiveOperationException ignored) {}
        return -1f;
    }

    private static int damageFromProjectile(Interaction inter) {
        try {
            Method m = inter.getClass().getMethod("getProjectileId");
            Object pid = m.invoke(inter);
            if (!(pid instanceof String s) || s.isBlank()) return -1;
            Projectile proj = Projectile.getAssetMap().getAsset(s.trim());
            if (proj == null) return -1;
            int d = proj.getDamage();
            return d > 0 ? d : -1;
        } catch (Exception ignored) { return -1; }
    }

    @SuppressWarnings("unchecked")
    private static String reflectField(Object obj, String name) {
        for (Class<?> c = obj.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                Object v = f.get(obj);
                if (v instanceof String s && !s.isBlank()) return s;
                return null;
            } catch (NoSuchFieldException ignored) {
            } catch (ReflectiveOperationException ignored) { return null; }
        }
        return null;
    }

    private static int fromStatModifiers(ItemWeapon weapon) {
        Int2ObjectMap<StaticModifier[]> map = weapon.getStatModifiers();
        if (map == null || map.isEmpty()) return 0;
        int sum = 0;
        for (var entry : map.int2ObjectEntrySet()) {
            EntityStatType est = EntityStatType.getAssetMap().getAsset(entry.getIntKey());
            String id = est != null ? est.getId() : null;
            if (!looksLikeDamageStat(id)) continue;
            for (StaticModifier m : entry.getValue()) {
                if (m != null && m.getCalculationType() == StaticModifier.CalculationType.ADDITIVE)
                    sum += Math.round(m.getAmount());
            }
        }
        return sum;
    }

    private static boolean looksLikeDamageStat(String id) {
        if (id == null) return false;
        if (id.equals("AttackDamage") || id.equals("PhysicalDamage")) return true;
        return id.endsWith("Damage") && !id.contains("Spell") && !id.contains("Magic");
    }

    private static int readFromAsset(String itemId) {
        try {
            Item item = Item.getAssetMap().getAsset(itemId);
            return readDamageFromItem(item);
        } catch (Exception e) { return -1; }
    }
}
