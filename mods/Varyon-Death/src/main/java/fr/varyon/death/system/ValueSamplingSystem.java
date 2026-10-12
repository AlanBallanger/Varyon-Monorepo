package fr.varyon.death.systeme;

import java.util.Set;


import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.OrderPriority;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import fr.varyon.death.combat.SondeValeurs;

/**
 * Releve le montant des degats juste apres toutes les reductions, avant qu'il ne soit consomme.
 *
 * <p>Deux systemes remettent ensuite ce montant a zero : {@code ApplyDamage} annule le coup
 * fatal, et les mods de degats flottants appellent {@code setAmount(0)} apres affichage. Sans
 * ce releve, la part mitigee serait irrecuperable et la colonne « degats mitiges » afficherait
 * toujours zero.
 *
 * <p>Le systeme vit dans {@code FilterDamage} et non {@code InspectDamage} : {@code ApplyDamage}
 * declare {@code BEFORE InspectDamage}, donc aucun systeme de ce groupe ne s'execute avant lui.
 * Il se place en revanche apres la reduction d'armure du serveur et, si Varyon-RPG est present,
 * apres sa reduction de classe.
 */
public final class SystemeReleveValeurs extends DamageEventSystem {

    private final Set<Dependency<EntityStore>> dependances;

    public SystemeReleveValeurs() {
        this.dependances = construireDependances();
    }

    private static Set<Dependency<EntityStore>> construireDependances() {
        // Une seule dependance, vers un systeme du serveur toujours present.
        //
        // Nommer un systeme d'un mod tiers (la reduction de classe de Varyon-RPG, par exemple)
        // serait fatal : le graphe exige que la classe citee soit DEJA enregistree, or l'ordre
        // de chargement des plugins n'est pas garanti. Une classe visible mais pas encore
        // enregistree fait echouer la validation et tomber le plugin entier.
        //
        // FURTHEST place le releve le plus loin possible apres la reduction d'armure, donc en
        // fin de groupe : il voit ainsi les reductions ajoutees par les autres mods sans jamais
        // les nommer.
        return Set.of(new SystemDependency<>(
                Order.AFTER, DamageSystems.ArmorDamageReduction.class, OrderPriority.FURTHEST));
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependances;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void handle(int index,
                       ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store,
                       CommandBuffer<EntityStore> tampon,
                       Damage degats) {
        if (degats == null) {
            return;
        }
        SondeValeurs.enregistrer(degats, degats.getAmount());
    }
}
