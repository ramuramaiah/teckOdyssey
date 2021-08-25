package netty.recycler;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.netty.util.internal.ObjectPool;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class UserTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public UserTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( UserTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testReferenceEquality()
    {
    	ObjectPool<User> recycler = User.getRecycler();
        User user1 = recycler.get();
        user1.recycle();
        User user2 = recycler.get();
        assertSame(user1, user2);
        user2.recycle();
    }
    
    public void testObjectEquality() throws JsonProcessingException
    {
    	ObjectPool<User> recycler = User.getRecycler();
    	User user1 = recycler.get();
    	
    	user1.setFirstName("Bill");
    	user1.setLastName("Clinton");
    	user1.setEmail("bclinton@usa.com");
    	
    	String user1AsStr = user1.toJson();
    	user1.recycle();
    	
    	User user2 = User.fromJson(user1AsStr);
        assertEquals(true, user2.equals(user1));
        user2.recycle();
    }
}
