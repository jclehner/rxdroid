package at.caspase.rxdroid.debug;

import java.sql.Date;
import java.sql.Time;

import at.caspase.rxdroid.DumbTime;
import at.caspase.rxdroid.util.DateTime;

public class NotificationServiceInfo
{
	public boolean isStarted = false;
	public boolean isThreadStarted = false;
	public Date date;
	public int activeDoseTime = 0xdeadbeef;
	public int nextDoseTime = 0xdeadbeef;
	public Time timeOfSleepBegin = null;
	public long sleepingUntil = -1;
	public int lastNotificationHash;
	public int pendingIntakes;
	public int forgottenIntakes;
	
	@Override
	public String toString()
	{
		final String sleepingUntilInfo;
		
		if(sleepingUntil != -1)
		{
			final long sleepingUntilOffset = sleepingUntil - DateTime.today().getTime();
			final DumbTime time = new DumbTime(sleepingUntilOffset, true);
			
			sleepingUntilInfo = time.toString();
		}
		else
			sleepingUntilInfo = "(not sleeping)";		
		
		return
			"NotificationService Info:" +
			"\n  isStarted           : " + isStarted +
			"\n  isThreadStarted     : " + isThreadStarted +
			"\n  date                : " + date +
			"\n  activeDoseTime      : " + activeDoseTime +
			"\n  nextDoseTime        : " + nextDoseTime +
			"\n  timeOfSleepBegin    : " + timeOfSleepBegin +
			"\n  sleepingUntil       : " + sleepingUntilInfo +
			"\n  lastNotificationHash: " + lastNotificationHash +
			"\n  pendingIntakes      : " + pendingIntakes +
			"\n  forgottenIntakes    : " + forgottenIntakes +
			"\n--------------------------------------" +
			"\n";
		
	}
}
