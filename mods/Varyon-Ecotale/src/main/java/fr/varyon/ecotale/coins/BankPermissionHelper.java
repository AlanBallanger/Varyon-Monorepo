package fr.varyon.ecotale.coins;

import com.hypixel.hytale.server.core.universe.PlayerRef;

public final class BankPermissionHelper {

    private static final String LEGACY_BANK = "ecotale.ecotalecoins.command.bank";
    private static final String VARYON_BANK = "varyon.varyon-ecotale.command.bank";
    private static final String VARYON_DEPOSIT = "varyon.varyon-ecotale.command.bank.deposit";
    private static final String VARYON_DEPOSIT_RIGHTCLICK = "varyon.varyon-ecotale.bank.deposit-rightclick";
    private static final String VARYON_WITHDRAW = "varyon.varyon-ecotale.command.bank.withdraw";

    private BankPermissionHelper() {}

    public static boolean canUseBankUi(PlayerRef playerRef) {
        return playerRef.hasPermission(LEGACY_BANK) || playerRef.hasPermission(VARYON_BANK);
    }

    public static boolean canDeposit(PlayerRef playerRef) {
        return playerRef.hasPermission(LEGACY_BANK)
            || playerRef.hasPermission(VARYON_BANK)
            || playerRef.hasPermission(VARYON_DEPOSIT);
    }

    public static boolean canDepositRightClick(PlayerRef playerRef) {
        return playerRef.hasPermission(VARYON_DEPOSIT_RIGHTCLICK);
    }

    public static boolean canWithdraw(PlayerRef playerRef) {
        return playerRef.hasPermission(LEGACY_BANK)
            || playerRef.hasPermission(VARYON_BANK)
            || playerRef.hasPermission(VARYON_WITHDRAW);
    }
}
