package com.boilerplate.domain.shared;

import com.boilerplate.domain.exception.CurrencyMismatchException;

/**
 * A monetary amount held as integer minor units (cents) so arithmetic never hits floating
 * point rounding. Immutable; every operation returns a new instance and rejects
 * mixed-currency operands. Used by {@code Product} (price), {@code Order} (total) and
 * {@code OrderLine} (unit price).
 */
public record Money(long amountMinorUnits, Currency currency) {

    public Money {
        if (currency == null) {
            throw new IllegalArgumentException("Money currency is required");
        }
        if (amountMinorUnits < 0) {
            throw new IllegalArgumentException("Money amount cannot be negative");
        }
    }

    public static Money fromMinorUnits(long amountMinorUnits, Currency currency) {
        return new Money(amountMinorUnits, currency);
    }

    public static Money zero(Currency currency) {
        return new Money(0, currency);
    }

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(amountMinorUnits + other.amountMinorUnits, currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(amountMinorUnits - other.amountMinorUnits, currency);
    }

    public Money multiply(int factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("Money multiplication factor cannot be negative");
        }
        return new Money(amountMinorUnits * factor, currency);
    }

    public boolean isGreaterThan(Money other) {
        assertSameCurrency(other);
        return amountMinorUnits > other.amountMinorUnits;
    }

    private void assertSameCurrency(Money other) {
        if (currency != other.currency) {
            throw new CurrencyMismatchException(currency.name(), other.currency.name());
        }
    }
}
