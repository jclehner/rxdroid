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

package at.caspase.rxdroid;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.style.SuperscriptSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Database.OnChangeListener;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Entries;
import at.caspase.rxdroid.db.Entry;
import at.caspase.rxdroid.db.Intake;
import at.caspase.rxdroid.util.DateTime;

/**
 * A class for displaying dose information.
 *
 *
 *
 * @author Joseph Lehner
 */
public class DoseView extends FrameLayout implements OnChangeListener
{
	private static final String TAG = DoseView.class.getName();
	private static final boolean LOGV = false;

	public static final int STATUS_INDETERMINATE = 0;
	public static final int STATUS_TAKEN = 1;
	public static final int STATUS_FORGOTTEN = 2;
	//public static final int STATUS_IGNORED = 3;

	private final ImageView mIntakeStatus;
	private final TextView mDoseText;
	private final ImageView mDoseTimeIcon;

	private int mDoseTime = -1;

	private Drug mDrug;
	private Date mDate;
	private int mIntakeCount = 0;

	private Fraction mDisplayDose;

	private int mStatus = STATUS_INDETERMINATE;

	public DoseView(Context context) {
		this(context, null);
	}

	public DoseView(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.dose_view, this, true);

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
		}
	}

	public void setDose(Fraction dose)
	{
		mDisplayDose = dose;
		updateView();
	}

	public Fraction getDose()
	{
		if(mDisplayDose != null)
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

		final int drawableIds[] = { R.drawable.ic_morning, R.drawable.ic_noon, R.drawable.ic_evening, R.drawable.ic_night };

		mDoseTimeIcon.setImageResource(drawableIds[doseTime]);
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

	public void setDoseFromDrugAndDate(Date date, Drug drug)
	{
		if(date == null || drug == null)
			throw new NullPointerException();

		mDate = date;
		mDrug = drug;

		//mDose = getDose();

		mDisplayDose = new Fraction();

		List<Intake> intakes = Entries.findIntakes(mDrug, mDate, mDoseTime);
		for(Intake intake : intakes)
		{
			mDisplayDose.add(intake.getDose());
			if(LOGV) Log.v(TAG, intake.toString());
		}

		mIntakeCount = intakes.size();

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

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		if(isClickable())
		{
			// A bit of a hack, but it makes the DoseView's background change much more
			// responsive.

			final int action = event.getAction() & MotionEvent.ACTION_MASK;

			switch(action)
			{
				case MotionEvent.ACTION_DOWN:
					setBackgroundResource(R.drawable.doseview_background_selected);
					setPadding(0, 0, 0, 0);
					break;

				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
				case MotionEvent.ACTION_OUTSIDE:
					setBackgroundResource(R.drawable.doseview_background);
					setPadding(0, 0, 0, 0);
					break;
			}
		}

		return super.onTouchEvent(event);
	}

	@Override
	public void onEntryCreated(Entry entry, int flags)
	{
		if(entry instanceof Intake)
		{
			Intake intake = (Intake) entry;
			if(isApplicableIntake(intake))
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
		if(entry instanceof Intake)
		{
			Intake intake = (Intake) entry;
			if(isApplicableIntake(intake))
			{
				mDisplayDose.subtract(intake.getDose());
				--mIntakeCount;
				updateView();

				Log.d(TAG, "onEntryDeleted: mDose=" + mDisplayDose + ", mIntakeCount=" + mIntakeCount);
			}
		}
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();

		setBackgroundResource(R.drawable.doseview_background);
		setPadding(0, 0, 0, 0);

		if(mDrug != null)
			Database.registerEventListener(this);
	}

	@Override
	protected void onDetachedFromWindow()
	{
		super.onDetachedFromWindow();

		setBackgroundDrawable(null);
		setPadding(0, 0, 0, 0);

		Database.unregisterEventListener(this);
	}

	private boolean isApplicableIntake(Intake intake)
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
			if(!mDisplayDose.isZero())
			{
				markAsTaken();

				final Fraction scheduledDose = mDrug.getDose(mDoseTime, mDate);
				int cmp = mDisplayDose.compareTo(scheduledDose);
				String suffix;

				if(cmp < 0)
					suffix = "-";
				else if(cmp > 0)
					suffix = "+";
				else
					suffix = null;

				SpannableStringBuilder sb = new SpannableStringBuilder(mDisplayDose.toString());

				if(suffix != null)
				{
					sb.append(suffix);
					sb.setSpan(new SuperscriptSpan(), sb.length() - 1, sb.length(), 0);
				}

				mDoseText.setText(sb);
			}
			else
			{
				Fraction dose = mDrug.getDose(mDoseTime, mDate);
				mDoseText.setText(dose.toString());

				if(mIntakeCount == 0)
				{
					if(!dose.isZero() && mDrug.getRepeatMode() != Drug.REPEAT_ON_DEMAND)
					{
						int offset = (int) Settings.getTrueDoseTimeEndOffset(mDoseTime);
						Date end = DateTime.add(mDate, Calendar.MILLISECOND, offset);

						if(DateTime.now().after(end))
							markAsForgotten();
					}
				}
				else
					markAsIgnored();
			}

		}
		else if(mDisplayDose != null)
			mDoseText.setText(mDisplayDose.toString());
	}

	private void markAsTaken()
	{
		mIntakeStatus.setImageResource(R.drawable.ic_dose_taken_light);
		mStatus = STATUS_TAKEN;
	}

	private void markAsIgnored()
	{
		mIntakeStatus.setImageResource(R.drawable.ic_dose_ignored_light);
		mStatus = STATUS_TAKEN; // this is intentional!
	}

	private void markAsForgotten()
	{
		mIntakeStatus.setImageResource(R.drawable.ic_dose_forgotten_light);
		mStatus = STATUS_FORGOTTEN;
	}
}
