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

package at.jclehner.rxdroid;

import java.util.BitSet;

import android.app.ListActivity;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import at.jclehner.rxdroid.FractionInputDialog.OnFractionSetListener;
import at.jclehner.rxdroid.db.Schedule;
import at.jclehner.rxdroid.ui.ScheduleViewHolder;
import at.jclehner.rxdroid.util.Components;
import at.jclehner.rxdroid.util.Constants;
import at.jclehner.rxdroid.util.Util;


public class AdvancedScheduleActivity extends ListActivity
		implements OnCheckedChangeListener
{
	private static final String TAG = AdvancedScheduleActivity.class.getSimpleName();
	private static final int WEEKDAY_NONE = -1;

	private ViewHolder[] mHolders = new ViewHolder[8];

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Components.onCreateActivity(this, 0);
		super.onCreate(savedInstanceState);

		for(int i = 0; i != mHolders.length; ++i)
			mHolders[i] = new ViewHolder();

		setListAdapter(mAdapter);
	}

	public void onDoseViewClick(View v)
	{
		final DoseView dv = (DoseView) v;
		final int weekDay = (Integer) dv.getTag();
		final int doseTime = dv.getDoseTime();

		FractionInputDialog d = new FractionInputDialog(this, dv.getDose(),
				new OnFractionSetListener() {

			@Override
			public void onFractionSet(FractionInputDialog dialog, Fraction value)
			{
				setDose(weekDay, doseTime, value);
			}
		});

		final String title;
		if(weekDay == WEEKDAY_NONE)
			title = Util.getDoseTimeName(doseTime);
		else
			title = Constants.LONG_WEEK_DAY_NAMES[weekDay];

		d.setTitle(title);
		d.setIcon(Util.getDoseTimeDrawableFromDoseTime(doseTime));
		d.setAutoInputModeEnabled(true);

		d.show();
	}

	@Override
	public void onCheckedChanged(CompoundButton v, boolean isChecked)
	{
		int weekDay = (Integer) v.getTag();
		setDayEnabled(weekDay, isChecked);
	}

	private void setDayEnabled(int weekDay, boolean enabled)
	{
		Log.d(TAG, "setDayEnabled: weekDay=" + weekDay + ", enabled=" + enabled);

		for(DoseView dv : mHolders[1+weekDay].doseViews)
		{
			dv.setEnabled(enabled);

			if(!enabled)
			{
				// apply the values from the default schedule
				dv.setDose(mHolders[0].doseViews[dv.getDoseTime()].getDose());
			}
		}
	}

	private void setDose(int weekDay, int doseTime, Fraction dose)
	{
		final int pos = 1 + weekDay;

		mHolders[pos].setDose(doseTime, dose, true);

		if(weekDay == WEEKDAY_NONE)
		{
			for(int i = 0; i != mHolders.length; ++i)
			{
				if(i != pos)
					mHolders[i].setDose(doseTime, dose, false);
			}
		}
	}

	private final ListAdapter mAdapter = new ListAdapter() {

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {}

		@Override
		public void registerDataSetObserver(DataSetObserver observer) {}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			Log.d(TAG, "getView: position=" + position);

			ViewHolder holder = mHolders[position];

			if(holder.view == null)
			{
				//holder = mHolders[position] = new ViewHolder();

				holder.view = LayoutInflater.from(getBaseContext()).inflate(R.layout.schedule_day, null);
				holder.setDoseViewsAndDividersFromLayout(holder.view);

				holder.dayContainer = (ViewGroup) holder.view.findViewById(R.id.day_container);
				holder.dayChecked = (CheckBox) holder.view.findViewById(R.id.day_checked);
				holder.dayName = (TextView) holder.view.findViewById(R.id.day_name);

				holder.dayChecked.setOnCheckedChangeListener(AdvancedScheduleActivity.this);


				for(View divider : holder.dividers)
					divider.setVisibility(View.INVISIBLE);

				boolean doseViewEnabled;
				int dayTag;

				if(position == 0)
				{
					holder.dayContainer.setVisibility(View.INVISIBLE);
					dayTag = WEEKDAY_NONE;
					doseViewEnabled = true;
				}
				else
				{
					int weekDay = position - 1;

					holder.dayContainer.setVisibility(View.VISIBLE);
					holder.dayName.setText(Constants.SHORT_WEEK_DAY_NAMES[weekDay]);
					dayTag = weekDay;
					doseViewEnabled = false;
				}

				for(DoseView dv : holder.doseViews)
				{
					//dv.setDose(holder.getDose(dv.getDoseTime()));
					dv.setEnabled(doseViewEnabled);
					dv.setTag(dayTag);
					dv.setDose(Fraction.ZERO);
				}

				holder.dayChecked.setTag(dayTag);
			}

			return holder.view;
		}

		@Override
		public int getItemViewType(int position) {
			return 0;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public int getCount()
		{
			// 7 weekdays plus one
			return 8;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}

		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}
	};

	static class ViewHolder extends ScheduleViewHolder
	{
		Fraction[] doses = new Fraction[Schedule.DOSE_TIME_COUNT];

		View view;
		ViewGroup dayContainer;
		CheckBox dayChecked;
		TextView dayName;

		void setDose(int doseTime, Fraction dose, boolean setIfDoseViewIsEnabled)
		{
			final DoseView dv = doseViews[doseTime];

			if(dv == null)
				doses[doseTime] = dose;
			else
			{
				if(!dv.isEnabled() || setIfDoseViewIsEnabled)
					dv.setDose(dose);
			}
		}

		Fraction getDose(int doseTime, boolean returnNullIfDoseViewIsDisabled)
		{
			final DoseView dv = doseViews[doseTime];

			if(dv == null)
			{
				Fraction dose = doses[doseTime];
				if(dose == null)
					return Fraction.ZERO;

				return dose;
			}
			else
			{
				try
				{
					if(!dv.isEnabled() && returnNullIfDoseViewIsDisabled)
						return null;

					return dv.getDose();
				}
				catch(Exception e)
				{
					return Fraction.ZERO;
				}
			}
		}
	}
}

