package fr.varyon.death.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Conserve le dernier recapitulatif de chaque joueur, pour la commande {@code /mort} qui
 * permet de le rouvrir si la page a ete fermee par megarde.
 */
public final class BanqueRecaps {

    private static final BanqueRecaps INSTANCE = new BanqueRecaps();
    private static volatile long staleAfterMs = 300_000L;

    private final Map<UUID, Entry> last = new ConcurrentHashMap<>();

    private BanqueRecaps() {}

    @Nonnull
    public static BanqueRecaps get() {
        return INSTANCE;
    }

    public static void applyConfig(long staleAfterMsIn) {
        staleAfterMs = Math.max(10_000L, staleAfterMsIn);
    }

    public void put(@Nonnull UUID playerUuid, @Nonnull SuiviCombat.Snapshot snapshot) {
        last.put(playerUuid, new Entry(snapshot, System.currentTimeMillis()));
        sweep();
    }

    /** Dernier recapitulatif, sans le consommer : {@code /mort} peut etre repete. */
    @Nullable
    public SuiviCombat.Snapshot peekLast(@Nonnull UUID playerUuid) {
        Entry entry = last.get(playerUuid);
        if (entry == null) {
            return null;
        }
        if (isStale(entry)) {
            last.remove(playerUuid);
            return null;
        }
        return entry.snapshot();
    }

    /** Rafraichit le dernier recapitulatif (reouverture differee apres fermeture accidentelle). */
    public void rebank(@Nonnull UUID playerUuid, @Nonnull SuiviCombat.Snapshot snapshot) {
        last.put(playerUuid, new Entry(snapshot, System.currentTimeMillis()));
    }

    public void dropAll(@Nonnull UUID playerUuid) {
        last.remove(playerUuid);
    }

    public int clearAll() {
        int size = last.size();
        last.clear();
        return size;
    }

    private static boolean isStale(@Nonnull Entry entry) {
        return System.currentTimeMillis() - entry.storedAtMs() > staleAfterMs;
    }

    private void sweep() {
        long now = System.currentTimeMillis();
        last.entrySet().removeIf(e -> now - e.getValue().storedAtMs() > staleAfterMs);
    }

    private record Entry(@Nonnull SuiviCombat.Snapshot snapshot, long storedAtMs) {}
}
