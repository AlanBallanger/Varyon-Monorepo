package fr.varyon.extendedteleporters.i18n;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import java.awt.Color;
import java.util.Map;

public final class Translations {
   public static final Color SUCCESS = Color.GREEN;
   public static final Color ERROR = Color.RED;
   public static final Color WARNING = new Color(255, 200, 0);
   public static final Color INFO = new Color(128, 128, 128);
   public static final Color HIGHLIGHT = new Color(85, 255, 255);

   private Translations() {
   }

   public static void init() {
      TranslationManager.getInstance();
   }

   public static String tr(String key, Object... params) {
      return TranslationManager.getInstance().get(key, params);
   }

   public static String tr(String key, Map<String, Object> params) {
      return TranslationManager.getInstance().get(key, params);
   }

   public static Message msg(String key, Object... params) {
      return TranslationManager.getInstance().message(key, params);
   }

   public static Message msg(String key, Color color, Object... params) {
      return TranslationManager.getInstance().message(key, color, params);
   }

   public static Message msgSuccess(String key, Object... params) {
      return msg(key, SUCCESS, params);
   }

   public static Message msgError(String key, Object... params) {
      return msg(key, ERROR, params);
   }

   public static Message msgWarning(String key, Object... params) {
      return msg(key, WARNING, params);
   }

   public static Message msgInfo(String key, Object... params) {
      return msg(key, INFO, params);
   }

   public static LocalizableString loc(String key, Object... params) {
      return TranslationManager.getInstance().localizable(key, params);
   }

   public static boolean hasKey(String key) {
      return TranslationManager.getInstance().hasKey(key);
   }

   public static void reload() {
      TranslationManager.getInstance().reload();
   }

   public static void setLanguage(String language) {
      TranslationManager.getInstance().setCurrentLanguage(language);
   }
}
