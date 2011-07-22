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

import java.io.Serializable;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import at.caspase.rxdroid.Database.Drug;
import at.caspase.rxdroid.Database.Intake;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.Dao;

public class DrugListActivity extends OrmLiteBaseActivity<Database.Helper> implements DatabaseWatcher, OnLongClickListener
{    
    public static final String TAG = DrugListActivity.class.getName();
		
	public static final int MENU_ADD = Menu.FIRST;
	public static final int MENU_DELETE = MENU_ADD + 1;
	public static final int MENU_DEBUG_FILL = MENU_DELETE + 1;
		
	public static final String EXTRA_DAY = "day";
	public static final String EXTRA_CLEAR_FORGOTTEN_NOTIFICATION = "clear_forgotten_notification";
	
	private static final int TAG_ID = R.id.tag_drug_id;

	// stores the ListViews for the ViewFlipper.
	// 
	// 0 ... ListView for previous day
	// 1 ... ListView for current day
	// 2 ... ListView for next day
	
	private ListView mListView;
		
	private ViewSwitcher mViewSwitcher;
	private List<Database.Drug> mDrugs;
	private Date mDate;
		
	private Dao<Database.Drug, Integer> mDao;
	private Dao<Database.Intake, Integer> mIntakeDao;
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.drug_list); 
                           
        mDao = getHelper().getDrugDao();
        mIntakeDao = getHelper().getIntakeDao();
        mViewSwitcher = (ViewSwitcher) findViewById(R.id.drug_list_view_flipper);
                
        updateDrugList();
        
        Database.addWatcher(this);
                
        Intent serviceIntent = new Intent();
        serviceIntent.setClass(this, DrugNotificationService.class);
        
        startService(serviceIntent);
    }
    
    @Override
    protected void onStart()
    {
        super.onStart();
        
        
        // TODO currently, pressing the BACK key in DrugEditActivity will revert to 
        // DrugListActivity with the date set to today intstead of the last viewed date!
    }
    
    @Override
    protected void onResume()
    {
    	super.onResume();  	

        final Intent intent = getIntent();
        final String action = (intent != null) ? intent.getAction() : null;
                
        if(Intent.ACTION_VIEW.equals(action) || Intent.ACTION_MAIN.equals(action))
        {
        	final Date date = (Date) intent.getSerializableExtra(EXTRA_DAY);
            setDate(date);
        }
    	else
    		throw new IllegalArgumentException("Received invalid intent; action=" + intent.getAction());
        
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(R.id.notification_intake);
        
        if(intent.getBooleanExtra(EXTRA_CLEAR_FORGOTTEN_NOTIFICATION, false))
        	manager.cancel(R.id.notification_intake_forgotten);
    }
    
    @Override
    public void onDestroy() 
    {
    	super.onDestroy();
    	Database.removeWatcher(this);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
    	menu.add(0, MENU_ADD, 0, "Add").setIcon(android.R.drawable.ic_menu_add);
    	menu.add(0, MENU_DELETE, 0, "Delete").setIcon(android.R.drawable.ic_menu_delete);
    	menu.add(0, MENU_DEBUG_FILL, 0, "Fill DB").setIcon(android.R.drawable.ic_menu_agenda);
    	
    	return super.onCreateOptionsMenu(menu);  	
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	switch(item.getItemId()) 
    	{
    		case MENU_ADD:
    		{
    			Intent intent = new Intent(Intent.ACTION_INSERT);
    			intent.setClass(this, DrugEditActivity.class);
    			
    			startActivityForResult(intent, 0);
    			    			
    			return true;
    		}
    		case MENU_DELETE:
    			getHelper().dropTables();
    			return true;
    			
    		case MENU_DEBUG_FILL:
    		{
    			final String[] names = { "Rivaroxaban", "Propranolol", "Thiamazole", 
    					"2-(3,4,5-trimethoxyphenyl)ethanamine", "N-Acetyl-5-Methoxytryptamine" };
    			
    			for(String name : names)
    			{
    				Drug drug = new Drug();
    				drug.setName(name);
    				drug.setForm(Drug.FORM_TABLET);
    				
    				int doseTime = 0;
    				do
    				{
    					if(doseTime % 2 == 0)
    						drug.setDose(doseTime, Fraction.decode("1/2"));
    					
    				} while(++doseTime <= Drug.TIME_NIGHT);
    				
    				try
    				{
    					Database.create(mDao, drug);
    				}
    				catch(RuntimeException e)
    				{
    					// ignore
    				}
    			}    			
    		}
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	if(resultCode == RESULT_OK)
    		updateListView();
    }
       
    public void onDateChangeRequest(View view)
    {
    	Timer t = new Timer();
    	
    	switch(view.getId())
        {
            case R.id.med_list_footer:
                shiftDate(0);
                break;
            case R.id.med_list_prev:
                shiftDate(-1);
                break;
            case R.id.med_list_next:
            	shiftDate(+1);
                break;
            default:
            	throw new IllegalArgumentException("Unhandled view " + view.getClass().getSimpleName() + ", id=" + view.getId());
        }
    	
    	Log.d(TAG, "onDateChangeRequest: " + t);
    }
    
    public void onDrugNameClick(View view)
    {
    	Intent intent = new Intent(Intent.ACTION_EDIT);
    	intent.setClass(this, DrugEditActivity.class);
    	
    	try
    	{
    		Database.Drug drug = mDao.queryForId((Integer) view.getTag(TAG_ID));
    		intent.putExtra(DrugEditActivity.EXTRA_DRUG, (Serializable) drug);
    	}
    	catch(SQLException e)
    	{
    		throw new RuntimeException(e);
    	}
    	
    	startActivityForResult(intent, 0);    	
    }
    
    @Override
    public boolean onLongClick(View view)
    {
    	if(view.getId() == R.id.drug_name)
    	{
    		Toast.makeText(this, "Long click registered", Toast.LENGTH_SHORT).show();
    		return true;    	
    	}
    	return false;
    }
    
    public void onDoseClick(final View view)
    {
    	final DoseView v = (DoseView) view;
    	final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	final Database.Drug drug;
    	    	
    	try
    	{
    		drug = mDao.queryForId(v.getDrugId());
    	}
    	catch(SQLException e)
    	{
    		throw new RuntimeException(e);
    	}
    	
    	final int doseTime = v.getDoseTime();
    	final Fraction dose = drug.getDose(doseTime);
    	final Fraction newSupply = drug.getCurrentSupply().minus(dose);
    	
    	final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) 
			{
				if(which == AlertDialog.BUTTON_POSITIVE)
				{
					final Database.Intake intake = new Database.Intake(drug, mDate, doseTime);
					
					if(newSupply.compareTo(0) != -1)
						drug.setCurrentSupply(newSupply);
					else
						drug.setCurrentSupply(Fraction.ZERO);
					
					Database.create(mIntakeDao, intake);
					Database.update(mDao, drug);
					
					Toast.makeText(getApplicationContext(), "Dose intake noted.", Toast.LENGTH_SHORT).show();
				}
				else if(which == AlertDialog.BUTTON_NEUTRAL)
				{
					Intent intent = new Intent(Intent.ACTION_EDIT);
					intent.setClass(getBaseContext(), DrugEditActivity.class);
					intent.putExtra(DrugEditActivity.EXTRA_DRUG, (Serializable) drug);
					intent.putExtra(DrugEditActivity.EXTRA_FOCUS_ON_CURRENT_SUPPLY, true);
					
					startActivityForResult(intent, 0);
				}
			}
		};    	
    	
    	
    	if(newSupply.compareTo(0) == -1)
    	{
    		builder.setIcon(android.R.drawable.ic_dialog_alert);
    		builder.setTitle(drug.getName());
    		builder.setMessage("According to the database, the current supplies are not sufficient for this dose!");
    		builder.setPositiveButton("Ignore", onClickListener);
    		builder.setNeutralButton("Edit drug", onClickListener);
    		
    		builder.show();
    		
    		return;
    	}    	
    	
    	if(!dose.equals(Fraction.ZERO))
    	{
    		builder.setTitle(drug.getName() + ": " + drug.getDose(doseTime));
    		
    		boolean hasIntake = Database.getIntakes(mIntakeDao, drug, mDate, doseTime).size() != 0;
    		
        	if(!hasIntake)
        	{
        		builder.setMessage("Take the above mentioned dose now and press OK.");
        		builder.setPositiveButton("OK", onClickListener);
        		builder.setNegativeButton("Cancel", null);
        	}
        	else
        	{
        		builder.setMessage("You have already taken the above mentioned dose. Do you want to take it regardless?");
        		builder.setPositiveButton("Yes", onClickListener);
        		builder.setNegativeButton("No", null);
        	}
    	}
    	else
    	{
    		builder.setTitle(drug.getName());
    		builder.setMessage("No intake is scheduled at this time. Do you still want to take a dose?");
    		builder.setPositiveButton("Yes", onClickListener);
    		builder.setNegativeButton("No", null);
    		
    		// TODO we should ask the user how much he wants to take
    	}
    	
    	builder.setIcon(Util.getDoseTimeDrawableFromDoseViewId(view.getId()));    	    	
    	builder.show();
    }
        
    @Override
	public void onDrugCreate(Drug drug)
	{
		mDrugs.add(drug);
		((DrugAdapter) mListView.getAdapter()).notifyDataSetChanged();
		Log.d(TAG, "onDrugCreate: drug=" + drug);
	}

	@Override
	public void onDrugDelete(Drug drug)
	{
		mDrugs.remove(drug);
		((DrugAdapter) mListView.getAdapter()).notifyDataSetChanged();		
	}

	@Override
	public void onDrugUpdate(Drug drug)
	{
		for(int i = 0; i != mDrugs.size(); ++i)
		{
			if(mDrugs.get(i).getId() == drug.getId())
			{
				mDrugs.remove(i);
				mDrugs.add(i, drug);
			}
		}
		((DrugAdapter) mListView.getAdapter()).notifyDataSetChanged();
	}

	@Override
	public void onIntakeCreate(Intake intake) {}
	
	@Override
	public void onIntakeDelete(Intake intake) {}
	
	@Override
	public void onDatabaseDropped() {}
	        
    private void setDate(Date newDate) {
    	setOrShiftDate(0, newDate);
    }
    
    private void shiftDate(int shiftBy) {
    	setOrShiftDate(shiftBy, null);
    }
    
    // shift to previous (-1) or next(1) date. passing 0
    // will reset to specified date, or current date
    // if newDate is -1
    private void setOrShiftDate(int shiftBy, Date newDate)
    {
    	if(mViewSwitcher.getChildCount() != 0)   	
    		mViewSwitcher.removeAllViews();
    	
    	if(shiftBy == 0)
    	{
    		if(newDate == null)
    			mDate = Util.DateTime.today();
    		else
    			mDate = newDate;    		
    		
    		mViewSwitcher.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
    		mViewSwitcher.setOutAnimation(null);    		
    	}
    	else
    	{    		    		
    		final long shiftedTime = mDate.getTime() + shiftBy * Util.Constants.MILLIS_PER_DAY;
    		mDate.setTime(shiftedTime);
    		
    		if(shiftBy == 1)
	    	{
	    		mViewSwitcher.getInAnimation().setInterpolator(ReverseInterpolator.INSTANCE);
				mViewSwitcher.getOutAnimation().setInterpolator(ReverseInterpolator.INSTANCE);
	    	}
	    	else if(shiftBy == -1)
	    	{
	    		mViewSwitcher.getInAnimation().setInterpolator(null);
				mViewSwitcher.getOutAnimation().setInterpolator(null);
	    	}
	    	else
	    		throw new IllegalArgumentException();
    		    		
    		mViewSwitcher.addView(mListView);    		
    	}
        	
    	ListView newListView = new ListView(this);
    	newListView.setAdapter(new DrugAdapter(this, R.layout.dose_view, mDrugs, mDate));
    	    	
    	mViewSwitcher.addView(newListView);
    	mViewSwitcher.showNext();
    	
    	mListView = newListView;   	
    	
    	SpannableString dateString = new SpannableString(mDate.toString());
    	
    	if(mDate.equals(Util.DateTime.today()))
    	   	dateString.setSpan(new UnderlineSpan(), 0, dateString.length(), 0);
    	
    	((TextView) findViewById(R.id.med_list_footer)).setText(dateString);
    	
    	// update the intent so our Activity is restarted with the last opened date
    	setIntent(getIntent().putExtra(EXTRA_DAY, (Serializable) mDate));
    	
    	if(shiftBy == 0)
    	{
	    	mViewSwitcher.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
			mViewSwitcher.setOutAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));
    	}
    }
    
    private void updateDrugList()
    {
    	try
		{
			mDrugs = mDao.queryForAll();
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}	
    }
    
    private void updateListView()
    {
    	Timer t = new Timer();
    	
    	updateDrugList();
    	((DrugAdapter) mListView.getAdapter()).notifyDataSetChanged();
    	
    	Log.d(TAG, "updateListView: " + t);
    }
	
	private class DrugAdapter extends ArrayAdapter<Database.Drug>
	{
		private final Date mDate;
		
	    public DrugAdapter(Context context, int textViewResId, List<Database.Drug> items, Date date) 
	    {
	        super(context, textViewResId, items);
	        mDate = date;
	    }
	    
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent)
	    {
	    	View v = convertView;
	        
	        if(v == null)
	        {
	            LayoutInflater li = LayoutInflater.from(getContext());
	            v = li.inflate(R.layout.drug_view2, null);
	        }
	        
	        final Drug drug = getItem(position);
	        
	        final TextView drugName = (TextView) v.findViewById(R.id.drug_name);
	        drugName.setText(drug.getName());
	        drugName.setTag(TAG_ID, drug.getId());
	        
	        final ImageView drugIcon = (ImageView) v.findViewById(R.id.drug_icon);
	        drugIcon.setImageResource(drug.getFormResourceId());
 	        
	        final int doseViewIds[] = { R.id.morning, R.id.noon, R.id.evening, R.id.night };
	        for(int doseViewId : doseViewIds)
	        {
	        	DoseView doseView = (DoseView) v.findViewById(doseViewId);
	        	doseView.setDate(mDate);
	        	doseView.setDrug(drug);
	        	doseView.setDao(mIntakeDao);
	        }
	        		        
	        return v;
	    }	   
	}
	
	private enum ReverseInterpolator implements Interpolator
	{	
		INSTANCE;
		
		@Override
		public float getInterpolation(float f) {
			return Math.abs(f - 1f);			
		}		
	}
}
