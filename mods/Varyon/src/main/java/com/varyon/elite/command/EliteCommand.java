package com.varyon.elite.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.varyon.elite.EliteConfig;
import com.varyon.elite.EliteDeeliteSystem;
import com.varyon.elite.EliteRoller;
import com.varyon.elite.EliteSpawnRefSystem;
import com.varyon.elite.MobClass;
import com.varyon.elite.ability.EliteAbility;
import com.varyon.elite.ui.EliteDashboardPage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@code /varyon elite} - admin entry point for the elite subsystem (sub-command of
 * {@code /varyon}).
 *   (no arg) / dashboard              open the admin dashboard (live view, zones, economy)
 *   spawn &lt;class&gt; &lt;tier&gt; [name]      promote the nearest non-elite NPC (BOSS refused)
 *   spawnat &lt;npcType&gt; &lt;class&gt; &lt;tier&gt; [name]  spawn a fresh NPC of that type, already an elite
 *   ability &lt;ability&gt;                 fire one ability from the nearest elite
 *   deelite                           run a de-elite sweep now
 *
 * Read-only inspection and per-zone tuning live in the dashboard - see {@link EliteDashboardPage}.
 */
public final class EliteCommand extends AbstractCommand {

    public static final String NAME = "elite";
    public static final String PERMISSION = "varyon.admin";

    private final EliteConfig config;
    private final com.varyon.config.ConfigManager configManager;
    private final java.nio.file.Path dataDir;

    public EliteCommand(@Nonnull com.varyon.config.ConfigManager configManager, @Nonnull java.nio.file.Path dataDir) {
        super(NAME, "Administration des mobs elites");
        this.configManager = configManager;
        this.config = configManager.getEliteConfig();
        this.dataDir = dataDir;
        requirePermission(PERMISSION);
        addSubCommand(new Dashboard());
        addSubCommand(new Spawn());
        addSubCommand(new SpawnAt());
        addSubCommand(new Ability());
        addSubCommand(new Deelite());
    }

