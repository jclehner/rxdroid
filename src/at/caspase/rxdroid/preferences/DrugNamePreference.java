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

import java.sql.SQLException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;
import at.caspase.rxdroid.Database;
import at.caspase.rxdroid.R;
import at.caspase.rxdroid.Database.Drug;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

public class DrugNamePreference extends EditTextPreference implements TextWatcher
{
	private static final String TAG = DrugNamePreference.class.getName();
	
	private EditText mInput;
	private String mInitialName = null;
		
	private Database.Helper mDbHelper;
	private Dao<Drug, Integer> mDao;
	
	public DrugNamePreference(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		
		mInput = getEditText();
		mInput.addTextChangedListener(this);
				
		setSummary(null);
	}
	
	public void setInitialName(String name)
	{
		if(name != null && !name.isEmpty())
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
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder)
	{
		super.onPrepareDialogBuilder(builder);
		//builder.setCancelable(false);
		//builder.setNegativeButton(null, null);
		
		initDao();
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult)
	{
		if(positiveResult)
		{
			String name = mInput.getText().toString();
			boolean isUniqueName = isUniqueDrugName(name);
			
			if(name.isEmpty() || !isUniqueName)
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
				
				if(name.isEmpty())
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
		if(mDao == null)
		{
			Log.d(TAG, "isUniqueDrugName: mDao == null, returning true");
			return true;
		}
		
		QueryBuilder<Drug, Integer> qb = mDao.queryBuilder();
		Where<Drug, Integer> where = qb.where();
				
		try
		{
			where.eq(Database.Drug.COLUMN_NAME, name);
			return mDao.query(qb.prepare()).size() == 0;
		}
		catch (SQLException e)
		{
			Log.e(TAG, "isUniqueDrugName", e);
			return false;
		}
	}
	
	private void initDao()
	{
		// if we did this in the constructor, there'd be a noticeable lag when instantiating 
		// an object of this Preference
		if(mDao == null)
		{
			mDbHelper = new Database.Helper(getContext());
			mDao = mDbHelper.getDrugDao();
		}
	}
}
