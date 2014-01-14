package at.jclehner.rxdroid;

import android.app.Activity;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import at.jclehner.rxdroid.db.Patient;
import at.jclehner.rxdroid.util.Components;
import at.jclehner.rxdroid.util.DateTime;

public class DrugListActivity2 extends SherlockFragmentActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Components.onCreate(this, Components.NO_DATABASE_INIT);

		super.onCreate(savedInstanceState);

		DrugListFragment f = DrugListFragment.newInstance(DateTime.today(), Patient.DEFAULT_PATIENT_ID,
				Settings.getDoseTimeInfo());

		getSupportFragmentManager().beginTransaction().add(android.R.id.content, f).commit();
	}
}
