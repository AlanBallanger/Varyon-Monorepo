package fr.varyon.vrpg.ui.classes;

import java.util.HashMap;
import java.util.Map;

public final class RpgClassUiState {

    public String pendingSpecId = null;
    public Integer pendingProfileIndex = null;
    public String classTreeSubTab = "talents";
    public String skillFilter = "all";
    public String selectedSkillSlot = null;
    public final Map<String, String> skillSlotAssignments = new HashMap<>();
    public int selectedClassNode = 0;
    public int hoveredClassNode = -1;
    public boolean classEditMode = false;
    public int skillPickerOffset = 0;
    public int[] pendingClassRanks = null;
    public int[] savedClassRanks = null;
}
