package at.jclehner.rxdroid;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;

import at.jclehner.rxdroid.util.Components;

public class DrugEditActivity2 extends ActionBarActivity
{
	//public static final String EXTRA_DRUG = "drug";
	public static final String EXTRA_DRUG_ID = "drug_id";
	public static final String EXTRA_FOCUS_ON_CURRENT_SUPPLY = "focus_on_current_supply";
	public static final String EXTRA_DISALLOW_DELETE = "disallow_delete";
	public static final String EXTRA_IS_FIRST_LAUNCH = "at.jclehner.rxroid.extras.IS_FIRST_LAUNCH";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Components.onCreateActivity(this, 0);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.simple_activity);

		if(savedInstanceState == null)
		{
			getSupportFragmentManager().beginTransaction().replace(android.R.id.content,
					new DrugEditFragment()).commit();
		}
	}

	@Override
	protected void onResume()
	{
		Components.onResumeActivity(this, 0);
		super.onResume();
	}
}
