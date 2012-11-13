package at.jclehner.rxdroid;

import java.util.List;

import android.app.Notification;
import android.content.Context;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import at.jclehner.rxdroid.db.Drug;

public class NotificationHelper
{
	private Context mContext;

	private List<Drug> mDrugsWithMissedDoses;
	private List<Drug> mDrugsWithDueDoses;
	private List<Drug> mDrugsWithLowSupplies;

	private boolean mForceUpdate = false;
	private boolean mForceSilent = false;

	public NotificationHelper(Context context) {
		mContext = context;
	}

	public NotificationHelper missedDoses(List<Drug> drugs)
	{
		mDrugsWithMissedDoses = drugs;
		return this;
	}

	public NotificationHelper dueDoses(List<Drug> drugs)
	{
		mDrugsWithDueDoses = drugs;
		return this;
	}

	public NotificationHelper lowSupplies(List<Drug> drugs)
	{
		mDrugsWithLowSupplies = drugs;
		return this;
	}

	public NotificationHelper forceUpdate(boolean forceUpdate)
	{
		mForceUpdate = forceUpdate;
		mForceSilent = false;
		return this;
	}

	public NotificationHelper forceSilent(boolean forceSilent)
	{
		if(!mForceUpdate)
			mForceSilent = forceSilent;
		return this;
	}

	public NotificationCompat.Builder createBuilder()
	{
		final int missedDoses = mDrugsWithMissedDoses.size();
		final int dueDoses = mDrugsWithDueDoses.size();
		final int lowSupplyDrugs = mDrugsWithLowSupplies.size();

		int titleResId, icon;
		final StringBuilder sb = new StringBuilder();

		if(missedDoses != 0 || dueDoses != 0)
		{
			if(dueDoses != 0)
				sb.append(getQuantityString(R.plurals._qmsg_due, dueDoses));

			if(missedDoses != 0)
			{
				if(sb.length() != 0)
					sb.append(", ");

				sb.append(getQuantityString(R.plurals._qmsg_missed, missedDoses));
			}

			titleResId = R.string._title_notification_doses;
			icon = R.drawable.ic_stat_normal;
		}

		if(lowSupplyDrugs != 0)
		{
			icon = R.drawable.ic_stat_exclamation;

			if(sb.length() == 0)
			{
				titleResId = R.string._title_notification_low_supplies;

				if(lowSupplyDrugs == 1)
					sb.append(mContext.getString(R.string._qmsg_low_supply_single));
				else
				{
					final String first = mDrugsWithLowSupplies.get(0).getName();
					final String second = mDrugsWithLowSupplies.get(1).getName();

					sb.append(getQuantityString(R.plurals._qmsg_low_supply_multiple, lowSupplyDrugs, first, second));
				}
			}
		}

		final String message = sb.toString();

		final int currentHash = message.hashCode();
		final int lastHash = Settings.getInt(Settings.Keys.LAST_MSG_HASH);

		final NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
		builder.setContentTitle(mContext.getString(titleResId));
		builder.setTicker(mContext.getString(R.string._msg_new_notification));
		builder.setSmallIcon(icon);
		builder.setOngoing(true);

		if(mForceUpdate || currentHash != lastHash)
		{
			builder.setOnlyAlertOnce(false);
			Settings.putInt(Settings.Keys.LAST_MSG_HASH, currentHash);
		}
		else
			builder.setOnlyAlertOnce(true);

		if(Settings.getBoolean(Settings.Keys.USE_LED, true))
			builder.setLights(0xff0000ff, 200, 800);

		int defaults;

		if(Settings.getBoolean(Settings.Keys.USE_SOUND, true))
		{
			final String ringtone = Settings.getString(Settings.Keys.NOTIFICATION_SOUND);
			if(ringtone != null)
				builder.setSound(Uri.parse(ringtone));
			else
				defaults |= Notification.DEFAULT_SOUND;
		}

		if(Settings.getBoolean(Settings.Keys.USE_VIBRATOR, true))
			defaults |= Notification.DEFAULT_VIBRATE;

		builder.setDefaults(defaults);
		return builder;
	}

	private String getQuantityString(int id, int quantity, Object... args) {
		return mContext.getResources().getQuantityString(id, quantity, args);
	}

	private static boolean isEmpty(List<Drug> drugs) {
		return drugs == null || drugs.isEmpty();
	}
}
