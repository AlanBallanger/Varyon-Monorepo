package com.varyon.varyonui.integration;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.UUID;

public final class CombatProfilBridge {

    private static final String RPG_MAIN = "fr.varyon.rpg.VaryonRPG";
    private static final String WEAPON_UI_ATK = "fr.varyon.rpg.classes.ui.WeaponUiAttackDisplay";

    private static final String ICON_HP = "Icons/health.png";
    private static final String ICON_ATK = "Icons/attack.png";
    private static final String ICON_ARM = "Icons/defense.png";
    private static final String ICON_STA = "Icons/stamina.png";
    private static final String ICON_CRIT_CHANCE = "Icons/taux_crit.png";
    private static final String ICON_CRIT_DAMAGE = "Icons/degat_crit.png";

    private static final PatchStyle CLEAR_ICON_BG =
            new PatchStyle().setColor(Value.of("#00000000"));

    private CombatProfilBridge() {}

    public static void applyCombatProfil(@Nonnull PlayerRef playerRef, Player player, @Nonnull UICommandBuilder ui) {
        Object rpg = getRpgInstance();
        if (rpg == null) {
            clearSidebarStats(ui);
            return;
        }
        Object module = invokeNoArg(rpg, "getClassModule");
        if (module == null) {
            clearSidebarStats(ui);
            return;
        }
        UUID playerId = playerRef.getUuid();
        Object dataManager = invokeNoArg(module, "getDataManager");
        Object data = invoke(dataManager, "getOrCreate", new Class[]{UUID.class}, new Object[]{playerId});
        if (data == null) {
            clearSidebarStats(ui);
            return;
        }
        int maxHp = invokeInt(data, "getMaxHp");
        int liveCur = readLiveHealthCurrent(playerRef);
        int liveMax = readLiveHealthMax(playerRef);
        int currentHp = liveCur >= 0 ? liveCur : invokeInt(data, "getCurrentHp");
        int displayMaxHp = liveMax >= 0 ? liveMax : maxHp;
        int weaponDmg = readWeaponDisplayDamage(player);
        double atkMult = invokeDouble(data, "getAtkDamageMultiplier");
        int atkDisplay = computeDisplayedAttackDamageReflect(weaponDmg, atkMult);
        int armor = invokeInt(data, "getBaseArmor");
        int maxStaminaData = invokeInt(data, "getMaxStamina");
        int liveStaCur = readLiveStaminaCurrent(playerRef);
        int liveStaMax = readLiveStaminaMax(playerRef);
        int displayMaxSta = liveStaMax >= 0 ? liveStaMax : maxStaminaData;
        int currentSta = liveStaCur >= 0 ? liveStaCur : displayMaxSta;
        int critPct = invokeInt(data, "getCritChancePercent");
        int critBonus = invokeInt(data, "getCritDamageBonusPercent");

        int critDamageTotalPct = 100 + critBonus;

        ui.set("#SidebarStatHPValueMain.TextSpans", Message.raw(currentHp + " / " + displayMaxHp));
        ui.set("#SidebarStatATKValueMain.TextSpans", Message.raw(String.valueOf(atkDisplay)));
        ui.set("#SidebarStatArmorValueMain.TextSpans", Message.raw(armor + "%"));
        ui.set("#SidebarStatStaminaValueMain.TextSpans", Message.raw(currentSta + " / " + displayMaxSta));
        ui.set("#SidebarStatCritChanceValueMain.TextSpans", Message.raw(critPct + "%"));
        ui.set("#SidebarStatCritDamageValueMain.TextSpans", Message.raw(critDamageTotalPct + "%"));

        setIconTexture(ui, "#SidebarStatHPIcon", ICON_HP);
        setIconTexture(ui, "#SidebarStatATKIcon", ICON_ATK);
        setIconTexture(ui, "#SidebarStatArmorIcon", ICON_ARM);
        setIconTexture(ui, "#SidebarStatStaminaIcon", ICON_STA);
        setIconTexture(ui, "#SidebarStatCritChanceIcon", ICON_CRIT_CHANCE);
        setIconTexture(ui, "#SidebarStatCritDamageIcon", ICON_CRIT_DAMAGE);
    }

    private static void clearSidebarStats(@Nonnull UICommandBuilder ui) {
        ui.set("#SidebarStatHPValueMain.TextSpans", Message.raw(""));
        ui.set("#SidebarStatATKValueMain.TextSpans", Message.raw(""));
        ui.set("#SidebarStatArmorValueMain.TextSpans", Message.raw(""));
        ui.set("#SidebarStatStaminaValueMain.TextSpans", Message.raw(""));
        ui.set("#SidebarStatCritChanceValueMain.TextSpans", Message.raw(""));
        ui.set("#SidebarStatCritDamageValueMain.TextSpans", Message.raw(""));
        ui.setObject("#SidebarStatHPIcon.Background", CLEAR_ICON_BG);
        ui.setObject("#SidebarStatATKIcon.Background", CLEAR_ICON_BG);
        ui.setObject("#SidebarStatArmorIcon.Background", CLEAR_ICON_BG);
        ui.setObject("#SidebarStatStaminaIcon.Background", CLEAR_ICON_BG);
        ui.setObject("#SidebarStatCritChanceIcon.Background", CLEAR_ICON_BG);
        ui.setObject("#SidebarStatCritDamageIcon.Background", CLEAR_ICON_BG);
    }

