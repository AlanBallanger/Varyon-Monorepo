package fr.varyon.extendedteleporters;

import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent;
import com.hypixel.hytale.server.core.universe.world.events.AllWorldsLoadedEvent;
import com.hypixel.hytale.server.core.universe.world.events.RemoveWorldEvent;
import com.hypixel.hytale.server.core.util.Config;
import fr.varyon.extendedteleporters.commands.TeleporterCommand;
import fr.varyon.extendedteleporters.config.ExtendedTeleportConfig;
import fr.varyon.extendedteleporters.gui.TeleporterSettingsPageSupplier;
import fr.varyon.extendedteleporters.i18n.Translations;
import fr.varyon.extendedteleporters.interaction.ExtendedTeleporterInteraction;
import fr.varyon.extendedteleporters.interaction.UnlimitedPlacementConditionInteraction;
import fr.varyon.extendedteleporters.system.TeleporterBreakBlockEventSystem;
import fr.varyon.extendedteleporters.system.TeleporterComponentRemovalSystem;
import fr.varyon.extendedteleporters.system.TeleporterPlaceBlockEventSystem;
import fr.varyon.extendedteleporters.system.TeleporterRestrictionTickingSystem;
import fr.varyon.extendedteleporters.system.TeleporterSelfDestructTickingSystem;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class VaryonExtendedTeleportersPlugin extends JavaPlugin {
   private static VaryonExtendedTeleportersPlugin instance;
   public static Config<ExtendedTeleportConfig> CONFIG;

   public VaryonExtendedTeleportersPlugin(@NonNullDecl JavaPluginInit init) {
      super(init);
      instance = this;
      CONFIG = this.withConfig("VaryonExtendedTeleporters", ExtendedTeleportConfig.CODEC);
   }

   public static VaryonExtendedTeleportersPlugin get() {
      return instance;
   }

   protected void setup() {
      super.setup();
      this.getLogger().at(Level.INFO).log("ExtendedTeleporters - Initializing...");
      Translations.init();
      Translations.setLanguage(CONFIG.get().getLanguage());
      this.getLogger().at(Level.INFO).log("ExtendedTeleporters - Translation system initialized (language: " + CONFIG.get().getLanguage() + ")");
      CONFIG.save();
      TeleporterManager manager = TeleporterManager.getInstance();
      TeleporterRestrictionTickingSystem restrictionSystem = new TeleporterRestrictionTickingSystem();
      this.getEntityStoreRegistry().registerSystem(restrictionSystem);
      manager.setRestrictionSystem(restrictionSystem);
      this.getEntityStoreRegistry().registerSystem(new TeleporterPlaceBlockEventSystem());
      this.getEntityStoreRegistry().registerSystem(new TeleporterBreakBlockEventSystem());
      this.getEntityStoreRegistry().registerSystem(new TeleporterSelfDestructTickingSystem());
      // TODO: ChunkStore system registration requires Teleporter component type to be initialized first.
      //       Registering via getChunkStoreRegistry() fails because the query from Teleporter.getComponentType()
      //       is not yet available during setup. For now, block removal cleanup is handled by other mechanisms.
      // this.getChunkStoreRegistry().registerSystem(new TeleporterComponentRemovalSystem());
      this.getCommandRegistry().registerCommand(new TeleporterCommand());
      this.getCodecRegistry(Interaction.CODEC)
         .register("PlacementCountCondition", UnlimitedPlacementConditionInteraction.class, UnlimitedPlacementConditionInteraction.CODEC);
      this.getLogger().at(Level.INFO).log("ExtendedTeleporters - Overrode PlacementCountCondition (unlimited teleporter placement)");
      this.getCodecRegistry(Interaction.CODEC).register("Teleporter", ExtendedTeleporterInteraction.class, ExtendedTeleporterInteraction.CODEC);
      this.getCodecRegistry(OpenCustomUIInteraction.PAGE_CODEC)
         .register("Teleporter", TeleporterSettingsPageSupplier.class, TeleporterSettingsPageSupplier.CODEC);
      this.getLogger().at(Level.INFO).log("ExtendedTeleporters - Overrode native teleporter interactions");
      this.getEventRegistry().registerGlobal(AddWorldEvent.class, event -> {
         World world = event.getWorld();
         manager.addWorld(world);
         this.getLogger().at(Level.INFO).log("ExtendedTeleporters - Applied to world: " + world.getName());
      });
      this.getEventRegistry().registerGlobal(RemoveWorldEvent.class, event -> manager.removeWorld(event.getWorld().getName()));
      this.getEventRegistry().registerGlobal(AllWorldsLoadedEvent.class, event -> {
         manager.initializePermissionProvider();
         manager.ensureAllTeleporterWarpsExist();
         manager.syncPrivateWarpsWithRegistry();
         manager.registerAllCustomDestinationsAsWarps();
         this.getLogger().at(Level.INFO).log("ExtendedTeleporters - Permission provider: " + manager.getPermissionProvider().getName());
         this.getLogger().at(Level.INFO).log("ExtendedTeleporters - Synced private warps with game registry");
      });
      this.getLogger().at(Level.INFO).log("ExtendedTeleporters - Teleporter block placement limit: " + manager.getNewLimit());
      this.getLogger().at(Level.INFO).log("ExtendedTeleporters - Features: Private warps, Proximity-based restrictions");
      this.getLogger().at(Level.INFO).log("ExtendedTeleporters - Successfully initialized!");
   }

   protected void shutdown() {
      TeleporterManager.getInstance().shutdown();
      this.getLogger().at(Level.INFO).log("ExtendedTeleporters - Shutdown complete");
   }
}
