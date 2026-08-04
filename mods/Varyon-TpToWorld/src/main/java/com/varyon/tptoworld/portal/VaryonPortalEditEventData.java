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
                    .build();

    String action;
    String command;

    public String getAction() {
        return action;
    }

    public String getCommand() {
        return command;
    }
}
