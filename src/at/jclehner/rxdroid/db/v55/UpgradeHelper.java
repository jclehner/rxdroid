package at.jclehner.rxdroid.db.v55;

import java.sql.SQLException;

import at.jclehner.rxdroid.db.Patient;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public final class UpgradeHelper
{
	public static void upgradeDatabase(ConnectionSource cs) throws SQLException
	{
		TableUtils.createTableIfNotExists(cs, Patient.class);

		Dao<Patient, Integer> dao = DaoManager.createDao(cs, Patient.class);
		dao.create(new Patient());
	}

	private UpgradeHelper() {}
}
