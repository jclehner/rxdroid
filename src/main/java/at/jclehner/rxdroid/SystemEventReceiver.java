/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2013 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RxDroid is distribute in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RxDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package at.jclehner.rxdroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import at.jclehner.androidutils.EventDispatcher;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.util.DateTime;

public class SystemEventReceiver extends BroadcastReceiver
{
	private static final String TAG = SystemEventReceiver.class.getSimpleName();

	public interface OnSystemTimeChangeListener
	{
		public static final int TIMEZONE_CHANGED = 0;
		public static final int TIME_CHANGED = 1;
		public static final int DATE_CHANGED = 2;

		public void onTimeChanged(int type);
	}

	private static final EventDispatcher<Object> sListeners =
			new EventDispatcher<Object>();

	public static void registerOnSystemTimeChangeListener(Object l) {
		sListeners.register(l);
	}

	public static void unregisterOnSystemTimeChangeListener(Object l) {
		sListeners.unregister(l);
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if(Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())
				|| Intent.ACTION_TIME_CHANGED.equals(intent.getAction())
				|| Intent.ACTION_DATE_CHANGED.equals(intent.getAction()))
		{
			DateTime.clearDateCache();
			Database.reload(context);

			sListeners.post("onTimeChanged", new Class<?>[] { int.class }, actionToListenerType(intent.getAction()));
		}
		else if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))
		{
			Settings.init();
			Settings.putLong(Settings.Keys.BOOT_COMPLETED_TIMESTAMP, RxDroid.getBootTimestamp());
		}

		Log.i(TAG, "Action: " + intent.getAction());

		NotificationReceiver.rescheduleAlarmsAndUpdateNotification(context, false);
	}

	private static int actionToListenerType(String action)
	{
		if(Intent.ACTION_TIMEZONE_CHANGED.equals(action))
			return OnSystemTimeChangeListener.TIMEZONE_CHANGED;

		if(Intent.ACTION_TIME_CHANGED.equals(action))
			return OnSystemTimeChangeListener.TIME_CHANGED;

		if(Intent.ACTION_DATE_CHANGED.equals(action))
			return OnSystemTimeChangeListener.DATE_CHANGED;

		return -1;
	}
}
