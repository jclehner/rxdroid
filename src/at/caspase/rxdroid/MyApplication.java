package at.caspase.rxdroid;

import android.app.Application;
import android.os.StrictMode;

public class MyApplication extends Application
{
	public static final boolean DEVELOPER_MODE = true;
	
	@Override
	public void onCreate()
	{
		if(DEVELOPER_MODE) 
		{
	         StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
	                 .detectLeakedSqlLiteObjects()
	                 /*.detectLeakedClosableObjects()*/
	                 .penaltyLog()
	                 .build());
	     }
	     super.onCreate();	
		
	}
}
