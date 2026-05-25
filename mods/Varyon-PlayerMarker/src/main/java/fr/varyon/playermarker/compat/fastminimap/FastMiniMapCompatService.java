package fr.varyon.playermarker;

import org.joml.Vector3d;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

final class FastMiniMapCompatService {

    private static final String API_CLASS = "dev.thenexusgates.fastminimap.FastMiniMapPlayerLayerApi";

    private final Method setProviderMethod;
    private final Constructor<?> playerDotCtor;
    private final Class<?> providerFnClass;

    FastMiniMapCompatService() {
        Method sp = null;
        Constructor<?> dotCtor = null;
        Class<?> fnClass = null;
        try {
            Class<?> api = Class.forName(API_CLASS, false, FastMiniMapCompatService.class.getClassLoader());
            Class<?> dotClass = Class.forName(API_CLASS + "$PlayerDot", false, FastMiniMapCompatService.class.getClassLoader());
            dotCtor = dotClass.getConstructor(double.class, double.class, BufferedImage.class, String.class);
            for (Method m : api.getMethods()) {
                if ("setProvider".equals(m.getName()) && m.getParameterCount() == 1) {
                    sp = m;
                    fnClass = m.getParameterTypes()[0];
                    break;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        this.setProviderMethod = sp;
        this.playerDotCtor = dotCtor;
        this.providerFnClass = fnClass;
    }

    void register() {
        if (setProviderMethod == null || providerFnClass == null) {
            return;
        }
        try {
            Object callback = Proxy.newProxyInstance(
                    providerFnClass.getClassLoader(),
                    new Class<?>[] { providerFnClass },
                    this::invokeProviderProxy);
            setProviderMethod.invoke(null, callback);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    void unregister() {
        if (setProviderMethod == null) {
            return;
        }
        try {
            setProviderMethod.invoke(null, new Object[] { null });
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private Object invokeProviderProxy(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            String name = method.getName();
            if ("equals".equals(name) && args != null && args.length == 1) {
                return proxy == args[0];
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("toString".equals(name)) {
                return "VaryonPlayerMarkerFastMiniMapProvider";
            }
        }

        if (args == null || args.length < 5) {
            return List.of();
        }

        String worldName = (String) args[0];
        UUID viewerUuid = (UUID) args[1];
        double viewerX = ((Number) args[2]).doubleValue();
        double viewerZ = ((Number) args[3]).doubleValue();
        int radiusBlocks = ((Number) args[4]).intValue();

        return buildDots(worldName, viewerUuid, viewerX, viewerZ, radiusBlocks);
    }

    private List<Object> buildDots(
            String worldName,
            UUID viewerUuid,
            double viewerX,
            double viewerZ,
            int radiusBlocks) throws Exception {

        if (playerDotCtor == null) {
            return List.of();
        }

        VaryonPlayerMarkerPlugin plugin = VaryonPlayerMarkerPlugin.getInstance();
        if (plugin != null && !plugin.resolvePlayerSettings(viewerUuid).isEnabled(PlayerMarkerSurface.MINIMAP)) {
            return List.of();
        }

        Universe universe = Universe.get();
        if (universe == null) {
            return List.of();
        }

        World world = null;
        for (World w : universe.getWorlds().values()) {
            if (w != null && w.isAlive() && worldName.equals(w.getName())) {
                world = w;
                break;
            }
        }
        if (world == null) {
            return List.of();
        }

        PlayerMarkerPlayerSettings viewerSettings =
                plugin != null ? plugin.resolvePlayerSettings(viewerUuid) : new PlayerMarkerPlayerSettings();
        if (!viewerSettings.isEnabled(PlayerMarkerSurface.MINIMAP)) {
            return List.of();
        }
        PlayerRef viewerRef = plugin != null ? plugin.getActivePlayerRef(viewerUuid) : null;

        Collection<PlayerRef> playerRefs = world.getPlayerRefs();
        if (playerRefs == null || playerRefs.isEmpty()) {
            return List.of();
        }

        PlayerMarkerVisibilityBatch visibilityBatch =
                PlayerMarkerVisibilityBatch.build(playerRefs, viewerRef, viewerUuid);

        double radiusSq =
                radiusBlocks <= 0 ? Double.POSITIVE_INFINITY : (double) radiusBlocks * radiusBlocks;

        PlayerMarkerConfig config = VaryonPlayerMarkerPlugin.getConfig();
        boolean showNickname = config == null || config.showNickname;

        List<Object> dots = new ArrayList<>();
        for (PlayerRef ref : playerRefs) {
            try {
                if (ref == null) {
                    continue;
                }

                UUID uuid = ref.getUuid();
                if (uuid == null || uuid.equals(viewerUuid)) {
                    continue;
                }

                PlayerMarkerVisibilityDecision visibility =
                        PlayerMarkerVisibilityService.resolve(
                                viewerRef, viewerUuid, uuid, false, false, visibilityBatch);
                PlayerMarkerVisibilityState visibilityState = visibility.state();

                boolean surfaceEnabled = viewerSettings.isEnabledFor(PlayerMarkerSurface.MINIMAP, viewerUuid, uuid);
                if (!surfaceEnabled) {
                    continue;
                }

                if (!visibility.isVisible()) {
                    continue;
                }

                Vector3d pos = PlayerMarkerLiveTracker.resolvePosition(ref);
                if (pos == null) {
                    continue;
                }

                double dx = pos.x - viewerX;
                double dz = pos.z - viewerZ;
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }

                String username = PlayerMarkerPlayerNames.resolve(ref);

                BufferedImage icon = resolveIcon(uuid, username, visibility.isGhosted());
                String label =
                        showNickname
                                ? PlayerMarkerVisuals.decorateLabel(username, visibilityState, viewerRef)
                                : null;
                dots.add(playerDotCtor.newInstance(pos.x, pos.z, icon, label));
            } catch (Exception ignored) {
            }
        }
        return dots;
    }

    private BufferedImage resolveIcon(UUID uuid, String username, boolean ghosted) {
        VaryonPlayerMarkerPlugin plugin = VaryonPlayerMarkerPlugin.getInstance();
        if (plugin == null || plugin.getAvatarService() == null) {
            return null;
        }
        return plugin.getAvatarService().resolveMinimapIcon(uuid, username, ghosted);
    }
}
