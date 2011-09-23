package at.caspase.rxdroid;

import java.util.StringTokenizer;

import android.content.Context;

public final class Version
{
	@SuppressWarnings("unused")
	private static final String TAG = Version.class.getName();
	
	public static final int FORMAT_SHORT = 0;
	public static final int FORMAT_LONG = 1;
	public static final int FORMAT_FULL = 2;
	
	private static String sVersion;
	private static String sRevision;
	private static String sAppName;
	
	public static String get() {
		return get(FORMAT_LONG);
	}
	
	public static String get(int format)
	{
		init();
		
		switch(format)
		{
			case FORMAT_SHORT:
				return sVersion;
				
			case FORMAT_LONG:
				return get(FORMAT_SHORT) + "_r" + sRevision;
				
			case FORMAT_FULL:
				return sAppName + " " + get(FORMAT_LONG);
			
			default:
				throw new IllegalArgumentException();
		}			
	}
	
	private static synchronized void init()
	{
		if(sVersion == null)
		{
			final Context c = ContextStorage.get();
			sVersion = c.getString(R.string.version);
			sRevision = c.getString(R.string.vcs_revision);
			sAppName = c.getString(R.string.app_name);
			
			final StringTokenizer st = new StringTokenizer(sRevision);
			if(st.countTokens() != 3)
				return;
			
			st.nextToken(); // discard
			sRevision = st.nextToken();			
		}		
	}
	
	private Version() {}
}
