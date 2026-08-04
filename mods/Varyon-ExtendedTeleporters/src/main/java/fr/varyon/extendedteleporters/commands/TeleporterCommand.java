package fr.varyon.extendedteleporters.commands;

import com.hypixel.hytale.builtin.teleport.TeleportPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import com.hypixel.hytale.math.vector.Rotation3f;
import org.joml.Vector3d;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.extendedteleporters.TeleporterManager;
import fr.varyon.extendedteleporters.data.CustomDestination;
import fr.varyon.extendedteleporters.data.TeleporterInfo;
import fr.varyon.extendedteleporters.gui.SubownerManagementGui;
import fr.varyon.extendedteleporters.gui.TeleporterSelectForSubownersGui;
import fr.varyon.extendedteleporters.gui.TeleporterSettingsGui;
import fr.varyon.extendedteleporters.i18n.Translations;
import fr.varyon.extendedteleporters.util.TeleporterLookup;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class TeleporterCommand extends AbstractAsyncCommand {
   private static final HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("ExtendedTeleport-Commands");
   private static final int ITEMS_PER_PAGE = 8;
   private static final double TELEPORT_HEIGHT_OFFSET = 6.0;
   private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
   private static final Color COLOR_HEADER = new Color(85, 255, 85);
   private static final Color COLOR_PLAYER = new Color(255, 255, 85);
   private static final Color COLOR_NAME = new Color(255, 255, 255);
   private static final Color COLOR_COORDS = new Color(85, 255, 255);
   private static final Color COLOR_DATE = new Color(170, 170, 170);
   private static final Color COLOR_FLAG_PRIVATE = new Color(255, 85, 85);
   private static final Color COLOR_FLAG_RESTRICTED = new Color(255, 170, 0);
   private static final Color COLOR_FLAG_LOCKED = new Color(170, 85, 255);
   private static final Color COLOR_FLAG_SELF_DESTRUCT = new Color(255, 85, 255);
   private static final Color COLOR_PAGE_INFO = new Color(170, 170, 170);

   public TeleporterCommand() {
      super("teleporter", "Manage teleporter settings");
      this.addAliases(new String[]{"tp-settings", "tpsettings"});
      this.setPermissionGroup(GameMode.Adventure);
      this.addSubCommand(new TeleporterCommand.ListTeleportersSubCommand());
      this.addSubCommand(new TeleporterCommand.TrustGuiSubCommand());
      this.addSubCommand(new TeleporterCommand.TrustSubCommand());
      this.addSubCommand(new TeleporterCommand.UntrustSubCommand());
      this.addSubCommand(new TeleporterCommand.TrustListSubCommand());
      this.addSubCommand(new TeleporterCommand.BypassSubCommand());
      this.addSubCommand(new TeleporterCommand.GoSubCommand());
      this.addSubCommand(new TeleporterCommand.DestinationSubCommand());
      this.addSubCommand(new TeleporterCommand.ServerSubCommand());
      this.addSubCommand(new TeleporterCommand.ReloadSubCommand());
   }

   @NonNullDecl
   protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
      if (ctx.sender() instanceof PlayerRef playerRef) {
         Ref<EntityStore> ref = playerRef.getReference();
         if (ref != null && ref.isValid()) {
            Store<EntityStore> store = ref.getStore();
            World world = ((EntityStore)store.getExternalData()).getWorld();
            return CompletableFuture.runAsync(() -> {
               List<TeleporterInfo> playerTeleporters = TeleporterManager.getInstance().getPlayerTeleportersSynced(playerRef.getUuid());
               if (playerTeleporters.isEmpty()) {
                  ctx.sendMessage(Translations.msgWarning("cmd.list.empty"));
               } else {
                  Player player = (Player)store.getComponent(ref, Player.getComponentType());
                  if (player != null) {
                     player.getPageManager().openCustomPage(ref, store, new TeleporterSettingsGui(playerRef));
                  }
               }
            }, world);
         } else {
            ctx.sendMessage(Translations.msgError("cmd.notInWorld"));
            return CompletableFuture.completedFuture(null);
         }
      } else {
         ctx.sendMessage(Translations.msgError("cmd.playerOnly"));
         return CompletableFuture.completedFuture(null);
      }
   }

   private static Map<UUID, String> getOnlinePlayerNames() {
      Map<UUID, String> names = new ConcurrentHashMap<>();

      try {
         for (PlayerRef playerRef : Universe.get().getPlayers()) {
            names.put(playerRef.getUuid(), playerRef.getUsername());
         }
      } catch (Exception var3) {
      }

      return names;
   }

   private static String getPlayerName(UUID uuid, Map<UUID, String> onlineNames) {
      if (uuid == null) {
         return Translations.tr("common.player.unknown");
      }

      String name = onlineNames.get(uuid);
      return name != null ? name : Translations.tr("common.player.uuidShort", "uuid", uuid.toString().substring(0, 8));
   }

   private static final class BypassSubCommand extends AbstractAsyncCommand {
      BypassSubCommand() {
         super("bypass", "Toggle bypass mode to access all teleporters");
      }

      @NonNullDecl
      protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
         if (ctx.sender() instanceof PlayerRef playerRef) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
               Store<EntityStore> store = ref.getStore();
               World world = ((EntityStore)store.getExternalData()).getWorld();
               return CompletableFuture.runAsync(() -> {
                  UUID playerUuid = playerRef.getUuid();
                  TeleporterManager manager = TeleporterManager.getInstance();
                  boolean nowBypassing = manager.toggleBypassMode(playerUuid);
                  if (nowBypassing) {
                     ctx.sendMessage(Translations.msgSuccess("cmd.bypass.enabled"));
                  } else {
                     ctx.sendMessage(Translations.msgWarning("cmd.bypass.disabled"));
                  }
               }, world);
            } else {
               ctx.sendMessage(Translations.msgError("cmd.notInWorld"));
               return CompletableFuture.completedFuture(null);
            }
         } else {
            ctx.sendMessage(Translations.msgError("cmd.playerOnly"));
            return CompletableFuture.completedFuture(null);
         }
      }
   }

   private static final class DestinationCreateSubCommand extends AbstractAsyncCommand {
      private final RequiredArg<String> nameArg = this.withRequiredArg("name", "Destination name (use quotes for names with spaces)", ArgTypes.STRING);
      private final RequiredArg<Double> xArg = this.withRequiredArg("x", "X coordinate", ArgTypes.DOUBLE);
      private final RequiredArg<Double> yArg = this.withRequiredArg("y", "Y coordinate", ArgTypes.DOUBLE);
      private final RequiredArg<Double> zArg = this.withRequiredArg("z", "Z coordinate", ArgTypes.DOUBLE);

      DestinationCreateSubCommand() {
         super("create", "Create custom destination (use quotes: \"name\" x y z)");
      }

      @NonNullDecl
      protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
         if (ctx.sender() instanceof PlayerRef playerRef) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
               String destName = (String)this.nameArg.get(ctx);
               double x = (Double)this.xArg.get(ctx);
               double y = (Double)this.yArg.get(ctx);
               double z = (Double)this.zArg.get(ctx);
               Store<EntityStore> store = ref.getStore();
               World world = ((EntityStore)store.getExternalData()).getWorld();
               return CompletableFuture.runAsync(
                  () -> {
                     String creatorUuid = playerRef.getUuid().toString();
                     TeleporterManager manager = TeleporterManager.getInstance();
                     if (destName == null || destName.isEmpty()) {
                        ctx.sendMessage(Translations.msgError("cmd.destination.create.nameEmpty"));
                     } else if (manager.customDestinationExists(destName)) {
                        ctx.sendMessage(Translations.msgError("cmd.destination.create.alreadyExists", "name", destName));
                        ctx.sendMessage(Translations.msgInfo("cmd.destination.create.removeHint"));
                     } else if (TeleportPlugin.get().getWarps().containsKey(destName.toLowerCase())) {
                        ctx.sendMessage(Translations.msgError("cmd.destination.create.warpExists", "name", destName));
                     } else {
                        boolean success = manager.createCustomDestination(destName, world.getName(), x, y, z, creatorUuid);
                        if (success) {
                           ctx.sendMessage(Message.raw(Translations.tr("cmd.destination.create.success", "name", destName)).color(Color.GREEN).bold(true));
                           ctx.sendMessage(
                              Message.raw(Translations.tr("cmd.destination.create.location", "dimension", world.getName(), "x", x, "y", y, "z", z))
                                 .color(TeleporterCommand.COLOR_COORDS)
                           );
                           ctx.sendMessage(Message.raw(Translations.tr("cmd.destination.create.linkHint", "name", destName)).color(Color.GRAY));
                        } else {
                           ctx.sendMessage(Translations.msgError("cmd.destination.create.failed"));
                        }
                     }
                  },
                  world
               );
            } else {
               ctx.sendMessage(Translations.msgError("cmd.notInWorld"));
               return CompletableFuture.completedFuture(null);
            }
         } else {
            ctx.sendMessage(Translations.msgError("cmd.playerOnly"));
            return CompletableFuture.completedFuture(null);
         }
      }
   }

   private static final class DestinationListSubCommand extends AbstractAsyncCommand {
      DestinationListSubCommand() {
         super("list", "List all custom destinations");
      }

      @NonNullDecl
      protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
         if (ctx.sender() instanceof PlayerRef playerRef) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
               Store<EntityStore> store = ref.getStore();
               World world = ((EntityStore)store.getExternalData()).getWorld();
               return CompletableFuture.runAsync(
                  () -> {
                     TeleporterManager manager = TeleporterManager.getInstance();
                     Collection<CustomDestination> destinations = manager.getAllCustomDestinations();
                     if (destinations.isEmpty()) {
                        ctx.sendMessage(Translations.msgWarning("cmd.destination.list.empty"));
                        ctx.sendMessage(Translations.msgInfo("cmd.destination.list.createHint"));
                     } else {
                        ctx.sendMessage(
                           Message.raw(Translations.tr("cmd.destination.list.header", "count", destinations.size()))
                              .color(TeleporterCommand.COLOR_HEADER)
                              .bold(true)
                        );
                        List<CustomDestination> sorted = new ArrayList<>(destinations);
                        sorted.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));

                        for (CustomDestination dest : sorted) {
                           ctx.sendMessage(Message.raw(dest.name()).color(TeleporterCommand.COLOR_NAME).bold(true));
                           ctx.sendMessage(
                              Message.raw("  %s (%.1f, %.1f, %.1f)".formatted(dest.dimension(), dest.x(), dest.y(), dest.z()))
                                 .color(TeleporterCommand.COLOR_COORDS)
                           );
                        }
                     }
                  },
                  world
               );
            } else {
               ctx.sendMessage(Translations.msgError("cmd.notInWorld"));
               return CompletableFuture.completedFuture(null);
            }
         } else {
            ctx.sendMessage(Translations.msgError("cmd.playerOnly"));
            return CompletableFuture.completedFuture(null);
         }
      }
   }

   private static final class DestinationRemoveSubCommand extends AbstractAsyncCommand {
      private final RequiredArg<String> nameArg = this.withRequiredArg("name", "Destination name (use quotes for names with spaces)", ArgTypes.STRING);

      DestinationRemoveSubCommand() {
         super("remove", "Remove custom destination (use quotes: \"name\")");
      }

      @NonNullDecl
      protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
         if (ctx.sender() instanceof PlayerRef playerRef) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
               String destName = (String)this.nameArg.get(ctx);
               Store<EntityStore> store = ref.getStore();
               World world = ((EntityStore)store.getExternalData()).getWorld();
               return CompletableFuture.runAsync(() -> {
                  TeleporterManager manager = TeleporterManager.getInstance();
                  if (!manager.customDestinationExists(destName)) {
                     ctx.sendMessage(Translations.msgError("cmd.destination.remove.notFound", "name", destName));
                     ctx.sendMessage(Translations.msgInfo("cmd.destination.remove.listHint"));
                  } else {
                     boolean success = manager.removeCustomDestination(destName);
                     if (success) {
                        ctx.sendMessage(Message.raw(Translations.tr("cmd.destination.remove.success", "name", destName)).color(Color.GREEN).bold(true));
                        ctx.sendMessage(Translations.msgWarning("cmd.destination.remove.warning"));
                     } else {
                        ctx.sendMessage(Translations.msgError("cmd.destination.remove.failed"));
                     }
                  }
               }, world);
            } else {
               ctx.sendMessage(Translations.msgError("cmd.notInWorld"));
               return CompletableFuture.completedFuture(null);
            }
         } else {
            ctx.sendMessage(Translations.msgError("cmd.playerOnly"));
            return CompletableFuture.completedFuture(null);
         }
      }
   }

   private static final class DestinationSetSubCommand extends AbstractAsyncCommand {
      private final RequiredArg<String> teleporterArg = this.withRequiredArg("teleporter", "Teleporter name (use quotes for spaces)", ArgTypes.STRING);
      private final RequiredArg<String> destinationArg = this.withRequiredArg("destination", "Destination name (use quotes for spaces)", ArgTypes.STRING);

      DestinationSetSubCommand() {
         super("set", "Set teleporter destination (use quotes: \"teleporter\" \"destination\")");
      }

      @NonNullDecl
      protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
         if (ctx.sender() instanceof PlayerRef playerRef) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
               String teleporterName = (String)this.teleporterArg.get(ctx);
               String destinationName = (String)this.destinationArg.get(ctx);
               Store<EntityStore> store = ref.getStore();
               World world = ((EntityStore)store.getExternalData()).getWorld();
               return CompletableFuture.runAsync(
                  () -> {
                     TeleporterManager manager = TeleporterManager.getInstance();
                     TeleporterInfo teleporter = TeleporterLookup.findFlexible(manager, teleporterName);
                     if (teleporter == null) {
                        ctx.sendMessage(Translations.msgError("cmd.destination.set.teleporterNotFound", "name", teleporterName));
                        ctx.sendMessage(Translations.msgInfo("cmd.destination.set.teleporterListHint"));
                     } else {
                        CustomDestination destination = manager.getCustomDestination(destinationName);
                        if (destination == null) {
                           ctx.sendMessage(Translations.msgError("cmd.destination.set.destinationNotFound", "name", destinationName));
                           ctx.sendMessage(Translations.msgInfo("cmd.destination.set.destinationListHint"));
                        } else {
                           boolean success = manager.updateTeleporterWarpDestination(teleporter, destinationName.toLowerCase());
                           if (success) {
                              teleporter.setWarpDestination(destinationName.toLowerCase());
                              manager.markDirty();
                              ctx.sendMessage(
                                 Message.raw(
                                       Translations.tr("cmd.destination.set.success", "teleporter", teleporter.displayName(), "destination", destination.name())
                                    )
                                    .color(Color.GREEN)
                                    .bold(true)
                              );
                              ctx.sendMessage(
                                 Message.raw(
                                       Translations.tr(
                                          "cmd.destination.set.location",
                                          "dimension",
                                          destination.dimension(),
                                          "x",
                                          destination.x(),
                                          "y",
                                          destination.y(),
                                          "z",
                                          destination.z()
                                       )
                                    )
                                    .color(TeleporterCommand.COLOR_COORDS)
                              );
                           } else {
                              ctx.sendMessage(Translations.msgError("cmd.destination.set.failed"));
                           }
                        }
                     }
                  },
                  world
               );
            } else {
               ctx.sendMessage(Translations.msgError("cmd.notInWorld"));
               return CompletableFuture.completedFuture(null);
            }
         } else {
            ctx.sendMessage(Translations.msgError("cmd.playerOnly"));
            return CompletableFuture.completedFuture(null);
         }
      }
   }

   private static final class DestinationSubCommand extends AbstractAsyncCommand {
      DestinationSubCommand() {
         super("destination", "Manage custom destinations");
         this.addSubCommand(new TeleporterCommand.DestinationCreateSubCommand());
         this.addSubCommand(new TeleporterCommand.DestinationListSubCommand());
         this.addSubCommand(new TeleporterCommand.DestinationRemoveSubCommand());
         this.addSubCommand(new TeleporterCommand.DestinationSetSubCommand());
      }

      @NonNullDecl
      protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
         ctx.sendMessage(Message.raw(Translations.tr("cmd.destination.header")).color(TeleporterCommand.COLOR_HEADER).bold(true));
         ctx.sendMessage(Message.raw(Translations.tr("cmd.destination.help.create")).color(Color.GRAY));
         ctx.sendMessage(Message.raw(Translations.tr("cmd.destination.help.list")).color(Color.GRAY));
         ctx.sendMessage(Message.raw(Translations.tr("cmd.destination.help.remove")).color(Color.GRAY));
         ctx.sendMessage(Message.raw(Translations.tr("cmd.destination.help.set")).color(Color.GRAY));
         return CompletableFuture.completedFuture(null);
      }
   }

   private static final class GoSubCommand extends AbstractAsyncCommand {
      private final RequiredArg<String> nameArg = this.withRequiredArg("name", "Teleporter name (use quotes for names with spaces)", ArgTypes.STRING);

      GoSubCommand() {
         super("go", "Teleport to a teleporter by name (use quotes: \"name\")");
      }

      @NonNullDecl
      protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
         if (ctx.sender() instanceof PlayerRef playerRef) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
               String targetName = (String)this.nameArg.get(ctx);
               Store<EntityStore> store = ref.getStore();
               World world = ((EntityStore)store.getExternalData()).getWorld();
               return CompletableFuture.runAsync(
                  () -> {
                     UUID playerUuid = playerRef.getUuid();
                     TeleporterManager manager = TeleporterManager.getInstance();
                     if (!manager.isInBypassMode(playerUuid)) {
                        ctx.sendMessage(Translations.msgError("cmd.go.bypassRequired"));
                     } else {
                        TeleporterInfo targetTeleporter = TeleporterLookup.findFlexible(manager, targetName);
                        if (targetTeleporter == null) {
                           ctx.sendMessage(Translations.msgError("cmd.go.notFound", "name", targetName));
                           ctx.sendMessage(Translations.msgInfo("cmd.go.useListHint"));
                        } else {
                           String targetDimension = targetTeleporter.dimension();
                           World targetWorld = manager.getWorld(targetDimension);
                           if (targetWorld == null) {
                              ctx.sendMessage(Translations.msgError("cmd.go.worldNotLoaded", "world", targetDimension));
                           } else {
                              double targetX = targetTeleporter.blockX() + 0.5;
                              double targetY = targetTeleporter.blockY() + 6.0;
                              double targetZ = targetTeleporter.blockZ() + 0.5;
                              if (world.getName().equals(targetDimension)) {
                                 this.performTeleport(store, ref, targetX, targetY, targetZ);
                                 ctx.sendMessage(Translations.msgSuccess("cmd.go.success", "name", targetTeleporter.displayName()));
                              } else {
                                 targetWorld.execute(
                                    () -> {
                                       Ref<EntityStore> targetRef = playerRef.getReference();
                                       if (targetRef != null && targetRef.isValid()) {
                                          Store<EntityStore> targetStore = targetRef.getStore();
                                          this.performTeleport(targetStore, targetRef, targetX, targetY, targetZ);
                                          ctx.sendMessage(
                                             Translations.msgSuccess(
                                                "cmd.go.successCrossWorld", "name", targetTeleporter.displayName(), "world", targetDimension
                                             )
                                          );
                                       } else {
                                          ctx.sendMessage(Translations.msgError("cmd.go.failed"));
                                       }
                                    }
                                 );
                              }
                           }
                        }
                     }
                  },
                  world
               );
            } else {
               ctx.sendMessage(Translations.msgError("cmd.notInWorld"));
               return CompletableFuture.completedFuture(null);
            }
         } else {
            ctx.sendMessage(Translations.msgError("cmd.playerOnly"));
            return CompletableFuture.completedFuture(null);
         }
      }

      private void performTeleport(Store<EntityStore> store, Ref<EntityStore> ref, double x, double y, double z) {
         TransformComponent transform = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());
         if (transform != null) {
            Rotation3f currentRotation = transform.getRotation();
            Teleport teleport = Teleport.createExact(new Vector3d(x, y, z), currentRotation, currentRotation);
            store.addComponent(ref, Teleport.getComponentType(), teleport);
         }
      }
   }

   private static final class ListTeleportersSubCommand extends AbstractAsyncCommand {
      private final OptionalArg<Integer> pageArg = this.withOptionalArg("page", "Page number", ArgTypes.INTEGER);

      ListTeleportersSubCommand() {
         super("list", "List your teleporters in chat (use /teleporter list <page> for pagination)");
      }

      @NonNullDecl
      protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
         if (ctx.sender() instanceof PlayerRef playerRef) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
               Store<EntityStore> store = ref.getStore();
               World world = ((EntityStore)store.getExternalData()).getWorld();
               int page = this.pageArg.provided(ctx) ? (Integer)this.pageArg.get(ctx) : 1;
               if (page < 1) {
                  page = 1;
               }

               int requestedPage = page;
               return CompletableFuture.runAsync(
                  () -> {
                     {
                        UUID playerUuid = playerRef.getUuid();
                        TeleporterManager manager = TeleporterManager.getInstance();
                        boolean isBypassing = manager.isInBypassMode(playerUuid);
                        List<TeleporterInfo> teleporters;
                        String headerTitle;
                        if (isBypassing) {
                           teleporters = new ArrayList<>(manager.getAllTeleporters());
                           headerTitle = Translations.tr("cmd.list.header.all");
                        } else {
                           teleporters = new ArrayList<>(manager.getPlayerTeleportersSynced(playerUuid));
                           headerTitle = Translations.tr("cmd.list.header.yours");
                        }

                        if (teleporters.isEmpty()) {
                           ctx.sendMessage(Translations.msgWarning("cmd.list.empty"));
                        } else {
                           teleporters.sort((a, b) -> a.displayName().compareToIgnoreCase(b.displayName()));
                           int totalItems = teleporters.size();
                           int totalPages = (int)Math.ceil(totalItems / 8.0);
                           int currentPage = Math.min(requestedPage, totalPages);
                           int startIndex = (currentPage - 1) * 8;
                           int endIndex = Math.min(startIndex + 8, totalItems);
                           Map<UUID, String> onlineNames = isBypassing ? TeleporterCommand.getOnlinePlayerNames() : Collections.emptyMap();
                           ctx.sendMessage(
                              Message.raw(Translations.tr("cmd.list.header.format", "title", headerTitle, "total", totalItems))
                                 .color(TeleporterCommand.COLOR_HEADER)
                                 .bold(true)
                           );

                           for (int i = startIndex; i < endIndex; i++) {
                              TeleporterInfo info = teleporters.get(i);
                              if (isBypassing) {
                                 this.displayAdminTeleporterInfo(ctx, info, onlineNames);
                              } else {
                                 this.displayPlayerTeleporterInfo(ctx, info);
                              }
                           }

                           if (totalPages > 1) {
                              ctx.sendMessage(
                                 Message.raw(Translations.tr("cmd.list.pagination", "current", currentPage, "total", totalPages))
                                    .color(TeleporterCommand.COLOR_PAGE_INFO)
                              );
                           }
                        }
                     }
                  },
                  world
               );
            } else {
               ctx.sendMessage(Translations.msgError("cmd.notInWorld"));
               return CompletableFuture.completedFuture(null);
            }
         } else {
            ctx.sendMessage(Translations.msgError("cmd.playerOnly"));
            return CompletableFuture.completedFuture(null);
         }
      }

      private void displayAdminTeleporterInfo(CommandContext ctx, TeleporterInfo info, Map<UUID, String> onlineNames) {
         String playerName = TeleporterCommand.getPlayerName(info.ownerUuid(), onlineNames);
         String portalName = info.warpName() != null && !info.warpName().isEmpty() ? info.warpName() : Translations.tr("cmd.list.unnamed");
         ctx.sendMessage(Message.raw(playerName + " - ").color(TeleporterCommand.COLOR_PLAYER));
         ctx.sendMessage(Message.raw("  " + portalName).color(TeleporterCommand.COLOR_NAME).bold(true));
         String coords = "  %s (%d, %d, %d)".formatted(info.dimension(), info.blockX(), info.blockY(), info.blockZ());
         String date = TeleporterCommand.DATE_FORMAT.format(new Date(info.placedTimestamp()));
         ctx.sendMessage(Message.raw(coords + " | " + date).color(TeleporterCommand.COLOR_COORDS));
         List<String> flags = new ArrayList<>();
         if (info.isPrivate()) {
            flags.add(Translations.tr("cmd.list.flag.private"));
         }

         if (info.isRestricted()) {
            flags.add(Translations.tr("cmd.list.flag.restricted"));
         }

         if (info.isInteractionLocked()) {
            flags.add(Translations.tr("cmd.list.flag.settingsLocked"));
         }

         if (info.isBreakLocked()) {
            flags.add(Translations.tr("cmd.list.flag.breakLocked"));
         }

         if (!info.displayWorld()) {
            flags.add(Translations.tr("cmd.list.flag.hiddenWorld"));
         }

         if (!info.displayCoordinates()) {
            flags.add(Translations.tr("cmd.list.flag.hiddenCoords"));
         }

         if (info.isSelfDestruct()) {
            long remainingMs = info.getSelfDestructRemainingMs();
            String timeStr = remainingMs > 0L
               ? Translations.tr("cmd.list.flag.selfDestruct.time", "minutes", remainingMs / 60000L, "seconds", remainingMs % 60000L / 1000L)
               : Translations.tr("cmd.list.flag.selfDestruct.expired");
            flags.add(Translations.tr("cmd.list.flag.selfDestruct.format", "time", timeStr));
         }

         if (info.isServerTeleporter()) {
            flags.add(Translations.tr("cmd.list.flag.server"));
         }

         if (info.isSingleUse()) {
            flags.add(Translations.tr("cmd.list.flag.singleUse"));
         }

         if (!flags.isEmpty()) {
            for (String flag : flags) {
               Color flagColor = this.getFlagColor(flag);
               ctx.sendMessage(Message.raw("  " + flag).color(flagColor));
            }
         }
      }

      private Color getFlagColor(String flag) {
         if (flag.contains("Private")) {
            return TeleporterCommand.COLOR_FLAG_PRIVATE;
         } else if (flag.contains("Restricted")) {
            return TeleporterCommand.COLOR_FLAG_RESTRICTED;
         } else if (flag.contains("Locked")) {
            return TeleporterCommand.COLOR_FLAG_LOCKED;
         } else if (flag.contains("SelfDestruct")) {
            return TeleporterCommand.COLOR_FLAG_SELF_DESTRUCT;
         } else if (flag.contains("Server")) {
            return new Color(0, 255, 255);
         } else {
            return flag.contains("SingleUse") ? new Color(255, 165, 0) : TeleporterCommand.COLOR_DATE;
         }
      }

      private void displayPlayerTeleporterInfo(CommandContext ctx, TeleporterInfo info) {
         TeleporterManager manager = TeleporterManager.getInstance();
         String portalName = info.warpName() != null && !info.warpName().isEmpty() ? info.warpName() : Translations.tr("cmd.list.unnamed");
         String destination = info.warpDestination();
         String destDisplay = destination != null && !destination.isEmpty()
            ? manager.formatWarpDisplayName(destination)
            : Translations.tr("cmd.list.noDestination");
         ctx.sendMessage(Message.raw(portalName).color(TeleporterCommand.COLOR_NAME).bold(true));
         ctx.sendMessage(Message.raw("  -> " + destDisplay).color(TeleporterCommand.COLOR_PLAYER));
         String coords;
         if (info.displayWorld()) {
            if (info.displayCoordinates()) {
               coords = "%s (%d, %d, %d)".formatted(info.dimension(), info.blockX(), info.blockY(), info.blockZ());
            } else {
               coords = info.dimension();
            }
         } else {
            coords = Translations.tr("cmd.list.locationHidden");
         }

         ctx.sendMessage(Message.raw("  " + coords).color(TeleporterCommand.COLOR_COORDS));
         String date = TeleporterCommand.DATE_FORMAT.format(new Date(info.placedTimestamp()));
         ctx.sendMessage(Message.raw("  " + date).color(TeleporterCommand.COLOR_DATE));
         StringBuilder flagsLine = new StringBuilder("  ");
         boolean hasFlags = false;
         if (info.isPrivate()) {
            flagsLine.append(Translations.tr("cmd.list.flag.private")).append(" ");
            hasFlags = true;
         }

         if (info.isRestricted()) {
            flagsLine.append(Translations.tr("cmd.list.flag.restricted")).append(" ");
            hasFlags = true;
         }

         if (info.isInteractionLocked()) {
            flagsLine.append(Translations.tr("cmd.list.flag.settingsLocked")).append(" ");
            hasFlags = true;
         }

         if (info.isBreakLocked()) {
            flagsLine.append(Translations.tr("cmd.list.flag.breakLocked")).append(" ");
            hasFlags = true;
         }

         if (info.isSelfDestruct()) {
            long remainingMs = info.getSelfDestructRemainingMs();
            String timeStr = remainingMs > 0L
               ? Translations.tr("cmd.list.flag.selfDestruct.time", "minutes", remainingMs / 60000L, "seconds", remainingMs % 60000L / 1000L)
               : Translations.tr("cmd.list.flag.selfDestruct.expired");
            flagsLine.append(Translations.tr("cmd.list.flag.selfDestruct.format", "time", timeStr)).append(" ");
            hasFlags = true;
         }

         if (info.isServerTeleporter()) {
            flagsLine.append(Translations.tr("cmd.list.flag.server")).append(" ");
            hasFlags = true;
         }

         if (info.isSingleUse()) {
            flagsLine.append(Translations.tr("cmd.list.flag.singleUse")).append(" ");
            hasFlags = true;
         }

         if (hasFlags) {
            Color primaryColor = TeleporterCommand.COLOR_DATE;
            if (info.isSelfDestruct()) {
               primaryColor = TeleporterCommand.COLOR_FLAG_SELF_DESTRUCT;
            } else if (info.isServerTeleporter()) {
               primaryColor = new Color(0, 255, 255);
            } else if (info.isSingleUse()) {
               primaryColor = new Color(255, 165, 0);
            } else if (info.isPrivate()) {
               primaryColor = TeleporterCommand.COLOR_FLAG_PRIVATE;
            } else if (info.isRestricted()) {
               primaryColor = TeleporterCommand.COLOR_FLAG_RESTRICTED;
            } else if (info.isInteractionLocked() || info.isBreakLocked()) {
               primaryColor = TeleporterCommand.COLOR_FLAG_LOCKED;
            }

            ctx.sendMessage(Message.raw(flagsLine.toString().trim()).color(primaryColor));
         }
      }
   }

   private static final class ReloadSubCommand extends AbstractAsyncCommand {
      ReloadSubCommand() {
         super("reload", "Reload teleporter and warp data from files");
      }

      @NonNullDecl
      protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
         if (ctx.sender() instanceof PlayerRef playerRef) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
               Store<EntityStore> store = ref.getStore();
               World world = ((EntityStore)store.getExternalData()).getWorld();
               ctx.sendMessage(Translations.msgWarning("cmd.reload.description"));
               return CompletableFuture.runAsync(() -> {
                  try {
                     TeleporterManager manager = TeleporterManager.getInstance();
                     manager.reloadAll();
                     ctx.sendMessage(Translations.msgSuccess("cmd.reload.success"));
                  } catch (Exception e) {
                     ctx.sendMessage(Translations.msgError("cmd.reload.error", "error", e.getMessage()));
                     ((Api)TeleporterCommand.logger.at(Level.SEVERE).withCause(e)).log("Failed to reload teleporter data");
                  }
               }, world);
            } else {
               ctx.sendMessage(Translations.msgError("cmd.notInWorld"));
               return CompletableFuture.completedFuture(null);
            }
         } else {
            ctx.sendMessage(Translations.msgError("cmd.playerOnly"));
            return CompletableFuture.completedFuture(null);
         }
      }
   }

   private static final class ServerSubCommand extends AbstractAsyncCommand {
      private final RequiredArg<String> nameArg = this.withRequiredArg("name", "Teleporter name (use quotes for spaces)", ArgTypes.STRING);

      ServerSubCommand() {
         super("server", "Toggle server teleporter mode (use quotes: \"name\")");
      }

      @NonNullDecl
      protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
         if (ctx.sender() instanceof PlayerRef playerRef) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
               String targetName = (String)this.nameArg.get(ctx);
               Store<EntityStore> store = ref.getStore();
               World world = ((EntityStore)store.getExternalData()).getWorld();
               return CompletableFuture.runAsync(
                  () -> {
                     TeleporterManager manager = TeleporterManager.getInstance();
                     TeleporterInfo targetTeleporter = TeleporterLookup.findFlexible(manager, targetName);
                     if (targetTeleporter == null) {
                        ctx.sendMessage(Translations.msgError("cmd.server.notFound", "name", targetName));
                        ctx.sendMessage(Translations.msgInfo("cmd.server.listHint"));
                     } else {
                        boolean nowServer = !targetTeleporter.isServerTeleporter();
                        targetTeleporter.setServerTeleporter(nowServer);
                        manager.markDirty();
                        if (nowServer) {
                           ctx.sendMessage(
                              Message.raw(Translations.tr("cmd.server.enabled", "name", targetTeleporter.displayName())).color(Color.GREEN).bold(true)
                           );
                           ctx.sendMessage(Translations.msgInfo("cmd.server.enabled.info1"));
                           ctx.sendMessage(Translations.msgInfo("cmd.server.enabled.info2"));
                        } else {
                           ctx.sendMessage(
                              Message.raw(Translations.tr("cmd.server.disabled", "name", targetTeleporter.displayName())).color(Color.YELLOW).bold(true)
                           );
                           ctx.sendMessage(Translations.msgInfo("cmd.server.disabled.info"));
                        }
                     }
                  },
                  world
               );
            } else {
               ctx.sendMessage(Translations.msgError("cmd.notInWorld"));
               return CompletableFuture.completedFuture(null);
            }
         } else {
            ctx.sendMessage(Translations.msgError("cmd.playerOnly"));
            return CompletableFuture.completedFuture(null);
         }
      }
   }

   private static final class TrustGuiSubCommand extends AbstractAsyncCommand {
      TrustGuiSubCommand() {
         super("trustgui", "Open the Trust Management GUI");
         this.setPermissionGroup(GameMode.Adventure);
      }

      @NonNullDecl
      protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
         if (ctx.sender() instanceof PlayerRef playerRef) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
               Store<EntityStore> store = ref.getStore();
               World world = ((EntityStore)store.getExternalData()).getWorld();
               return CompletableFuture.runAsync(() -> {
                  UUID playerUuid = playerRef.getUuid();
                  TeleporterManager manager = TeleporterManager.getInstance();
                  List<TeleporterInfo> playerTeleporters = manager.getPlayerTeleportersSynced(playerUuid);
                  boolean isBypassing = manager.isInBypassMode(playerUuid);
                  if (playerTeleporters.isEmpty() && !isBypassing) {
                     ctx.sendMessage(Translations.msgWarning("cmd.trustgui.noTeleporters"));
                  } else {
                     Player player = (Player)store.getComponent(ref, Player.getComponentType());
                     if (player != null) {
                        if (playerTeleporters.size() == 1 && !isBypassing) {
                           player.getPageManager().openCustomPage(ref, store, new SubownerManagementGui(playerRef, playerTeleporters.getFirst()));
                        } else {
                           player.getPageManager().openCustomPage(ref, store, new TeleporterSelectForSubownersGui(playerRef));
                        }
                     }
                  }
               }, world);
            } else {
               ctx.sendMessage(Translations.msgError("cmd.notInWorld"));
               return CompletableFuture.completedFuture(null);
            }
         } else {
            ctx.sendMessage(Translations.msgError("cmd.playerOnly"));
            return CompletableFuture.completedFuture(null);
         }
      }
   }

   private static final class TrustListSubCommand extends AbstractAsyncCommand {
      private final RequiredArg<String> teleporterArg = this.withRequiredArg("teleporter", "Teleporter name (must use quotes)", ArgTypes.STRING);

      TrustListSubCommand() {
         super("trustlist", "List trusted players on your teleporter (use: trustlist \"<teleporter>\")");
         this.setPermissionGroup(GameMode.Adventure);
      }

      @NonNullDecl
      protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
         if (ctx.sender() instanceof PlayerRef playerRef) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
               String teleporterName = (String)this.teleporterArg.get(ctx);
               Store<EntityStore> store = ref.getStore();
               World world = ((EntityStore)store.getExternalData()).getWorld();
               return CompletableFuture.runAsync(
                  () -> {
                     {
                        UUID playerUuid = playerRef.getUuid();
                        TeleporterManager manager = TeleporterManager.getInstance();
                        TeleporterInfo teleporter = TeleporterLookup.findExact(manager, teleporterName);
                        if (teleporter == null) {
                           ctx.sendMessage(Translations.msgError("cmd.trust.teleporterNotFound", "name", teleporterName));
                           ctx.sendMessage(Translations.msgInfo("cmd.trust.useQuotes"));
                        } else if (!teleporter.isOwnerOrTrusted(playerUuid) && !manager.isInBypassMode(playerUuid)) {
                           ctx.sendMessage(Translations.msgError("cmd.trustlist.notOwnerOrTrusted"));
                        } else {
                           Set<UUID> trusted = teleporter.getTrustedPlayers();
                           if (trusted.isEmpty()) {
                              ctx.sendMessage(Translations.msgWarning("cmd.trustlist.empty", "teleporter", teleporter.displayName()));
                              ctx.sendMessage(Translations.msgInfo("cmd.trustlist.addHint"));
                           } else {
                              Map<UUID, String> onlineNames = TeleporterCommand.getOnlinePlayerNames();
                              ctx.sendMessage(
                                 Message.raw(Translations.tr("cmd.trustlist.header", "teleporter", teleporter.displayName()))
                                    .color(TeleporterCommand.COLOR_HEADER)
                                    .bold(true)
                              );

                              for (UUID trustedUuid : trusted) {
                                 String name = TeleporterCommand.getPlayerName(trustedUuid, onlineNames);
                                 ctx.sendMessage(Message.raw("  - " + name).color(TeleporterCommand.COLOR_PLAYER));
                              }

                              ctx.sendMessage(
                                 Message.raw(Translations.tr("cmd.trustlist.total", "count", trusted.size())).color(TeleporterCommand.COLOR_PAGE_INFO)
                              );
                           }
                        }
                     }
                  },
                  world
               );
            } else {
               ctx.sendMessage(Translations.msgError("cmd.notInWorld"));
               return CompletableFuture.completedFuture(null);
            }
         } else {
            ctx.sendMessage(Translations.msgError("cmd.playerOnly"));
            return CompletableFuture.completedFuture(null);
         }
      }
   }

   private static final class TrustSubCommand extends AbstractAsyncCommand {
      private final RequiredArg<String> playerArg = this.withRequiredArg("player", "Player name to trust", ArgTypes.STRING);
      private final RequiredArg<String> teleporterArg = this.withRequiredArg("teleporter", "Teleporter name (must use quotes)", ArgTypes.STRING);

      TrustSubCommand() {
         super("trust", "Trust a player on your teleporter (use: trust <player> \"<teleporter>\")");
         this.setPermissionGroup(GameMode.Adventure);
      }

      @NonNullDecl
      protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
         if (ctx.sender() instanceof PlayerRef playerRef) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
               String targetPlayerName = (String)this.playerArg.get(ctx);
               String teleporterName = (String)this.teleporterArg.get(ctx);
               Store<EntityStore> store = ref.getStore();
               World world = ((EntityStore)store.getExternalData()).getWorld();
               return CompletableFuture.runAsync(() -> {
                  UUID playerUuid = playerRef.getUuid();
                  TeleporterManager manager = TeleporterManager.getInstance();
                  TeleporterInfo teleporter = TeleporterLookup.findExact(manager, teleporterName);
                  if (teleporter == null) {
                     ctx.sendMessage(Translations.msgError("cmd.trust.teleporterNotFound", "name", teleporterName));
                     ctx.sendMessage(Translations.msgInfo("cmd.trust.useQuotes"));
                     ctx.sendMessage(Translations.msgInfo("cmd.trust.example"));
                  } else if (!teleporter.isOwner(playerUuid) && !manager.isInBypassMode(playerUuid)) {
                     ctx.sendMessage(Translations.msgError("cmd.trust.notOwner"));
                  } else {
                     UUID targetUuid = null;
                     String resolvedName = null;

                     for (PlayerRef onlinePlayer : Universe.get().getPlayers()) {
                        if (onlinePlayer.getUsername().equalsIgnoreCase(targetPlayerName)) {
                           targetUuid = onlinePlayer.getUuid();
                           resolvedName = onlinePlayer.getUsername();
                           break;
                        }
                     }

                     if (targetUuid == null) {
                        ctx.sendMessage(Translations.msgError("cmd.trust.playerNotFound", "name", targetPlayerName));
                        ctx.sendMessage(Translations.msgInfo("cmd.trust.playerMustBeOnline"));
                     } else if (targetUuid.equals(playerUuid)) {
                        ctx.sendMessage(Translations.msgWarning("cmd.trust.cannotTrustSelf"));
                     } else if (teleporter.isTrusted(targetUuid)) {
                        ctx.sendMessage(Translations.msgWarning("cmd.trust.alreadyTrusted", "name", resolvedName));
                     } else {
                        teleporter.addTrustedPlayer(targetUuid);
                        manager.markDirty();
                        ctx.sendMessage(Translations.msgSuccess("cmd.trust.success", "name", resolvedName, "teleporter", teleporter.displayName()));
                        ctx.sendMessage(Translations.msgInfo("cmd.trust.successInfo"));
                     }
                  }
               }, world);
            } else {
               ctx.sendMessage(Translations.msgError("cmd.notInWorld"));
               return CompletableFuture.completedFuture(null);
            }
         } else {
            ctx.sendMessage(Translations.msgError("cmd.playerOnly"));
            return CompletableFuture.completedFuture(null);
         }
      }
   }

   private static final class UntrustSubCommand extends AbstractAsyncCommand {
      private final RequiredArg<String> playerArg = this.withRequiredArg("player", "Player name to untrust", ArgTypes.STRING);
      private final RequiredArg<String> teleporterArg = this.withRequiredArg("teleporter", "Teleporter name (must use quotes)", ArgTypes.STRING);

      UntrustSubCommand() {
         super("untrust", "Remove trust from a player on your teleporter (use: untrust <player> \"<teleporter>\")");
         this.setPermissionGroup(GameMode.Adventure);
      }

      @NonNullDecl
      protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
         if (ctx.sender() instanceof PlayerRef playerRef) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
               String targetPlayerName = (String)this.playerArg.get(ctx);
               String teleporterName = (String)this.teleporterArg.get(ctx);
               Store<EntityStore> store = ref.getStore();
               World world = ((EntityStore)store.getExternalData()).getWorld();
               return CompletableFuture.runAsync(() -> {
                  UUID playerUuid = playerRef.getUuid();
                  TeleporterManager manager = TeleporterManager.getInstance();
                  TeleporterInfo teleporter = TeleporterLookup.findExact(manager, teleporterName);
                  if (teleporter == null) {
                     ctx.sendMessage(Translations.msgError("cmd.trust.teleporterNotFound", "name", teleporterName));
                     ctx.sendMessage(Translations.msgInfo("cmd.trust.useQuotes"));
                  } else if (!teleporter.isOwner(playerUuid) && !manager.isInBypassMode(playerUuid)) {
                     ctx.sendMessage(Translations.msgError("cmd.untrust.notOwner"));
                  } else {
                     UUID targetUuid = null;
                     String resolvedName = targetPlayerName;

                     for (PlayerRef onlinePlayer : Universe.get().getPlayers()) {
                        if (onlinePlayer.getUsername().equalsIgnoreCase(targetPlayerName)) {
                           targetUuid = onlinePlayer.getUuid();
                           resolvedName = onlinePlayer.getUsername();
                           break;
                        }
                     }

                     if (targetUuid == null) {
                        for (UUID trusted : teleporter.getTrustedPlayers()) {
                           if (trusted.toString().toLowerCase().startsWith(targetPlayerName.toLowerCase())) {
                              targetUuid = trusted;
                              resolvedName = trusted.toString().substring(0, 8) + "...";
                              break;
                           }
                        }
                     }

                     if (targetUuid == null) {
                        ctx.sendMessage(Translations.msgError("cmd.untrust.playerNotFound", "name", targetPlayerName));
                        ctx.sendMessage(Translations.msgInfo("cmd.untrust.useTrustlist"));
                     } else if (!teleporter.isTrusted(targetUuid)) {
                        ctx.sendMessage(Translations.msgWarning("cmd.untrust.notTrusted", "name", resolvedName));
                     } else {
                        teleporter.removeTrustedPlayer(targetUuid);
                        manager.markDirty();
                        ctx.sendMessage(Translations.msgSuccess("cmd.untrust.success", "name", resolvedName, "teleporter", teleporter.displayName()));
                     }
                  }
               }, world);
            } else {
               ctx.sendMessage(Translations.msgError("cmd.notInWorld"));
               return CompletableFuture.completedFuture(null);
            }
         } else {
            ctx.sendMessage(Translations.msgError("cmd.playerOnly"));
            return CompletableFuture.completedFuture(null);
         }
      }
   }
}
