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

package at.jclehner.rxdroid;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;

public final class Help
{
	private static final String TAG = Help.class.getSimpleName();

	private static final int ABOVE = 1;
	private static final int BELOW = 1 << 1;
	private static final int TO_LEFT_OF = 1 << 2;
	private static final int TO_RIGHT_OF = 1 << 3;

	static class SpotlightView extends View
	{
		private final int[] mCutoutLocation = new int[2];
		private final DisplayMetrics mMetrics = new DisplayMetrics();

		private WindowManager mWinMgr;

		private int mCutoutHeight = 0;
		private int mCutoutWidth = 0;
		private Rect mCutoutRect;

		private int mBgAlpha = 0x7f;

		private int mPaddingPixels = 10;

		private Paint mCutoutPaint;
		private int mCutoutStrokeColor = Color.RED;

		private final int[] mContentOffset = new int[2];

		public SpotlightView(Context context) {
			this(context, null);
		}

		public SpotlightView(Context context, AttributeSet attrs)
		{
			super(context, attrs);

			if(attrs == null)
			{
				setBackgroundColor(Color.TRANSPARENT);
				setSpotlightBorderColor(Color.RED);
			}
			else
				handleAttributes(context, attrs);

			mWinMgr = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		}

		public void setBackgroundDim(float percentage)
		{
			if(percentage < 0 || percentage > 100)
				throw new IllegalArgumentException("percentage=" + percentage);

			final int alpha = Math.round(255f * (percentage / 100f));
			if(alpha < 0 || alpha > 255)
				throw new IllegalStateException("alpha=" + alpha);

			mBgAlpha = alpha;
			//setBackgroundColor(Color.argb(alpha, 0, 0, 0));
		}

		public void setSpotlightBorderColor(int color)
		{
			mCutoutStrokeColor = color;

			mCutoutPaint = new Paint();
			mCutoutPaint.setStrokeWidth(3.0f);
			mCutoutPaint.setStrokeJoin(Join.ROUND);
			mCutoutPaint.setAntiAlias(true);
			mCutoutPaint.setXfermode(new PorterDuffXfermode(Mode.SRC_OVER));
			//mCutoutPaint.setXfermode(new PorterDuffXfermode(Mode.))
			//mCutoutPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
		}

		public void setTargetView(View view)
		{
			view.getLocationOnScreen(mCutoutLocation);
			mCutoutHeight = view.getHeight();
			mCutoutWidth = view.getWidth();

			final int left = mCutoutLocation[0] - mPaddingPixels;
			final int top = mCutoutLocation[1] - mPaddingPixels;
			final int right = left + mCutoutWidth + mPaddingPixels;
			final int bottom = top + mCutoutHeight + mPaddingPixels;

			mCutoutRect = new Rect(left, top, right, bottom);
			//mCutoutRectF = new RectF(mCutoutRect);

			Log.d(TAG, "setTargetView:\n  mCutoutRect=" + mCutoutRect);

			final View content = view.getRootView().findViewById(android.R.id.content);
			if(content != null)
				content.getLocationOnScreen(mContentOffset);
		}

		public boolean hasLargestSpaceToLeftOfSpotlight()
		{
			mWinMgr.getDefaultDisplay().getMetrics(mMetrics);
			return mCutoutLocation[0] + mContentOffset[0] > mMetrics.widthPixels / 2;
		}

		public boolean hasLargestSpaceAboveSpotlight()
		{
			mWinMgr.getDefaultDisplay().getMetrics(mMetrics);
			return mCutoutLocation[1] + mContentOffset[1] > mMetrics.heightPixels / 2;
		}

		@Override
		protected void onDraw(Canvas canvas)
		{
			//super.onDraw(canvas);
			canvas.save();
			canvas.clipRect(mCutoutRect, Op.DIFFERENCE);
			canvas.drawARGB(mBgAlpha, 0, 0, 0);
			canvas.restore();

			if(mCutoutRect != null && mCutoutPaint != null)
			{
				// First, punch a hole in the canvas...
				/*mCutoutPaint.setStyle(Style.FILL);
				mCutoutPaint.setColor(Color.TRANSPARENT);
				canvas.drawRect(mCutoutRect, mCutoutPaint);*/

				// ... then draw a stroke around it.
				mCutoutPaint.setStyle(Style.STROKE);
				mCutoutPaint.setColor(mCutoutStrokeColor);
				canvas.drawRect(mCutoutRect, mCutoutPaint);
			}
		}

		private void handleAttributes(Context context, AttributeSet attrs)
		{
			if(attrs == null)
				return;

			final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SpotlightView);

