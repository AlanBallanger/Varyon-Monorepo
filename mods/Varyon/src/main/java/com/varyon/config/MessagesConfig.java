package com.varyon.config;

import com.hypixel.hytale.logger.HytaleLogger;
import com.moandjiezana.toml.Toml;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Path;
import java.util.logging.Level;

public class MessagesConfig {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private HudMessages hud;
    private ExtractionMessages extraction;
    private ReturnMessages returnMessages;
    private SafeZoneMessages safeZone;
    private RtpMessages rtp;
    private PointsMessages points;

    public static class HudMessages {
        public String labelHealth;
        public String labelDamage;
        public String labelLoot;
        public String labelMultipliers;

        public HudMessages(String labelHealth, String labelDamage, String labelLoot, String labelMultipliers) {
            this.labelHealth = labelHealth;
            this.labelDamage = labelDamage;
            this.labelLoot = labelLoot;
            this.labelMultipliers = labelMultipliers;
        }
    }

    public static class ExtractionMessages {
        public String portalSpawned;
        public String portalExpired;
        public String alreadyHasPortal;
        public String cooldown;
        public String noSafeLocation;
        public String teleporting;
        public String notYourPortal;
        public String error;

        public ExtractionMessages(String portalSpawned, String portalExpired, String alreadyHasPortal,
                                  String cooldown, String noSafeLocation, String teleporting,
                                  String notYourPortal, String error) {
            this.portalSpawned = portalSpawned;
            this.portalExpired = portalExpired;
            this.alreadyHasPortal = alreadyHasPortal;
            this.cooldown = cooldown;
            this.noSafeLocation = noSafeLocation;
            this.teleporting = teleporting;
            this.notYourPortal = notYourPortal;
            this.error = error;
        }
    }

    public static class ReturnMessages {
        public String success;
        public String cooldown;
        public String noDeathPoint;
        public String expired;
        public String alreadyUsed;
        public String teleporting;
        public String noSafeLocation;
        public String error;
        public String deathPointInfo;
        public String firstUseWarning;
        public String cooldownWarning;

        public ReturnMessages(String success, String cooldown, String noDeathPoint, String expired,
                              String alreadyUsed, String teleporting, String noSafeLocation,
                              String error, String deathPointInfo, String firstUseWarning, String cooldownWarning) {
            this.success = success;
            this.cooldown = cooldown;
            this.noDeathPoint = noDeathPoint;
            this.expired = expired;
            this.alreadyUsed = alreadyUsed;
            this.teleporting = teleporting;
            this.noSafeLocation = noSafeLocation;
            this.error = error;
            this.deathPointInfo = deathPointInfo;
            this.firstUseWarning = firstUseWarning;
            this.cooldownWarning = cooldownWarning;
        }
    }

    public static class SafeZoneMessages {
        public String enterSafeTitle;
        public String enterSafeSubtitle;
        public String enterPvpTitle;
        public String enterPvpSubtitle;
        public String pvpDisabled;
        public String pvpEnabled;
        public String rotation;
        public String overlapStart;
        public String overlapEnd;
        public String timeRemaining;

        public SafeZoneMessages(String enterSafeTitle, String enterSafeSubtitle,
                                String enterPvpTitle, String enterPvpSubtitle,
                                String pvpDisabled, String pvpEnabled, String rotation,
                                String overlapStart, String overlapEnd, String timeRemaining) {
            this.enterSafeTitle = enterSafeTitle;
            this.enterSafeSubtitle = enterSafeSubtitle;
            this.enterPvpTitle = enterPvpTitle;
            this.enterPvpSubtitle = enterPvpSubtitle;
            this.pvpDisabled = pvpDisabled;
            this.pvpEnabled = pvpEnabled;
            this.rotation = rotation;
            this.overlapStart = overlapStart;
            this.overlapEnd = overlapEnd;
            this.timeRemaining = timeRemaining;
        }
    }

    public static class RtpMessages {
        public String teleporting;
        public String success;
        public String noSafeLocation;
        public String zoneNotFound;
        public String availableZones;
        public String worldNotSupported;
        public String randomZone;
        public String error;
        public String noPermission;

