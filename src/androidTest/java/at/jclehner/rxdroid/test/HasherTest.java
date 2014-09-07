/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2013 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. Additional terms apply (see LICENSE).
 *
 * RxDroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RxDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package at.jclehner.rxdroid.test;

import android.test.AndroidTestCase;
import at.jclehner.rxdroid.util.Hasher;

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

	public void testHasherReset()
	{
		Hasher hasher = Hasher.getInstance();
		hasher.hash("Steak!");

		int firstHash = hasher.getHashCode();

		hasher.reset().hash("Steak!");

		if(hasher.getHashCode() != firstHash)
			fail("First and second hashes differ!");
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
			Hasher hasher = Hasher.getInstance();
			hasher.hash(a);
			hasher.hash(b);
			hasher.hash(c);

			return hasher.getHashCode();
		}

		public int hashCode2()
		{
			Hasher hasher = Hasher.getInstance();
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
			Hasher hasher = Hasher.getInstance();
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
