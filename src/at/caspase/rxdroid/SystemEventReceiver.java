/**
 * Copyright (C) 2012 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 * This file is part of RxDroid.
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

package at.caspase.rxdroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SystemEventReceiver extends BroadcastReceiver
{
	private static final String TAG = SystemEventReceiver.class.getName();

	@Override
	public void onReceive(Context context, Intent intent)
	{
		Log.d(TAG, "Received intent with action " + intent.getAction());

		Intent service = new Intent();
		service.setClass(context.getApplicationContext(), NotificationService.class);
		//service.putExtra(NotificationService.EXTRA_RESTART_FLAGS, NotificationService.RESTART_FORCE);
		context.startService(service);
	}
}
