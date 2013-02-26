/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2013 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
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

package at.jclehner.rxdroid.ui;

import java.util.Date;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.Drug;

public class DrugListFragment extends ListFragment
{
	private static final String TAG = DrugListFragment.class.getSimpleName();

	//public static final String ARG_ADAPTER = "adapter";
	public static final String ARG_DATE = "date";
	public static final String ARG_DOSE_TIME = "dose_time";

	public DrugListFragment() {
		Log.d(TAG, "DrugListFragment created");
	}

	@Override
	public void onResume()
	{
		super.onResume();

		final Bundle args = getArguments();

		final Date date = (Date) args.get(ARG_DATE);
		final int doseTime = args.getInt(ARG_DOSE_TIME, Drug.TIME_INVALID);

		//setListAdapter((ListAdapter) args.get(ARG_ADAPTER));

		AbsDrugAdapter adapter = new DrugDetailAdapter(getActivity(), Database.getAll(Drug.class), date, doseTime);
		setListAdapter(adapter);

		Log.d(TAG, "onResume: getListAdapter()=" + getListAdapter());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.drug_list_fragment, container, false);
	}
}
