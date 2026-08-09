package fr.varyon.readablebooks;

import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import fr.varyon.readablebooks.commands.BookCommand;
import fr.varyon.readablebooks.interaction.ReadableBookInteraction;
import fr.varyon.readablebooks.system.BookBreakBlockEventSystem;
import fr.varyon.readablebooks.system.BookParticleTickSystem;

import java.util.logging.Level;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class VaryonReadableBooksPlugin extends JavaPlugin {
    private static VaryonReadableBooksPlugin instance;

    public VaryonReadableBooksPlugin(@NonNullDecl JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static VaryonReadableBooksPlugin get() {
        return instance;
    }

    protected void setup() {
        super.setup();
        this.getLogger().at(Level.INFO).log("ReadableBooks - Initializing...");

        BookManager.getInstance().load();

        this.getCodecRegistry(Interaction.CODEC).register(
            "Varyon_Readable_Book_Open",
            ReadableBookInteraction.class,
            ReadableBookInteraction.CODEC
        );

        this.getEntityStoreRegistry().registerSystem(new BookBreakBlockEventSystem());
        this.getEntityStoreRegistry().registerSystem(new BookParticleTickSystem());
        this.getCommandRegistry().registerCommand(new BookCommand());

        this.getLogger().at(Level.INFO).log(
            "ReadableBooks - Loaded " + BookManager.getInstance().count() + " book(s)");
        this.getLogger().at(Level.INFO).log("ReadableBooks - Successfully initialized!");
    }

    protected void shutdown() {
        BookManager.getInstance().shutdown();
        this.getLogger().at(Level.INFO).log("ReadableBooks - Shutdown complete");
    }
}
