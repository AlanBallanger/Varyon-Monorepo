package fr.varyon.ecotale.coins;

import com.hypixel.hytale.server.core.universe.PlayerRef;

public final class BankPermissionHelper {

    public static final String PERMISSION_BANK = "varyon.varyon-ecotale.command.bank";
    public static final String PERMISSION_DEPOSIT = "varyon.varyon-ecotale.command.bank.deposit";
    public static final String PERMISSION_DEPOSIT_RIGHTCLICK = "varyon.varyon-ecotale.bank.deposit-rightclick";
    public static final String PERMISSION_WITHDRAW = "varyon.varyon-ecotale.command.bank.withdraw";

    private BankPermissionHelper() {}

    public static boolean canUseBankUi(PlayerRef playerRef) {
        return playerRef.hasPermission(PERMISSION_BANK);
    }

    public static boolean canDeposit(PlayerRef playerRef) {
        return playerRef.hasPermission(PERMISSION_BANK)
            || playerRef.hasPermission(PERMISSION_DEPOSIT);
    }

    public static boolean canDepositRightClick(PlayerRef playerRef) {
        return playerRef.hasPermission(PERMISSION_DEPOSIT_RIGHTCLICK);
    }

    public static boolean canWithdraw(PlayerRef playerRef) {
        return playerRef.hasPermission(PERMISSION_BANK)
            || playerRef.hasPermission(PERMISSION_WITHDRAW);
    }
}
