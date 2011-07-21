package at.caspase.rxdroid;


import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.TableUtils;

/**
 * All DB access goes here.
 * 
 * Note that all ORMLite related classes will have members prefixed without the
 * usual "m" (i.e. "comment" instead of "mComment").
 * 
 * @author caspase
 *
 */
public class Database 
{
	private static final String TAG = Database.class.getName();
	
	private static HashSet<DatabaseWatcher> sWatchers = new HashSet<DatabaseWatcher>();
	
	/**
	 * Add an object to the DatabseWatcher registry.
	 * 
	 * Whenever the methods create(), update(), or delete() are used, all
	 * objects that were registered using this method will have their
	 * callback functions called accordingly (see DatabaseWatcher).
	 * 
	 * @param watcher
	 */
	public static synchronized void addWatcher(DatabaseWatcher watcher) {
		sWatchers.add(watcher);
	}
	
	
	/**
	 * Removes an object from the DatabaseWatcher registry.
	 * 
	 * @param watcher
	 */
	public static synchronized void removeWatcher(DatabaseWatcher watcher) {
		sWatchers.remove(watcher);
	}
	
	public static <T, ID> void create(final Dao<T, ID> dao, final T t)
	{
		Thread th = new Thread(new Runnable() {
			
			@Override
			public void run()
			{
				try
				{
					dao.create(t);
				}
				catch (SQLException e)
				{
					throw new RuntimeException(e);
				}				
			}
		});
		
		th.start();
				
		if(t instanceof Drug)
		{
			for(DatabaseWatcher watcher : sWatchers)
				watcher.onDrugCreate((Drug) t);			
		}
		else if(t instanceof Intake)
		{
			for(DatabaseWatcher watcher : sWatchers)
				watcher.onIntakeCreate((Intake) t);
		}
	}
	
	public static <T, ID> void update(final Dao<T, ID> dao, final T t)
	{
		Thread th = new Thread(new Runnable() {
			
			@Override
			public void run()
			{
				try
				{
					dao.update(t);
				}
				catch (SQLException e)
				{
					throw new RuntimeException(e);
				}				
			}
		});
		
		th.start();
				
		if(t instanceof Drug)
		{
			for(DatabaseWatcher watcher : sWatchers)
				watcher.onDrugUpdate((Drug) t);			
		}
	}	
	
	public static <T, ID> void delete(final Dao<T, ID> dao, final T t)
	{
		Thread th = new Thread(new Runnable() {
			
			@Override
			public void run()
			{
				try
				{
					dao.delete(t);
				}
				catch (SQLException e)
				{
					throw new RuntimeException(e);
				}				
			}
		});
		
		th.start();
		
		if(t instanceof Drug)
		{
			for(DatabaseWatcher watcher : sWatchers)
				watcher.onDrugDelete((Drug) t);
		}
		else if(t instanceof Intake)
		{
			for(DatabaseWatcher watcher : sWatchers)
				watcher.onIntakeDelete((Intake) t);
		}
	}
	
	public static List<Intake> getIntakes(Dao<Intake, Integer> dao, Drug drug, long day, int doseTime)
	{		
		try
    	{    		
	    	QueryBuilder<Database.Intake, Integer> qb = dao.queryBuilder();
	    	Where<Database.Intake, Integer> where = qb.where();
	    	where.eq(Database.Intake.COLUMN_DRUG_ID, drug.getId());
	    	where.and();
	    	where.eq(Database.Intake.COLUMN_DAY, day);
	    	where.and();
	    	where.eq(Database.Intake.COLUMN_DOSE_TIME, doseTime);
    			        	
	    	return dao.query(qb.prepare());
    	}
    	catch(SQLException e)
    	{
    		throw new RuntimeException(e);
    	}    
		
	}
	
	private Database() {}
	
    public static abstract class Entry implements Serializable
    {
    	private static final long	serialVersionUID	= 8300191193261799857L;

		public static final String COLUMN_ID = "id";
		
		public boolean equals(Entry other) {
			throw new RuntimeException("Not implemented");
		}
    	
    	@DatabaseField(columnName = COLUMN_ID, generatedId = true)
    	protected int id;
    	
    	int getId() {
    		return id;
    	}
    }
        
