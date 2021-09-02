package guice.mockito;

import java.math.BigDecimal;

public class PizzaOrder {
	BigDecimal amount;
	
	public PizzaOrder(BigDecimal amount) {
		this.amount = amount;
	}
	
	public BigDecimal getAmount() {
		return amount;
	}
}
