package at.caspase.rxdroid.ui;

import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import at.caspase.rxdroid.DoseView;
import at.caspase.rxdroid.DrugListActivity;
import at.caspase.rxdroid.R;
import at.caspase.rxdroid.Settings;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Entries;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.Timer;

public class DrugOverviewAdapter extends AbsDrugAdapter
{
	private static final String TAG = DrugOverviewAdapter.class.getName();
	private static final boolean LOGV = true;

	private final Timer mTimer;

	public DrugOverviewAdapter(Activity activity, List<Drug> items, Date date)
	{
		super(activity, items, date);

		if(LOGV)
			mTimer = new Timer();
	}

	@Override
	public View getView(int position, View v, ViewGroup parent)
	{
		if(LOGV && position == 0)
			mTimer.restart();

		final Drug drug = getItem(position);
		final DoseViewHolder holder;

		if(v == null)
		{
			v = mActivity.getLayoutInflater().inflate(R.layout.drug_view2, null);

			holder = new DoseViewHolder();

			holder.name = (TextView) v.findViewById(R.id.drug_name);
			holder.icon = (ImageView) v.findViewById(R.id.drug_icon);
			holder.notification = (ImageView) v.findViewById(R.id.drug_notification_icon);
			holder.notification.setTag(drug);

			for(int i = 0; i != holder.doseViews.length; ++i)
			{
				final int doseViewId = Constants.DOSE_VIEW_IDS[i];
				holder.doseViews[i] = (DoseView) v.findViewById(doseViewId);
				mActivity.registerForContextMenu(holder.doseViews[i]);
			}

			v.setTag(holder);
		}
		else
			holder = (DoseViewHolder) v.getTag();

		holder.name.setText(Settings.instance().getDrugName(drug));
		holder.name.setTag(DrugListActivity.TAG_DRUG_ID, drug.getId());
		holder.icon.setImageResource(drug.getFormResourceId());

		final int visibility;

		if(DateTime.todayDate().equals(mAdapterDate) && Entries.hasMissingIntakesBeforeDate(drug, mAdapterDate))
			visibility = View.VISIBLE;
		else
			visibility = View.GONE;

		holder.notification.setVisibility(visibility);

		for(DoseView doseView : holder.doseViews)
		{
			if(!doseView.hasInfo(mAdapterDate, drug))
				doseView.setDoseFromDrugAndDate(mAdapterDate, drug);
		}

		if(LOGV && position == getCount() - 1)
		{
			final double elapsed = mTimer.elapsedSeconds();
			final int viewCount = getCount() * 4;
			final double timePerView = elapsed / viewCount;

			Log.v(TAG, viewCount + " views created in " + elapsed + "s (" + timePerView + "s per view)");
		}

		return v;
	}
}
