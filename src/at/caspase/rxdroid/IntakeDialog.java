package at.caspase.rxdroid;

import java.util.Date;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import at.caspase.rxdroid.FractionInput.OnChangedListener;
import at.caspase.rxdroid.db.Drug;

public class IntakeDialog extends AlertDialog implements OnClickListener, OnShowListener, OnChangedListener
{
	private static final String TAG = IntakeDialog.class.getName();
	
	private Drug mDrug;
	private int mDoseTime;
	private Date mDate;
	
	private Fraction mDose;
	
	private int mFlags;
	
	private TextView mDoseText;
	private TextView mHint;
	private FractionInput mDoseEdit;
	
	public static final int FLAG_ALLOW_DOSE_EDIT = 1;
	
	public IntakeDialog(Context context, Drug drug, int doseTime, Date date) 
	{
		super(context);
		
		mDrug = drug;
		mDoseTime = doseTime;
		mDate = date;
		
		mDose = drug.getDose(doseTime, date);
		
		LayoutInflater lf = LayoutInflater.from(context);
		View view = lf.inflate(R.layout.intake, null);
		
		mDoseText = (TextView) view.findViewById(R.id.dose_text);
		mHint = (TextView) view.findViewById(R.id.dose_hint);
		mDoseEdit = (FractionInput) view.findViewById(R.id.dose_edit);
				
		mDoseText.setText(mDose.toString());
		mDoseText.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v)
			{
				mDoseText.setVisibility(View.GONE);
				mHint.setVisibility(View.GONE);
				mDoseEdit.setVisibility(View.VISIBLE);
				getButton(BUTTON_NEUTRAL).setEnabled(true);
			}
		});
		
		//mDoseText.setError(getString(R.string._msg_click_to_edit));
		
		mDoseEdit.setValue(mDose);
		mDoseEdit.setOnChangeListener(this);
		//mDoseEdit.setVisibility(View.GONE);
		
		setTitle(mDrug.getName());
		
		setButton(BUTTON_POSITIVE, getString(android.R.string.ok), (OnClickListener) null);
		setButton(BUTTON_NEUTRAL, "1 ↔ 1¾", this);
		setButton(BUTTON_NEGATIVE, getString(android.R.string.cancel), this);
		
		setupMessages();
		
		setView(view);		
		
		setOnShowListener(this);
	}

	@Override
	public void onShow(final DialogInterface dialog)
	{		
		setNonDismissingListener(BUTTON_POSITIVE);
		setNonDismissingListener(BUTTON_NEUTRAL);		
		getButton(BUTTON_NEUTRAL).setEnabled(false);
		
		Log.d(TAG, "onShow: OK");
	}

	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		Toast.makeText(getContext(), "Clicked " + which, Toast.LENGTH_SHORT).show();
		
		if(which == BUTTON_NEUTRAL)
			mDoseEdit.setMixedNumberMode(!mDoseEdit.isInMixedNumberMode());
		else if(which == BUTTON_POSITIVE)
		{
			if(mDoseEdit.getValue().isZero())
			{
				removeAllCustomViews();		
				setMessage("YARRRR, ZERO!");
			}			
		}	
	}
	
	@Override
	public void onChanged(FractionInput widget, Fraction oldValue)
	{
		Log.d(TAG, "onChanged: " + oldValue + " -> " + widget.getValue());		
	}
	
	private void setNonDismissingListener(int button)
	{
		Button b = getButton(button);
		if(b != null)
			b.setOnClickListener(new NonDismissingListener(button));		
	}
		
	private void setupMessages()
	{
		if(mDose.isZero() || (mFlags & FLAG_ALLOW_DOSE_EDIT) != 0)
		{
			setMessage(getString(R.string._msg_intake_unscheduled));
		}
		else
		{
			setMessage(getString(R.string._msg_intake_normal));
		}
	}
	
	private String getString(int resId) {
		return getContext().getString(resId);
	}
	
	private void removeAllCustomViews()
	{
		// setView() has no effect once the dialog is shown, so
		// just hide the custom views
		
		View views[] = { mDoseEdit, mDoseText, mHint };
		
		for(View v : views)
			v.setVisibility(View.GONE);		
	}
	
	private class NonDismissingListener implements View.OnClickListener
	{
		private int mButton;
		
		public NonDismissingListener(int button) {
			mButton = button;
		}
		
		@Override
		public void onClick(View v)
		{
			// Allows handling the click in the expected place, 
			// while allowing control over whether the dialog
			// should be dismissed or not.
			IntakeDialog.this.onClick(IntakeDialog.this, mButton);				
		}		
	}
}
