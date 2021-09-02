package guice.mockito;

import java.math.BigDecimal;

public class Receipt {
	String receiptContent;
	
	private Receipt(String receiptContent) {
		this.receiptContent = receiptContent;
	}
	
	public static Receipt forSuccessfulCharge(BigDecimal amount) {
		Receipt receipt = new Receipt("Your credit card is charged for amount: " + amount);
		return receipt;
	}
	
	public static Receipt forDeclinedCharge(String message) {
		Receipt receipt = new Receipt("Insufficient balance in your credit card.");
		return receipt;
	}
	
	public static Receipt forSystemFailure(String message) {
		Receipt receipt = new Receipt("An error occured while contacting your credit card service provider.");
		return receipt;
	}
}
