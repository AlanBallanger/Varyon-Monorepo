package fr.varyon.vrpg.ui.events;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.vrpg.VaryonRpgPlugin;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.ClassProfile;
import fr.varyon.vrpg.classes.ClassXpCurve;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.rpg.PlayerAccount;
import fr.varyon.vrpg.rpg.Profession;
import fr.varyon.vrpg.rpg.ProfessionManager;
import fr.varyon.vrpg.rpg.XpCurve;
import fr.varyon.vrpg.ui.RpgMainUI;
import fr.varyon.vrpg.ui.RpgUiAdmin;
import fr.varyon.vrpg.ui.profession.ProfessionSkillTrees;
import fr.varyon.vrpg.ui.profession.RpgProfessionUiState;
import fr.varyon.vrpg.ui.tabs.admin.AdminTab;

import javax.annotation.Nonnull;
import java.util.List;

public final class AdminUiEvents {

    private AdminUiEvents() {}

    public static UiEventResult handle(@Nonnull PlayerRef playerRef,
                                       @Nonnull RpgProfessionUiState state,
                                       @Nonnull RpgMainUI.Data data) {
        if (data.action == null) return UiEventResult.NONE;

        if ("adminSubTab".equals(data.action) && data.sub != null) {
            if (!RpgUiAdmin.isAdmin(playerRef)) return UiEventResult.NONE;
            if (AdminTab.SUB_CLASSES.equals(data.sub) || AdminTab.SUB_PROFESSIONS.equals(data.sub)) {
                state.adminSubTab = data.sub;
            }
            return UiEventResult.REBUILD;
        }

        UiEventResult shared = handleShared(playerRef, state, data);
        if (shared != UiEventResult.NONE) return shared;

        if (AdminTab.SUB_CLASSES.equals(state.adminSubTab)) {
            return handleClasses(playerRef, state, data);
        }
        return handleProfessions(playerRef, state, data);
    }

