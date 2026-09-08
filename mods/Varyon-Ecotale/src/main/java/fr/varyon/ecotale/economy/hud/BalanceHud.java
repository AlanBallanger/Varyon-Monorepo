package fr.varyon.ecotale.economy.hud;

import fr.varyon.ecotale.economy.lib.simplehud.SimpleHud;
import fr.varyon.ecotale.economy.lib.simplehud.HudScheduler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.Message;

import java.util.concurrent.ScheduledFuture;

/**
 * HUD to display player balance with smart animated counting.
 * Features:
 * - Progressive counter animation
 * - Trailing digits for large balances (shows ...005 during animation)
 * @author michiweon
 */
public class BalanceHud extends SimpleHud {
    
    // HUD enabled/disabled is now controlled by config: EnableHudDisplay

    // Low-frequency repaint model: the HUD is deliberately unhurried so that a burst of
    // balance changes (mining, coin pickups) coalesces into a handful of packets instead
    // of restarting a fast per-change animation every time.
    private static final long TICK_INTERVAL_MS = 400;
    private static final int MAX_TICKS = 6;

    // If change is less than 0.1% of balance, use trailing digits
    private static final double TRAILING_THRESHOLD = 0.001;
    private final PlayerRef ownerRef;


    private double displayedBalance = 0;
    private double targetBalance = 0;
    private boolean useTrailingDigits = false;
    private ScheduledFuture<?> animationFuture;
    private boolean animating = false;
    private boolean warnedDisabled = false;

    public BalanceHud(PlayerRef playerRef) {
        super(playerRef, "Pages/Ecotale_BalanceHud.ui");
        this.ownerRef = playerRef;
        
        double currentBalance = fr.varyon.ecotale.VaryonEcotalePlugin.getInstance().getEconomyManager().getBalance(playerRef.getUuid());
        displayedBalance = currentBalance;
        targetBalance = currentBalance;
        
        updateDisplayFinal(currentBalance);
    }

    @Override
    protected void build(com.hypixel.hytale.server.core.ui.builder.UICommandBuilder builder) {
        // Check config instead of hardcoded boolean
        if (!fr.varyon.ecotale.VaryonEcotalePlugin.getInstance().getEconomyConfig().isEnableHudDisplay()) {
            return;
        }
        super.build(builder);
    }

    public void updateBalance(double newBalance) {
        if (Math.abs(newBalance - targetBalance) < 0.01) {
            return;
        }
        
        // If animation is disabled, update instantly (safer for MultipleHUD compatibility)
        if (!fr.varyon.ecotale.VaryonEcotalePlugin.getInstance().getEconomyConfig().isEnableHudAnimation()) {
            targetBalance = newBalance;
            displayedBalance = newBalance;
            updateDisplayFinal(newBalance);
            return;
        }
        
        double change = Math.abs(newBalance - targetBalance);
        double ratio = targetBalance > 0 ? change / targetBalance : 1.0;

        // Use trailing digits if change is tiny relative to balance AND balance >= 10K
        useTrailingDigits = (ratio < TRAILING_THRESHOLD) && (targetBalance >= 10_000);

        targetBalance = newBalance;

        // Just move the target; a single slow ticker converges toward it. Rapid successive
        // changes only shift the target, they don't spawn a new animation burst.
        if (!animating) {
            animating = true;
            scheduleTick(0);
        }
    }
    
    /**
     * Refresh HUD display with current config (used when symbol/formatting changes)
     */
    public void refresh() {
        updateDisplayFinal(displayedBalance);
    }
    
    /**
     * Cleanup resources when HUD is removed (cancel pending animations)
     */
    public void cleanup() {
        animating = false;
        if (animationFuture != null) {
            HudScheduler.cancel(animationFuture);
            animationFuture = null;
        }
    }

    private void scheduleTick(long delayMs) {
        animationFuture = HudScheduler.runLater(this::tick, delayMs);
    }

    /**
     * One repaint step. Moves {@link #displayedBalance} a fraction of the remaining gap
     * toward {@link #targetBalance} and reschedules itself until it has converged. Each
     * call is at most one packet, spaced {@link #TICK_INTERVAL_MS} apart.
     */
    private void tick() {
        double remaining = targetBalance - displayedBalance;

        // Close enough: snap, paint once, stop the ticker.
        if (Math.abs(remaining) < 1.0) {
            displayedBalance = targetBalance;
            useTrailingDigits = false;
            animating = false;
            animationFuture = null;
            updateDisplayFinal(displayedBalance);
            return;
        }

        // Advance ~1/MAX_TICKS of the gap per tick so a change lands in a bounded number
        // of steps regardless of magnitude.
        displayedBalance += remaining / MAX_TICKS;

        if (useTrailingDigits) {
            updateDisplayTrailing(displayedBalance);
        } else {
            updateDisplayFinal(displayedBalance);
        }

        scheduleTick(TICK_INTERVAL_MS);
    }
    
    /**
     * Show trailing digits during animation for large balances.
     * Example: 1,100,000,005 shows as "...005"
     */
    private void updateDisplayTrailing(double balance) {
        if (!fr.varyon.ecotale.VaryonEcotalePlugin.getInstance().getEconomyConfig().isEnableHudDisplay()) {
            notifyDisabledOnce();
            return;
        }
        String symbol = fr.varyon.ecotale.VaryonEcotalePlugin.getInstance().getEconomyConfig().getCurrencySymbol();
        long rounded = Math.round(balance);
        // Show last 5 digits
        long lastDigits = rounded % 100_000;
        String amount = "..." + lastDigits;
        
        // Use per-player translation if available, otherwise config value
        String hudPrefix = fr.varyon.ecotale.economy.util.TranslationHelper.t(ownerRef, "hud.prefix", 
            fr.varyon.ecotale.VaryonEcotalePlugin.getInstance().getEconomyConfig().getHudPrefix());
        
        this.setText("CurrencyName", hudPrefix);
        this.setText("BalanceSymbol", symbol);
        this.setText("BalanceAmount", amount);
        this.pushUpdates();
    }
    
    /**
     * Show final abbreviated format (K/M/B).
     */
    private void updateDisplayFinal(double balance) {
        if (!fr.varyon.ecotale.VaryonEcotalePlugin.getInstance().getEconomyConfig().isEnableHudDisplay()) {
            notifyDisabledOnce();
            return;
        }
        String formatted = fr.varyon.ecotale.VaryonEcotalePlugin.getInstance().getEconomyConfig().formatShort(balance);
        String symbol = fr.varyon.ecotale.VaryonEcotalePlugin.getInstance().getEconomyConfig().getCurrencySymbol();
        // Use per-player translation if available, otherwise config value
        String hudPrefix = fr.varyon.ecotale.economy.util.TranslationHelper.t(ownerRef, "hud.prefix", 
            fr.varyon.ecotale.VaryonEcotalePlugin.getInstance().getEconomyConfig().getHudPrefix());
        String amount = formatted.startsWith(symbol) 
            ? formatted.substring(symbol.length()) 
            : formatted;

        this.setText("CurrencyName", hudPrefix);
        this.setText("BalanceSymbol", symbol);
        this.setText("BalanceAmount", amount);
        this.pushUpdates();
    }

    private void notifyDisabledOnce() {
        if (warnedDisabled) {
            return;
        }
        warnedDisabled = true;
        ownerRef.sendMessage(Message.raw("Balance HUD desactivado por error de UI."));
    }
}

