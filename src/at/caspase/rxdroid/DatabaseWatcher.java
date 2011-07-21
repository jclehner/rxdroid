package at.caspase.rxdroid;

import at.caspase.rxdroid.Database.Drug;
import at.caspase.rxdroid.Database.Intake;

public interface DatabaseWatcher
{
	public void onDrugCreate(Drug drug);
	
	public void onDrugDelete(Drug drug);
	
	public void onDrugUpdate(Drug drug);
	
	public void onIntakeCreate(Intake intake);
	
	public void onIntakeDelete(Intake intake);
	
	public void onDatabaseDropped();	
}
