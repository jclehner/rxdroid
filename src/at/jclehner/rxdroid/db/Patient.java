package at.jclehner.rxdroid.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "patients")
public class Patient extends Entry implements Comparable<Patient>
{
	private static final long serialVersionUID = -7632154835094837404L;

	public static final int DEFAULT_PATIENT_ID = 0;

	@DatabaseField(unique = true)
	private String name;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
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

		return name.equals(((Patient) other).name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public int compareTo(Patient another) {
		return name.compareTo(another.name);
	}
}
