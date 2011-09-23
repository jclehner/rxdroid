package at.caspase.rxdroid.debug;

import java.sql.Date;

import at.caspase.rxdroid.DumbTime;
import at.caspase.rxdroid.util.DateTime;

public class NotificationServiceInfo
{
	public boolean isStarted = false;
	public boolean isThreadStarted = false;
	public Date date;
	public int activeDoseTime = 0xdeadbeef;
	public int nextDoseTime = 0xdeadbeef;
	public long sleepingUntil = -1;
	public int lastNotificationHash;
	public int pendingIntakes;
	public int forgottenIntakes;
	
	@Override
	public String toString()
	{
		long sleepingUntilOffset = sleepingUntil - DateTime.today().getTime();
		
		return
			"NotificationService Info:" +
			"\n  isStarted           : " + isStarted +
			"\n  isThreadStarted     : " + isThreadStarted +
			"\n  date                : " + date +
			"\n  activeDoseTime      : " + activeDoseTime +
			"\n  nextDoseTime        : " + nextDoseTime +
			"\n  sleepingUntil       : " + (sleepingUntil == -1 ? "(not sleeping)" : new DumbTime(sleepingUntilOffset, true)) +
			"\n  lastNotificationHash: " + lastNotificationHash +
			"\n  pendingIntakes      : " + pendingIntakes +
			"\n  forgottenIntakes    : " + forgottenIntakes +
			"\n--------------------------------------" +
			"\n";
		
	}
}