    /** No sub-command opens the dashboard. */
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
        return openDashboard(ctx);
    }

    private CompletableFuture<Void> openDashboard(@Nonnull CommandContext ctx) {
        Ref<EntityStore> playerRef = ctx.senderAsPlayerRef();
        World world = world(playerRef);
        if (playerRef == null || world == null) {
            ctx.sendMessage(Message.raw("Commande joueur uniquement."));
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            PlayerRef pref = store.getComponent(playerRef, PlayerRef.getComponentType());
            Player player = store.getComponent(playerRef, Player.getComponentType());
            if (pref == null || player == null) {
                return;
            }
            EliteDashboardPage page = new EliteDashboardPage(pref, configManager, dataDir);
            player.getPageManager().openCustomPage(playerRef, store, page);
        }, world);
    }

    private static World world(@Nullable Ref<EntityStore> ref) {
        if (ref == null || !ref.isValid()) {
            return null;
        }
        Object ext = ref.getStore().getExternalData();
        return ext instanceof EntityStore es ? es.getWorld() : null;
    }

    /** Rolls and applies elite state to a resolved NPC ref on the world thread. */
    private void makeElite(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> target, @Nonnull MobClass mobClass, int tier,
                           @Nullable String name) {
        NPCEntity npc = store.getComponent(target, NPCEntity.getComponentType());
        if (npc == null) {
            ctx.sendMessage(Message.raw("Cible sans composant NPC."));
            return;
        }
        long seed = EliteRoller.seedFor("cmd", tier, System.nanoTime(), 0, 0);
        List<EliteAbility> abilities = EliteRoller.rollAbilities(seed, mobClass, tier);
        EliteSpawnRefSystem.promoteDirect(target, store, npc, mobClass, tier, seed, abilities,
                name != null && !name.isBlank() ? name : null, 0.0, config);
        ctx.sendMessage(Message.raw("Elite : " + mobClass + " T" + tier
                + (name != null && !name.isBlank() ? " \"" + name + "\"" : "")
                + " (" + abilities.size() + " capacites)"));
    }

    @Nullable
    private static Ref<EntityStore> nearestNpc(@Nonnull Store<EntityStore> store, @Nonnull org.joml.Vector3d from,
                                               double range, boolean eliteOnly) {
        AtomicReference<Ref<EntityStore>> best = new AtomicReference<>();
        AtomicReference<Double> bestDist = new AtomicReference<>(Double.MAX_VALUE);
        try {
            Selector.selectNearbyEntities(store, new org.joml.Vector3d(from), range, t -> {
                try {
                    if (store.getComponent(t, NPCEntity.getComponentType()) == null) {
                        return;
                    }
                    boolean isElite = store.getComponent(t,
                            com.varyon.elite.component.EliteComponent.getComponentType()) != null;
                    if (eliteOnly != isElite) {
                        return;
                    }
                    TransformComponent tc = store.getComponent(t, TransformComponent.getComponentType());
                    if (tc == null) {
                        return;
                    }
                    double d = from.distance(new org.joml.Vector3d(tc.getPosition()));
                    if (d < bestDist.get()) {
                        bestDist.set(d);
                        best.set(t);
                    }
                } catch (Exception ignored) {
                }
            }, t -> true);
        } catch (Exception ignored) {
        }
        return best.get();
    }

    private static int clampTier(int raw) {
        return Math.max(1, Math.min(EliteConfig.TIER_COUNT, raw));
    }

    // ------------------------------------------------------------------

    private final class Dashboard extends AbstractCommand {
        Dashboard() {
            super("dashboard", "Ouvre le tableau de bord admin");
            requirePermission(PERMISSION);
        }

        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
            return openDashboard(ctx);
        }
    }

    /** {@code /varyon elite spawn <class> <tier> [name]} - promote the nearest non-elite NPC. */
    private final class Spawn extends AbstractCommand {
        private final RequiredArg<MobClass> classArg;
        private final RequiredArg<Integer> tierArg;
        private final OptionalArg<String> nameArg;

        Spawn() {
            super("spawn", "Promeut le PNJ le plus proche : /varyon elite spawn <classe> <tier> [nom]");
            requirePermission(PERMISSION);
            classArg = withRequiredArg("classe", "classe d'elite (BOSS exclu)",
                    ArgTypes.forEnum("classe", MobClass.class));
            tierArg = withRequiredArg("tier", "1-5", ArgTypes.INTEGER);
            nameArg = withOptionalArg("nom", "nom d'elite (optionnel)", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
            Ref<EntityStore> playerRef = ctx.senderAsPlayerRef();
            World world = world(playerRef);
            if (playerRef == null || world == null) {
                ctx.sendMessage(Message.raw("Commande joueur uniquement."));
                return CompletableFuture.completedFuture(null);
            }
            MobClass mobClass = ctx.get(classArg);
            if (mobClass == MobClass.BOSS) {
                ctx.sendMessage(Message.raw("Les BOSS n'apparaissent que naturellement."));
                return CompletableFuture.completedFuture(null);
            }
            int tier = clampTier(ctx.get(tierArg));
            if (!mobClass.allowsTier(tier)) {
                ctx.sendMessage(Message.raw(mobClass + " n'est pas disponible au tier " + tier
                        + " (min " + mobClass.getMinTier() + ")."));
                return CompletableFuture.completedFuture(null);
            }
            String name = ctx.get(nameArg);

            world.execute(() -> {
                Store<EntityStore> store = world.getEntityStore().getStore();
                TransformComponent ptc = store.getComponent(playerRef, TransformComponent.getComponentType());
                if (ptc == null) {
                    ctx.sendMessage(Message.raw("Position joueur introuvable."));
                    return;
                }
                Ref<EntityStore> target = nearestNpc(store, new org.joml.Vector3d(ptc.getPosition()), 12.0, false);
                if (target == null) {
                    ctx.sendMessage(Message.raw("Aucun PNJ non-elite a moins de 12 blocs."));
                    return;
                }
                makeElite(ctx, store, target, mobClass, tier, name);
            });
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * {@code /varyon elite spawnat <npcType> <class> <tier> [name]} - spawn a fresh NPC of
     * {@code npcType} at the player's position and promote it immediately.
     */
    private final class SpawnAt extends AbstractCommand {
        private final RequiredArg<String> typeArg;
        private final RequiredArg<MobClass> classArg;
        private final RequiredArg<Integer> tierArg;
        private final OptionalArg<String> nameArg;

        SpawnAt() {
            super("spawnat", "Fait apparaitre un NPC deja elite : /varyon elite spawnat <npcType> <classe> <tier> [nom]");
            requirePermission(PERMISSION);
            typeArg = withRequiredArg("npcType", "id du type de NPC (ex: Skeleton)", ArgTypes.STRING);
            classArg = withRequiredArg("classe", "classe d'elite (BOSS exclu)",
                    ArgTypes.forEnum("classe", MobClass.class));
            tierArg = withRequiredArg("tier", "1-5", ArgTypes.INTEGER);
            nameArg = withOptionalArg("nom", "nom d'elite (optionnel)", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
            Ref<EntityStore> playerRef = ctx.senderAsPlayerRef();
            World world = world(playerRef);
            if (playerRef == null || world == null) {
                ctx.sendMessage(Message.raw("Commande joueur uniquement."));
                return CompletableFuture.completedFuture(null);
            }
            String npcType = ctx.get(typeArg).trim();
            MobClass mobClass = ctx.get(classArg);
            if (mobClass == MobClass.BOSS) {
                ctx.sendMessage(Message.raw("Les BOSS n'apparaissent que naturellement."));
                return CompletableFuture.completedFuture(null);
            }
            int tier = clampTier(ctx.get(tierArg));
            if (!mobClass.allowsTier(tier)) {
                ctx.sendMessage(Message.raw(mobClass + " n'est pas disponible au tier " + tier
                        + " (min " + mobClass.getMinTier() + ")."));
                return CompletableFuture.completedFuture(null);
            }
            String name = ctx.get(nameArg);

            world.execute(() -> {
                Store<EntityStore> store = world.getEntityStore().getStore();
                TransformComponent ptc = store.getComponent(playerRef, TransformComponent.getComponentType());
                if (ptc == null) {
                    ctx.sendMessage(Message.raw("Position joueur introuvable."));
                    return;
                }
                org.joml.Vector3d pos = new org.joml.Vector3d(ptc.getPosition());
                Ref<EntityStore> spawned;
                try {
                    var result = NPCPlugin.get().spawnNPC(store, npcType, null, pos, Rotation3f.ZERO);
                    spawned = result != null ? result.first() : null;
                } catch (Exception e) {
                    ctx.sendMessage(Message.raw("Echec du spawn de '" + npcType + "' : " + e.getMessage()));
                    return;
                }
                if (spawned == null || !spawned.isValid()) {
                    ctx.sendMessage(Message.raw("Type de NPC inconnu ou spawn refuse : '" + npcType + "'."));
                    return;
                }
                makeElite(ctx, store, spawned, mobClass, tier, name);
            });
            return CompletableFuture.completedFuture(null);
        }
    }

    private final class Ability extends AbstractCommand {
        private final RequiredArg<String> abilityArg;

        Ability() {
            super("ability", "Declenche une capacite de l'elite le plus proche : /varyon elite ability <capacite>");
            requirePermission(PERMISSION);
            abilityArg = withRequiredArg("capacite", "nom d'EliteAbility", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
            Ref<EntityStore> playerRef = ctx.senderAsPlayerRef();
            World world = world(playerRef);
            if (playerRef == null || world == null) {
                ctx.sendMessage(Message.raw("Commande joueur uniquement."));
                return CompletableFuture.completedFuture(null);
            }
            EliteAbility ability;
            try {
                ability = EliteAbility.valueOf(ctx.get(abilityArg).trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                ctx.sendMessage(Message.raw("Capacite inconnue."));
                return CompletableFuture.completedFuture(null);
            }
            world.execute(() -> {
                Store<EntityStore> store = world.getEntityStore().getStore();
                TransformComponent ptc = store.getComponent(playerRef, TransformComponent.getComponentType());
                if (ptc == null) {
                    return;
                }
                org.joml.Vector3d ppos = new org.joml.Vector3d(ptc.getPosition());
                Ref<EntityStore> eliteRef = nearestNpc(store, ppos, 40.0, true);
                if (eliteRef == null) {
                    ctx.sendMessage(Message.raw("Aucun elite a moins de 40 blocs."));
                    return;
                }
                var elite = store.getComponent(eliteRef, com.varyon.elite.component.EliteComponent.getComponentType());
                TransformComponent etc = store.getComponent(eliteRef, TransformComponent.getComponentType());
                if (elite == null || etc == null) {
                    return;
                }
                com.varyon.elite.ability.EliteAbilityExecutor.execute(ability, elite, eliteRef, store, store,
                        new org.joml.Vector3d(etc.getPosition()), ppos);
                ctx.sendMessage(Message.raw("Capacite " + ability + " declenchee."));
            });
            return CompletableFuture.completedFuture(null);
        }
    }

    private final class Deelite extends AbstractCommand {
        Deelite() {
            super("deelite", "Repasse tous les elites en mobs normaux");
            requirePermission(PERMISSION);
        }

        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
            EliteDeeliteSystem.requestPurge();
            ctx.sendMessage(Message.raw("De-elite demande - les elites reviennent a la normale."));
            return CompletableFuture.completedFuture(null);
        }
    }
}
