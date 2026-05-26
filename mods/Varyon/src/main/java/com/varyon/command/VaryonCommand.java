package com.varyon.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.VaryonPlugin;
import com.varyon.config.ConfigManager;
import com.varyon.faction.FactionManager;
import com.varyon.deposit.DepositBlockManager;
import com.varyon.shop.ShopUIPage;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class VaryonCommand extends AbstractAsyncCommand {
    private final VaryonPlugin plugin;
    private final FactionManager factionManager;
    private final DepositBlockManager depositBlockManager;

    public VaryonCommand(VaryonPlugin plugin, FactionManager factionManager, DepositBlockManager depositBlockManager) {
        super("varyon", "Varyon commands");
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.depositBlockManager = depositBlockManager;
        this.addSubCommand(new HelpSubCommand());
        this.addSubCommand(new ReloadSubCommand(plugin));
        this.addSubCommand(new ClearMapSubCommand());
        this.addSubCommand(new ResetRewardsSubCommand());
        this.addSubCommand(new ResetBalanceSubCommand());
        this.addSubCommand(new com.varyon.command.CreateDepositSubCommand(depositBlockManager));
        this.addSubCommand(new com.varyon.command.ResetDepositSubCommand(depositBlockManager));
        this.addSubCommand(new WhoIsSubCommand(factionManager));
        this.addSubCommand(new ShopSubCommand());
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext context) {
        return new HelpSubCommand().executeAsync(context);
    }

    public class HelpSubCommand extends AbstractAsyncCommand {
        public HelpSubCommand() {
            super("help", "Show available commands");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            CommandSender sender = context.sender();
            boolean isAdmin = false;
            boolean hasRtp = false;

            if (sender instanceof PlayerRef pr) {
                isAdmin = pr.hasPermission("varyon.admin");
                hasRtp = pr.hasPermission("varyon.rtp");
            } else {
                isAdmin = true;
                hasRtp = true;
            }

            context.sendMessage(Message.raw("  /varyon extract : Invoque un portail d'extraction").color(Color.WHITE));
            context.sendMessage(Message.raw("  /varyon shop : Boutique (clés contre fragments)").color(Color.WHITE));
            context.sendMessage(Message.raw("  /varyon whois : Voir votre faction détectée").color(Color.WHITE));
            context.sendMessage(Message.raw("  /return : Menu de confirmation (retour près de votre point de mort)").color(Color.WHITE));
            context.sendMessage(Message.raw("  /points : Voir vos points de faction").color(Color.WHITE));
            context.sendMessage(Message.raw("  /points top : Classement des points de faction").color(Color.WHITE));
            context.sendMessage(Message.raw("  /points deposit <montant> : Déposer des points de faction pour votre faction").color(Color.WHITE));

            if (isAdmin) {
                context.sendMessage(Message.raw("  /varyon reload : Recharger la configuration").color(Color.WHITE));
                context.sendMessage(Message.raw("  /varyon clearmap : Vider le cache de la map").color(Color.WHITE));
                context.sendMessage(Message.raw("  /varyon createdeposit : Créer un bloc de dépôt").color(Color.WHITE));
                context.sendMessage(Message.raw("  /varyon resetdeposit : Supprimer tous les blocs de dépôt").color(Color.WHITE));
                context.sendMessage(Message.raw("  /varyon resetrewards : Reset les cooldowns des récompenses de faction").color(Color.WHITE));
                context.sendMessage(Message.raw("  /varyon resetbalance : Remettre la jauge globale (points) à 0").color(Color.WHITE));
                context.sendMessage(Message.raw("  /points give <joueur> <montant> : Donner des points de faction à un joueur").color(Color.WHITE));
                context.sendMessage(Message.raw("  /points take <joueur> <montant> : Retirer des points de faction à un joueur").color(Color.WHITE));
                context.sendMessage(Message.raw("  /points setmax <joueur> <montant> : Fixer le stock de points de faction d'un joueur").color(Color.WHITE));
            }

            if (hasRtp) {
                context.sendMessage(Message.raw("  /rtpv <zone> : TP dans une zone de varyon").color(Color.WHITE));
                context.sendMessage(Message.raw("  /join <joueur> : Rejoindre un ami après son /rtpv").color(Color.WHITE));
                context.sendMessage(Message.raw("  /rtpz [zone] : TP dans une zone du jeu vanilla").color(Color.WHITE));
            }

            return CompletableFuture.completedFuture(null);
        }
    }

    public static class WhoIsSubCommand extends AbstractAsyncCommand {
        private final FactionManager factionManager;

        public WhoIsSubCommand(FactionManager factionManager) {
            super("whois", "Show your current faction");
            this.factionManager = factionManager;
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            CommandSender sender = context.sender();
            if (!(sender instanceof PlayerRef playerRef)) {
                context.sendMessage(Message.raw("Commande joueur uniquement.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            context.sendMessage(Message.raw("=== Faction Debug ===").color(Color.YELLOW));
            context.sendMessage(Message.raw("PlayerRef: " + playerRef.getUsername() + " / " + playerRef.getUuid())
                .color(Color.GREEN));
            context.sendMessage(Message.raw("hasPermission(group.fracture) = " + playerRef.hasPermission("group.fracture")).color(Color.WHITE));
            context.sendMessage(Message.raw("hasPermission(group.noyau)    = " + playerRef.hasPermission("group.noyau")).color(Color.WHITE));

            FactionManager.Faction faction = factionManager.getFactionVerbose(playerRef);
            context.sendMessage(Message.raw("Faction LP : " + (faction != null ? faction.getDisplayName() : "aucune"))
                .color(faction != null ? Color.GREEN : Color.RED));
            context.sendMessage(Message.raw("(Voir les logs serveur pour le détail)").color(Color.GRAY));
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class ReloadSubCommand extends AbstractAsyncCommand {
        private final VaryonPlugin plugin;

        public ReloadSubCommand(VaryonPlugin plugin) {
            super("reload", "Reload configuration");
            this.plugin = plugin;
            this.requirePermission("varyon.admin");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            try {
                ConfigManager configManager = plugin.getConfigManager();
                configManager.reload();
                plugin.onConfigurationReloaded();

                int zoneCount = configManager.getZoneConfig().getZones().size();
                context.sendMessage(Message.raw("Configuration rechargée! (" + zoneCount + " zones)").color(Color.GREEN));
            } catch (Exception e) {
                context.sendMessage(Message.raw("Erreur: " + e.getMessage()).color(Color.RED));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class ClearMapSubCommand extends AbstractAsyncCommand {
        public ClearMapSubCommand() {
            super("clearmap", "Clear entire map cache");
            this.requirePermission("varyon.admin");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            int worldCount = Universe.get().getWorlds().size();
            if (worldCount == 0) {
                context.sendMessage(Message.raw("Aucun monde trouvé.").color(Color.YELLOW));
                return CompletableFuture.completedFuture(null);
            }

            for (World world : Universe.get().getWorlds().values()) {
                world.execute(() -> {
                    world.getWorldMapManager().clearImages();
                    for (PlayerRef playerRef : world.getPlayerRefs()) {
                        Ref ref = playerRef.getReference();
                        if (ref == null || !ref.isValid()) continue;
                        Player player = (Player) world.getEntityStore().getStore().getComponent(ref, Player.getComponentType());
                        if (player == null) continue;
                        player.getWorldMapTracker().clear();
                    }
                });
            }

            context.sendMessage(Message.raw("Cache map vidé pour " + worldCount + " monde(s)!").color(Color.GREEN));
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class ResetRewardsSubCommand extends AbstractAsyncCommand {
        public ResetRewardsSubCommand() {
            super("resetrewards", "Reset all reward tier cooldowns");
            this.requirePermission("varyon.admin");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            com.varyon.essence.GlobalRewardsManager rewardsManager = VaryonPlugin.getStaticGlobalRewardsManager();
            
            if (rewardsManager == null) {
                context.sendMessage(Message.raw("Système de récompenses non initialisé.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }
            
            rewardsManager.resetAllCooldowns();
            context.sendMessage(Message.raw("Tous les cooldowns de récompenses ont été réinitialisés.").color(Color.GREEN));
            
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class ResetBalanceSubCommand extends AbstractAsyncCommand {
        public ResetBalanceSubCommand() {
            super("resetbalance", "Remettre la jauge globale de points de faction à 0");
            this.requirePermission("varyon.admin");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            com.varyon.essence.EssenceManager essenceManager = VaryonPlugin.getStaticEssenceManager();
            
            if (essenceManager == null) {
                context.sendMessage(Message.raw("Système de points de faction non initialisé.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }
            
            int oldBalance = essenceManager.getGlobalBalance();
            essenceManager.setGlobalBalance(0);
            
            context.sendMessage(Message.raw("Balance globale réinitialisée: " + oldBalance + " → 0").color(Color.GREEN));
            
            // Update all HUDs
            VaryonPlugin plugin = VaryonPlugin.getInstance();
            if (plugin != null && plugin.getHudManager() != null) {
                plugin.getHudManager().broadcastBalanceUpdate();
            }
            
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class ShopSubCommand extends AbstractAsyncCommand {
        public ShopSubCommand() {
            super("shop", "Ouvre la boutique (clés contre fragments)");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext context) {
            CommandSender sender = context.sender();
            if (!(sender instanceof Player player)) {
                context.sendMessage(Message.raw("Commande joueur uniquement.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }
            Ref<EntityStore> ref = context.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) {
                context.sendMessage(Message.raw("Joueur non connecté au monde.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }
            Store<EntityStore> store = ref.getStore();
            World world = ((EntityStore) store.getExternalData()).getWorld();
            if (world == null) {
                context.sendMessage(Message.raw("Monde indisponible.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }
            return CompletableFuture.runAsync(() -> {
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                Player playerComp = store.getComponent(ref, Player.getComponentType());
                if (playerRef == null || playerComp == null) return;
                ShopUIPage page = new ShopUIPage(playerRef);
                playerComp.getPageManager().openCustomPage(ref, store, page);
            }, world);
        }
    }
}
