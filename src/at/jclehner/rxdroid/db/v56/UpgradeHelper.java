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