    private static void setIconTexture(UICommandBuilder ui, String elementId, String texturePath) {
        ui.setObject(elementId + ".Background", new PatchStyle().setTexturePath(Value.of(texturePath)));
    }

    private static Object getRpgInstance() {
        try {
            Class<?> c = Class.forName(RPG_MAIN);
            Method m = c.getMethod("getRpgInstance");
            return m.invoke(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object invokeNoArg(Object target, String name) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(name);
            return m.invoke(target);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object[] args) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(name, types);
            return m.invoke(target, args);
        } catch (Throwable t) {
            return null;
        }
    }

    private static int invokeInt(Object target, String name) {
        if (target == null) return 0;
        try {
            Method m = target.getClass().getMethod(name);
            Object r = m.invoke(target);
            if (r instanceof Number n) return n.intValue();
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static int readWeaponDisplayDamage(Player player) {
        if (player == null) return -1;
        try {
            Class<?> c = Class.forName(WEAPON_UI_ATK);
            Method m = c.getDeclaredMethod("readHeldWeaponDisplayDamage", Player.class);
            m.setAccessible(true);
            Object r = m.invoke(null, player);
            if (r instanceof Number n) return n.intValue();
        } catch (Throwable ignored) {
        }
        return -1;
    }

    private static int computeDisplayedAttackDamageReflect(int weaponRaw, double atkMult) {
        try {
            Class<?> c = Class.forName(WEAPON_UI_ATK);
            Method m = c.getMethod("computeDisplayedAttackDamage", int.class, double.class);
            Object r = m.invoke(null, weaponRaw, atkMult);
            if (r instanceof Number n) return n.intValue();
        } catch (Throwable ignored) {
        }
        int base = weaponRaw < 0 ? 1 : weaponRaw;
        double m = atkMult > 0.0 ? atkMult : 1.0;
        return (int) Math.max(1L, Math.round((double) base * m));
    }

    private static double invokeDouble(Object target, String name) {
        if (target == null) return 1.0;
        try {
            Method m = target.getClass().getMethod(name);
            Object r = m.invoke(target);
            if (r instanceof Number n) return n.doubleValue();
        } catch (Throwable ignored) {
        }
        return 1.0;
    }

    private static int readLiveHealthCurrent(PlayerRef playerRef) {
        if (playerRef == null || !playerRef.isValid()) {
            return -1;
        }
        try {
            EntityStatMap statMap = playerRef.getComponent(EntityStatMap.getComponentType());
            if (statMap == null) {
                return -1;
            }
            int hIdx = DefaultEntityStatTypes.getHealth();
            var healthStat = statMap.get(hIdx);
            if (healthStat == null) {
                return -1;
            }
            return Math.round(healthStat.get());
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static int readLiveHealthMax(PlayerRef playerRef) {
        if (playerRef == null || !playerRef.isValid()) {
            return -1;
        }
        try {
            EntityStatMap statMap = playerRef.getComponent(EntityStatMap.getComponentType());
            if (statMap == null) {
                return -1;
            }
            int hIdx = DefaultEntityStatTypes.getHealth();
            var healthStat = statMap.get(hIdx);
            if (healthStat == null) {
                return -1;
            }
            return Math.round(healthStat.getMax());
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static int readLiveStaminaCurrent(PlayerRef playerRef) {
        if (playerRef == null || !playerRef.isValid()) {
            return -1;
        }
        try {
            EntityStatMap statMap = playerRef.getComponent(EntityStatMap.getComponentType());
            if (statMap == null) {
                return -1;
            }
            int sIdx = DefaultEntityStatTypes.getStamina();
            var staminaStat = statMap.get(sIdx);
            if (staminaStat == null) {
                return -1;
            }
            return Math.round(staminaStat.get());
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static int readLiveStaminaMax(PlayerRef playerRef) {
        if (playerRef == null || !playerRef.isValid()) {
            return -1;
        }
        try {
            EntityStatMap statMap = playerRef.getComponent(EntityStatMap.getComponentType());
            if (statMap == null) {
                return -1;
            }
            int sIdx = DefaultEntityStatTypes.getStamina();
            var staminaStat = statMap.get(sIdx);
            if (staminaStat == null) {
                return -1;
            }
            return Math.round(staminaStat.getMax());
        } catch (Throwable ignored) {
            return -1;
        }
    }
}
