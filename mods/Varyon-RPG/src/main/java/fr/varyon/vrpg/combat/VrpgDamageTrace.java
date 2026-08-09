package fr.varyon.vrpg.combat;

import com.hypixel.hytale.server.core.modules.entity.damage.Damage;

import javax.annotation.Nonnull;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Trace de debug (voir {@code debug_combat}) : accumule les etapes de reduction/bonus
 * appliquees a un meme evenement {@link Damage}, pour un log final consolide en une ligne.
 * Cle sur l'identite de l'objet Damage (un objet par coup, jamais reutilise).
 *
 * <p>Le premier systeme Varyon-RPG a toucher un Damage donne fixe la valeur "brute" —
 * elle peut deja inclure l'effet des enchantements SimpleEnchantments si ce mod s'execute
 * avant (aucune dependance d'ordre garantie entre les deux mods).
 */
public final class VrpgDamageTrace {

    private static final Map<Damage, VrpgDamageTrace> TRACES = new IdentityHashMap<>();

    private final float baseDamage;
    private final StringBuilder steps = new StringBuilder();

    private VrpgDamageTrace(float baseDamage) {
        this.baseDamage = baseDamage;
    }

    private static VrpgDamageTrace of(@Nonnull Damage damage, float fallbackBefore) {
        return TRACES.computeIfAbsent(damage, d -> new VrpgDamageTrace(fallbackBefore));
    }

    /**
     * Fixe la valeur "brute" de reference si aucune trace n'existe encore pour ce coup.
     * A appeler le plus tot possible dans le pipeline (voir {@link VrpgDamageProbeSystem}).
     * Sans effet si une trace existe deja (ne doit jamais ecraser une base deja posee).
     */
    public static void markBase(@Nonnull Damage damage, float amount) {
        of(damage, amount);
    }

    /**
     * @param label nom de l'etape (ex: "armure", "protection", "red dgt classe")
     * @param pctApplied pourcentage affiche a cote du label (ex: 66 pour "armure 66%")
     * @param before montant avant cette etape
     * @param after montant apres cette etape
     */
    public static void step(@Nonnull Damage damage, String label, float pctApplied, float before, float after) {
        VrpgDamageTrace trace = of(damage, before);
        if (Math.abs(after - before) < 0.001f) return;
        float factor = before != 0f ? after / before : 0f;
        trace.steps.append(String.format(" -> (%s %.0f%%) %.1f*%.2f = %.1f",
            label, pctApplied, before, factor, after));
    }

    public static void logFinal(@Nonnull Damage damage, com.hypixel.hytale.logger.HytaleLogger log) {
        VrpgDamageTrace trace = TRACES.remove(damage);
        if (trace == null) return;
        float finalAmount = damage.getAmount();
        log.atInfo().log(String.format("[VrpgDamage] base=%.1f%s -> final=%.1f (sans enchantement)",
            trace.baseDamage, trace.steps, finalAmount));
    }
}
