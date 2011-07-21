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

import com.j256.ormlite.dao.Dao;

/**
 * List item view for DrugListActivity.
 *  
 * @author caspase
 *
 */
public class DrugView extends RelativeLayout implements DatabaseWatcher
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
		
		onDrugUpdate(drug);		
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
	public void onDrugUpdate(Drug drug) 
	{
		mDrugName.setText(drug.getName());
		// TODO set the correct icon
	}

	@Override
	public void onDrugCreate(Drug drug) {}
	
	@Override
	public void onDrugDelete(Drug drug) {}
	
	@Override
	public void onIntakeCreate(Intake intake) {}
	
	@Override
	public void onIntakeDelete(Intake intake) {}
	
	@Override
	public void onDatabaseDropped() {}
}
