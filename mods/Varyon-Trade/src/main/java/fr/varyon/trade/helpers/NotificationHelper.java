package fr.varyon.trade.helpers;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import fr.varyon.trade.data.NotificationData;
import com.hypixel.hytale.protocol.ItemWithAllMetadata;


public class NotificationHelper
{
    public static void send(PlayerRef playerRef, NotificationData params)
    {
        var packetHandler = playerRef.getPacketHandler();

        var primaryMessage = Message.raw(params.title).color(params.titleColor);
        var secondaryMessage = Message.raw(params.subtitle).color(params.subtitleColor);
        
        var icon = new ItemStack(params.iconId, 1).toPacket();

        NotificationUtil.sendNotification(
            packetHandler,
            primaryMessage,
            secondaryMessage,
            (ItemWithAllMetadata) icon
        );
    }
}

