package fr.varyon.shop.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.shop.VaryonShopPlugin;
import fr.varyon.shop.config.BindWandManager;
import fr.varyon.shop.config.MerchantRegistry;
import fr.varyon.shop.config.ShopCatalog;
import fr.varyon.shop.integration.DenizensBridge;
import fr.varyon.shop.util.EntityApiCompat;
import fr.varyon.shop.util.WorldDayUtil;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * "/vshop" (no arguments) opens the graphical admin panel, which handles shop creation, item
 * editing, currencies and the general buyback rate. "/vshop bind racheteur [categorie]" or
 * "/vshop bind boutique <shopId>" arms a wand: the next Denizen NPC the player interacts with
 * (right click) gets bound accordingly. "/vshop unbind" does the same to remove a binding.
 * "/vshop list" lists every currently bound merchant NPC.
 */
public final class VShopCommand extends CommandBase {
    private static final String USAGE = "Usage:\n"
            + "  /vshop : ouvre le panneau d'administration graphique (boutiques, taux, devises).\n"
            + "  /vshop bind racheteur [categorie] puis interagissez (clic droit) avec le NPC.\n"
            + "  /vshop bind profession <categorie> puis interagissez (clic droit) avec le NPC.\n"
            + "  /vshop bind boutique <shopId> puis interagissez (clic droit) avec le NPC.\n"
            + "  /vshop unbind puis interagissez avec le NPC.\n"
            + "  /vshop rotate [shopId] : force un nouveau tirage (prix + items) dans votre monde. Sans shopId, toutes les boutiques.\n"
            + "  /vshop list\n"
            + "  /vshop reload";

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
            handleAdmin(ctx);
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
            case "rotate" -> handleRotate(ctx, tokens);
            default -> ctx.sendMessage(Message.raw(USAGE));
        }
    }

    private void handleRotate(CommandContext ctx, String[] tokens) {
        PlayerRef playerRef = EntityApiCompat.senderAsPlayerRef(ctx);
        if (playerRef == null) {
            ctx.sendMessage(Message.raw("Impossible de resoudre le joueur."));
            return;
        }
        UUID worldUuid = playerRef.getWorldUuid();
        World world = worldUuid == null ? null : Universe.get().getWorld(worldUuid);
        int day = WorldDayUtil.currentDayOfYear(world);
        if (day < 0) {
            day = 0;
        }

        if (tokens.length < 3) {
            int count = 0;
            for (String id : plugin.getShopCatalogRepository().listShopIds()) {
                ShopCatalog catalog = plugin.getShopCatalogRepository().get(id).orElse(null);
                if (catalog == null) {
                    continue;
                }
                plugin.getShopRotationState().forceReroll(id, String.valueOf(worldUuid), day, catalog);
                count++;
            }
            ctx.sendMessage(Message.raw("Varyon-Shop: rotation forcee pour " + count + " boutique(s) (dans votre monde)."));
            return;
        }

        String shopId = tokens[2];
        ShopCatalog catalog = plugin.getShopCatalogRepository().get(shopId).orElse(null);
        if (catalog == null) {
            ctx.sendMessage(Message.raw("Varyon-Shop: boutique inconnue '" + shopId + "'."));
            return;
        }
        plugin.getShopRotationState().forceReroll(shopId, String.valueOf(worldUuid), day, catalog);
        ctx.sendMessage(Message.raw("Varyon-Shop: rotation de '" + shopId + "' forcee (nouveau prix et objets, dans votre monde)."));
    }

    private void handleAdmin(CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw(USAGE));
            return;
        }
        var playerRef = EntityApiCompat.senderAsPlayerRef(ctx);
        if (playerRef == null) {
            ctx.sendMessage(Message.raw("Impossible de resoudre le joueur."));
            return;
        }
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || ref.getStore() == null) {
            ctx.sendMessage(Message.raw("Varyon-Shop: impossible d'ouvrir le panneau."));
            return;
        }
        EntityStore entityStore = ref.getStore().getExternalData();
        World world = entityStore == null ? null : entityStore.getWorld();
        if (world == null) {
            ctx.sendMessage(Message.raw("Varyon-Shop: impossible d'ouvrir le panneau."));
            return;
        }
        world.execute(() -> {
            Player player = EntityApiCompat.getPlayer(playerRef);
            if (player == null || player.getPageManager() == null) {
                return;
            }
            try {
                player.getPageManager().openCustomPage(ref, ref.getStore(),
                        new fr.varyon.shop.ui.admin.ShopAdminListPage(plugin, playerRef));
            } catch (Throwable t) {
                System.err.println("[Varyon-Shop] Failed to open admin panel: " + t);
                t.printStackTrace();
            }
        });
    }

    private void handleReload(CommandContext ctx) {
        plugin.reloadConfig();
        ctx.sendMessage(Message.raw("Varyon-Shop: prix, catalogues et liaisons marchands recharges depuis le disque."));
    }

    private void handleBind(CommandContext ctx, String[] tokens) {
        if (tokens.length < 3) {
            ctx.sendMessage(Message.raw(USAGE));
            return;
        }
        MerchantRegistry.MerchantType type = parseType(tokens[2]);
        if (type == null) {
            ctx.sendMessage(Message.raw("Type inconnu: " + tokens[2] + "\n" + USAGE));
            return;
        }
        if (!denizensBridge().isAvailable()) {
            ctx.sendMessage(Message.raw("Varyon-Shop: QuestLinesDenizens n'est pas disponible sur ce serveur."));
            return;
        }
        UUID playerUuid = playerUuid(ctx);
        if (playerUuid == null) {
            return;
        }

        if (type == MerchantRegistry.MerchantType.MARKET_SHOP) {
            if (tokens.length < 4) {
                ctx.sendMessage(Message.raw("Usage: /vshop bind boutique <shopId>"));
                return;
            }
            String shopId = tokens[3];
            if (!plugin.getShopCatalogRepository().exists(shopId)) {
                ctx.sendMessage(Message.raw("Varyon-Shop: boutique inconnue '" + shopId + "'. Creez-la depuis le panneau d'administration (/vshop)."));
                return;
            }
            bindWandManager.armBind(playerUuid, type, null, shopId);
            ctx.sendMessage(Message.raw("Varyon-Shop: interagissez (clic droit) avec le NPC a lier a la boutique '" + shopId + "' (60s)."));
            return;
        }

        String category = tokens.length >= 4 ? tokens[3] : null;
        if (type == MerchantRegistry.MerchantType.BUYBACK_PROFESSION && category == null) {
            ctx.sendMessage(Message.raw("Usage: /vshop bind profession <catégorie>"));
            return;
        }
        if (category != null && !plugin.getBuybackPriceRepository().listCategories().contains(category)) {
            String available = String.join(", ", plugin.getBuybackPriceRepository().listCategories());
            ctx.sendMessage(Message.raw("Varyon-Shop: catégorie inconnue '" + category + "'."
                    + (available.isBlank() ? " Aucune catégorie configurée dans buyback_prices.json." : " Catégories disponibles: " + available)));
            return;
        }
        bindWandManager.armBind(playerUuid, type, category, null);
        String categorySuffix = category == null ? "" : " (catégorie: " + category + ")";
        ctx.sendMessage(Message.raw("Varyon-Shop: interagissez (clic droit) avec le NPC à lier au marchand " + type.name() + categorySuffix + " (60s)."));
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
        bindWandManager.armUnbind(playerUuid);
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
            MerchantRegistry.Binding binding = merchantRegistry.bindingOf(id);
            String name = denizensBridge().getName(id);
            String typeText = binding == null ? "?" : binding.type().name();
            String detail = binding != null && binding.category() != null ? " (" + binding.category() + ")"
                    : binding != null && binding.shopId() != null ? " (" + binding.shopId() + ")" : "";
            sb.append("\n - ").append(name == null ? id.toString() : name).append(": ").append(typeText).append(detail);
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
            case "boutique", "shop", "market", "ressources", "resources" -> MerchantRegistry.MerchantType.MARKET_SHOP;
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
