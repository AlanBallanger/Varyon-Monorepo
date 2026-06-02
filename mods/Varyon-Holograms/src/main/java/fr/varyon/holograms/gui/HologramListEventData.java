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
            .addField(new KeyedCodec<>("@CreateName", Codec.STRING),
                (d, v) -> d.createName = v, d -> d.createName)
            .addField(new KeyedCodec<>("@CreateGroup", Codec.STRING),
                (d, v) -> d.createGroup = v, d -> d.createGroup)
            .addField(new KeyedCodec<>("@CreateHoloGroup", Codec.STRING),
                (d, v) -> d.createHoloGroup = v, d -> d.createHoloGroup)
            .addField(new KeyedCodec<>("GroupPath", Codec.STRING),
                (d, v) -> d.groupPath = v, d -> d.groupPath)
            .build();

    String action;
    String holoName;
    String createName;
    String createGroup;
    String createHoloGroup;
    String groupPath;

    public String getAction() { return action; }
    public String getHoloName() { return holoName; }
    public String getCreateName() { return createName; }
    public String getCreateGroup() { return createGroup; }
    public String getCreateHoloGroup() { return createHoloGroup; }
    public String getGroupPath() { return groupPath; }
}
