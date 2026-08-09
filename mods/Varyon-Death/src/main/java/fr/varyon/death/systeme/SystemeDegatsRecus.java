package fr.varyon.death.systeme;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import javax.annotation.Nullable;

import com.hypixel.hytale.logger.HytaleLogger;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import fr.varyon.death.combat.SuiviCombat;
import fr.varyon.death.combat.SondeValeurs;
import fr.varyon.death.config.Diagnostic;
import fr.varyon.death.config.PreferencesRecap;

/**
 * Intercepte chaque degat inflige a un joueur et l'ajoute a sa session de combat.
 *
 * <p>Le systeme s'execute APRES le groupe de filtrage des degats, de sorte que
 * {@link Damage#getAmount()} reflete deja les reductions d'armure et de resistance :
 * la difference avec {@link Damage#getInitialAmount()} donne la part mitigee.
 * Ce systeme observe uniquement, il ne modifie jamais l'evenement.
 */
public final class SystemeDegatsRecus extends DamageEventSystem {

    private static final HytaleLogger LOGGER =
            HytaleLogger.getLogger().getSubLogger("VaryonDeathRecap-Damage");

    private final Set<Dependency<EntityStore>> dependencies;

    public SystemeDegatsRecus() {
        this.dependencies = buildDependencies();
    }

    /**
     * Ordonnancement critique. Le systeme doit s'executer :
     * <ul>
     *   <li>APRES le groupe {@code FilterDamage}, pour que {@code getAmount()} tienne compte
     *       de l'armure et des reductions de classe (Varyon-RPG y applique ses mitigations) ;</li>
     *   <li>AVANT {@code DamageSystems.EntityUIEvents}, car les mods d'affichage de degats
     *       (Varyon-Damage_Number) s'ancrent la et appellent {@code setAmount(0)} apres avoir
     *       affiche leur nombre flottant. Sans cette contrainte, on ne lit plus que des zeros
     *       et tous les degats basculent a tort dans la colonne « mitiges ».</li>
     * </ul>
     */
    private static Set<Dependency<EntityStore>> buildDependencies() {
        try {
            DamageModule damageModule = DamageModule.get();
            if (damageModule != null) {
                return Set.of(
                        new SystemGroupDependency<>(Order.AFTER, damageModule.getFilterDamageGroup()),
                        new SystemDependency<>(Order.AFTER, DamageSystems.ApplyDamage.class),
                        new SystemDependency<>(Order.BEFORE, DamageSystems.EntityUIEvents.class));
            }
        } catch (Throwable ignored) {
            // Le module peut ne pas etre pret pendant l'init du plugin.
        }
        return Set.of(new SystemDependency<>(Order.BEFORE, DamageSystems.EntityUIEvents.class));
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getInspectDamageGroup();
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void handle(int index,
                       ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store,
                       CommandBuffer<EntityStore> commandBuffer,
                       Damage damage) {
        if (damage == null || !SuiviCombat.estActif()) {
            return;
        }
        float rawAmount = damage.getAmount();
        float initialAmount = damage.getInitialAmount();
        // Les soins arrivent en montant negatif : ils ne concernent pas le recapitulatif.
        if (rawAmount <= 0f && initialAmount <= 0f) {
            return;
        }

        // Valeur de reference : le montant releve par la sonde juste apres l'armure, seul
        // moment ou il est encore exact. A ce stade, ApplyDamage a annule le coup fatal et les
        // mods de degats flottants ont pu appeler setAmount(0) : lire getAmount() ici donnerait
        // zero et basculerait tout a tort dans la colonne « mitiges ».
        Float probed = SondeValeurs.consommer(damage);
        float finalAmount;
        boolean consumedByAnotherSystem = rawAmount <= 0f && initialAmount > 0f;
        if (probed != null) {
            finalAmount = probed;
        } else {
            // Repli si la sonde n'a pas tourne : degats justes, mitigation perdue.
            finalAmount = consumedByAnotherSystem ? initialAmount : rawAmount;
        }

        Ref<EntityStore> defenderRef = chunk.getReferenceTo(index);
        PlayerRef playerRef = resolvePlayerRef(store, commandBuffer, defenderRef);
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        UUID defenderUuid = playerRef.getUuid();
        if (defenderUuid == null || !PreferencesRecap.estActif(defenderUuid)) {
            return;
        }

        if (Diagnostic.estActif()) {
            LOGGER.at(Level.INFO).log(
                    "degats recus: brut=%.2f initial=%.2f sonde=%s retenu=%.2f mitige=%.2f source=%s",
                    rawAmount, initialAmount, probed, finalAmount,
                    Math.max(0f, initialAmount - finalAmount),
                    damage.getSource() == null ? "null" : damage.getSource().getClass().getSimpleName());
        }

        SuiviCombat.get().enregistrerDegats(
                defenderUuid,
                damage,
                resolveAttackerRef(damage),
                store,
                commandBuffer,
                Math.max(0f, initialAmount),
                Math.max(0f, finalAmount));
    }

    @Nullable
    private static PlayerRef resolvePlayerRef(Store<EntityStore> store,
                                              CommandBuffer<EntityStore> commandBuffer,
                                              Ref<EntityStore> ref) {
        try {
            PlayerRef fromBuffer = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
            if (fromBuffer != null) {
                return fromBuffer;
            }
            return store.getComponent(ref, PlayerRef.getComponentType());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /**
     * Pour un projectile, on credite le tireur et non la fleche : {@code getRef()} de
     * {@link Damage.ProjectileSource} designe deja l'entite d'origine.
     */
    @Nullable
    private static Ref<EntityStore> resolveAttackerRef(Damage damage) {
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return null;
        }
        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) {
            return null;
        }
        return attackerRef;
    }
}