        public RtpMessages(String teleporting, String success, String noSafeLocation,
                           String zoneNotFound, String availableZones, String worldNotSupported,
                           String randomZone, String error, String noPermission) {
            this.teleporting = teleporting;
            this.success = success;
            this.noSafeLocation = noSafeLocation;
            this.zoneNotFound = zoneNotFound;
            this.availableZones = availableZones;
            this.worldNotSupported = worldNotSupported;
            this.randomZone = randomZone;
            this.error = error;
            this.noPermission = noPermission;
        }
    }

    public static class PointsMessages {
        public String balanceInfo;
        public String given;
        public String taken;
        public String maxSet;
        public String deposited;
        public String noFaction;
        public String notEnough;

        public PointsMessages(String balanceInfo, String given, String taken, String maxSet,
                               String deposited, String noFaction, String notEnough) {
            this.balanceInfo = balanceInfo;
            this.given = given;
            this.taken = taken;
            this.maxSet = maxSet;
            this.deposited = deposited;
            this.noFaction = noFaction;
            this.notEnough = notEnough;
        }
    }

    public MessagesConfig(HudMessages hud, ExtractionMessages extraction, ReturnMessages returnMessages,
                          SafeZoneMessages safeZone, RtpMessages rtp, PointsMessages points) {
        this.hud = hud;
        this.extraction = extraction;
        this.returnMessages = returnMessages;
        this.safeZone = safeZone;
        this.rtp = rtp;
        this.points = points;
    }

