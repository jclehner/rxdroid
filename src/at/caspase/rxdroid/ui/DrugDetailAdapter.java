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
import at.caspase.rxdroid.db.Intake;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.Timer;
import at.caspase.rxdroid.view.Rot13TextView;

public class DrugDetailAdapter extends AbsDrugAdapter
{
	private final String TAG = DrugDetailAdapter.class.getName();
	private final boolean LOGV = true;

	private final int mDoseTime;

	private final Timer mTimer;

	public DrugDetailAdapter(Activity activity, List<Drug> items, Date date, int doseTime) {
		super(activity, items, date);

		mDoseTime = doseTime;

		if(LOGV)
		{
			mTimer = new Timer();
			Log.v(TAG, "<init>: activity=" + activity + ", date=" + date + ", doseTime=" + doseTime);
			Log.v(TAG, "  mActivity=" + mActivity + ", mDate=" + mAdapterDate);
		}
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
			v = mActivity.getLayoutInflater().inflate(R.layout.drug_view_detail, null);

			holder = new DoseViewHolder();

			holder.name = (Rot13TextView) v.findViewById(R.id.drug_name);
			holder.icon = (ImageView) v.findViewById(R.id.drug_icon);
			//holder.notification = (ImageView) v.findViewById(R.id.drug_notification_icon);
			//holder.notification.setTag(drug);
			holder.info1 = (TextView) v.findViewById(R.id.text_info1);
			holder.info2 = (TextView) v.findViewById(R.id.text_info2);

			holder.doseViews[0] = (DoseView) v.findViewById(R.id.dose_view);
			mActivity.registerForContextMenu(holder.doseViews[0]);

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

		//holder.notification.setVisibility(visibility);

		final DoseView doseView = holder.doseViews[0];
		doseView.setDoseTime(mDoseTime);
		if(!doseView.hasInfo(mAdapterDate, drug))
			doseView.setDoseFromDrugAndDate(mAdapterDate, drug);

		// TODO fill them with meaningful info
		holder.info1.setVisibility(View.GONE);
		holder.info2.setVisibility(View.GONE);

		//// FIXME hack
		if(Entries.countIntakes(drug, mAdapterDate, mDoseTime) != 0)
		{
			final Intake intake = Entries.findIntakes(drug, mAdapterDate, mDoseTime).get(0);

			holder.info1.setText("Taken: " + intake.getTimestamp());
			holder.info1.setVisibility(View.VISIBLE);
		}
		////////////////////////////

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
