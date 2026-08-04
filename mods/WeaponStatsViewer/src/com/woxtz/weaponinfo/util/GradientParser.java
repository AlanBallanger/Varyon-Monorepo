/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.Message
 */
package com.woxtz.weaponinfo.util;

import com.hypixel.hytale.server.core.Message;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradientParser {
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:(#[0-9A-Fa-f]{6}):(#[0-9A-Fa-f]{6})>(.*?)</gradient>");

    public static Message parse(String text) {
        Matcher matcher = GRADIENT_PATTERN.matcher(text);
        ArrayList<Message> messages = new ArrayList<Message>();
        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String before = text.substring(lastEnd, matcher.start());
                messages.add(Message.raw((String)before));
            }
            String startColor = matcher.group(1);
            String endColor = matcher.group(2);
            String content = matcher.group(3);
            Message gradient = GradientParser.createGradient(content, startColor, endColor);
            messages.add(gradient);
            lastEnd = matcher.end();
        }
        if (lastEnd < text.length()) {
            String after = text.substring(lastEnd);
            messages.add(Message.raw((String)after));
        }
        return Message.join((Message[])messages.toArray(new Message[0]));
    }

    private static Message createGradient(String text, String startHex, String endHex) {
        if (text.isEmpty()) {
            return Message.raw((String)"");
        }
        int[] startRgb = GradientParser.hexToRgb(startHex);
        int[] endRgb = GradientParser.hexToRgb(endHex);
        ArrayList<Message> chars = new ArrayList<Message>();
        int length = text.length();
        int i = 0;
        while (i < length) {
            float ratio = length == 1 ? 0.0f : (float)i / (float)(length - 1);
            int r = (int)((float)startRgb[0] + (float)(endRgb[0] - startRgb[0]) * ratio);
            int g = (int)((float)startRgb[1] + (float)(endRgb[1] - startRgb[1]) * ratio);
            int b = (int)((float)startRgb[2] + (float)(endRgb[2] - startRgb[2]) * ratio);
            String color = String.format("#%02X%02X%02X", r, g, b);
            chars.add(Message.raw((String)String.valueOf(text.charAt(i))).color(color));
            ++i;
        }
        return Message.join((Message[])chars.toArray(new Message[0]));
    }

    private static int[] hexToRgb(String hex) {
        hex = hex.replace("#", "");
        return new int[]{Integer.parseInt(hex.substring(0, 2), 16), Integer.parseInt(hex.substring(2, 4), 16), Integer.parseInt(hex.substring(4, 6), 16)};
    }
}

