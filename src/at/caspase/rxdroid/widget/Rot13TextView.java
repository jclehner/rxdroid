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

package at.caspase.rxdroid.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import at.caspase.rxdroid.util.Util;

public class Rot13TextView extends TextView
{
	//private static final String TAG = Rot13TextView.class.getName();

	private boolean mIsCurrentlyScrambled = false;
	private long mUnscrambledDuration = 1000;
	private OnClickListener mOnClickListener;
	private OnLongClickListener mOnLongClickListener;

	private String mText;

	public Rot13TextView(Context context) {
		this(context, null);
	}

	public Rot13TextView(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.textViewStyle);
	}

	public Rot13TextView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

		super.setOnClickListener(mLocalOnClickListener);
		super.setOnLongClickListener(mLocalOnLongClickListener);
	}

	public void setScrambled(boolean scrambled)
	{
		setText(scrambled ? Util.rot13(mText) : mText);
		mIsCurrentlyScrambled = scrambled;
	}

	public void setUnscrambledText(String text)
	{
		mText = text;
		setText(text);
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

	private final OnClickListener mLocalOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v)
		{
			if(mIsCurrentlyScrambled)
			{
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
