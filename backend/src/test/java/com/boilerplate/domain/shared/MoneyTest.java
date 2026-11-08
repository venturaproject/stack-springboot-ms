package com.boilerplate.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boilerplate.domain.exception.CurrencyMismatchException;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void adds_and_multiplies_within_a_currency() {
        Money five = Money.fromMinorUnits(500, Currency.USD);
        assertThat(five.add(Money.fromMinorUnits(250, Currency.USD)).amountMinorUnits())
                .isEqualTo(750);
        assertThat(five.multiply(3).amountMinorUnits()).isEqualTo(1500);
    }

    @Test
    void rejects_a_negative_amount() {
        assertThatThrownBy(() -> Money.fromMinorUnits(-1, Currency.USD)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_mixed_currency_arithmetic() {
        assertThatThrownBy(() -> Money.fromMinorUnits(100, Currency.USD).add(Money.fromMinorUnits(100, Currency.EUR)))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void rejects_a_negative_multiplier() {
        assertThatThrownBy(() -> Money.fromMinorUnits(100, Currency.USD).multiply(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
