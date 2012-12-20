package at.jclehner.rxdroid.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entry;
import at.jclehner.rxdroid.db.Intake;

public class DrugSupplyMonitor extends TextView implements Database.OnChangeListener
{
	private int mDrugId = -1;

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
		mDrugId = drug.getId();
		setText(drug.getCurrentSupply().toString());
	}

	public Drug getDrug() {
		return Database.find(Drug.class, mDrugId);
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
			if(mDrugId != entry.getId())
				return;

			drug = (Drug) entry;
		}
		else if(entry instanceof Intake)
		{
			if(mDrugId != ((Intake) entry).getDrugId())
				return;

			drug = ((Intake) entry).getDrug();
		}
		else
			return;

		if(drug != null)
			setText(drug.getCurrentSupply().toString());
	}
}
