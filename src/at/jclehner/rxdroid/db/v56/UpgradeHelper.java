/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2013 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
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

package at.jclehner.rxdroid.db.v56;

import java.sql.SQLException;

import at.jclehner.rxdroid.db.Schedule;
import at.jclehner.rxdroid.db.SchedulePart;

import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public final class UpgradeHelper
{
	public static void upgradeDatabase(ConnectionSource cs) throws SQLException
	{
		TableUtils.dropTable(cs, Schedule.class, true);
		TableUtils.createTable(cs, Schedule.class);
		TableUtils.createTableIfNotExists(cs, SchedulePart.class);
	}
}
