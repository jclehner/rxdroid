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


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.widget.RemoteViews;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.Util;

public class MyNotification
{
	private static final String TAG = MyNotification.class.getName();
	private static final boolean LOGV = true;

	public static final int FLAG_SILENT = 1;
	
	private Notification mNotification;
	private Context mContext;

	private int mPendingCount;
	private int mForgottenCount;
	private String mLowSupplyMessage;
	
	private boolean mIsSnoozing = false;

	public MyNotification(Context context)
	{
		String tickerText = context.getString(R.string._msg_new_notification);
		mNotification = new Notification(R.drawable.ic_stat_pill, tickerText, 0);
		mContext = context;
	}

	public void setPendingCount(int pendingCount) {
		mPendingCount = pendingCount;
	}

	public void setForgottenCount(int forgottenCount) {
		mForgottenCount = forgottenCount;
	}

	public void setLowSupplyMessage(String lowSupplyMessage) {
		mLowSupplyMessage = lowSupplyMessage;
	}
	
	@Deprecated
	public void update() {
		update(true, 0);
	}

	public void update(boolean forceUpdate, int flags)
	{
		if(LOGV) Log.d(TAG, "update(" + forceUpdate + ", " + flags + ")");
		
		NotificationManager notificationMgr =
				(NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

		if(mIsSnoozing && forceUpdate)
			mIsSnoozing = false;
		
		boolean haveNoDoses = !mIsSnoozing && (mPendingCount + mForgottenCount) == 0;
				
		if(haveNoDoses && mLowSupplyMessage == null)
			notificationMgr.cancel(R.id.notification);
		else
		{
			String doseMessage = null;
			int notificationItems = 1;

			if(!mIsSnoozing)
			{			
				if(mPendingCount != 0 && mForgottenCount != 0)
				{
					doseMessage = mContext.getString(R.string._msg_doses_fp, mForgottenCount, mPendingCount);
					notificationItems = 2;
				}
				else if(mPendingCount != 0)
					doseMessage = mContext.getString(R.string._msg_doses_p, mPendingCount);
				else if(mForgottenCount != 0)
					doseMessage = mContext.getString(R.string._msg_doses_f, mForgottenCount);
				else
					notificationItems = 0;
			}
			else
				doseMessage = mContext.getString(R.string._msg_snoozing);

			final String bullet;

			if(doseMessage == null || mLowSupplyMessage == null)
				bullet = "";
			else
				bullet = Constants.NOTIFICATION_BULLET;

			final StringBuilder sb = new StringBuilder();

			if(doseMessage != null)
				sb.append(bullet + doseMessage);

			if(mLowSupplyMessage != null)
			{
				if(doseMessage != null)
					sb.append("\n");

				sb.append(bullet + mLowSupplyMessage);
				++notificationItems;
			}

			final String message = sb.toString();

			final RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.notification);
			views.setCharSequence(R.id.stat_title, "setText",
					createTitleSpannable(mContext.getString(R.string._title_notifications)));
			views.setCharSequence(R.id.stat_text, "setText", createContentSpannable(message));
			views.setTextViewText(R.id.stat_time, "");

			mNotification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONLY_ALERT_ONCE;
			mNotification.defaults |= Notification.DEFAULT_ALL;
			mNotification.contentView = views;
			if(notificationItems > 1)
				mNotification.number = notificationItems;

			Settings settings = Settings.instance();
			settings.setLastNotificationCount(notificationItems);
			
			int messageHash = message.hashCode();
			
			if(forceUpdate || settings.getLastNotificationMessageHash() != messageHash)
			{
				settings.setLastNotificationMessageHash(messageHash);
				mNotification.flags ^= Notification.FLAG_ONLY_ALERT_ONCE;
			}
			
			applyDefaultsFromSettings();
			setupNotificationContentIntent();
			
			boolean silent = (flags & FLAG_SILENT) != 0;
			
			if(silent || (!forceUpdate && notificationItems < settings.getLastNotificationCount()))
				mNotification.defaults ^= Notification.DEFAULT_ALL;			
	
			notificationMgr.notify(R.id.notification, mNotification);		
		}
	}
	
	public void setSnoozeMessageEnabled(boolean enabled) {
		mIsSnoozing = enabled;
	}
	
	public boolean isSnoozing() {
		return mIsSnoozing;
	}
	
	private void setupNotificationContentIntent()
	{		
		if(!mIsSnoozing)
		{		
			final Intent intent = new Intent(mContext, DrugListActivity.class);
			intent.setAction(Intent.ACTION_VIEW);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra(DrugListActivity.EXTRA_STARTED_FROM_NOTIFICATION, true);			
			int flags = PendingIntent.FLAG_UPDATE_CURRENT;
			mNotification.contentIntent = PendingIntent.getActivity(mContext, 0, intent, flags);	
		}
		else
		{
			final Intent intent = new Intent(mContext, NotificationReceiver.class);
			intent.putExtra(NotificationReceiver.EXTRA_CANCEL_SNOOZE, true);			
			int flags = PendingIntent.FLAG_CANCEL_CURRENT;			
			mNotification.contentIntent = PendingIntent.getBroadcast(mContext, 0, intent, flags);			
		}			
	}
	
	private void applyDefaultsFromSettings()
	{
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
		
		if(!sp.getBoolean("use_led", true))
			mNotification.defaults ^= Notification.DEFAULT_LIGHTS;
		
		if(!sp.getBoolean("use_sound", true))
			mNotification.defaults ^= Notification.DEFAULT_SOUND;
		
		if(!sp.getBoolean("use_vibrator", true))
			mNotification.defaults ^= Notification.DEFAULT_VIBRATE;	
	}

	private CharSequence createTitleSpannable(String title)
	{
		int appearance = Util.getStyleResId("TextAppearance_StatusBar_EventContent_Title",
				android.R.style.TextAppearance_Medium_Inverse);

		return createSpannableWithAppearance(title, appearance);
	}

	private CharSequence createContentSpannable(String content)
	{
		int appearance = Util.getStyleResId("TextAppearance_StatusBar_EventContent",
				android.R.style.TextAppearance_Small_Inverse);

		return createSpannableWithAppearance(content, appearance);
	}

	private SpannableString createSpannableWithAppearance(String string, int appearance)
	{
		SpannableString s = new SpannableString(string);
		s.setSpan(new TextAppearanceSpan(mContext, appearance), 0, s.length() - 1, 0);
		return s;
	}
}
