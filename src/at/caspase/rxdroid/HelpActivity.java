package at.caspase.rxdroid;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class HelpActivity extends Activity
{
	private static final String TAG = HelpActivity.class.getName();
	private static final boolean LOGV = false;

	private static final String EXTRA_HELP_IDS = TAG + ".EXTRA_HELP_IDS";
	private static final String KEY_DISPLAYED_HELP_IDS = "displayed_help_ids";

	private static final Set<Integer> sHelpIdQueue = new HashSet<Integer>();

	private TextView mTextHelp;

	public static void enqueue(int helpId) {
		sHelpIdQueue.add(helpId);
	}

	public static void showQueued()
	{
		final Set<String> displayedHelpIds = Settings.getStringSet(KEY_DISPLAYED_HELP_IDS);
		final HashSet<Integer> helpIdsToShow = new HashSet<Integer>();

		for(Integer helpId : sHelpIdQueue)
		{
			final String helpIdStr = Integer.toString(helpId);
			if(!displayedHelpIds.contains(helpIdStr))
			{
				helpIdsToShow.add(helpId);
				displayedHelpIds.add(helpIdStr);
			}
		}

		Settings.putStringSet(KEY_DISPLAYED_HELP_IDS, displayedHelpIds);
		sHelpIdQueue.clear();

		if(helpIdsToShow.isEmpty())
			return;

		final Context context = GlobalContext.get();
		final Intent intent = new Intent(context, HelpActivity.class);
		intent.putExtra(EXTRA_HELP_IDS, helpIdsToShow);
		context.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		if(Version.SDK_IS_HONEYCOMB_OR_LATER)
			setTheme(android.R.style.Theme_Holo_Dialog);
		else
			setTheme(android.R.style.Theme_Dialog);

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_help);

		mTextHelp = (TextView) findViewById(R.id.text_help);
	}
}
