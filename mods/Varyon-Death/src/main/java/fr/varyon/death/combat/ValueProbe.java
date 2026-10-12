package fr.varyon.death.combat;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.server.core.modules.entity.damage.Damage;

/**
 * Memorise le montant reel des degats juste apres application de l'armure.
 *
 * <p>Necessaire car deux systemes ecrasent {@code Damage.getAmount()} avant que le
 * recapitulatif ne puisse le lire : {@code ApplyDamage} annule le coup fatal, et les mods
 * d'affichage de degats flottants appellent {@code setAmount(0)}. Sans cette sonde, la part
 * mitigee par l'armure serait irrecuperable et afficherait toujours zero.
 *
 * <p>Les entrees sont indexees par instance de {@link Damage} dans une {@link WeakHashMap} :
 * un evenement non consomme est recupere par le GC sans fuite memoire.
 */
public final class SondeValeurs {

    private static final Map<Damage, Float> POST_ARMOR_AMOUNT = new WeakHashMap<>();
    private static final ReentrantLock LOCK = new ReentrantLock();

    private SondeValeurs() {}

    /** Enregistre le montant observe apres reduction d'armure, avant toute consommation. */
    public static void enregistrer(@Nonnull Damage damage, float amount) {
        LOCK.lock();
        try {
            POST_ARMOR_AMOUNT.put(damage, amount);
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * @return le montant post-armure releve pour cet evenement, ou {@code null} si la sonde
     *         n'a pas pu s'executer (module de degats indisponible au demarrage).
     */
    @Nullable
    public static Float consommer(@Nonnull Damage damage) {
        LOCK.lock();
        try {
            return POST_ARMOR_AMOUNT.remove(damage);
        } finally {
            LOCK.unlock();
        }
    }
}
