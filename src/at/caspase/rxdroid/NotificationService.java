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
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Database.OnDatabaseChangedListener;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Entry;

public class NotificationService extends Service implements OnDatabaseChangedListener, OnSharedPreferenceChangeListener
{
	@SuppressWarnings("unused")
	private static final String TAG = NotificationService.class.getName();
		
	private SharedPreferences mSharedPrefs;
	
	@Override
	public void onEntryCreated(Entry entry, int flags)
	{
		final boolean beQuiet = entry instanceof Drug ? true : false;
		NotificationReceiver.sendInitialBroadcast(this, beQuiet);
	}

	@Override
	public void onEntryUpdated(Entry entry, int flags)
	{
		NotificationReceiver.sendInitialBroadcast(this, false);
	}

	@Override
	public void onEntryDeleted(Entry entry, int flags)
	{
		NotificationReceiver.sendInitialBroadcast(this, false);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		// TODO filter
		NotificationReceiver.sendInitialBroadcast(this, true);
	}
	
	public NotificationService()
	{
		Log.d(TAG, "<init>");
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
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		NotificationReceiver.sendInitialBroadcast(this, true);			
		return START_STICKY;
	}
}
