package fr.varyon.vrpg.ui.profession;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.vrpg.VaryonRpgPlugin;
import fr.varyon.vrpg.rpg.PlayerAccount;
import fr.varyon.vrpg.rpg.ProfessionManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ProfessionAccounts {

    private ProfessionAccounts() {}

    @Nullable
    public static PlayerAccount get(@Nonnull PlayerRef playerRef) {
        ProfessionManager m = VaryonRpgPlugin.getInstance().getProfessionManager();
        if (m == null) return null;
        m.ensureAccount(playerRef.getUuid(), playerRef.getUsername());
        return m.getAccount(playerRef.getUuid());
    }
}
