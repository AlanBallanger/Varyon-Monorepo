package fr.varyon.vrpg.ui;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.vrpg.VaryonRpgPlugin;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AbilitySlotsHud extends CustomUIHud {

    public static final String HUD_KEY = "vrpg_ability_slots_hud";

    private static final String ASSETS = "AbilitySlots/Assets/";
    private static final PatchStyle BG_NORMAL = new PatchStyle().setTexturePath(Value.of(ASSETS + "BackgroundAbility@2x.png"));
    private static final PatchStyle BG_ON_USE = new PatchStyle().setTexturePath(Value.of(ASSETS + "BackgroundAbilityOnUse@2x.png"));

    private static final ConcurrentHashMap<UUID, AbilitySlotsHud> INSTANCES = new ConcurrentHashMap<>();

    public AbilitySlotsHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, HUD_KEY);
    }

    @Nonnull
    public static AbilitySlotsHud getOrCreate(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        AbilitySlotsHud existing = INSTANCES.get(uuid);
        if (existing != null) return existing;
        AbilitySlotsHud hud = new AbilitySlotsHud(playerRef);
        AbilitySlotsHud race = INSTANCES.putIfAbsent(uuid, hud);
        if (race != null) return race;
        player.getHudManager().addCustomHud(playerRef, hud);
        return hud;
    }

    @Nullable
    public static AbilitySlotsHud get(@Nonnull UUID uuid) {
        return INSTANCES.get(uuid);
    }

    public static void cleanup(@Nonnull UUID uuid) {
        INSTANCES.remove(uuid);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        builder.append("Hud/AbilitySlots/VRpgAbilitySlots.ui");
    }

    public void refreshSlots() {
        String e = resolveSlotItemId("E");
        String r = resolveSlotItemId("R");
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass()
            .atInfo().log("[AbilitySlotsHud] refreshSlots e=" + e + " r=" + r);
        UICommandBuilder cmd = new UICommandBuilder();
        applySlots(cmd, e, r);
        this.update(false, cmd);
    }

    public void setSlotOnUse(@Nonnull String slotId, boolean onUse) {
        UICommandBuilder cmd = new UICommandBuilder();
        String bgId = "E".equals(slotId) ? "#VRpgSlotEBg" : "#VRpgSlotRBg";
        cmd.setObject(bgId + ".Background", onUse ? BG_ON_USE : BG_NORMAL);
        this.update(false, cmd);
    }

    public void setSlotAvailable(@Nonnull String slotId, boolean available) {
        UICommandBuilder cmd = new UICommandBuilder();
        String overlayId = "E".equals(slotId) ? "#VRpgSlotEErrorOverlay" : "#VRpgSlotRErrorOverlay";
        cmd.set(overlayId + ".Visible", !available);
        this.update(false, cmd);
    }

    private void applySlots(@Nonnull UICommandBuilder cmd, @Nullable String eItemId, @Nullable String rItemId) {
        applyIcon(cmd, "#VRpgSlotEIcon", eItemId);
        applyIcon(cmd, "#VRpgSlotRIcon", rItemId);
    }

    private void applyIcon(@Nonnull UICommandBuilder cmd, @Nonnull String elementId, @Nullable String iconPath) {
        if (iconPath != null && !iconPath.isBlank()) {
            cmd.setObject(elementId + ".Background", new PatchStyle(Value.of(iconPath), Value.of(0)));
        }
    }

    @Nullable
    private String resolveSlotItemId(@Nonnull String slotId) {
        VaryonRpgPlugin plugin = VaryonRpgPlugin.getInstance();
        if (plugin == null) return null;
        ClassManager classManager = plugin.getClassManager();
        if (classManager == null) return null;
        PlayerRef ref = getPlayerRef();
        UUID uuid = ref.getUuid();
        if (uuid == null) return null;
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (acc == null) return null;
        PlayerClass activeClass = acc.getActiveClass();
        if (activeClass == null) return null;
        String result = acc.getSkillSlot(activeClass, slotId);
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass()
            .atInfo().log("[AbilitySlotsHud] slot=" + slotId + " iconPath=" + result);
        return result;
    }
}
