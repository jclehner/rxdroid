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
