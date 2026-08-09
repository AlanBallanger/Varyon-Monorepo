package fr.varyon.death.systeme;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.builtin.npccombatactionevaluator.evaluator.CombatActionEvaluator;
import com.hypixel.hytale.builtin.npccombatactionevaluator.memory.TargetMemory;
import com.hypixel.hytale.builtin.npccombatactionevaluator.memory.TargetMemorySystems;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;

import fr.varyon.death.etat.GestionnaireATerre;

/**
 * Empeche les creatures de cibler un joueur a terre.
 *
 * <p>Un joueur a terre est invulnerable : laisser les mobs le frapper n'a aucun effet
 * mecanique, mais reste tres genant a l'ecran et empeche les allies de le relever.
 *
 * <p>Le ciblage repose sur l'<em>attitude</em> que chaque creature porte envers ses voisins.
 * Purger la memoire de ciblage ne suffit donc pas : le collecteur d'entites la reconstruit
 * au tick suivant a partir de cette attitude, et le mob revient aussitot. La seule methode
 * durable est de forcer l'attitude a {@link Attitude#IGNORE} via
 * {@link WorldSupport#overrideAttitude}. La surcharge est renouvelee a chaque tick tant que
 * le joueur reste a terre, puis expire d'elle-meme des qu'il est releve ou qu'il meurt.
 *
 * <p>La memoire et l'evaluateur de combat sont nettoyes en complement, pour que la creature
 * lache immediatement une cible deja acquise au lieu d'attendre la fin de son action.
 */
public final class SystemeDesaggro extends EntityTickingSystem<EntityStore> {

    private static final Query<EntityStore> QUERY = NPCEntity.getComponentType();

    /**
     * Duree de la surcharge d'attitude, en secondes. Volontairement courte : elle est
     * renouvelee a chaque tick, et doit cesser d'agir tres vite apres le relevement.
     */
    private static final double DUREE_IGNORE = 1.0d;

    private final GestionnaireATerre gestionnaire;

