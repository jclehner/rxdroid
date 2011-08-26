/**
 * Copyright (C) 2011 Joseph Lehner <joseph.c.lehner@gmail.com>
 * 
 * This file is part of RxDroid.
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

package at.caspase.rxdroid.util;

import java.lang.reflect.Array;

/**
 * Helper class for writing hashCode methods.
 * 
 * The design is essentially copied from the Android SDK's javadoc, which in
 * turn is based on <em>Effective Java</em> item 8.
 * 
 * Usage example:
 * <pre>
 * 
 * // ...
 * &#64;Override 	
 * public int hashCode()
 * {
 * 	int result = Hasher.SEED;
 * 	
 * 	result = Hasher.hash(result, mMember1);
 * 	result = Hasher.hash(result, mMember2);
 * 	// ...
 * 	result = Hasher.hash(result, mMemberX);
 *  
 * 	return result;
 * }
 * // ...
 * </pre>
 * 
 * @author Joseph Lehner
 *
 */
public class Hasher
{
	/**
	 * Initial seed for the hash.
	 */
	public static final int SEED = 23;
	
	public static int hash(int seed, boolean b) {
		return mult(seed) + (b ? 1 : 0);
	}
	
	public static int hash(int seed, char c) {
		return hash(seed, (int) c);
	}
	
	public static int hash(int seed, int i) {
		return mult(seed) + i;
	}
	
	public static int hash(int seed, long l) {
		return mult(seed) + (int) (l ^ (l >>> 32));
	}
	
	public static int hash(int seed, float f) {
		return hash(seed, Float.floatToIntBits(f));
	}
	
	public static int hash(int seed, double d) {
		return hash(seed, Double.doubleToLongBits(d));
	}
	
	public static int hash(int seed, Object o)
	{
		int result = seed;
		
		if(o == null)
			result = hash(result, 0);
		else if(!o.getClass().isArray())
			result = hash(result, o.hashCode());
		else
		{
			final int length = Array.getLength(o);
			for(int i = 0; i != length; ++i)
				result = hash(result, Array.get(o, i));
		}    		
		
		return result;    		
	}
	
	
	private static int mult(int seed) {
		return PRIME * seed;
	}
	    	
	private static final int PRIME = 37;    	
}