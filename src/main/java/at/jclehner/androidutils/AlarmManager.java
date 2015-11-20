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

	public static class Alarm
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

		static Alarm type(int type)
		{
			final Alarm alarm = new Alarm();
			alarm.type = type;
			alarm.elapsed = (type == ELAPSED_REALTIME || type == ELAPSED_REALTIME_WAKEUP);
			alarm.wakeup = (type == ELAPSED_REALTIME_WAKEUP || type == RTC_WAKEUP);
			return alarm;
		}

		public Alarm elapsed(boolean elapsed)
		{
			this.elapsed = elapsed;
			return this;
		}

		public static Alarm elapsed()
		{
			return new Alarm().elapsed(true);
		}

		public Alarm rtc(boolean rtc)
		{
			return elapsed(!rtc);
		}

		public static Alarm rtc()
		{
			return new Alarm().rtc(true);
		}

		public Alarm wakeup()
		{
			return wakeup(true);
		}

		public Alarm wakeup(boolean wakeup)
		{
			this.wakeup = wakeup;
			return this;
		}

		public Alarm allowWhileIdle()
		{
			return allowWhileIdle(true);
		}

		public Alarm allowWhileIdle(boolean allowWhileIdle)
		{
			this.allowWhileIdle = allowWhileIdle;
			return this;
		}

		public Alarm exact()
		{
			return exact(true);
		}

		public Alarm exact(boolean exact)
		{
			this.exact = exact;
			return this;
		}

		public Alarm time(long triggerAtMillis)
		{
			this.triggerAtMillis = triggerAtMillis;
			this.intervalMillis = this.windowStartMillis = this.windowLengthMillis = -1;
			return this;
		}

		public Alarm repeating(long triggerAtMillis, long intervalMillis)
		{
			this.intervalMillis = intervalMillis;
			this.windowStartMillis = this.windowLengthMillis = -1;
			return time(triggerAtMillis);
		}

		public Alarm window(long windowStartMillis, long windowLengthMillis)
		{
			if(windowStartMillis == -1 ^ windowLengthMillis == -1)
			{
				throw new IllegalArgumentException(windowStartMillis + ", " + windowLengthMillis);
			}

			this.windowStartMillis = windowStartMillis;
			this.windowLengthMillis = windowLengthMillis;
			this.triggerAtMillis = this.intervalMillis = -1;

			return this;
		}

		@Override
		public String toString()
		{
			return "Alarm {"
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
			if(this.triggerAtMillis == -1 && this.windowStartMillis == -1)
				throw new IllegalArgumentException("No alarm time specified");

			if(this.windowStartMillis != -1 && this.intervalMillis != -1)
				throw new IllegalArgumentException("Both window and repeating specified");


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

	private static final SparseArray<Alarm> sAlarms = new SparseArray<>();
	private final android.app.AlarmManager mAm;

	public static AlarmManager from(Context context)
	{
		return new AlarmManager(context.getApplicationContext());
	}

	@TargetApi(Build.VERSION_CODES.M)
	public void set(int id, Alarm alarm, PendingIntent operation)
	{
		alarm.verifyAndAdjust();

		Log.d(TAG, "set: #" + id + ", " + alarm);

		if(alarm.triggerAtMillis != -1)
		{
			if(alarm.intervalMillis == -1)
			{
				if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || !alarm.exact)
					mAm.set(alarm.type, alarm.triggerAtMillis, operation);
				else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || !alarm.allowWhileIdle)
					mAm.setExact(alarm.type, alarm.triggerAtMillis, operation);
				else
					mAm.setExactAndAllowWhileIdle(alarm.type, alarm.triggerAtMillis, operation);
			}
			else
			{
				if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT && !alarm.exact)
				{
					mAm.setInexactRepeating(alarm.type, alarm.triggerAtMillis, alarm.intervalMillis,
							operation);
				}
				else
				{
					mAm.setRepeating(alarm.type, alarm.triggerAtMillis, alarm.intervalMillis,
							operation);
				}
			}
		}
		else if(alarm.windowStartMillis != -1)
		{
			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
				mAm.set(alarm.type, alarm.windowStartMillis, operation);
			else
			{
				mAm.setWindow(alarm.type, alarm.windowStartMillis, alarm.windowLengthMillis,
						operation);
			}
		}

		sAlarms.put(id, alarm);
	}

	public void set(int id, int type, long triggerAtMillis, PendingIntent operation)
	{
		set(id, Alarm.type(type).time(triggerAtMillis), operation);
	}

	public void setAndAllowWhileIdle(int id, int type, long triggerAtMillis,
			PendingIntent operation)
	{
		set(id, Alarm.type(type).time(triggerAtMillis).allowWhileIdle(), operation);
	}

	public void setExact(int id, int type, long triggerAtMillis, PendingIntent operation)
	{
		set(id, Alarm.type(type).time(triggerAtMillis).exact(), operation);
	}

	public void setExactAndAllowWhileIdle(int id, int type, long triggerAtMillis,
										  PendingIntent operation)
	{
		set(id, Alarm.type(type).time(triggerAtMillis).exact().allowWhileIdle(), operation);
	}

	public void setInexactRepeating(int id, int type, long triggerAtMillis, long intervalMillis,
									PendingIntent operation)
	{
		set(id, Alarm.type(type).repeating(triggerAtMillis, intervalMillis), operation);
	}

	public void setRepeating(int id, int type, long triggerAtMillis, long intervalMillis,
							 PendingIntent operation)
	{
		set(id, Alarm.type(type).repeating(triggerAtMillis, intervalMillis).exact(), operation);
	}

	public void setWindow(int id, int type, long windowStartMillis, long windowLengthMillis,
						  PendingIntent operation)
	{
		set(id, Alarm.type(type).window(windowStartMillis, windowLengthMillis), operation);
	}

	public void setAlarmClock(int id, Object info, PendingIntent operation)
	{
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			final AlarmClockInfo alarmInfo = (AlarmClockInfo) info;
			cancelIfExists(id, operation);
			mAm.setAlarmClock(alarmInfo, operation);
			sAlarms.put(id, Alarm.rtc().wakeup().exact().time(alarmInfo.getTriggerTime()));
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

	public static void clearAlarms()
	{
		sAlarms.clear();
	}

	public static long onAlarmTriggered(int id)
	{
		final Alarm alarm = sAlarms.get(id);
		if(alarm == null)
		{
			Log.w(TAG, "Unknown alarm #" + id);
			return 0;
		}

		Log.d(TAG, "onAlarmTriggered: #" + id + ": " + alarm);

		sAlarms.remove(id);

		final long now = alarm.elapsed ? SystemClock.elapsedRealtime() : System.currentTimeMillis();

		if(alarm.triggerAtMillis != -1)
		{
			final long diff = now - alarm.triggerAtMillis;

			if(alarm.intervalMillis == -1)
				return diff;
			else
				return diff % alarm.intervalMillis;
		}
		else if(alarm.windowStartMillis != -1)
		{
			final long end = alarm.windowStartMillis + alarm.windowLengthMillis;
			if(now >= alarm.windowStartMillis && now <= end)
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
