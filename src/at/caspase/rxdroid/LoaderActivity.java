package at.caspase.rxdroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.DatabaseHelper.DbError;

public class LoaderActivity extends Activity implements OnClickListener
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loader);
		loadDatabaseAndLaunchMainActivity();
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		if(which == DialogInterface.BUTTON_POSITIVE)
		{
			Database.getHelper().reset(true);
			loadDatabaseAndLaunchMainActivity();
		}
		else if(which == DialogInterface.BUTTON_NEGATIVE)
			finish();
	}
	
	private void loadDatabaseAndLaunchMainActivity()
	{
		if(loadDatabase())
			launchMainActivity();
	}
	
	private boolean loadDatabase()
	{
		GlobalContext.set(getApplicationContext());
		
		try
		{			
			Database.load();
			return true;
		}
		catch(DbError e)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string._title_database_error);
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setCancelable(false);	
			
			StringBuilder sb = new StringBuilder();
			
			switch(e.getType())
			{
				case DbError.GENERAL:
					sb.append(getString(R.string._msg_db_error_general));
					break;
					
				case DbError.UPGRADE:
					sb.append(getString(R.string._msg_db_error_upgrade));
					break;
					
				case DbError.DOWNGRADE:
					sb.append(getString(R.string._msg_db_error_downgrade));
					break;
			}
			
			sb.append(getString(R.string._msg_db_error_footer));
			sb.append("\n");			
			sb.append(e.getMessage());		
			
			builder.setMessage(sb.toString());
			builder.setNegativeButton(android.R.string.cancel, this);
			builder.setPositiveButton(R.string._btn_backup_and_reset, this);
			
			builder.show();
		}
		
		return false;
	}
	
	private void launchMainActivity()
	{
		finish();
		
		Intent intent = new Intent(this, DrugListActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
		startActivity(intent);
	}
}
