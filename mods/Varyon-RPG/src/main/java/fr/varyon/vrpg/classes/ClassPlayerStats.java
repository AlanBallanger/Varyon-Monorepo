package fr.varyon.vrpg.classes;

public record ClassPlayerStats(
    int maxHp,
    int atk,
    int armorPct,
    int maxStamina,
    int critChancePct,
    int critDamagePct,
    double hpMult
) {}
