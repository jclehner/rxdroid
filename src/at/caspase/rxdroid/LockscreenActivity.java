package at.caspase.rxdroid;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import at.caspase.rxdroid.util.CollectionUtils;

public class LockscreenActivity extends Activity implements OnClickListener
{
	private static final String TAG = LockscreenActivity.class.getName();

	private static final int PIN_LENGTH = 4;
	private static final String DIGITS = "0123456789";

	public static final String EXTRA_UNLOCK_INTENT = TAG + ".EXTRA_UNLOCK_INTENT";

	private Button[] mKeypadDigits = new Button[10];
	private EditText[] mPinDigits = new EditText[PIN_LENGTH];

	private int mDigitFocusIndex = -1;
	private Intent mUnlockIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		setTheme(Theme.get());
		setContentView(R.layout.activity_lockscreen);

		final int[] pinDigitIds = { R.id.pin_digit1, R.id.pin_digit2, R.id.pin_digit3, R.id.pin_digit4 };
		for(int i = 0; i != pinDigitIds.length; ++i)
			mPinDigits[i] = (EditText) findViewById(pinDigitIds[i]);

		final int[] keypadDigitIds = { R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
				R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
		};

		for(int i = 0; i != keypadDigitIds.length; ++i)
		{
			mKeypadDigits[i] = (Button) findViewById(keypadDigitIds[i]);
			mKeypadDigits[i].setOnClickListener(this);
		}

		findViewById(R.id.btn_clear).setOnClickListener(this);
		findViewById(R.id.btn_delete).setOnClickListener(this);

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

		findViewById(R.id.keypad).setVisibility(View.VISIBLE);

//		final Configuration config = getResources().getConfiguration();
//		final int visibility;
//
//		if(config.keyboard == Configuration.KEYBOARD_NOKEYS ||
//		   config.keyboard == Configuration.KEYBOARD_UNDEFINED)
//			visibility = View.VISIBLE;
//		else
//			visibility = View.GONE;
//
//		if(true) Log.v(TAG, config.toString());
//
//		findViewById(R.id.keypad).setVisibility(visibility);

		clearDigits();
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		switch(keyCode)
		{
			case KeyEvent.KEYCODE_0:
				onDigit(0);
				break;

			case KeyEvent.KEYCODE_1:
				onDigit(1);
				break;

			case KeyEvent.KEYCODE_2:
				onDigit(2);
				break;

			case KeyEvent.KEYCODE_3:
				onDigit(3);
				break;

			case KeyEvent.KEYCODE_4:
				onDigit(4);
				break;

			case KeyEvent.KEYCODE_5:
				onDigit(5);
				break;

			case KeyEvent.KEYCODE_6:
				onDigit(6);
				break;

			case KeyEvent.KEYCODE_7:
				onDigit(7);
				break;

			case KeyEvent.KEYCODE_8:
				onDigit(8);
				break;

			case KeyEvent.KEYCODE_9:
				onDigit(9);
				break;

			case KeyEvent.KEYCODE_DEL:
				clearLastDigit();
				break;

			default:
				return super.onKeyUp(keyCode, event);
		}

		return true;
	}

	@Override
	public void onClick(View v)
	{
		final int digit = CollectionUtils.indexOfByReference(v, mKeypadDigits);
		if(digit != -1)
		{
			onDigit(digit);
			return;
		}

		switch(v.getId())
		{
			case R.id.btn_clear:
				clearDigits();
				return;

			case R.id.btn_delete:
				clearLastDigit();
				return;
		}
	}

	public static void startMaybe(Activity caller) {
		startMaybe(caller, null);
	}

	public static void startMaybe(Activity caller, Intent unlockIntent)
	{
		if(Application.isLocked() && false) // currently disabled
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

	private void onDigit(int digit)
	{
		if(mDigitFocusIndex == -1 || mDigitFocusIndex == PIN_LENGTH)
			return;

		mPinDigits[mDigitFocusIndex].setText(DIGITS.substring(digit, digit + 1));
		if(++mDigitFocusIndex == PIN_LENGTH)
		{
			checkPinAndLaunchActivityIfOk();
		}
		else
			mPinDigits[mDigitFocusIndex].requestFocus();
	}

	private void clearDigits()
	{
		for(EditText digit : mPinDigits)
			digit.setText("");

		mDigitFocusIndex = 0;
		mPinDigits[0].requestFocus();
	}

	private void clearLastDigit()
	{
		if(mDigitFocusIndex == 0)
			return;

		--mDigitFocusIndex;

		mPinDigits[mDigitFocusIndex].setText("");
		mPinDigits[mDigitFocusIndex].requestFocus();
	}

	private void setKeypadEnabled(boolean enabled)
	{
		for(Button digit : mKeypadDigits)
			digit.setEnabled(enabled);

		findViewById(R.id.btn_delete).setEnabled(enabled);
		findViewById(R.id.btn_clear).setEnabled(enabled);
	}

	private void checkPinAndLaunchActivityIfOk()
	{
		try
		{
			final StringBuilder sb = new StringBuilder();
			for(EditText digit : mPinDigits)
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
				for(EditText digit : mPinDigits)
					digit.setText("");

				Toast.makeText(this, "Invalid PIN", Toast.LENGTH_SHORT).show();
			}
		}
		finally
		{
			setKeypadEnabled(true);
		}
	}

	private void launchActivityAndFinishSelf()
	{
		startActivity(mUnlockIntent);
		finish();
	}
}
