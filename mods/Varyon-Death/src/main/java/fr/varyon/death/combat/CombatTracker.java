package fr.varyon.death.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import fr.varyon.death.compat.VaryonMobLevel;

/**
 * Accumule les degats subis par chaque joueur pendant une session de combat glissante.
 *
 * <p>Une session demarre au premier coup recu et expire apres {@code combatTimeoutMs} sans
 * activite, ce qui evite de melanger deux combats distincts dans un meme recapitulatif.
 * A la mort, la session est figee en {@link Snapshot} classe par degats decroissants.
 */
public final class SuiviCombat {

    private static final SuiviCombat INSTANCE = new SuiviCombat();

    private static volatile boolean enabled = true;
    private static volatile long combatTimeoutMs = 10_000L;
    private static volatile long maxCombatDurationMs = 60_000L;
    private static volatile int maxTrackedThreats = 20;
    private static volatile int nombreAffiche = 10;

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    private SuiviCombat() {}

    @Nonnull
    public static SuiviCombat get() {
        return INSTANCE;
    }

    public static boolean estActif() {
        return enabled;
    }

    public static long combatTimeoutMs() {
        return combatTimeoutMs;
    }

    public static int nombreAffiche() {
        return nombreAffiche;
    }

    public static void applyConfig(boolean enabledIn,
                                   long combatTimeoutMsIn,
                                   long maxCombatDurationMsIn,
                                   int maxTrackedThreatsIn,
                                   int topNDisplayedIn) {
        enabled = enabledIn;
        combatTimeoutMs = Math.max(1_000L, combatTimeoutMsIn);
        maxCombatDurationMs = Math.max(combatTimeoutMs, maxCombatDurationMsIn);
        maxTrackedThreats = Math.max(1, maxTrackedThreatsIn);
        nombreAffiche = Math.max(1, Math.min(topNDisplayedIn, maxTrackedThreats));
    }

    /**
     * Enregistre un coup recu par {@code defenderUuid}.
     *
     * @param originalDamage montant avant reduction, sert a calculer la part mitigee
     * @param finalDamage    montant reellement applique aux points de vie
     */
    public void enregistrerDegats(@Nonnull UUID defenderUuid,
                               @Nullable Damage damage,
                               @Nullable Ref<EntityStore> attackerRef,
                               @Nullable Store<EntityStore> store,
                               @Nullable CommandBuffer<EntityStore> commandBuffer,
                               float originalDamage,
                               float finalDamage) {
        if (!enabled || (finalDamage <= 0f && originalDamage <= 0f)) {
            return;
        }
        long timeout = combatTimeoutMs;
        long maxDuration = maxCombatDurationMs;
        int maxThreats = maxTrackedThreats;
        long now = System.currentTimeMillis();

        Session session = sessions.compute(defenderUuid, (key, existing) -> {
            if (existing == null) {
                return new Session(now);
            }
            // Une session gelee decrit un combat dont la mort est encore en attente : le
            // joueur a ete releve, ce coup ouvre donc un nouveau combat.
            boolean expired = now - existing.lastActivityMs > timeout;
            if (expired || existing.frozen) {
                return new Session(now);
            }
            return existing;
        });
        if (now - session.startedAtMs > maxDuration) {
            return;
        }

        ThreatKey key = resolveThreatKey(damage, attackerRef, store, commandBuffer);
        TypeDegats kind = TypeDegats.classify(damage);
        float applied = Math.max(0f, finalDamage);
        float mitigated = Math.max(0f, originalDamage - finalDamage);

        synchronized (session) {
            session.lastActivityMs = now;
            session.totalDamageTaken += applied;
            session.totalDamageMitigated += mitigated;

            ThreatRecord record = session.threats.get(key.id());
            if (record == null) {
                if (session.threats.size() >= maxThreats) {
                    String evictId = findLowestDamageThreat(session.threats);
                    ThreatRecord lowest = evictId == null ? null : session.threats.get(evictId);
                    // Ne pas evincer une menace plus dangereuse que le coup courant.
                    if (lowest == null || lowest.damageDealt >= applied) {
                        return;
                    }
                    session.threats.remove(evictId);
                }
                record = buildThreatRecord(key, damage, attackerRef, store, commandBuffer);
                session.threats.put(key.id(), record);
            }
            record.damageDealt += applied;
            record.damageMitigated += mitigated;
            record.damageByKind.merge(kind, applied, Float::sum);
            record.hitsByKind.merge(kind, 1, Integer::sum);
            record.lastHitMs = now;
            record.hitCount++;
            // Le niveau affiche suit le dernier coup recu : plusieurs exemplaires du meme
            // type de creature peuvent avoir des niveaux differents une fois regroupes.
            if (!key.isPlayer() && !key.isEnvironment()) {
                record.level = VaryonMobLevel.read(store, commandBuffer, attackerRef);
            }
        }
    }

