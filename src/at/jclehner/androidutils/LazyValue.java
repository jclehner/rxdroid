package at.jclehner.androidutils;

public abstract class LazyValue<T>
{
	private T mValue;

	public T get()
	{
		if(mValue == null)
		{
			synchronized(this) {
				mValue = value();
			}
		}

		return mValue;
	}

	public void clear() {
		mValue = null;
	}

	public abstract T value();
}
