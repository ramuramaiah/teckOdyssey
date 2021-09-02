package guice.mockito;

import com.google.inject.AbstractModule;

public class BillingModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(TransactionLog.class).to(JavaLoggerTransactionLog.class);
		bind(CreditCardProcessor.class).to(PaypalCreditCardProcessor.class);
		bind(BillingService.class).to(RealBillingService.class);
	}
}
