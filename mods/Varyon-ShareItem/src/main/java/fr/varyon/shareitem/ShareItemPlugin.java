package fr.varyon.shareitem;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class ShareItemPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public ShareItemPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getCommandRegistry().registerCommand(new ShareCommand());
        LOGGER.atInfo().log("ShareItem loaded");
    }
}
