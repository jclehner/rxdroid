package at.caspase.rxdroid;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.widget.RemoteViews;
import at.caspase.rxdroid.util.Constants;

public class MyNotification
{
	private static final String TAG = MyNotification.class.getName();

	private Notification mNotification;

	private int mPendingCount;
	private int mForgottenCount;
	private String mLowSupplyMessage;

	private Context mContext = GlobalContext.get();

	public MyNotification()
	{
		String tickerText = mContext.getString(R.string._msg_new_notification);
		mNotification = new Notification(R.drawable.ic_stat_pill, tickerText, 0);

		final Intent intent = new Intent(mContext, DrugListActivity.class);
		intent.setAction(Intent.ACTION_VIEW);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		mNotification.contentIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
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

	public void update()
	{
		NotificationManager notificationMgr =
				(NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

		if((mPendingCount + mForgottenCount) == 0 && mLowSupplyMessage == null)
			notificationMgr.cancel(R.id.notification);
		else
		{
			String doseMessage = null;
			int notificationItems = 1;

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

			int messageHash = message.hashCode();
			if(settings.getLastNotificationMessageHash() != messageHash)
			{
				settings.setLastNotificationMessageHash(messageHash);
				mNotification.flags ^= Notification.FLAG_ONLY_ALERT_ONCE;
			}

			if(notificationItems < settings.getLastNotificationCount())
				mNotification.defaults ^= Notification.DEFAULT_ALL;

			notificationMgr.notify(R.id.notification, mNotification);
			settings.setLastNotificationCount(notificationItems);
		}
	}

	private CharSequence createTitleSpannable(String title)
	{
		int appearance = getAppearanceResId("TextAppearance_StatusBar_EventContent_Title",
				android.R.style.TextAppearance_Medium_Inverse);

		return createSpannableWithAppearance(title, appearance);
	}

	private CharSequence createContentSpannable(String content)
	{
		int appearance = getAppearanceResId("TextAppearance_StatusBar_EventContent",
				android.R.style.TextAppearance_Small_Inverse);

		return createSpannableWithAppearance(content, appearance);
	}

	private SpannableString createSpannableWithAppearance(String string, int appearance)
	{
		SpannableString s = new SpannableString(string);
		s.setSpan(new TextAppearanceSpan(mContext, appearance), 0, s.length() - 1, 0);
		return s;
	}

	private int getAppearanceResId(String resIdFieldName, int defaultResId)
	{
		Class<?> cls = android.R.style.class;
		try
		{
			Field f = cls.getField(resIdFieldName);
			return f.getInt(null);
		}
		catch(IllegalAccessException e)
		{
			// eat exception
		}
		catch(SecurityException e)
		{
			// eat exception
		}
		catch(NoSuchFieldException e)
		{
			// eat exception
		}

		Log.w(TAG, "getAppearance: inaccessible field in android.R.style: " + resIdFieldName);

		return defaultResId;
	}



}
