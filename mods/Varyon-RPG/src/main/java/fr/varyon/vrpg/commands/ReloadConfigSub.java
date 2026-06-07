package fr.varyon.vrpg.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.vrpg.VaryonRpgPlugin;
import fr.varyon.vrpg.config.ClassXpConfig;
import fr.varyon.vrpg.config.MobCategoriesConfig;
import fr.varyon.vrpg.config.TierMappingConfig;
import fr.varyon.vrpg.config.VrpgConfig;
import fr.varyon.vrpg.config.XpTableConfig;
import fr.varyon.vrpg.ui.RpgUiAdmin;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class ReloadConfigSub extends AbstractAsyncCommand {

    public ReloadConfigSub() {
        super("reload", "Recharge tous les fichiers de configuration du mod");
    }

    @Override
    @Nonnull
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        if (!(ctx.sender() instanceof PlayerRef playerRef)) {
            ctx.sender().sendMessage(Message.raw("Commande joueur uniquement.").color(Color.RED));
            return done();
        }
        if (!RpgUiAdmin.isAdmin(playerRef)) {
            ctx.sender().sendMessage(Message.raw("Permission refusée.").color(Color.RED));
            return done();
        }
        VaryonRpgPlugin plugin = VaryonRpgPlugin.getInstance();
        if (plugin == null) return done();
        Path dataDir = plugin.getPluginDataDirectory();
        VrpgConfig.load(dataDir);
        ClassXpConfig.load(dataDir);
        XpTableConfig.load(dataDir);
        MobCategoriesConfig.load(dataDir);
        TierMappingConfig.load(dataDir);
        ctx.sender().sendMessage(Message.raw("Config rechargée.").color(new Color(0x6BCB7A)));
        return done();
    }

    private static CompletableFuture<Void> done() {
        return CompletableFuture.completedFuture(null);
    }
}
