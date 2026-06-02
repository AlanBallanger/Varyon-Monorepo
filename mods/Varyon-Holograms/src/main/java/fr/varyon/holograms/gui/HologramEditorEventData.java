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
            .addField(new KeyedCodec<>("Mode", Codec.STRING),
                (d, v) -> d.mode = v, d -> d.mode)
            .addField(new KeyedCodec<>("Anim", Codec.STRING),
                (d, v) -> d.anim = v, d -> d.anim)
            .addField(new KeyedCodec<>("Image", Codec.STRING),
                (d, v) -> d.image = v, d -> d.image)
            .addField(new KeyedCodec<>("@TextContent", Codec.STRING),
                (d, v) -> d.textContent = v, d -> d.textContent)
            .addField(new KeyedCodec<>("@ImageName", Codec.STRING),
                (d, v) -> d.imageName = v, d -> d.imageName)
            .addField(new KeyedCodec<>("@ItemId", Codec.STRING),
                (d, v) -> d.itemId = v, d -> d.itemId)
            .addField(new KeyedCodec<>("@Scale", Codec.STRING),
                (d, v) -> d.scale = v, d -> d.scale)
            .addField(new KeyedCodec<>("@ItemScale", Codec.STRING),
                (d, v) -> d.itemScale = v, d -> d.itemScale)
            .addField(new KeyedCodec<>("@HologramName", Codec.STRING),
                (d, v) -> d.hologramNameInput = v, d -> d.hologramNameInput)
            .addField(new KeyedCodec<>("@HologramGroup", Codec.STRING),
                (d, v) -> d.hologramGroupInput = v, d -> d.hologramGroupInput)
            .addField(new KeyedCodec<>("@X", Codec.STRING),
                (d, v) -> d.posX = v, d -> d.posX)
            .addField(new KeyedCodec<>("@Y", Codec.STRING),
                (d, v) -> d.posY = v, d -> d.posY)
            .addField(new KeyedCodec<>("@Z", Codec.STRING),
                (d, v) -> d.posZ = v, d -> d.posZ)
            .build();

    String action;
    String lineIndex;
    String mode;
    String anim;
    String image;
    String textContent;
    String imageName;
    String itemId;
    String scale;
    String itemScale;
    String hologramNameInput;
    String hologramGroupInput;
    String posX;
    String posY;
    String posZ;

    public String getAction() { return action; }
    public String getMode() { return mode; }
    public String getAnim() { return anim; }
    public String getImage() { return image; }
    public String getTextContent() { return textContent; }
    public String getImageName() { return imageName; }
    public String getItemId() { return itemId; }
    public String getHologramNameInput() { return hologramNameInput; }
    public String getHologramGroupInput() { return hologramGroupInput; }

    public int getLineIndexInt() {
        if (lineIndex == null) return -1;
        try { return Integer.parseInt(lineIndex); } catch (NumberFormatException e) { return -1; }
    }

    public float getScale(float def) {
        return parseFloat(scale, def);
    }

    public float getItemScale(float def) {
        return parseFloat(itemScale, def);
    }

    private static float parseFloat(String value, float def) {
        if (value == null || value.isBlank()) return def;
        try { return Float.parseFloat(value.trim()); } catch (NumberFormatException e) { return def; }
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
