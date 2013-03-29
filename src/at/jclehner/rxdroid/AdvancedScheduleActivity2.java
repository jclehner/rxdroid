package at.jclehner.rxdroid;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import at.jclehner.rxdroid.util.Components;

public class AdvancedScheduleActivity2 extends FragmentActivity
{
	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		Components.onCreateActivity(this, 0);
		setContentView(R.layout.activity_advanced_schedule);
	}
}
