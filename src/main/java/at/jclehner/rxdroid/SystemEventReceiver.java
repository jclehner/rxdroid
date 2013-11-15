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

import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.util.DateTime;

public class SystemEventReceiver extends BroadcastReceiver
{
	private static final String TAG = SystemEventReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if(Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())
				|| Intent.ACTION_TIME_CHANGED.equals(intent.getAction())
				|| Intent.ACTION_DATE_CHANGED.equals(intent.getAction()))
		{
			DateTime.clearDateCache();
			Database.reload(context);
		}
		else if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))
		{
			Settings.init();
			Settings.putLong(Settings.Keys.BOOT_COMPLETED_TIMESTAMP, RxDroid.getBootTimestamp());
		}

		Log.i(TAG, "Action: " + intent.getAction());

		NotificationReceiver.rescheduleAlarmsAndUpdateNotification(context, false);
	}
}
