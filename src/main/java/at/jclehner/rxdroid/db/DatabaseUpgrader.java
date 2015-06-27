/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2014 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. Additional terms apply (see LICENSE).
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

package at.jclehner.rxdroid.db;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.table.TableUtils;

import net.lingala.zip4j.exception.ZipException;

import java.io.Closeable;
import java.io.File;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import at.jclehner.rxdroid.Backup;

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
		try
		{
			final File f = Backup.makeBackupFilename("pre_v" + oldVersion + "to" + newVersion);
			Backup.createBackup(f, null);
			Log.i(TAG, "Created backup: " + f);
		}
		catch(ZipException e)
		{
			Log.w(TAG, e);
		}

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

		// TODO cleanup stale database columns
		// Since SQLite does not support removing columns, the upgrade process leaves stale
		// stale columns whenever a 'rename' or 'delete' happens in an entry's schema.
		// A possible workaround would be http://www.sqlite.org/faq.html#q11
	}

	@Override
	public void close() {
		mDc.closeQuietly();
	}

	private void upgradeTo(int version) throws SQLException
	{
		// Note to possibly bored future self: to upgrade from v53 (RxDroid 0.9.1, first release
		// on Google Play), the Serializable representation of all Fraction values must be
		// converted first. Format: (115 bytes, both denominator and numerator are 4 bytes)
		// ACED00057372001B61742E636173706173
		// 652E727864726F69642E4672616374696F
		// 6E1C74F7F02E65C9FC02000249000C6D44
		// 656E6F6D696E61746F7249000A6D4E756D
		// 657261746F72787200106A6176612E6C61
		// 6E672E4E756D62657286AC951D0B94E08B
		// 0200007870<DENOMINATOR><NUMERATOR>

		switch(version)
		{
			case 55:
				execute("UPDATE [drugs] SET [sortRank]=" + Integer.MAX_VALUE);
				break;

			case 56:
				// TODO this must be changed if the [patient] table is ever changed
				TableUtils.createTableIfNotExists(mCs, getEntryClass(Patient.class, 56));
				execute("INSERT INTO [patients] ( [name], [id] ) VALUES ( NULL, " + Patient.DEFAULT_PATIENT_ID + " )");
				execute("ALTER TABLE [drugs] ADD COLUMN [patient_id] INTEGER");
				execute("UPDATE [drugs] SET [patient_id]=" + Patient.DEFAULT_PATIENT_ID);

				break;

			case 57:
				execute("ALTER TABLE [drugs] ADD COLUMN [lastScheduleUpdateDate] VARCHAR DEFAULT NULL");
				break;

			case 58:
				// Theoretically, we should create the [schedules] and [schedulepart] tables, but since
				// these are disabled for now, this upgrade is a no-op.
				break;

			case 59:
				execute("DROP TABLE IF EXISTS [schedules]");
				execute("DROP TABLE IF EXISTS [schedulepart]");

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

				// Set currentSupply to zero where refillSize is zero (i.e. not set) as this
				// how things are handled since 8eaf8c2.
				execute("UPDATE [drugs] SET [currentSupply]='0' WHERE [refillSize]=0");

				break;

			case 60:
				execute("ALTER TABLE [drugs] ADD COLUMN [scheduleEndDate] VARCHAR");
				execute(
						"CREATE TRIGGER [drug_cleanup] AFTER DELETE ON [drugs] " +
						"FOR EACH ROW " +
						"BEGIN " +
						"  DELETE FROM [dose_events] WHERE drug_id = OLD.id; " +
						"END"
				);
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
