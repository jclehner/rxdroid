package at.caspase.rxdroid.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import at.caspase.rxdroid.util.Util;

public class Rot13TextView extends TextView
{
	private static final String TAG = Rot13TextView.class.getName();

	private boolean mIsCurrentlyScrambled = false;
	//private boolean mWasUnscramblerPosted = false;
	private long mUnscrambledDuration = 1000;
	private OnClickListener mOnClickListener;
	private OnLongClickListener mOnLongClickListener;

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

	public void setScramblingEnabled(boolean enabled)
	{
		if(mIsCurrentlyScrambled != enabled)
			applyRot13();

		mIsCurrentlyScrambled = enabled;
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

	private void applyRot13() {
		setText(Util.rot13(getText().toString()));
	}

	private final OnClickListener mLocalOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v)
		{
			if(mIsCurrentlyScrambled)
			{
				applyRot13();
				mIsCurrentlyScrambled = false;

				if(mUnscrambledDuration > 0)
					postDelayed(mRescrambler, mUnscrambledDuration);
			}
			else if(mOnClickListener != null)
				mOnClickListener.onClick(v);
		}
	};

	private final OnLongClickListener mLocalOnLongClickListener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v)
		{
			if(mIsCurrentlyScrambled)
			{
				if(mOnClickListener != null)
					mOnClickListener.onClick(v);
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
			applyRot13();
			mIsCurrentlyScrambled = true;
			//mWasUnscramblerPosted = false;
		}
	};
}
