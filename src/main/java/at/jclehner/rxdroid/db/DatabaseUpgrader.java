package at.jclehner.rxdroid.db;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.table.TableUtils;

import java.io.Closeable;
import java.sql.SQLException;

public class DatabaseUpgrader implements Closeable
{
	private final SQLiteDatabase mDb;
	private final ConnectionSource mCs;
	private final DatabaseConnection mDc;

	public DatabaseUpgrader(SQLiteDatabase db, ConnectionSource cs, DatabaseConnection dc) throws SQLException
	{
		mDb = db;
		mCs = cs;
		mDc = cs.getReadWriteConnection();
	}

	public void onUpgrade(int oldVersion, int newVersion) throws SQLException
	{
		if(oldVersion < 54)
		{
			throw new DatabaseHelper.DatabaseError(DatabaseHelper.DatabaseError.E_UPGRADE,
					"Unsupported database version");
		}



		if(oldVersion < 55)
			execute("UPDATE [drugs] SET [sortRank]=" + Integer.MAX_VALUE);

		if(oldVersion < 56)
		{
			TableUtils.createTableIfNotExists(mCs, Patient.class);

			Dao<Patient, Integer> dao = DaoManager.createDao(mCs, Patient.class);
			dao.create(new Patient());

			execute("ALTER TABLE [drugs] ADD COLUMN [patient] INTEGER DEFAULT " + Patient.DEFAULT_PATIENT_ID);
		}

		if(oldVersion < 57)
			execute("ALTER TABLE [drugs] ADD COLUMN [lastScheduleUpdateDate] VARCHAR DEFAULT NULL");

		if(oldVersion < 58)
		{
			// Theoretically, we should create the [schedules] and [schedulepart] tables, but since
			// these are disabled for now, this upgrade is a no-op. We set any schedule_id
		}

		if(oldVersion < 59)
		{

		}


	}

	private int execute(String statement) throws SQLException {
		return mDc.executeStatement(statement, DatabaseConnection.DEFAULT_RESULT_FLAGS);
	}
}
