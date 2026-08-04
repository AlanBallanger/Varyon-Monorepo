package com.varyon.tptoworld;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.varyon.tptoworld.portal.VaryonPortalCommandBlock;
import com.varyon.tptoworld.portal.VaryonPortalEditInteraction;
import com.varyon.tptoworld.portal.VaryonPortalSpawnInteraction;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public final class TpToWorldPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public TpToWorldPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        try {
            getCommandRegistry().registerCommand(new TpToWorldCommand());
            getCommandRegistry().registerCommand(new PortalEditCommand());
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-TpToWorld] Échec enregistrement des commandes");
        }

        try {
            getCodecRegistry(Interaction.CODEC).register("Varyon_Portal_Spawn",
                    VaryonPortalSpawnInteraction.class, VaryonPortalSpawnInteraction.CODEC);
            getCodecRegistry(Interaction.CODEC).register("Varyon_Portal_Edit",
                    VaryonPortalEditInteraction.class, VaryonPortalEditInteraction.CODEC);
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-TpToWorld] Échec enregistrement des interactions de portail");
        }

        try {
            ComponentType<ChunkStore, VaryonPortalCommandBlock> commandBlockType =
                    getChunkStoreRegistry().registerComponent(
                            VaryonPortalCommandBlock.class,
                            "VaryonPortalCommandBlock",
                            VaryonPortalCommandBlock.CODEC
                    );
            VaryonPortalCommandBlock.setComponentType(commandBlockType);
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-TpToWorld] Échec enregistrement du composant VaryonPortalCommandBlock");
        }
    }
}
