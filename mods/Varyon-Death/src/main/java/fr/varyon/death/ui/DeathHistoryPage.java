package fr.varyon.death.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
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
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import fr.varyon.death.combat.HistoriqueMorts;
import fr.varyon.death.combat.HistoriqueMorts.DeathEntry;

/**
 * Page « HISTORIQUE DES MORTS » : liste des {@value HistoriqueMorts#MAX_ENTRIES} dernieres
 * morts du joueur (date, monde, resume), chaque ligne ouvrant le recapitulatif complet
 * correspondant via {@link DeathRecapPage}.
 */
public final class DeathHistoryPage extends InteractiveCustomUIPage<DeathHistoryPage.EventDataClass> {

    private static final HytaleLogger LOGGER =
            HytaleLogger.getLogger().getSubLogger("VaryonDeathRecap-Historique-UI");

    private static final String ACTION_CLOSE = "hist:close";
    private static final String ACTION_ENTRY_PREFIX = "hist:entry:";
    // Le serveur peut tourner dans un autre fuseau (souvent UTC) : on fixe explicitement
    // Europe/Paris, qui gere lui-meme le passage heure d'ete/hiver (CEST/CET), plutot que
    // ZoneId.systemDefault() qui suivait le fuseau de la machine hote.
    private static final ZoneId FUSEAU_AFFICHAGE = ZoneId.of("Europe/Paris");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRANCE).withZone(FUSEAU_AFFICHAGE);

    private final List<DeathEntry> entries;
    private volatile boolean resolved;

    private DeathHistoryPage(@Nonnull PlayerRef playerRef, @Nonnull List<DeathEntry> entries) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventDataClass.CODEC);
        this.entries = entries;
    }

    public static void openFor(@Nonnull PlayerRef playerRef, @Nonnull List<DeathEntry> entries) {
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
        DeathHistoryPage page = new DeathHistoryPage(playerRef, entries);
        player.getPageManager().openCustomPage(ref, store, page);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder ui,
                      @Nonnull UIEventBuilder events,
                      @Nonnull Store<EntityStore> store) {
        ui.append("Pages/DeathRecap/DeathHistoryPage.ui");

        ui.set("#HistTitle.Text", "HISTORIQUE DES MORTS");
        ui.set("#HistCloseButton.Text", "FERMER");

        events.addEventBinding(CustomUIEventBindingType.Activating, "#HistCloseButton",
                EventData.of("Action", ACTION_CLOSE));
        for (int i = 0; i < HistoriqueMorts.MAX_ENTRIES; i++) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#HistEntry" + i,
                    EventData.of("Action", ACTION_ENTRY_PREFIX + i));
        }

        ui.set("#HistEmpty.Visible", entries.isEmpty());
        for (int i = 0; i < HistoriqueMorts.MAX_ENTRIES; i++) {
            String entryId = "#HistEntry" + i;
            boolean show = i < entries.size();
            if (show) {
                DeathEntry entry = entries.get(i);
                ui.set(entryId + "Date.Text", DATE_FORMAT.format(Instant.ofEpochMilli(entry.timestampMs())));
                ui.set(entryId + "World.Text", entry.worldName());
                ui.set(entryId + "Summary.Text", resume(entry));
            }
            ui.set("#HistRow" + i + ".Visible", show);
        }
    }

    @Nonnull
    private static String resume(@Nonnull DeathEntry entry) {
        String premiereMenace = entry.snapshot().topThreats().isEmpty()
                ? "Inconnu"
                : entry.snapshot().topThreats().get(0).displayName();
        return RecapFormat.number(entry.snapshot().totalDamageTaken()) + " degats - " + premiereMenace;
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull EventDataClass data) {
        String action = data.action;
        if (resolved || action == null || action.isEmpty()) {
            releaseInterface();
            return;
        }
        if (ACTION_CLOSE.equals(action)) {
            handleClose();
            return;
        }
        if (action.startsWith(ACTION_ENTRY_PREFIX)) {
            try {
                handleEntryOpen(Integer.parseInt(action.substring(ACTION_ENTRY_PREFIX.length())));
            } catch (NumberFormatException ignored) {
                releaseInterface();
            }
            return;
        }
        releaseInterface();
    }

    private void handleEntryOpen(int index) {
        if (index < 0 || index >= entries.size()) {
            releaseInterface();
            return;
        }
        // Ouvrir une nouvelle page remplace celle-ci (un seul slot de page custom cote client) :
        // pas besoin de fermer explicitement, le recap prend directement la main. La liste
        // complete est transmise pour que le bouton RETOUR du recap puisse y revenir.
        resolved = true;
        DeathRecapPage.openHistorique(playerRef, entries.get(index).snapshot(), entries);
    }

    private void releaseInterface() {
        try {
            sendUpdate(new UICommandBuilder(), false);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Mise a jour de l'interface echouee: %s", e.getMessage());
        }
    }

    private void handleClose() {
        resolved = true;
        try {
            close();
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Fermeture de la page echouee: %s", e.getMessage());
            releaseInterface();
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        resolved = true;
        super.onDismiss(ref, store);
    }

    /**
     * Charge utile des evenements d'interface, meme raison que {@link DeathRecapPage.EventDataClass} :
     * {@link InteractiveCustomUIPage} est necessaire pour recevoir les clics en BSON structure.
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
