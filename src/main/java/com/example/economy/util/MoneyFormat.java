package com.example.economy.util;

import java.text.DecimalFormat;

public final class MoneyFormat {
    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.00");

    private MoneyFormat() {
    }

    public static String pretty(String symbol, double amount) {
        return symbol + FORMAT.format(amount);
    }
}
