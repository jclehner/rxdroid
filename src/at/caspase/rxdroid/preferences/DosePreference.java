/**
 * Copyright (C) 2011 Joseph Lehner <joseph.c.lehner@gmail.com>
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
import at.caspase.rxdroid.Database.Drug;

public class DosePreference extends FractionPreference implements OnClickListener
{
	@SuppressWarnings("unused")
	private static final String TAG = DosePreference.class.getName();
	
	private int mDoseTime = -1;
	private Drug mDrug;
	private DoseView mDoseView;
	
	public DosePreference(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		
		mDoseTime = getDoseTimeFromKey(getKey());
		
		//final int[] doseTimeIcons = { R.drawable.ic_morning, R.drawable.ic_noon, R.drawable.ic_evening, R.drawable.ic_night };
		//setDialogIcon(doseTimeIcons[mDoseTime]);
		setDialogIcon(android.R.drawable.ic_dialog_dialer);
		setDialogTitle(getTitle());
				
		setWidgetLayoutResource(R.layout.dose_preference);
	} 
	
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
	public void onClick(View v) {
		onPreferenceClick(this);		
	}
	
	public static int getDoseTimeFromKey(String key)
	{
		final String[] keys = { "morning", "noon", "evening", "night" };
		//mDoseTime = Arrays.binarySearch(keys, 0, keys.length, getKey());
		
		int doseTime = -1;
		
		for(int i = 0; i != keys.length; ++i)
		{
			if(keys[i].equals(key))
			{
				doseTime = i;
				break;
			}
		}
				
		if(doseTime == -1)
			throw new IllegalStateException("Illegal key '" + key + "' for DosePreference. Valid keys: morning, noon, evening, night");
		
		return doseTime;		
	}
	
	@Override
	protected void onBindView(View view)
	{
		super.onBindView(view);
		
		if(view != null)
		{
			mDoseView = (DoseView) view.findViewById(R.id.dose_view);
			if(mDoseView == null)
				return;
			
			mDoseView.setDoseTime(mDoseTime);
			mDoseView.setDrug(mDrug);
			mDoseView.setOnClickListener(this);
		}
	}
}
