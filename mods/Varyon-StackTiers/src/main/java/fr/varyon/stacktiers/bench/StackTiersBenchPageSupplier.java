package fr.varyon.stacktiers.bench;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction.CustomPageSupplier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import fr.varyon.stacktiers.research.ResearchManager;
import fr.varyon.stacktiers.ui.ResearchTreeUI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Ouvre l'arbre de recherche à l'interaction avec l'établi Stacks (voir
 * Server/Item/Items/Bench/Stacks_CraftingBench.json, Interactions.Use -> "Type": "OpenCustomUI",
 * "Page": {"Id": "StackTiersBench"}). Même pattern que Varyon-ExtendedTeleporters
 * (TeleporterSettingsPageSupplier) : un CustomPageSupplier enregistré sous un Id, résolu par le
 * moteur au clic sur le bloc.
 *
 * Contrairement au téléporteur, l'établi ne porte aucun état propre par instance placée dans le
 * monde — n'importe quel établi ouvre l'arbre de recherche PERSONNEL du joueur qui interagit
 * (identique à /varyonresearch). Pas besoin de résoudre context.getTargetBlock() : le bloc
 * cliqué n'a pas d'incidence sur la page ouverte.
 */
public final class StackTiersBenchPageSupplier implements CustomPageSupplier {
    public static final BuilderCodec<StackTiersBenchPageSupplier> CODEC =
            BuilderCodec.builder(StackTiersBenchPageSupplier.class, StackTiersBenchPageSupplier::new).build();

    @Nullable
    @Override
    public CustomUIPage tryCreate(@Nonnull Ref<EntityStore> ref, ComponentAccessor<EntityStore> componentAccessor,
                                   @Nonnull PlayerRef playerRef, @Nonnull InteractionContext context) {
        // Résolu paresseusement (pas au constructeur) : le codec peut être instancié pendant le
        // chargement des assets, potentiellement avant que VaryonStackTiersPlugin.setup() ait
        // tourné et initialisé le ResearchManager.
        ResearchManager researchManager = fr.varyon.stacktiers.VaryonStackTiersPlugin.getInstance().getResearchManager();
        return new ResearchTreeUI(playerRef, researchManager);
    }
}
