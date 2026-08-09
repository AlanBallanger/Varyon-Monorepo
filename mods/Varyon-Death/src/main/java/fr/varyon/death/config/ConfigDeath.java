package fr.varyon.death.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Configuration du mod, chargee depuis {@code config.json}.
 *
 * <p>Le fichier est lu avec Gson (analyseur JSON reel) : les valeurs absentes ou hors bornes
 * retombent sur les valeurs par defaut sans faire echouer le chargement. Un fichier complet
 * est ecrit au premier demarrage, mais il n'est jamais reecrit ensuite : les modifications
 * manuelles de l'administrateur sont donc preservees.
 */
public final class ConfigDeath {

    private static final HytaleLogger LOGGER =
            HytaleLogger.getLogger().getSubLogger("VaryonRevive-Config");
    private static final String NOM_FICHIER = "config.json";

    // --- Valeurs par defaut -------------------------------------------------
    private static final int DUREE_SAIGNEMENT_DEFAUT = 120;
    private static final int DUREE_ABANDON_DEFAUT = 3;
    /** Hauteur de la camera au-dessus du corps couche. */
    private static final double CAMERA_Y_DEFAUT = 2.2d;
    /** Recul de la camera derriere le joueur, pour le voir en entier. */
    private static final double CAMERA_DISTANCE_DEFAUT = 3.5d;
    private static final double CAMERA_DISTANCE_MIN = 0.0d;
    private static final double CAMERA_DISTANCE_MAX = 12.0d;
    private static final int DUREE_RELEVE_DEFAUT = 10;
    private static final int DUREE_RELEVE_MINEURE_DEFAUT = 12;
    private static final int DUREE_RELEVE_CLASSIQUE_DEFAUT = 9;
    private static final int DUREE_RELEVE_MAJEURE_DEFAUT = 6;
    private static final int DUREE_RELEVE_MYTHIQUE_DEFAUT = 1;
    private static final double DISTANCE_MAX_DEFAUT = 3.0d;
    private static final double TOLERANCE_MOUVEMENT_DEFAUT = 0.5d;
    private static final int PV_RENDUS_POURCENT_DEFAUT = 30;
    private static final boolean CARTE_ACTIVE_DEFAUT = true;

    // --- Bornes de securite -------------------------------------------------
    private static final int DUREE_MIN = 1;
    private static final int DUREE_MAX = 600;
    private static final double DISTANCE_MIN = 1.0d;
    private static final double DISTANCE_MAX = 20.0d;
    private static final double TOLERANCE_MIN = 0.1d;
    private static final double TOLERANCE_MAX = 5.0d;
    private static final int POURCENT_MIN = 1;
    private static final int POURCENT_MAX = 100;
    /** Journalisation detaillee de l'etat a terre, desactivee par defaut. */
    private static final boolean LOGS_DIAGNOSTIC_DEFAUT = false;

    /** Permission requise pour voir les marqueurs des joueurs a terre sur la carte. */
    public static final String PERMISSION_CARTE = "varyon.revive.carte";

    private final int dureeSaignementSecondes;
    private final int dureeAbandonSecondes;
    private final double cameraDecalageX;
    private final double cameraDecalageY;
    private final double cameraDecalageZ;
    private final double cameraDistance;
    private final int dureeReleveSecondes;
    private final int dureeReleveMineureSecondes;
    private final int dureeReleveClassiqueSecondes;
    private final int dureeReleveMajeureSecondes;
    private final int dureeReleveMythiqueSecondes;
    private final double distanceMaxBlocs;
    private final double toleranceMouvementBlocs;
    private final int pvRendusPourcent;
    private final boolean carteActive;
    private final boolean logsDiagnostic;

