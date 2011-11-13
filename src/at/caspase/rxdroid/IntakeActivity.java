package at.caspase.rxdroid;

import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Intake;

public class IntakeActivity extends Activity implements OnClickListener
{
	/**
	 * The drug's id. Required. Expected type: <code>int</code>.
	 */
	public static final String EXTRA_DRUG_ID = "drug_id";
	/**
	 * The dose time for the intake. Required. Expected type: <code>int</code>.
	 */
	public static final String EXTRA_DOSE_TIME = "dose_time";
	/**
	 * Dose date for the intake. Required. Expected type: <code>java.util.Date</code>.
	 */
	public static final String EXTRA_DATE = "date";
	/**
	 * Set this to <code>true</code> to allow editing the dose.
	 * <p>
	 * This only has an observable effect when the intake would have been considered
	 * a regular one, thus normally not presenting the user with an option to edit
	 * the dose.
	 */
	public static final String EXTRA_ALWAYS_EDIT_DOSE = "always_edit_dose";
	
	private TextView mMessage;
	private ViewStub mFractionPickerStub;
	private Button mBtnLeft;
	private Button mBtnRight;
	
	private Intent mIntent;
	
	private Drug mDrug;
	private int mDoseTime;
	private Date mDate;
	
	private Fraction mDose;
	
	private boolean mInInsufficientSupplyMode = false;
	private boolean mAlwaysEditDose;
		
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setTheme(android.R.style.Theme_Dialog);
		setContentView(R.layout.intake);
				
		mMessage = (TextView) findViewById(R.id.message);
		mFractionPickerStub = (ViewStub) findViewById(R.id.fraction_picker);
		mBtnLeft = (Button) findViewById(R.id.btn_left);
		mBtnRight = (Button) findViewById(R.id.btn_right);
		
		mMessage.setOnClickListener(this);
		mBtnLeft.setOnClickListener(this);
		mBtnRight.setOnClickListener(this);
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();
		
		mIntent = getIntent();
		
		if(mIntent == null)
			return;
		
		int drugId = mIntent.getIntExtra(EXTRA_DRUG_ID, -1);
		mDoseTime = mIntent.getIntExtra(EXTRA_DOSE_TIME, -1);
		mDate = (Date) mIntent.getSerializableExtra(EXTRA_DATE);
		mAlwaysEditDose = mIntent.getBooleanExtra(EXTRA_ALWAYS_EDIT_DOSE, false);
		
		if(drugId == -1 || mDoseTime == -1 || mDate == null)
			throw new IllegalArgumentException("Not all extras set");
		
		mDrug = Database.getDrug(drugId);
		mDose = mDrug.getDose(mDoseTime);
		
		if(mDose.isZero() || mAlwaysEditDose)
			setupNonStandardIntake();
		else
			setupStandardIntake();	
	}
	
	@Override
	public void onClick(View view)
	{
		if(view.getId() == R.id.btn_left)
		{
			final Fraction newSupply = mDrug.getCurrentSupply().minus(mDose);
			
			if(!mInInsufficientSupplyMode && newSupply.compareTo(0) == -1)
				setupInsufficientSupplies();			
			else
			{
				Intake intake = new Intake(mDrug, mDate, mDoseTime, mDose);
				Database.create(intake);
				
				if(newSupply.compareTo(0) == -1)
					mDrug.setCurrentSupply(new Fraction(0));
				else
					mDrug.setCurrentSupply(newSupply);
				
				Database.update(mDrug);
				
				Toast.makeText(this, R.string._toast_intake_noted, Toast.LENGTH_SHORT).show();
				finish();
			}			
		}
		else if(view.getId() == R.id.btn_right)
		{
			if(mInInsufficientSupplyMode)
			{
				Intent intent = new Intent(this, DrugEditActivity.class);
				intent.setAction(Intent.ACTION_EDIT);
				intent.putExtra(DrugEditActivity.EXTRA_DRUG, mDrug);
				startActivity(intent);				
			}
			else
				finish();
		}
	}
	
	private void setupStandardIntake()
	{
		setupStandardButtons();
		
		mMessage.setText(R.string._msg_intake_normal);		
	}
	
	private void setupNonStandardIntake()
	{
		setupStandardButtons();
		
		mMessage.setText(R.string._msg_intake_unscheduled);		
	}
	
	private void setupInsufficientSupplies()
	{
		mBtnLeft.setText(R.string._btn_ignore);
		
		
		// TODO
	}
	
	private void setupStandardButtons()
	{
		mBtnLeft.setText(android.R.string.cancel);
		mBtnRight.setText(android.R.string.ok);
	}
	
	
	
	
	
	
}
