/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.assetstore.event.LoadedAssetsEvent
 *  com.hypixel.hytale.server.core.asset.type.item.config.Item
 *  com.hypixel.hytale.server.core.command.system.AbstractCommand
 *  com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent
 *  com.hypixel.hytale.server.core.modules.i18n.event.MessagesUpdated
 *  com.hypixel.hytale.server.core.plugin.JavaPlugin
 *  com.hypixel.hytale.server.core.plugin.JavaPluginInit
 *  javax.annotation.Nonnull
 */
package com.woxtz.weaponinfo;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.i18n.event.MessagesUpdated;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.woxtz.weaponinfo.PluginConfig;
import com.woxtz.weaponinfo.WeaponDamageCache;
import com.woxtz.weaponinfo.commands.WeaponsAdminCommand;
import com.woxtz.weaponinfo.commands.WeaponsCommand;
import com.woxtz.weaponinfo.events.PlayerReadyListener;
import com.woxtz.weaponinfo.util.WeaponTooltipInjector;
import javax.annotation.Nonnull;

public class WeaponInfoPlugin
extends JavaPlugin {
    public WeaponInfoPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    protected void setup() {
        PluginConfig.getInstance().load(this);
        this.getCommandRegistry().registerCommand((AbstractCommand)new WeaponsCommand("weapons", "Browse all weapons with damage stats"));
        this.getCommandRegistry().registerCommand((AbstractCommand)new WeaponsAdminCommand("weaponsadmin", "Configure Weapon Stats Viewer settings", this));
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, PlayerReadyListener::onPlayerReady);
        this.getEventRegistry().registerGlobal(MessagesUpdated.class, event -> WeaponTooltipInjector.injectTooltips());
        this.getEventRegistry().register(LoadedAssetsEvent.class, Item.class, WeaponDamageCache::cacheWeaponDamages);
    }
}

