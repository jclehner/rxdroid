package at.caspase.rxdroid.db.v48;

import java.sql.SQLException;

import at.caspase.rxdroid.db.DatabaseHelper.DatabaseError;
import at.caspase.rxdroid.db.Schedule;

import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class Hook implements Runnable
{
	ConnectionSource mCs;

	public Hook() {}

	public Hook(ConnectionSource cs) {
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
