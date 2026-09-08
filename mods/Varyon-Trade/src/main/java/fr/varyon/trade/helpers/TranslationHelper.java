package fr.varyon.trade.helpers;

import fr.varyon.trade.TradeConfig;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class TranslationHelper
{
    private static final Map<String, JsonObject> translations = new HashMap<>();
    private static JsonObject internalEnUs;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static String getItemStackDisplayName(String language, ItemStack itemStack)
    {
        return I18nModule.get().getMessage(language, itemStack.getItem().getTranslationKey());
    }

    public static void loadAllTranslations()
    {
        translations.clear();
        loadInternalEnUs();

        try
        {
            Path langFolderPath = Paths.get(TradeConfig.get().getConfigFolderPath(), "lang");
            
            if (!Files.exists(langFolderPath))
            {
                Files.createDirectories(langFolderPath);

                URL langFolderUrl = TranslationHelper.class.getClassLoader().getResource("lang");
                if (langFolderUrl != null)
                {
                    URI uri = langFolderUrl.toURI();
                    if (uri.getScheme().equals("jar"))
                    {
                        try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap()))
                        {
                            Path jarPath = fileSystem.getPath("/lang");
                            copyFromJar(jarPath, langFolderPath);
                        }
                    }
                    else
                    {
                        Path resourcePath = Paths.get(uri);
                        copyFromJar(resourcePath, langFolderPath);
                    }
                }
            }

            // Load translations from external folder
            try (Stream<Path> walk = Files.walk(langFolderPath, 1))
            {
                walk.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String langCode = fileName.substring(0, fileName.lastIndexOf('.'));
                        
                        try
                        {
                            // Merge with internal if exists
                            JsonObject externalJson;
                            try (InputStream is = Files.newInputStream(path))
                            {
                                externalJson = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
                            }

                            try (InputStream internalIs = TranslationHelper.class.getClassLoader().getResourceAsStream("lang/" + fileName))
                            {
                                if (internalIs != null)
                                {
                                    JsonObject internalJson = JsonParser.parseReader(new InputStreamReader(internalIs, StandardCharsets.UTF_8)).getAsJsonObject();
                                    boolean modified = false;
                                    for (String key : internalJson.keySet())
                                    {
                                        if (!externalJson.has(key))
                                        {
                                            externalJson.add(key, internalJson.get(key));
                                            modified = true;
                                        }
                                    }
                                    if (modified)
                                    {
                                        Files.writeString(path, GSON.toJson(externalJson), StandardCharsets.UTF_8);
                                    }
                                }
                            }

                            if (externalJson.has("language.name"))
                            {
                                translations.put(langCode, externalJson);
                            }
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    });
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private static void loadInternalEnUs()
    {
        try (InputStream is = TranslationHelper.class.getClassLoader().getResourceAsStream("lang/en-US.json"))
        {
            if (is != null)
            {
                internalEnUs = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private static void copyFromJar(Path sourceFolder, Path targetFolder) throws Exception
    {
        try (Stream<Path> walk = Files.walk(sourceFolder, 1))
        {
            walk.filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    Path targetPath = targetFolder.resolve(path.getFileName().toString());
                    if (!Files.exists(targetPath))
                    {
                        try (InputStream is = Files.newInputStream(path))
                        {
                            Files.copy(is, targetPath);
                        }
                        catch (Exception e) { e.printStackTrace(); }
                    }
                });
        }
    }

    public static String getTranslation(String key, String language)
    {
        JsonObject langJson = translations.get(language);
        
        if (langJson == null)
        {
            langJson = translations.get("en-US");
        }

        if ((langJson == null || !langJson.has(key)) && internalEnUs != null && internalEnUs.has(key))
        {
            return internalEnUs.get(key).getAsString();
        }

        if (langJson != null && langJson.has(key))
        {
            return langJson.get(key).getAsString();
        }

        return key;
    }

    public static Map<String, String> getAvailableLanguages()
    {
        Map<String, String> languages = new HashMap<>();
        
        if (internalEnUs != null && internalEnUs.has("language.name"))
        {
            languages.put("en-US", internalEnUs.get("language.name").getAsString());
        }

        for (Map.Entry<String, JsonObject> entry : translations.entrySet())
        {
            if (entry.getValue().has("language.name"))
            {
                languages.put(entry.getKey(), entry.getValue().get("language.name").getAsString());
            }
        }
        return languages;
    }
}


