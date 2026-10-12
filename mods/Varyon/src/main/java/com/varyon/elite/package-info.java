/**
 * Varyon EliteMobs - promotes eligible NPCs to tiered elite variants with a class identity,
 * telegraphed abilities, a combat brain and dedicated currencies.
 *
 * Design and rationale: {@code mods/Varyon/docs/ELITE_MOBS_DESIGN.md}.
 * Sources of inspiration: RPGMobs (Frotty27) and endless-elite-mobs / EEM (Zbeve) - see
 * {@code Ideas/_ANALYSES.md}.
 *
 * Wiring: call {@link com.varyon.elite.EliteSubsystem#register} once from {@code VaryonPlugin}
 * init, passing the loaded {@link com.varyon.elite.EliteConfig} (default via
 * {@link com.varyon.elite.EliteConfig#createDefault}).
 *
 * Current state:
 *   - spawn roll, per-tier HP/damage/tankiness scaling, EliteRegistry: done
 *   - abilities (leap/charge/dash/dodge/multi-slash/volley/arrow-rain/heal-aura/heal-leap/
 *     shield/war-cry/summon), telegraph wind-up on heavy abilities, enrage passive: done
 *   - threat table fed by incoming damage: done
 *   - currency items (Cristal du Vide / Cœur de Corruption) + deferred loot drop: done
 *   - de-elite sweep on config reload / {@code /varyon elite deelite}: done
 *   - {@code /varyon elite} command (sub-command of {@code /varyon}): (no arg) opens the
 *     dashboard; sub-commands spawn (promote nearest NPC), spawnat (spawn a fresh NPC of a
 *     given type already elite), ability, deelite. Inspection & tuning (was config / nearby
 *     / stats / setchance) is now the dashboard only.
 *   - Varyon-RPG kill-XP scaling via {@code VaryonEliteBridge}: done
 *   - weapon + armour rolled and equipped per class/tier ({@code EliteEquipment}),
 *     stripped again on de-elite; ids guarded against the item asset map; constrained by
 *     the matching {@code MobRuleDef} when one applies: done
 *   - passive per-tier effects ({@code EliteEffects} + {@code Varyon_Elite_*} assets),
 *     global inter-ability cooldown, RPGMobs-style mob rules, and per-world config
 *     overlays ({@code EliteConfig.WorldOverlay}): done
 *   - graphical admin dashboard {@code /varyon elite} ({@code EliteDashboardPage} +
 *     {@code EliteDashboard*.ui}): live view, editable per-zone chance/tier weights saved
 *     to {@code elite.toml}, read-only economy tab. Needs an in-game check that the
 *     {@code .ui} layout renders (custom UI only fails at runtime).
 */
package com.varyon.elite;
