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
import android.net.Uri;
import android.util.Log;

public class MyNotification
{
	//private static final String TAG = MyNotification.class.getName();
	//private static final boolean LOGV = false;

	private final Context mContext;

	private String mTickerText;
	private String mTitle1;
	private String mTitle2;
	private String mMessage1;
	private String mMessage2;
	private int mIcon1 = R.drawable.ic_stat_normal;
	private int mIcon2;

	private boolean mDoAutoCancel = false;
	private boolean mForceUpdate = false;
	private boolean mShowTime = false;
	private boolean mIsPersistent = false;

	private int mFlags = 0;
	private boolean mIsSilent = false;

	private PendingIntent mContentIntent;
	private PendingIntent mDeleteIntent;

	public static class Builder
	{
		private final MyNotification mNotification;
		private final Context mContext;

		public Builder(Context context)
		{
			mNotification = new MyNotification(context);
			mNotification.mTickerText = context.getString(R.string._msg_new_notification);
			mNotification.mTitle1 = context.getString(R.string.app_name);
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

		public Builder setTitle1(String title)
		{
			mNotification.mTitle1 = title;
			return this;
		}

		public Builder setTitle1(int resId, Object... formatArgs) {
			return setTitle1(mContext.getString(resId, formatArgs));
		}

		public Builder setTitle2(String title)
		{
			mNotification.mTitle2 = title;
			return this;
		}

		public Builder setTitle2(int resId, Object... formatArgs) {
			return setTitle2(mContext.getString(resId, formatArgs));
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

		public Builder setIcon1(int iconResId)
		{
			mNotification.mIcon1 = iconResId;
			return this;
		}

		public Builder setIcon2(int iconResId)
		{
			mNotification.mIcon2 = iconResId;
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

		public Builder addFlags(int flags)
		{
			mNotification.mFlags |= flags;
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

		public Builder setSilent(boolean silent)
		{
			mNotification.mIsSilent = silent;
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

	@SuppressWarnings("deprecation")
	public void post()
	{
		final Notification notification = new Notification();

		if(mDeleteIntent != null)
		{
			notification.deleteIntent = mDeleteIntent;
			if(mDoAutoCancel)
				notification.flags |= Notification.FLAG_AUTO_CANCEL;
		}

		if(mShowTime)
			notification.when = System.currentTimeMillis();
		else
			notification.when = 0;

		//notification.flags |= Notification.FLAG_SHOW_LIGHTS;

		notification.flags = mFlags;

		if(mIsPersistent)
			notification.flags |= Notification.FLAG_ONGOING_EVENT;

		if(!mForceUpdate)
			notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;

		notification.tickerText = mTickerText;
		notification.icon = getIcon();

		final String message = getMessage();
		notification.setLatestEventInfo(mContext, getTitle(), message, mContentIntent);

		//int lastHash = Settings.getLastNotificationMessageHash();

		final int lastHash = Settings.getInt(Settings.Keys.LAST_MSG_HASH);
		final int thisHash = message.hashCode();

		if(lastHash != thisHash)
		{
			notification.flags ^= Notification.FLAG_ONLY_ALERT_ONCE;
			Settings.putInt(Settings.Keys.LAST_MSG_HASH, thisHash);
		}

		notification.defaults = 0;

		if(Settings.getBoolean(Settings.Keys.USE_LED, true))
		{
			notification.flags |= Notification.FLAG_SHOW_LIGHTS;
			notification.ledARGB = 0xff0000ff;
			notification.ledOnMS = 200;
			notification.ledOffMS = 800;
			notification.defaults ^= Notification.DEFAULT_LIGHTS;
		}

		if(Settings.getBoolean(Settings.Keys.USE_SOUND, true) && mIsSilent)
		{
			final String ringtone = Settings.getString(Settings.Keys.NOTIFICATION_SOUND);
			if(ringtone != null)
				notification.sound = Uri.parse(ringtone);
			else
				notification.defaults |= Notification.DEFAULT_SOUND;
		}

		if(Settings.getBoolean(Settings.Keys.USE_VIBRATOR, true))
			notification.defaults |= Notification.DEFAULT_VIBRATE;

		final NotificationManager notificationMgr = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationMgr.notify(R.id.notification, notification);
	}

	public String getTitle() {
		return mMessage1 != null ? mTitle1 : mTitle2;
	}

	public String getMessage() {
		return mMessage1 != null ? mMessage1 : mMessage2;
	}

	public int getIcon()
	{
		// The use of mMessage2 is intentional here!
		return mMessage2 != null ? mIcon2 : mIcon1;
	}

	private MyNotification(Context context) {
		mContext = context;
	}
}
