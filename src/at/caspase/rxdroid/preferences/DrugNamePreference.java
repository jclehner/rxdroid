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

package at.caspase.rxdroid.preferences;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import at.caspase.rxdroid.R;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Drug;

public class DrugNamePreference extends EditTextPreference implements TextWatcher
{
	@SuppressWarnings("unused")
	private static final String TAG = DrugNamePreference.class.getName();

	private EditText mInput;
	private String mInitialName = null;

	public DrugNamePreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		mInput = getEditText();
		mInput.addTextChangedListener(this);

		setSummary(null);
	}

	public void setInitialName(String name)
	{
		if(name != null && name.length() != 0)
		{
			mInitialName = name;
			setName(mInitialName);
		}
	}

	public void setName(String name)
	{
		if(name != null)
		{
			setTitle(name);
			setText(name);
		}
		else
		{
			setTitle(R.string._title_drug_name);
			setText("");
		}
	}

	@Override
	public void afterTextChanged(Editable s)
	{
		if(!s.toString().equals(mInitialName) && !isUniqueDrugName(s.toString()))
			mInput.setError("Another drug with that name already exists!");
		else
			mInput.setError(null);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}

	@Override
	protected void showDialog(Bundle state)
	{
		super.showDialog(null);

		Dialog dialog = getDialog();
		if(dialog != null)
		{
			// this is required on devices with small screens that would otherwise squash
			// the
			Window window = dialog.getWindow();
			window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE |
					WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		}
	}

	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder)
	{
		super.onPrepareDialogBuilder(builder);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult)
	{
		if(positiveResult)
		{
			String name = mInput.getText().toString();
			boolean isUniqueName = isUniqueDrugName(name);

			if(name.length() == 0 || !isUniqueName)
			{
				if(name.equals(mInitialName))
					return;

				AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
				builder.setIcon(android.R.drawable.ic_dialog_alert);
				builder.setCancelable(false);
				builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						mInput.setSelectAllOnFocus(true);
						showDialog(null);
					}
				});

				if(name.length() == 0)
				{
					builder.setTitle(R.string._title_error);
					builder.setMessage(R.string._msg_err_empty_drug_name);
				}
				else if(!isUniqueDrugName(name))
				{
					builder.setTitle(name);
					builder.setMessage(R.string._msg_err_non_unique_drug_name);
				}

				builder.show();
				return;
			}
		}

		super.onDialogClosed(positiveResult);
	}

	private boolean isUniqueDrugName(String name)
	{
		for(Drug drug : Database.getDrugs())
		{
			if(name.equals(drug.getName()))
				return false;
		}

		return true;
	}
}
