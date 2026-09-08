package fr.varyon.trade.data;

import fr.varyon.trade.TradeConfig;
import fr.varyon.trade.TradeVaultBridge;
import fr.varyon.trade.helpers.TranslationHelper;
import com.hypixel.hytale.function.consumer.TriConsumer;
import com.hypixel.hytale.protocol.Vector2i;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TradeContentLayoutData
{
    public String name;
    public String translationKey;
    public Vector2i size;
    public String iconPath;
    public String layoutIconSelector;
    public String layoutButtonSelector;
    public Consumer<UICommandBuilder> buildFunction;

    private final static List<TradeContentLayoutData> layouts = List.of(
        new TradeContentLayoutData(
                "Horizontal",
                "ui.layout.horizontal",
                new Vector2i(950, 650),
                "Pages/Icons/horizontalLayout.png",
                "#HorizontalLayoutIcon",
                "#HorizontalLayoutBtn",
                uiBuilder -> {
                    uiBuilder.append("#TradePanel #Content", "Pages/TradePanel/ContentLayouts/HorizontalTradeContent.ui");
                    uiBuilder.insertBefore("#TradePools", "Pages/TradePanel/Elements/InventoryContainer.ui");

                    uiBuilder.insertBeforeInline("#TradePools", "Group { Anchor: (Width: 5); }");
                    uiBuilder.append("#TradePools", "Pages/TradePanel/Elements/TradePoolYours.ui");

                    uiBuilder.appendInline("#TradePools", "Group { Anchor: (Height: 5); }");
                    uiBuilder.append("#TradePools", "Pages/TradePanel/Elements/TradePoolTheirs.ui");
                }
        ),
        new TradeContentLayoutData(
            "Vertical",
            "ui.layout.vertical",
            new Vector2i(450, 950),
            "Pages/Icons/verticalLayout.png",
            "#VerticalLayoutIcon",
            "#VerticalLayoutBtn",
            uiBuilder -> {
                uiBuilder.append("#TradePanel #Content", "Pages/TradePanel/ContentLayouts/VerticalTradeContent.ui");
                uiBuilder.append("#ItemLists", "Pages/TradePanel/Elements/InventoryContainer.ui");
                uiBuilder.append("#ItemLists", "Pages/TradePanel/Elements/TradePoolYours.ui");
                uiBuilder.append("#ItemLists", "Pages/TradePanel/Elements/TradePoolTheirs.ui");
            }
        )
    );

    public TradeContentLayoutData(String name, String translationKey,
                                  Vector2i size,
                                  String iconPath,
                                  String layoutIconSelector,
                                  String layoutButtonSelector,
                                  Consumer<UICommandBuilder> buildFunction)
    {
        this.name = name;
        this.translationKey = translationKey;
        this.size = size;
        this.iconPath = iconPath;
        this.layoutIconSelector = layoutIconSelector;
        this.layoutButtonSelector = layoutButtonSelector;
        this.buildFunction = buildFunction;
    }

    public static List<TradeContentLayoutData> getAllLayouts()
    {
        return layouts;
    }
}

