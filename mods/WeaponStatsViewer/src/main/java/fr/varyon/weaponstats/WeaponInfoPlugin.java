package fr.varyon.weaponstats;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.modules.i18n.event.MessagesUpdated;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import fr.varyon.weaponstats.WeaponDamageCache;
import fr.varyon.weaponstats.commands.WeaponsCommand;
import fr.varyon.weaponstats.util.WeaponTooltipInjector;
import javax.annotation.Nonnull;

public class WeaponInfoPlugin
extends JavaPlugin {
    public WeaponInfoPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    protected void setup() {
        this.getCommandRegistry().registerCommand((AbstractCommand)new WeaponsCommand("weapons", "Browse all weapons with damage stats"));
        this.getEventRegistry().registerGlobal(MessagesUpdated.class, event -> WeaponTooltipInjector.injectTooltips());
        this.getEventRegistry().register(LoadedAssetsEvent.class, Item.class, WeaponDamageCache::cacheWeaponDamages);
    }
}
