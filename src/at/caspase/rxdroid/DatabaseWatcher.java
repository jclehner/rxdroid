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
