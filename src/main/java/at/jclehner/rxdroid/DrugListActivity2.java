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

package at.jclehner.rxdroid;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

import at.jclehner.rxdroid.db.Patient;
import at.jclehner.rxdroid.util.Components;
import at.jclehner.rxdroid.util.DateTime;

public class DrugListActivity2 extends ActionBarActivity
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

	public void onDrugSupplyMonitorClick(View view) {
		throw new UnsupportedOperationException();
	}

	public void onDrugSupplyMonitorLongClick(View view) {
		throw new UnsupportedOperationException();
	}
}
