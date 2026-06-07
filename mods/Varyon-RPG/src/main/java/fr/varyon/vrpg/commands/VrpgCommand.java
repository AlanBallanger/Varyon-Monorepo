package fr.varyon.vrpg.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.VaryonRpgPlugin;
import fr.varyon.vrpg.classes.ability.ClassSkillService;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.ui.RpgMainUI;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.concurrent.CompletableFuture;

public final class VrpgCommand extends AbstractAsyncCommand {

    @SuppressWarnings("unused")
    public VrpgCommand(@Nonnull VaryonRpgPlugin plugin) {
        super("vrpg", "Varyon RPG - Classes");
        this.addSubCommand(new ReloadConfigSub());
        this.addSubCommand(new ResetClassSub());
        this.addSubCommand(new ResetTalentsSub());
        this.addSubCommand(new ResetAllSub());
        this.addSubCommand(new SkillSub());
    }

    @Override
    @Nonnull
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        CommandSender sender = ctx.sender();
        if (!(sender instanceof PlayerRef playerRef)) {
            sender.sendMessage(Message.raw("Commande joueur uniquement.").color(Color.RED));
            return done();
        }
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref != null && ref.isValid()) {
            Store<EntityStore> store = ref.getStore();
            ((EntityStore) store.getExternalData()).getWorld().execute(() -> {
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player != null) {
                    player.getPageManager().openCustomPage(ref, store, new RpgMainUI(playerRef, "classes"));
                }
            });
        }
        return done();
    }

    private static final class ResetClassSub extends AbstractAsyncCommand {
        ResetClassSub() { super("reset-class", "Réinitialise la classe du profil actif"); }

        @Override
        @Nonnull
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            if (!(ctx.sender() instanceof PlayerRef playerRef)) return done();
            ClassManager cm = VaryonRpgPlugin.getInstance().getClassManager();
            if (cm == null) return done();
            cm.setActiveClass(playerRef.getUuid(), null);
            ctx.sender().sendMessage(Message.raw("Classe du profil actif réinitialisée.").color(new Color(0x6BCB7A)));
            return done();
        }
    }

    private static final class ResetTalentsSub extends AbstractAsyncCommand {
        ResetTalentsSub() { super("reset-talents", "Réinitialise les talents de la classe active"); }

        @Override
        @Nonnull
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            if (!(ctx.sender() instanceof PlayerRef playerRef)) return done();
            ClassManager cm = VaryonRpgPlugin.getInstance().getClassManager();
            if (cm == null) return done();
            ClassAccount acc = cm.getAccount(playerRef.getUuid());
            PlayerClass active = acc != null ? acc.getActiveClass() : null;
            if (active == null) {
                ctx.sender().sendMessage(Message.raw("Aucune classe active.").color(Color.RED));
                return done();
            }
            cm.resetTalents(playerRef.getUuid(), active);
            ctx.sender().sendMessage(Message.raw("Talents réinitialisés pour " + active.getDisplayName() + ".").color(new Color(0x6BCB7A)));
            return done();
        }
    }

    private static final class ResetAllSub extends AbstractAsyncCommand {
        ResetAllSub() { super("reset-all", "Réinitialise classe, spé et talents du profil actif"); }

        @Override
        @Nonnull
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            if (!(ctx.sender() instanceof PlayerRef playerRef)) return done();
            ClassManager cm = VaryonRpgPlugin.getInstance().getClassManager();
            if (cm == null) return done();
            cm.setActiveClass(playerRef.getUuid(), null);
            for (PlayerClass c : PlayerClass.values()) {
                cm.setActiveSpec(playerRef.getUuid(), c, null);
                cm.resetTalents(playerRef.getUuid(), c);
            }
            ctx.sender().sendMessage(Message.raw("Profil actif entièrement réinitialisé.").color(new Color(0x6BCB7A)));
            return done();
        }
    }

    private static final class SkillSub extends AbstractAsyncCommand {
        SkillSub() { super("skill", "Utilise Assaut éclair (Duelliste)"); }

        @Override
        @Nonnull
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            if (!(ctx.sender() instanceof PlayerRef playerRef)) return done();
            VaryonRpgPlugin plugin = VaryonRpgPlugin.getInstance();
            ClassSkillService skills = plugin != null ? plugin.getClassSkillService() : null;
            if (skills == null) return done();
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) return done();
            Store<EntityStore> store = ref.getStore();
            ((EntityStore) store.getExternalData()).getWorld().execute(() -> {
                skills.tryCastAssautEclair(playerRef.getUuid(), playerRef, ref, store, null);
            });
            return done();
        }
    }

    private static CompletableFuture<Void> done() {
        return CompletableFuture.completedFuture(null);
    }
}
