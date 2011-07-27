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

import java.sql.Date;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import at.caspase.rxdroid.Database.Drug;
import at.caspase.rxdroid.Database.Intake;
import at.caspase.rxdroid.Database.OnDatabaseChangedListener;

import com.j256.ormlite.dao.Dao;

/**
 * List item view for DrugListActivity.
 *  
 * @author Joseph Lehner
 *
 */
public class DrugView extends RelativeLayout implements OnDatabaseChangedListener
{
	private ImageView mDrugIcon;
	private TextView mDrugName;
	
	private DoseView mDoseViews[] = new DoseView[4];
		
	public DrugView(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		
		final LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.drug_view2, this, true);
		
		mDrugIcon = (ImageView) findViewById(R.id.drug_icon);
		mDrugName = (TextView) findViewById(R.id.drug_name);
		
		final int doseViewIds[] = { R.id.morning, R.id.noon, R.id.evening, R.id.night };
		
		for(int i = 0; i != mDoseViews.length; ++i)
			mDoseViews[i] = (DoseView) findViewById(doseViewIds[i]);
	}
	
	public void initialize(Drug drug, Date date, Dao<Intake, Integer> dao)
	{
		for(DoseView doseView : mDoseViews)
		{
			doseView.setDrug(drug);
			doseView.setDate(date);
			doseView.setDao(dao);
		}
		
		onUpdateEntry(drug);		
	}
	
	@Override
	public void onWindowVisibilityChanged(int visibility)
	{
		if(visibility != VISIBLE)
			Database.removeWatcher(this);
		else
			Database.addWatcher(this);
	}
		
	@Override
	public void onUpdateEntry(Drug drug) 
	{
		mDrugName.setText(drug.getName());
		// TODO set the correct icon
	}

	@Override
	public void onCreateEntry(Drug drug) {}
	
	@Override
	public void onDeleteEntry(Drug drug) {}
	
	@Override
	public void onCreateEntry(Intake intake) {}
	
	@Override
	public void onDeleteEntry(Intake intake) {}
	
	@Override
	public void onDatabaseDropped() {}
}
