package com.varyon.logs;

import com.hypixel.hytale.logger.backend.HytaleConsole;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.util.logging.Level;

public class VaryonLogsPlugin extends JavaPlugin {

    private OutputStreamWriter originalSout;
    private OutputStreamWriter originalSerr;

    public VaryonLogsPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();
        if (LogFilters.PATTERNS.length == 0) {
            getLogger().at(Level.INFO).log("[Varyon-Logs] No filter rules defined — nothing to install.");
            return;
        }
        try {
            Field soutField = HytaleConsole.class.getDeclaredField("soutwriter");
            Field serrField = HytaleConsole.class.getDeclaredField("serrwriter");
            soutField.setAccessible(true);
            serrField.setAccessible(true);

            originalSout = (OutputStreamWriter) soutField.get(HytaleConsole.INSTANCE);
            originalSerr = (OutputStreamWriter) serrField.get(HytaleConsole.INSTANCE);

            if (originalSout != null) soutField.set(HytaleConsole.INSTANCE, new FilteringWriter(originalSout));
            if (originalSerr != null) serrField.set(HytaleConsole.INSTANCE, new FilteringWriter(originalSerr));

            getLogger().at(Level.INFO).log("[Varyon-Logs] Installed " + LogFilters.PATTERNS.length + " filter rule(s).");
        } catch (Exception e) {
            getLogger().at(Level.SEVERE).log("[Varyon-Logs] Failed to install filter: " + e);
        }
    }

    @Override
    protected void shutdown() {
        if (originalSout == null && originalSerr == null) return;
        try {
            Field soutField = HytaleConsole.class.getDeclaredField("soutwriter");
            Field serrField = HytaleConsole.class.getDeclaredField("serrwriter");
            soutField.setAccessible(true);
            serrField.setAccessible(true);
            if (originalSout != null) soutField.set(HytaleConsole.INSTANCE, originalSout);
            if (originalSerr != null) serrField.set(HytaleConsole.INSTANCE, originalSerr);
        } catch (Exception ignored) {}
    }
}
