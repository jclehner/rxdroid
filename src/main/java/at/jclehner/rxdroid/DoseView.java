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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.support.v4.view.ViewCompat;
import android.text.SpannableStringBuilder;
import android.text.style.SuperscriptSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import at.jclehner.rxdroid.Fraction.MutableFraction;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.Database.OnChangeListener;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entries;
import at.jclehner.rxdroid.db.Entry;
import at.jclehner.rxdroid.db.DoseEvent;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Util;

/**
 * A class for displaying dose information.
 *
 *
 *
 * @author Joseph Lehner
 */
public class DoseView extends FrameLayout implements OnChangeListener
{
	private static final String TAG = DoseView.class.getSimpleName();
	private static final boolean LOGV = false;

	private static final int[] DOSE_TIME_DRAWABLES = {
			R.drawable.ic_morning,
			R.drawable.ic_noon,
			R.drawable.ic_evening,
			R.drawable.ic_night
	};

	public static final int STATUS_INDETERMINATE = 0;
	public static final int STATUS_TAKEN = 1;
	public static final int STATUS_MISSED = 2;
	public static final int STATUS_IGNORED = 3;

	private final ImageView mIntakeStatus;
	private final TextView mDoseText;
	private final ImageView mDoseTimeIcon;

	private boolean mIsConstantBackground = false;

	private int mDoseTime = -1;

	private Drug mDrug;
	private Date mDate;
	private int mIntakeCount = 0;

	private MutableFraction mDisplayDose;

	private int mStatus = STATUS_INDETERMINATE;

	public DoseView(Context context) {
		this(context, null);
	}

	public DoseView(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		LayoutInflater.from(context).inflate(R.layout.dose_view, this, true);

		mIntakeStatus = (ImageView) findViewById(R.id.icon_intake_status);
		mDoseText = (TextView) findViewById(R.id.text_dose);
		mDoseTimeIcon = (ImageView) findViewById(R.id.icon_dose_time);

		switch(getId())
		{
			case R.id.morning:
				setDoseTime(Drug.TIME_MORNING);
				break;

			case R.id.noon:
				setDoseTime(Drug.TIME_NOON);
				break;

			case R.id.evening:
				setDoseTime(Drug.TIME_EVENING);
				break;

			case R.id.night:
				setDoseTime(Drug.TIME_NIGHT);
				break;

			// XXX
			/*case R.id.bed:
				mDoseTimeIcon.setImageResource(R.drawable.ic_night_test);
				mDoseText.setText("-");
				break;*/
			// XXX
		}
	}

	public void setDose(Fraction dose)
	{
		mDisplayDose = dose.mutate();
		updateView();
	}

	public Fraction getDose()
	{
		if(mDisplayDose != null && !mDisplayDose.isZero())
			return mDisplayDose;

		if(mDrug != null && mDoseTime != -1)
		{
			if(mDate == null)
				return mDrug.getDose(mDoseTime);

			return mDrug.getDose(mDoseTime, mDate);
		}

		throw new IllegalStateException("Neither dose nor drug and dose time were set");
	}

	public void setDoseTime(int doseTime)
	{
		if(doseTime > Drug.TIME_NIGHT)
			throw new IllegalArgumentException();

		mDoseTimeIcon.setImageResource(DOSE_TIME_DRAWABLES[doseTime]);
		mDoseTime = doseTime;
	}

	public int getDoseTime() {
		return mDoseTime;
	}

	public Drug getDrug()
	{
		if(mDrug == null)
			throw new IllegalStateException("This DoseView was not intialized with a drug and date");
		return mDrug;
	}

	public Date getDate()
	{
		if(mDate == null)
			throw new IllegalStateException("This DoseView was not intialized with a drug and date");
		return mDate;
	}

	public void setDoseTimeIconVisible(boolean visible) {
		mDoseTimeIcon.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
	}

	public void setDoseFromDrugAndDate(Date date, Drug drug)
	{
		if(date == null || drug == null)
			throw new NullPointerException();

		mDate = date;
		mDrug = drug;

		//mDose = getDose();

		mDisplayDose = new MutableFraction();

		final List<DoseEvent> events = Entries.findDoseEvents(mDrug, mDate, mDoseTime);
		for(DoseEvent intake : events)
		{
			mDisplayDose.add(intake.getDose());
			if(LOGV) Log.v(TAG, intake.toString());
		}

		mIntakeCount = events.size();

		updateView();
	}

	public boolean hasInfo(Date date, Drug drug)
	{
		if(mDate == null || !mDate.equals(date))
			return false;

		if(mDrug == null || !mDrug.equals(drug))
			return false;

		return true;
	}

	public boolean wasDoseTaken() {
		return mStatus == STATUS_TAKEN;
	}