    private ConfigDeath(int dureeSaignementSecondes,
                         int dureeAbandonSecondes,
                         double cameraDecalageX,
                         double cameraDecalageY,
                         double cameraDecalageZ,
                         double cameraDistance,
                         int dureeReleveSecondes,
                         int dureeReleveMineureSecondes,
                         int dureeReleveClassiqueSecondes,
                         int dureeReleveMajeureSecondes,
                         int dureeReleveMythiqueSecondes,
                         double distanceMaxBlocs,
                         double toleranceMouvementBlocs,
                         int pvRendusPourcent,
                         boolean carteActive,
                         boolean logsDiagnostic) {
        this.dureeSaignementSecondes = borner(dureeSaignementSecondes, DUREE_MIN, DUREE_MAX, DUREE_SAIGNEMENT_DEFAUT);
        this.dureeAbandonSecondes = borner(dureeAbandonSecondes, DUREE_MIN, DUREE_MAX, DUREE_ABANDON_DEFAUT);
        this.cameraDecalageX = cameraDecalageX;
        this.cameraDecalageY = cameraDecalageY;
        this.cameraDecalageZ = cameraDecalageZ;
        this.cameraDistance =
                borner(cameraDistance, CAMERA_DISTANCE_MIN, CAMERA_DISTANCE_MAX, CAMERA_DISTANCE_DEFAUT);
        this.dureeReleveSecondes = borner(dureeReleveSecondes, DUREE_MIN, DUREE_MAX, DUREE_RELEVE_DEFAUT);
        this.dureeReleveMineureSecondes =
                borner(dureeReleveMineureSecondes, DUREE_MIN, DUREE_MAX, DUREE_RELEVE_MINEURE_DEFAUT);
        this.dureeReleveClassiqueSecondes =
                borner(dureeReleveClassiqueSecondes, DUREE_MIN, DUREE_MAX, DUREE_RELEVE_CLASSIQUE_DEFAUT);
        this.dureeReleveMajeureSecondes =
                borner(dureeReleveMajeureSecondes, DUREE_MIN, DUREE_MAX, DUREE_RELEVE_MAJEURE_DEFAUT);
        this.dureeReleveMythiqueSecondes =
                borner(dureeReleveMythiqueSecondes, DUREE_MIN, DUREE_MAX, DUREE_RELEVE_MYTHIQUE_DEFAUT);
        this.distanceMaxBlocs = borner(distanceMaxBlocs, DISTANCE_MIN, DISTANCE_MAX, DISTANCE_MAX_DEFAUT);
        this.toleranceMouvementBlocs =
                borner(toleranceMouvementBlocs, TOLERANCE_MIN, TOLERANCE_MAX, TOLERANCE_MOUVEMENT_DEFAUT);
        this.pvRendusPourcent = borner(pvRendusPourcent, POURCENT_MIN, POURCENT_MAX, PV_RENDUS_POURCENT_DEFAUT);
        this.carteActive = carteActive;
        this.logsDiagnostic = logsDiagnostic;
    }

    @Nonnull
    public static ConfigDeath defauts() {
        return new ConfigDeath(
                DUREE_SAIGNEMENT_DEFAUT,
                DUREE_ABANDON_DEFAUT,
                0.0d, CAMERA_Y_DEFAUT, 0.0d,
                CAMERA_DISTANCE_DEFAUT,
                DUREE_RELEVE_DEFAUT,
                DUREE_RELEVE_MINEURE_DEFAUT,
                DUREE_RELEVE_CLASSIQUE_DEFAUT,
                DUREE_RELEVE_MAJEURE_DEFAUT,
                DUREE_RELEVE_MYTHIQUE_DEFAUT,
                DISTANCE_MAX_DEFAUT,
                TOLERANCE_MOUVEMENT_DEFAUT,
                PV_RENDUS_POURCENT_DEFAUT,
                CARTE_ACTIVE_DEFAUT,
                LOGS_DIAGNOSTIC_DEFAUT);
    }

