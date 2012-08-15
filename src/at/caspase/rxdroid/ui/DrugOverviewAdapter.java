/**
 * Copyright (C) 2011, 2012 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 * This file is part of RxDroid.
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

package at.caspase.rxdroid.ui;

import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import at.caspase.rxdroid.DoseView;
import at.caspase.rxdroid.DrugListActivity;
import at.caspase.rxdroid.R;
import at.caspase.rxdroid.Settings;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Entries;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.Timer;
import at.caspase.rxdroid.widget.Rot13TextView;

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

			holder.name = (Rot13TextView) v.findViewById(R.id.drug_name);
			holder.icon = (ImageView) v.findViewById(R.id.drug_icon);
			holder.missedDoseIndicator = (ViewStub) v.findViewById(R.id.missed_dose_indicator);
			holder.lowSupplyIndicator = (ViewStub) v.findViewById(R.id.low_supply_indicator);

			//holder.missedDoseIndicator.setTag(drug);
			//holder.lowSupplyIndicator.setTag(drug);

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

		//holder.name.setScramblingEnabled(Settings.inst)
		//holder.name.setScramblingEnabled(true);
		holder.name.setText(drug.getName());
		holder.name.setScramblingEnabled(Settings.instance().getBoolean("privacy_scramble_names", false));
		holder.name.setTag(DrugListActivity.TAG_DRUG_ID, drug.getId());

		holder.icon.setImageResource(drug.getFormResourceId());

		if(DateTime.todayDate().equals(mAdapterDate))
		{
			if(Entries.hasMissingIntakesBeforeDate(drug, mAdapterDate))
				holder.missedDoseIndicator.inflate().setTag(drug);
			else
				holder.missedDoseIndicator.setVisibility(View.GONE);

			if(Settings.instance().hasLowSupplies(drug))
				holder.lowSupplyIndicator.inflate().setTag(drug);
			else
				holder.lowSupplyIndicator.setVisibility(View.GONE);
		}

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

			Log.v(TAG, mAdapterDate + ": " + viewCount + " views created in " + elapsed + "s (" + timePerView + "s per view)");
		}

		return v;
	}
}
