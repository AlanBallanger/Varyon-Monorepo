package fr.varyon.extendedteleporters.util;

import fr.varyon.extendedteleporters.i18n.Translations;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

public final class WarpNameValidator {
   public static final int MAX_NAME_LENGTH = 64;
   private static final String JSON_BREAKING_CHARS = "\"\\";
   private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x1F\\x7F]");
   private static final Pattern COUNTER_PATTERN = Pattern.compile("^(.+?)\\s*\\((\\d+)\\)$");

   private WarpNameValidator() {
   }

   public static WarpNameValidator.ValidationResult validate(@Nullable String name) {
      if (name != null && !name.isEmpty()) {
         String trimmed = name.trim();
         if (trimmed.isEmpty()) {
            return WarpNameValidator.ValidationResult.failure(Translations.tr("msg.warpName.whitespaceOnly"), "whitespace-only");
         }

         if (name.length() > 64) {
            return WarpNameValidator.ValidationResult.failure(Translations.tr("msg.warpName.tooLong", "max", 64), "too long (" + name.length() + " chars)");
         }

         for (char c : "\"\\".toCharArray()) {
            if (name.indexOf(c) >= 0) {
               String charName = switch (c) {
                  case '"' -> "double quote (\")";
                  case '\\' -> "backslash (\\)";
                  default -> String.valueOf(c);
               };
               return WarpNameValidator.ValidationResult.failure(
                  Translations.tr("msg.warpName.invalidChar", "char", charName), "contains '" + c + "' (JSON-breaking character)"
               );
            }
         }

         return CONTROL_CHARS.matcher(name).find()
            ? WarpNameValidator.ValidationResult.failure(Translations.tr("msg.warpName.controlChars"), "contains control characters")
            : WarpNameValidator.ValidationResult.success();
      } else {
         return WarpNameValidator.ValidationResult.success();
      }
   }

   public static String sanitizeForDisplay(@Nullable String name) {
      if (name != null && !name.isEmpty()) {
         String sanitized = CONTROL_CHARS.matcher(name).replaceAll("");
         sanitized = sanitized.replace("\"", "").replace("\\", "");
         if (sanitized.length() > 64) {
            sanitized = sanitized.substring(0, 64);
         }

         return sanitized;
      } else {
         return "";
      }
   }

   public static boolean isDuplicate(@Nullable String name, Collection<String> existingNames) {
      if (name != null && !name.isEmpty() && existingNames != null) {
         String lowerName = name.toLowerCase();

         for (String existing : existingNames) {
            if (existing != null && existing.toLowerCase().equals(lowerName)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static String generateUniqueName(String baseName, Collection<String> existingNames) {
      if (baseName != null && !baseName.isEmpty()) {
         if (existingNames != null && !existingNames.isEmpty()) {
            String lowerBaseName = baseName.toLowerCase();
            boolean baseExists = false;
            int highestCounter = 1;
            String actualBaseName = baseName;
            Matcher baseMatcher = COUNTER_PATTERN.matcher(baseName);
            if (baseMatcher.matches()) {
               actualBaseName = baseMatcher.group(1).trim();
               lowerBaseName = actualBaseName.toLowerCase();
            }

            for (String existing : existingNames) {
               if (existing != null) {
                  String lowerExisting = existing.toLowerCase();
                  if (lowerExisting.equals(lowerBaseName)) {
                     baseExists = true;
                  } else {
                     Matcher matcher = COUNTER_PATTERN.matcher(existing);
                     if (matcher.matches()) {
                        String existingBase = matcher.group(1).trim().toLowerCase();
                        if (existingBase.equals(lowerBaseName)) {
                           baseExists = true;
                           int counter = Integer.parseInt(matcher.group(2));
                           if (counter >= highestCounter) {
                              highestCounter = counter + 1;
                           }
                        }
                     }
                  }
               }
            }

            if (!baseExists && !isDuplicate(baseName, existingNames)) {
               return baseName;
            }

            String newName = actualBaseName + " (" + highestCounter + ")";
            if (newName.length() > 64) {
               int counterSuffix = (" (" + highestCounter + ")").length();
               actualBaseName = actualBaseName.substring(0, 64 - counterSuffix);
               newName = actualBaseName + " (" + highestCounter + ")";
            }

            return newName;
         } else {
            return baseName;
         }
      } else {
         return baseName;
      }
   }

   public static final class ValidationResult {
      private final boolean valid;
      @Nullable
      private final String errorMessage;
      @Nullable
      private final String reason;

      private ValidationResult(boolean valid, @Nullable String errorMessage, @Nullable String reason) {
         this.valid = valid;
         this.errorMessage = errorMessage;
         this.reason = reason;
      }

      public boolean isValid() {
         return this.valid;
      }

      @Nullable
      public String getErrorMessage() {
         return this.errorMessage;
      }

      @Nullable
      public String getReason() {
         return this.reason;
      }

      public static WarpNameValidator.ValidationResult success() {
         return new WarpNameValidator.ValidationResult(true, null, null);
      }

      public static WarpNameValidator.ValidationResult failure(String errorMessage, String reason) {
         return new WarpNameValidator.ValidationResult(false, errorMessage, reason);
      }
   }
}
