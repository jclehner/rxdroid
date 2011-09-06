package at.caspase.rxdroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyBroadcastReceiver extends BroadcastReceiver
{
	private static final String TAG = MyBroadcastReceiver.class.getName();

	@Override
	public void onReceive(Context context, Intent intent)
	{
		Log.d(TAG, "Received intent with action " + intent.getAction());

		Intent service = new Intent();
		service.setClass(context.getApplicationContext(), NotificationService.class);
		service.putExtra(NotificationService.EXTRA_FORCE_RESTART, true);
		context.startService(service);
	}
}
