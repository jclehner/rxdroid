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

package at.caspase.rxdroid;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Database.OnChangedListener;
import at.caspase.rxdroid.db.Entry;
import at.caspase.rxdroid.db.Intake;
import at.caspase.rxdroid.util.Constants;

public class NotificationService extends Service implements OnChangedListener, OnSharedPreferenceChangeListener
{
	private static final String TAG = NotificationService.class.getName();

	private SharedPreferences mSharedPrefs;

	private static boolean sIsStarted = false;

	@Override
	public void onEntryCreated(Entry entry, int flags)
	{
		if(entry instanceof Intake)
		{
			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {

				@Override
				public void run()
				{
					NotificationReceiver.sendBroadcast(getApplicationContext(), true);
				}
			}, Constants.NOTIFICATION_INITIAL_DELAY);

		}
		else
			NotificationReceiver.sendBroadcast(this, false);
	}

	@Override
	public void onEntryUpdated(Entry entry, int flags)
	{
		NotificationReceiver.sendBroadcast(this, false);
	}

	@Override
	public void onEntryDeleted(Entry entry, int flags)
	{
		NotificationReceiver.sendBroadcast(this, false);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		// TODO filter
		NotificationReceiver.sendBroadcast(this, true);
	}

	@Override
	public void onCreate()
	{
		Log.d(TAG, "onCreate");
		super.onCreate();
		Database.registerOnChangedListener(this);
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mSharedPrefs.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onDestroy()
	{
		Log.d(TAG, "onDestroy");
		super.onDestroy();
		Database.unregisterOnChangedListener(this);
		mSharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
		Settings.instance().setLastNotificationMessageHash(0);
		sIsStarted = false;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if(!sIsStarted)
		{
			NotificationReceiver.sendBroadcast(this, true);
			sIsStarted = true;
		}
		return START_STICKY;
	}
}
