package at.caspase.rxdroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import at.caspase.rxdroid.util.CollectionUtils;

public class LockscreenActivity extends Activity
{
	private static final String TAG = LockscreenActivity.class.getName();
	private static final int PIN_LENGTH = 4;

	public static final String EXTRA_UNLOCK_INTENT = TAG + ".EXTRA_UNLOCK_INTENT";

	private Button mBtnExit;
	private Button mBtnUnlock;

	private EditText[] mDigits = new EditText[PIN_LENGTH];

	private int mFocusedIndex = -1;

	private Intent mUnlockIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		setTheme(Theme.get());
		setContentView(R.layout.activity_lockscreen);

		mBtnExit = (Button) findViewById(R.id.btn_exit);
		mBtnExit.setOnClickListener(mButtonHandler);

		mBtnUnlock = (Button) findViewById(R.id.btn_unlock);
		mBtnUnlock.setOnClickListener(mButtonHandler);

		final int[] digitIds = { R.id.pin_digit1, R.id.pin_digit2, R.id.pin_digit3, R.id.pin_digit4 };
		for(int i = 0; i != mDigits.length; ++i)
		{
			mDigits[i] = (EditText) findViewById(digitIds[i]);
			mDigits[i].setOnFocusChangeListener(mFocusListener);
		}

		mUnlockIntent = (Intent) getIntent().getParcelableExtra(EXTRA_UNLOCK_INTENT);
		if(mUnlockIntent == null)
			throw new IllegalStateException("Missing EXTRA_UNLOCK_INTENT");

		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		if(!Application.isLocked())
		{
			Log.w(TAG, "onResume: application is not locked; activity should not have been created");
			launchActivityAndFinishSelf();
			return;
		}

		mDigits[0].requestFocus();
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if(mFocusedIndex == -1)
			return super.onKeyUp(keyCode, event);

		final char ch = digitKeyCodeToChar(keyCode);
		if(ch == DIGIT_INVALID)
		{
			mDigits[mFocusedIndex].setText("");
			return super.onKeyUp(keyCode, event);
		}

		mDigits[mFocusedIndex].setText(Character.toString(ch));
		if(mFocusedIndex + 1 != PIN_LENGTH)
			mDigits[mFocusedIndex + 1].requestFocus();
		else
		{
			//mBtnUnlock.requestFocus();
			mBtnUnlock.performClick();
		}

		return true;
	}

	public static void startMaybe(Activity caller) {
		startMaybe(caller, null);
	}

	public static void startMaybe(Activity caller, Intent unlockIntent)
	{
		if(/*Application.isLocked()*/ false)
		{
			if(unlockIntent == null)
			{
				unlockIntent = caller.getIntent();
				if(unlockIntent == null)
					throw new IllegalStateException("Intent is null in argument and calling activity");
			}

			final Intent intent = new Intent(caller.getApplicationContext(), LockscreenActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra(EXTRA_UNLOCK_INTENT, unlockIntent);

			caller.startActivity(intent);
			caller.finish();
		}
	}

	private void checkPinAndLaunchActivityIfOk()
	{
		final StringBuilder sb = new StringBuilder();
		for(EditText digit : mDigits)
			sb.append(digit.getText().toString());

		if(sb.length() > PIN_LENGTH)
			throw new IllegalStateException("Pin has unexpected length: " + sb);


		final String pin = Settings.getString("pin", null);
		if(pin == null || pin.equals(sb.toString()))
		{
			Application.unlock();
			launchActivityAndFinishSelf();
		}
		else
		{
			for(EditText digit : mDigits)
				digit.setText("");

			Toast.makeText(this, "Invalid PIN", Toast.LENGTH_SHORT).show();
		}
	}

	private void launchActivityAndFinishSelf()
	{
		startActivity(mUnlockIntent);
		finish();
	}

	private OnFocusChangeListener mFocusListener = new OnFocusChangeListener() {

		@Override
		public void onFocusChange(View v, boolean hasFocus)
		{
			if(hasFocus)
				mFocusedIndex = CollectionUtils.indexOf(v, mDigits);
			else
				mFocusedIndex = -1;
		}
	};

	private OnClickListener mButtonHandler = new OnClickListener() {

		@Override
		public void onClick(View v)
		{
			if(v.getId() == R.id.btn_exit)
				finish();
			else if(v.getId() == R.id.btn_unlock)
				checkPinAndLaunchActivityIfOk();
		}
	};

	private static final char DIGIT_INVALID = '?';

	private static char digitKeyCodeToChar(int keycode)
	{
		switch(keycode)
		{
			case KeyEvent.KEYCODE_0:
				return '0';

			case KeyEvent.KEYCODE_1:
				return '1';

			case KeyEvent.KEYCODE_2:
				return '2';

			case KeyEvent.KEYCODE_3:
				return '3';

			case KeyEvent.KEYCODE_4:
				return '4';

			case KeyEvent.KEYCODE_5:
				return '5';

			case KeyEvent.KEYCODE_6:
				return '6';

			case KeyEvent.KEYCODE_7:
				return '7';

			case KeyEvent.KEYCODE_8:
				return '8';

			case KeyEvent.KEYCODE_9:
				return '9';

			default:
				return DIGIT_INVALID;
		}
	}
}
