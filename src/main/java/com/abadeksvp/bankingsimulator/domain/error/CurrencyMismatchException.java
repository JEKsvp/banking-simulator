package com.abadeksvp.bankingsimulator.domain.error;

import com.abadeksvp.bankingsimulator.domain.model.Currency;
import lombok.Getter;

@Getter
public class CurrencyMismatchException extends RuntimeException {

    private final Currency sourceCurrency;
    private final Currency targetCurrency;

    public CurrencyMismatchException(Currency accountCurrency, Currency transactionCurrency) {
        super("Account currency %s does not match transaction currency %s"
                .formatted(accountCurrency, transactionCurrency));
        this.sourceCurrency = accountCurrency;
        this.targetCurrency = transactionCurrency;
    }

    private CurrencyMismatchException(String message, Currency sourceCurrency, Currency targetCurrency) {
        super(message);
        this.sourceCurrency = sourceCurrency;
        this.targetCurrency = targetCurrency;
    }

    public static CurrencyMismatchException betweenAccounts(Currency sourceCurrency, Currency destinationCurrency) {
        return new CurrencyMismatchException(
                "Source account currency %s does not match destination account currency %s"
                        .formatted(sourceCurrency, destinationCurrency),
                sourceCurrency, destinationCurrency);
    }
}
