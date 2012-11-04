/**
 * Copyright (C) 2012 Joseph Lehner <joseph.c.lehner@gmail.com>
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

package at.jclehner.rxdroid.db.v48;

import java.sql.SQLException;

import at.jclehner.rxdroid.db.DatabaseHelper.DatabaseError;
import at.jclehner.rxdroid.db.Schedule;

import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class BeforeDatabaseUpgradeHook implements Runnable
{
	ConnectionSource mCs;

	public BeforeDatabaseUpgradeHook() {}

	public BeforeDatabaseUpgradeHook(ConnectionSource cs) {
		mCs = cs;
	}

	@Override
	public void run()
	{
		try
		{
			TableUtils.createTable(mCs, Schedule.class);
		}
		catch(SQLException e)
		{
			throw new DatabaseError(DatabaseError.E_GENERAL, e);
		}
	}
}
