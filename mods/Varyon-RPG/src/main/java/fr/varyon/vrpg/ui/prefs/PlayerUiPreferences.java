package fr.varyon.vrpg.ui.prefs;

public final class PlayerUiPreferences {

    public boolean classHudVisible = true;
    public HudCorner classHudCorner = HudCorner.TOP_LEFT;
    public int classHudOffsetX;
    public int classHudOffsetY;

    public boolean professionHudVisible = true;
    public HudCorner professionHudCorner = HudCorner.TOP_LEFT;
    public int professionHudOffsetX;
    public int professionHudOffsetY;

    public boolean xpNotificationsEnabled = true;
    public boolean skillNotificationsEnabled = true;
    public boolean professionSoundsEnabled = true;

    public PlayerUiPreferences copy() {
        PlayerUiPreferences c = new PlayerUiPreferences();
        c.classHudVisible = classHudVisible;
        c.classHudCorner = classHudCorner;
        c.classHudOffsetX = classHudOffsetX;
        c.classHudOffsetY = classHudOffsetY;
        c.professionHudVisible = professionHudVisible;
        c.professionHudCorner = professionHudCorner;
        c.professionHudOffsetX = professionHudOffsetX;
        c.professionHudOffsetY = professionHudOffsetY;
        c.xpNotificationsEnabled = xpNotificationsEnabled;
        c.skillNotificationsEnabled = skillNotificationsEnabled;
        c.professionSoundsEnabled = professionSoundsEnabled;
        return c;
    }
}