    @Nonnull
    public static MessagesConfig load(@Nonnull Path configPath) {
        File configFile = configPath.resolve("messages.toml").toFile();
        if (!configFile.exists()) {
            LOGGER.at(Level.INFO).log("messages.toml not found, creating default");
            MessagesConfig def = createDefault();
            def.save(configPath);
            return def;
        }
        try {
            Toml toml = new Toml().read(configFile);

            Toml hudToml = toml.getTable("hud");
            HudMessages hud = new HudMessages(
                hudToml != null ? hudToml.getString("labelHealth", "HP") : "HP",
                hudToml != null ? hudToml.getString("labelDamage", "DMG") : "DMG",
                hudToml != null ? hudToml.getString("labelLoot", "Loot") : "Loot",
                hudToml != null ? hudToml.getString("labelMultipliers", "Multiplicateurs") : "Multiplicateurs"
            );

            Toml extractToml = toml.getTable("extraction");
            ExtractionMessages extraction = new ExtractionMessages(
                extractToml.getString("portalSpawned", "Portail d'extraction créé à {distance}m ({x}, {y}, {z}) ! Il reste actif {duration} s."),
                extractToml.getString("portalExpired", "Votre portail d'extraction a expiré."),
                extractToml.getString("alreadyHasPortal", "Vous avez déjà un portail actif"),
                extractToml.getString("cooldown", "Cooldown actif. Temps restant: {remaining} secondes"),
                extractToml.getString("noSafeLocation", "Impossible de trouver un emplacement sûr"),
                extractToml.getString("teleporting", "Téléportation vers le spawn..."),
                extractToml.getString("notYourPortal", "Ce portail ne vous appartient pas"),
                extractToml.getString("error", "Erreur lors de la création du portail")
            );

            Toml returnToml = toml.getTable("return");
            ReturnMessages returnMsg = new ReturnMessages(
                returnToml != null ? returnToml.getString("success", "Téléporté près de votre point de mort à {distance}m ({x}, {y}, {z})") : "Téléporté près de votre point de mort à {distance}m ({x}, {y}, {z})",
                returnToml != null ? returnToml.getString("cooldown", "Cooldown actif. Temps restant: {remaining} secondes") : "Cooldown actif. Temps restant: {remaining} secondes",
                returnToml != null ? returnToml.getString("noDeathPoint", "Aucun point de mort enregistré") : "Aucun point de mort enregistré",
                returnToml != null ? returnToml.getString("expired", "Votre point de mort a expiré") : "Votre point de mort a expiré",
                returnToml != null ? returnToml.getString("alreadyUsed", "Vous avez déjà utilisé votre téléportation pour cette mort") : "Vous avez déjà utilisé votre téléportation pour cette mort",
                returnToml != null ? returnToml.getString("teleporting", "Recherche d'un emplacement sûr près de votre point de mort...") : "Recherche d'un emplacement sûr près de votre point de mort...",
                returnToml != null ? returnToml.getString("noSafeLocation", "Impossible de trouver un emplacement sûr après {attempts} tentatives") : "Impossible de trouver un emplacement sûr après {attempts} tentatives",
                returnToml != null ? returnToml.getString("error", "Erreur lors de la téléportation") : "Erreur lors de la téléportation",
                returnToml != null ? returnToml.getString("deathPointInfo", "Point de mort enregistré : ({x}, {y}, {z}) — {world}") : "Point de mort enregistré : ({x}, {y}, {z}) — {world}",
                returnToml != null ? returnToml.getString("firstUseWarning", "⚠ ATTENTION: Vous ne pourrez utiliser /return qu'UNE SEULE FOIS pour cette mort!") : "⚠ ATTENTION: Vous ne pourrez utiliser /return qu'UNE SEULE FOIS pour cette mort!",
                returnToml != null ? returnToml.getString("cooldownWarning", "⏳ Cooldown encore actif ({remaining}s) : prochain retour {cost} coins (×{multiplier}). Ouvrez /return pour valider le montant et confirmer.") : "⏳ Cooldown encore actif ({remaining}s) : prochain retour {cost} coins (×{multiplier}). Ouvrez /return pour valider le montant et confirmer."
            );

            Toml safeToml = toml.getTable("safezone");
            SafeZoneMessages safeZone = new SafeZoneMessages(
                safeToml.getString("enterTitle", "Zone non-PvP"),
                safeToml.getString("enterSubtitle", "Vous êtes en sécurité"),
                safeToml.getString("enterPvpTitle", "Zone PvP"),
                safeToml.getString("enterPvpSubtitle", "Attention !"),
                safeToml.getString("pvpDisabled", "[PvP] Vous êtes dans une zone non-PvP!"),
                safeToml.getString("pvpEnabled", "[PvP] Vous êtes dans une zone PvP!"),
                safeToml.getString("rotation", "[PvP] La zone non-PvP est maintenant au {direction}!"),
                safeToml.getString("overlapStart", "[PvP] Double zone non-PvP active pendant {minutes} minutes!"),
                safeToml.getString("overlapEnd", "[PvP] Fin de la double zone non-PvP!"),
                safeToml.getString("timeRemaining", "[PvP] Rotation dans {minutes} minutes")
            );

            Toml rtpToml = toml.getTable("rtp");
            RtpMessages rtp = new RtpMessages(
                rtpToml.getString("teleporting", "Téléportation en cours..."),
                rtpToml.getString("success", "Téléporté en {zone} à ({x}, {y}, {z})"),
                rtpToml.getString("noSafeLocation", "Impossible de trouver un emplacement sûr après {attempts} tentatives"),
                rtpToml.getString("zoneNotFound", "Zone '{zone}' introuvable."),
                rtpToml.getString("availableZones", "Zones disponibles"),
                rtpToml.getString("worldNotSupported", "Ce monde ne supporte pas la téléportation"),
                rtpToml.getString("randomZone", "zone aléatoire"),
                rtpToml.getString("error", "Erreur lors de la téléportation"),
                rtpToml.getString("noPermission", "Vous n'avez pas la permission pour cette zone. Permission requise: {permission}")
            );

            Toml pointsToml = toml.getTable("points");
            if (pointsToml == null) {
                pointsToml = toml.getTable("essence");
            }
            PointsMessages points = new PointsMessages(
                pointsToml.getString("balanceInfo", "Points de faction : {current}/{max} | Faction : {faction} | Ta faction sur la jauge : {global}/{gaugeMax}"),
                pointsToml.getString("given", "Don de {amount} points de faction à {player} effectué"),
                pointsToml.getString("taken", "Retrait de {amount} points de faction de {player} effectué"),
                pointsToml.getString("maxSet", "Points de faction de {player} : plafond fixé à {max}"),
                pointsToml.getString("deposited", "Déposé {amount} points de faction pour {faction}. Le nombre de points de ta faction est monté à {global}/{gaugeMax}"),
                pointsToml.getString("noFaction", "Vous devez rejoindre une faction d'abord (/varyon faction <nom>)"),
                pointsToml.getString("notEnough", "Vous n'avez pas assez de points de faction")
            );

            return new MessagesConfig(hud, extraction, returnMsg, safeZone, rtp, points);

        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to load messages.toml, using defaults: " + e.getMessage());
            return createDefault();
        }
    }

