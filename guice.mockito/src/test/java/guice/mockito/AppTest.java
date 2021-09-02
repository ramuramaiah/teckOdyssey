package guice.mockito;

import java.math.BigDecimal;

import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    @Test(expected = UnreachableException.class)
    public void testUnreachableCreditCardProcessor() throws UnreachableException
    {
    	CreditCardProcessor unreachableCCP = Mockito.mock(CreditCardProcessor.class);
    	CreditCard mockCreditCard = Mockito.mock(CreditCard.class);
    	Mockito.when(unreachableCCP.charge(mockCreditCard, new BigDecimal(100.0))).thenThrow(new UnreachableException());
    	unreachableCCP.charge(mockCreditCard, new BigDecimal(100.0));
    }
}
