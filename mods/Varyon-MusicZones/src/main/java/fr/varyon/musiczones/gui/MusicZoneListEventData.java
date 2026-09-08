package fr.varyon.musiczones.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class MusicZoneListEventData {

    public static final BuilderCodec<MusicZoneListEventData> CODEC =
            BuilderCodec.builder(MusicZoneListEventData.class, MusicZoneListEventData::new)
                    .addField(new KeyedCodec<>("Action", Codec.STRING),
                            (d, v) -> d.action = v, d -> d.action)
                    .addField(new KeyedCodec<>("ZoneId", Codec.STRING),
                            (d, v) -> d.zoneId = v, d -> d.zoneId)
                    .addField(new KeyedCodec<>("GroupPath", Codec.STRING),
                            (d, v) -> d.groupPath = v, d -> d.groupPath)
                    .addField(new KeyedCodec<>("@CreateName", Codec.STRING),
                            (d, v) -> d.createName = v, d -> d.createName)
                    .addField(new KeyedCodec<>("@CreateGroup", Codec.STRING),
                            (d, v) -> d.createGroup = v, d -> d.createGroup)
                    .addField(new KeyedCodec<>("@CreateZoneGroup", Codec.STRING),
                            (d, v) -> d.createZoneGroup = v, d -> d.createZoneGroup)
                    .build();

    String action;
    String zoneId;
    String groupPath;
    String createName;
    String createGroup;
    String createZoneGroup;

    public String getAction() { return action; }
    public String getZoneId() { return zoneId; }
    public String getGroupPath() { return groupPath; }
    public String getCreateName() { return createName; }
    public String getCreateGroup() { return createGroup; }
    public String getCreateZoneGroup() { return createZoneGroup; }
}
