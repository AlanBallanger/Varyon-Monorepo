package com.varyon.comet.config.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared lightweight JSON helpers used by config parsing.
 * <p>
 * This is intentionally minimal (no external dependencies) and is designed for
 * the current config schema shape used by Comet mod files. Key lookups use
 * indexOfKey to avoid matching key names that appear inside string values.
 */
public final class ConfigJson {

    private ConfigJson() {
    }

    /**
     * Find the index of a JSON key ("key") only when it appears as a real key (not inside a string value).
     * Requires the key to be preceded by { or , and followed by optional whitespace and :.
     */
    static int indexOfKey(String json, String key) {
        if (json == null || key == null) return -1;
        String searchKey = "\"" + key + "\"";
        int pos = 0;
        boolean inString = false;
        int i = 0;
        while (i < json.length() - searchKey.length()) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                if (!inString) {
                    // Check for key at this quote before toggling inString (keys start with ")
                    if (json.regionMatches(i, searchKey, 0, searchKey.length())) {
                        int before = i - 1;
                        while (before >= 0 && (json.charAt(before) == ' ' || json.charAt(before) == '\t' || json.charAt(before) == '\n' || json.charAt(before) == '\r')) before--;
                        if (before < 0 || json.charAt(before) == '{' || json.charAt(before) == ',' || json.charAt(before) == '"') {
                            int after = i + searchKey.length();
                            while (after < json.length() && (json.charAt(after) == ' ' || json.charAt(after) == '\t' || json.charAt(after) == '\n' || json.charAt(after) == '\r')) after++;
                            if (after < json.length() && json.charAt(after) == ':') return i;
                        }
                    }
                    inString = true;
                    i++;
                    continue;
                }
                inString = false;
                i++;
                continue;
            }
            if (inString) {
                i++;
                continue;
            }
            if (json.regionMatches(i, searchKey, 0, searchKey.length())) {
                int before = i - 1;
                while (before >= 0 && (json.charAt(before) == ' ' || json.charAt(before) == '\t' || json.charAt(before) == '\n' || json.charAt(before) == '\r')) before--;
                if (before >= 0) {
                    char b = json.charAt(before);
                    // Allow { or , (standard), or " (previous value's closing quote - handles missing comma between keys)
                    if (b != '{' && b != ',' && b != '"') {
                        i++;
                        continue;
                    }
                }
                int after = i + searchKey.length();
                while (after < json.length() && (json.charAt(after) == ' ' || json.charAt(after) == '\t' || json.charAt(after) == '\n' || json.charAt(after) == '\r')) after++;
                if (after < json.length() && json.charAt(after) == ':') return i;
            }
            i++;
        }
        return -1;
    }

    public static String extractJsonObject(String json, String key) {
        try {
            int keyIndex = indexOfKey(json, key);
            if (keyIndex == -1) {
                return null;
            }

            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) {
                return null;
            }

            int braceIndex = json.indexOf("{", colonIndex);
            if (braceIndex == -1) {
                return null;
            }

            return extractObjectFromPosition(json, braceIndex);
        } catch (Exception e) {
            return null;
        }
    }

    public static String extractJsonArray(String json, String key) {
        try {
            int keyIndex = indexOfKey(json, key);
            if (keyIndex == -1) {
                return null;
            }

            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) {
                return null;
            }

            int bracketIndex = json.indexOf("[", colonIndex);
            if (bracketIndex == -1) {
                return null;
            }

            return extractArrayFromPosition(json, bracketIndex);
        } catch (Exception e) {
            return null;
        }
    }

    public static String extractObjectFromPosition(String json, int startPos) {
        if (startPos < 0 || startPos >= json.length() || json.charAt(startPos) != '{') {
            return null;
        }

        int depth = 0;
        int endPos = startPos;
        boolean inString = false;

        for (int i = startPos; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
            }

            if (!inString) {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        endPos = i + 1;
                        break;
                    }
                }
            }
        }

        return endPos > startPos ? json.substring(startPos, endPos) : null;
    }

    /**
     * Advance past a JSON value starting at {@code start} (after optional whitespace).
     */
    static int skipJsonValue(String json, int start) {
        if (json == null || start < 0 || start >= json.length()) {
            return json != null ? json.length() : 0;
        }
        int i = start;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        if (i >= json.length()) {
            return i;
        }
        char c = json.charAt(i);
        if (c == '{') {
            String o = extractObjectFromPosition(json, i);
            return o != null ? i + o.length() : json.length();
        }
        if (c == '[') {
            String a = extractArrayFromPosition(json, i);
            return a != null ? i + a.length() : json.length();
        }
        if (c == '"') {
            i++;
            while (i < json.length()) {
                if (json.charAt(i) == '"' && json.charAt(i - 1) != '\\') {
                    return i + 1;
                }
                if (json.charAt(i) == '\\' && i + 1 < json.length()) {
                    i += 2;
                    continue;
                }
                i++;
            }
            return json.length();
        }
        if (c == 't' && i + 3 < json.length() && json.regionMatches(i, "true", 0, 4)) {
            return i + 4;
        }
        if (c == 'f' && i + 4 < json.length() && json.regionMatches(i, "false", 0, 5)) {
            return i + 5;
        }
        if (c == 'n' && i + 3 < json.length() && json.regionMatches(i, "null", 0, 4)) {
            return i + 4;
        }
        if (c == '-' || Character.isDigit(c)) {
            while (i < json.length()) {
                char ch = json.charAt(i);
                if (ch == '.' || ch == '-' || ch == '+' || ch == 'e' || ch == 'E' || Character.isDigit(ch)) {
                    i++;
                } else {
                    break;
                }
            }
            return i;
        }
        return Math.min(i + 1, json.length());
    }

    /**
     * Direct children of a JSON object whose values are objects.
     * Only top-level keys (no nested keys inside nested objects).
     */
    static List<String[]> extractTopLevelObjectEntries(String json) {
        List<String[]> out = new ArrayList<>();
        if (json == null || json.isEmpty()) {
            return out;
        }
        int i = 0;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        if (i >= json.length() || json.charAt(i) != '{') {
            return out;
        }
        i++;
        while (true) {
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
                i++;
            }
            if (i >= json.length()) {
                break;
            }
            if (json.charAt(i) == '}') {
                break;
            }
            if (json.charAt(i) == ',') {
                i++;
                continue;
            }
            if (json.charAt(i) != '"') {
                break;
            }
            int keyStart = i + 1;
            int keyEnd = keyStart;
            while (keyEnd < json.length()) {
                char ch = json.charAt(keyEnd);
                if (ch == '"' && json.charAt(keyEnd - 1) != '\\') {
                    break;
                }
                keyEnd++;
            }
            if (keyEnd >= json.length()) {
                break;
            }
            String key = json.substring(keyStart, keyEnd);
            i = keyEnd + 1;
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
                i++;
            }
            if (i >= json.length() || json.charAt(i) != ':') {
                break;
            }
            i++;
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
                i++;
            }
            if (i >= json.length()) {
                break;
            }
            if (json.charAt(i) == '{') {
                String obj = extractObjectFromPosition(json, i);
                if (obj != null) {
                    out.add(new String[]{key, obj});
                    i += obj.length();
                } else {
                    break;
                }
            } else {
                i = skipJsonValue(json, i);
            }
        }
        return out;
    }

    public static String extractArrayFromPosition(String json, int startPos) {
        if (startPos < 0 || startPos >= json.length() || json.charAt(startPos) != '[') {
            return null;
        }

        int depth = 0;
        int endPos = startPos;
        boolean inString = false;

        for (int i = startPos; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
            }

            if (!inString) {
                if (c == '[') {
                    depth++;
                } else if (c == ']') {
                    depth--;
                    if (depth == 0) {
                        endPos = i + 1;
                        break;
                    }
                }
            }
        }

        return endPos > startPos ? json.substring(startPos, endPos) : null;
    }

    public static List<String> extractArrayObjects(String arrayJson) {
        List<String> objects = new ArrayList<>();
        if (arrayJson == null || arrayJson.length() < 2) {
            return objects;
        }

        int i = 1; // skip opening bracket
        while (i < arrayJson.length()) {
            char c = arrayJson.charAt(i);
            if (c == '{') {
                String obj = extractObjectFromPosition(arrayJson, i);
                if (obj != null) {
                    objects.add(obj);
                    i += obj.length();
                    continue;
                }
            }
            i++;
        }

        return objects;
    }

    public static List<String> extractStringArray(String arrayJson) {
        List<String> strings = new ArrayList<>();
        if (arrayJson == null) {
            return strings;
        }

        Pattern pattern = Pattern.compile("\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(arrayJson);
        while (matcher.find()) {
            strings.add(matcher.group(1));
        }
        return strings;
    }

    public static List<Integer> extractIntArray(String json, String key) {
        List<Integer> ints = new ArrayList<>();
        String arrayJson = extractJsonArray(json, key);
        if (arrayJson == null) {
            return ints;
        }

        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher matcher = pattern.matcher(arrayJson);
        while (matcher.find()) {
            try {
                ints.add(Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException ignored) {
            }
        }

        return ints;
    }

    public static String extractStringValue(String json, String key) {
        try {
            int keyIndex = indexOfKey(json, key);
            if (keyIndex == -1) {
                return null;
            }

            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) {
                return null;
            }

            int startQuote = json.indexOf("\"", colonIndex + 1);
            if (startQuote == -1) {
                return null;
            }

            int endQuote = json.indexOf("\"", startQuote + 1);
            if (endQuote == -1) {
                return null;
            }

            return json.substring(startQuote + 1, endQuote);
        } catch (Exception e) {
            return null;
        }
    }

    public static Integer extractIntValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) {
                return null;
            }

            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) {
                return null;
            }

            int startIndex = colonIndex + 1;
            while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
                startIndex++;
            }

            StringBuilder num = new StringBuilder();
            while (startIndex < json.length()) {
                char c = json.charAt(startIndex);
                if (Character.isDigit(c) || c == '-') {
                    num.append(c);
                    startIndex++;
                } else {
                    break;
                }
            }

            return num.length() > 0 ? Integer.parseInt(num.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Double extractDoubleValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) {
                return null;
            }

            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) {
                return null;
            }

            int startIndex = colonIndex + 1;
            while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
                startIndex++;
            }

            StringBuilder num = new StringBuilder();
            while (startIndex < json.length()) {
                char c = json.charAt(startIndex);
                if (Character.isDigit(c) || c == '.' || c == '-') {
                    num.append(c);
                    startIndex++;
                } else {
                    break;
                }
            }

            return num.length() > 0 ? Double.parseDouble(num.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Boolean extractBooleanValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) {
                return null;
            }

            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) {
                return null;
            }

            String afterColon = json.substring(colonIndex + 1).trim();
            if (afterColon.startsWith("true")) {
                return true;
            }
            if (afterColon.startsWith("false")) {
                return false;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parses a flat JSON object {@code { "key": 1, "key2": 2 }} into string→positive int weights.
     * Keys starting with {@code _} are ignored (pseudo-comments).
     */
    public static java.util.Map<String, Integer> extractStringIntMapFromObject(String objectJson) {
        java.util.Map<String, Integer> out = new java.util.LinkedHashMap<>();
        if (objectJson == null || objectJson.isEmpty()) {
            return out;
        }
        Pattern p = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(-?(?:\\d+)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)");
        Matcher m = p.matcher(objectJson);
        while (m.find()) {
            String key = m.group(1);
            if (key.startsWith("_")) {
                continue;
            }
            double d = Double.parseDouble(m.group(2));
            int w = (int) Math.round(d);
            if (w > 0) {
                out.put(key, w);
            }
        }
        return out;
    }

    public static String extractPrimitiveValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) {
                return null;
            }

            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) {
                return null;
            }

            int startIndex = colonIndex + 1;
            while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
                startIndex++;
            }

            int endIndex = startIndex;
            while (endIndex < json.length()) {
                char c = json.charAt(endIndex);
                if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) {
                    break;
                }
                endIndex++;
            }

            return json.substring(startIndex, endIndex).trim();
        } catch (Exception e) {
            return null;
        }
    }
}
