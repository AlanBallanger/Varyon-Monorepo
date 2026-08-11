package fr.varyon.shop.economy;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Reflection bridge to VaultUnlocked's Economy service. No compile-time dependency on the
 * VaultUnlocked jar: mirrors the pattern used by BetterShopAuction's VaultUnlockedEconomyProvider.
 */
public final class VaultEconomyBridge {
    private static final String PLUGIN_NAME = "Varyon-Shop";

    private final Method economyObjMethod;
    private final Method servicesMethod;
    private final Method serviceManagerEconomyObjMethod;
    private final Method isEnabledMethod;
    private final Method balanceMethod;
    private final Method withdrawMethod;
    private final Method depositMethod;
    private final Method formatMethod;
    private final Method defaultCurrencyNameSingularMethod;
    private final Method transactionSuccessMethod;
    private final boolean methodsResolved;

    private VaultEconomyBridge(
            Method economyObjMethod,
            Method servicesMethod,
            Method serviceManagerEconomyObjMethod,
            Method isEnabledMethod,
            Method balanceMethod,
            Method withdrawMethod,
            Method depositMethod,
            Method formatMethod,
            Method defaultCurrencyNameSingularMethod,
            Method transactionSuccessMethod,
            boolean methodsResolved
    ) {
        this.economyObjMethod = economyObjMethod;
        this.servicesMethod = servicesMethod;
        this.serviceManagerEconomyObjMethod = serviceManagerEconomyObjMethod;
        this.isEnabledMethod = isEnabledMethod;
        this.balanceMethod = balanceMethod;
        this.withdrawMethod = withdrawMethod;
        this.depositMethod = depositMethod;
        this.formatMethod = formatMethod;
        this.defaultCurrencyNameSingularMethod = defaultCurrencyNameSingularMethod;
        this.transactionSuccessMethod = transactionSuccessMethod;
        this.methodsResolved = methodsResolved;
    }

    private static final VaultEconomyBridge UNAVAILABLE =
            new VaultEconomyBridge(null, null, null, null, null, null, null, null, null, null, false);

    /** Attempts to resolve VaultUnlocked's Economy API via reflection. Never returns null. */
    public static VaultEconomyBridge connect() {
        try {
            Class<?> vaultClass = Class.forName("net.cfh.vault.VaultUnlocked");
            Class<?> serviceManagerClass = findClass("net.cfh.vault.VaultUnlockedServicesManager");
            Class<?> economyClass = Class.forName("net.milkbowl.vault2.economy.Economy");
            Class<?> economyResponseClass = Class.forName("net.milkbowl.vault2.economy.EconomyResponse");

            Method economyObjMethod = vaultClass.getMethod("economyObj");
            Method servicesMethod = findMethod(vaultClass, "services");
            Method serviceManagerEconomyObjMethod = serviceManagerClass == null
                    ? null
                    : findMethod(serviceManagerClass, "economyObj");
            Method isEnabledMethod = economyClass.getMethod("isEnabled");
            Method balanceMethod = economyClass.getMethod("balance", String.class, UUID.class);
            Method withdrawMethod = economyClass.getMethod("withdraw", String.class, UUID.class, BigDecimal.class);
            Method depositMethod = economyClass.getMethod("deposit", String.class, UUID.class, BigDecimal.class);
            Method formatMethod = economyClass.getMethod("format", String.class, BigDecimal.class);
            Method currencyNameMethod = economyClass.getMethod("defaultCurrencyNameSingular", String.class);
            Method transactionSuccessMethod = economyResponseClass.getMethod("transactionSuccess");

            return new VaultEconomyBridge(
                    economyObjMethod, servicesMethod, serviceManagerEconomyObjMethod,
                    isEnabledMethod, balanceMethod, withdrawMethod, depositMethod,
                    formatMethod, currencyNameMethod, transactionSuccessMethod, true
            );
        } catch (Exception e) {
            return UNAVAILABLE;
        }
    }

    private static Class<?> findClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>... params) {
        try {
            return clazz.getMethod(name, params);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public boolean isAvailable() {
        Object economy = resolveEconomy();
        if (economy == null) {
            return false;
        }
        if (isEnabledMethod == null) {
            return true;
        }
        try {
            Object value = isEnabledMethod.invoke(economy);
            return !(value instanceof Boolean) || (Boolean) value;
        } catch (Exception ignored) {
            return false;
        }
    }

    public double getBalance(UUID playerUuid) {
        Object economy = resolveEconomy();
        if (economy == null || playerUuid == null || balanceMethod == null) {
            return 0.0;
        }
        try {
            Object value = balanceMethod.invoke(economy, PLUGIN_NAME, playerUuid);
            if (value instanceof BigDecimal decimal) {
                return decimal.doubleValue();
            }
            if (value instanceof Number number) {
                return number.doubleValue();
            }
        } catch (Exception ignored) {
        }
        return 0.0;
    }

    public boolean addFunds(UUID playerUuid, double amount) {
        Object economy = resolveEconomy();
        if (economy == null || playerUuid == null || amount < 0.0 || depositMethod == null) {
            return false;
        }
        if (amount == 0.0) {
            return true;
        }
        try {
            Object response = depositMethod.invoke(economy, PLUGIN_NAME, playerUuid, toAmount(amount));
            return isTransactionSuccess(response);
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean takeFunds(UUID playerUuid, double amount) {
        Object economy = resolveEconomy();
        if (economy == null || playerUuid == null || amount < 0.0 || withdrawMethod == null) {
            return false;
        }
        if (amount == 0.0) {
            return true;
        }
        try {
            Object response = withdrawMethod.invoke(economy, PLUGIN_NAME, playerUuid, toAmount(amount));
            return isTransactionSuccess(response);
        } catch (Exception ignored) {
            return false;
        }
    }

    public String unitName() {
        Object economy = resolveEconomy();
        if (economy != null && defaultCurrencyNameSingularMethod != null) {
            try {
                Object value = defaultCurrencyNameSingularMethod.invoke(economy, PLUGIN_NAME);
                if (value instanceof String text && !text.isBlank()) {
                    return text;
                }
            } catch (Exception ignored) {
            }
        }
        return "Coins";
    }

    public String format(double amount) {
        Object economy = resolveEconomy();
        if (economy != null && formatMethod != null) {
            try {
                Object value = formatMethod.invoke(economy, PLUGIN_NAME, toAmount(amount));
                if (value instanceof String text && !text.isBlank()) {
                    return text;
                }
            } catch (Exception ignored) {
            }
        }
        return String.format("%.0f", amount);
    }

    private Object resolveEconomy() {
        if (!methodsResolved || economyObjMethod == null) {
            return null;
        }
        try {
            Object economy = economyObjMethod.invoke(null);
            if (economy != null) {
                return economy;
            }
        } catch (Exception ignored) {
        }
        if (servicesMethod == null || serviceManagerEconomyObjMethod == null) {
            return null;
        }
        try {
            Object services = servicesMethod.invoke(null);
            if (services == null) {
                return null;
            }
            return serviceManagerEconomyObjMethod.invoke(services);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isTransactionSuccess(Object response) {
        if (response == null || transactionSuccessMethod == null) {
            return false;
        }
        try {
            Object value = transactionSuccessMethod.invoke(response);
            return value instanceof Boolean && (Boolean) value;
        } catch (Exception ignored) {
            return false;
        }
    }

    private BigDecimal toAmount(double amount) {
        return BigDecimal.valueOf(Math.max(0.0, amount));
    }
}