	public void setDimmed(boolean dimmed)
	{
		final int alpha = dimmed ? 0x7f : 0xff;
		setImageAlpha(mDoseTimeIcon, alpha);
		setImageAlpha(mIntakeStatus, alpha);

		mDoseText.setEnabled(!dimmed);
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		setDimmed(!enabled);
	}

	@Override
	public void onEntryCreated(Entry entry, int flags)
	{
		if(entry instanceof DoseEvent)
		{
			DoseEvent intake = (DoseEvent) entry;
			if(isApplicableDoseEvent(intake))
			{
				mDisplayDose.add(intake.getDose());
				++mIntakeCount;

				updateView();
			}
		}
	}

	@Override
	public void onEntryUpdated(Entry entry, int flags)
	{
		if(entry instanceof Drug)
		{
			Drug drug = (Drug) entry;

			if(mDrug == null || drug.getId() == mDrug.getId())
				setDoseFromDrugAndDate(mDate, drug);
		}
	}

	@Override
	public void onEntryDeleted(Entry entry, int flags)
	{
		if(entry instanceof DoseEvent)
		{
			DoseEvent intake = (DoseEvent) entry;
			if(isApplicableDoseEvent(intake))
			{
				mDisplayDose.subtract(intake.getDose());
				--mIntakeCount;
				updateView();
			}
		}
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		if(mDrug != null)
			Database.registerEventListener(this);
	}

	@Override
	protected void onDetachedFromWindow()
	{
		super.onDetachedFromWindow();
		Database.unregisterEventListener(this);
	}

	private boolean isApplicableDoseEvent(DoseEvent intake)
	{
		if(intake.getDrugId() != mDrug.getId())
			return false;
		else if(intake.getDoseTime() != mDoseTime)
			return false;
		else if(!intake.getDate().equals(mDate))
			return false;

		return true;
	}

	private void updateView()
	{
		mStatus = STATUS_INDETERMINATE;
		mIntakeStatus.setImageDrawable(null);

		if(mDrug != null)
		{
			if(!mDrug.isActive())
				mDoseText.setText("0");
			else if(!mDisplayDose.isZero())
			{
				setStatus(STATUS_TAKEN);

				SpannableStringBuilder sb = new SpannableStringBuilder(Util.prettify(mDisplayDose));

				final Date lastScheduleUpdate = mDrug.getLastScheduleUpdateDate();

				if(lastScheduleUpdate == null || !mDate.before(lastScheduleUpdate))
				{
					final Fraction scheduledDose = mDrug.getDose(mDoseTime, mDate);
					int cmp = mDisplayDose.compareTo(scheduledDose);
					String suffix;

					if(cmp < 0)
						suffix = "-";
					else if(cmp > 0)
						suffix = "+";
					else
						suffix = null;

					if(suffix != null)
					{
						sb.append(suffix);
						sb.setSpan(new SuperscriptSpan(), sb.length() - 1, sb.length(), 0);
					}
				}

				mDoseText.setText(sb);
			}
			else
			{
				Fraction dose = mDrug.getDose(mDoseTime, mDate);
				mDoseText.setText(Util.prettify(dose));

				if(mIntakeCount == 0)
				{
					if(!dose.isZero() && !mDrug.isAsNeeded())
					{
						int offset = (int) Settings.getTrueDoseTimeEndOffset(mDoseTime);
						Date end = DateTime.add(mDate, Calendar.MILLISECOND, offset);

						if(DateTime.now().after(end))
							setStatus(STATUS_MISSED);
					}
				}
				else
					setStatus(STATUS_IGNORED);
			}

		}
		else if(mDisplayDose != null)
			mDoseText.setText(Util.prettify(mDisplayDose));

		if("0".equals(mDoseText.getText()))
		{
			String zeroStr = null;

			if(BuildConfig.DEBUG)
				zeroStr = Settings.getString("doseview_zero");

			if(zeroStr == null)
				zeroStr = "-";

			mDoseText.setText(zeroStr);

		}
	}

	private void setStatus(int status)
	{
		if(status == mStatus)
			return;

		mStatus = status;

		final int[] attrs = { R.attr.doseStatusTaken, R.attr.doseStatusMissed, R.attr.doseStatusIgnored };
		final int imageResId;

		if(status == STATUS_INDETERMINATE)
			imageResId = 0;
		else
		{
			if(status == STATUS_IGNORED)
				mStatus = STATUS_TAKEN; // this is intentional!

			imageResId = Theme.getResourceAttribute(attrs[status - 1]);
		}

		mIntakeStatus.setImageResource(imageResId);
	}

	@TargetApi(16)
	@SuppressWarnings("deprecation")
	private static void setImageAlpha(ImageView image, int alpha)
	{
		if(Version.SDK_IS_JELLYBEAN_OR_NEWER)
			image.setImageAlpha(alpha);
		else
			image.setAlpha(alpha);
	}
}
