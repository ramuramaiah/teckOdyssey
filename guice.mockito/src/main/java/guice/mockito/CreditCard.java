package guice.mockito;

public class CreditCard {
	String ccNumber;
	int monthExpiry;
	int yearExpiry;
	
	public CreditCard(String ccNumber, int monthExpiry, int yearExpiry) {
		this.ccNumber = ccNumber;
		this.monthExpiry = monthExpiry;
		this.yearExpiry = yearExpiry;
	}
}
