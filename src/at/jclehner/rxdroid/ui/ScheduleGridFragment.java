package at.jclehner.rxdroid.ui;

import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListAdapter;
import android.widget.TextView;
import at.jclehner.rxdroid.DoseView;
import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.FractionInputDialog;
import at.jclehner.rxdroid.FractionInputDialog.OnFractionSetListener;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.db.Schedule;
import at.jclehner.rxdroid.util.Constants;
import at.jclehner.rxdroid.util.SimpleBitSet;
import at.jclehner.rxdroid.util.Util;

public class ScheduleGridFragment extends ListFragment implements
		ListAdapter, OnClickListener, OnCheckedChangeListener
{
	private static final String TAG = ScheduleGridFragment.class.getSimpleName();

	private static final int NO_WEEKDAY = -1;

	private ViewHolder[] mHolders = new ViewHolder[8];
	// private View[] mViews = new View[8];
	private SimpleBitSet mDayStatus = new SimpleBitSet(0);

	private Schedule mSchedule;

	@Override
	public void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		//mSchedule = Database.get(Schedule.class, getArguments().getInt("drug_id"));

		for(int i = 0; i != mHolders.length; ++i)
		{
			final ViewHolder holder = mHolders[i] = new ViewHolder();
			final View v = mHolders[i].view = getLayoutInflater(icicle).inflate(R.layout.schedule_day, null);

			holder.fillDoseViewsAndDividers(v);

			holder.dayContainer = (ViewGroup) v.findViewById(R.id.day_container);
			holder.dayChecked = (CheckBox) v.findViewById(R.id.day_checked);
			holder.dayName = (TextView) v.findViewById(R.id.day_name);

			holder.dayChecked.setOnCheckedChangeListener(this);

			if(i != 0)
				holder.dayName.setText(Constants.SHORT_WEEK_DAY_NAMES[i - 1]);

			holder.dayContainer.setVisibility(i == 0 ? View.INVISIBLE : View.VISIBLE);

			final int weekDay = i - 1;

			for(DoseView dv : holder.doseViews)
			{
				dv.setDose(Fraction.ZERO);
				dv.setOnClickListener(this);
				dv.setTag(weekDay);
			}

			for(View divider : holder.dividers)
				divider.setVisibility(View.GONE);

			holder.dayChecked.setTag(weekDay);
		}

		onRestoreInstanceState(icicle);

		setListAdapter(this);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		final ViewHolder holder = mHolders[position];

		final int weekDay = position - 1;

		if(weekDay != NO_WEEKDAY)
		{
			final boolean enabled = mDayStatus.get(weekDay);
			holder.dayChecked.setChecked(enabled);
			//holder.doseContainer.setEnabled(enabled);
			setDoseViewsEnabled(weekDay, enabled);
		}
		else
		{
			//holder.doseContainer.setEnabled(mEnabled.cardinality() < 7);
			setDoseViewsEnabled(NO_WEEKDAY, !areAllDaysEnabled());
		}

		return holder.view;
	}

	@Override
	public void onClick(View v)
	{
		if(v instanceof DoseView)
		{
			final DoseView dv = (DoseView) v;
			final int weekDay = (Integer) dv.getTag();
			final int doseTime = dv.getDoseTime();

			// FIXME change to fragment
			final FractionInputDialog d = new FractionInputDialog(getActivity(), dv.getDose(), new OnFractionSetListener() {

				@Override
				public void onFractionSet(FractionInputDialog dialog, Fraction value)
				{
					setDose(weekDay, doseTime, value);
				}
			});

			final String title;
			if(weekDay == NO_WEEKDAY)
				title = Util.getDoseTimeName(doseTime);
			else
				title = Constants.LONG_WEEK_DAY_NAMES[weekDay];

			d.setTitle(title);
			d.setIcon(Util.getDoseTimeDrawableFromDoseTime(doseTime));
			d.setAutoInputModeEnabled(true);

			d.show();
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton v, boolean isChecked)
	{
		final int weekDay = (Integer) v.getTag();

		if(weekDay == NO_WEEKDAY)
		{
			Log.w(TAG, "onCheckedChanged: weekDay == NO_WEEKDAY");
			return;
		}

		setWeekDayEnabled(weekDay, isChecked);
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);

		//final Fraction[][] doses = new Fraction[mHolders.length][4];

		for(int i = 0; i != mHolders.length; ++i)
		{
			final Fraction[] doses = new Fraction[4];

			for(int j = 0; j != 4; ++j)
				doses[j] = mHolders[i].doseViews[j].getDose();

			outState.putParcelableArray("doses_" + i, doses);
		}

		outState.putLong("day_status", mDayStatus.longValue());
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {}

	@Override
	public int getCount() {
		return 8;
	}

	@Override
	public Object getItem(int position) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEnabled(int position) {
		return true;
	}

	private void onRestoreInstanceState(Bundle state)
	{
		if(state == null)
			return;

		for(int i = 0; i != mHolders.length; ++i)
		{
			Fraction[] doses = (Fraction[]) state.getParcelableArray("doses_" + i);
			if(doses != null)
			{
				for(int j = 0; j != doses.length; ++j)
					mHolders[i].doseViews[j].setDose(doses[j]);
			}
		}

		mDayStatus.set(state.getLong("day_status"));
	}

	private boolean areAllDaysEnabled()
	{
		return mDayStatus.cardinality() == 7;
	}

	private void setDose(int weekDay, int doseTime, Fraction dose)
	{
		final ViewHolder holder = mHolders[weekDay + 1];
		holder.doseViews[doseTime].setDose(dose);

		if(weekDay == NO_WEEKDAY && !areAllDaysEnabled())
		{
			for(int i = 1; i != mHolders.length; ++i)
			{
				final DoseView dv = mHolders[i].doseViews[doseTime];
				if(!dv.isEnabled())
					dv.setDose(dose);
			}
		}
	}

	private void setDoseViewsEnabled(int weekDay, boolean enabled)
	{
		Log.d(TAG, "setDoseViewsEnabled(" + weekDay + ", " + enabled + ")");

		final ViewHolder holder = mHolders[weekDay + 1];
		for(DoseView dv : holder.doseViews)
			dv.setEnabled(enabled);
	}

	private void setWeekDayEnabled(int weekDay, boolean enabled)
	{
		Log.d(TAG, "setWeekDayEnabled(" + weekDay + ", " + enabled + ")");

		final ViewHolder holder = mHolders[weekDay + 1];
		setDoseViewsEnabled(weekDay, enabled);
		mDayStatus.set(weekDay, enabled);

		if(!enabled)
		{
			// restore the DoseView's dose to the default value
			for(DoseView dv : holder.doseViews)
			{
				final int doseTime = dv.getDoseTime();
				dv.setDose(mHolders[0].doseViews[doseTime].getDose());
			}
		}

		setDoseViewsEnabled(NO_WEEKDAY, !areAllDaysEnabled());
	}
}

class ViewHolder extends ScheduleViewHolder
{
	View view;
	ViewGroup dayContainer;
	CheckBox dayChecked;
	TextView dayName;
}