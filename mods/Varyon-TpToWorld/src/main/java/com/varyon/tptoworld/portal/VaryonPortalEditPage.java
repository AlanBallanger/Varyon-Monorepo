package com.varyon.tptoworld.portal;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.List;

public class VaryonPortalEditPage extends InteractiveCustomUIPage<VaryonPortalEditEventData> {

    private final World world;
    private final int x;
    private final int y;
    private final int z;
    private final boolean isTemplate;
    private final boolean isWide;

    public VaryonPortalEditPage(@Nonnull PlayerRef playerRef, @Nonnull World world, int x, int y, int z) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, VaryonPortalEditEventData.CODEC);
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        BlockType blockType = world.getBlockType(x, y, z);
        String blockId = blockType != null ? blockType.getId() : null;
        this.isWide = VaryonPortalConfig.TEMPLATE_X2_ITEM_ID.equals(blockId);
        this.isTemplate = this.isWide || VaryonPortalConfig.TEMPLATE_ITEM_ID.equals(blockId);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evt, @Nonnull Store<EntityStore> store) {
        if (isTemplate) {
            cmd.append("Pages/VaryonPortalTemplateEditPage.ui");
            VaryonPortalConfig existing = VaryonPortalConfig.getAt(world, x, y, z);
            cmd.set("#CommandInput.Value", existing != null ? existing.getCommand() : "");
            cmd.set("#TypeInput.Value", existing != null ? existing.getType() : VaryonPortalConfig.DEFAULT_TYPE);
            cmd.set("#BackgroundInput.Value", existing != null ? existing.getBackground() : VaryonPortalConfig.NO_BACKGROUND);
            cmd.set("#ScaleInput.Value", existing != null ? String.valueOf(existing.getScale()) : String.valueOf(VaryonPortalConfig.DEFAULT_SCALE));
            cmd.set("#CenterOffsetYInput.Value", existing != null ? String.valueOf(existing.getCenterOffsetY()) : String.valueOf(VaryonPortalConfig.DEFAULT_CENTER_OFFSET_Y));
            cmd.set("#DirectionInput.Value", existing != null ? String.valueOf(existing.getYaw()) : String.valueOf(VaryonPortalConfig.DEFAULT_YAW));
            cmd.set("#AsServerInput.Value", existing == null || existing.isAsServer() ? "true" : "false");
        } else {
            cmd.append("Pages/VaryonPortalEditPage.ui");
            VaryonPortalCommandBlock existing = VaryonPortalCommandBlock.getAt(world, x, y, z);
            cmd.set("#CommandInput.Value", existing != null ? existing.getCommand() : "");
            cmd.set("#AsServerInput.Value", existing == null || existing.isAsServer() ? "true" : "false");
        }
        bindEvents(evt);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull VaryonPortalEditEventData data) {
        String action = data.getAction();
        if (action == null) {
            return;
        }
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if ("save".equals(action)) {
            if (isTemplate) {
                saveTemplate(store, playerRef, data);
            } else {
                saveLegacy(playerRef, data);
            }
            closePage(ref, store);
        } else if ("cancel".equals(action)) {
            closePage(ref, store);
        }
    }

    private void saveLegacy(PlayerRef playerRef, VaryonPortalEditEventData data) {
        String command = data.getCommand() != null ? data.getCommand().trim() : "";
        boolean asServer = !"false".equals(data.getAsServer());
        boolean attached = PortalCommandBlockAttacher.attach(world, x, y, z, command, asServer);
        if (playerRef != null) {
            if (attached) {
                playerRef.sendMessage(Message.raw("Commande du portail enregistrée.").color(Color.GREEN));
            } else {
                playerRef.sendMessage(Message.raw("Échec de l'enregistrement de la commande.").color(Color.RED));
            }
        }
    }

    private void saveTemplate(@Nonnull Store<EntityStore> store, PlayerRef playerRef, VaryonPortalEditEventData data) {
        String command = data.getCommand() != null ? data.getCommand().trim() : "";
        String type = data.getType() != null && !data.getType().isBlank() ? data.getType() : VaryonPortalConfig.DEFAULT_TYPE;
        String background = data.getBackground() != null ? data.getBackground().trim() : VaryonPortalConfig.NO_BACKGROUND;
        float scale = parseFloatOrDefault(data.getScale(), VaryonPortalConfig.DEFAULT_SCALE);
        float centerOffsetY = parseFloatOrDefault(data.getCenterOffsetY(), VaryonPortalConfig.DEFAULT_CENTER_OFFSET_Y);
        float yaw = parseYawOrDefault(data.getDirection());
        boolean asServer = !"false".equals(data.getAsServer());

        VaryonPortalConfig config = new VaryonPortalConfig(command, type, background, scale, centerOffsetY, yaw, asServer);
        boolean attached = PortalConfigAttacher.attach(world, x, y, z, config);

        if (attached) {
            String dimension = world.getName() != null ? world.getName() : "world";
            PortalTemplateRegistry.getInstance().save(dimension, x, y, z, type, background, scale, centerOffsetY, isWide, yaw);
            replayPortalEffect(store, config);
        }

        if (playerRef != null) {
            if (attached) {
                playerRef.sendMessage(Message.raw("Configuration du portail enregistrée.").color(Color.GREEN));
            } else {
                playerRef.sendMessage(Message.raw("Échec de l'enregistrement de la configuration.").color(Color.RED));
            }
        }
    }

    private void replayPortalEffect(@Nonnull Store<EntityStore> store, @Nonnull VaryonPortalConfig config) {
        try {
            org.joml.Vector3d center = new org.joml.Vector3d(0, y + config.getEffectiveOffsetY(), 0);
            VaryonPortalConfig.computeHorizontalCenter(world, x, y, z, isWide, center);
            ComponentAccessor<EntityStore> accessor = store;
            SpatialResource<Ref<EntityStore>, EntityStore> playerSpatial =
                    accessor.getResource(EntityModule.get().getPlayerSpatialResourceType());
            List<Ref<EntityStore>> playerRefs = SpatialResource.getThreadLocalReferenceList();
            playerSpatial.getSpatialStructure().collect(
                    center, PortalTemplateTickSystem.PARTICLE_RENDER_DISTANCE, playerRefs);

            float yawRadians = (float) Math.toRadians(config.getYaw());
            ParticleUtil.spawnParticleEffect(VaryonPortalConfig.resolveParticleSystemId(config.getType()), center.x, center.y, center.z,
                    yawRadians, 0f, 0f, config.getScale(), null, null, playerRefs, accessor,
                    PortalTemplateTickSystem.PARTICLE_MAX_DURATION_SECONDS);

            if (config.hasBackground()) {
                String backgroundId = VaryonPortalConfig.resolveBackgroundParticleSystemId(config.getType(), config.getBackground());
                ParticleUtil.spawnParticleEffect(backgroundId, center.x, center.y, center.z,
                        yawRadians, 0f, 0f, config.getScale(), null, null, playerRefs, accessor,
                        PortalTemplateTickSystem.PARTICLE_MAX_DURATION_SECONDS);
            }
        } catch (Exception ignored) {
        }
    }

    private static float parseFloatOrDefault(String raw, float fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            float value = Float.parseFloat(raw.trim());
            return value > 0 ? value : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static float parseYawOrDefault(String raw) {
        if (raw == null || raw.isBlank()) {
            return VaryonPortalConfig.DEFAULT_YAW;
        }
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException e) {
            return VaryonPortalConfig.DEFAULT_YAW;
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
    }

    private void bindEvents(@Nonnull UIEventBuilder evt) {
        if (isTemplate) {
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#SaveButton",
                    new EventData().append("Action", "save")
                            .append("@Command", "#CommandInput.Value")
                            .append("@Type", "#TypeInput.Value")
                            .append("@Background", "#BackgroundInput.Value")
                            .append("@Scale", "#ScaleInput.Value")
                            .append("@CenterOffsetY", "#CenterOffsetYInput.Value")
                            .append("@Direction", "#DirectionInput.Value")
                            .append("@AsServer", "#AsServerInput.Value"));
        } else {
            evt.addEventBinding(CustomUIEventBindingType.Activating, "#SaveButton",
                    new EventData().append("Action", "save")
                            .append("@Command", "#CommandInput.Value")
                            .append("@AsServer", "#AsServerInput.Value"));
        }
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CancelButton",
                EventData.of("Action", "cancel"));
    }

    private void closePage(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().setPage(ref, store, Page.None);
        }
    }
}
