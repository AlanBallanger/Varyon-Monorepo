package fr.varyon.vrpg.ui.classes;

import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassTalentTree;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.ui.classes.layout.ClassTalentTreeLayouts;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class ClassUnlockedActiveSkills {

    public record Entry(int nodeIndex, @Nonnull ClassTalentTree.Node node) {}

    private ClassUnlockedActiveSkills() {}

    @Nonnull
    public static List<Entry> list(@Nullable ClassAccount acc) {
        if (acc == null) return List.of();
        PlayerClass activeClass = acc.getActiveClass();
        if (activeClass == null) return List.of();
        PlayerSpecialization spec = acc.getActiveSpec(activeClass);
        if (spec == null) return List.of();

        ClassTalentTree.Node[] nodes = ClassTalentTreeLayouts.talentNodes(acc);
        List<Entry> out = new ArrayList<>();
        for (int i = 0; i < nodes.length; i++) {
            ClassTalentTree.Node node = nodes[i];
            if (!"Actif".equals(node.type())) continue;
            String talentKey = fr.varyon.vrpg.ui.classes.ClassTalentTreeLogic.nodeKey(acc, activeClass, i);
            if (acc.getTalentRank(activeClass, talentKey) <= 0) continue;
            out.add(new Entry(i, node));
        }
        return List.copyOf(out);
    }

    public static boolean isUnlockedActive(@Nullable ClassAccount acc, @Nullable String itemId) {
        if (itemId == null || itemId.isBlank()) return false;
        for (Entry e : list(acc)) {
            if (itemId.equals(e.node().itemId())) return true;
        }
        return false;
    }
}