	/**
	 * Class for handling the drug database.
	 * 
	 * The word "dose" in the context of this documentation refers to
	 * the smallest available dose of that drug without having to 
	 * manually reduce its amount (i.e. no pill-splitting). For example,
	 * a package of Aspirin containing 30 tablets contains 30 doses; of
	 * course, the intake schedule may also contain fractions (see {@link Fraction}) 
	 * of doses.
	 * 
	 * Any drug in the database will have the following attributes:
	 * <ul>
	 *  <li>A unique name</li>
	 *  <li>The form of the medication. This will be reflected in the UI by 
	 *      displaying a corresponding icon next to the drug's name.</li>
	 *  <li>The size of one refill. This corresponds to the amount of doses 
	 *      per prescription, package, etc. Note that due to the definition of
	 *      the word "dose" mentioned above, this size must not be a fraction.</li>
	 *  <li>The current supply. This contains the number of doses left for this particular drug.</li>
	 *  <li>An optional comment for that drug (e.g. "Take with food").</li>
	 * </ul>
	 *  
	 * 
	 * @author caspase
	 *
	 */
	@DatabaseTable(tableName = "drugs")
	public static class Drug extends Entry
	{
	    private static final long serialVersionUID = -2569745648137404894L;
		
	    public static final int FORM_TABLET = 0;
	    public static final int FORM_INJECTION = 1;
	    public static final int FORM_SPRAY = 2;
	    public static final int FORM_DROP = 3;
	    public static final int FORM_GEL = 4;
	    public static final int FORM_OTHER = 5;
	    
	    public static final int TIME_MORNING = 0;
	    public static final int TIME_NOON = 1;
	    public static final int TIME_EVENING = 2;
	    public static final int TIME_NIGHT = 3;
	    public static final int TIME_WHOLE_DAY = 4;
	    
	    public static final String COLUMN_NAME = "name";
	  	   	   
	    @DatabaseField(unique = true)
	    private String name;
	    
	    @DatabaseField(useGetSet = true)
	    private int form;
	    
	    @DatabaseField(defaultValue = "true")
	    private boolean active;
	    
	    // if mRefillSize == 0, mCurrentSupply should be ignored
	    @DatabaseField(useGetSet = true)
	    private int refillSize;
	    
	    @DatabaseField(dataType = DataType.SERIALIZABLE, useGetSet = true)
	    private Fraction currentSupply = new Fraction();
	    
	    @DatabaseField(dataType = DataType.SERIALIZABLE)
	    private Fraction doseMorning = new Fraction();
	    
	    @DatabaseField(dataType = DataType.SERIALIZABLE)
	    private Fraction doseNoon = new Fraction();
	    
	    @DatabaseField(dataType = DataType.SERIALIZABLE)
	    private Fraction doseEvening = new Fraction();
	    
	    @DatabaseField(dataType = DataType.SERIALIZABLE)
	    private Fraction doseNight = new Fraction();
	    
	    @DatabaseField(dataType = DataType.SERIALIZABLE)
	    private Fraction doseWholeDay = new Fraction();
	    
	    @DatabaseField(canBeNull = true)
	    private String comment;
	    
	    public Drug() {}
	    	    
	    public String getName() {
	        return name;
	    }
	    
	    public int getForm() {
	        return form;
	    }
	    
	    public boolean isActive() {
	    	return active;
	    }
	    
	    public int getRefillSize() {
	        return refillSize;
	    }
	    
	    public Fraction getCurrentSupply() {
	        return currentSupply;
	    }
	    
	    public Fraction[] getSchedule() {
	        return new Fraction[] { doseMorning, doseNoon, doseEvening, doseNight, doseWholeDay };
	    }
	    
	    public Fraction getDose(int doseTime) 
	    {	    	
	    	final Fraction doses[] = {
	                doseMorning,
	                doseNoon,
	                doseEvening,
	                doseNight,
	                doseWholeDay
	        };
	    	
	    	return doses[doseTime];
	    }
	    
	    public String getComment() {
	        return comment;
	    }
	    
	    public void setName(String name) {
	    	this.name = name;
	    }
	    
	    public void setForm(int form) 
	    {
	        if(form > FORM_OTHER)
	            throw new IllegalArgumentException();
	        this.form = form;
	    }
	    

		public void setActive(boolean active) {
			this.active = active;
		}
	    
	    public void setRefillSize(int refillSize)
	    {
	        if(refillSize < 0)
	            throw new IllegalArgumentException();
	        this.refillSize = refillSize;
	    }
	    
	    public void setCurrentSupply(Fraction currentSupply)
	    {
	        if(currentSupply == null)
	            this.currentSupply = Fraction.ZERO;
	        else if(currentSupply.compareTo(0) == -1)
	            throw new IllegalArgumentException();
	        
	        this.currentSupply = currentSupply;
	    }
	
	    public void setDose(int doseTime, Fraction value) 
	    {
	    	switch(doseTime)
	    	{
	    		case TIME_MORNING:
	    			doseMorning = value;
	    			break;
	    		case TIME_NOON:
	    			doseNoon = value;
	    			break;
	    		case TIME_EVENING:
	    			doseEvening = value;
	    			break;
	    		case TIME_NIGHT:
	    			doseNight = value;
	    			break;
	    		default:
	    			throw new IllegalArgumentException();
	    	}
	    }
	
