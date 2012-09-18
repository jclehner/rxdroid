package at.caspase.rxdroid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class HelpActivity extends Activity
{
	private static final String TAG = HelpActivity.class.getName();
	private static final boolean LOGV = false;

	private static final String EXTRA_HELP_IDS = TAG + ".EXTRA_HELP_IDS";
	private static final String KEY_DISPLAYED_HELP_IDS = "displayed_help_ids";

	private static final Set<Integer> sHelpIdQueue = new HashSet<Integer>();

	private TextView mTextHelp;
	private Button mBtnNext;
	private Button mBtnBack;

	private List<Integer> mHelpIds;
	private int mCurrentHelpIndex;

	public static void enqueue(int helpId) {
		sHelpIdQueue.add(helpId);
	}

	public static void showQueued()
	{
		Set<String> displayedHelpIds = Settings.getStringSet(KEY_DISPLAYED_HELP_IDS);
		displayedHelpIds = new HashSet<String>();
		final HashSet<Integer> helpIdSet = new HashSet<Integer>();

		for(Integer helpId : sHelpIdQueue)
		{
			final String helpIdStr = Integer.toString(helpId);
			if(!displayedHelpIds.contains(helpIdStr))
			{
				helpIdSet.add(helpId);
				displayedHelpIds.add(helpIdStr);
			}
		}

		Settings.putStringSet(KEY_DISPLAYED_HELP_IDS, displayedHelpIds);
		sHelpIdQueue.clear();

		if(helpIdSet.isEmpty())
			return;

		final Context context = GlobalContext.get();
		final Intent intent = new Intent(context, HelpActivity.class);
		intent.putExtra(EXTRA_HELP_IDS, new ArrayList<Integer>(helpIdSet));
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	@SuppressWarnings("unchecked")
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
		mBtnNext = (Button) findViewById(R.id.btn_next);
		mBtnBack = (Button) findViewById(R.id.btn_back);

		mBtnNext.setOnClickListener(mNavigationListener);
		mBtnBack.setOnClickListener(mNavigationListener);

		mHelpIds = (List<Integer>) getIntent().getSerializableExtra(EXTRA_HELP_IDS);
		if(mHelpIds == null || mHelpIds.isEmpty())
			finish();
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		showHelpIndex(mCurrentHelpIndex);
	}

	private void updateButtonStates()
	{
		if(mCurrentHelpIndex == 0)
			mBtnBack.setText(android.R.string.cancel);
		else
			mBtnBack.setText(R.string._btn_back);

		if(mCurrentHelpIndex == mHelpIds.size() - 1)
			mBtnNext.setText(android.R.string.ok);
		else
			mBtnNext.setText(R.string._btn_next);
	}

	@TargetApi(11)
	private void updateTitle()
	{
		final String title = getString(R.string._title_help);
		final String subtitle = getString(R.string._title_n_of_n,
				mCurrentHelpIndex + 1,
				mHelpIds.size()
		);

		if(Version.SDK_IS_HONEYCOMB_OR_LATER)
		{
			final ActionBar ab = getActionBar();
			if(ab != null)
			{
				ab.setTitle(title);
				ab.setSubtitle(subtitle);
				return;
			}
		}

		setTitle(title + " (" + subtitle + ")");
	}

	private void showHelpIndex(int index)
	{
		// TODO maybe add some fancy animation?
		mTextHelp.setText(mHelpIds.get(index));
		mCurrentHelpIndex = index;
		updateButtonStates();
		updateTitle();
	}

	private final OnClickListener mNavigationListener = new OnClickListener() {

		@Override
		public void onClick(View v)
		{
			switch(v.getId())
			{
				case R.id.btn_back:

					if(mCurrentHelpIndex == 0)
						finish();
					else
						showHelpIndex(mCurrentHelpIndex - 1);

					break;

				case R.id.btn_next:

					if(mCurrentHelpIndex == mHelpIds.size() - 1)
						finish();
					else
						showHelpIndex(mCurrentHelpIndex + 1);

					break;

				default:
					;
			}
		}
	};
}
