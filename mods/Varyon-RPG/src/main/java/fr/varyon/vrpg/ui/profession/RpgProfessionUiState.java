package fr.varyon.vrpg.ui.profession;

import fr.varyon.vrpg.rpg.Profession;

public final class RpgProfessionUiState {

    public Profession classementFilter = Profession.MINEUR;
    public boolean classementAsc = false;
    public int selectedNode = 0;
    public int hoveredNode = -1;
    public int selectedBonusNode = -1;
    public int hoveredBonusNode = -1;
    public final int[] skillRanks = new int[32];
    public boolean isEditMode = false;
    public int[] pendingRanks = null;
    public int[] pendingBonusRanks = null;
    public String reconvertSourceId = null;
    public int talentTreeSlotIndex = 0;
    public int adminPlayerIndex = 0;
    public int adminProfIndex = 0;
}
