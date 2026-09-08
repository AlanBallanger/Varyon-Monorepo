package fr.varyon.trade.pages;

import fr.varyon.trade.data.PlayerConfigData;
import fr.varyon.trade.data.EventActionData;
import fr.varyon.trade.data.TradeContentLayoutData;
import fr.varyon.trade.helpers.TranslationHelper;
import com.google.gson.JsonElement;
import fr.varyon.trade.helpers.TranslationHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Settings extends InteractiveCustomUIPage<EventActionData>
{
    private PlayerConfigData playerConfigData = null;
    private String language = "en-US";

    public Settings(@NonNullDecl PlayerRef playerRef)
    {
        super(playerRef, CustomPageLifetime.CanDismiss, EventActionData.CODEC);
        playerConfigData = PlayerConfigData.getConfigData(playerRef.getUuid());
        language = playerConfigData.vars.language.getValue();
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref,
                      @NonNullDecl UICommandBuilder uiBuilder,
                      @NonNullDecl UIEventBuilder eventBuilder,
                      @NonNullDecl Store<EntityStore> store)
    {
        uiBuilder.append("Pages/Settings/Settings.ui");

        buildLayoutPosition(uiBuilder, eventBuilder);
        buildIgnoreTradeCheckbox(uiBuilder, eventBuilder);
        buildIgnoredPlayersList(uiBuilder, eventBuilder, "");
        buildLanguageDropdown(uiBuilder, eventBuilder);

        translate(uiBuilder);

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.FocusLost,
                "#TextField",
                new EventData()
                        .append("ActionId", "SEARCH_IGNORED_PLAYERS")
                        .append("@Search", "#Settings #TextField.Value")
        );

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SearchBtn",
                new EventData()
                        .append("ActionId", "SEARCH_IGNORED_PLAYERS")
                        .append("@Search", "#Settings #TextField.Value")
        );

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#AddBtn",
                new EventData()
                        .append("ActionId", "ADD_IGNORED_PLAYER")
                        .append("@Search", "#Settings #TextField.Value")
        );

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#Settings #CloseBtn",
                new EventData().append("ActionId", "CLOSE")
        );
    }

    private void buildLayoutPosition(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder)
    {
        uiBuilder.clear("#LayoutPositionButtons");
        buildPositionButtons(uiBuilder, eventBuilder);
        uiBuilder.appendInline("#LayoutPositionButtons", "Group { FlexWeight: 1; }");
        buildLayoutButtons(uiBuilder, eventBuilder);
    }

    private void buildIgnoreTradeCheckbox(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder)
    {
        boolean tradeIgnore = playerConfigData.vars.tradeIgnore.getValue();

        uiBuilder.set("#IgnoreTradeRequestsCheckbox #CheckBox.Value", tradeIgnore);

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#IgnoreTradeRequestsCheckbox #CheckBox",
                EventData.of("@IgnoreTradeRequests", "#IgnoreTradeRequestsCheckbox #CheckBox.Value")
                         .append("ActionId", "IGNORE_ALL_TRADE_UPDATE")
        );
    }

    private void buildPositionButtons(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder)
    {
        int currentPos = playerConfigData.vars.tradePanelPosition.getValue();

        var buttons = new Object[][] {
                { "#posLeft",   "left.png",   -1, "Left" },
                { "#posCenter", "center.png",  0, "Center" },
                { "#posRight",  "right.png",   1, "Right" }
        };

        for (var btn : buttons)
        {
            String id = (String) btn[0];
            String icon = (String) btn[1];
            int posValue = (int) btn[2];
            String tooltip = (String) btn[3];

            String uiFile = (currentPos == posValue) ? "SelectedButton.ui" : "TertiaryButton.ui";

            uiBuilder.appendInline("#LayoutPositionButtons", "Group " + id + " {}");
            uiBuilder.append("#LayoutPositionButtons " + id, "Pages/Settings/" + uiFile);
            uiBuilder.set("#LayoutPositionButtons " + id + " #Button #Icon.Background", "Pages/Icons/" + icon);
            uiBuilder.set("#LayoutPositionButtons " + id + " #Button.TooltipText", tooltip);

            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                    "#LayoutPositionButtons " + id + " #Button",
                    new EventData().append("ActionId", "POS_UPDATE")
                                   .append("NewPos", String.valueOf(posValue)));
        }
    }

    private void buildLayoutButtons(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder)
    {
        List<TradeContentLayoutData> contentLayouts = TradeContentLayoutData.getAllLayouts();
        TradeContentLayoutData selectedLayout = contentLayouts.stream()
                .filter(el -> el.name.equals(playerConfigData.vars.tradePanelLayoutName.getValue()))
                .findFirst().get();

        for (TradeContentLayoutData layout : contentLayouts)
        {
            uiBuilder.appendInline("#LayoutPositionButtons", "Group #%s {}".formatted(layout.name));
            uiBuilder.append("#LayoutPositionButtons #%s".formatted(layout.name),
                    "Pages/Settings/" + (selectedLayout.name.equals(layout.name) ?
                                         "SelectedButton.ui" : "TertiaryButton.ui"));
            uiBuilder.set("#LayoutPositionButtons #%s #Button #Icon.Background".formatted(layout.name), layout.iconPath);
            uiBuilder.set("#LayoutPositionButtons #%s #Button.TooltipText".formatted(layout.name), layout.name);

            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#LayoutPositionButtons #%s #Button".formatted(layout.name),
                    new EventData().append("ActionId", "LAYOUT_UPDATE").append("NewLayout", layout.name));
        }
    }

    private void buildIgnoredPlayersList(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder, String search)
    {
        uiBuilder.clear("#IgnoredPlayersList");

        List<PlayerConfigData.IgnoredPlayer> ignoredPlayersList = playerConfigData.vars.ignoredPlayers.getValue();
        for (PlayerConfigData.IgnoredPlayer ignoredPlayer : ignoredPlayersList)
        {
            if (search.length() > 0 &&
                    !ignoredPlayer.uuid.toUpperCase().contains(search.toUpperCase()) &&
                    !ignoredPlayer.username.toUpperCase().contains(search.toUpperCase()))
            {
                continue;
            }

            String identifier = "#Player" + toHashLetterString(ignoredPlayer.username);

            uiBuilder.appendInline("#IgnoredPlayersList", "Group %s {}".formatted(identifier));
            uiBuilder.append("#IgnoredPlayersList %s".formatted(identifier), "Pages/Settings/IgnoredPlayerLine.ui");
            uiBuilder.set("#IgnoredPlayersList %s #PlayerName.Text".formatted(identifier),
                    "%s (%s)".formatted(ignoredPlayer.username, ignoredPlayer.uuid));

            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#IgnoredPlayersList %s #RemovePlayer".formatted(identifier),
                    EventData.of("ActionId", "REMOVE_IGNORED_PLAYER").append("Uuid", ignoredPlayer.uuid)
            );
        }
    }

    public String toHashLetterString(String input)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());

            StringBuilder letters = new StringBuilder();
            for (byte b : hash) {
                int val = Byte.toUnsignedInt(b);
                letters.append((char) ('a' + (val % 26)));
            }
            return letters.toString();
        }
        catch (Exception e) {}

        return "";
    }

    private void refresh()
    {
        language = playerConfigData.vars.language.getValue();

        UICommandBuilder uiBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();

        buildLayoutPosition(uiBuilder, eventBuilder);

        translate(uiBuilder);

        sendUpdate(uiBuilder, eventBuilder, false);
    }

    private void translate(UICommandBuilder uiBuilder)
    {
        uiBuilder.set("#SettingsTitle.Text", TranslationHelper.getTranslation("ui.settings.title", language));
        uiBuilder.set("#LanguageTitle.Text", TranslationHelper.getTranslation("ui.settings.language", language));
        uiBuilder.set("#PositionLayoutTitle.Text", TranslationHelper.getTranslation("ui.settings.position_and_layout", language));
        uiBuilder.set("#IgnoredPlayerTitle.Text", TranslationHelper.getTranslation("ui.settings.ignored_players", language));
        uiBuilder.set("#TextField.PlaceholderText", TranslationHelper.getTranslation("ui.settings.field_placeholder", language));
        uiBuilder.set("#IgnoreTradeRequestsCheckbox[1].Text",
                TranslationHelper.getTranslation("ui.settings.ignore_all_trade_requests", language));
        uiBuilder.set("#CloseBtn.Text", TranslationHelper.getTranslation("ui.settings.close", language));
        uiBuilder.set("#Signature.Text", TranslationHelper.getTranslation("ui.settings.signature", language));

        List<TradeContentLayoutData> layouts = TradeContentLayoutData.getAllLayouts();
        for (TradeContentLayoutData layout : layouts)
        {
            uiBuilder.set("#LayoutPositionButtons #%s #Button.TooltipText".formatted(layout.name),
                    TranslationHelper.getTranslation(layout.translationKey, language));
        }

        uiBuilder.set("#posLeft #Button.TooltipText", TranslationHelper.getTranslation("ui.settings.pos.left", language));
        uiBuilder.set("#posCenter #Button.TooltipText", TranslationHelper.getTranslation("ui.settings.pos.center", language));
        uiBuilder.set("#posRight #Button.TooltipText", TranslationHelper.getTranslation("ui.settings.pos.right", language));
    }

    private void buildLanguageDropdown(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder)
    {
        List<DropdownEntryInfo> languagesEntries = new ArrayList<>();
        Map<String, String> availableLanguages = TranslationHelper.getAvailableLanguages();
        availableLanguages.forEach((code, name) -> {
            DropdownEntryInfo entry = new DropdownEntryInfo(LocalizableString.fromString(name), code);
            if (code.equals(language))
            {
                languagesEntries.addFirst(entry);
            }
            else
            {
                languagesEntries.add(entry);
            }
        });
        uiBuilder.set("#LanguageSelector.Entries", languagesEntries);

        uiBuilder.set("#LanguageSelector.Value", availableLanguages.containsKey(language) ?
                                                 language : "en-US");

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#LanguageSelector",
                EventData.of("ActionId", "LANGUAGE_CHANGED").append("@Language", "#LanguageSelector.Value")
        );
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, String rawData)
    {
        JsonElement data = JsonParser.parseString(rawData);
        JsonObject jsonData = data.getAsJsonObject();

        String actionId = jsonData.get("ActionId").getAsString();
        switch (actionId)
        {
            case "LANGUAGE_CHANGED" -> changeLanguage(jsonData.get("@Language").getAsString());
            case "IGNORE_ALL_TRADE_UPDATE" -> updateIgnoreAllTrades(jsonData.get("@IgnoreTradeRequests").getAsBoolean());
            case "POS_UPDATE" -> updatePos(jsonData.get("NewPos").getAsInt());
            case "LAYOUT_UPDATE" -> updateLayout(jsonData.get("NewLayout").getAsString());
            case "SEARCH_IGNORED_PLAYERS" -> searchIgnoredPlayer(jsonData.get("@Search").getAsString(), false);
            case "ADD_IGNORED_PLAYER" -> addIgnoredPlayer(jsonData.get("@Search").getAsString());
            case "REMOVE_IGNORED_PLAYER" -> removeIgnoredPlayer(jsonData.get("Uuid").getAsString());
            case "CLOSE" -> close();
        }

        refresh();
    }

    private void changeLanguage(String newLanguage)
    {
        playerConfigData.vars.language.setValue(newLanguage);
        refresh();
    }

    private void updateIgnoreAllTrades(boolean newValue)
    {
        playerConfigData.vars.tradeIgnore.setValue(newValue);
    }

    private void updatePos(int newPos)
    {
        playerConfigData.vars.tradePanelPosition.setValue(newPos);
    }

    private void updateLayout(String newLayoutName)
    {
        playerConfigData.vars.tradePanelLayoutName.setValue(newLayoutName);
    }

    private void searchIgnoredPlayer(String search, boolean emptySearchBar)
    {
        UICommandBuilder uiBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();

        if (emptySearchBar) uiBuilder.set("#Settings #TextField.Value", "");
        buildIgnoredPlayersList(uiBuilder, eventBuilder, search);

        sendUpdate(uiBuilder, eventBuilder, false);
    }

    private void addIgnoredPlayer(String search)
    {
        if (search.isEmpty()) return;

        PlayerRef playerRef = Universe.get().getPlayerByUsername(search, NameMatching.STARTS_WITH_IGNORE_CASE);

        if (playerRef != null)
        {
            List<PlayerConfigData.IgnoredPlayer> ignoredPlayers = playerConfigData.vars.ignoredPlayers.getValue();
            boolean isAlreadyIgnored = ignoredPlayers.stream().filter(el -> {
                return el.username.equals(search) || el.uuid.equals(search);
            }).count() > 0;

            if (!isAlreadyIgnored)
            {
                ignoredPlayers.add(new PlayerConfigData.IgnoredPlayer(playerRef.getUsername(), playerRef.getUuid().toString()));
                playerConfigData.vars.ignoredPlayers.setValue(ignoredPlayers);

                searchIgnoredPlayer("", true);
            }
        }
    }

    private void removeIgnoredPlayer(String uuid)
    {
        List<PlayerConfigData.IgnoredPlayer> ignoredPlayers = playerConfigData.vars.ignoredPlayers.getValue();
        ignoredPlayers = ignoredPlayers.stream().filter(el -> !el.uuid.equals(uuid)).collect(Collectors.toList());
        playerConfigData.vars.ignoredPlayers.setValue(ignoredPlayers);

        UICommandBuilder uiBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();

        uiBuilder.set("#Settings #TextField.Value", "");
        buildIgnoredPlayersList(uiBuilder, eventBuilder, "");

        sendUpdate(uiBuilder, eventBuilder, false);
    }
}

