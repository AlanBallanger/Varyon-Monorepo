package fr.varyon.damagenumber;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public final class DamageNumberCommand extends AbstractPlayerCommand {

    private static final String GREEN = "#55FF55";
    private static final String RED = "#FF5555";
    private static final String GRAY = "#AAAAAA";

    public DamageNumberCommand() {
        super("dmgnum", "Basculer l'affichage custom des degats");
        addAliases(new String[] {"degats", "damagenumbers"});
    }

    @Override
    protected void execute(@Nonnull CommandContext context,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        boolean enabled = DamageNumberDisplaySettings.toggle(playerRef.getUuid());
        context.sendMessage(Message.raw("Affichage custom des degats : ")
            .color(GRAY)
            .insert(Message.raw(enabled ? "active" : "desactive").color(enabled ? GREEN : RED)));
    }
}
