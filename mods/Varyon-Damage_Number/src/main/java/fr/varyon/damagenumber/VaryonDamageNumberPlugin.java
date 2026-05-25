package fr.varyon.damagenumber;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import irai.mod.DynamicFloatingDamageFormatter.DamageNumberConfig;
import irai.mod.DynamicFloatingDamageFormatter.DamageNumbers;

public final class VaryonDamageNumberPlugin extends JavaPlugin {

    public VaryonDamageNumberPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        try {
            DamageNumbers.applyConfig(new DamageNumberConfig());
            System.out.println("[VaryonDamageNumber] DamageNumberConfig appliquée depuis les defaults intégrés.");
        } catch (Throwable t) {
            System.err.println("[VaryonDamageNumber] Échec chargement DamageNumberConfig: " + t.getMessage());
            t.printStackTrace();
        }

        tryRegisterDamageSystem("irai.mod.reforge.Entity.Events.DamageNumberEST");
        tryRegisterDamageSystem("irai.mod.reforge.Entity.Events.HealingFloatTickSystem");
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
            System.out.println("[VaryonDamageNumber] Système enregistré: " + className);
        } catch (ClassNotFoundException e) {
            System.out.println("[VaryonDamageNumber] Adapter absent (" + className + ").");
        } catch (Throwable t) {
            System.err.println("[VaryonDamageNumber] Échec enregistrement: " + t.getMessage());
            t.printStackTrace();
        }
    }
}
