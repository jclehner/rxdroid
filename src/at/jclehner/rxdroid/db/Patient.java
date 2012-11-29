package at.jclehner.rxdroid.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "patients")
public class Patient extends Entry implements Comparable<Patient>
{
	private static final long serialVersionUID = -7632154835094837404L;

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
	public int compareTo(Patient another) {
		return name.compareTo(another.name);
	}
}
