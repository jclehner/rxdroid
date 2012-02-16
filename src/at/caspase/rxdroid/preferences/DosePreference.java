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

package at.caspase.rxdroid.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import at.caspase.rxdroid.DoseView;
import at.caspase.rxdroid.R;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.util.CollectionUtils;
import at.caspase.rxdroid.util.Util;

public class DosePreference extends FractionPreference
{
	@SuppressWarnings("unused")
	private static final String TAG = DosePreference.class.getName();

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
		mDoseTime = getDoseTimeFromKey(getKey());
		setValue(mDrug.getDose(mDoseTime));
	}

	@Override
	public void setValue(Object object)
	{
		super.setValue(object);
		// FIXME
		mDoseTime = getDoseTimeFromKey(getKey());
	}

	@Override
	public CharSequence getSummary() {
		return null;
	}

	@Override
	protected void onBindView(View view)
	{
		super.onBindView(view);

		if(view != null)
		{

			setDialogIcon(Util.getDoseTimeDrawableFromDoseTime(mDoseTime));

			mDoseView = (DoseView) view.findViewById(R.id.dose_view);
			if(mDoseView == null)
				return;

			mDoseView.setDoseTime(mDoseTime);
			//mDoseView.setDrug(mDrug);
			//mDoseView.setDoseFromDrugAndDate(date, drug)
			mDoseView.setDose(getValue());
			mDoseView.setOnClickListener(mViewClickListener);
		}
	}

	public static int getDoseTimeFromKey(String key)
	{
		final String[] keys = { "morning", "noon", "evening", "night" };

		int doseTime = CollectionUtils.indexOf(key, keys);

		if(doseTime == -1)
			throw new IllegalStateException("Illegal key '" + key + "' for DosePreference. Valid keys: " + keys);

		return doseTime;
	}

	private OnClickListener mViewClickListener = new OnClickListener() {

		@Override
		public void onClick(View v)
		{
			DosePreference.this.onClick();
		}
	};
}
