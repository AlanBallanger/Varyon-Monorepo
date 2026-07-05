package fr.varyon.travelingcamera;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.event.events.player.RemovedPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public final class VaryonTravelingCameraPlugin extends JavaPlugin {

    private CameraPathStore pathStore;
    private PacketFilter movementBoostPacketFilter;

    public VaryonTravelingCameraPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        pathStore = new CameraPathStore(getDataDirectory());
        pathStore.initialize();
        TravelingCameraManager.bind(pathStore);

        tryRegisterTickSystem("fr.varyon.travelingcamera.CameraPlaybackTickSystem");

        getEventRegistry().registerGlobal(RemovedPlayerFromWorldEvent.class, event -> {
            PlayerRef ref = event.getHolder().getComponent(PlayerRef.getComponentType());
            if (ref != null) {
                TravelingCameraManager.stop(ref.getUuid(), null);
                TravelingCameraManager.cancelRecording(ref.getUuid());
                TravelingCameraManager.stopVisualization(ref.getUuid(), null);
                TravelCamBoostHintHud.cleanup(ref.getUuid());
            }
        });

        movementBoostPacketFilter = PacketAdapters.registerInbound(new TravelCamMovementBoostFilter());
    }

    @Override
    protected void start() {
        try {
            getCommandRegistry().registerCommand(new TravelCamCommand());
            getCommandRegistry().registerCommand(new TravelCamPlayForCommand());
            getCommandRegistry().registerCommand(new TravelCamStopForCommand());
            System.out.println("[VaryonTravelingCamera] Commandes /travelcam, /travelcamplay, /travelcamstop enregistrees.");
        } catch (Throwable t) {
            System.err.println("[VaryonTravelingCamera] Echec enregistrement commande: " + t.getMessage());
            t.printStackTrace();
        }
    }

    @Override
    protected void shutdown() {
        if (movementBoostPacketFilter != null) {
            PacketAdapters.deregisterInbound(movementBoostPacketFilter);
            movementBoostPacketFilter = null;
        }
    }

    private void tryRegisterTickSystem(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            Object system = clazz.getDeclaredConstructor().newInstance();
            Object registry = this.getEntityStoreRegistry();
            java.lang.reflect.Method register = null;
            for (java.lang.reflect.Method method : registry.getClass().getMethods()) {
                if ("registerSystem".equals(method.getName()) && method.getParameterCount() == 1) {
                    register = method;
                    break;
                }
            }
            if (register == null) {
                System.out.println("[VaryonTravelingCamera] registerSystem(...) introuvable.");
                return;
            }
            register.invoke(registry, system);
            System.out.println("[VaryonTravelingCamera] Systeme enregistre: " + className);
        } catch (Throwable t) {
            System.err.println("[VaryonTravelingCamera] Echec enregistrement systeme: " + t.getMessage());
            t.printStackTrace();
        }
    }
}
