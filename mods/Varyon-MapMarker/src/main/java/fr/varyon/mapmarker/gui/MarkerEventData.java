package fr.varyon.mapmarker.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class MarkerEventData {

    public static final BuilderCodec<MarkerEventData> CODEC =
        BuilderCodec.builder(MarkerEventData.class, MarkerEventData::new)
            .addField(new KeyedCodec<>("Action", Codec.STRING),
                (d, v) -> d.action = v, d -> d.action)
            .addField(new KeyedCodec<>("MarkerId", Codec.STRING),
                (d, v) -> d.markerId = v, d -> d.markerId)
            .addField(new KeyedCodec<>("Image", Codec.STRING),
                (d, v) -> d.image = v, d -> d.image)
            .addField(new KeyedCodec<>("GroupPath", Codec.STRING),
                (d, v) -> d.groupPath = v, d -> d.groupPath)
            .addField(new KeyedCodec<>("@MarkerName", Codec.STRING),
                (d, v) -> d.markerName = v, d -> d.markerName)
            .addField(new KeyedCodec<>("@MarkerGroup", Codec.STRING),
                (d, v) -> d.markerGroup = v, d -> d.markerGroup)
            .addField(new KeyedCodec<>("@CreateName", Codec.STRING),
                (d, v) -> d.createName = v, d -> d.createName)
            .addField(new KeyedCodec<>("@CreateImage", Codec.STRING),
                (d, v) -> d.createImage = v, d -> d.createImage)
            .build();

    String action;
    String markerId;
    String image;
    String groupPath;
    String markerName;
    String markerGroup;
    String createName;
    String createImage;

    public String getAction() { return action; }
    public String getMarkerId() { return markerId; }
    public String getImage() { return image; }
    public String getGroupPath() { return groupPath; }
    public String getMarkerName() { return markerName; }
    public String getMarkerGroup() { return markerGroup; }
    public String getCreateName() { return createName; }
    public String getCreateImage() { return createImage; }
}
