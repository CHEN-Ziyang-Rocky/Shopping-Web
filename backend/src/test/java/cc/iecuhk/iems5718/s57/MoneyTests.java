package cc.iecuhk.iems5718.s57;

import cc.iecuhk.iems5718.s57.service.Money;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class MoneyTests {
    @Test void decimalPricesDoNotLoseCents() {
        assertThat(Money.cents(19.99)).isEqualTo(1999);
        assertThat(Money.cents(0.29)).isEqualTo(29);
        assertThat(Money.amount(Money.cents(0.10) + Money.cents(0.20))).isEqualByComparingTo("0.30");
    }
    @Test void fractionalCentsAndNonFinitePricesAreRejected() {
        assertThatThrownBy(() -> Money.cents(1.001)).isInstanceOf(ArithmeticException.class);
        assertThatThrownBy(() -> Money.cents(Double.NaN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.cents(Double.POSITIVE_INFINITY)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.cents(-1.0)).isInstanceOf(IllegalArgumentException.class);
    }
}
