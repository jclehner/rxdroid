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

package at.jclehner.rxdroid.ui;

import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import at.jclehner.rxdroid.BuildConfig;
import at.jclehner.rxdroid.DoseView;
import at.jclehner.rxdroid.DrugListActivity;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.Settings;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entries;
import at.jclehner.rxdroid.util.Constants;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Timer;
import at.jclehner.rxdroid.util.Util;
import at.jclehner.rxdroid.widget.DrugNameView;
import at.jclehner.rxdroid.widget.DrugSupplyMonitor;

public class DrugOverviewAdapter extends AbsDrugAdapter
{
	private static final String TAG = DrugOverviewAdapter.class.getSimpleName();
	private static final boolean LOGV = BuildConfig.DEBUG;

	private final Timer mTimer;

	public DrugOverviewAdapter(Activity activity, List<Drug> items, Date date)
	{
		super(activity, items, date);

		mTimer = LOGV ? new Timer() : null;
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
			v = mActivity.getLayoutInflater().inflate(R.layout.drug_view, null);

			holder = new DoseViewHolder();

			holder.name = (DrugNameView) v.findViewById(R.id.drug_name);
			holder.icon = (ImageView) v.findViewById(R.id.drug_icon);
			holder.missedDoseIndicator = v.findViewById(R.id.missed_dose_indicator);
//			holder.lowSupplyIndicator = v.findViewById(R.id.low_supply_indicator);
			holder.currentSupply = (DrugSupplyMonitor) v.findViewById(R.id.text_supply);

			for(int i = 0; i != holder.doseViews.length; ++i)
			{
				final int doseViewId = Constants.DOSE_VIEW_IDS[i];
				holder.doseViews[i] = (DoseView) v.findViewById(doseViewId);
				mActivity.registerForContextMenu(holder.doseViews[i]);
			}

			final int[] dividerIds = { R.id.divider1, R.id.divider2, R.id.divider3 /*, R.id.divider4*/ };
			for(int i = 0; i != holder.dividers.length; ++i)
				holder.dividers[i] = v.findViewById(dividerIds[i]);

			v.setTag(holder);
		}
		else
			holder = (DoseViewHolder) v.getTag();

		//holder.name.setUnscrambledText(drug.getName());
		holder.name.setDrug(drug);
		holder.name.setTag(DrugListActivity.TAG_DRUG_ID, drug.getId());

		//holder.icon.setImageResource(drug.getIconResourceId());
		holder.icon.setImageResource(Util.getDrugIconDrawable(getContext(), drug.getIcon()));
		holder.currentSupply.setDrugAndDate(drug, mAdapterDate);

		final Date today = DateTime.today();
		boolean isCurrentSupplyVisible;

		if(today.equals(mAdapterDate))
		{
			if(Entries.hasMissingIntakesBeforeDate(drug, mAdapterDate))
			{
				if(holder.missedDoseIndicator instanceof ViewStub)
					holder.missedDoseIndicator = ((ViewStub) holder.missedDoseIndicator).inflate();

				holder.missedDoseIndicator.setTag(drug);
				holder.missedDoseIndicator.setVisibility(View.VISIBLE);
			}
			else
				holder.missedDoseIndicator.setVisibility(View.GONE);


			if(Settings.hasLowSupplies(drug))
				holder.currentSupply.setTypeface(null, Typeface.BOLD_ITALIC);
			else
				holder.currentSupply.setTextAppearance(mActivity, android.R.style.TextAppearance_Small);

			isCurrentSupplyVisible = drug.getRefillSize() != 0 || !drug.getCurrentSupply().isZero();
		}
		else
			isCurrentSupplyVisible = mAdapterDate.after(today);

		holder.currentSupply.setVisibility(isCurrentSupplyVisible ? View.VISIBLE : View.INVISIBLE);

		for(DoseView doseView : holder.doseViews)
		{
			if(!doseView.hasInfo(mAdapterDate, drug))
				doseView.setDoseFromDrugAndDate(mAdapterDate, drug);
		}

		final int dividerVisibility;
		if(v.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
			dividerVisibility = View.GONE;
		else
			dividerVisibility = View.VISIBLE;

		for(int i = 0; i != holder.dividers.length; ++i)
		{
			final View divider = holder.dividers[i];
			if(divider != null)
				divider.setVisibility(dividerVisibility);
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