	    public void setComment(String comment) {
	        this.comment = comment;
	    }	
	    
	    public boolean equals(Drug other)
	    {
	    	if(other == this)
	    		return true;
	    	
	    	final Object[] thisMembers = {
	    		this.name,
	    		this.form,
	    		this.doseMorning,
	    		this.doseNoon,
	    		this.doseEvening,
	    		this.doseNight,
	    		this.currentSupply,
	    		this.refillSize,
	    		this.comment	    		
	    	};
	    	
	    	final Object[] otherMembers = {
		    	other.name,
		    	other.form,
		    	other.doseMorning,
		    	other.doseNoon,
		    	other.doseEvening,
		    	other.doseNight,
		    	other.currentSupply,
		    	other.refillSize,
		    	other.comment	    		
		    };
	    	
	    	for(int i = 0; i != thisMembers.length; ++i)
	    	{
	    		if(!thisMembers[i].equals(otherMembers[i]))
	    			return false;
	    	}
	    	
	    	return true;	    	
	    }
	    
	    @Override
	    public String toString() {
	    	return name + "(" + id + ")={ " + doseMorning + " - " + doseNoon + " - " + doseEvening + " - " + doseNight + "}";
	    }
	}
	
	@DatabaseTable(tableName = "intake")
	public static class Intake extends Entry
	{
	    private static final long serialVersionUID = -9158847314588407608L;
		
		public static final String COLUMN_DRUG_ID = "drug_id";
		public static final String COLUMN_DAY = "day";
		public static final String COLUMN_TIMESTAMP = "timestamp";
		public static final String COLUMN_DOSE_TIME = "dose_time";
	    
		@DatabaseField(columnName = COLUMN_DRUG_ID, foreign = true)
		private Drug drug;
		
		@DatabaseField(columnName = COLUMN_DAY)
        private long day;
		
		@DatabaseField(columnName = COLUMN_TIMESTAMP)
        private long timestamp;
		
		@DatabaseField(columnName = COLUMN_DOSE_TIME)
        private int doseTime;
		
		public Intake() {}
		
		public Intake(Drug drug, long day, int doseTime) 
		{
			this.drug = drug;
			this.day = day;
			this.timestamp = System.currentTimeMillis();
			this.doseTime = doseTime;		
		}
		
		public Drug getDrug() {
			return drug;
		}

		public long getDay() {
			return day;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public int getDoseTime() {
			return doseTime;
		}

		public void setDrug(Drug drug) {
			this.drug = drug;
		}

		public void setDay(long day) {
			this.day = day;
		}

		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}

		public void setDoseTime(int doseTime) {
			this.doseTime = doseTime;
		}

    }
	
	public static class Helper extends OrmLiteSqliteOpenHelper
	{
		private static final String DB_NAME = "db.sqlite";
		private static final int DB_VERSION = 36;
		
		private Dao<Database.Drug, Integer> mDrugDao = null;
		private Dao<Database.Intake, Integer> mIntakeDao = null;
				
		public Helper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db, ConnectionSource cs) 
		{
		    Log.d("DatabaseHelper", "onCreate()");
			try
			{
				TableUtils.createTable(cs, Database.Drug.class);
				TableUtils.createTable(cs, Database.Intake.class);
				
			}
			catch(SQLException e)
			{
				throw new RuntimeException("Error while creating tables", e);
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, ConnectionSource cs, int oldVersion, int newVersion) 
		{
			try 
			{
				TableUtils.dropTable(cs, Database.Drug.class, true);
				TableUtils.dropTable(cs, Database.Intake.class, true);
				onCreate(db, cs);
			}
			catch (SQLException e) 
			{
				throw new RuntimeException("Error while deleting tables", e);
			}
		}
			
		public synchronized Dao<Database.Drug, Integer> getDrugDao()
		{
			try
			{
				if(mDrugDao == null)
					mDrugDao = BaseDaoImpl.createDao(getConnectionSource(), Database.Drug.class);
			}
			catch(SQLException e)
			{
				throw new RuntimeException("Cannot get DAO", e);
			}
			return mDrugDao;
		}
		
		public synchronized Dao<Database.Intake, Integer> getIntakeDao()
		{
			try
			{
				if(mIntakeDao == null)
					mIntakeDao = BaseDaoImpl.createDao(getConnectionSource(), Database.Intake.class);
			}
			catch(SQLException e)
			{
				throw new RuntimeException("Cannot get DAO", e);
			}
			return mIntakeDao;
		}
		
		@Override
		public void close()
		{
			super.close();
			mDrugDao = null;
			mIntakeDao = null;
		}
	}
}

