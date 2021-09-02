package guice.mockito;

import java.math.BigDecimal;

public class PaypalCreditCardProcessor implements CreditCardProcessor {

	public ChargeResult charge(CreditCard creditCard, BigDecimal amount) throws UnreachableException {
		return new ChargeResult() {
			public boolean wasSuccessful() {
				return true;
			}
			
			public String getDeclineMessage() {
				return null;
			}
		};
	}
}
