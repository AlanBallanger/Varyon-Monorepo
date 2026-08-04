/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.command.system.AbstractCommand
 *  com.hypixel.hytale.server.core.command.system.CommandContext
 *  com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg
 *  com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
 *  com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType
 *  com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
 *  com.hypixel.hytale.server.core.plugin.JavaPlugin
 *  javax.annotation.Nonnull
 *  javax.annotation.Nullable
 */
package com.woxtz.weaponinfo.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.woxtz.weaponinfo.PluginConfig;
import com.woxtz.weaponinfo.util.GradientParser;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class WeaponsAdminCommand
extends AbstractCommandCollection {
    private final JavaPlugin plugin;

    public WeaponsAdminCommand(String name, String description, JavaPlugin plugin) {
        super(name, description);
        this.plugin = plugin;
        this.addSubCommand(new PublicSubcommand(plugin));
        this.addSubCommand(new WelcomeSubcommand(plugin));
        this.addSubCommand(new StatusSubcommand());
    }

    protected boolean canGeneratePermission() {
        return true;
    }

    private static class PublicSubcommand
    extends AbstractCommand {
        private final JavaPlugin plugin;
        private final RequiredArg<Boolean> enableArg;

        public PublicSubcommand(JavaPlugin plugin) {
            super("public", "Enable/disable public access to /weapons command");
            this.plugin = plugin;
            this.enableArg = this.withRequiredArg("enabled", "true or false", (ArgumentType)ArgTypes.BOOLEAN);
        }

        @Nullable
        protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
            Boolean enable = (Boolean)this.enableArg.get(context);
            PluginConfig config = PluginConfig.getInstance();
            config.setPublicCommand(enable);
            config.save(this.plugin);
            if (enable.booleanValue()) {
                context.sendMessage(GradientParser.parse("<gradient:#00FF00:#90EE90>Configuration saved: /weapons is now PUBLIC</gradient>"));
                context.sendMessage(Message.raw((String)"All players can use /weapons without OP").color("#CCCCCC"));
                context.sendMessage(Message.raw((String)"Change applied immediately!").color("#00FF00").bold(true));
            } else {
                context.sendMessage(GradientParser.parse("<gradient:#FF6B6B:#FF0000>Configuration saved: /weapons is now RESTRICTED</gradient>"));
                context.sendMessage(Message.raw((String)"Only OPs can use /weapons command").color("#CCCCCC"));
                context.sendMessage(Message.raw((String)"Change applied immediately!").color("#00FF00").bold(true));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class StatusSubcommand
    extends AbstractCommand {
        public StatusSubcommand() {
            super("status", "Show current configuration");
        }

        @Nullable
        protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
            PluginConfig config = PluginConfig.getInstance();
            context.sendMessage(Message.raw((String)""));
            context.sendMessage(Message.raw((String)"===========================================").color("#FFD700"));
            context.sendMessage(Message.raw((String)"    Weapon Stats Viewer - Configuration").color("#FFD700").bold(true));
            context.sendMessage(Message.raw((String)"===========================================").color("#FFD700"));
            context.sendMessage(Message.raw((String)""));
            if (config.isPublicCommand()) {
                context.sendMessage(GradientParser.parse("/weapons command access: <gradient:#00FF00:#90EE90>PUBLIC</gradient>"));
                context.sendMessage(Message.raw((String)""));
                context.sendMessage(Message.raw((String)"All players can use /weapons without OP").color("#90EE90"));
            } else {
                context.sendMessage(GradientParser.parse("/weapons command access: <gradient:#FF6B6B:#FF0000>RESTRICTED</gradient>"));
                context.sendMessage(Message.raw((String)""));
                context.sendMessage(Message.raw((String)"Only OPs can use /weapons command").color("#FF9999"));
            }
            context.sendMessage(Message.raw((String)""));
            if (config.isShowWelcomeMessage()) {
                context.sendMessage(GradientParser.parse("Welcome message: <gradient:#00FF00:#90EE90>ENABLED</gradient>"));
            } else {
                context.sendMessage(GradientParser.parse("Welcome message: <gradient:#FF6B6B:#FF0000>DISABLED</gradient>"));
            }
            context.sendMessage(Message.raw((String)""));
            context.sendMessage(GradientParser.parse("Use <gradient:#FFD700:#FF1493>/weaponsadmin public <true|false></gradient> to change access"));
            context.sendMessage(GradientParser.parse("Use <gradient:#FFD700:#FF1493>/weaponsadmin welcome <true|false></gradient> to toggle welcome msg"));
            context.sendMessage(Message.raw((String)"===========================================").color("#FFD700"));
            context.sendMessage(Message.raw((String)""));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class WelcomeSubcommand
    extends AbstractCommand {
        private final JavaPlugin plugin;
        private final RequiredArg<Boolean> enableArg;

        public WelcomeSubcommand(JavaPlugin plugin) {
            super("welcome", "Enable/disable welcome message on join");
            this.plugin = plugin;
            this.enableArg = this.withRequiredArg("enabled", "true or false", (ArgumentType)ArgTypes.BOOLEAN);
        }

        @Nullable
        protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
            Boolean enable = (Boolean)this.enableArg.get(context);
            PluginConfig config = PluginConfig.getInstance();
            config.setShowWelcomeMessage(enable);
            config.save(this.plugin);
            if (enable.booleanValue()) {
                context.sendMessage(GradientParser.parse("<gradient:#00FF00:#90EE90>Configuration saved: Welcome message is now ENABLED</gradient>"));
            } else {
                context.sendMessage(GradientParser.parse("<gradient:#FF6B6B:#FF0000>Configuration saved: Welcome message is now DISABLED</gradient>"));
            }
            return CompletableFuture.completedFuture(null);
        }
    }
}

