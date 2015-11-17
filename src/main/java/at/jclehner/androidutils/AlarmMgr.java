package at.jclehner.androidutils;

import android.app.AlarmManager;
import android.os.Build;

/**
 * Created by jclehner on 16.11.15.
 */
public class AlarmMgr
{
	public static class Args
	{
		private boolean elapsed = true;
		private boolean wakeup = true;

		int mode = -1;

		boolean exact = true;
		boolean allowWhileIdle = true;

		long triggerAtMillis = -1;
		long intervalMillis = -1;
		long windowStartMillis = -1;
		long windowLengthMillis = -1;

		Args elapsed(boolean elapsed)
		{
			this.elapsed = elapsed;
			return this;
		}

		Args elapsed()
		{
			return elapsed(true);
		}

		Args rtc(boolean rtc)
		{
			return elapsed(!rtc);
		}

		Args rtc()
		{
			return rtc(true);
		}

		Args mode(int mode)
		{
			this.mode = mode;
		}

		Args exact(boolean exact)
		{
			this.exact = exact;
		}

		Args allowWhileIdle(boolean allowWhileIdle)
		{
			this.allowWhileIdle = allowWhileIdle;
		}

		Args time(long triggerAtMillis)
		{
			this.triggerAtMillis = triggerAtMillis;
			return this;
		}

		Args setInterval(long intervalMillis)
		{
			this.intervalMillis = intervalMillis;
			return this;
		}

		Args setWindow(long windowStartMillis, long windowLengthMillis)
		{
			if (windowStartMillis == -1 ^ windowLengthMillis == -1) {
				throw new IllegalArgumentException(windowStartMillis + ", " + windowLengthMillis);
			}

			this.windowStartMillis = windowStartMillis;
			this.windowLengthMillis = windowLengthMillis;

		}

		private static long validateTime(long time, String name)
		{
			if (time < 0) {
				throw new IllegalArgumentException(name + "=" + time);
			}

			return time;
		}


	}




		void adjustAndVerify()
		{
			if (mode == -1) {
				if (elapsed) {
					mode = wakeup ? AlarmManager.ELAPSED_REALTIME_WAKEUP : AlarmManager.ELAPSED_REALTIME;
				} else {
					mode = wakeup ? AlarmManager.RTC_WAKEUP : AlarmManager.RTC;
				}
			}
		}
}