    /**
     * Charge la configuration, en ecrivant le fichier par defaut s'il n'existe pas encore.
     * Toute erreur de lecture retombe silencieusement sur les valeurs par defaut : un fichier
     * corrompu ne doit jamais empecher le serveur de demarrer.
     */
    @Nonnull
    public static ConfigDeath charger(@Nullable Path dossierDonnees) {
        Path chemin = cheminConfig(dossierDonnees);
        try {
            Path parent = chemin.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(chemin)) {
                ConfigDeath defauts = defauts();
                Files.writeString(chemin, defauts.versJson(), StandardCharsets.UTF_8);
                LOGGER.at(Level.INFO).log("Configuration par defaut ecrite dans %s", chemin);
                return defauts;
            }
            String brut = Files.readString(chemin, StandardCharsets.UTF_8);
            ConfigDeath config = depuisJson(brut);
            // Un fichier ecrit par une version anterieure ne contient pas les reglages ajoutes
            // depuis. On le reecrit avec les valeurs lues, completees par les defauts, pour que
            // les nouvelles options y soient visibles et modifiables.
            String aJour = config.versJson();
            if (!aJour.equals(brut)) {
                Files.writeString(chemin, aJour, StandardCharsets.UTF_8);
                LOGGER.at(Level.INFO).log("Configuration mise a jour dans %s", chemin);
            }
            return config;
        } catch (IOException | RuntimeException erreur) {
            LOGGER.at(Level.WARNING).log(
                    "Lecture de %s impossible (%s) : valeurs par defaut utilisees.",
                    chemin, erreur.getMessage());
            return defauts();
        }
    }

    @Nonnull
    public static Path cheminConfig(@Nullable Path dossierDonnees) {
        Path base = dossierDonnees == null ? Path.of("Varyon-Death") : dossierDonnees;
        return base.resolve(NOM_FICHIER);
    }

    @Nonnull
    private static ConfigDeath depuisJson(@Nullable String brut) {
        if (brut == null || brut.isBlank()) {
            return defauts();
        }
        JsonObject racine = JsonParser.parseString(brut).getAsJsonObject();
        JsonObject aTerre = objet(racine, "joueur_a_terre");
        JsonObject abandon = objet(aTerre, "abandon");
        JsonObject camera = objet(aTerre, "camera");
        JsonObject decalage = objet(camera, "decalage");
        JsonObject carte = objet(aTerre, "carte");
        JsonObject releve = objet(racine, "releve");
        JsonObject potions = objet(releve, "potions_resurrection");

        return new ConfigDeath(
                entier(aTerre, "duree_saignement", DUREE_SAIGNEMENT_DEFAUT),
                entier(abandon, "duree_maintien", DUREE_ABANDON_DEFAUT),
                decimal(decalage, "x", 0.0d),
                decimal(decalage, "y", CAMERA_Y_DEFAUT),
                decimal(decalage, "z", 0.0d),
                decimal(camera, "distance", CAMERA_DISTANCE_DEFAUT),
                entier(releve, "duree", DUREE_RELEVE_DEFAUT),
                entier(potions, "duree_mineure", DUREE_RELEVE_MINEURE_DEFAUT),
                entier(potions, "duree_classique", DUREE_RELEVE_CLASSIQUE_DEFAUT),
                entier(potions, "duree_majeure", DUREE_RELEVE_MAJEURE_DEFAUT),
                entier(potions, "duree_mythique", DUREE_RELEVE_MYTHIQUE_DEFAUT),
                decimal(releve, "distance_max", DISTANCE_MAX_DEFAUT),
                decimal(releve, "tolerance_mouvement", TOLERANCE_MOUVEMENT_DEFAUT),
                entier(releve, "pv_rendus_pourcent", PV_RENDUS_POURCENT_DEFAUT),
                booleen(carte, "icone_visible", CARTE_ACTIVE_DEFAUT),
                booleen(racine, "logs_diagnostic", LOGS_DIAGNOSTIC_DEFAUT));
    }

    @Nonnull
    private String versJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject racine = new JsonObject();

        JsonObject decalage = new JsonObject();
        decalage.addProperty("x", cameraDecalageX);
        decalage.addProperty("y", cameraDecalageY);
        decalage.addProperty("z", cameraDecalageZ);

        JsonObject camera = new JsonObject();
        camera.addProperty("distance", cameraDistance);
        camera.add("decalage", decalage);

        JsonObject abandon = new JsonObject();
        abandon.addProperty("duree_maintien", dureeAbandonSecondes);

        JsonObject carte = new JsonObject();
        carte.addProperty("icone_visible", carteActive);

        JsonObject aTerre = new JsonObject();
        aTerre.addProperty("duree_saignement", dureeSaignementSecondes);
        aTerre.add("abandon", abandon);
        aTerre.add("camera", camera);
        aTerre.add("carte", carte);

        JsonObject potions = new JsonObject();
        potions.addProperty("duree_mineure", dureeReleveMineureSecondes);
        potions.addProperty("duree_classique", dureeReleveClassiqueSecondes);
        potions.addProperty("duree_majeure", dureeReleveMajeureSecondes);
        potions.addProperty("duree_mythique", dureeReleveMythiqueSecondes);

        JsonObject releve = new JsonObject();
        releve.addProperty("duree", dureeReleveSecondes);
        releve.add("potions_resurrection", potions);
        releve.addProperty("distance_max", distanceMaxBlocs);
        releve.addProperty("tolerance_mouvement", toleranceMouvementBlocs);
        releve.addProperty("pv_rendus_pourcent", pvRendusPourcent);

        racine.add("joueur_a_terre", aTerre);
        racine.add("releve", releve);
        // Journalisation detaillee : a activer pour diagnostiquer, bruyant en production.
        racine.addProperty("logs_diagnostic", logsDiagnostic);
        return gson.toJson(racine) + System.lineSeparator();
    }

    // --- Lecture defensive --------------------------------------------------

    @Nonnull
    private static JsonObject objet(@Nullable JsonObject parent, @Nonnull String cle) {
        if (parent != null && parent.has(cle) && parent.get(cle).isJsonObject()) {
            return parent.getAsJsonObject(cle);
        }
        return new JsonObject();
    }

    private static int entier(@Nonnull JsonObject parent, @Nonnull String cle, int defaut) {
        try {
            return parent.has(cle) ? parent.get(cle).getAsInt() : defaut;
        } catch (RuntimeException ignore) {
            return defaut;
        }
    }

    private static double decimal(@Nonnull JsonObject parent, @Nonnull String cle, double defaut) {
        try {
            return parent.has(cle) ? parent.get(cle).getAsDouble() : defaut;
        } catch (RuntimeException ignore) {
            return defaut;
        }
    }

    private static boolean booleen(@Nonnull JsonObject parent, @Nonnull String cle, boolean defaut) {
        try {
            return parent.has(cle) ? parent.get(cle).getAsBoolean() : defaut;
        } catch (RuntimeException ignore) {
            return defaut;
        }
    }

    private static int borner(int valeur, int min, int max, int defaut) {
        return valeur < min || valeur > max ? defaut : valeur;
    }

    private static double borner(double valeur, double min, double max, double defaut) {
        return valeur < min || valeur > max ? defaut : valeur;
    }

    // --- Accesseurs ---------------------------------------------------------

    public int getDureeSaignementSecondes() {
        return dureeSaignementSecondes;
    }

    /** Duree du saignement convertie en ticks serveur (20 ticks par seconde). */
    public int getDureeSaignementTicks() {
        return dureeSaignementSecondes * 20;
    }

    public int getDureeAbandonSecondes() {
        return dureeAbandonSecondes;
    }

    public int getDureeAbandonTicks() {
        return dureeAbandonSecondes * 20;
    }

    public double getCameraDecalageX() {
        return cameraDecalageX;
    }

    public double getCameraDecalageY() {
        return cameraDecalageY;
    }

    public double getCameraDecalageZ() {
        return cameraDecalageZ;
    }

    /** Recul de la camera derriere le joueur a terre, en blocs. */
    public double getCameraDistance() {
        return cameraDistance;
    }

    public int getDureeReleveSecondes() {
        return dureeReleveSecondes;
    }

    /**
     * Duree de relevement pour UN soigneur, en ticks. Avec N soigneurs la progression
     * avance de N pas par tick : la duree effective est donc {@code duree / N}.
     */
    public int getDureeReleveTicks() {
        return dureeReleveSecondes * 20;
    }

    /** Duree de relevement, en ticks, quand une potion mineure fixe la duree de base. */
    public int getDureeReleveMineureTicks() {
        return dureeReleveMineureSecondes * 20;
    }

    /** Duree de relevement, en ticks, quand une potion classique fixe la duree de base. */
    public int getDureeReleveClassiqueTicks() {
        return dureeReleveClassiqueSecondes * 20;
    }

    /** Duree de relevement, en ticks, quand une potion majeure fixe la duree de base. */
    public int getDureeReleveMajeureTicks() {
        return dureeReleveMajeureSecondes * 20;
    }

    /** Duree de relevement, en ticks, quand une potion mythique fixe la duree de base. */
    public int getDureeReleveMythiqueTicks() {
        return dureeReleveMythiqueSecondes * 20;
    }

    public double getDistanceMaxBlocs() {
        return distanceMaxBlocs;
    }

    /** Carre de la distance maximale, pour comparer sans racine carree. */
    public double getDistanceMaxCarree() {
        return distanceMaxBlocs * distanceMaxBlocs;
    }

    public double getToleranceMouvementBlocs() {
        return toleranceMouvementBlocs;
    }

    public double getToleranceMouvementCarree() {
        return toleranceMouvementBlocs * toleranceMouvementBlocs;
    }

    /** Journalisation seconde par seconde de l'etat a terre. Utile pour diagnostiquer. */
    public boolean isLogsDiagnostic() {
        return logsDiagnostic;
    }

    public int getPvRendusPourcent() {
        return pvRendusPourcent;
    }

    public boolean isCarteActive() {
        return carteActive;
    }
}
