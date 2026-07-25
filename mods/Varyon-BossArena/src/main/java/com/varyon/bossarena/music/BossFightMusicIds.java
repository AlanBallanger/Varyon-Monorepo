package com.varyon.bossarena.music;

import java.util.Locale;

final class BossFightMusicIds {

    /** Distinct MusicContainers + track copies so client resume memory cannot reuse progress. */
    static final int RESTART_SLOTS = 8;

    private BossFightMusicIds() {}

    static String stem(String musicFileName) {
        if (musicFileName == null || musicFileName.isBlank()) {
            return "Track";
        }
        String base = musicFileName.trim().replace('\\', '/');
        if (base.contains("/")) {
            base = base.substring(base.lastIndexOf('/') + 1);
        }
        if (base.toLowerCase(Locale.ROOT).endsWith(".ogg")) {
            return base.substring(0, base.length() - 4);
        }
        return base;
    }

    static String token(String musicFileName) {
        String raw = stem(musicFileName);
        StringBuilder b = new StringBuilder();
        boolean capitalizeNext = true;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                b.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            } else if (c == '_' || c == '-' || c == '.') {
                b.append('_');
                capitalizeNext = true;
            }
        }
        String s = b.toString().replaceAll("_+", "_");
        if (s.isEmpty() || s.charAt(0) == '_') {
            return "Track_" + Integer.toHexString(raw.hashCode());
        }
        if (s.length() > 48) {
            return s.substring(0, 48);
        }
        return s;
    }

    static int normalizeSlot(int restartSlot) {
        int n = RESTART_SLOTS;
        int s = restartSlot % n;
        return s < 0 ? s + n : s;
    }

    static String musicOggFileName(String musicFileName) {
        return musicOggFileName(musicFileName, 0);
    }

    static String musicOggFileName(String musicFileName, int restartSlot) {
        return "VaryonBA_" + token(musicFileName) + "_R" + normalizeSlot(restartSlot) + ".ogg";
    }

    static String musicCommonTrackPath(String musicFileName) {
        return musicCommonTrackPath(musicFileName, 0);
    }

    static String musicCommonTrackPath(String musicFileName, int restartSlot) {
        return "Music/VaryonBA/" + musicOggFileName(musicFileName, restartSlot);
    }

    static String musicContainerId(String musicFileName) {
        return musicContainerId(musicFileName, 0);
    }

    static String musicContainerId(String musicFileName, int restartSlot) {
        return "VaryonBA_" + token(musicFileName) + "_MC" + normalizeSlot(restartSlot);
    }

    static String ambienceAssetId(String musicFileName) {
        return ambienceAssetId(musicFileName, 0);
    }

    static String ambienceAssetId(String musicFileName, int restartSlot) {
        return "VaryonBA_" + token(musicFileName) + "_Amb" + normalizeSlot(restartSlot);
    }
}
