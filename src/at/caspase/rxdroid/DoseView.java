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
import android.text.TextWatcher;
import android.text.style.SuperscriptSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Database.OnChangedListener;
import at.caspase.rxdroid.db.Drug;
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
public class DoseView extends FrameLayout implements OnChangedListener
{
	@SuppressWarnings("unused")
	private static final String TAG = DoseView.class.getName();

	public static final int STATUS_INDETERMINATE = 0;
	public static final int STATUS_TAKEN = 1;
	public static final int STATUS_FORGOTTEN = 2;
	//public static final int STATUS_IGNORED = 3;

	private ImageView mIntakeStatus;
	private TextView mDoseText;
	private ImageView mDoseTimeIcon;

	private Drug mDrug;
	private int mDoseTime = -1;
	private Date mDate;

	// this value is updated by a call to countIntakes()
	private Fraction mCumulativeDose = new Fraction();

	private boolean mHasDoseOnDate = true;

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

		setBackgroundResource(R.drawable.doseview_background);

		mDoseText.setText("0");

		setClickable(true);
		setFocusable(true);
	}

	public void setDoseTime(int doseTime)
	{
		if(doseTime > Drug.TIME_NIGHT)
			throw new IllegalArgumentException();

		final int drawableIds[] = { R.drawable.ic_morning, R.drawable.ic_noon, R.drawable.ic_evening, R.drawable.ic_night };
		final int[] hints = { R.string._Morning, R.string._Noon, R.string._Evening, R.string._Night };

		mDoseText.setHint(hints[doseTime]);
		mDoseTimeIcon.setImageResource(drawableIds[doseTime]);
		mDoseTime = doseTime;
	}

	public int getDoseTime() {
		return mDoseTime;
	}

	public void setDrug(final Drug drug)
	{
		setInfo(mDate, drug);
	}

	public void setDate(Date date) {
		setInfo(date, mDrug);
	}

	public Date getDate() {
		return mDate;
	}

	public int getDrugId() {
		return mDrug.getId();
	}

	public Fraction getDose()
	{
		if(mDrug != null && mDoseTime != -1)
			return mDrug.getDose(mDoseTime);
		return new Fraction(0);
		//return Fraction.decode(mDoseText.getText().toString());
	}

	public TextView getTextView() {
		return mDoseText;
	}

	public void addTextChangedListener(TextWatcher watcher) {
		mDoseText.addTextChangedListener(watcher);
	}

	public void setInfo(Date date, Drug drug)
	{
		final boolean doUpdateView;

		if(date != null)
			mDate = date;

		if(drug != null)
		{
			mDrug = drug;
			doUpdateView = true;
		}
		else
			doUpdateView = false;

		if(mDate != null && mDrug != null)
		{
			mHasDoseOnDate = mDrug.hasDoseOnDate(mDate);
			updateIntakeStatusIcon();
		}
		else
			mHasDoseOnDate = true;

		// we have to delay the call to updateView as mHasDoseOnDate has to be set
		if(doUpdateView)
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
		int action = event.getAction() & MotionEvent.ACTION_MASK;

		switch(action)
		{
			case MotionEvent.ACTION_DOWN:
				setBackgroundResource(R.drawable.doseview_background_selected);
				break;

			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_OUTSIDE:
				setBackgroundResource(R.drawable.doseview_background);
				break;
		}

		return super.onTouchEvent(event);
	}

	@Override
	public void onEntryCreated(Entry entry, int flags)
	{
		if(entry instanceof Intake)
		{
			if(mDate == null)
				return;

			Intake intake = (Intake) entry;

			if(isApplicableIntake(intake))
			{
				/*if(intake.isEmptyIntake() && mCumulativeDose.isZero())
					markAsIgnored();
				else
				{
					mCumulativeDose.add(intake.getDose());
					markAsTaken();
				}*/
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
				setDrug(drug);
		}
	}

	@Override
	public void onEntryDeleted(Entry entry, int flags)
	{
		if(entry instanceof Intake)
		{
			if(mDate == null)
				return;

			if(isApplicableIntake((Intake) entry))
				updateIntakeStatusIcon();
		}
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		Database.registerOnChangedListener(this);
	}

	@Override
	protected void onDetachedFromWindow()
	{
		super.onDetachedFromWindow();
		Database.unregisterOnChangedListener(this);
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
		if(mDrug == null)
			return;

		final Fraction actualDose;
		if(mDate == null || mCumulativeDose == null || mCumulativeDose.isZero())
			actualDose = getDose();
		else if(!mCumulativeDose.isZero())
			actualDose = mCumulativeDose;
		else
			actualDose = null;

		if(actualDose != null && !actualDose.isZero())
		{
			SpannableStringBuilder sb = new SpannableStringBuilder(actualDose.toString());

			if(mDate != null)
			{
				final String suffix;
				switch(actualDose.compareTo(mDrug.getDose(mDoseTime, mDate)))
				{
					case -1:
						suffix = "-";
						break;
					case +1:
						suffix = "+";
						break;
					default:
						suffix = null;
				}

				if(suffix != null)
				{
					sb.append(suffix);
					sb.setSpan(new SuperscriptSpan(), sb.length() - 1, sb.length(), 0);
				}
			}

			if(mHasDoseOnDate)
				mDoseText.setText(sb);
			else
			{
				sb.insert(0, "(");
				sb.append(")");
				mDoseText.setText(sb);
			}
		}
		else
			mDoseText.setText("0");
	}

	private void updateIntakeStatusIcon()
	{
		if(mDate == null || mDrug == null)
			return;

		if(countIntakes() != 0)
		{
			if(mCumulativeDose.isZero())
				markAsIgnored();
			else
				markAsTaken();
			return;
		}

		mStatus = STATUS_INDETERMINATE;

		if(mHasDoseOnDate && mDrug.getRepeatMode() != Drug.REPEAT_ON_DEMAND)
		{
			if(!mDrug.getDose(mDoseTime, mDate).isZero())
			{
				final Calendar end = DateTime.calendarFromDate(mDate);
				end.add(Calendar.MILLISECOND, (int) Settings.instance().getTrueDoseTimeEndOffset(mDoseTime));

				if(DateTime.now().compareTo(end) != -1)
					mStatus = STATUS_FORGOTTEN;
			}
		}

		if(mStatus == STATUS_FORGOTTEN)
			mIntakeStatus.setImageResource(R.drawable.ic_dose_forgotten);
		else
			mIntakeStatus.setImageDrawable(null);
	}

	private void markAsTaken()
	{
		mIntakeStatus.setImageResource(R.drawable.ic_dose_taken);
		mStatus = STATUS_TAKEN;
	}

	private void markAsIgnored()
	{
		mIntakeStatus.setImageResource(R.drawable.ic_dose_ignored);
		mStatus = STATUS_TAKEN; // this is intentional!
	}

	private int countIntakes()
	{
		if(mDate == null || mDrug == null)
			throw new IllegalStateException("Cannot obtain intake data from DoseView with unset date and/or drug");

		List<Intake> intakes = Intake.findAll(mDrug, mDate, mDoseTime);

		mCumulativeDose = new Fraction();

		for(Intake intake : intakes)
			mCumulativeDose.add(intake.getDose());

		return intakes.size();
	}
}
