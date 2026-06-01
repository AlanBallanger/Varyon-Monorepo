package fr.varyon.holograms.placeholder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class PlaceholderIntegration {

    @Nonnull
    public String process(@Nonnull String text, @Nullable UUID playerUuid, @Nullable String playerName) {
        String result = text;
        if (playerName != null) {
            result = result.replace("%player_name%", playerName);
            result = result.replace("%player%", playerName);
        }
        if (playerUuid != null) {
            result = result.replace("%player_uuid%", playerUuid.toString());
        }
        return result;
    }

    @Nonnull
    public String process(@Nonnull String text) {
        return process(text, null, null);
    }
}
