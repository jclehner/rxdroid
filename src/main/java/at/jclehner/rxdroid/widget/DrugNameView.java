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

package at.jclehner.rxdroid.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import at.jclehner.rxdroid.Settings;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entries;

public class DrugNameView extends TextView
{
	private boolean mIsCurrentlyScrambled = false;
	private long mUnscrambledDuration = 1000;
	private OnClickListener mOnClickListener;
	private OnLongClickListener mOnLongClickListener;

	private String mName;
	private Drug mDrug;

	public DrugNameView(Context context) {
		this(context, null);
	}

	public DrugNameView(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.textViewStyle);
	}

	public DrugNameView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

		super.setOnClickListener(mLocalOnClickListener);
		super.setOnLongClickListener(mLocalOnLongClickListener);
	}

	public void setDrug(Drug drug)
	{
		mDrug = drug;
		setName(drug.getName());
		setScrambled(Settings.getBoolean(Settings.Keys.SCRAMBLE_NAMES, false));
	}

	public void setScrambled(boolean scrambled)
	{
		final String name = scrambled ? Entries.getDrugName(mDrug) : mName;
		setText(name);
		mIsCurrentlyScrambled = !name.equals(mName);
	}

	public void setUnscrambledDuration(long millis) {
		mUnscrambledDuration = millis;
	}

	@Override
	public void setOnClickListener(OnClickListener l) {
		mOnClickListener = l;
	}

	@Override
	public void setOnLongClickListener(OnLongClickListener l) {
		mOnLongClickListener = l;
	}

	private void setName(String text)
	{
		mName = text;
		setText(text);
	}

	private final OnClickListener mLocalOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v)
		{
			if(mIsCurrentlyScrambled)
			{
				// TODO show toast for the first few times to let the user know

				setScrambled(false);

				if(mUnscrambledDuration > 0)
					postDelayed(mRescrambler, mUnscrambledDuration);
			}
			else if(mOnClickListener != null)
			{
				//removeCallbacks(mRescrambler);
				mOnClickListener.onClick(v);
				//if(!mIsCurrentlyScrambled)
				//	mRescrambler.run();
			}
		}
	};

	private final OnLongClickListener mLocalOnLongClickListener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v)
		{
			if(mIsCurrentlyScrambled)
			{
				if(mOnClickListener != null)
				{
					mOnClickListener.onClick(v);
					return true;
				}
			}
			else if(mOnLongClickListener != null)
				return mOnLongClickListener.onLongClick(v);

			return false;
		}
	};

	private final Runnable mRescrambler = new Runnable() {

		@Override
		public void run()
		{
			setScrambled(true);
		}
	};
}
