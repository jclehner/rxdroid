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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.Fraction.MutableFraction;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.Theme;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.DoseEvent;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entries;
import at.jclehner.rxdroid.db.Entry;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Util;
import at.jclehner.rxdroid.util.WrappedCheckedException;

public class DrugSupplyMonitor extends TextView implements
		Database.OnChangeListener/*, OnLongClickListener*/
{
	//private int mDrugId = -1;
	private Drug mDrug;
	private Date mDate;
	private Date mToday;

	public DrugSupplyMonitor(Context context) {
		super(context);
	}

	public DrugSupplyMonitor(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		handleAttributes(attrs);
	}

	public DrugSupplyMonitor(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		handleAttributes(attrs);
	}

	public void setDrug(Drug drug)
	{
		mDrug = drug;
		updateText(drug, mDate);
	}

	public void setDate(Date date)
	{
		mDate = date;
		updateText(mDrug, date);
	}

	public void setDrugAndDate(Drug drug, Date date)
	{
		mDrug = drug;
		mDate = date;
		updateText(drug, date);
	}

	public Drug getDrug() {
		return mDrug;
	}

	public void setToday(Date date) {
		mToday = date;
	}

	@Override
	public void onEntryCreated(Entry entry, int flags) {
		updateTextIfApplicable(entry);
	}

	@Override
	public void onEntryUpdated(Entry entry, int flags) {
		updateTextIfApplicable(entry);
	}

	@Override
	public void onEntryDeleted(Entry entry, int flags) {
		updateTextIfApplicable(entry);
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		Database.registerEventListener(this);
	}

	@Override
	protected void onDetachedFromWindow()
	{
		super.onDetachedFromWindow();
		Database.unregisterEventListener(this);
	}

	private void updateTextIfApplicable(Entry entry)
	{
		final Drug drug;

		if(entry instanceof Drug)
		{
			if(mDrug.getId() != entry.getId())
				return;

			drug = (Drug) entry;
		}
		else if(entry instanceof DoseEvent)
		{
			if(mDrug.getId() != ((DoseEvent) entry).getDrugId())
				return;

			drug = ((DoseEvent) entry).getDrug();
		}
		else
			return;

		setDrug(drug);
	}

	private void updateText(Drug drug, Date date)
	{
		int typeface = Typeface.NORMAL;
		float textScaleX = 1.0f;
		boolean highlight = false;

		if(drug != null)
		{
			MutableFraction currentSupply = drug.getCurrentSupply().mutate();

			if(drug.isActive() && date != null)
			{
				final Date today = mToday != null ? mToday : DateTime.today();
				if(date.after(today))
				{
					//Fraction doseInTimePeriod_smart = Entries.getTotalDoseInTimePeriod_smart(drug, today, date);
					Fraction doseInTimePeriod_dumb = Entries.getTotalDoseInTimePeriod_dumb(drug, today, date, true);
					currentSupply.subtract(doseInTimePeriod_dumb);
				}
				else if(date.equals(today) && (Entries.hasLowSupplies(drug, date) || Entries.willExpireSoon(drug, date)))
				{
					//typeface = Typeface.BOLD_ITALIC;
					//textScaleX = 1.25f;
					highlight = true;
				}
			}

			if(!currentSupply.isNegative())
				setText(Util.prettify(currentSupply));
			else
				setText("0");
		}

		setBackgroundResource(highlight ? R.drawable.highlight
						: Theme.getResourceAttribute(R.attr.selectableItemBackground));

		setTypeface(null, typeface);
		setTextScaleX(textScaleX);
	}

	private void handleAttributes(AttributeSet attrs)
	{
		if(attrs == null)
			return;
		final Context context = getContext();
		final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.DrugSupplyMontior);
		final String onLongClick = a.getString(R.styleable.DrugSupplyMontior_onLongClick);
		if(onLongClick != null)
		{
			setOnLongClickListener(new OnLongClickListener() {

				@Override
				public boolean onLongClick(View v)
				{
					final Class<?> cls = context.getClass();
					try
					{
						final Method m = cls.getMethod(onLongClick, View.class);
						m.invoke(context, DrugSupplyMonitor.this);
						return true;
					}
					catch(NoSuchMethodException e)
					{
						throw new WrappedCheckedException(e);
					}
					catch(IllegalArgumentException e)
					{
						throw new WrappedCheckedException(e);
					}
					catch(IllegalAccessException e)
					{
						throw new WrappedCheckedException(e);
					}
					catch(InvocationTargetException e)
					{
						throw new WrappedCheckedException(e);
					}
				}
			});
		}

		a.recycle();
	}
}
