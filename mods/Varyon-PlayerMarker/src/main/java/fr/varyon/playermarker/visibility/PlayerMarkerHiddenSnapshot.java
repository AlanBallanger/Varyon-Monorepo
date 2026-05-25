package fr.varyon.playermarker;

import com.hypixel.hytale.server.core.entity.entities.player.HiddenPlayersManager;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

final class PlayerMarkerHiddenSnapshot {

    private static volatile Field hiddenPlayersField;

    private PlayerMarkerHiddenSnapshot() {}

    @SuppressWarnings("unchecked")
    static Set<UUID> snapshot(HiddenPlayersManager manager) {
        if (manager == null) {
            return Collections.emptySet();
        }
        try {
            Field field = hiddenPlayersField;
            if (field == null) {
                synchronized (PlayerMarkerHiddenSnapshot.class) {
                    field = hiddenPlayersField;
                    if (field == null) {
                        Field f = HiddenPlayersManager.class.getDeclaredField("hiddenPlayers");
                        f.setAccessible(true);
                        hiddenPlayersField = f;
                        field = f;
                    }
                }
            }
            Object raw = field.get(manager);
            if (!(raw instanceof Set<?> set)) {
                return Collections.emptySet();
            }
            if (set.isEmpty()) {
                return Collections.emptySet();
            }
            return Set.copyOf((Set<UUID>) raw);
        } catch (ReflectiveOperationException e) {
            return Collections.emptySet();
        }
    }
}
