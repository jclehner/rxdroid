package at.caspase.rxdroid;

import at.caspase.rxdroid.Database.Drug;
import at.caspase.rxdroid.Database.Intake;

/**
 * Notifies objects of database changes.
 * 
 * Objects implementing this interface and registering themselves with
 * Database.addWatcher will be notified upon any changes to the database,
 * as long as they are handled by the functions in Database.
 * @see Database#create
 * @see Database#update
 * @see Database#delete
 * @see Database#dropDatabase
 * @author Joseph Lehner
 *
 */
public interface DatabaseWatcher
{
	public void onDrugCreate(Drug drug);
	
	public void onDrugDelete(Drug drug);
	
	public void onDrugUpdate(Drug drug);
	
	public void onIntakeCreate(Intake intake);
	
	public void onIntakeDelete(Intake intake);
	
	public void onDatabaseDropped();	
}
