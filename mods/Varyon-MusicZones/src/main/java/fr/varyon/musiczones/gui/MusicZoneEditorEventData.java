package fr.varyon.musiczones.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class MusicZoneEditorEventData {

    public static final BuilderCodec<MusicZoneEditorEventData> CODEC =
            BuilderCodec.builder(MusicZoneEditorEventData.class, MusicZoneEditorEventData::new)
                    .addField(new KeyedCodec<>("Action", Codec.STRING),
                            (d, v) -> d.action = v, d -> d.action)
                    .addField(new KeyedCodec<>("@ZoneId", Codec.STRING),
                            (d, v) -> d.zoneId = v, d -> d.zoneId)
                    .addField(new KeyedCodec<>("@ZoneGroup", Codec.STRING),
                            (d, v) -> d.zoneGroup = v, d -> d.zoneGroup)
                    .addField(new KeyedCodec<>("@ZoneMusic", Codec.STRING),
                            (d, v) -> d.zoneMusic = v, d -> d.zoneMusic)
                    .addField(new KeyedCodec<>("@ZoneIntensity", Codec.FLOAT),
                            (d, v) -> d.zoneIntensity = v, d -> d.zoneIntensity)
                    .addField(new KeyedCodec<>("@P1X", Codec.STRING), (d, v) -> d.p1x = v, d -> d.p1x)
                    .addField(new KeyedCodec<>("@P1Y", Codec.STRING), (d, v) -> d.p1y = v, d -> d.p1y)
                    .addField(new KeyedCodec<>("@P1Z", Codec.STRING), (d, v) -> d.p1z = v, d -> d.p1z)
                    .addField(new KeyedCodec<>("@P2X", Codec.STRING), (d, v) -> d.p2x = v, d -> d.p2x)
                    .addField(new KeyedCodec<>("@P2Y", Codec.STRING), (d, v) -> d.p2y = v, d -> d.p2y)
                    .addField(new KeyedCodec<>("@P2Z", Codec.STRING), (d, v) -> d.p2z = v, d -> d.p2z)
                    .build();

    String action;
    String zoneId;
    String zoneGroup;
    String zoneMusic;
    Float zoneIntensity;
    String p1x;
    String p1y;
    String p1z;
    String p2x;
    String p2y;
    String p2z;

    public String getAction() { return action; }
    public String getZoneId() { return zoneId; }
    public String getZoneGroup() { return zoneGroup; }
    public String getZoneMusic() { return zoneMusic; }

    public float getZoneIntensity(float def) {
        return zoneIntensity != null ? zoneIntensity : def;
    }

    public double getP1X(double def) { return parse(p1x, def); }
    public double getP1Y(double def) { return parse(p1y, def); }
    public double getP1Z(double def) { return parse(p1z, def); }
    public double getP2X(double def) { return parse(p2x, def); }
    public double getP2Y(double def) { return parse(p2y, def); }
    public double getP2Z(double def) { return parse(p2z, def); }

    private static double parse(String value, double def) {
        if (value == null || value.isBlank()) {
            return def;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
