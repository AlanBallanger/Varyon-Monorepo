package com.varyon.craftrestrict.packets;

import com.varyon.craftrestrict.Main;
import com.varyon.craftrestrict.config.CraftRestrictConfig;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;

public abstract class CraftRestrictPacketInterceptor {

    public PacketFilter packetFilter;
    public HytaleLogger logger;
    public static CraftRestrictConfig config;

    public void init() {
        this.logger = Main.getPluginInstance().getLogger();
        config = Main.getConfig();
    }

    public static void reloadConfig(CraftRestrictConfig craftRestrictConfig) {
        config = craftRestrictConfig;
    }

    public void unregister() {
        if (this.packetFilter != null) {
            try {
                PacketAdapters.deregisterInbound(this.packetFilter);
            } catch (IllegalArgumentException ignored) {
            }
            try {
                PacketAdapters.deregisterOutbound(this.packetFilter);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public abstract void register();
}
