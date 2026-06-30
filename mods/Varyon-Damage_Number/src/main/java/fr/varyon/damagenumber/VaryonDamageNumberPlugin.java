package fr.varyon.damagenumber;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import irai.mod.DynamicFloatingDamageFormatter.DamageNumberConfig;
import irai.mod.DynamicFloatingDamageFormatter.DamageNumbers;

public final class VaryonDamageNumberPlugin extends JavaPlugin {

    private DamageNumberDisplaySettingsManager displaySettingsManager;

    public VaryonDamageNumberPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        displaySettingsManager = new DamageNumberDisplaySettingsManager(getDataDirectory());
        displaySettingsManager.initialize();
        DamageNumberDisplaySettings.bind(displaySettingsManager);

        try {
            DamageNumbers.applyConfig(new DamageNumberConfig());
            System.out.println("[VaryonDamageNumber] DamageNumberConfig chargee.");
        } catch (Throwable t) {
            System.err.println("[VaryonDamageNumber] Echec chargement DamageNumberConfig: " + t.getMessage());
            t.printStackTrace();
        }

        tryRegisterDamageSystem("irai.mod.reforge.Entity.Events.DamageCritResetSystem");
        tryRegisterDamageSystem("irai.mod.reforge.Entity.Events.ImpactCriticalSanitizerSystem");
        tryRegisterDamageSystem("irai.mod.reforge.Entity.Events.DamageNumberEST");
        tryRegisterDamageSystem("irai.mod.reforge.Entity.Events.HealingFloatTickSystem");

        getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, event -> {
            PlayerRef ref = event.getHolder().getComponent(PlayerRef.getComponentType());
            if (ref != null) {
                DamageNumberDisplaySettings.ensureLoaded(ref.getUuid());
            }
        });
    }

    @Override
    protected void start() {
        registerDamageNumberCommands();
    }

    @Override
    protected void shutdown() {
        if (displaySettingsManager != null) {
            displaySettingsManager.flush();
        }
    }

    private void registerDamageNumberCommands() {
        try {
            getCommandRegistry().registerCommand(new DamageNumberCommand());
            System.out.println("[VaryonDamageNumber] Commande /dmgnum enregistree.");
        } catch (Throwable t) {
            System.err.println("[VaryonDamageNumber] Echec enregistrement commande: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private void tryRegisterDamageSystem(String className) {
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
                System.out.println("[VaryonDamageNumber] registerSystem(...) introuvable.");
                return;
            }
            register.invoke(registry, system);
            System.out.println("[VaryonDamageNumber] Systeme enregistre: " + className);
        } catch (ClassNotFoundException e) {
            System.out.println("[VaryonDamageNumber] Adapter absent (" + className + ").");
        } catch (Throwable t) {
            System.err.println("[VaryonDamageNumber] Echec enregistrement: " + t.getMessage());
            t.printStackTrace();
        }
    }
}
