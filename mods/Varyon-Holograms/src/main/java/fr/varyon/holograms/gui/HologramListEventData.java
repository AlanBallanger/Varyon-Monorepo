package fr.varyon.holograms.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class HologramListEventData {

    public static final BuilderCodec<HologramListEventData> CODEC =
        BuilderCodec.builder(HologramListEventData.class, HologramListEventData::new)
            .addField(new KeyedCodec<>("Action", Codec.STRING),
                (d, v) -> d.action = v, d -> d.action)
            .addField(new KeyedCodec<>("HoloName", Codec.STRING),
                (d, v) -> d.holoName = v, d -> d.holoName)
            .build();

    String action;
    String holoName;

    public String getAction() { return action; }
    public String getHoloName() { return holoName; }
}
