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

package at.jclehner.rxdroid.ui;

import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.Arrays;

import at.jclehner.androidutils.ArrayOfParcelables;
import at.jclehner.rxdroid.DoseView;
import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.FractionInputDialog;
import at.jclehner.rxdroid.FractionInputDialog.OnFractionSetListener;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Schedule;
import at.jclehner.rxdroid.util.Constants;
import at.jclehner.rxdroid.util.SimpleBitSet;
import at.jclehner.rxdroid.util.Util;

public class ScheduleGridFragment extends ListFragment implements
		OnClickListener, OnCheckedChangeListener
{
	private static final String TAG = ScheduleGridFragment.class.getSimpleName();

	private static final int NO_WEEKDAY_IDX = 0;
	private static final int NO_WEEKDAY = positionToWeekDay(NO_WEEKDAY_IDX);

	private SimpleBitSet mStates = new SimpleBitSet(0);
	private Fraction[][] mDoses = new Fraction[8][Constants.DOSE_TIMES.length];

	private int mDrugId = -1;

	@SuppressWarnings("unused")
	private Schedule mSchedule;

	@Override
	public void onCreate(Bundle icicle)
	{
		Log.d(TAG, "onCreate: icicle=" + icicle);
		super.onCreate(icicle);

		if(icicle != null)
		{
			final ArrayOfParcelables array = icicle.getParcelable("doses");
			mDoses = array.get(Fraction.CREATOR, mDoses);
			mStates.set(icicle.getLong("states"));
			mDrugId = icicle.getInt("drug_id", -1);
		}
		else
		{
			for(int i = 0; i != mDoses.length; ++i)
				mDoses[i] = new Fraction[] { Fraction.ZERO, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO };
		}

		setListAdapter(mAdapter);
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
		if(v.getTag() == null)
			return;

		final int weekDay = (Integer) v.getTag();
		if(weekDay == NO_WEEKDAY)
		{
			Log.w(TAG, "onCheckedChanged: weekDay == NO_WEEKDAY");
			return;
		}

		final int position = weekDayToPosition(weekDay);

		mStates.set(position, isChecked);

		if(!isChecked)
			mDoses[position] = mDoses[NO_WEEKDAY_IDX];

		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		Log.d(TAG, "onSaveInstanceState");

		super.onSaveInstanceState(outState);

		outState.putParcelable("doses", new ArrayOfParcelables(mDoses));
		outState.putLong("states", mStates.longValue());
		outState.putInt("drug_id", mDrugId);
	}

	private boolean areAllDaysEnabled() {
		return mStates.cardinality() == 7;
	}

	private void setDose(int weekDay, int doseTime, Fraction dose)
	{
		final int index = weekDayToPosition(weekDay);
		mDoses[index][doseTime] = dose;

		if(weekDay == NO_WEEKDAY && !areAllDaysEnabled())
		{
			for(int i = 1; i != mDoses.length; ++i)
			{
				if(!mStates.get(i))
					mDoses[i][doseTime] = dose;
			}
		}

		mAdapter.notifyDataSetChanged();
	}

	private final BaseAdapter mAdapter = new BaseAdapter() {

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
		public View getView(int position, View view, ViewGroup parent)
		{
			final ViewHolder holder;

			if(view == null)
			{
				view = LayoutInflater.from(getActivity()).inflate(R.layout.schedule_day, null, false);
				holder = new ViewHolder();

				holder.setDoseViewsAndDividersFromLayout(view);

				holder.dayContainer = (ViewGroup) view.findViewById(R.id.day_container);
				holder.dayChecked = (CheckBox) view.findViewById(R.id.day_checked);
				holder.dayName = (TextView) view.findViewById(R.id.day_name);

				holder.dayChecked.setOnCheckedChangeListener(ScheduleGridFragment.this);

				for(DoseView dv : holder.doseViews)
				{
					dv.setDose(Fraction.ZERO);
					dv.setOnClickListener(ScheduleGridFragment.this);
				}

				for(View divider : holder.dividers)
					divider.setVisibility(View.GONE);

				view.setTag(holder);
			}
			else
				holder = (ViewHolder) view.getTag();

			view.setVisibility(View.VISIBLE);

			final boolean enabled = mStates.get(position);
			final int weekDay = positionToWeekDay(position);

			for(int i = 0; i != mDoses[position].length; ++i)
			{
				holder.doseViews[i].setDose(mDoses[position][i]);
				holder.doseViews[i].setTag(weekDay);

				if(weekDay != NO_WEEKDAY)
				{
					holder.doseViews[i].setEnabled(enabled);
					holder.doseViews[i].setDoseTimeIconVisible(enabled);
				}
				else
				{
					holder.doseViews[i].setEnabled(true);
					holder.doseViews[i].setDoseTimeIconVisible(true);
				}
			}

			holder.dayChecked.setTag(null);
			holder.dayChecked.setChecked(enabled);
			holder.dayChecked.setTag(weekDay);

			if(weekDay != NO_WEEKDAY)
			{
				holder.dayContainer.setVisibility(View.VISIBLE);
				holder.dayName.setText(Constants.SHORT_WEEK_DAY_NAMES[weekDay]);
			}
			else
			{
				holder.dayContainer.setVisibility(View.INVISIBLE);

				if(areAllDaysEnabled())
					view.setVisibility(View.GONE);
			}

			return view;
		}
	};

	private static int positionToWeekDay(int position) {
		return position - 1;
	}

	private static int weekDayToPosition(int weekDay) {
		return weekDay + 1;
	}
}

class ViewHolder extends ScheduleViewHolder
{
	ViewGroup dayContainer;
	CheckBox dayChecked;
	TextView dayName;
}