package fr.varyon.shareitem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.asset.util.ColorParseUtil;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.console.ConsoleModule;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.MessageUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;

public class ShareCommand extends AbstractPlayerCommand {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Message NO_ITEM = Message.raw("Vous ne tenez aucun objet.");

    public ShareCommand() {
        super("share", "Share your held item to chat");
    }

    @Override
    protected void execute(@NotNull CommandContext context, @NotNull Store<EntityStore> store,
                           @NotNull Ref<EntityStore> ref, @NotNull PlayerRef playerRef, @NotNull World world) {
        world.execute(() -> {
            InventoryComponent.Hotbar hotbar = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
            if (hotbar == null) return;

            ItemStack stack = hotbar.getActiveItem();
            if (stack == null || !stack.isValid()) {
                playerRef.sendMessage(NO_ITEM.color(java.awt.Color.RED));
                return;
            }

            int quantity = hotbar.getInventory().countItemStacks(
                s -> s != null && s.isValid() && s.getItemId().equals(stack.getItemId())
            );

            Color color = ((ItemQuality) ItemQuality.getAssetMap()
                .getAsset(stack.getItem().getQualityIndex())).getTextColor();
            String itemName = I18nModule.get().getMessage(playerRef.getLanguage(), stack.getItem().getTranslationKey());

            Message msg = Message.raw(playerRef.getUsername() + " a partagé [")
                .insert(Message.raw(quantity + "x ").bold(true))
                .insert(Message.raw(itemName).color(ColorParseUtil.colorToHexString(color)).bold(true))
                .insert(Message.raw("]"));

            UUID playerUUID = playerRef.getUuid();
            Collection<PlayerRef> allPlayers = Universe.get().getPlayers();
            ObjectArrayList<PlayerRef> targets = new ObjectArrayList<>(allPlayers);
            targets.removeIf(t -> t.getHiddenPlayersManager().isPlayerHidden(playerUUID));

            String chatLine = " shared [%dx %s]".formatted(quantity, itemName);

            HytaleServer.get().getEventBus()
                .dispatchForAsync(PlayerChatEvent.class)
                .dispatch(new PlayerChatEvent(playerRef, targets, chatLine))
                .whenComplete((event, throwable) -> {
                    if (throwable != null) {
                        LOGGER.at(Level.SEVERE).withCause(throwable)
                            .log("Error dispatching PlayerChatEvent for %s", playerRef.getUsername());
                    } else if (!event.isCancelled()) {
                        LOGGER.at(Level.INFO).log(
                            MessageUtil.toAnsiString(msg).toAnsi(ConsoleModule.get().getTerminal())
                        );
                        for (PlayerRef target : event.getTargets()) {
                            target.sendMessage(msg);
                        }
                    }
                });
        });
    }
}
