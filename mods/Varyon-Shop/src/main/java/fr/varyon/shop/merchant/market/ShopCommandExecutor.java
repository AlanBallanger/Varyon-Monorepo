package fr.varyon.shop.merchant.market;

import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * Runs a service entry's command, with placeholder substitution. Mirrors BetterShopAuction's
 * command-item execution pattern (FileShopProvider). Supported placeholders: {player},
 * {display_name}/{displayname}, {uuid}, {amount}. Multiple ";"-separated segments run as
 * separate commands. asPlayer=false (default) runs as server console (any command, no permission
 * check); asPlayer=true runs as the buying player themselves (subject to their own permissions).
 */
public final class ShopCommandExecutor {
    private ShopCommandExecutor() {
    }

    public static void run(String template, PlayerRef playerRef, int amount) {
        run(template, playerRef, amount, false);
    }

    public static void run(String template, PlayerRef playerRef, int amount, boolean asPlayer) {
        if (template == null || template.isBlank() || playerRef == null) {
            return;
        }
        String playerName = playerRef.getUsername();
        if (playerName == null || playerName.isBlank()) {
            playerName = playerRef.getUuid() == null ? "Player" : playerRef.getUuid().toString();
        }
        String playerUuid = playerRef.getUuid() == null ? "" : playerRef.getUuid().toString();
        String resolved = resolve(template, playerName, playerUuid, amount);
        CommandSender sender = asPlayer ? playerRef : ConsoleSender.INSTANCE;

        for (String segmentRaw : resolved.split(";")) {
            String segment = segmentRaw == null ? "" : segmentRaw.trim();
            if (segment.isEmpty()) {
                continue;
            }
            if (segment.startsWith("/")) {
                segment = segment.substring(1).trim();
            }
            if (segment.isEmpty()) {
                continue;
            }
            CommandManager.get().handleCommand(sender, segment);
        }
    }

    private static String resolve(String template, String playerName, String playerUuid, int amount) {
        return template
                .replace("{player}", playerName)
                .replace("{display_name}", playerName)
                .replace("{displayname}", playerName)
                .replace("{uuid}", playerUuid)
                .replace("{amount}", String.valueOf(Math.max(1, amount)));
    }
}
