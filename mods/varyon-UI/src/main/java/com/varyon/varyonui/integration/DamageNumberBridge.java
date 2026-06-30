package com.varyon.varyonui.integration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DamageNumberBridge {

    private static final Logger LOG = Logger.getLogger("VaryonUI");

    private static final String SETTINGS_CLASS = "fr.varyon.damagenumber.DamageNumberDisplaySettings";

    private static Boolean available = null;

    private DamageNumberBridge() {}

    public static boolean isAvailable() {
        if (available == null) {
            try {
                Class.forName(SETTINGS_CLASS);
                available = true;
            } catch (ClassNotFoundException e) {
                available = false;
            }
        }
        return Boolean.TRUE.equals(available);
    }

    public static void ensureLoaded(@Nullable UUID uuid) {
        if (!isAvailable() || uuid == null) {
            return;
        }
        try {
            Class<?> c = Class.forName(SETTINGS_CLASS);
            Method m = c.getMethod("ensureLoaded", UUID.class);
            m.invoke(null, uuid);
        } catch (Throwable t) {
            LOG.log(Level.FINE, "[DamageNumberBridge] ensureLoaded failed uuid=" + uuid, t);
        }
    }

    public static boolean isEnabled(@Nullable UUID uuid) {
        if (uuid == null) {
            return true;
        }
        if (!isAvailable()) {
            return true;
        }
        ensureLoaded(uuid);
        try {
            Class<?> c = Class.forName(SETTINGS_CLASS);
            Method m = c.getMethod("isEnabled", UUID.class);
            Object result = m.invoke(null, uuid);
            return result instanceof Boolean b && b;
        } catch (Throwable t) {
            LOG.log(Level.FINE, "[DamageNumberBridge] isEnabled failed uuid=" + uuid, t);
            return true;
        }
    }
}
