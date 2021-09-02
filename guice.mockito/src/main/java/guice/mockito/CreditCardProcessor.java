package guice.mockito;

import java.math.BigDecimal;

public interface CreditCardProcessor {
	public ChargeResult charge(CreditCard creditCard, BigDecimal amount) throws UnreachableException;
}
