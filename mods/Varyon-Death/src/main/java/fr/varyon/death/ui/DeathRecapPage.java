package fr.varyon.death.ui;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import fr.varyon.death.combat.SuiviCombat;
import fr.varyon.death.combat.SuiviCombat.ThreatSnapshot;
import fr.varyon.death.combat.TypeDegats;
import fr.varyon.death.combat.BanqueRecaps;
import fr.varyon.death.config.Diagnostic;
import fr.varyon.death.config.PreferencesRecap;

/**
 * Page « VOUS ETES MORT » : classement des menaces sur deux rangees de cinq cases,
 * plus un panneau de detail alimente au clic sur une case.
 */
public final class DeathRecapPage extends InteractiveCustomUIPage<DeathRecapPage.EventDataClass> {

    private static final HytaleLogger LOGGER =
            HytaleLogger.getLogger().getSubLogger("VaryonDeathRecap-UI");

    public static final int MAX_CELLS = 10;
    private static final String ACTION_CLOSE = "recap:close";
    private static final String ACTION_REOPEN = "recap:reopen";
    private static final String ACTION_TOGGLE = "recap:toggle";
    private static final String ACTION_DETAIL_PREFIX = "recap:detail:";
    private static final int DETAIL_KIND_SLOTS = 4;
    private static final int OPEN_RETRY_TICKS_MAX = 60;

    private static final ConcurrentHashMap<UUID, DeathRecapPage> OPEN_PAGES = new ConcurrentHashMap<>();

    private final SuiviCombat.Snapshot snapshot;
    private volatile boolean resolved;
    private volatile boolean expanded;

    private DeathRecapPage(@Nonnull PlayerRef playerRef,
                           @Nonnull CustomPageLifetime lifetime,
                           @Nonnull SuiviCombat.Snapshot snapshot,
                           boolean expanded) {
        super(playerRef, lifetime, EventDataClass.CODEC);
        this.snapshot = snapshot;
        this.expanded = expanded;
    }

    /**
     * Ouvre la page repliee : seul le bouton "VOIR LE RECAPITULATIF" est visible, pour ne pas
     * masquer le jeu des la mise a terre.
     */
    public static void openFor(@Nonnull PlayerRef playerRef,
                               @Nonnull SuiviCombat.Snapshot snapshot) {
        openForInternal(playerRef, snapshot, false, OPEN_RETRY_TICKS_MAX);
    }

    /** Ouvre la page directement depliee : utilise par {@code /mort}, une demande explicite. */
    public static void openExpandedFor(@Nonnull PlayerRef playerRef,
                                       @Nonnull SuiviCombat.Snapshot snapshot) {
        openForInternal(playerRef, snapshot, true, OPEN_RETRY_TICKS_MAX);
    }

    public static boolean hasOpenPage(@Nonnull UUID playerUuid) {
        DeathRecapPage page = OPEN_PAGES.get(playerUuid);
        return page != null && !page.resolved;
    }

