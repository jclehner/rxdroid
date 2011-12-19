package at.caspase.rxdroid;

import java.util.Date;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import at.caspase.rxdroid.FractionInput.OnChangedListener;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Entry;
import at.caspase.rxdroid.db.Intake;

public class IntakeDialog extends AlertDialog implements OnClickListener, OnShowListener, OnChangedListener, Database.OnChangedListener
{
	private static final String TAG = IntakeDialog.class.getName();

	private Drug mDrug;
	private int mDoseTime;
	private Date mDate;

	private Fraction mDose;

	private int mFlags;

	private TextView mDoseText;
	private TextView mHintText;
	private FractionInput mDoseEdit;

	private boolean mIsInInsufficientSupplyMode = false;
	private boolean mDismissCalledFromSwitchMode = false;

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
		mHintText = (TextView) view.findViewById(R.id.dose_hint);
		mDoseEdit = (FractionInput) view.findViewById(R.id.dose_edit);

		mDoseText.setText(mDose.toString());
		mDoseText.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v)
			{
				setEditable(true);
			}
		});

		mDoseEdit.setValue(mDose);
		mDoseEdit.setFractionInputMode(mDose.isInteger() ? FractionInput.MODE_INTEGER : FractionInput.MODE_FRACTION);
		mDoseEdit.setOnChangeListener(this);

		setTitle(mDrug.getName());

		setView(view);

		setButton(BUTTON_POSITIVE, getString(android.R.string.ok), (OnClickListener) null);
		setButton(BUTTON_NEGATIVE, getString(android.R.string.cancel), this);

		setupMessages();

		setOnShowListener(this);
		Database.registerOnChangedListener(this);
	}

	public void setEditable(boolean editable)
	{
		int editVisibility = editable ? View.VISIBLE : View.INVISIBLE;
		int textVisibility = editable ? View.INVISIBLE : View.VISIBLE;

		mDoseText.setVisibility(textVisibility);
		mHintText.setVisibility(textVisibility);
		mDoseEdit.setVisibility(editVisibility);
	}

	@Override
	public void onShow(final DialogInterface dialog)
	{
		setNonDismissingListener(BUTTON_POSITIVE);
		setNonDismissingListener(BUTTON_NEUTRAL);

		setupNormalMode();

		Log.d(TAG, "onShow: OK");
	}

	@Override
	public void onStop()
	{
		Log.d(TAG, "onStop: calledFromSwitchMode=" + mDismissCalledFromSwitchMode);
		if(!mDismissCalledFromSwitchMode)
			Database.unregisterOnChangedListener(this);
	}

	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		if(!mIsInInsufficientSupplyMode)
			handleOnClickInNormalMode(which);
		else
			handleOnClickInInsufficientSupplyMode(which);
	}

	@Override
	public void onChanged(FractionInput widget, Fraction oldValue)
	{
		Log.d(TAG, "onChanged: " + oldValue + " -> " + widget.getValue());

		mDose = widget.getValue();
		getButton(BUTTON_POSITIVE).setEnabled(!mDose.isZero());
		mDoseText.setText(mDose.toString());
	}

	@Override
	public void onEntryUpdated(Entry entry, int flags)
	{
		Log.d(TAG, "onEntryUpdated: entry=" + entry);

		if(entry instanceof Drug && entry.getId() == mDrug.getId())
		{
			mDrug = (Drug) entry;

			if(mIsInInsufficientSupplyMode)
			{
				if(mDrug.getCurrentSupply().compareTo(mDose) != -1)
				{
					switchMode(false);
					return;
				}
			}

			updateCurrentMode();
		}
	}

	@Override
	public void onEntryCreated(Entry entry, int flags) {}

	@Override
	public void onEntryDeleted(Entry entry, int flags) {}

	private void handleOnClickInNormalMode(int which)
	{
		if(which == BUTTON_POSITIVE)
		{
			if(mDrug.getCurrentSupply().compareTo(mDose) == -1)
				switchMode(false);
			else
				addIntakeAndDismiss();
		}
	}

	private void handleOnClickInInsufficientSupplyMode(int which)
	{
		if(which == BUTTON_POSITIVE)
		{
			Context context = getContext();

			Intent intent = new Intent(context, DrugEditActivity.class);
			intent.setAction(Intent.ACTION_EDIT);
			intent.putExtra(DrugEditActivity.EXTRA_DRUG, mDrug);
			intent.putExtra(DrugEditActivity.EXTRA_FOCUS_ON_CURRENT_SUPPLY, true);

			context.startActivity(intent);
		}
		else if(which == BUTTON_NEGATIVE)
			addIntakeAndDismiss();
	}

	private void addIntakeAndDismiss()
	{
		Intake intake = new Intake(mDrug, mDate, mDoseTime, mDose);
		Database.create(intake);

		Fraction newSupply = mDrug.getCurrentSupply().minus(mDose);
		mDrug.setCurrentSupply(newSupply.isNegative() ? new Fraction() : newSupply);
		Database.update(mDrug);

		dismiss();

		Toast.makeText(getContext(), R.string._toast_intake_noted, Toast.LENGTH_SHORT).show();
	}

	private void switchMode(boolean dismissBeforeUpdate)
	{
		mIsInInsufficientSupplyMode = !mIsInInsufficientSupplyMode;

		if(dismissBeforeUpdate)
		{
			// dismissing the dialog and then showing it again makes for a
			// smoother user experience
			mDismissCalledFromSwitchMode = true;
			dismiss();
			mDismissCalledFromSwitchMode = false;
		}

		updateCurrentMode();

		if(dismissBeforeUpdate)
			show();
	}

	private void updateCurrentMode()
	{
		if(mIsInInsufficientSupplyMode)
		{
			setCustomViewVisibility(View.GONE);

			Context context = getContext();
			setMessage(context.getString(R.string._msg_insufficient_supplies,
					mDrug.getName(), mDose, mDrug.getCurrentSupply()));
			getButton(BUTTON_POSITIVE).setText(context.getString(R.string._btn_edit_drug));
			getButton(BUTTON_NEGATIVE).setText(context.getString(R.string._btn_ignore));
		}
		else
		{
			setupNormalMode();
		}
	}

	private void setupNormalMode()
	{
		boolean doseIsZero = mDose.isZero();

		setEditable(doseIsZero);

		getButton(BUTTON_POSITIVE).setText(getString(android.R.string.ok));
		getButton(BUTTON_NEGATIVE).setText(getString(android.R.string.cancel));

		Button b = getButton(BUTTON_POSITIVE);
		b.setEnabled(!doseIsZero);

		setupMessages();
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

	private void setCustomViewVisibility(int visibility)
	{
		View views[] = { mDoseEdit, mDoseText, mHintText };
		for(View v : views)
			v.setVisibility(visibility);
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
