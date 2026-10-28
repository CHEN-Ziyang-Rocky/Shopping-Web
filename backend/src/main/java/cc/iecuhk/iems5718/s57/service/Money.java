package cc.iecuhk.iems5718.s57.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** HKD amounts cross the payment boundary as integer cents, never binary floating point. */
public final class Money {
    private Money() {}

    public static long cents(Number amount) {
        if (amount == null || !Double.isFinite(amount.doubleValue()) || amount.doubleValue() < 0) {
            throw new IllegalArgumentException("Invalid monetary amount");
        }
        return new BigDecimal(amount.toString()).movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY).longValueExact();
    }

    // JSON remains numeric; persistence uses exact DECIMAL values.
    public static BigDecimal amount(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }
}