    private static UiEventResult handleShared(@Nonnull PlayerRef playerRef,
                                              @Nonnull RpgProfessionUiState state,
                                              @Nonnull RpgMainUI.Data data) {
        if ("adminPlayerNav".equals(data.action) && data.dir != null) {
            if (!RpgUiAdmin.isAdmin(playerRef)) return UiEventResult.NONE;
            List<PlayerRef> players = RpgUiAdmin.getOnlinePlayers();
            if (!players.isEmpty()) {
                int dir = "1".equals(data.dir) ? 1 : -1;
                state.adminPlayerIndex = Math.floorMod(state.adminPlayerIndex + dir, players.size());
            }
            return UiEventResult.REBUILD;
        }
        if ("adminPlayerSelf".equals(data.action)) {
            if (!RpgUiAdmin.isAdmin(playerRef)) return UiEventResult.NONE;
            List<PlayerRef> players = RpgUiAdmin.getOnlinePlayers();
            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).getUuid().equals(playerRef.getUuid())) {
                    state.adminPlayerIndex = i;
                    break;
                }
            }
            return UiEventResult.REBUILD;
        }
        return UiEventResult.NONE;
    }

    private static UiEventResult handleProfessions(@Nonnull PlayerRef playerRef,
                                                   @Nonnull RpgProfessionUiState state,
                                                   @Nonnull RpgMainUI.Data data) {
        if ("adminProfNav".equals(data.action) && data.dir != null) {
            if (!RpgUiAdmin.isAdmin(playerRef)) return UiEventResult.NONE;
            int dir = "1".equals(data.dir) ? 1 : -1;
            state.adminProfIndex = Math.floorMod(state.adminProfIndex + dir, ProfessionSkillTrees.CATALOG_ORDER.length);
            return UiEventResult.REBUILD;
        }
        if ("adminXp".equals(data.action) && data.amount != null) {
            if (!RpgUiAdmin.isAdmin(playerRef)) return UiEventResult.NONE;
            PlayerRef target = RpgUiAdmin.adminTargetRef(state);
            if (target == null) return UiEventResult.NONE;
            ProfessionManager mgr = VaryonRpgPlugin.getInstance().getProfessionManager();
            if (mgr == null) return UiEventResult.NONE;
            Profession prof = ProfessionSkillTrees.CATALOG_ORDER[state.adminProfIndex];
            try {
                int amount = Integer.parseInt(data.amount);
                mgr.addXp(target.getUuid(), prof, amount);
            } catch (NumberFormatException ignored) {}
            return UiEventResult.REBUILD;
        }
        if ("adminLevel".equals(data.action) && data.delta != null) {
            if (!RpgUiAdmin.isAdmin(playerRef)) return UiEventResult.NONE;
            PlayerRef target = RpgUiAdmin.adminTargetRef(state);
            if (target == null) return UiEventResult.NONE;
            ProfessionManager mgr = VaryonRpgPlugin.getInstance().getProfessionManager();
            if (mgr == null) return UiEventResult.NONE;
            Profession prof = ProfessionSkillTrees.CATALOG_ORDER[state.adminProfIndex];
            mgr.ensureAccount(target.getUuid(), target.getUsername());
            PlayerAccount acc = mgr.getAccount(target.getUuid());
            if (acc == null) return UiEventResult.NONE;
            int currentLevel = acc.getProgress(prof).getLevel();
            int newLevel;
            if ("max".equals(data.delta)) {
                newLevel = XpCurve.MAX_LEVEL;
            } else {
                try {
                    newLevel = Math.max(1, Math.min(XpCurve.MAX_LEVEL, currentLevel + Integer.parseInt(data.delta)));
                } catch (NumberFormatException ignored) { return UiEventResult.NONE; }
            }
            mgr.setLevel(target.getUuid(), prof, newLevel);
            return UiEventResult.REBUILD;
        }
        if ("adminResetTalents".equals(data.action)) {
            if (!RpgUiAdmin.isAdmin(playerRef)) return UiEventResult.NONE;
            PlayerRef target = RpgUiAdmin.adminTargetRef(state);
            if (target == null) return UiEventResult.NONE;
            ProfessionManager mgr = VaryonRpgPlugin.getInstance().getProfessionManager();
            if (mgr == null) return UiEventResult.NONE;
            Profession prof = ProfessionSkillTrees.CATALOG_ORDER[state.adminProfIndex];
            mgr.resetTalents(target.getUuid(), prof);
            return UiEventResult.REBUILD;
        }
        if ("adminResetAll".equals(data.action)) {
            if (!RpgUiAdmin.isAdmin(playerRef)) return UiEventResult.NONE;
            PlayerRef target = RpgUiAdmin.adminTargetRef(state);
            if (target == null) return UiEventResult.NONE;
            ProfessionManager mgr = VaryonRpgPlugin.getInstance().getProfessionManager();
            if (mgr == null) return UiEventResult.NONE;
            mgr.resetAccount(target.getUuid());
            return UiEventResult.REBUILD;
        }
        return UiEventResult.NONE;
    }

    private static UiEventResult handleClasses(@Nonnull PlayerRef playerRef,
                                               @Nonnull RpgProfessionUiState state,
                                               @Nonnull RpgMainUI.Data data) {
        if ("adminClassNav".equals(data.action) && data.dir != null) {
            if (!RpgUiAdmin.isAdmin(playerRef)) return UiEventResult.NONE;
            int dir = "1".equals(data.dir) ? 1 : -1;
            state.adminProfileIndex = Math.floorMod(state.adminProfileIndex + dir, ClassProfile.COUNT);
            return UiEventResult.REBUILD;
        }
        if ("adminClassXp".equals(data.action) && data.amount != null) {
            if (!RpgUiAdmin.isAdmin(playerRef)) return UiEventResult.NONE;
            PlayerRef target = RpgUiAdmin.adminTargetRef(state);
            if (target == null) return UiEventResult.NONE;
            ClassManager mgr = VaryonRpgPlugin.getInstance().getClassManager();
            if (mgr == null) return UiEventResult.NONE;
            mgr.ensureAccount(target.getUuid(), target.getUsername());
            ClassAccount acc = mgr.getAccount(target.getUuid());
            if (acc == null) return UiEventResult.NONE;
            PlayerClass profileClass = acc.getProfiles()[state.adminProfileIndex].getActiveClass();
            if (profileClass == null) return UiEventResult.NONE;
            try {
                int amount = Integer.parseInt(data.amount);
                mgr.addXp(target.getUuid(), profileClass, amount, target);
            } catch (NumberFormatException ignored) {}
            mgr.applyStats(target.getUuid(), target);
            return UiEventResult.REBUILD;
        }
        if ("adminClassLevel".equals(data.action) && data.delta != null) {
            if (!RpgUiAdmin.isAdmin(playerRef)) return UiEventResult.NONE;
            PlayerRef target = RpgUiAdmin.adminTargetRef(state);
            if (target == null) return UiEventResult.NONE;
            ClassManager mgr = VaryonRpgPlugin.getInstance().getClassManager();
            if (mgr == null) return UiEventResult.NONE;
            mgr.ensureAccount(target.getUuid(), target.getUsername());
            ClassAccount acc = mgr.getAccount(target.getUuid());
            if (acc == null) return UiEventResult.NONE;
            PlayerClass profileClass = acc.getProfiles()[state.adminProfileIndex].getActiveClass();
            if (profileClass == null) return UiEventResult.NONE;
            int currentLevel = acc.getProgress(profileClass).getLevel();
            int newLevel;
            if ("max".equals(data.delta)) {
                newLevel = ClassXpCurve.MAX_LEVEL;
            } else {
                try {
                    newLevel = Math.max(1, Math.min(ClassXpCurve.MAX_LEVEL, currentLevel + Integer.parseInt(data.delta)));
                } catch (NumberFormatException ignored) { return UiEventResult.NONE; }
            }
            mgr.setLevel(target.getUuid(), profileClass, newLevel);
            mgr.applyStats(target.getUuid(), target);
            return UiEventResult.REBUILD;
        }
        if ("adminClassResetTalents".equals(data.action)) {
            if (!RpgUiAdmin.isAdmin(playerRef)) return UiEventResult.NONE;
            PlayerRef target = RpgUiAdmin.adminTargetRef(state);
            if (target == null) return UiEventResult.NONE;
            ClassManager mgr = VaryonRpgPlugin.getInstance().getClassManager();
            if (mgr == null) return UiEventResult.NONE;
            mgr.ensureAccount(target.getUuid(), target.getUsername());
            ClassAccount acc = mgr.getAccount(target.getUuid());
            if (acc == null) return UiEventResult.NONE;
            PlayerClass profileClass = acc.getProfiles()[state.adminProfileIndex].getActiveClass();
            if (profileClass == null) return UiEventResult.NONE;
            mgr.resetTalents(target.getUuid(), profileClass);
            mgr.pruneInvalidSkillSlots(mgr.getOrLoad(target.getUuid()), profileClass);
            mgr.applyStats(target.getUuid(), target);
            return UiEventResult.REBUILD;
        }
        if ("adminClassResetAll".equals(data.action)) {
            if (!RpgUiAdmin.isAdmin(playerRef)) return UiEventResult.NONE;
            PlayerRef target = RpgUiAdmin.adminTargetRef(state);
            if (target == null) return UiEventResult.NONE;
            ClassManager mgr = VaryonRpgPlugin.getInstance().getClassManager();
            if (mgr == null) return UiEventResult.NONE;
            mgr.resetAccount(target.getUuid());
            mgr.applyStats(target.getUuid(), target);
            return UiEventResult.REBUILD;
        }
        return UiEventResult.NONE;
    }
}
