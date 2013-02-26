package at.jclehner.rxdroid.widget;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.TextView;
import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.Fraction.MutableFraction;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entries;
import at.jclehner.rxdroid.db.Entry;
import at.jclehner.rxdroid.db.Intake;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.WrappedCheckedException;

public class DrugSupplyMonitor extends TextView implements
		Database.OnChangeListener/*, OnLongClickListener*/
{
	//private int mDrugId = -1;
	private Drug mDrug;
	private Date mDate;

	public DrugSupplyMonitor(Context context) {
		super(context);
	}

	public DrugSupplyMonitor(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		handleAttributes(attrs);
	}

	public DrugSupplyMonitor(Context context, AttributeSet attrs, int defStyle) {
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
		else if(entry instanceof Intake)
		{
			if(mDrug.getId() != ((Intake) entry).getDrugId())
				return;

			drug = ((Intake) entry).getDrug();
		}
		else
			return;

		setDrug(drug);
	}

	private void updateText(Drug drug, Date date)
	{
		if(drug == null)
			return;

		final Date today = DateTime.today();
		MutableFraction currentSupply = drug.getCurrentSupply().mutate();

		if(date != null && date.after(today))
		{
			//Fraction doseInTimePeriod_smart = Entries.getTotalDoseInTimePeriod_smart(drug, today, date);
			Fraction doseInTimePeriod_dumb = Entries.getTotalDoseInTimePeriod_dumb(drug, today, date, true);
			currentSupply.subtract(doseInTimePeriod_dumb);
		}

		if(!currentSupply.isNegative())
			setText(currentSupply.toString());
		else
			setText("0");
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
