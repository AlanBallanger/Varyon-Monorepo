package com.varyon.varyonui.config;

import com.moandjiezana.toml.Toml;
import com.varyon.varyonui.VaryonUIPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AdminCommandsConfig {
    private static AdminCommandsConfig instance;
    private List<CommandsConfig.CommandCategory> categories = new ArrayList<>();
    private final Path configPath;

    private AdminCommandsConfig() {
        File dataFolder = VaryonUIPlugin.getInstance().getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.configPath = dataFolder.toPath().resolve("admin_commands.toml");
        System.out.println("[VaryonUI] Admin config path: " + configPath.toAbsolutePath());
        loadConfig();
    }

    public static AdminCommandsConfig getInstance() {
        if (instance == null) {
            instance = new AdminCommandsConfig();
        }
        return instance;
    }

    public void reload() {
        categories.clear();
        loadConfig();
    }

    private void loadConfig() {
        try {
            if (!Files.exists(configPath)) {
                System.out.println("[VaryonUI] Creating default admin config file...");
                Files.createDirectories(configPath.getParent());
                try (InputStream in = getClass().getResourceAsStream("/admin_commands.toml")) {
                    if (in != null) {
                        Files.copy(in, configPath);
                        System.out.println("[VaryonUI] Default admin config created at: " + configPath);
                    } else {
                        System.err.println("[VaryonUI] Could not find /admin_commands.toml in resources!");
                    }
                }
            }

            if (Files.exists(configPath)) {
                System.out.println("[VaryonUI] Loading admin config from: " + configPath);
                Toml toml = new Toml().read(configPath.toFile());
                List<Toml> categoryList = toml.getTables("category");

                if (categoryList != null) {
                    for (Toml categoryToml : categoryList) {
                        String name = categoryToml.getString("name");
                        List<Toml> buttonsList = categoryToml.getTables("buttons");

                        List<CommandsConfig.CommandButton> buttons = new ArrayList<>();
                        if (buttonsList != null) {
                            for (Toml buttonToml : buttonsList) {
                                String label = buttonToml.getString("label");
                                String command = buttonToml.getString("command");
                                String type = buttonToml.getString("type");

                                CommandsConfig.CommandType cmdType = "chat".equalsIgnoreCase(type)
                                    ? CommandsConfig.CommandType.CHAT
                                    : CommandsConfig.CommandType.EXECUTE;

                                buttons.add(new CommandsConfig.CommandButton(label, command, cmdType, null));
                            }
                        }

                        categories.add(new CommandsConfig.CommandCategory(name, buttons));
                    }
                }
                System.out.println("[VaryonUI] Loaded " + categories.size() + " admin categories");
            }
        } catch (IOException e) {
            System.err.println("[VaryonUI] Error loading admin config:");
            e.printStackTrace();
        }
    }

    public List<CommandsConfig.CommandCategory> getCategories() {
        return categories;
    }
}
