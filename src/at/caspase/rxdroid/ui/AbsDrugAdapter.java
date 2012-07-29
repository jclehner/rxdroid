package at.caspase.rxdroid.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import at.caspase.rxdroid.DoseView;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.util.CollectionUtils;
import at.caspase.rxdroid.view.Rot13TextView;

public abstract class AbsDrugAdapter extends ArrayAdapter<Drug>
{
	private final ArrayList<Drug> mAllItems;

	protected final Activity mActivity;
	protected ArrayList<Drug> mItems;
	protected final Date mAdapterDate;

	public AbsDrugAdapter(Activity activity, List<Drug> items, Date date)
	{
		super(activity.getApplicationContext(), 0, items);

		mActivity = activity;
		mAllItems = mItems = new ArrayList<Drug>(items);
		mAdapterDate = date;
	}

	public void setFilter(CollectionUtils.Filter<Drug> filter)
	{
		if(filter != null)
			mItems = (ArrayList<Drug>) CollectionUtils.filter(mAllItems, filter);
		else
			mItems = mAllItems;

		notifyDataSetChanged();
	}

	@Override
	public abstract View getView(int position, View convertView, ViewGroup parent);

	@Override
	public Drug getItem(int position) {
		return mItems.get(position);
	}

	@Override
	public int getPosition(Drug drug) {
		return mItems.indexOf(drug);
	}

	@Override
	public int getCount() {
		return mItems.size();
	}

	static class DoseViewHolder
	{
		Rot13TextView name;
		ImageView icon;
		DoseView[] doseViews = new DoseView[4];
		ImageView notification;
		TextView info1;
		TextView info2;
	}
}

