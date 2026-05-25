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

public class CommandsConfig {
    private static CommandsConfig instance;
    private List<CommandCategory> categories = new ArrayList<>();
    private final Path configPath;

    private CommandsConfig() {
        File dataFolder = VaryonUIPlugin.getInstance().getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.configPath = dataFolder.toPath().resolve("commands.toml");
        System.out.println("[VaryonUI] Config path: " + configPath.toAbsolutePath());
        loadConfig();
    }

    public static CommandsConfig getInstance() {
        if (instance == null) {
            instance = new CommandsConfig();
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
                System.out.println("[VaryonUI] Creating default config file...");
                Files.createDirectories(configPath.getParent());
                try (InputStream in = getClass().getResourceAsStream("/commands.toml")) {
                    if (in != null) {
                        Files.copy(in, configPath);
                        System.out.println("[VaryonUI] Default config created at: " + configPath);
                    } else {
                        System.err.println("[VaryonUI] Could not find /commands.toml in resources!");
                    }
                }
            }

            if (Files.exists(configPath)) {
                System.out.println("[VaryonUI] Loading config from: " + configPath);
                Toml toml = new Toml().read(configPath.toFile());
                List<Toml> categoryList = toml.getTables("category");
                
                if (categoryList != null) {
                    for (Toml categoryToml : categoryList) {
                        String name = categoryToml.getString("name");
                        String headerIcon = categoryToml.getString("header_icon");
                        List<Toml> buttonsList = categoryToml.getTables("buttons");
                        
                        List<CommandButton> buttons = new ArrayList<>();
                        if (buttonsList != null) {
                            for (Toml buttonToml : buttonsList) {
                                String label = buttonToml.getString("label");
                                String command = buttonToml.getString("command");
                                String type = buttonToml.getString("type");
                                String iconUrl = buttonToml.getString("icon_url");
                                
                                CommandType cmdType = "chat".equalsIgnoreCase(type) 
                                    ? CommandType.CHAT 
                                    : CommandType.EXECUTE;
                                
                                buttons.add(new CommandButton(label, command, cmdType, iconUrl));
                            }
                        }
                        
                        categories.add(new CommandCategory(name, buttons, headerIcon));
                    }
                }
                System.out.println("[VaryonUI] Loaded " + categories.size() + " categories");
            }
        } catch (IOException e) {
            System.err.println("[VaryonUI] Error loading config:");
            e.printStackTrace();
        }
    }

    public List<CommandCategory> getCategories() {
        return categories;
    }

    public static class CommandCategory {
        private final String name;
        private final List<CommandButton> buttons;
        private final String headerIcon;

        public CommandCategory(String name, List<CommandButton> buttons) {
            this(name, buttons, null);
        }

        public CommandCategory(String name, List<CommandButton> buttons, String headerIcon) {
            this.name = name;
            this.buttons = buttons;
            this.headerIcon = headerIcon;
        }

        public String getName() {
            return name;
        }

        public List<CommandButton> getButtons() {
            return buttons;
        }

        public String getHeaderIcon() {
            return headerIcon;
        }
    }

    public static class CommandButton {
        private final String label;
        private final String command;
        private final CommandType type;
        private final String iconUrl;

        public CommandButton(String label, String command, CommandType type, String iconUrl) {
            this.label = label;
            this.command = command;
            this.type = type;
            this.iconUrl = iconUrl;
        }

        public String getLabel() {
            return label;
        }

        public String getCommand() {
            return command;
        }

        public CommandType getType() {
            return type;
        }

        public String getIconUrl() {
            return iconUrl;
        }
    }

    public enum CommandType {
        EXECUTE,
        CHAT
    }
}