    public SystemeDesaggro(@Nonnull GestionnaireATerre gestionnaire) {
        this.gestionnaire = gestionnaire;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency<>(Order.AFTER, TargetMemorySystems.Ticking.class));
    }

    @Override
    public void tick(float delta,
                     int index,
                     @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> tampon) {
        // Rien a faire tant que personne n'est a terre : c'est le cas le plus frequent.
        if (gestionnaire.tousLesEtats().isEmpty()) {
            return;
        }
        NPCEntity npc = chunk.getComponent(index, NPCEntity.getComponentType());
        if (npc == null) {
            return;
        }

        forcerAttitudeIgnore(npc, tampon);

        TargetMemory memoire = chunk.getComponent(index, TargetMemory.getComponentType());
        if (memoire != null) {
            purgerHostilesConnus(memoire, tampon);
            oublierCiblePrincipale(memoire, tampon);
        }
        oublierCiblesDeCombat(chunk.getComponent(index, CombatActionEvaluator.getComponentType()), tampon);
    }

    /**
     * Force cette creature a ignorer tous les joueurs actuellement a terre.
     * C'est le mecanisme central : sans lui, le ciblage se reconstruit a chaque tick.
     */
    private void forcerAttitudeIgnore(@Nonnull NPCEntity npc,
                                      @Nonnull CommandBuffer<EntityStore> tampon) {
        WorldSupport support = supportDe(npc);
        if (support == null) {
            return;
        }
        for (UUID uuidATerre : gestionnaire.tousLesEtats().stream().map(e -> e.getUuidJoueur()).toList()) {
            Ref<EntityStore> refJoueur = referenceDuJoueur(uuidATerre);
            if (refJoueur == null) {
                continue;
            }
            try {
                support.overrideAttitude(refJoueur, Attitude.IGNORE, DUREE_IGNORE);
            } catch (RuntimeException ignore) {
                // La creature a pu etre retiree du monde entre-temps.
            }
        }
    }

    @Nullable
    private static WorldSupport supportDe(@Nonnull NPCEntity npc) {
        try {
            Role role = npc.getRole();
            return role == null ? null : role.getWorldSupport();
        } catch (RuntimeException ignore) {
            return null;
        }
    }

    @Nullable
    private static Ref<EntityStore> referenceDuJoueur(@Nullable UUID uuid) {
        PlayerRef playerRef = SystemeTickATerre.joueurEnLigne(uuid);
        if (playerRef == null) {
            return null;
        }
        try {
            Ref<EntityStore> ref = playerRef.getReference();
            return ref != null && ref.isValid() ? ref : null;
        } catch (RuntimeException ignore) {
            return null;
        }
    }

    /** Retire de la liste des hostiles connus tous les joueurs actuellement a terre. */
    private void purgerHostilesConnus(@Nonnull TargetMemory memoire,
                                      @Nonnull CommandBuffer<EntityStore> tampon) {
        List<Ref<EntityStore>> hostiles;
        try {
            hostiles = memoire.getKnownHostilesList();
        } catch (RuntimeException ignore) {
            return;
        }
        if (hostiles == null || hostiles.isEmpty()) {
            return;
        }
        for (Iterator<Ref<EntityStore>> it = hostiles.iterator(); it.hasNext(); ) {
            Ref<EntityStore> hostile = it.next();
            if (!estJoueurATerre(hostile, tampon)) {
                continue;
            }
            try {
                it.remove();
                memoire.getKnownHostiles().remove(hostile.getIndex());
            } catch (RuntimeException ignore) {
                // L'entite a pu etre retiree du monde entre-temps.
            }
        }
    }

    /** Efface la cible la plus proche si c'est un joueur a terre. */
    private void oublierCiblePrincipale(@Nonnull TargetMemory memoire,
                                        @Nonnull CommandBuffer<EntityStore> tampon) {
        try {
            if (estJoueurATerre(memoire.getClosestHostile(), tampon)) {
                memoire.setClosestHostile(null);
            }
        } catch (RuntimeException ignore) {
            // La reference peut etre devenue invalide.
        }
    }

    /** Efface les cibles retenues par l'evaluateur de combat si ce sont des joueurs a terre. */
    private void oublierCiblesDeCombat(@Nullable CombatActionEvaluator evaluateur,
                                       @Nonnull CommandBuffer<EntityStore> tampon) {
        if (evaluateur == null) {
            return;
        }
        try {
            // Une action deja engagee (currentAction) se deroule jusqu'a son terme meme si sa
            // cible est effacee ailleurs : c'est elle, et non la memoire de ciblage, qui pilote
            // les coups reellement portes. Sans l'interrompre explicitement, un mob dont
            // l'attaque a demarre juste avant la mise a terre continue de frapper.
            if (evaluateur.getCurrentAction() != null
                    && (estJoueurATerre(evaluateur.getBasicAttackTarget(), tampon)
                        || estJoueurATerre(evaluateur.getPrimaryTarget(), tampon))) {
                evaluateur.terminateCurrentAction();
            }
            if (estJoueurATerre(evaluateur.getBasicAttackTarget(), tampon)) {
                evaluateur.setBasicAttackTarget(null);
                evaluateur.clearCurrentBasicAttack();
            }
            if (estJoueurATerre(evaluateur.getPrimaryTarget(), tampon)) {
                evaluateur.clearPrimaryTarget();
                evaluateur.clearTimeout();
            }
        } catch (RuntimeException ignore) {
            // Les references peuvent devenir invalides en cours de tick.
        }
    }

    private boolean estJoueurATerre(@Nullable Ref<EntityStore> ref,
                                    @Nonnull CommandBuffer<EntityStore> tampon) {
        if (ref == null || !ref.isValid()) {
            return false;
        }
        try {
            PlayerRef playerRef = tampon.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) {
                return false;
            }
            UUID uuid = playerRef.getUuid();
            return uuid != null && gestionnaire.estATerre(uuid);
        } catch (RuntimeException ignore) {
            return false;
        }
    }
}