			setSpotlightBorderColor(a.getColor(R.styleable.SpotlightView_spotBorderColor, Color.RED));
			setBackgroundDim(a.getFraction(R.styleable.SpotlightView_backgroundDim, 1, 1, 50));

			final int spotOnId = a.getResourceId(R.styleable.SpotlightView_spotOnView, 0);
			if(spotOnId == 0)
				return;

			final ViewParent parent = getParent();
			if(parent instanceof View)
			{
				final View target = ((View) parent).findViewById(spotOnId);
				if(target != null)
				{
					setTargetView(target);
					return;
				}
			}

			throw new IllegalArgumentException("No view with id " + spotOnId + " in view hierarchy");
		}
	}

	static class HelpOverlayView extends RelativeLayout
	{
		private final SpotlightView mSpotlight;
		/* package */ final TextView mText;

		public HelpOverlayView(Context context)
		{
			super(context);
			mSpotlight = new SpotlightView(context);
			mText = new TextView(context);
			mText.setTextColor(Color.WHITE);

			mSpotlight.setBackgroundDim(70.0f);
			mSpotlight.setSpotlightBorderColor(Color.RED);
		}

		public void setTargetView(View target)
		{
			mSpotlight.setTargetView(target);

			int targetId = target.getId();
			if(targetId == View.NO_ID)
			{
				targetId = 0x0b00b135;
				target.setId(targetId);
			}

			final int xRule;
			final int yRule;

			if(mSpotlight.hasLargestSpaceAboveSpotlight())
				yRule = RelativeLayout.ALIGN_PARENT_TOP;
			else
				yRule = RelativeLayout.ALIGN_PARENT_BOTTOM;

			if(mSpotlight.hasLargestSpaceToLeftOfSpotlight())
				xRule = RelativeLayout.ALIGN_PARENT_LEFT;
			else
				xRule = RelativeLayout.ALIGN_PARENT_RIGHT;

			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			addView(mSpotlight, params);

			params = new RelativeLayout.LayoutParams(params);
			params.addRule(xRule);
			params.addRule(yRule);

			final DisplayMetrics metrics = target.getContext().getResources().getDisplayMetrics();
			params.topMargin = params.bottomMargin = metrics.heightPixels / 8;
			params.leftMargin = params.rightMargin = metrics.widthPixels / 8;

			addView(mText, params);
		}

		public void setHelpText(int resId) {
			mText.setText(resId);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event)
		{
			removeFromWindow();
			return true;
		}

		@Override
		public boolean onKeyUp(int keyCode, KeyEvent event)
		{
			removeFromWindow();
			return true;
		}

		private void removeFromWindow()
		{
			final WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
			try
			{
				wm.removeView(this);
			}
			catch(IllegalArgumentException e)
			{
				Log.w(TAG, e);
			}
			Help.sSpotlightOn = false;
		}
	}

	private static final ArrayList<Pair<View, Integer>> sHelpQueue = new ArrayList<Pair<View, Integer>>();
	private static volatile boolean sSpotlightOn = false;

	public static void enqueue(View v, String helpSuffix)
	{
		final Resources res = RxDroid.getContext().getResources();
		final int resId = res.getIdentifier("at.jclehner.rxdroid:string/_help_" + helpSuffix, null, null);
		if(resId == 0)
		{
			Log.w("Help", "Missing resource id for help suffix " + helpSuffix);
			return;
		}

		if(Settings.getStringSet(Settings.Keys.DISPLAYED_HELP_SUFFIXES).contains(helpSuffix))
			return;

		synchronized(sHelpQueue) {
			sHelpQueue.add(new Pair<View, Integer>(v, resId));
		}
	}

	public static boolean hasEnqueued()
	{
		synchronized(sHelpQueue) {
			return !sHelpQueue.isEmpty();
		}
	}

	public static void showEnqueued()
	{
		synchronized(sHelpQueue)
		{
			for(Pair<View, Integer> data : sHelpQueue)
			{
//				while(Help.sSpotlightOn)
//					Util.sleepAtMost(10);

				spotOn(data.first, data.second);
			}

			sHelpQueue.clear();
		}
	}

	public static void spotOn(View view, int helpTextResId)
	{
		Help.sSpotlightOn = true;

		final Context context = view.getContext();

		final HelpOverlayView helpOverlay = new HelpOverlayView(context);
		helpOverlay.mText.setText(helpTextResId);
		helpOverlay.setTargetView(view);

		final LayoutParams params = new LayoutParams();
		params.height = LayoutParams.MATCH_PARENT;
		params.width = LayoutParams.MATCH_PARENT;
		params.flags = LayoutParams.FLAG_LAYOUT_IN_SCREEN;
		params.format = PixelFormat.TRANSLUCENT;

		final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		wm.addView(helpOverlay, params);
	}

	private Help() {}
}
