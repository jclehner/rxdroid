package at.jclehner.androidutils;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;

public class NotificationBuilder extends NotificationCompat.Builder
{
	public static final int HEADS_UP_NATIVE = 0;
	public static final int HEADS_UP_FLASH = 1;
	public static final int HEADS_UP_DISABLE = 2;

	private final Context mContext;
	private final Handler mHandler;

	private int mHeadsUpMode = HEADS_UP_NATIVE;
	private long mMaxFlashDuration = 0;
	private int mDefaults = 0;
	private Uri mSound = null;
	private long[] mPattern = null;
	private int mPriority = NotificationCompat.PRIORITY_DEFAULT;
	private int mStreamType = Notification.STREAM_DEFAULT;

	public NotificationBuilder(Context context)
	{
		super(context);
		mContext = context;
		mHandler = new Handler();
	}

	public void setHeadsUpMode(int mode, long maxFlashDuration)
	{
		mHeadsUpMode = mode;
		mMaxFlashDuration = maxFlashDuration;
	}

	public void setHeadsUpMode(int mode) {
		setHeadsUpMode(mode, mMaxFlashDuration);
	}

	@Override
	public NotificationBuilder setDefaults(int defaults)
	{
		super.setDefaults(defaults);
		mDefaults = defaults;
		return this;
	}

	@Override
	public NotificationBuilder setSound(Uri sound)
	{
		super.setSound(sound);
		mSound = sound;
		return this;
	}

	@Override
	public NotificationBuilder setSound(Uri sound, int streamType)
	{
		super.setSound(sound, streamType);
		mSound = sound;
		mStreamType = streamType;
		return this;
	}

	@Override
	public NotificationBuilder setVibrate(long[] pattern)
	{
		super.setVibrate(pattern);
		mPattern = pattern;
		return this;
	}

	@Override
	public NotificationBuilder setPriority(int priority)
	{
		super.setPriority(priority);
		mPriority = priority;
		return this;
	}

	public void notify(int id) {
		notify(null, id);
	}

	public void notify(final String tag, final int id)
	{
		final NotificationManager nm = (NotificationManager)
				mContext.getSystemService(Context.NOTIFICATION_SERVICE);

		final boolean needFix = Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT &&
				mHeadsUpMode != HEADS_UP_NATIVE && mPriority >= NotificationCompat.PRIORITY_HIGH &&
				(mSound != null || (mDefaults & Notification.DEFAULT_SOUND) != 0) &&
				(mPattern != null || (mDefaults & Notification.DEFAULT_VIBRATE) != 0);

		if(!needFix)
		{
			// Fire away!
			nm.notify(tag, id, build());
		}
		else
		{
			// From the Android docs: "Examples of conditions that may trigger
			// heads-up notifications include: [...] The notification has high
			// priority and uses ringtones or vibrations.
			//
			// We use the following approach:
			// 1) Display the notification with default priority
			// 2) Immediately cancel it
			// 3) Display the notification with its set priority,
			//    but disable sound and vibration.
			//
			// It's important that the second notification retains the
			// set priority, as this determines the notification's
			// sort rank.

			if(mHeadsUpMode != HEADS_UP_FLASH)
				setPriority(NotificationCompat.PRIORITY_DEFAULT);

			nm.notify(tag, id, build());

			mHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					setPriority(mPriority);
					setSound(null);
					setVibrate(null);
					setDefaults((mDefaults ^ Notification.DEFAULT_SOUND)
							^ Notification.DEFAULT_VIBRATE);

					nm.cancel(tag, id);
					nm.notify(tag, id, build());

					// Restore the builder's state

					if(mSound != null)
					{
						if(mStreamType != Notification.STREAM_DEFAULT)
							setSound(mSound, mStreamType);
						else
							setSound(mSound);
					}

					setVibrate(mPattern);
					setDefaults(mDefaults);
				}
			}, getDelay());
		}
	}

	private long getDelay()
	{
		long delay = 0;

		if(mPattern != null)
		{
			for(long part : mPattern)
				delay += part;
		}

		if(mSound != null)
			delay = Math.max(MediaPlayer.create(mContext, mSound).getDuration(), delay);

		delay = Math.max(1000, delay + 100);

		if(mHeadsUpMode == HEADS_UP_FLASH && mMaxFlashDuration > 0)
			delay = Math.min(delay, mMaxFlashDuration);

		return delay;
	}
}
