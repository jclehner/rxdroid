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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;

public class MyNotification
{
	private static final String TAG = MyNotification.class.getName();
	private static final boolean LOGV = false;

	private Context mContext;

	private String mTickerText;
	private String mTitle;
	private String mMessage1;
	private String mMessage2;

	private int mDefaults = Notification.DEFAULT_LIGHTS;

	private int mIconResId = R.drawable.ic_stat_pill;

	private boolean mDoAutoCancel = false;
	private boolean mForceUpdate = false;
	private boolean mShowTime = false;
	private boolean mIsPersistent = false;

	private PendingIntent mContentIntent;
	private PendingIntent mDeleteIntent;

	public static class Builder
	{
		private MyNotification mNotification;
		private Context mContext;

		public Builder(Context context)
		{
			mNotification = new MyNotification(context);
			mNotification.mTickerText = context.getString(R.string._msg_new_notification);
			mNotification.mTitle = context.getString(R.string.app_name);
			mContext = context;
		}

		public Builder setTickerText(String tickerText)
		{
			mNotification.mTickerText = tickerText;
			return this;
		}

		public Builder setTickerText(int resId, Object... formatArgs) {
			return setTickerText(mContext.getString(resId, formatArgs));
		}

		public Builder setTitle(String title)
		{
			mNotification.mTitle = title;
			return this;
		}

		public Builder setTitle(int resId, Object... formatArgs) {
			return setTitle(mContext.getString(resId, formatArgs));
		}

		public Builder setMessage1(String message1)
		{
			mNotification.mMessage1 = message1;
			return this;
		}

		public Builder setMessage1(int resId, Object... formatArgs) {
			return setMessage1(mContext.getString(resId, formatArgs));
		}

		public Builder setMessage2(String message2)
		{
			mNotification.mMessage2 = message2;
			return this;
		}

		public Builder setMessage2(int resId, Object... formatArgs) {
			return setMessage2(mContext.getString(resId, formatArgs));
		}

		public Builder setIcon(int iconResId)
		{
			mNotification.mIconResId = iconResId;
			return this;
		}

		public Builder setDefaults(int defaults)
		{
			mNotification.mDefaults = defaults;
			return this;
		}

		public Builder setAutoCancel(boolean doAutoCancel)
		{
			mNotification.mDoAutoCancel = doAutoCancel;
			return this;
		}

		public Builder setForceUpdate(boolean force)
		{
			mNotification.mForceUpdate = force;
			return this;
		}

		public Builder setShowTime(boolean showTime) {
			mNotification.mShowTime = showTime;
			return this;
		}

		public Builder setPersistent(boolean persistent)
		{
			mNotification.mIsPersistent = persistent;
			return this;
		}

		public Builder setContentIntent(PendingIntent contentIntent)
		{
			mNotification.mContentIntent = contentIntent;
			return this;
		}

		public Builder setDeleteIntent(PendingIntent deleteIntent)
		{
			mNotification.mDeleteIntent = deleteIntent;
			return this;
		}

		public MyNotification build() {
			return mNotification;
		}

		public void post() {
			mNotification.post();
		}

		public void cancel()
		{
			NotificationManager notificationMgr = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
			notificationMgr.cancel(R.id.notification);
		}
	}

	public void post()
	{
		final Notification notification = new Notification();

		if(mDeleteIntent != null)
		{
			notification.deleteIntent = mDeleteIntent;
			if(mDoAutoCancel)
				notification.flags |= Notification.FLAG_AUTO_CANCEL;
		}

		if(mDefaults != 0)
			notification.defaults |= mDefaults;
		else
			notification.defaults |= Notification.DEFAULT_ALL;

		if(mShowTime)
			notification.when = System.currentTimeMillis();

		//notification.flags |= Notification.FLAG_SHOW_LIGHTS;

		if(mIsPersistent)
			notification.flags |= Notification.FLAG_ONGOING_EVENT;

		if(!mForceUpdate)
			notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;

		notification.tickerText = mTickerText;
		notification.icon = mIconResId;

		final String message =  mMessage1 != null ? mMessage1 : mMessage2;
		notification.setLatestEventInfo(mContext, mTitle, message, mContentIntent);

		int lastHash = Settings.instance().getLastNotificationMessageHash();
		int thisHash = message.hashCode();

		if(lastHash != thisHash)
		{
			notification.flags ^= Notification.FLAG_ONLY_ALERT_ONCE;
			Settings.instance().setLastNotificationMessageHash(thisHash);
		}

		NotificationManager notificationMgr = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationMgr.notify(R.id.notification, notification);
	}

	public String getMessage1() {
		return mMessage1;
	}

	public String getMessage2() {
		return mMessage2;
	}

	private MyNotification(Context context) {
		mContext = context;
	}
}
