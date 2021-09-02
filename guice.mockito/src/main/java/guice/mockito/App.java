package guice.mockito;

import java.math.BigDecimal;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class App 
{
    public static void main( String[] args )
    {
    	Injector injector = Guice.createInjector(new BillingModule());
        BillingService billingService = injector.getInstance(BillingService.class);
        PizzaOrder order = new PizzaOrder(new BigDecimal(100.00));
        CreditCard creditCard = new CreditCard("123456", 12, 2030);
        billingService.chargeOrder(order, creditCard);
    }
}
