package at.caspase.rxdroid;

import android.content.Context;

public final class ContextStorage
{
	static Context sContext;
	
	static public void set(Context context)
	{
		if(sContext == null)
			sContext = context;
	}
	
	static public Context get() {
		return sContext;
	}
	
	private ContextStorage() {}
}
