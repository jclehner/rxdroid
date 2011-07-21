package at.caspase.rxdroid;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import at.caspase.rxdroid.Database.Drug;
import at.caspase.rxdroid.Database.Intake;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

public class DrugListActivity extends OrmLiteBaseActivity<Database.Helper> implements DatabaseWatcher, OnLongClickListener
{    
    public static final String TAG = DrugListActivity.class.getName();
		
	public static final int MENU_ADD = Menu.FIRST;
	public static final int MENU_DELETE = MENU_ADD + 1;
	public static final int MENU_DEBUG_FILL = MENU_DELETE + 1;
		
	public static final String EXTRA_DAY = "day";
	
	private static final int TAG_ID = R.id.tag_drug_id;

	// stores the ListViews for the ViewFlipper.
	// 
	// 0 ... ListView for previous day
	// 1 ... ListView for current day
	// 2 ... ListView for next day
	
	private ListView mListView;
		
	private ViewSwitcher mViewSwitcher;
	private List<Database.Drug> mDrugs;
	private long mDay;
		
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
            long day = intent.getLongExtra(EXTRA_DAY, Util.getMidnightMillisFromNow());
            setDate(day);
        }
    	else
    		throw new IllegalArgumentException("Received invalid intent; action=" + intent.getAction());
        
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(R.id.notification_intake);
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
    			
    			//mDbHelper.clearTable();
    			//refresh();
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
					final Database.Intake intake = new Database.Intake(drug, mDay, doseTime);
					
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
    		
    		boolean hasIntake = getIntakes(drug, doseTime).size() != 0;
    		
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
	        
    private void setDate(long newDate) {
    	setOrShiftDate(0, newDate);
    }
    
    private void shiftDate(int shiftBy) {
    	setOrShiftDate(shiftBy, -1);
    }
    
    // shift to previous (-1) or next(1) date. passing 0
    // will reset to specified date, or current date
    // if newDate is -1
    private void setOrShiftDate(int shiftBy, long newDate)
    {
    	if(mViewSwitcher.getChildCount() != 0)   	
    		mViewSwitcher.removeAllViews();
    	
    	if(shiftBy == 0)
    	{
    		if(newDate == -1)
    			mDay = Util.getMidnightMillisFromNow();
    		else if((newDate % Util.Constants.MILLIS_PER_DAY) != 0)
    		{
    			
    		    throw new IllegalArgumentException("Time must be 00:00:00");
    		}
    		else
    			mDay = newDate;
    		
    		
    		mViewSwitcher.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
    		mViewSwitcher.setOutAnimation(null);
    	}
    	else
    	{
    		final Animation inAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
    		final Animation outAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);    		
    		
    		mDay += shiftBy * Util.Constants.MILLIS_PER_DAY;
    		
    		if(shiftBy == 1)
	    	{
	    		// reverses the animations
	    		final Interpolator interpolator = new Interpolator() {
					
					@Override
					public float getInterpolation(float f) {
						return Math.abs(f - 1f);
					}
				};
	    		
	    		inAnimation.setInterpolator(interpolator);
	    		outAnimation.setInterpolator(interpolator);
	    	}
	    	else if(shiftBy != -1)
	    		throw new IllegalArgumentException();
    		    	    		    		    		    		
    		mViewSwitcher.setInAnimation(inAnimation);
    		mViewSwitcher.setOutAnimation(outAnimation);
    		
    		mViewSwitcher.addView(mListView);
    	}
    	
    	ListView newListView = new ListView(this);
    	newListView.setAdapter(new DrugAdapter(this, R.layout.dose_view, mDrugs, mDay));
    	
    	mViewSwitcher.addView(newListView);
    	mViewSwitcher.showNext();
    	
    	mListView = newListView;
    	
    	((TextView) findViewById(R.id.med_list_footer)).setText(Util.getDateString(mDay));
    	
    	// update the intent so our Activity is restarted with the last opened date
    	setIntent(getIntent().putExtra(EXTRA_DAY, mDay));
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
    
    private List<Database.Intake> getIntakes(Database.Drug drug, int doseTime)
    {
    	try
    	{    		
	    	QueryBuilder<Database.Intake, Integer> qb = mIntakeDao.queryBuilder();
	    	Where<Database.Intake, Integer> where = qb.where();
	    	where.eq(Database.Intake.COLUMN_DRUG_ID, drug.getId());
	    	where.and();
	    	where.eq(Database.Intake.COLUMN_DAY, mDay);
	    	where.and();
	    	where.eq(Database.Intake.COLUMN_DOSE_TIME, doseTime);
    			        	
	    	return mIntakeDao.query(qb.prepare());
    	}
    	catch(SQLException e)
    	{
    		throw new RuntimeException(e);
    	}    	
    }    
	
	private class DrugAdapter extends ArrayAdapter<Database.Drug>
	{
		private final long mDay;
		
	    public DrugAdapter(Context context, int textViewResId, List<Database.Drug> items, long day) 
	    {
	        super(context, textViewResId, items);
	        mDay = day;
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
	        	        	       	        
	        final int doseViewIds[] = { R.id.morning, R.id.noon, R.id.evening, R.id.night };
	        for(int doseViewId : doseViewIds)
	        {
	        	DoseView doseView = (DoseView) v.findViewById(doseViewId);
	        	doseView.setDay(mDay);
	        	doseView.setDrug(drug);
	        	doseView.setDao(mIntakeDao);
	        }
	        		        
	        return v;
	    }	   
	}

	

	
	
}
