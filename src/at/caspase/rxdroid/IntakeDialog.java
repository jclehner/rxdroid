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

import java.util.Date;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import at.caspase.rxdroid.FractionInput.OnChangedListener;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Entries;
import at.caspase.rxdroid.db.Entry;
import at.caspase.rxdroid.db.Intake;

public class IntakeDialog extends AlertDialog implements OnChangedListener, Database.OnChangeListener
{
	private static final String TAG = IntakeDialog.class.getName();
	private static final boolean LOGV = true;

	private Drug mDrug;
	private int mDoseTime;
	private Date mDate;

	private Fraction mDose;
	private final int mIntakeCount;

	private int mFlags;

	private TextView mDoseText;
	private TextView mHintText;
	private FractionInput mDoseEdit;

	private PopupWindow mPopup;

	private OnShowListener mOnShowListener;

	public static final int FLAG_ALLOW_DOSE_EDIT = 1;

	public IntakeDialog(Context context, Drug drug, int doseTime, Date date)
	{
		super(context);

		mDrug = drug;
		mDoseTime = doseTime;
		mDate = date;

		if(LOGV) Log.v(TAG, "<init>: doseTime=" + doseTime + ", date=" + date + ", drug=" + drug);

		mIntakeCount = Entries.findIntakes(drug, date, doseTime).size();

		if(mIntakeCount == 0)
			mDose = drug.getDose(doseTime, date);
		else
			mDose = new Fraction();

		View view = getLayoutInflater().inflate(R.layout.intake, null);

		view.findViewById(R.id.dose_container).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v)
			{
				setEditable(true);
			}
		});

		mDoseText = (TextView) view.findViewById(R.id.dose_text);
		mHintText = (TextView) view.findViewById(R.id.dose_hint);
		mDoseEdit = (FractionInput) view.findViewById(R.id.dose_edit);

		mDoseText.setText(mDose.toString());

		mDoseEdit.setValue(mDose);
		mDoseEdit.setAutoInputModeEnabled(true);
		mDoseEdit.setOnChangeListener(this);

		//setTitle(mDrug.getName());
		setView(view);

		setButton(BUTTON_NEGATIVE, getString(android.R.string.cancel), mLocalOnClickListener);
		// The actual listener for this button is added in mLocalOnShowListener!
		setButton(BUTTON_POSITIVE, getString(android.R.string.ok), (OnClickListener) null);

		setupMessages();

		super.setOnShowListener(mLocalOnShowListener);
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
	public void setOnShowListener(android.content.DialogInterface.OnShowListener listener) {
		mOnShowListener = listener;
	}

	@Override
	protected void onStop()
	{
		dismissPopup();
		Database.unregisterOnChangedListener(this);
	}

	@Override
	public void onFractionChanged(FractionInput widget, Fraction oldValue)
	{
		mDose = widget.getValue();
		getButton(BUTTON_POSITIVE).setEnabled(!mDose.isZero());
		mDoseText.setText(mDose.toString());
	}

	@Override
	public void onEntryUpdated(Entry entry, int flags)
	{
		if(entry instanceof Drug && entry.getId() == mDrug.getId())
		{
			mDrug = (Drug) entry;

			if(!hasInsufficientSupplies())
				dismissPopup();
		}
	}

	@Override
	public void onEntryCreated(Entry entry, int flags) {}

	@Override
	public void onEntryDeleted(Entry entry, int flags)
	{
		if(entry instanceof Drug && entry.getId() == mDrug.getId())
		{
			if(mPopup != null)
				dismissPopup();

			dismiss();
		}
	}

	@Override
	public void onBackPressed()
	{
		if(!isPopupShowing())
			super.onBackPressed();
		else
			dismissPopup();
	}

	private boolean hasInsufficientSupplies()
	{
		if(mDrug.getRefillSize() == 0)
			return false;

		Fraction supplies = mDrug.getCurrentSupply();
		return supplies.compareTo(mDose) == -1;
	}

	private void showPopup()
	{
		if(mPopup == null)
			setupPopupWindow();
		mPopup.showAtLocation(mDoseEdit, Gravity.CENTER, 0, 0);
	}

	private boolean isPopupShowing() {
		return mPopup == null ? false : mPopup.isShowing();
	}

	private void dismissPopup()
	{
		if(mPopup != null)
			mPopup.dismiss();
	}

	private void setupPopupWindow()
	{
		final Context context = getContext();

		View view = getLayoutInflater().inflate(R.layout.intake_popup, null, false);
		String okStr = context.getString(android.R.string.ok);
		String text = context.getString(R.string._msg_footer_insufficient_supplies,
				mDrug.getCurrentSupply(), okStr, mDrug.getName());

		TextView tv = (TextView) view.findViewById(R.id.text);
		tv.setText(text);
		tv.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(context, DrugEditActivity.class);
				intent.setAction(Intent.ACTION_EDIT);
				intent.putExtra(DrugEditActivity.EXTRA_DRUG, mDrug);
				intent.putExtra(DrugEditActivity.EXTRA_FOCUS_ON_CURRENT_SUPPLY, true);

				context.startActivity(intent);
			}
		});

		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();

		@SuppressWarnings("deprecation")
		int width  = display.getWidth()  * 8 / 10;
		@SuppressWarnings("deprecation")
		int height = display.getHeight() * 1 / 3;

		mPopup = new PopupWindow(
				view,
				width,
				height,
				false
		);

		mPopup.setOutsideTouchable(true);
		mPopup.setTouchable(true);
	}

	private void addIntakeAndDismiss()
	{
		Intake intake = new Intake(mDrug, mDate, mDoseTime, mDose);
		Database.create(intake);

		Fraction newSupply = mDrug.getCurrentSupply().minus(mDose);
		mDrug.setCurrentSupply(newSupply.isNegative() ? new Fraction() : newSupply);
		Database.update(mDrug, Database.FLAG_DONT_NOTIFY_LISTENERS);

		dismiss();

		Toast.makeText(getContext(), R.string._toast_intake_noted, Toast.LENGTH_SHORT).show();
	}

	private void setupViews()
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

	private final OnShowListener mLocalOnShowListener = new OnShowListener() {

		@Override
		public void onShow(final DialogInterface dialog)
		{
			setNonDismissingListener(BUTTON_POSITIVE);
			setNonDismissingListener(BUTTON_NEUTRAL);

			setupViews();

			if(mOnShowListener != null)
				mOnShowListener.onShow(dialog);
		}
	};

	private final OnClickListener mLocalOnClickListener = new OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			if(which == BUTTON_POSITIVE)
			{
				if(hasInsufficientSupplies() && !isPopupShowing())
					showPopup();
				else
					addIntakeAndDismiss();
			}
		}
	};

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
			mLocalOnClickListener.onClick(IntakeDialog.this, mButton);
		}
	}
}
