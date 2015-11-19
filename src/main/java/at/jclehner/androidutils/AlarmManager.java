package at.jclehner.androidutils;

import android.annotation.TargetApi;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

public class AlarmManager
{
	static final String TAG = AlarmManager.class.getSimpleName();

	public static final int ELAPSED_REALTIME = android.app.AlarmManager.ELAPSED_REALTIME;
	public static final int ELAPSED_REALTIME_WAKEUP = android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP;
	public static final int RTC = android.app.AlarmManager.RTC;
	public static final int RTC_WAKEUP = android.app.AlarmManager.RTC_WAKEUP;

	public static final long INTERVAL_FIFTEEN_MINUTES = android.app.AlarmManager.INTERVAL_FIFTEEN_MINUTES;
	public static final long INTERVAL_HALF_HOUR = android.app.AlarmManager.INTERVAL_HALF_HOUR;
	public static final long INTERVAL_HOUR = android.app.AlarmManager.INTERVAL_HOUR;
	public static final long INTERVAL_HALF_DAY = android.app.AlarmManager.INTERVAL_HALF_DAY;
	public static final long INTERVAL_DAY = android.app.AlarmManager.INTERVAL_DAY;

	public static class Args
	{
		private boolean elapsed = true;
		private boolean wakeup = false;

		int type = -1;

		boolean exact = false;
		boolean allowWhileIdle = false;

		long triggerAtMillis = -1;
		long intervalMillis = -1;
		long windowStartMillis = -1;
		long windowLengthMillis = -1;

		static Args type(int type)
		{
			final Args args = new Args();
			args.type = type;
			args.elapsed = (type == ELAPSED_REALTIME || type == ELAPSED_REALTIME_WAKEUP);
			args.wakeup = (type == ELAPSED_REALTIME_WAKEUP || type == RTC_WAKEUP);
			return args;
		}

		public Args elapsed(boolean elapsed)
		{
			this.elapsed = elapsed;
			return this;
		}

		public static Args elapsed()
		{
			return new Args().elapsed(true);
		}

		public Args rtc(boolean rtc)
		{
			return elapsed(!rtc);
		}

		public static Args rtc()
		{
			return new Args().rtc(true);
		}

		public Args wakeup()
		{
			return wakeup(true);
		}

		public Args wakeup(boolean wakeup)
		{
			this.wakeup = wakeup;
			return this;
		}

		public Args allowWhileIdle()
		{
			return allowWhileIdle(true);
		}

		public Args allowWhileIdle(boolean allowWhileIdle)
		{
			this.allowWhileIdle = allowWhileIdle;
			return this;
		}

		public Args exact()
		{
			return exact(true);
		}

		public Args exact(boolean exact)
		{
			this.exact = exact;
			return this;
		}

		public Args time(long triggerAtMillis)
		{
			this.triggerAtMillis = triggerAtMillis;
			return this;
		}

		public Args repeating(long triggerAtMillis, long intervalMillis)
		{
			this.intervalMillis = intervalMillis;
			return time(triggerAtMillis);
		}

		public Args window(long windowStartMillis, long windowLengthMillis)
		{
			if(windowStartMillis == -1 ^ windowLengthMillis == -1)
			{
				throw new IllegalArgumentException(windowStartMillis + ", " + windowLengthMillis);
			}

			this.windowStartMillis = windowStartMillis;
			this.windowLengthMillis = windowLengthMillis;

			return this;
		}

		@Override
		public String toString()
		{
			return "Args {"
					+ " type=" + (elapsed ? "elapsed" : "rtc")
					+ " wakeup=" + wakeup
					+ " exact=" + exact
					+ " whileIdle=" + allowWhileIdle
					+ " trigger=" + triggerAtMillis
					+ " interval=" + intervalMillis
					+ " window=(" + windowStartMillis + ", " + windowLengthMillis + ")"
					+ " }";
		}

		void verifyAndAdjust()
		{
			// No need to check windowLengthMillis, as this is done by window().
			if (this.triggerAtMillis == -1 && this.windowStartMillis == -1)
			{
				throw new IllegalArgumentException("No alarm time specified");
			}

			if(type == -1)
			{
				if(elapsed)
				{
					type = wakeup ? AlarmManager.ELAPSED_REALTIME_WAKEUP : AlarmManager.ELAPSED_REALTIME;
				}
				else
				{
					type = wakeup ? AlarmManager.RTC_WAKEUP : AlarmManager.RTC;
				}
			}
		}
	}

	private static final SparseArray<Args> sAlarms = new SparseArray<>();
	private final android.app.AlarmManager mAm;

