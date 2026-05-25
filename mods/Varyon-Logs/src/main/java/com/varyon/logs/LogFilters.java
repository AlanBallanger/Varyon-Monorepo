package com.varyon.logs;

import java.util.regex.Pattern;

public final class LogFilters {

    private static final Pattern[] BASE_GAME_PATTERNS = compile(
        "\\[NPC\\|P\\] Animation .+ does not exist for model .+",
        "\\[Hytale\\]\\s+Missing interaction\\s+.+",
        "\\[AssetStore\\|[^\\]]+\\]\\s+Unused key",
        "\\[AssetStore\\|Item\\] Failed to find inherited parent asset .+",
        "\\[BlockSetModule\\|P\\] Creating block sets: Failed to find .+",
        "\\[AssetModule\\|P\\]\\s+Skipping pack at .+ missing or invalid manifest\\.json",
        "\\[AssetStore\\|[^\\]]+\\]\\s+Asset key .+ has incorrect format! Expected:",
        "\\[TagSet\\|P\\]\\s+Tag Set .+ references .+ which is not a pattern and does not otherwise exist",
        "\\[CommonAssetModule\\|P\\] Duplicated Asset Count: \\d+",
        "\\[CommonAssetModule\\|P\\] Took .+ to walk file tree and load \\d+ assets\\.",
        "\\[Hytale\\]\\s+Loading common assets phase completed!.+",
        "\\[Hytale\\]\\s+Loading common assets from:.+",
        "\\[HytaleGenerator\\]\\s+Duplicate export name for asset:.+",
        "\\[HytaleGenerator\\]\\s+Exported Scanner asset .+",
        "\\[Hytale\\]\\s+Loading assets from: /Server",
        "\\[AssetRegistryLoader\\] Took .+ to load all assets",
        "\\[AssetRegistryLoader\\] Loading assets from /Server",
        "\\[CollisionModule\\|P\\] Block extents for CollisionSystem is Max=.+, Min=.+",
        "\\[NPC\\|P\\] Validating loaded NPC configurations\\.\\.\\.",
        "\\[NPC\\|P\\] Validation complete\\.",
        "\\[NPC\\|P\\] Loaded \\d+ NPC configurations.*",
        "\\[NPC\\|P\\] Starting to load NPC builders!",
        "\\[NPC\\|P\\] Unknown JSON attribute '[^']+' found in Role\\|Variant:.+",
        "\\[I18nModule\\|P\\] Loaded \\d+ entries for '[^']+' from /Server/Languages"
    );

    private static final Pattern[] MOD_PATTERNS = compile(
        "\\[SOUT\\].*\\[Catalyst\\].*\\[Sync\\] Field .+ not found in .+",
        "\\[WeaponTooltipInjector\\] Successfully injected dynamic tooltips for \\d+ weapons\\.",
        "\\[EnchantmentRecipeManager\\] Perfect Parries mod present: .+",
        "\\[EnchantmentRecipeManager\\] Enchantment '[^']+' is disabled, will filter scrolls: .+",
        "\\[EnchantmentGlowInjector\\] EnchantmentGlowInjector: Injected glow conditions for .+",
        "\\[ItemCategoryManager\\] Populating ItemCategoryManager category cache\\.\\.\\.",
        "\\[ItemCategoryManager\\] Cached categories for \\d+ items\\.",
        "\\[PerfectParries\\] Injected stun/wake animations into \\d+ model assets",
        "\\[NPC\\|P\\] Builder 'HyCitizens_.+' validation failed:.+",
        "\\[NPC\\|P\\] Reloading entities of type 'HyCitizens_.+' because dependency .+",
        "\\[NPC\\|P\\] Reloaded NPC builder HyCitizens_.+",
        "\\[I18nModule\\|P\\] '[^']+' has multiple definitions:.+",
        "\\[ConfigManager\\] \\[EssentialsPlus\\] Updated priority for group .+",
        "\\[LuckPermsUtil\\] \\[EssentialsPlus\\] LuckPerms weight for group .+",
        "\\[ConfigManager\\] \\[EssentialsPlus\\] Preserving existing chat format for group .+"
    );

    static final Pattern[] PATTERNS = merge(BASE_GAME_PATTERNS, MOD_PATTERNS);

    private static Pattern[] compile(String... patterns) {
        Pattern[] result = new Pattern[patterns.length];
        for (int i = 0; i < patterns.length; i++) {
            result[i] = Pattern.compile(patterns[i]);
        }
        return result;
    }

    private static Pattern[] merge(Pattern[] a, Pattern[] b) {
        Pattern[] r = new Pattern[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    private LogFilters() {}
}
