package fr.varyon.trade.data;

import com.hypixel.hytale.server.core.universe.PlayerRef;

public class NotificationData
{
    public String title;
    public String subtitle;
    public String iconId;
    public String titleColor;
    public String subtitleColor;

    public NotificationData(String title, String subtitle, String iconId, String titleColor, String subtitleColor)
    {
        this.title = title;
        this.subtitle = subtitle;
        this.iconId = iconId;
        this.titleColor = titleColor;
        this.subtitleColor = subtitleColor;
    }
}

