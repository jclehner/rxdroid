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

package at.caspase.rxdroid;

import java.sql.Date;

import android.content.Context;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import at.caspase.rxdroid.Database.Drug;
import at.caspase.rxdroid.Database.Entry;
import at.caspase.rxdroid.Database.Intake;
import at.caspase.rxdroid.Database.OnDatabaseChangedListener;
import at.caspase.rxdroid.util.DateTime;

/**
 * A class for viewing drug doses.
 *
 *
 *
 *
 * @author Joseph Lehner
 *
 */
public class DoseView extends FrameLayout implements OnDatabaseChangedListener
{	
	@SuppressWarnings("unused")
	private static final String TAG = DoseView.class.getName();

	public static final int STATUS_INDETERMINATE = 0;
	public static final int STATUS_TAKEN = 1;
	public static final int STATUS_FORGOTTEN = 2;	
	
	private ImageView mIntakeStatus;
	private TextView mDoseText;
	private ImageView mDoseTimeIcon;
	
	private Drug mDrug;
	private int mDoseTime = -1;
	private Date mDate;
	
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
				setDoseTime(Database.Drug.TIME_MORNING);
				break;

			case R.id.noon:
				setDoseTime(Database.Drug.TIME_NOON);
				break;

			case R.id.evening:
				setDoseTime(Database.Drug.TIME_EVENING);
				break;

			case R.id.night:
				setDoseTime(Database.Drug.TIME_NIGHT);
				break;
		}

		setBackgroundResource(R.drawable.doseview_background);

		mDoseText.setText("0");

		setClickable(true);
		setFocusable(true);
	}

	public void setDoseTime(int doseTime)
	{
		if(doseTime > Database.Drug.TIME_NIGHT)
			throw new IllegalArgumentException();

		final int drawableIds[] = { R.drawable.ic_morning, R.drawable.ic_noon, R.drawable.ic_evening, R.drawable.ic_night };
		final String[] hints = { "Morning", "Noon", "Evening", "Night" };

		mDoseText.setHint(hints[doseTime]);
		mDoseTimeIcon.setImageResource(drawableIds[doseTime]);
		mDoseTime = doseTime;
	}

	public int getDoseTime() {
		return mDoseTime;
	}

	public void setDrug(final Drug drug)
	{
		setInfo(null, drug);
	}

	public void setDate(Date date) {
		setInfo(date, null);
	}

	public Date getDate() {
		return mDate;
	}

	public int getDrugId() {
		return mDrug.getId();
	}

	public Fraction getDose() {
		return Fraction.decode(mDoseText.getText().toString());
	}

	public TextView getTextView() {
		return mDoseText;
	}

	public void addTextChangedListener(TextWatcher watcher) {
		mDoseText.addTextChangedListener(watcher);
	}

	public void setInfo(Date date, Drug drug)
	{
		if(date != null)
			mDate = date;

		if(drug != null)
		{
			mDrug = drug;
			updateView();
		}
		
		if(mDate != null && mDrug != null)
			updateIntakeStatusIcon();
	}
	
	public boolean hasInfo(Date date, Drug drug)
	{		
		if(mDate == null || !mDate.equals(date))
			return false;
		
		if(mDrug == null || !mDrug.equals(drug))
			return false;
		
		return true;		
	}
	
	public boolean hasSameInfo(DoseView other)
	{
		return hasInfo(other.mDate, other.mDrug);
	}
	
	public int getIntakeStatus() {
		return mStatus;
	}

	@Override
	public boolean onTouchEvent(final MotionEvent event)
	{
		int action = event.getAction() & MotionEvent.ACTION_MASK;

		switch(action)
		{
			case MotionEvent.ACTION_DOWN:
				setBackgroundResource(R.drawable.doseview_background_focus);
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
	
			if(isApplicableIntake((Intake) entry))
				markAsTaken();
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

		if(mDate == null || mDrug.hasDoseOnDate(mDate))
			mDoseText.setText(mDrug.getDose(mDoseTime).toString());
		else
			mDoseText.setText("0");
	}

	private void updateIntakeStatusIcon()
	{
		if(mDate == null || mDrug == null)
			return;

		if(countIntakes() != 0)
		{
			markAsTaken();
			return;
		}
		
		final Date end = new Date(mDate.getTime() + Preferences.instance().getDoseTimeEndOffset(mDoseTime));		
		
		if(mDrug.isActive() && !mDoseText.getText().equals("0") && DateTime.now().compareTo(end) != -1)
		{
			mIntakeStatus.setImageResource(R.drawable.bg_dose_forgotten);
			mStatus = STATUS_FORGOTTEN;
		}
		else
		{
			mIntakeStatus.setImageDrawable(null);
			mStatus = STATUS_INDETERMINATE;
		}
	}
	
	private void markAsTaken()
	{
		mIntakeStatus.setImageResource(R.drawable.bg_dose_taken);
		mStatus = STATUS_TAKEN;
	}

	private int countIntakes()
	{
		if(mDate == null || mDrug == null)
			throw new IllegalStateException("Cannot obtain intake data from DoseView with unset date and/or drug");

		return Database.findIntakes(mDrug, mDate, mDoseTime).size();
	}
}
