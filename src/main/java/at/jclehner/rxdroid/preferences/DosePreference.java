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

package at.jclehner.rxdroid.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import at.jclehner.rxdroid.DoseView;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.util.CollectionUtils;
import at.jclehner.rxdroid.util.Util;

public class DosePreference extends FractionPreference
{
	private static final String TAG = DosePreference.class.getSimpleName();

	private int mDoseTime = -1;
	private Drug mDrug;
	private DoseView mDoseView;

	public DosePreference(Context context) {
		this(context, null);
	}

	public DosePreference(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.preferenceStyle);
	}

	public DosePreference(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

		setDialogTitle(getTitle());
		setWidgetLayoutResource(R.layout.dose_preference);
	}

	/**
	 * Set the drug to be used by this preference.
	 *
	 * @param drug
	 */
	public void setDrug(Drug drug)
	{
		mDrug = drug;
		setValue(mDrug.getDose(mDoseTime));
	}

	@Override
	public CharSequence getSummary() {
		return null;
	}

	@Override
	public void setKey(String key)
	{
		super.setKey(key);

		mDoseTime = getDoseTimeFromKey(key);
		setDialogIcon(Util.getDoseTimeDrawableFromDoseTime(mDoseTime));
	}

	@Override
	protected void onBindView(View view)
	{
		super.onBindView(view);

		if(view != null)
		{
			//setDialogIcon(Util.getDoseTimeDrawableFromDoseTime(mDoseTime));

			mDoseView = (DoseView) view.findViewById(R.id.dose_view);
			if(mDoseView == null)
				return;

			if(mDoseTime != -1)
				mDoseView.setDoseTime(mDoseTime);
			else
				Log.w(TAG, "onBindView: mDoseTime=-1");

			//mDoseView.setDoseTime(mDoseTime);
			//mDoseView.setDrug(mDrug);
			//mDoseView.setDoseFromDrugAndDate(date, drug)
			mDoseView.setDose(getValue());
			//mDoseView.setOnClickListener(mViewClickListener);
		}
	}

	public static int getDoseTimeFromKey(String key)
	{
		final String[] keys = { "morning", "noon", "evening", "night" };

		int doseTime = CollectionUtils.indexOf(key, keys);

		if(doseTime == -1)
			throw new IllegalStateException("Illegal key '" + key + "' for DosePreference");

		return doseTime;
	}
}
