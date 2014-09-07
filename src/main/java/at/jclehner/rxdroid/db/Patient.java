/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2014 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. Additional terms apply (see LICENSE).
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

package at.jclehner.rxdroid.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "patients")
public class Patient extends Entry implements Comparable<Patient>
{
	public static final int DEFAULT_PATIENT_ID = 1;

	@DatabaseField(unique = true)
	private String name;

	public Patient() {
		id = DEFAULT_PATIENT_ID;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName()
	{
		if(name == null && id != DEFAULT_PATIENT_ID)
			throw new IllegalStateException();

		return name;
	}

	public boolean isDefaultPatient() {
		return name == null;
	}

	@Override
	public boolean equals(Object other)
	{
		if(other == null || !(other instanceof Patient))
			return false;

		if(name == null)
			return ((Patient) other).name == null;

		return name.equals(((Patient) other).name);
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

	@Override
	public String toString() {
		return isDefaultPatient() ? "(default)" : name;
	}

	@Override
	public int compareTo(Patient another) {
		return name.compareTo(another.name);
	}
}
