package at.jclehner.rxdroid;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import at.jclehner.rxdroid.db.Patient;
import at.jclehner.rxdroid.util.Components;
import at.jclehner.rxdroid.util.DateTime;

public class DrugListActivity2 extends SherlockFragmentActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Components.onCreateActivity(this, Components.NO_DATABASE_INIT);

		super.onCreate(savedInstanceState);

		final DrugListPagerFragment f = new DrugListPagerFragment();
		final Bundle args = new Bundle();
		args.putSerializable(DrugListFragment.ARG_DATE, DateTime.today());
		args.putInt(DrugListFragment.ARG_PATIENT_ID, Patient.DEFAULT_PATIENT_ID);
		f.setArguments(args);

		final FragmentManager fm = getSupportFragmentManager();

		if(!(fm.findFragmentById(android.R.id.content) instanceof DrugListPagerFragment))
			fm.beginTransaction().add(android.R.id.content, f).commit();
	}
}
