package at.caspase.rxdroid.test;

import android.test.AndroidTestCase;
import at.caspase.rxdroid.util.Hasher;

public class HasherTest extends AndroidTestCase
{
	public void testHasher()
	{	
		Alice alice = new Alice(3, 0, "foobar");
		Bob bob = new Bob(0, '\u0003', "foobar");
		
		if(alice.hashCode1() == alice.hashCode2())
			fail("hashCode1 and hashCode2 return the same hash");
		
		if(alice.hashCode1() == bob.hashCode1())
			fail("hashCode1 and hashCode1 return the same hash for Alice and Bob");
		
		alice.a = 0;
		alice.b = 0;
		alice.c = null;
		
		if(alice.hashCode1() == 0)
			fail("Alice.hashCode1() returned 0");
	}
	
	private static class Alice
	{
		Alice(int a, int b, String c)
		{
			this.a = a;
			this.b = b;
			this.c = c;
		}
		
		public int hashCode1()
		{
			Hasher hasher = new Hasher();
			hasher.hash(a);
			hasher.hash(b);
			hasher.hash(c);
			
			return hasher.getHashCode();
		}
		
		public int hashCode2()
		{
			Hasher hasher = new Hasher();
			hasher.hash(b);
			hasher.hash(a);
			hasher.hash(c);
			
			return hasher.getHashCode();
		}
		
		int    a;
		int    b;
		String c;		
	}
	
	private static class Bob
	{
		Bob(long a, char b, String c)
		{
			this.a = a;
			this.b = b;
			this.c = c;
		}
		
		public int hashCode1()
		{
			Hasher hasher = new Hasher();
			hasher.hash(a);
			hasher.hash(b);
			hasher.hash(c);
			
			return hasher.getHashCode();
		}
		
		String c;
		long   a;
		char   b;		
	}
}
