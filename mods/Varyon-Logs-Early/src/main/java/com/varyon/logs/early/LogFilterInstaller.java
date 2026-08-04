package com.varyon.logs.early;

import com.hypixel.hytale.plugin.early.ClassTransformer;

import java.io.OutputStreamWriter;
import java.lang.reflect.Field;

public class LogFilterInstaller implements ClassTransformer {

    private static volatile boolean installed = false;

    public LogFilterInstaller() {
        install();
    }

    private static synchronized void install() {
        if (installed) return;
        installed = true;
        try {
            Class<?> consoleClass = Class.forName(
                "com.hypixel.hytale.logger.backend.HytaleConsole", true, LogFilterInstaller.class.getClassLoader());

            Field instanceField = consoleClass.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            Object instance = instanceField.get(null);

            Field soutField = consoleClass.getDeclaredField("soutwriter");
            Field serrField = consoleClass.getDeclaredField("serrwriter");
            soutField.setAccessible(true);
            serrField.setAccessible(true);

            OutputStreamWriter originalSout = (OutputStreamWriter) soutField.get(instance);
            OutputStreamWriter originalSerr = (OutputStreamWriter) serrField.get(instance);

            if (originalSout != null) soutField.set(instance, new FilteringWriter(originalSout));
            if (originalSerr != null) serrField.set(instance, new FilteringWriter(originalSerr));
        } catch (Exception ignored) {
        }
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public byte[] transform(String name, String path, byte[] bytes) {
        return bytes;
    }
}
