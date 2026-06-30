package fr.varyon.vrpg.ui;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.vrpg.VaryonRpgPlugin;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassProgress;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.ui.prefs.HudLayoutHelper;
import fr.varyon.vrpg.ui.prefs.PlayerUiPreferences;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassXpHud extends CustomUIHud {

    public static final String HUD_KEY = "vrpg_class_xp_hud";

    private static final ConcurrentHashMap<UUID, ClassXpHud> INSTANCES = new ConcurrentHashMap<>();

    private static final PatchStyle TRANSPARENT = new PatchStyle().setColor(Value.of("#00000000"));

    private boolean built = false;
    private boolean hidden = false;
    private boolean panelBackgroundHidden = false;

    public ClassXpHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, HUD_KEY);
    }

    @Nonnull
    public static ClassXpHud getOrCreate(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        ClassXpHud existing = INSTANCES.get(uuid);
        if (existing != null) return existing;

        ClassXpHud hud = new ClassXpHud(playerRef);
        ClassXpHud race = INSTANCES.putIfAbsent(uuid, hud);
        if (race != null) return race;

        player.getHudManager().addCustomHud(playerRef, hud);
        return hud;
    }

    @Nullable
    public static ClassXpHud get(@Nonnull UUID uuid) {
        return INSTANCES.get(uuid);
    }

    public static void refreshIfPresent(@Nonnull UUID uuid) {
        ClassXpHud hud = INSTANCES.get(uuid);
        if (hud != null) hud.refresh();
    }

    public static void cleanup(@Nonnull UUID uuid) {
        INSTANCES.remove(uuid);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        builder.append("VRpgClassHud.ui");
        built = true;
        applyBar(builder);
    }

    public void refresh() {
        if (!built) return;
        UICommandBuilder builder = new UICommandBuilder();
        applyBar(builder);
        update(false, builder);
    }

    public void setHidden(boolean hidden) {
        if (this.hidden == hidden) return;
        this.hidden = hidden;
        if (!built) return;
        UICommandBuilder builder = new UICommandBuilder();
        applyBar(builder);
        update(false, builder);
    }

    public void applyPreferences(@Nonnull PlayerUiPreferences prefs) {
        hidden = !prefs.classHudVisible;
        if (!built) return;
        UICommandBuilder builder = new UICommandBuilder();
        HudLayoutHelper.applyClassPanelAnchor(builder, prefs);
        applyBar(builder);
        update(false, builder);
    }

    private void applyBar(@Nonnull UICommandBuilder builder) {
        if (hidden) {
            builder.setObject("#ClassXPPanel.Background", TRANSPARENT);
            panelBackgroundHidden = true;
            builder.set("#ClassHudBorder.Visible", false);
            hideBar(builder);
            return;
        }

        if (panelBackgroundHidden) {
            builder.setObject("#ClassXPPanel.Background", RpgUiStyles.HUD_XP_PANEL_STYLE);
            panelBackgroundHidden = false;
        }

        builder.set("#ClassHudBorder.Visible", true);

        VaryonRpgPlugin plugin = VaryonRpgPlugin.getInstance();
        if (plugin == null || plugin.getClassManager() == null) {
            hideBar(builder);
            return;
        }

        ClassAccount acc = plugin.getClassManager().getOrLoad(getPlayerRef().getUuid());
        PlayerClass activeClass = acc.getActiveClass();
        if (activeClass == null) {
            hideBar(builder);
            return;
        }

        ClassProgress prog = acc.getProgress(activeClass);
        PlayerSpecialization spec = prog.getActiveSpec();

        String displayName = spec != null ? spec.getDisplayName() : activeClass.getDisplayName();
        String levelText = "Nv." + prog.getLevel();
        if (acc.availableTalentPoints(activeClass) > 0) {
            levelText += " *";
        }

        builder.set("#ClassRow.Visible", true);
        builder.set("#ClassProgBar.Visible", true);
        builder.set("#ClassLevel.TextSpans", Message.raw(levelText));
        builder.set("#ClassName.TextSpans", Message.raw(displayName));

        if (prog.isMaxLevel()) {
            builder.set("#ClassXPText.TextSpans", Message.raw("MAX"));
            builder.set("#ClassProgBarFill.Value", 1.0);
        } else {
            builder.set("#ClassXPText.TextSpans",
                Message.raw(prog.getXpInLevel() + "/" + prog.getXpToNextLevel()));
            double ratio = prog.getXpToNextLevel() > 0L
                ? Math.min(1.0, (double) prog.getXpInLevel() / prog.getXpToNextLevel())
                : 1.0;
            builder.set("#ClassProgBarFill.Value", ratio);
        }
    }

    private void hideBar(@Nonnull UICommandBuilder builder) {
        builder.set("#ClassRow.Visible", false);
        builder.set("#ClassProgBar.Visible", false);
        builder.set("#ClassLevel.TextSpans", Message.raw(" "));
        builder.set("#ClassName.TextSpans", Message.raw(" "));
        builder.set("#ClassXPText.TextSpans", Message.raw(" "));
        builder.set("#ClassProgBarFill.Value", 0.0);
    }
}
