package at.caspase.rxdroid.util;

import java.util.Collection;

/**
 * Utility functions for dealing with java <code>Collections</code>.
 * 
 * @author Joseph Lehner
 */
public final class CollectionUtils
{
	/**
	 * Interface for filtering collections based on specific criteria.
	 * 
	 * @author Joseph Lehner
	 */
	public interface Filter<E>
	{
		/**
		 * Returns <code>true</code> if the given entry has passed the filter.
		 * <p>
		 * Returning <code>false</code> here leads to this entry not being 
		 * included in the <code>Collection</code> returned by filter().
		 */
		boolean matches(E e);
	}
	
	public static<E> Collection<E> filter(final Collection<E> collection, Filter<E> filter)
	{
		final Collection<E> filtered = create(collection);
			
		for(E e : collection)
		{
			if(filter.matches(e))
				filtered.add(e);
		}
		
		return filtered;		
	}
	
	public static<E> Collection<E> clone(final Collection<E> collection)
	{
		final Collection<E> cloned = create(collection);
		cloned.addAll(collection);
		return cloned;
	}
	
	@SuppressWarnings("unchecked")
	public static<E> Collection<E> create(final Collection<E> collection)
	{
		if(collection == null)
			throw new IllegalArgumentException("Supplied argument was null");
		
		try
		{
			return (Collection<E>) collection.getClass().newInstance();
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException(collection.getClass().getName() + " lacks a visible default constructor", e);
		}
	}
	
	private CollectionUtils() {}
}
