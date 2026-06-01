package fr.varyon.holograms.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import fr.varyon.holograms.gui.HologramEditorPage;
import fr.varyon.holograms.gui.HologramListPage;
import fr.varyon.holograms.gui.HologramTestPage;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.holograms.VaryonHologramsPlugin;
import fr.varyon.holograms.hologram.HologramLineType;
import fr.varyon.holograms.hologram.Hologram;
import org.joml.Vector3d;
import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public final class HologramCommand extends AbstractCommandCollection {

    private static final String GOLD   = "#FFAA00";
    private static final String YELLOW = "#FFFF55";
    private static final String GRAY   = "#AAAAAA";
    private static final String WHITE  = "#FFFFFF";
    private static final String GREEN  = "#55FF55";
    private static final String RED    = "#FF5555";

    public HologramCommand(@Nonnull VaryonHologramsPlugin plugin) {
        super("hologram", "Gérer les hologrammes");
        addAliases(new String[]{"holo", "hg"});
        addSubCommand(new CreateCommand(plugin));
        addSubCommand(new EditCommand(plugin));
        addSubCommand(new DeleteCommand(plugin));
        addSubCommand(new ListCommand(plugin));
        addSubCommand(new TestUiCommand());
        addSubCommand(new CloseUiCommand());
        addSubCommand(new MoveToCommand(plugin));
        addSubCommand(new MoveHereCommand(plugin));
        addSubCommand(new AddLineCommand(plugin));
        addSubCommand(new SetLineCommand(plugin));
        addSubCommand(new RemoveLineCommand(plugin));
        addSubCommand(new InfoCommand(plugin));
        addSubCommand(new ReloadCommand(plugin));
        addSubCommand(new CleanupCommand(plugin));
        addSubCommand(new AnimListCommand());
        addSubCommand(new EntityResendCommand(plugin));
    }

    @Override
    protected boolean canGeneratePermission() { return false; }

    static boolean perm(@Nonnull CommandContext context, @Nonnull String permission) {
        if (context.sender() instanceof ConsoleSender) return true;
        return context.sender().hasPermission("*") || context.sender().hasPermission(permission);
    }

    private static class CreateCommand extends AbstractPlayerCommand {
        private final VaryonHologramsPlugin plugin;
        private final RequiredArg<String> nameArg;

        CreateCommand(@Nonnull VaryonHologramsPlugin plugin) {
            super("create", "Créer un hologramme à votre position");
            this.plugin = plugin;
            this.nameArg = withRequiredArg("nom", "Nom de l'hologramme", (ArgumentType<String>) ArgTypes.STRING);
        }

        @Override protected boolean canGeneratePermission() { return false; }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            if (!perm(context, "varyon.holograms.create")) {
                context.sendMessage(Message.raw("Permission refusée.").color(RED)); return;
            }
            String name = nameArg.get(context);
            if (plugin.getHologramManager().hologramExists(name)) {
                context.sendMessage(Message.raw("Un hologramme '" + name + "' existe déjà.").color(RED)); return;
            }
            try {
                Vector3d pos = playerRef.getTransform().getPosition();
                plugin.getHologramManager().createHologram(name,
                    new Vector3d(pos.x, pos.y + 1.5, pos.z),
                    world.getWorldConfig().getUuid(), playerRef.getUuid());
                context.sendMessage(Message.raw("Hologramme '").color(GREEN)
                    .insert(Message.raw(name).color(YELLOW))
                    .insert(Message.raw("' créé.").color(GREEN)));
            } catch (Exception e) {
                context.sendMessage(Message.raw(e.getMessage()).color(RED));
            }
        }
    }

    private static class EditCommand extends AbstractPlayerCommand {
        private final VaryonHologramsPlugin plugin;
        private final RequiredArg<String> nameArg;

        EditCommand(@Nonnull VaryonHologramsPlugin plugin) {
            super("edit", "Ouvrir l'éditeur GUI d'un hologramme");
            this.plugin = plugin;
            this.nameArg = withRequiredArg("nom", "Nom de l'hologramme", (ArgumentType<String>) ArgTypes.STRING);
        }

        @Override protected boolean canGeneratePermission() { return false; }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            if (!perm(context, "varyon.holograms.edit")) {
                context.sendMessage(Message.raw("Permission refusée.").color(RED)); return;
            }
            String name = nameArg.get(context);
            Hologram h = plugin.getHologramManager().getHologram(name);
            if (h == null) { context.sendMessage(Message.raw("Hologramme '" + name + "' introuvable.").color(RED)); return; }
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            player.getPageManager().openCustomPage(ref, store, new HologramEditorPage(playerRef, plugin, h));
        }
    }

    private static class DeleteCommand extends CommandBase {
        private final VaryonHologramsPlugin plugin;
        private final RequiredArg<String> nameArg;

        DeleteCommand(@Nonnull VaryonHologramsPlugin plugin) {
            super("delete", "Supprimer un hologramme");
            this.plugin = plugin;
            this.nameArg = withRequiredArg("nom", "Nom de l'hologramme", (ArgumentType<String>) ArgTypes.STRING);
            addAliases(new String[]{"remove", "del"});
        }

        @Override protected boolean canGeneratePermission() { return false; }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (!perm(context, "varyon.holograms.delete")) {
                context.sendMessage(Message.raw("Permission refusée.").color(RED)); return;
            }
            String name = nameArg.get(context);
            if (plugin.getHologramManager().deleteHologram(name)) {
                context.sendMessage(Message.raw("Hologramme '").color(GREEN).insert(Message.raw(name).color(YELLOW)).insert(Message.raw("' supprimé.").color(GREEN)));
            } else {
                context.sendMessage(Message.raw("Hologramme '" + name + "' introuvable.").color(RED));
            }
        }
    }

    private static class TestUiCommand extends AbstractPlayerCommand {
        TestUiCommand() {
            super("testui", "Ouvrir une page UI minimale de diagnostic");
        }

        @Override protected boolean canGeneratePermission() { return false; }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            player.getPageManager().openCustomPage(ref, store, new HologramTestPage(playerRef));
            context.sendMessage(Message.raw("Page test ouverte.").color(GREEN));
        }
    }

    private static class CloseUiCommand extends AbstractPlayerCommand {
        CloseUiCommand() {
            super("close", "Fermer l'interface hologramme ouverte");
            addAliases(new String[]{"fermer", "ui"});
        }

        @Override protected boolean canGeneratePermission() { return false; }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            player.getPageManager().setPage(ref, store, Page.None);
            context.sendMessage(Message.raw("Interface fermée.").color(GREEN));
        }
    }

    private static class ListCommand extends AbstractPlayerCommand {
        private final VaryonHologramsPlugin plugin;

        ListCommand(@Nonnull VaryonHologramsPlugin plugin) {
            super("list", "Ouvrir l'interface de gestion des hologrammes");
            this.plugin = plugin;
        }

        @Override protected boolean canGeneratePermission() { return false; }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            if (!perm(context, "varyon.holograms.list")) {
                context.sendMessage(Message.raw("Permission refusée.").color(RED)); return;
            }
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            player.getPageManager().openCustomPage(ref, store, new HologramListPage(playerRef, plugin));
        }
    }

    private static class MoveToCommand extends AbstractPlayerCommand {
        private final VaryonHologramsPlugin plugin;
        private final RequiredArg<String> nameArg;

        MoveToCommand(@Nonnull VaryonHologramsPlugin plugin) {
            super("moveto", "Se téléporter à un hologramme");
            this.plugin = plugin;
            this.nameArg = withRequiredArg("nom", "Nom de l'hologramme", (ArgumentType<String>) ArgTypes.STRING);
            addAliases(new String[]{"tp", "goto"});
        }

        @Override protected boolean canGeneratePermission() { return false; }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            if (!perm(context, "varyon.holograms.move")) {
                context.sendMessage(Message.raw("Permission refusée.").color(RED)); return;
            }
            String name = nameArg.get(context);
            Hologram h = plugin.getHologramManager().getHologram(name);
            if (h == null) { context.sendMessage(Message.raw("Hologramme '" + name + "' introuvable.").color(RED)); return; }
            Vector3d pos = h.getPosition();
            store.addComponent(ref, Teleport.getComponentType(),
                Teleport.createForPlayer(world, new Vector3d(pos), Rotation3f.ZERO));
            context.sendMessage(Message.raw("Téléporté à '").color(GREEN).insert(Message.raw(name).color(YELLOW)).insert(Message.raw("'.").color(GREEN)));
        }
    }

    private static class MoveHereCommand extends AbstractPlayerCommand {
        private final VaryonHologramsPlugin plugin;
        private final RequiredArg<String> nameArg;

        MoveHereCommand(@Nonnull VaryonHologramsPlugin plugin) {
            super("movehere", "Déplacer un hologramme ici");
            this.plugin = plugin;
            this.nameArg = withRequiredArg("nom", "Nom de l'hologramme", (ArgumentType<String>) ArgTypes.STRING);
            addAliases(new String[]{"move"});
        }

        @Override protected boolean canGeneratePermission() { return false; }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            if (!perm(context, "varyon.holograms.move")) {
                context.sendMessage(Message.raw("Permission refusée.").color(RED)); return;
            }
            String name = nameArg.get(context);
            Hologram h = plugin.getHologramManager().getHologram(name);
            if (h == null) { context.sendMessage(Message.raw("Hologramme '" + name + "' introuvable.").color(RED)); return; }
            Vector3d pos = playerRef.getTransform().getPosition();
            plugin.getHologramManager().moveHologram(h, new Vector3d(pos.x, pos.y + 2.5, pos.z));
            context.sendMessage(Message.raw("Hologramme '").color(GREEN).insert(Message.raw(name).color(YELLOW)).insert(Message.raw("' déplacé ici.").color(GREEN)));
        }
    }

    private static class AddLineCommand extends CommandBase {
        private final VaryonHologramsPlugin plugin;
        private final RequiredArg<String> nameArg;
        private final RequiredArg<String> textArg;

        AddLineCommand(@Nonnull VaryonHologramsPlugin plugin) {
            super("addline", "Ajouter une ligne à un hologramme");
            this.plugin = plugin;
            this.nameArg = withRequiredArg("nom", "Nom de l'hologramme", (ArgumentType<String>) ArgTypes.STRING);
            this.textArg = withRequiredArg("texte", "Texte à ajouter", (ArgumentType<String>) ArgTypes.GREEDY_STRING);
        }

        @Override protected boolean canGeneratePermission() { return false; }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (!perm(context, "varyon.holograms.edit")) {
                context.sendMessage(Message.raw("Permission refusée.").color(RED)); return;
            }
            String name = nameArg.get(context);
            Hologram h = plugin.getHologramManager().getHologram(name);
            if (h == null) { context.sendMessage(Message.raw("Hologramme '" + name + "' introuvable.").color(RED)); return; }
            String line = HologramLineType.extractSpecialLine(textArg.get(context).trim());
            h.addLine(line);
            plugin.getHologramManager().updateHologram(h);
            context.sendMessage(Message.raw("Ligne ajoutée à '").color(GREEN).insert(Message.raw(name).color(YELLOW)).insert(Message.raw("'.").color(GREEN)));
        }
    }

    private static class SetLineCommand extends CommandBase {
        private final VaryonHologramsPlugin plugin;
        private final RequiredArg<String> nameArg;
        private final RequiredArg<Integer> lineArg;
        private final RequiredArg<String> textArg;

        SetLineCommand(@Nonnull VaryonHologramsPlugin plugin) {
            super("setline", "Modifier une ligne d'un hologramme");
            this.plugin = plugin;
            this.nameArg = withRequiredArg("nom", "Nom de l'hologramme", (ArgumentType<String>) ArgTypes.STRING);
            this.lineArg = withRequiredArg("ligne", "Numéro de ligne (à partir de 1)", (ArgumentType<Integer>) ArgTypes.INTEGER);
            this.textArg = withRequiredArg("texte", "Nouveau texte", (ArgumentType<String>) ArgTypes.GREEDY_STRING);
        }

        @Override protected boolean canGeneratePermission() { return false; }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (!perm(context, "varyon.holograms.edit")) {
                context.sendMessage(Message.raw("Permission refusée.").color(RED)); return;
            }
            String name = nameArg.get(context);
            int idx = lineArg.get(context) - 1;
            if (idx < 0) { context.sendMessage(Message.raw("Numéro de ligne invalide.").color(RED)); return; }
            Hologram h = plugin.getHologramManager().getHologram(name);
            if (h == null) { context.sendMessage(Message.raw("Hologramme '" + name + "' introuvable.").color(RED)); return; }
            if (idx >= h.getLineCount()) { context.sendMessage(Message.raw("Ligne hors limites (1-" + h.getLineCount() + ").").color(RED)); return; }
            h.setLine(idx, textArg.get(context));
            plugin.getHologramManager().updateHologram(h);
            context.sendMessage(Message.raw("Ligne " + (idx + 1) + " modifiée.").color(GREEN));
        }
    }

    private static class RemoveLineCommand extends CommandBase {
        private final VaryonHologramsPlugin plugin;
        private final RequiredArg<String> nameArg;
        private final RequiredArg<Integer> lineArg;

        RemoveLineCommand(@Nonnull VaryonHologramsPlugin plugin) {
            super("removeline", "Supprimer une ligne d'un hologramme");
            this.plugin = plugin;
            this.nameArg = withRequiredArg("nom", "Nom de l'hologramme", (ArgumentType<String>) ArgTypes.STRING);
            this.lineArg = withRequiredArg("ligne", "Numéro de ligne (à partir de 1)", (ArgumentType<Integer>) ArgTypes.INTEGER);
            addAliases(new String[]{"delline"});
        }

        @Override protected boolean canGeneratePermission() { return false; }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (!perm(context, "varyon.holograms.edit")) {
                context.sendMessage(Message.raw("Permission refusée.").color(RED)); return;
            }
            String name = nameArg.get(context);
            int idx = lineArg.get(context) - 1;
            if (idx < 0) { context.sendMessage(Message.raw("Numéro de ligne invalide.").color(RED)); return; }
            Hologram h = plugin.getHologramManager().getHologram(name);
            if (h == null) { context.sendMessage(Message.raw("Hologramme '" + name + "' introuvable.").color(RED)); return; }
            if (idx >= h.getLineCount()) { context.sendMessage(Message.raw("Ligne hors limites (1-" + h.getLineCount() + ").").color(RED)); return; }
            h.removeLine(idx);
            plugin.getHologramManager().updateHologram(h);
            context.sendMessage(Message.raw("Ligne " + (idx + 1) + " supprimée.").color(GREEN));
        }
    }

    private static class InfoCommand extends CommandBase {
        private final VaryonHologramsPlugin plugin;
        private final RequiredArg<String> nameArg;

        InfoCommand(@Nonnull VaryonHologramsPlugin plugin) {
            super("info", "Informations sur un hologramme");
            this.plugin = plugin;
            this.nameArg = withRequiredArg("nom", "Nom de l'hologramme", (ArgumentType<String>) ArgTypes.STRING);
        }

        @Override protected boolean canGeneratePermission() { return false; }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (!perm(context, "varyon.holograms.list")) {
                context.sendMessage(Message.raw("Permission refusée.").color(RED)); return;
            }
            String name = nameArg.get(context);
            Hologram h = plugin.getHologramManager().getHologram(name);
            if (h == null) { context.sendMessage(Message.raw("Hologramme '" + name + "' introuvable.").color(RED)); return; }
            Vector3d p = h.getPosition();
            context.sendMessage(Message.raw("=== " + h.getName() + " ===").color(GOLD));
            context.sendMessage(Message.raw("ID: ").color(YELLOW).insert(Message.raw(h.getId().toString()).color(GRAY)));
            context.sendMessage(Message.raw("Position: ").color(YELLOW).insert(Message.raw(String.format("%.2f, %.2f, %.2f", p.x, p.y, p.z)).color(GRAY)));
            context.sendMessage(Message.raw("Monde: ").color(YELLOW).insert(Message.raw(h.getWorldId().toString()).color(GRAY)));
            context.sendMessage(Message.raw("Lignes (" + h.getLineCount() + "):").color(YELLOW));
            List<String> lines = h.getLines();
            for (int i = 0; i < lines.size(); i++) {
                context.sendMessage(Message.raw("  " + (i + 1) + ". ").color(GRAY).insert(Message.raw(lines.get(i)).color(WHITE)));
            }
        }
    }

    private static class ReloadCommand extends CommandBase {
        private final VaryonHologramsPlugin plugin;

        ReloadCommand(@Nonnull VaryonHologramsPlugin plugin) {
            super("reload", "Recharger les hologrammes");
            this.plugin = plugin;
        }

        @Override protected boolean canGeneratePermission() { return false; }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (!perm(context, "varyon.holograms.reload")) {
                context.sendMessage(Message.raw("Permission refusée.").color(RED)); return;
            }
            try {
                plugin.getHologramManager().reload();
                context.sendMessage(Message.raw("Rechargé! Hologrammes: " + plugin.getHologramManager().getHologramCount()).color(GREEN));
            } catch (Exception e) {
                context.sendMessage(Message.raw("Erreur: " + e.getMessage()).color(RED));
            }
        }
    }

    private static class CleanupCommand extends CommandBase {
        private final VaryonHologramsPlugin plugin;

        CleanupCommand(@Nonnull VaryonHologramsPlugin plugin) {
            super("cleanup", "Supprimer les entités hologramme orphelines");
            this.plugin = plugin;
            addAliases(new String[]{"clean", "purge"});
        }

        @Override protected boolean canGeneratePermission() { return false; }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (!perm(context, "varyon.holograms.cleanup")) {
                context.sendMessage(Message.raw("Permission refusée.").color(RED)); return;
            }
            int removed = plugin.getHologramManager().cleanupOrphanedEntities();
            if (removed > 0) {
                context.sendMessage(Message.raw(removed + " entité(s) orpheline(s) supprimée(s).").color(GREEN));
            } else {
                context.sendMessage(Message.raw("Aucune entité orpheline trouvée.").color(GRAY));
            }
        }
    }

    private static class AnimListCommand extends CommandBase {
        AnimListCommand() {
            super("animlist", "Lister les animations disponibles");
            addAliases(new String[]{"animations", "anims"});
        }

        @Override protected boolean canGeneratePermission() { return false; }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            context.sendMessage(Message.raw("=== Animations disponibles ===").color(GOLD));
            context.sendMessage(Message.raw("Flottement: ").color(YELLOW).insert(Message.raw("float, float_slow, float_fast").color(WHITE)));
            context.sendMessage(Message.raw("Rotation: ").color(YELLOW).insert(Message.raw("spin, spin_slow, spin_fast, tumble").color(WHITE)));
            context.sendMessage(Message.raw("Pulsation: ").color(YELLOW).insert(Message.raw("pulse, pulse_big, heartbeat, breathe").color(WHITE)));
            context.sendMessage(Message.raw("Rebond: ").color(YELLOW).insert(Message.raw("bounce, bounce_small").color(WHITE)));
            context.sendMessage(Message.raw("Balancement: ").color(YELLOW).insert(Message.raw("sway, sway_big").color(WHITE)));
            context.sendMessage(Message.raw("Oscillation: ").color(YELLOW).insert(Message.raw("wobble, wobble_slow, wobble_fast").color(WHITE)));
            context.sendMessage(Message.raw("Secousse: ").color(YELLOW).insert(Message.raw("shake, shake_big").color(WHITE)));
            context.sendMessage(Message.raw("Orbite: ").color(YELLOW).insert(Message.raw("orbit, orbit_small, orbit_fast").color(WHITE)));
            context.sendMessage(Message.raw("Autre: ").color(YELLOW).insert(Message.raw("wave, flip, flip_full").color(WHITE)));
            context.sendMessage(Message.raw("Usage: ").color(GRAY).insert(Message.raw(":anim_<nom> dans une ligne").color(YELLOW)));
            context.sendMessage(Message.raw("Exemple: ").color(GRAY).insert(Message.raw("image:logo:anim_bounce").color(WHITE)));
        }
    }

    private static class EntityResendCommand extends CommandBase {
        private final VaryonHologramsPlugin plugin;

        EntityResendCommand(@Nonnull VaryonHologramsPlugin plugin) {
            super("entityresend", "Renvoyer les entités à tous les joueurs");
            this.plugin = plugin;
            addAliases(new String[]{"resend", "er"});
        }

        @Override protected boolean canGeneratePermission() { return false; }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (!perm(context, "varyon.holograms.reload")) {
                context.sendMessage(Message.raw("Permission refusée.").color(RED)); return;
            }
            try {
                Collection<PlayerRef> players = Universe.get().getPlayers();
                int count = 0;
                for (PlayerRef pr : players) {
                    String username = pr.getUsername();
                    if (username == null) continue;
                    com.hypixel.hytale.server.core.command.system.CommandManager.get()
                        .handleCommand(ConsoleSender.INSTANCE, "entity resend " + username);
                    count++;
                }
                context.sendMessage(Message.raw("Entités renvoyées à " + count + " joueur(s).").color(GREEN));
            } catch (Exception e) {
                context.sendMessage(Message.raw("Erreur: " + e.getMessage()).color(RED));
            }
        }
    }
}
