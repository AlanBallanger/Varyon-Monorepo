package fr.varyon.playermarker;

import java.util.UUID;

final class PlayerMarkerEssentialsPlusVanishProvider implements PlayerMarkerVanishProvider {

    private static final String PLUGIN_CLASS = "de.fof1092.essentialsplus.EssentialsPlus";
    private static final String PLUGIN_IDENTIFIER = "fof1092:EssentialsPlus";
    private static final String PLUGIN_GETTER = "getPlugin";
    private static final String VANISH_MANAGER_CLASS = "de.fof1092.essentialsplus.features.player.status.vanish.VanishManager";

    @Override
    public String name() {
        return "EssentialsPlus";
    }

    @Override
    public boolean isAvailable() {
        try {
            return PlayerMarkerVanishReflection.resolveEnabledPluginInstance(PLUGIN_CLASS, PLUGIN_IDENTIFIER, PLUGIN_GETTER) != null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    @Override
    public boolean isVanished(UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }

        try {
            Object plugin = PlayerMarkerVanishReflection.resolveEnabledPluginInstance(PLUGIN_CLASS, PLUGIN_IDENTIFIER, PLUGIN_GETTER);
            if (plugin == null) {
                return false;
            }

            Class<?> vanishManagerClass = PlayerMarkerVanishReflection.loadClass(
                    VANISH_MANAGER_CLASS,
                    plugin.getClass().getClassLoader());
            if (vanishManagerClass == null) {
                return false;
            }

            Object vanishManager = PlayerMarkerVanishReflection.invokeStaticNoArgs(vanishManagerClass, "getInstance");
            return vanishManager != null
                    && PlayerMarkerVanishReflection.invokeBoolean(vanishManagerClass, vanishManager, "isVanished", UUID.class, playerUuid);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }
}

