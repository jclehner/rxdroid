package at.jclehner.rxdroid.widget;

import java.util.Date;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;
import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.Fraction.MutableFraction;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entries;
import at.jclehner.rxdroid.db.Entry;
import at.jclehner.rxdroid.db.Intake;
import at.jclehner.rxdroid.util.DateTime;

public class DrugSupplyMonitor extends TextView implements Database.OnChangeListener
{
	//private int mDrugId = -1;
	private Drug mDrug;
	private Date mDate;

	public DrugSupplyMonitor(Context context) {
		super(context);
	}

	public DrugSupplyMonitor(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public DrugSupplyMonitor(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
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
			Fraction doseInTimePeriod_smart = Entries.getTotalDoseInTimePeriod_smart(drug, today, date);
			Fraction doseInTimePeriod_dumb = Entries.getTotalDoseInTimePeriod_dumb(drug, today, date);

			if(!doseInTimePeriod_smart.equals(doseInTimePeriod_dumb))
			{
				Log.w("DrugSupplyMonitor", "smart: " + doseInTimePeriod_smart + "\ndumb: " + doseInTimePeriod_dumb + "\n---");
			}

			currentSupply.subtract(doseInTimePeriod_smart);
		}

		if(!currentSupply.isNegative())
			setText(currentSupply.toString());
		else
			setText("0");
	}
}
