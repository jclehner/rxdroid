package at.jclehner.rxdroid.db;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.table.TableUtils;

import java.io.Closeable;
import java.sql.SQLException;

public class DatabaseUpgrader implements Closeable
{
	private static final String TAG = DatabaseUpgrader.class.getSimpleName();

	private final SQLiteDatabase mDb;
	private final ConnectionSource mCs;
	private final DatabaseConnection mDc;

	public DatabaseUpgrader(SQLiteDatabase db, ConnectionSource cs) throws SQLException
	{
		mDb = db;
		mCs = cs;
		mDc = cs.getReadWriteConnection();
	}

	public void onUpgrade(int oldVersion, int newVersion) throws SQLException
	{
		for(int version = oldVersion + 1; version <= newVersion; ++version)
		{
			try
			{
				Log.i(TAG, "Upgrading to v" + version);
				upgradeTo(version);
				mDb.setVersion(version);
			}
			catch(SQLException e)
			{
				mDb.setVersion(version - 1);
				throw new DatabaseHelper.DatabaseError(DatabaseHelper.DatabaseError.E_UPGRADE, e);
			}
		}
	}

	@Override
	public void close() {
		mDc.closeQuietly();
	}

	private void upgradeTo(int version) throws SQLException
	{
		switch(version)
		{
			case 55:
				execute("UPDATE [drugs] SET [sortRank]=" + Integer.MAX_VALUE);
				break;

			case 56:
				// TODO this must be changed if the [patient] table is ever changed
				TableUtils.createTableIfNotExists(mCs, getEntryClass(Patient.class, 56));
				execute("INSERT INTO [patients] ( [name], [id] ) VALUES ( NULL, " + Patient.DEFAULT_PATIENT_ID);
				break;

			case 57:
				execute("ALTER TABLE [drugs] ADD COLUMN [lastScheduleUpdateDate] VARCHAR DEFAULT NULL");
				break;

			case 58:
				// Theoretically, we should create the [schedules] and [schedulepart] tables, but since
				// these are disabled for now, this upgrade is a no-op.
				break;

			case 59:
				execute("DROP TABLE [schedules]");
				execute("DROP TABLE [schedulepart]");

				execute("ALTER TABLE [intake] RENAME TO [dose_events]");
				// Rename autoAddIntakes -> hasAutoDoseEvents
				execute("ALTER TABLE [drugs] ADD COLUMN [hasAutoDoseEvents] SMALLINT");
				execute("UPDATE [drugs] SET [hasAutoDoseEvents]=[autoAddIntakes]");
				// Rename lastAutoIntakeCreationDate -> lastAutoDoseEventCreationDate
				execute("ALTER TABLE [drugs] ADD COLUMN [lastAutoDoseEventCreationDate] VARCHAR");
				execute("UPDATE [drugs] SET [lastAutoDoseEventCreationDate]=[lastAutoIntakeCreationDate]");

				execute("ALTER TABLE [drugs] ADD COLUMN [expirationDate] VARCHAR");

				// Separate asNeeded from repeatMode. All drugs with REPEAT_AS_NEEDED (3) will be
				// switched to REPEAT_DAILY (0). Also, decrement all repeatModes >= REPEAT_21_7 (4)
				// to prevent a gap in the REPEAT_* indexes.

				execute("ALTER TABLE [drugs] ADD COLUMN [asNeeded] SMALLINT DEFAULT 0");
				execute("UPDATE [drugs] SET [asNeeded]=1, [repeatMode]=0 WHERE [repeatMode]=3");
				execute("UPDATE [drugs] SET [repeatMode]=[repeatMode]-1 WHERE [repeatMode]>=4");

				break;

			default:
				throw new DatabaseHelper.DatabaseError(DatabaseHelper.DatabaseError.E_UPGRADE,
						"Unsupported database version " + version);
		}
	}

	private int execute(String statement) throws SQLException {
		return mDc.executeStatement(statement, DatabaseConnection.DEFAULT_RESULT_FLAGS);
	}

	private static Class<?> getEntryClass(Class<? extends Entry> clazz, int dbVersion)
	{
		try
		{
			final String pkgName = Database.class.getPackage().getName() + ".v" + dbVersion;
			return Class.forName(pkgName + ".Old" + clazz.getSimpleName());
		}
		catch(ClassNotFoundException e)
		{
			return clazz;
		}
	}
}
