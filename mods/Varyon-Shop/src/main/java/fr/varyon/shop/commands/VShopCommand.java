package fr.varyon.shop.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import fr.varyon.shop.VaryonShopPlugin;
import fr.varyon.shop.config.BindWandManager;
import fr.varyon.shop.config.MerchantRegistry;
import fr.varyon.shop.integration.DenizensBridge;
import fr.varyon.shop.util.EntityApiCompat;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * "/vshop bind &lt;type&gt;" arms a wand: the next Denizen NPC the player interacts with (right
 * click) gets bound to that merchant type. "/vshop unbind" does the same to remove a binding.
 * "/vshop list" lists every currently bound merchant NPC.
 * Types: racheteur (buyback general), profession, decor, ressources.
 */
public final class VShopCommand extends CommandBase {
    private static final String USAGE = "Types valides: racheteur, profession, decor, ressources.\n"
            + "Usage: /vshop bind <type> puis interagissez (clic droit) avec le NPC.\n"
            + "       /vshop unbind puis interagissez avec le NPC.\n"
            + "       /vshop list\n"
            + "       /vshop reload";

    private final VaryonShopPlugin plugin;
    private final MerchantRegistry merchantRegistry;
    private final BindWandManager bindWandManager;

    public VShopCommand(VaryonShopPlugin plugin, MerchantRegistry merchantRegistry, BindWandManager bindWandManager) {
        super("vshop", "Administration des marchands Varyon-Shop");
        this.plugin = plugin;
        this.merchantRegistry = merchantRegistry;
        this.bindWandManager = bindWandManager;
        this.requirePermission("varyonshop.admin");
        this.setAllowsExtraArguments(true);
    }

    private DenizensBridge denizensBridge() {
        return plugin.getDenizensBridge();
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        String[] tokens = tokens(ctx);
        if (tokens.length < 2) {
            ctx.sendMessage(Message.raw(USAGE));
            return;
        }

        if ("reload".equalsIgnoreCase(tokens[1])) {
            handleReload(ctx);
            return;
        }

        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("Cette commande est reservee aux joueurs."));
            return;
        }

        switch (tokens[1].toLowerCase(Locale.ROOT)) {
            case "bind" -> handleBind(ctx, tokens);
            case "unbind" -> handleUnbind(ctx);
            case "list" -> handleList(ctx);
            default -> ctx.sendMessage(Message.raw(USAGE));
        }
    }

    private void handleReload(CommandContext ctx) {
        plugin.reloadConfig();
        ctx.sendMessage(Message.raw("Varyon-Shop: prix de rachat et liaisons marchands recharges depuis le disque."));
    }

    private void handleBind(CommandContext ctx, String[] tokens) {
        if (tokens.length < 3) {
            ctx.sendMessage(Message.raw(USAGE));
            return;
        }
        if (!denizensBridge().isAvailable()) {
            ctx.sendMessage(Message.raw("Varyon-Shop: QuestLinesDenizens n'est pas disponible sur ce serveur."));
            return;
        }
        MerchantRegistry.MerchantType type = parseType(tokens[2]);
        if (type == null) {
            ctx.sendMessage(Message.raw("Type inconnu: " + tokens[2] + "\n" + USAGE));
            return;
        }
        UUID playerUuid = playerUuid(ctx);
        if (playerUuid == null) {
            return;
        }
        bindWandManager.arm(playerUuid, type);
        ctx.sendMessage(Message.raw("Varyon-Shop: interagissez (clic droit) avec le NPC a lier au marchand " + type.name() + " (60s)."));
    }

    private void handleUnbind(CommandContext ctx) {
        if (!denizensBridge().isAvailable()) {
            ctx.sendMessage(Message.raw("Varyon-Shop: QuestLinesDenizens n'est pas disponible sur ce serveur."));
            return;
        }
        UUID playerUuid = playerUuid(ctx);
        if (playerUuid == null) {
            return;
        }
        bindWandManager.arm(playerUuid, null);
        ctx.sendMessage(Message.raw("Varyon-Shop: interagissez (clic droit) avec le NPC a delier (60s)."));
    }

    private void handleList(CommandContext ctx) {
        if (!denizensBridge().isAvailable()) {
            ctx.sendMessage(Message.raw("Varyon-Shop: QuestLinesDenizens n'est pas disponible sur ce serveur."));
            return;
        }
        List<UUID> bound = merchantRegistry.listBoundDenizenIds();
        if (bound.isEmpty()) {
            ctx.sendMessage(Message.raw("Varyon-Shop: aucun marchand configure."));
            return;
        }
        StringBuilder sb = new StringBuilder("Varyon-Shop: marchands configures (" + bound.size() + ")");
        for (UUID id : bound) {
            MerchantRegistry.MerchantType type = merchantRegistry.typeOf(id);
            String name = denizensBridge().getName(id);
            sb.append("\n - ").append(name == null ? id.toString() : name).append(": ").append(type == null ? "?" : type.name());
        }
        ctx.sendMessage(Message.raw(sb.toString()));
    }

    private UUID playerUuid(CommandContext ctx) {
        var playerRef = EntityApiCompat.senderAsPlayerRef(ctx);
        return playerRef == null ? null : playerRef.getUuid();
    }

    private MerchantRegistry.MerchantType parseType(String raw) {
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "racheteur", "buyback", "general" -> MerchantRegistry.MerchantType.BUYBACK_GENERAL;
            case "profession", "metier" -> MerchantRegistry.MerchantType.BUYBACK_PROFESSION;
            case "decor", "decoration", "decorations" -> MerchantRegistry.MerchantType.MARKET_DECOR;
            case "ressources", "resources" -> MerchantRegistry.MerchantType.MARKET_RESOURCES;
            default -> null;
        };
    }

    private String[] tokens(CommandContext ctx) {
        String input = ctx == null ? null : ctx.getInputString();
        if (input == null || input.isBlank()) {
            return new String[0];
        }
        return input.trim().split("\\s+");
    }
}
