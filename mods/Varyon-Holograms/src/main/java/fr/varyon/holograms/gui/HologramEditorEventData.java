package fr.varyon.holograms.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class HologramEditorEventData {

    public static final BuilderCodec<HologramEditorEventData> CODEC =
        BuilderCodec.builder(HologramEditorEventData.class, HologramEditorEventData::new)
            .addField(new KeyedCodec<>("Action", Codec.STRING),
                (d, v) -> d.action = v, d -> d.action)
            .addField(new KeyedCodec<>("LineIndex", Codec.STRING),
                (d, v) -> d.lineIndex = v, d -> d.lineIndex)
            .addField(new KeyedCodec<>("@NewText", Codec.STRING),
                (d, v) -> d.newText = v, d -> d.newText)
            .addField(new KeyedCodec<>("@X", Codec.STRING),
                (d, v) -> d.posX = v, d -> d.posX)
            .addField(new KeyedCodec<>("@Y", Codec.STRING),
                (d, v) -> d.posY = v, d -> d.posY)
            .addField(new KeyedCodec<>("@Z", Codec.STRING),
                (d, v) -> d.posZ = v, d -> d.posZ)
            .build();

    String action;
    String lineIndex;
    String newText;
    String posX;
    String posY;
    String posZ;

    public String getAction() { return action; }
    public String getNewText() { return newText; }

    public int getLineIndexInt() {
        if (lineIndex == null) return -1;
        try { return Integer.parseInt(lineIndex); } catch (NumberFormatException e) { return -1; }
    }

    public double getPosX(double def) {
        if (posX == null || posX.isBlank()) return def;
        try { return Double.parseDouble(posX); } catch (NumberFormatException e) { return def; }
    }

    public double getPosY(double def) {
        if (posY == null || posY.isBlank()) return def;
        try { return Double.parseDouble(posY); } catch (NumberFormatException e) { return def; }
    }

    public double getPosZ(double def) {
        if (posZ == null || posZ.isBlank()) return def;
        try { return Double.parseDouble(posZ); } catch (NumberFormatException e) { return def; }
    }
}
