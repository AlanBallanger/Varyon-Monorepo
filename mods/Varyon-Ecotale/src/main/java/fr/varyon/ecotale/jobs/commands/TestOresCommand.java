package fr.varyon.ecotale.jobs.commands;

import fr.varyon.ecotale.VaryonEcotalePlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;

/**
 * Debug command to generate a wall with ALL ores organized by family.
 * Usage: /jobstest
 * DebugMode=true in earnings_config.yml
 */
public class TestOresCommand extends AbstractAsyncCommand {
    
    // All ore families with their variants (58 total)
    private static final String[][] ORE_GROUPS = {
        // COMMON tier
        {"Ore_Copper", "Ore_Copper_Basalt", "Ore_Copper_Sandstone", "Ore_Copper_Shale", "Ore_Copper_Stone", "Ore_Copper_Volcanic"},
        
        // UNCOMMON tier
        {"Ore_Iron", "Ore_Iron_Basalt", "Ore_Iron_Sandstone", "Ore_Iron_Shale", "Ore_Iron_Slate", "Ore_Iron_Stone", "Ore_Iron_Volcanic"},
        
        // RARE tier
        {"Ore_Gold", "Ore_Gold_Basalt", "Ore_Gold_Sandstone", "Ore_Gold_Shale", "Ore_Gold_Stone", "Ore_Gold_Volcanic"},
        {"Ore_Silver", "Ore_Silver_Basalt", "Ore_Silver_Sandstone", "Ore_Silver_Shale", "Ore_Silver_Slate", "Ore_Silver_Stone", "Ore_Silver_Volcanic"},
        
        // EPIC tier
        {"Ore_Cobalt", "Ore_Cobalt_Basalt", "Ore_Cobalt_Sandstone", "Ore_Cobalt_Shale", "Ore_Cobalt_Slate", "Ore_Cobalt_Stone", "Ore_Cobalt_Volcanic"},
        
        // LEGENDARY tier
        {"Ore_Mithril", "Ore_Mithril_Basalt", "Ore_Mithril_Magma", "Ore_Mithril_Slate", "Ore_Mithril_Stone", "Ore_Mithril_Volcanic"},
        {"Ore_Thorium", "Ore_Thorium_Basalt", "Ore_Thorium_Sandstone", "Ore_Thorium_Shale", "Ore_Thorium_Stone", "Ore_Thorium_Volcanic"},
        {"Ore_Adamantite", "Ore_Adamantite_Basalt", "Ore_Adamantite_Shale", "Ore_Adamantite_Slate", "Ore_Adamantite_Stone", "Ore_Adamantite_Volcanic"},
        {"Ore_Onyxium", "Ore_Onyxium_Basalt", "Ore_Onyxium_Sandstone", "Ore_Onyxium_Shale", "Ore_Onyxium_Stone", "Ore_Onyxium_Volcanic"}
        // Note: Prisma is an item-only ore, not a placeable block
    };
    
    private static final String[] GROUP_NAMES = {
        "Copper (COMMON)",
        "Iron (UNCOMMON)",
        "Gold (RARE)", "Silver (RARE)",
        "Cobalt (EPIC)",
        "Mithril (LEGENDARY)", "Thorium (LEGENDARY)", "Adamantite (LEGENDARY)", "Onyxium (LEGENDARY)"
    };
    
    private static final int MAX_HEIGHT = 7; // Maximum column height
    
    public TestOresCommand() {
        super("jobstest", "Generate ore wall for testing (debug only)");
    }
    
    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext context) {
        if (!(context.sender() instanceof Player player)) {
            return CompletableFuture.completedFuture(null);
        }
        
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) {
            return CompletableFuture.completedFuture(null);
        }

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        if (world == null) {
            return CompletableFuture.completedFuture(null);
        }

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return CompletableFuture.completedFuture(null);
        }

        // Check debug mode
        if (!VaryonEcotalePlugin.getInstance().getEconomyConfig().isDebugMode()) {
            playerRef.sendMessage(Message.raw("Debug mode is disabled in earnings_config.yml.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            spawnOreWall(player, store, ref, playerRef, world);
        }, world);
    }

    private void spawnOreWall(Player player, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }

        // Get player position
        org.joml.Vector3d pos = transform.getPosition();
        double px = pos.x;
        double py = pos.y;
        double pz = pos.z;

        // Spawn 3 blocks in front
        int startX = (int) px + 3;
        int startY = (int) py;
        int startZ = (int) pz;

        playerRef.sendMessage(Message.raw("Generating ore museum with " + countTotalOres() + " ores...").color(Color.GREEN));

        // Block operations on world thread
        world.execute(() -> {
            int zOffset = 0;

            for (int groupIdx = 0; groupIdx < ORE_GROUPS.length; groupIdx++) {
                String[] ores = ORE_GROUPS[groupIdx];

                int xOffset = 0;
                int yOffset = 0;

                for (String oreId : ores) {
                    int x = startX + xOffset;
                    int y = startY + yOffset + 1;
                    int z = startZ + zOffset;

                    try {
                        world.setBlock(x, y, z, oreId);
                    } catch (Exception e) {
                        // Ore doesn't exist, skip
                    }

                    // Stack vertically up to MAX_HEIGHT
                    yOffset++;
                    if (yOffset >= MAX_HEIGHT) {
                        yOffset = 0;
                        xOffset++;
                    }
                }

                // Gap between groups
                zOffset += 2;
            }

            playerRef.sendMessage(Message.raw("Ore museum generated! Groups:").color(Color.GREEN));
            for (int i = 0; i < GROUP_NAMES.length; i++) {
                playerRef.sendMessage(Message.raw("  " + (i+1) + ". " + GROUP_NAMES[i]).color(Color.YELLOW));
            }
            playerRef.sendMessage(Message.raw("Mine them to test reward tiers!").color(Color.GREEN));
        });
    }
    
    private int countTotalOres() {
        int count = 0;
        for (String[] group : ORE_GROUPS) {
            count += group.length;
        }
        return count;
    }
}
