package fr.varyon.mapmarker.assets;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

final class MapMarkerImageHelper {

    private MapMarkerImageHelper() {
    }

    static byte[] normalizeForMapMarker(byte[] sourcePng, int size) {
        if (sourcePng == null || sourcePng.length == 0) {
            return sourcePng;
        }
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(sourcePng));
            if (source == null) {
                return sourcePng;
            }
            int iconSize = Math.max(16, size);
            BufferedImage out = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double scale = Math.min(
                    (double) iconSize / Math.max(1, source.getWidth()),
                    (double) iconSize / Math.max(1, source.getHeight()));
            int drawWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int drawHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));
            int offsetX = (iconSize - drawWidth) / 2;
            int offsetY = (iconSize - drawHeight) / 2;
            g.drawImage(source, offsetX, offsetY, drawWidth, drawHeight, null);
            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(out, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            return sourcePng;
        }
    }

    static byte[] createTransparentPng() {
        try {
            BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    static byte[] createFallbackMarkerPng(int size) {
        try {
            int iconSize = Math.max(16, size);
            BufferedImage out = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            g.setColor(new Color(28, 33, 40, 220));
            g.fillOval(1, 1, iconSize - 2, iconSize - 2);

            g.setColor(new Color(110, 123, 139, 255));
            int headSize = Math.max(6, iconSize / 3);
            int headX = (iconSize - headSize) / 2;
            int headY = Math.max(4, iconSize / 6);
            g.fillOval(headX, headY, headSize, headSize);

            int torsoWidth = Math.max(8, iconSize / 2);
            int torsoHeight = Math.max(6, iconSize / 3);
            int torsoX = (iconSize - torsoWidth) / 2;
            int torsoY = headY + headSize - Math.max(1, iconSize / 16);
            g.fill(new RoundRectangle2D.Float(
                    torsoX,
                    torsoY,
                    torsoWidth,
                    torsoHeight,
                    Math.max(4, iconSize / 5f),
                    Math.max(4, iconSize / 5f)));

            g.setColor(new Color(255, 255, 255, 38));
            g.setStroke(new BasicStroke(Math.max(1.5f, iconSize / 20f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawOval(1, 1, iconSize - 2, iconSize - 2);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(out, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            return createTransparentPng();
        }
    }
}
