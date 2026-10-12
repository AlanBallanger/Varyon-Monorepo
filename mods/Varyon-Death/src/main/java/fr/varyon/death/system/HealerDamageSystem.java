package fr.varyon.death.systeme;

import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import fr.varyon.death.etat.GestionnaireATerre;

/**
 * Annule le relevement d'un soigneur qui vient d'etre touche.
 *
 * <p>Le systeme observe seulement : il ne modifie jamais les degats. Il s'execute apres
 * {@code ApplyDamage} pour ne reagir qu'aux coups reellement encaisses — un degat annule
 * en amont par un autre mod ne doit pas interrompre le relevement.
 */
public final class SystemeDegatsSoigneur extends DamageEventSystem {

    private final GestionnaireATerre gestionnaire;
    private final SystemeReleve systemeReleve;

    public SystemeDegatsSoigneur(@Nonnull GestionnaireATerre gestionnaire,
                                 @Nonnull SystemeReleve systemeReleve) {
        this.gestionnaire = gestionnaire;
        this.systemeReleve = systemeReleve;
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getInspectDamageGroup();
    }

    /**
     * Aucune dependance : le groupe {@code InspectDamage} s'execute deja apres
     * {@code ApplyDamage}, qui declare lui-meme {@code BEFORE InspectDamage}. On ne reagit
     * donc qu'aux coups reellement encaisses, sans avoir a l'exiger — et redeclarer cette
     * contrainte formait un cycle qui empechait le serveur de charger le mod.
     */
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> tampon,
                       @Nullable Damage degats) {
        if (degats == null || degats.isCancelled() || degats.getAmount() <= 0f) {
            return;
        }
        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        UUID uuid = playerRef.getUuid();
        if (uuid == null || !gestionnaire.estSoigneur(uuid)) {
            return;
        }
        systemeReleve.annulerPourDegats(uuid, playerRef);
    }
}