    /**
     * Fige la session sans la consommer et la met a l'abri de l'expiration.
     *
     * <p>Appele lorsqu'un mod tiers (Varyon-Revive) intercepte le coup fatal pour passer le
     * joueur « a terre » : la mort reelle peut alors survenir bien plus tard, apres un
     * saignement de plusieurs minutes. Sans ce gel, la session serait expiree au moment du
     * deces et le recapitulatif serait vide.
     */
    /**
     * Prolonge la duree de vie des sessions.
     *
     * <p>Utile lorsqu'un mod differe la mort (Varyon-Revive et son etat « a terre ») : la
     * session doit survivre au saignement, sans quoi le recapitulatif serait vide au deces.
     */
    public static void appliquerMargeMortDifferee(long graceMs) {
        combatTimeoutMs = Math.max(combatTimeoutMs, graceMs);
        maxCombatDurationMs = Math.max(maxCombatDurationMs, graceMs);
    }

    public void gelerPourMortDifferee(@Nonnull UUID defenderUuid) {
        Session session = sessions.get(defenderUuid);
        if (session == null) {
            return;
        }
        synchronized (session) {
            session.frozen = true;
        }
    }

    /**
     * Fige et retire la session du joueur. Retourne {@code null} si aucune session active
     * n'existe (mort hors combat) ou si la derniere activite est trop ancienne.
     */
    @Nullable
    public Snapshot consommerALaMort(@Nonnull UUID defenderUuid) {
        Session session = sessions.remove(defenderUuid);
        if (session == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        // Une session gelee survit a l'expiration : la mort est la consequence differee du
        // combat qu'elle decrit.
        if (!session.frozen && now - session.lastActivityMs > combatTimeoutMs) {
            return null;
        }
        synchronized (session) {
            List<ThreatSnapshot> ordered = new ArrayList<>(session.threats.size());
            for (ThreatRecord record : session.threats.values()) {
                ordered.add(record.freeze());
            }
            ordered.sort(Comparator.comparingDouble(ThreatSnapshot::damageDealt).reversed()
                    .thenComparingLong(threat -> -threat.lastHitMs()));
            if (ordered.size() > nombreAffiche) {
                ordered = new ArrayList<>(ordered.subList(0, nombreAffiche));
            }
            long rawDuration = Math.max(0L, session.lastActivityMs - session.startedAtMs);
            long duration = Math.min(rawDuration, maxCombatDurationMs);
            return new Snapshot(defenderUuid, session.startedAtMs, duration,
                    session.totalDamageTaken, session.totalDamageMitigated,
                    Collections.unmodifiableList(ordered));
        }
    }

    /**
     * Photographie la session en cours sans la consommer ni la retirer : utilise a la mise
     * a terre, avant la mort reelle, qui seule appelle {@link #consommerALaMort}.
     */
    @Nullable
    public Snapshot snapshotSansConsommer(@Nonnull UUID defenderUuid) {
        Session session = sessions.get(defenderUuid);
        if (session == null) {
            return null;
        }
        synchronized (session) {
            List<ThreatSnapshot> ordered = new ArrayList<>(session.threats.size());
            for (ThreatRecord record : session.threats.values()) {
                ordered.add(record.freeze());
            }
            ordered.sort(Comparator.comparingDouble(ThreatSnapshot::damageDealt).reversed()
                    .thenComparingLong(threat -> -threat.lastHitMs()));
            if (ordered.size() > nombreAffiche) {
                ordered = new ArrayList<>(ordered.subList(0, nombreAffiche));
            }
            long rawDuration = Math.max(0L, session.lastActivityMs - session.startedAtMs);
            long duration = Math.min(rawDuration, maxCombatDurationMs);
            return new Snapshot(defenderUuid, session.startedAtMs, duration,
                    session.totalDamageTaken, session.totalDamageMitigated,
                    Collections.unmodifiableList(ordered));
        }
    }

    public void clear(@Nonnull UUID defenderUuid) {
        sessions.remove(defenderUuid);
    }

    public int clearAll() {
        int size = sessions.size();
        sessions.clear();
        return size;
    }

    @Nullable
    private static String findLowestDamageThreat(@Nonnull Map<String, ThreatRecord> threats) {
        String evictId = null;
        float lowest = Float.MAX_VALUE;
        for (Map.Entry<String, ThreatRecord> entry : threats.entrySet()) {
            float damage = entry.getValue().damageDealt;
            if (damage < lowest) {
                lowest = damage;
                evictId = entry.getKey();
            }
        }
        return evictId;
    }

    @Nonnull
    private static ThreatKey resolveThreatKey(@Nullable Damage damage,
                                              @Nullable Ref<EntityStore> attackerRef,
                                              @Nullable Store<EntityStore> store,
                                              @Nullable CommandBuffer<EntityStore> commandBuffer) {
        if (attackerRef == null || !attackerRef.isValid()) {
            // Pas d'entite : la menace est regroupee par type d'environnement (chute, lave...).
            return new ThreatKey(LibellesEnvironnement.groupingKey(damage), false, true);
        }
        PlayerRef playerRef = component(store, commandBuffer, attackerRef, PlayerRef.getComponentType());
        if (playerRef != null && playerRef.isValid() && playerRef.getUuid() != null) {
            return new ThreatKey("p:" + playerRef.getUuid(), true, false);
        }
        // Les creatures sont regroupees par NOM uniquement, pas par UUID d'entite ni par
        // niveau : trois Sandswept Skeleton Archer distincts, meme generes a des niveaux
        // differents, doivent apparaitre comme une seule menace cumulant leurs degats.
        String name = resolveMobDisplayName(store, commandBuffer, attackerRef);
        return new ThreatKey("m:" + name.toLowerCase(Locale.ROOT), false, false);
    }

    @Nonnull
    private static ThreatRecord buildThreatRecord(@Nonnull ThreatKey key,
                                                  @Nullable Damage damage,
                                                  @Nullable Ref<EntityStore> attackerRef,
                                                  @Nullable Store<EntityStore> store,
                                                  @Nullable CommandBuffer<EntityStore> commandBuffer) {
        ThreatRecord record = new ThreatRecord(key.id(), key.isPlayer(), key.isEnvironment());
        if (key.isEnvironment()) {
            record.displayName = LibellesEnvironnement.displayName(damage);
            return record;
        }
        if (attackerRef == null || !attackerRef.isValid()) {
            record.displayName = "Inconnu";
            return record;
        }
        if (key.isPlayer()) {
            PlayerRef playerRef = component(store, commandBuffer, attackerRef, PlayerRef.getComponentType());
            record.displayName = playerRef != null && playerRef.getUsername() != null
                    ? playerRef.getUsername()
                    : "Joueur";
        } else {
            record.displayName = resolveMobDisplayName(store, commandBuffer, attackerRef);
            record.level = VaryonMobLevel.read(store, commandBuffer, attackerRef);
        }
        if (record.displayName == null || record.displayName.isBlank()) {
            record.displayName = key.isPlayer() ? "Joueur" : "Creature";
        }
        return record;
    }

    @Nonnull
    private static String resolveMobDisplayName(@Nullable Store<EntityStore> store,
                                                @Nullable CommandBuffer<EntityStore> commandBuffer,
                                                @Nonnull Ref<EntityStore> ref) {
        DisplayNameComponent displayName =
                component(store, commandBuffer, ref, DisplayNameComponent.getComponentType());
        if (displayName != null && displayName.getDisplayName() != null) {
            String ansi = displayName.getDisplayName().getAnsiMessage();
            if (ansi != null && !ansi.isBlank()) {
                return stripLevelSuffix(ansi.trim());
            }
        }
        return "Creature";
    }

    /**
     * Varyon prefixe les plaques nominatives par {@code "Lvl N"} : le niveau est deja affiche
     * separement sur la carte, on retire donc le doublon du nom.
     */
    @Nonnull
    private static String stripLevelSuffix(@Nonnull String name) {
        String cleaned = name.replaceAll("(?i)\\blvl?\\s*\\.?\\s*\\d+\\b", "").trim();
        cleaned = cleaned.replaceAll("\\s{2,}", " ").trim();
        return cleaned.isEmpty() ? name : cleaned;
    }

    @Nullable
    private static <T extends com.hypixel.hytale.component.Component<EntityStore>> T component(
            @Nullable Store<EntityStore> store,
            @Nullable CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Ref<EntityStore> ref,
            @Nullable ComponentType<EntityStore, T> type) {
        if (type == null) {
            return null;
        }
        try {
            if (commandBuffer != null) {
                T fromBuffer = commandBuffer.getComponent(ref, type);
                if (fromBuffer != null) {
                    return fromBuffer;
                }
            }
            if (store != null) {
                return store.getComponent(ref, type);
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
    }

    private static final class Session {
        final long startedAtMs;
        long lastActivityMs;
        float totalDamageTaken;
        float totalDamageMitigated;
        /** Mise a l'abri de l'expiration : mort differee en cours (etat « a terre »). */
        boolean frozen;
        final Map<String, ThreatRecord> threats = new HashMap<>();

        Session(long now) {
            this.startedAtMs = now;
            this.lastActivityMs = now;
        }
    }

    private record ThreatKey(@Nonnull String id, boolean isPlayer, boolean isEnvironment) {}

    private static final class ThreatRecord {
        final String id;
        final boolean isPlayer;
        final boolean isEnvironment;
        String displayName = "Inconnu";
        int level = VaryonMobLevel.NO_LEVEL;
        float damageDealt;
        float damageMitigated;
        int hitCount;
        long lastHitMs;
        final Map<TypeDegats, Float> damageByKind = new EnumMap<>(TypeDegats.class);
        final Map<TypeDegats, Integer> hitsByKind = new EnumMap<>(TypeDegats.class);

        ThreatRecord(String id, boolean isPlayer, boolean isEnvironment) {
            this.id = id;
            this.isPlayer = isPlayer;
            this.isEnvironment = isEnvironment;
        }

        @Nonnull
        ThreatSnapshot freeze() {
            return new ThreatSnapshot(id, isPlayer, isEnvironment, displayName, level,
                    damageDealt, damageMitigated, hitCount, lastHitMs,
                    Collections.unmodifiableMap(new EnumMap<>(damageByKind)),
                    Collections.unmodifiableMap(new EnumMap<>(hitsByKind)));
        }
    }

    /** Une menace figee : entite attaquante ou cause environnementale regroupee. */
    public record ThreatSnapshot(@Nonnull String id,
                                 boolean isPlayer,
                                 boolean isEnvironment,
                                 @Nonnull String displayName,
                                 int level,
                                 float damageDealt,
                                 float damageMitigated,
                                 int hitCount,
                                 long lastHitMs,
                                 @Nonnull Map<TypeDegats, Float> damageByKind,
                                 @Nonnull Map<TypeDegats, Integer> hitsByKind) {

        /** Types de degats utilises, du plus au moins dommageable. */
        @Nonnull
        public List<TypeDegats> kindsByDamage() {
            List<TypeDegats> kinds = new ArrayList<>(damageByKind.keySet());
            kinds.sort(Comparator.comparingDouble(
                    (TypeDegats kind) -> damageByKind.getOrDefault(kind, 0f)).reversed());
            return kinds;
        }

        public float damageOfKind(@Nonnull TypeDegats kind) {
            return damageByKind.getOrDefault(kind, 0f);
        }

        public int hitsOfKind(@Nonnull TypeDegats kind) {
            return hitsByKind.getOrDefault(kind, 0);
        }
    }

    /** Recapitulatif complet d'une mort. */
    public record Snapshot(@Nonnull UUID playerUuid,
                           long startedAtMs,
                           long combatDurationMs,
                           float totalDamageTaken,
                           float totalDamageMitigated,
                           @Nonnull List<ThreatSnapshot> topThreats) {}
}
