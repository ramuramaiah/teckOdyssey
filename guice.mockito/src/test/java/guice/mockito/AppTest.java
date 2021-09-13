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
    	Mockito.when(unreachableCCP.charge(Mockito.any(CreditCard.class), Mockito.any(BigDecimal.class))).thenThrow(new UnreachableException());
    	unreachableCCP.charge(Mockito.any(CreditCard.class), Mockito.any(BigDecimal.class));
    }
}