	public static AlarmManager from(Context context)
	{
		return new AlarmManager(context.getApplicationContext());
	}

	@TargetApi(Build.VERSION_CODES.M)
	public void set(int id, Args args, PendingIntent operation)
	{
		args.verifyAndAdjust();

		if(args.triggerAtMillis != -1)
		{
			if(args.intervalMillis == -1)
			{
				if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || !args.exact)
					mAm.set(args.type, args.triggerAtMillis, operation);
				else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || !args.allowWhileIdle)
					mAm.setExact(args.type, args.triggerAtMillis, operation);
				else
					mAm.setExactAndAllowWhileIdle(args.type, args.triggerAtMillis, operation);
			}
			else
			{
				if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT && !args.exact)
				{
					mAm.setInexactRepeating(args.type, args.triggerAtMillis, args.intervalMillis,
							operation);
				}
				else
				{
					mAm.setRepeating(args.type, args.triggerAtMillis, args.intervalMillis,
							operation);
				}
			}
		}
		else if(args.windowStartMillis != -1)
		{
			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
				mAm.set(args.type, args.windowStartMillis, operation);
			else
			{
				mAm.setWindow(args.type, args.windowStartMillis, args.windowLengthMillis,
						operation);
			}
		}

		sAlarms.put(id, args);
	}

	public void set(int id, int type, long triggerAtMillis, PendingIntent operation)
	{
		set(id, Args.type(type).time(triggerAtMillis), operation);
	}

	public void setAndAllowWhileIdle(int id, int type, long triggerAtMillis,
			PendingIntent operation)
	{
		set(id, Args.type(type).time(triggerAtMillis).allowWhileIdle(), operation);
	}

	public void setExact(int id, int type, long triggerAtMillis, PendingIntent operation)
	{
		set(id, Args.type(type).time(triggerAtMillis).exact(), operation);
	}

	public void setExactAndAllowWhileIdle(int id, int type, long triggerAtMillis,
										  PendingIntent operation)
	{
		set(id, Args.type(type).time(triggerAtMillis).exact().allowWhileIdle(), operation);
	}

	public void setInexactRepeating(int id, int type, long triggerAtMillis, long intervalMillis,
									PendingIntent operation)
	{
		set(id, Args.type(type).repeating(triggerAtMillis, intervalMillis), operation);
	}

	public void setRepeating(int id, int type, long triggerAtMillis, long intervalMillis,
							 PendingIntent operation)
	{
		set(id, Args.type(type).repeating(triggerAtMillis, intervalMillis).exact(), operation);
	}

	public void setWindow(int id, int type, long windowStartMillis, long windowLengthMillis,
						  PendingIntent operation)
	{
		set(id, Args.type(type).window(windowStartMillis, windowLengthMillis), operation);
	}

	public void setAlarmClock(int id, Object info, PendingIntent operation)
	{
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			final AlarmClockInfo alarmInfo = (AlarmClockInfo) info;
			cancelIfExists(id, operation);
			mAm.setAlarmClock(alarmInfo, operation);
			sAlarms.put(id, Args.rtc().wakeup().exact().time(alarmInfo.getTriggerTime()));
		}

		throw new UnsupportedOperationException();
	}

	public Object getNextAlarmClock()
	{
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			return mAm.getNextAlarmClock();

		throw new UnsupportedOperationException();
	}

	public void cancel(int id, PendingIntent operation)
	{
		mAm.cancel(operation);
		sAlarms.remove(id);
	}

	public static long onAlarmTriggered(int id)
	{
		final Args args = sAlarms.get(id);
		if(args == null)
		{
			Log.w(TAG, "Unknown alarm #" + id);
			return 0;
		}

		sAlarms.remove(id);

		final long now = args.elapsed ? SystemClock.elapsedRealtime() : System.currentTimeMillis();

		if(args.triggerAtMillis != -1)
		{
			final long diff = now - args.intervalMillis;

			if(args.intervalMillis == -1)
				return diff;
			else
				return diff % args.intervalMillis;
		}
		else if(args.windowStartMillis != -1)
		{
			final long end = args.windowStartMillis + args.windowLengthMillis;
			if(now >= args.windowStartMillis && now <= end)
				return 0;

			return now - end;
		}

		return 0;
	}

	private void cancelIfExists(int id, PendingIntent operation)
	{
		if(sAlarms.indexOfKey(id) >= 0)
		{
			Log.w(TAG, "Overwriting alarm #" + id + ": " + sAlarms.get(id));
			cancel(id, operation);
		}
	}

	private AlarmManager(Context context)
	{
		mAm = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	}
}
