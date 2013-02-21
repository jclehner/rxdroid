package at.jclehner.androidutils;

public abstract class LazyValue<T>
{
	public interface Mutator<T>
	{
		void mutate(T value);
	}

	private volatile T mValue;

	public T get()
	{
		if(mValue == null)
		{
			synchronized(this)
			{
				if(mValue == null)
					mValue = value();
			}
		}

		return mValue;
	}

	public void set(T value)
	{
		synchronized(this) {
			mValue = value;
		}
	}

	public synchronized void reset() {
		mValue = null;
	}

	public synchronized void mutate(Mutator<T> mutator)
	{
		if(mValue == null)
			return;

		mutator.mutate(mValue);
	}

	public abstract T value();
}
