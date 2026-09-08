package fr.varyon.trade.commands.subcommands;

import fr.varyon.trade.VaryonTradePlugin;
import fr.varyon.trade.TradeConfig;
import fr.varyon.trade.data.NotificationData;
import fr.varyon.trade.data.PlayerConfigData;
import fr.varyon.trade.helpers.NotificationHelper;
import fr.varyon.trade.helpers.TranslationHelper;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ReloadCommand extends AbstractPlayerCommand
{
    public ReloadCommand()
    {
        super("reload", "Reloads the mod configuration.");
        this.requirePermission("varyon.trade.reload");
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world)
    {
        String language = PlayerConfigData.getConfigData(playerRef.getUuid()).vars.language.getValue();

        TradeConfig.get().reload();
        TranslationHelper.loadAllTranslations();

        NotificationData notificationData = new NotificationData(
                TranslationHelper.getTranslation("notification.varyon_trade.title", language),
                TranslationHelper.getTranslation("notification.config_reloaded", language),
                TradeConfig.get().getNotificationIconId(),
                "#ffffff", "#2ae917"
        );

        NotificationHelper.send(playerRef, notificationData);
    }
}