    private static void openForInternal(@Nonnull PlayerRef playerRef,
                                        @Nonnull SuiviCombat.Snapshot snapshot,
                                        boolean expanded,
                                        int ticksRemaining) {
        UUID playerUuid = playerRef.getUuid();
        if (playerUuid == null || !playerRef.isValid()) {
            return;
        }
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        if (store == null) {
            return;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        DeathRecapPage existing = OPEN_PAGES.get(playerUuid);
        if (existing != null && !existing.resolved) {
            if (expanded && !existing.expanded) {
                existing.expanded = true;
                existing.envoyerEtatDeplie();
            }
            return;
        }
        // Le client refuse une page tant que l'ecran de mort vanilla est actif : on attend
        // que le DeathComponent disparaisse avant d'ouvrir.
        if (store.getComponent(ref, DeathComponent.getComponentType()) != null) {
            if (ticksRemaining <= 0) {
                BanqueRecaps.get().rebank(playerUuid, snapshot);
                return;
            }
            World world = resolveDispatchWorld(playerUuid);
            if (world == null) {
                return;
            }
            world.execute(() -> openForInternal(playerRef, snapshot, expanded, ticksRemaining - 1));
            return;
        }
        DeathRecapPage page = new DeathRecapPage(playerRef, CustomPageLifetime.CanDismiss, snapshot, expanded);
        OPEN_PAGES.put(playerUuid, page);
        player.getPageManager().openCustomPage(ref, store, page);
    }

    @Nullable
    private static World resolveDispatchWorld(@Nonnull UUID uuid) {
        Universe universe = Universe.get();
        if (universe == null) {
            return null;
        }
        PlayerRef playerRef = universe.getPlayer(uuid);
        if (playerRef != null && playerRef.isValid()) {
            Ref<EntityStore> ref = playerRef.getReference();
            Store<EntityStore> store = ref != null && ref.isValid() ? ref.getStore() : null;
            if (store != null && store.getExternalData() != null) {
                World world = store.getExternalData().getWorld();
                if (world != null) {
                    return world;
                }
            }
        }
        return universe.getDefaultWorld();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder ui,
                      @Nonnull UIEventBuilder events,
                      @Nonnull Store<EntityStore> store) {
        ui.append("Pages/DeathRecap/DeathRecapPage.ui");

        ui.set("#RecapTitle.Text", "VOUS ETES MORT");
        ui.set("#RecapDurationLabel.Text", "DUREE");
        ui.set("#RecapDamageTakenLabel.Text", "DEGATS SUBIS");
        ui.set("#RecapMitigatedLabel.Text", "DEGATS MITIGES");
        ui.set("#RecapThreatDetailTitle.Text", "DETAIL DE LA MENACE");
        ui.set("#RecapDetailDmgLabel.Text", "DEGATS INFLIGES");
        ui.set("#RecapDetailMitigatedLabel.Text", "VOUS AVEZ MITIGE");
        ui.set("#RecapDetailHitsLabel.Text", "COUPS");
        ui.set("#RecapDetailAvgLabel.Text", "MOY. / COUP");
        ui.set("#RecapBreakdownTitle.Text", "REPARTITION DES DEGATS");
        ui.set("#RecapToggleLabel.Text", "Desactiver les recaps de mort");
        ui.set("#RecapHint.Text", "Astuce : tapez /mort pour rouvrir ce recapitulatif.");
        ui.set("#RecapReopenButton.Text", "VOIR LE RECAPITULATIF");

        // Ne pas passer le 4e parametre (locksInterface) : il vaut false et rend les liaisons
        // inertes. La surcharge a 3 arguments utilise true, comme les pages Varyon qui marchent.
        events.addEventBinding(CustomUIEventBindingType.Activating, "#RecapCloseButton",
                EventData.of("Action", ACTION_CLOSE));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#RecapReopenButton",
                EventData.of("Action", ACTION_REOPEN));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#RecapToggle",
                EventData.of("Action", ACTION_TOGGLE));
        for (int i = 0; i < MAX_CELLS; i++) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#RecapCell" + i,
                    EventData.of("Action", ACTION_DETAIL_PREFIX + i));
        }

        ui.set("#RecapCollapsedRoot.Visible", !expanded);
        ui.set("#RecapExpandedRoot.Visible", expanded);

        UUID playerUuid = playerRef.getUuid();
        if (playerUuid != null) {
            // La case est cochee quand les recaps sont DESACTIVES (libelle negatif).
            ui.set("#RecapToggle.Value", !PreferencesRecap.estActif(playerUuid));
        }

        ui.set("#RecapCombatDuration.Text", RecapFormat.duration(snapshot.combatDurationMs()));
        ui.set("#RecapDamageTaken.Text", RecapFormat.number(snapshot.totalDamageTaken()));
        ui.set("#RecapDamageMitigated.Text", RecapFormat.number(snapshot.totalDamageMitigated()));

        populateCells(ui);
        List<ThreatSnapshot> threats = snapshot.topThreats();
        populateDetailPanel(ui, threats.isEmpty() ? null : threats.get(0));
    }

    private void populateCells(@Nonnull UICommandBuilder ui) {
        List<ThreatSnapshot> threats = snapshot.topThreats();
        int activeCells = Math.min(MAX_CELLS, SuiviCombat.nombreAffiche());
        for (int i = 0; i < MAX_CELLS; i++) {
            String cellId = "#RecapCell" + i;
            boolean show = i < activeCells && i < threats.size();
            if (show) {
                ThreatSnapshot threat = threats.get(i);
                ui.set(cellId + "Rank.Text", "#" + (i + 1));
                ui.set(cellId + "Level.Text", RecapFormat.levelLabel(threat));
                ui.set(cellId + "Name.Text", threat.displayName());
                ui.set(cellId + "Damage.Text", RecapFormat.number(threat.damageDealt()) + " degats");
                ui.set(cellId + "Kind.Text", RecapFormat.kindLabel(threat));
                // Sur un type mixte, on colore selon le type dominant (le plus dommageable).
                ui.set(cellId + "Kind.Style.TextColor", RecapFormat.dominantKindColor(threat));
            }
            ui.set(cellId + ".Visible", show);
        }
        // La seconde rangee disparait entierement quand elle est vide, pour ne pas laisser
        // un bandeau de cases masquees sous la premiere.
        ui.set("#RecapRow2.Visible", threats.size() > 5 && activeCells > 5);
        ui.set("#RecapNoThreats.Visible", threats.isEmpty());
    }

    private void populateDetailPanel(@Nonnull UICommandBuilder ui, @Nullable ThreatSnapshot threat) {
        if (threat == null) {
            ui.set("#RecapDetailSubtitle.Text", "Aucune menace a inspecter.");
            ui.set("#RecapDetailDmg.Text", "-");
            ui.set("#RecapDetailMitigated.Text", "-");
            ui.set("#RecapDetailHits.Text", "-");
            ui.set("#RecapDetailAvg.Text", "-");
            for (int i = 0; i < DETAIL_KIND_SLOTS; i++) {
                ui.set("#RecapBreakdown" + i + ".Visible", false);
            }
            return;
        }
        ui.set("#RecapDetailSubtitle.Text", RecapFormat.detailSubtitle(threat));
        ui.set("#RecapDetailDmg.Text", RecapFormat.number(threat.damageDealt()));
        ui.set("#RecapDetailMitigated.Text", RecapFormat.number(threat.damageMitigated()));
        ui.set("#RecapDetailHits.Text", Integer.toString(threat.hitCount()));
        ui.set("#RecapDetailAvg.Text", RecapFormat.averagePerHit(threat));

        List<TypeDegats> kinds = threat.kindsByDamage();
        for (int i = 0; i < DETAIL_KIND_SLOTS; i++) {
            String slotId = "#RecapBreakdown" + i;
            boolean show = i < kinds.size();
            if (show) {
                TypeDegats kind = kinds.get(i);
                ui.set(slotId + "Text.Text", RecapFormat.kindBreakdownLine(threat, kind));
                ui.set(slotId + "Text.Style.TextColor", kind.color());
            }
            ui.set(slotId + ".Visible", show);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull EventDataClass data) {
        String action = data.action;
        UUID playerUuid = playerRef.getUuid();
        if (Diagnostic.estActif()) {
            LOGGER.at(Level.INFO).log("interaction recue: action=%s resolved=%s joueur=%s",
                    action, resolved, playerUuid);
        }
        // Les liaisons sont declarees avec locksInterface = true : le client fige son interface
        // ("Loading...") jusqu'a recevoir une reponse du serveur. TOUT chemin de sortie doit
        // donc repondre, y compris les cas ignores, sinon le verrou n'est jamais leve.
        if (resolved || action == null || action.isEmpty() || playerUuid == null) {
            releaseInterface();
            return;
        }
        if (ACTION_CLOSE.equals(action)) {
            handleCollapse();
            return;
        }
        if (ACTION_REOPEN.equals(action)) {
            handleReopen();
            return;
        }
        if (ACTION_TOGGLE.equals(action)) {
            handleToggle(playerUuid);
            return;
        }
        if (action.startsWith(ACTION_DETAIL_PREFIX)) {
            try {
                handleDetailOpen(Integer.parseInt(action.substring(ACTION_DETAIL_PREFIX.length())));
            } catch (NumberFormatException ignored) {
                releaseInterface();
            }
            return;
        }
        releaseInterface();
    }

    private void handleDetailOpen(int index) {
        List<ThreatSnapshot> threats = snapshot.topThreats();
        int activeCells = Math.min(MAX_CELLS, SuiviCombat.nombreAffiche());
        if (index < 0 || index >= threats.size() || index >= activeCells) {
            releaseInterface();
            return;
        }
        UICommandBuilder ui = new UICommandBuilder();
        populateDetailPanel(ui, threats.get(index));
        envoyerMiseAJour(ui);
    }

    /**
     * Repond au client sans rien modifier, uniquement pour lever le verrou d'interface pose
     * par {@code locksInterface}. Sans cela, un clic sans effet laisse un « Loading... » infini.
     */
    private void releaseInterface() {
        envoyerMiseAJour(new UICommandBuilder());
    }

    private static final int ENVOI_RETRY_TICKS_MAX = 20;

    /**
     * Enveloppe {@code sendUpdate} d'un filet de securite.
     *
     * <p>{@code sendUpdate} dereference {@code store.getExternalData()} de facon synchrone,
     * avant meme de planifier l'envoi sur le thread du monde. Si l'entite n'est pas encore
     * pleinement rattachee a son monde, l'appel leve alors une exception nue : sans filet, le
     * client restait bloque sur « Loading... » car aucune reponse ne repartait. On reessaie
     * donc quelques ticks plus tard plutot que d'abandonner silencieusement.
     */
    private void envoyerMiseAJour(@Nonnull UICommandBuilder ui) {
        envoyerMiseAJour(ui, ENVOI_RETRY_TICKS_MAX);
    }

    private void envoyerMiseAJour(@Nonnull UICommandBuilder ui, int ticksRemaining) {
        try {
            sendUpdate(ui, false);
        } catch (Exception e) {
            UUID playerUuid = playerRef.getUuid();
            World world = resolved || ticksRemaining <= 0 || playerUuid == null
                    ? null
                    : resolveDispatchWorld(playerUuid);
            if (world == null) {
                LOGGER.at(Level.WARNING).log("Mise a jour de l'interface echouee: %s", e.getMessage());
                return;
            }
            world.execute(() -> envoyerMiseAJour(ui, ticksRemaining - 1));
        }
    }

    private void handleToggle(@Nonnull UUID playerUuid) {
        boolean nowEnabled = PreferencesRecap.toggle(playerUuid);
        if (!nowEnabled) {
            SuiviCombat.get().clear(playerUuid);
            BanqueRecaps.get().dropAll(playerUuid);
            handleClose(playerUuid);
            return;
        }
        // Recaps reactives : la page reste ouverte, il faut donc lever le verrou.
        releaseInterface();
    }

    private void handleClose(@Nonnull UUID playerUuid) {
        resolved = true;
        OPEN_PAGES.remove(playerUuid, this);
        try {
            close();
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Fermeture de la page echouee pour %s: %s", playerUuid, e.getMessage());
            // La fermeture a echoue : la page reste affichee et son interface verrouillee.
            releaseInterface();
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UUID playerUuid = playerRef.getUuid();
        if (playerUuid != null) {
            OPEN_PAGES.remove(playerUuid, this);
        }
        resolved = true;
        super.onDismiss(ref, store);
    }

    /**
     * Charge utile des evenements d'interface. Les clics arrivent en BSON structure : la page
     * doit donc etendre {@link InteractiveCustomUIPage} et fournir ce codec. La surcharge
     * {@code handleDataEvent(..., String)} de {@link CustomUIPage} n'est qu'un stub qui leve
     * {@code UnsupportedOperationException} et ne recoit jamais rien.
     */
    public static final class EventDataClass {
        public static final BuilderCodec<EventDataClass> CODEC =
                BuilderCodec.builder(EventDataClass.class, EventDataClass::new)
                        .addField(new KeyedCodec<>("Action", Codec.STRING),
                                (event, value) -> event.action = value, event -> event.action)
                        .build();

        public String action;
    }
}