class ScheduleGrid extends ListView
		implements ListAdapter
{
	static class ViewHolder extends ScheduleViewHolder
	{
		ViewGroup dayContainer;
		CheckBox dayChecked;
		TextView dayName;
	}

	static final int NO_WEEKDAY = -1;

	private View[] mViews = new View[8];
	private BitSet mEnabled = new BitSet(7);

	public ScheduleGrid(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		for(int i = 0; i != mViews.length; ++i)
		{
			final View v = mViews[i] = LayoutInflater.from(context).inflate(R.layout.schedule_day, null);

			ViewHolder holder = new ViewHolder();
			holder.setDoseViewsAndDividersFromLayout(v);

			holder.dayContainer = (ViewGroup) v.findViewById(R.id.day_container);
			holder.dayChecked = (CheckBox) v.findViewById(R.id.day_checked);
			holder.dayName = (TextView) v.findViewById(R.id.day_name);

			if(i != 0)
				holder.dayName.setText(Constants.SHORT_WEEK_DAY_NAMES[i - 1]);

			v.setTag(holder);
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View v = mViews[position];
		final ViewHolder holder = (ViewHolder) v.getTag();

		final int weekDay = position - 1;

		if(weekDay != NO_WEEKDAY)
		{
			final boolean enabled = mEnabled.get(weekDay);
			holder.dayChecked.setChecked(enabled);
			holder.doseContainer.setEnabled(enabled);
		}
		else
		{
			holder.doseContainer.setEnabled(mEnabled.cardinality() < 7);
		}

		return v;
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {}

	@Override
	public int getCount() {
		return mViews.length;
	}

	@Override
	public Object getItem(int position) {
		return mViews[position];
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
}