    public void save(@Nonnull Path configPath) {
        try {
            File configFile = configPath.resolve("messages.toml").toFile();
            configFile.getParentFile().mkdirs();

            StringBuilder sb = new StringBuilder();

            sb.append("[hud]\n");
            sb.append("labelHealth = \"").append(hud.labelHealth).append("\"\n");
            sb.append("labelDamage = \"").append(hud.labelDamage).append("\"\n");
            sb.append("labelLoot = \"").append(hud.labelLoot).append("\"\n");
            sb.append("labelMultipliers = \"").append(hud.labelMultipliers).append("\"\n\n");

            sb.append("[extraction]\n");
            sb.append("portalSpawned = \"").append(extraction.portalSpawned).append("\"\n");
            sb.append("portalExpired = \"").append(extraction.portalExpired).append("\"\n");
            sb.append("alreadyHasPortal = \"").append(extraction.alreadyHasPortal).append("\"\n");
            sb.append("cooldown = \"").append(extraction.cooldown).append("\"\n");
            sb.append("noSafeLocation = \"").append(extraction.noSafeLocation).append("\"\n");
            sb.append("teleporting = \"").append(extraction.teleporting).append("\"\n");
            sb.append("notYourPortal = \"").append(extraction.notYourPortal).append("\"\n");
            sb.append("error = \"").append(extraction.error).append("\"\n\n");

            sb.append("[return]\n");
            sb.append("success = \"").append(returnMessages.success).append("\"\n");
            sb.append("cooldown = \"").append(returnMessages.cooldown).append("\"\n");
            sb.append("noDeathPoint = \"").append(returnMessages.noDeathPoint).append("\"\n");
            sb.append("expired = \"").append(returnMessages.expired).append("\"\n");
            sb.append("alreadyUsed = \"").append(returnMessages.alreadyUsed).append("\"\n");
            sb.append("teleporting = \"").append(returnMessages.teleporting).append("\"\n");
            sb.append("noSafeLocation = \"").append(returnMessages.noSafeLocation).append("\"\n");
            sb.append("error = \"").append(returnMessages.error).append("\"\n");
            sb.append("deathPointInfo = \"").append(returnMessages.deathPointInfo).append("\"\n");
            sb.append("firstUseWarning = \"").append(returnMessages.firstUseWarning).append("\"\n");
            sb.append("cooldownWarning = \"").append(returnMessages.cooldownWarning).append("\"\n\n");

            sb.append("[safezone]\n");
            sb.append("enterTitle = \"").append(safeZone.enterSafeTitle).append("\"\n");
            sb.append("enterSubtitle = \"").append(safeZone.enterSafeSubtitle).append("\"\n");
            sb.append("enterPvpTitle = \"").append(safeZone.enterPvpTitle).append("\"\n");
            sb.append("enterPvpSubtitle = \"").append(safeZone.enterPvpSubtitle).append("\"\n");
            sb.append("pvpDisabled = \"").append(safeZone.pvpDisabled).append("\"\n");
            sb.append("pvpEnabled = \"").append(safeZone.pvpEnabled).append("\"\n");
            sb.append("rotation = \"").append(safeZone.rotation).append("\"\n");
            sb.append("overlapStart = \"").append(safeZone.overlapStart).append("\"\n");
            sb.append("overlapEnd = \"").append(safeZone.overlapEnd).append("\"\n");
            sb.append("timeRemaining = \"").append(safeZone.timeRemaining).append("\"\n\n");

            sb.append("[rtp]\n");
            sb.append("teleporting = \"").append(rtp.teleporting).append("\"\n");
            sb.append("success = \"").append(rtp.success).append("\"\n");
            sb.append("noSafeLocation = \"").append(rtp.noSafeLocation).append("\"\n");
            sb.append("zoneNotFound = \"").append(rtp.zoneNotFound).append("\"\n");
            sb.append("availableZones = \"").append(rtp.availableZones).append("\"\n");
            sb.append("worldNotSupported = \"").append(rtp.worldNotSupported).append("\"\n");
            sb.append("randomZone = \"").append(rtp.randomZone).append("\"\n");
            sb.append("error = \"").append(rtp.error).append("\"\n");
            sb.append("noPermission = \"").append(rtp.noPermission).append("\"\n\n");

            sb.append("[points]\n");
            sb.append("balanceInfo = \"").append(points.balanceInfo).append("\"\n");
            sb.append("given = \"").append(points.given).append("\"\n");
            sb.append("taken = \"").append(points.taken).append("\"\n");
            sb.append("maxSet = \"").append(points.maxSet).append("\"\n");
            sb.append("deposited = \"").append(points.deposited).append("\"\n");
            sb.append("noFaction = \"").append(points.noFaction).append("\"\n");
            sb.append("notEnough = \"").append(points.notEnough).append("\"\n");

            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(sb.toString());
            }
            LOGGER.at(Level.INFO).log("Messages configuration saved");

        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to save messages.toml: " + e.getMessage());
        }
    }

    @Nonnull
    public static MessagesConfig createDefault() {
        HudMessages hud = new HudMessages("HP", "DMG", "Loot", "Multiplicateurs");

        ExtractionMessages extraction = new ExtractionMessages(
            "Portail d'extraction créé à {distance}m ({x}, {y}, {z}) ! Il reste actif {duration} s.",
            "Votre portail d'extraction a expiré.",
            "Vous avez déjà un portail actif",
            "Cooldown actif. Temps restant: {remaining} secondes",
            "Impossible de trouver un emplacement sûr",
            "Téléportation vers le spawn...",
            "Ce portail ne vous appartient pas",
            "Erreur lors de la création du portail"
        );

        ReturnMessages returnMsg = new ReturnMessages(
            "Téléporté près de votre point de mort à {distance}m ({x}, {y}, {z})",
            "Cooldown actif. Temps restant: {remaining} secondes",
            "Aucun point de mort enregistré",
            "Votre point de mort a expiré",
            "Vous avez déjà utilisé votre téléportation pour cette mort",
            "Recherche d'un emplacement sûr près de votre point de mort...",
            "Impossible de trouver un emplacement sûr après {attempts} tentatives",
            "Erreur lors de la téléportation",
            "Point de mort enregistré : ({x}, {y}, {z}) — {world}",
            "⚠ ATTENTION: Vous ne pourrez utiliser /return qu'UNE SEULE FOIS pour cette mort!",
            "⏳ Cooldown encore actif ({remaining}s) : prochain retour {cost} coins (×{multiplier}). Ouvrez /return pour valider le montant et confirmer."
        );

        SafeZoneMessages safeZone = new SafeZoneMessages(
            "Zone non-PvP",
            "Vous êtes en sécurité",
            "Zone PvP",
            "Attention !",
            "[PvP] Vous êtes dans une zone non-PvP!",
            "[PvP] Vous êtes dans une zone PvP!",
            "[PvP] La zone non-PvP est maintenant au {direction}!",
            "[PvP] Double zone non-PvP active pendant {minutes} minutes!",
            "[PvP] Fin de la double zone non-PvP!",
            "[PvP] Rotation dans {minutes} minutes"
        );

        RtpMessages rtp = new RtpMessages(
            "Téléportation en cours...",
            "Téléporté en {zone} à ({x}, {y}, {z})",
            "Impossible de trouver un emplacement sûr après {attempts} tentatives",
            "Zone '{zone}' introuvable.",
            "Zones disponibles",
            "Ce monde ne supporte pas la téléportation",
            "zone aléatoire",
            "Erreur lors de la téléportation",
            "Vous n'avez pas la permission pour cette zone. Permission requise: {permission}"
        );

        PointsMessages points = new PointsMessages(
            "Points de faction : {current}/{max} | Faction : {faction} | Ta faction sur la jauge : {global}/{gaugeMax}",
            "Don de {amount} points de faction à {player} effectué",
            "Retrait de {amount} points de faction de {player} effectué",
            "Points de faction de {player} : plafond fixé à {max}",
            "Déposé {amount} points de faction pour {faction}. Le nombre de points de ta faction est monté à {global}/{gaugeMax}",
            "Vous devez rejoindre une faction d'abord (/varyon faction <nom>)",
            "Vous n'avez pas assez de points de faction"
        );

        return new MessagesConfig(hud, extraction, returnMsg, safeZone, rtp, points);
    }

    @Nonnull public HudMessages getHud()             { return hud; }
    @Nonnull public ExtractionMessages getExtraction(){ return extraction; }
    @Nonnull public ReturnMessages getReturn()        { return returnMessages; }
    @Nonnull public SafeZoneMessages getSafeZone()    { return safeZone; }
    @Nonnull public RtpMessages getRtp()              { return rtp; }
    @Nonnull public PointsMessages getPoints()       { return points; }
}
