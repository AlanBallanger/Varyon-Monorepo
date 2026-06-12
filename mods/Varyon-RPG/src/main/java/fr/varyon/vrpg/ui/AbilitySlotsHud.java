package fr.varyon.vrpg.ui;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.vrpg.VaryonRpgPlugin;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.classes.WeaponCategory;
import fr.varyon.vrpg.classes.ability.ClassSkillService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class AbilitySlotsHud extends CustomUIHud {

    public static final String HUD_KEY = "vrpg_ability_slots_hud";

    private static final int SLOT_SIZE = 46;

    private static final List<String> SLOT_IDS = List.of("E", "R", "A", "CrouchE", "CrouchR", "CrouchA");

    private static final ConcurrentHashMap<UUID, AbilitySlotsHud> INSTANCES = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService SCHEDULER =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ability-slots-hud");
            t.setDaemon(true);
            return t;
        });

    private final ConcurrentHashMap<String, ScheduledFuture<?>> cooldownTasks = new ConcurrentHashMap<>();
    private ScheduledFuture<?> weaponWatchTask;
    private WeaponCategory lastSeenCategory = null;

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
        AbilitySlotsHud hud = INSTANCES.remove(uuid);
        if (hud != null) hud.cancelAllCooldownTasks();
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        builder.append("Hud/AbilitySlots/VRpgAbilitySlots.ui");
        startWeaponWatch();
    }

    private void startWeaponWatch() {
        if (weaponWatchTask != null) weaponWatchTask.cancel(false);
        weaponWatchTask = SCHEDULER.scheduleAtFixedRate(() -> {
            try {
                PlayerRef ref = getPlayerRef();
                if (ref == null) return;
                UUID uid = ref.getUuid();
                if (uid == null) return;
                com.hypixel.hytale.server.core.universe.Universe universe =
                    com.hypixel.hytale.server.core.universe.Universe.get();
                if (universe == null) return;
                PlayerRef liveRef = universe.getPlayer(uid);
                if (liveRef == null) return;
                java.util.UUID worldUuid = liveRef.getWorldUuid();
                com.hypixel.hytale.server.core.universe.world.World world =
                    worldUuid != null ? universe.getWorld(worldUuid) : null;
                if (world == null) return;
                world.execute(() -> {
                    try {
                        WeaponCategory current = WeaponCategory.heldCategory(liveRef);
                        if (current != lastSeenCategory) {
                            lastSeenCategory = current;
                            refreshSlots();
                        }
                    } catch (Exception ignored) {}
                });
            } catch (Exception ignored) {}
        }, 500, 500, TimeUnit.MILLISECONDS);
    }

    public void refreshSlots() {
        PlayerRef ref = getPlayerRef();
        PlayerSpecialization spec = resolveActiveSpec();
        boolean weaponOk;
        if (spec == PlayerSpecialization.BAGARREUR) {
            weaponOk = WeaponCategory.heldCategory(ref) == null;
        } else {
            weaponOk = spec != null && WeaponCategory.specCanUseHeldWeapon(spec, ref);
        }

        UICommandBuilder cmd = new UICommandBuilder();
        for (String slotId : SLOT_IDS) {
            String uiId = slotUiId(slotId);
            String iconPath = resolveSlotItemId(slotId);
            boolean bound = weaponOk && iconPath != null && !iconPath.isBlank();
            cmd.set("#VRpgSlot" + uiId + ".Visible", bound);
            cmd.set("#VRpgSlot" + uiId + "Key.Visible", bound);
            if (bound) {
                cmd.setObject("#VRpgSlot" + uiId + "Icon.Background",
                    new PatchStyle(Value.of(iconPath), Value.of(0)));
            }
        }
        this.update(false, cmd);
    }

    @Nullable
    private PlayerSpecialization resolveActiveSpec() {
        VaryonRpgPlugin plugin = VaryonRpgPlugin.getInstance();
        if (plugin == null) return null;
        ClassManager classManager = plugin.getClassManager();
        if (classManager == null) return null;
        UUID uuid = getPlayerRef().getUuid();
        if (uuid == null) return null;
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (acc == null) return null;
        PlayerClass cls = acc.getActiveClass();
        if (cls == null) return null;
        return acc.getActiveSpec(cls);
    }

    public void startCooldown(@Nonnull String slotId, @Nonnull String skillId) {
        VaryonRpgPlugin plugin = VaryonRpgPlugin.getInstance();
        if (plugin == null) return;
        ClassSkillService skills = plugin.getClassSkillService();
        ClassManager classManager = plugin.getClassManager();
        if (skills == null || classManager == null) return;
        UUID uuid = getPlayerRef().getUuid();
        if (uuid == null) return;
        ClassAccount acc = classManager.getOrLoad(uuid);
        if (acc == null) return;
        PlayerClass cls = acc.getActiveClass();
        if (cls == null) return;

        long totalMs = skills.getCooldownTotalMs(skillId, acc, cls);
        if (totalMs <= 0L) return;

        cancelCooldownTask(slotId);
        scheduleCooldownTick(slotId, skillId, totalMs, uuid, acc, cls, skills);
    }

    private void scheduleCooldownTick(@Nonnull String slotId, @Nonnull String skillId,
                                      long totalMs, @Nonnull UUID uuid,
                                      @Nonnull ClassAccount acc, @Nonnull PlayerClass cls,
                                      @Nonnull ClassSkillService skills) {
        long remainingMs = skills.getCooldownRemainingMs(uuid, skillId, acc, cls);
        if (remainingMs <= 0L) {
            clearCooldownOverlay(slotId);
            return;
        }

        updateCooldownOverlay(slotId, remainingMs, totalMs);

        long delayMs = 200L;
        ScheduledFuture<?> task = SCHEDULER.schedule(
            () -> scheduleCooldownTick(slotId, skillId, totalMs, uuid, acc, cls, skills),
            delayMs, TimeUnit.MILLISECONDS
        );
        cooldownTasks.put(slotId, task);
    }

    private void updateCooldownOverlay(@Nonnull String slotId, long remainingMs, long totalMs) {
        String uiId = slotUiId(slotId);
        double ratio = Math.min(1.0, (double) remainingMs / totalMs);

        String timerText = remainingMs > 1000L
            ? String.valueOf((int) Math.ceil(remainingMs / 1000.0))
            : String.format("%.1f", remainingMs / 1000.0).replace(",", ".");

        UICommandBuilder cmd = new UICommandBuilder();
        int cropHeight = (int) Math.round(ratio * SLOT_SIZE);
        int cropTop = 6 + (SLOT_SIZE - cropHeight);
        Anchor cdAnchor = new Anchor();
        cdAnchor.setLeft(Value.of(6));
        cdAnchor.setTop(Value.of(cropTop));
        cdAnchor.setWidth(Value.of(SLOT_SIZE));
        cdAnchor.setHeight(Value.of(cropHeight));
        cmd.set("#VRpgSlot" + uiId + "CooldownBg.Visible", true);
        cmd.set("#VRpgSlot" + uiId + "Cooldown.Visible", true);
        cmd.setObject("#VRpgSlot" + uiId + "Cooldown.Anchor", cdAnchor);
        cmd.set("#VRpgSlot" + uiId + "TimerGroup.Visible", true);
        cmd.set("#VRpgSlot" + uiId + "TimerLabel.Text", timerText);
        this.update(false, cmd);
    }

    private void clearCooldownOverlay(@Nonnull String slotId) {
        String uiId = slotUiId(slotId);
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#VRpgSlot" + uiId + "CooldownBg.Visible", false);
        cmd.set("#VRpgSlot" + uiId + "Cooldown.Visible", false);
        cmd.set("#VRpgSlot" + uiId + "TimerGroup.Visible", false);
        cmd.set("#VRpgSlot" + uiId + "TimerLabel.Text", "");
        this.update(false, cmd);
    }

    private void cancelCooldownTask(@Nonnull String slotId) {
        ScheduledFuture<?> existing = cooldownTasks.remove(slotId);
        if (existing != null) existing.cancel(false);
    }

    private void cancelAllCooldownTasks() {
        cooldownTasks.values().forEach(f -> f.cancel(false));
        cooldownTasks.clear();
        if (weaponWatchTask != null) { weaponWatchTask.cancel(false); weaponWatchTask = null; }
    }

    @Nonnull
    private String slotUiId(@Nonnull String slotId) {
        return switch (slotId) {
            case "CrouchE" -> "CrouchE";
            case "CrouchR" -> "CrouchR";
            case "CrouchA" -> "CrouchA";
            default -> slotId;
        };
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
        return acc.getSkillSlot(activeClass, slotId);
    }
}
