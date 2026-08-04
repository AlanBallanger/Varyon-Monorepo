package fr.varyon.extendedteleporters.i18n;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TranslationManager {
   private static final HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("ExtendedTeleport-i18n");
   private static final String LANG_FILE_PATH = "Server/Languages/%s/extendedteleporters.lang";
   private static final String DEFAULT_LANGUAGE = "en-US";
   private static final String KEY_PREFIX = "";
   private static final Pattern PARAM_PATTERN = Pattern.compile("\\{([^}]+)}");
   private static TranslationManager instance;
   private final Map<String, Map<String, String>> translations = new ConcurrentHashMap<>();
   private String currentLanguage = "en-US";

   private TranslationManager() {
      this.loadLanguage("en-US");
   }

   public static TranslationManager getInstance() {
      if (instance == null) {
         synchronized (TranslationManager.class) {
            if (instance == null) {
               instance = new TranslationManager();
            }
         }
      }

      return instance;
   }

   public void loadLanguage(String language) {
      String path = "Server/Languages/%s/extendedteleporters.lang".formatted(language);
      Map<String, String> langMap = new HashMap<>();

      try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(path)) {
         if (is != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
               int lineNum = 0;

               String line;
               while ((line = reader.readLine()) != null) {
                  lineNum++;
                  line = line.trim();
                  if (!line.isEmpty() && !line.startsWith("#")) {
                     int equalsIndex = line.indexOf(61);
                     if (equalsIndex == -1) {
                        logger.at(Level.WARNING).log("Invalid translation line %d in %s: %s".formatted(lineNum, path, line));
                     } else {
                        String key = line.substring(0, equalsIndex).trim();
                        String value = line.substring(equalsIndex + 1).trim();
                        langMap.put(key, value);
                     }
                  }
               }
            }

            this.translations.put(language, langMap);
            logger.at(Level.INFO).log("Loaded %d translations for language: %s".formatted(langMap.size(), language));
         } else if (!language.equals("en-US")) {
            logger.at(Level.WARNING).log("Translation file not found for language: " + language + ", falling back to en-US");
         } else {
            logger.at(Level.SEVERE).log("Default translation file not found: " + path);
         }
      } catch (IOException e) {
         logger.at(Level.SEVERE).log("Error loading translations for " + language + ": " + e.getMessage());
      }
   }

   public void setCurrentLanguage(String language) {
      if (!this.translations.containsKey(language)) {
         this.loadLanguage(language);
      }

      this.currentLanguage = language;
   }

   public String getRaw(String key) {
      String fullKey = key.startsWith("") ? key : key + "";
      Map<String, String> currentLang = this.translations.get(this.currentLanguage);
      if (currentLang != null && currentLang.containsKey(fullKey)) {
         return currentLang.get(fullKey);
      }

      if (!this.currentLanguage.equals("en-US")) {
         Map<String, String> defaultLang = this.translations.get("en-US");
         if (defaultLang != null && defaultLang.containsKey(fullKey)) {
            return defaultLang.get(fullKey);
         }
      }

      logger.at(Level.WARNING).log("Missing translation key: " + fullKey);
      return key;
   }

   public String get(String key, Object... params) {
      String raw = this.getRaw(key);
      if (params.length == 0) {
         return raw;
      }

      Map<String, Object> paramMap = new HashMap<>();

      for (int i = 0; i + 1 < params.length; i += 2) {
         paramMap.put(String.valueOf(params[i]), params[i + 1]);
      }

      return this.format(raw, paramMap);
   }

   public String get(String key, Map<String, Object> params) {
      String raw = this.getRaw(key);
      return this.format(raw, params);
   }

   private String format(String text, Map<String, Object> params) {
      if (params != null && !params.isEmpty()) {
         Matcher matcher = PARAM_PATTERN.matcher(text);
         StringBuilder result = new StringBuilder();

         while (matcher.find()) {
            String paramName = matcher.group(1);
            Object value = params.get(paramName);
            String replacement = value != null ? String.valueOf(value) : matcher.group(0);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
         }

         matcher.appendTail(result);
         return result.toString();
      } else {
         return text;
      }
   }

   public Message message(String key, Object... params) {
      return Message.raw(this.get(key, params));
   }

   public Message message(String key, Color color, Object... params) {
      return Message.raw(this.get(key, params)).color(color);
   }

   public LocalizableString localizable(String key, Object... params) {
      return LocalizableString.fromString(this.get(key, params));
   }

   public Set<String> getLoadedLanguages() {
      return this.translations.keySet();
   }

   public boolean hasKey(String key) {
      String fullKey = key.startsWith("") ? key : key + "";
      Map<String, String> currentLang = this.translations.get(this.currentLanguage);
      if (currentLang != null && currentLang.containsKey(fullKey)) {
         return true;
      }

      Map<String, String> defaultLang = this.translations.get("en-US");
      return defaultLang != null && defaultLang.containsKey(fullKey);
   }

   public void reload() {
      for (String lang : new ArrayList<>(this.translations.keySet())) {
         this.loadLanguage(lang);
      }
   }
}
