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

import java.sql.SQLException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import at.caspase.rxdroid.Database.Drug;
import at.caspase.rxdroid.EditFraction.FractionPickerDialog;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

/**
 * Edit a drug's database entry.
 * @author Joseph Lehner
 *
 */

public class DrugEditActivity extends OrmLiteBaseActivity<Database.Helper> implements TextWatcher, View.OnClickListener
{
	public static final String EXTRA_DRUG = "drug";
	public static final String EXTRA_FOCUS_ON_CURRENT_SUPPLY = "focus_on_current_supply";

	private static final String TAG = DrugEditActivity.class.getName();

	private EditText mTextName;
	private DoseView[] mDoses;
	private EditText mTextCurrentSupply;
	private EditText mTextRefillSize;
	private Spinner mDrugFormChooser;
	private int mDrugForm;
	private CheckBox mIsActive;

	// indicates whether a change was made to the drug we're editing
	private boolean mChanged = false;

	private Dao<Database.Drug, Integer> mDao;
	private Database.Drug mDrug;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		final Timer t = new Timer();

		setContentView(R.layout.drug_edit);

		Log.d(TAG, "onCreate: setContentView: " + t);

		mTextName = (EditText) findViewById(R.id.drug_name);
		mTextCurrentSupply = (EditText) findViewById(R.id.current_supply);
		mTextRefillSize = (EditText) findViewById(R.id.refill_size);
		mDrugFormChooser = (Spinner) findViewById(R.id.drug_form_chooser);
		mIsActive = (CheckBox) findViewById(R.id.drug_active);

		mTextName.addTextChangedListener(this);
		mTextCurrentSupply.addTextChangedListener(this);
		mTextRefillSize.addTextChangedListener(this);

		final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.drug_forms, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		mDrugFormChooser.setAdapter(adapter);
		mDrugFormChooser.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				mDrugForm = position;
				mChanged = true;
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				mDrugForm = Drug.FORM_TABLET;
			}
		});

		mDoses = new DoseView[4];

		final int[] viewIds = { R.id.morning, R.id.noon, R.id.evening, R.id.night };

		for(int i = 0; i != viewIds.length; ++i)
		{
			mDoses[i] = (DoseView) findViewById(viewIds[i]);
			mDoses[i].addTextChangedListener(this);
			mDoses[i].setOnClickListener(this);
		}

		mDao = getHelper().getDrugDao();
	}

	@Override
	public void onResume()
	{
		super.onResume();

		final Intent intent = getIntent();
		final String action = intent.getAction();

		if(Intent.ACTION_EDIT.equals(action))
		{
			mDrug = (Database.Drug) intent.getSerializableExtra(EXTRA_DRUG);

			if(mDrug == null)
				throw new IllegalStateException("ACTION_EDIT requires EXTRA_DRUG!");

			mTextName.setText(mDrug.getName());

			mTextCurrentSupply.setText(mDrug.getCurrentSupply().toString());
			mTextRefillSize.setText(Integer.toString(mDrug.getRefillSize()));
			mIsActive.setChecked(mDrug.isActive());
			mDrugFormChooser.setSelection(mDrug.getForm());

			final boolean focusOnCurrentSupply = intent.getBooleanExtra(EXTRA_FOCUS_ON_CURRENT_SUPPLY, false);
			if(focusOnCurrentSupply)
				mTextCurrentSupply.requestFocus();

			setTitle("Edit " + mDrug.getName());
		}
		else if(Intent.ACTION_INSERT.equals(action))
		{
			findViewById(R.id.delete_drug).setVisibility(View.GONE);
			setTitle("Add new drug");
		}
		else
			throw new RuntimeException("Unexpected intent action: " + action);

		for(DoseView v : mDoses)
		{
			if(mDrug != null)
				v.setDrug(mDrug);
		}

		mChanged = false;
		Log.d(TAG, "onResume: setting mChanged to false");
	}

	@Override
	public void onBackPressed()
	{
		final Intent intent = getIntent();
		final String action = intent.getAction();

		if(mDrug == null)
			mDrug = new Database.Drug();

		if(!verifyDrugName())
			return;

		mDrug.setForm(Database.Drug.FORM_TABLET);
		mDrug.setCurrentSupply(Fraction.decode(mTextCurrentSupply.getText().toString()));
		mDrug.setRefillSize(Integer.parseInt(mTextRefillSize.getText().toString(), 10));
		mDrug.setForm(mDrugForm);
		mDrug.setActive(mIsActive.isChecked());

		for(DoseView v : mDoses)
			mDrug.setDose(v.getDoseTime(), v.getDose());

		if(Intent.ACTION_EDIT.equals(action))
		{
			if(mChanged)
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Save changes");
				builder.setIcon(android.R.drawable.ic_dialog_info);
				builder.setMessage("Do you want to save changes made to this drug?");

				final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						if(which == AlertDialog.BUTTON_POSITIVE)
						{
							Log.d(TAG, "saving drug: " + mDrug);
							Database.update(mDao, mDrug);
							setResult(RESULT_OK);
							Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
						}

						finish();
					}
				};

				builder.setPositiveButton("Save", onClickListener);
				builder.setNegativeButton("Discard", onClickListener);

				builder.show();
				return;
			}
		}
		else if(Intent.ACTION_INSERT.equals(action))
		{
			Database.create(mDao, mDrug);
			setResult(RESULT_OK);
			Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
		}

		finish();
	}

	public void onDeleteDrug(View view)
	{
		if(mDrug != null)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setIcon(android.R.drawable.ic_dialog_info);
			builder.setTitle("Delete drug?");
			builder.setMessage("Are you sure that you want to delete " + mDrug.getName() + "?");

			builder.setPositiveButton("Yes", new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					Database.delete(mDao, mDrug);
					Toast.makeText(getApplicationContext(), "Deleted.", Toast.LENGTH_SHORT);
					finish();
				}
			});

			builder.setNegativeButton("No", null);
			builder.show();
		}
	}

	@Override
	public void afterTextChanged(Editable arg0)
	{
		mChanged = true;
		Log.d(TAG, "onTextChanged: arg0=" + arg0.toString());
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}

	@Override
	public void onClick(View v)
	{
		FractionPickerDialog dialog = new FractionPickerDialog(this, ((DoseView) v).getTextView());
		dialog.show();
	}

	private boolean isUniqueDrugName(String name)
	{
		QueryBuilder<Drug, Integer> qb = mDao.queryBuilder();
		Where<Drug, Integer> where = qb.where();
		try
		{
			where.eq(Database.Drug.COLUMN_NAME, name);
			return mDao.query(qb.prepare()).size() == 0;
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	private boolean verifyDrugName()
	{
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setTitle("Error");
		builder.setPositiveButton("OK", null);

		final String name = mTextName.getText().toString();

		if(name.length() != 0)
		{
			boolean haveNameCollision;

			if(!name.equals(mDrug.getName()))
				haveNameCollision = !isUniqueDrugName(name);
			else
				haveNameCollision = false;

			if(haveNameCollision)
				builder.setMessage("A drug with the specified name already exists in the database!");
			else
			{
				mDrug.setName(name);
				return true;
			}

		}
		else
		{
			builder.setMessage("The drug name must not be empty!");
			mTextName.setText(mDrug.getName());
			mTextName.selectAll();
		}

		builder.show();
		mTextName.requestFocus();

		return false;
	}






}
