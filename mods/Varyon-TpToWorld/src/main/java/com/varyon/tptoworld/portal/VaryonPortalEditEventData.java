package com.varyon.tptoworld.portal;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class VaryonPortalEditEventData {

    public static final BuilderCodec<VaryonPortalEditEventData> CODEC =
            BuilderCodec.builder(VaryonPortalEditEventData.class, VaryonPortalEditEventData::new)
                    .addField(new KeyedCodec<>("Action", Codec.STRING),
                            (d, v) -> d.action = v, d -> d.action)
                    .addField(new KeyedCodec<>("@Command", Codec.STRING),
                            (d, v) -> d.command = v, d -> d.command)
                    .addField(new KeyedCodec<>("@Type", Codec.STRING),
                            (d, v) -> d.type = v, d -> d.type)
                    .addField(new KeyedCodec<>("@Background", Codec.STRING),
                            (d, v) -> d.background = v, d -> d.background)
                    .addField(new KeyedCodec<>("@Scale", Codec.STRING),
                            (d, v) -> d.scale = v, d -> d.scale)
                    .addField(new KeyedCodec<>("@CenterOffsetY", Codec.STRING),
                            (d, v) -> d.centerOffsetY = v, d -> d.centerOffsetY)
                    .addField(new KeyedCodec<>("@Direction", Codec.STRING),
                            (d, v) -> d.direction = v, d -> d.direction)
                    .addField(new KeyedCodec<>("@AsServer", Codec.STRING),
                            (d, v) -> d.asServer = v, d -> d.asServer)
                    .build();

    String action;
    String command;
    String type;
    String background;
    String scale;
    String centerOffsetY;
    String direction;
    String asServer;

    public String getAction() {
        return action;
    }

    public String getCommand() {
        return command;
    }

    public String getType() {
        return type;
    }

    public String getBackground() {
        return background;
    }

    public String getScale() {
        return scale;
    }

    public String getCenterOffsetY() {
        return centerOffsetY;
    }

    public String getDirection() {
        return direction;
    }

    public String getAsServer() {
        return asServer;
    }
}
