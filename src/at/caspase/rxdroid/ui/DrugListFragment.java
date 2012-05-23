package at.caspase.rxdroid.ui;

import java.util.Date;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import at.caspase.rxdroid.R;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Drug;

public class DrugListFragment extends ListFragment
{
	private static final String TAG = DrugListFragment.class.getName();

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
