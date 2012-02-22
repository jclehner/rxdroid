package at.caspase.rxdroid.preferences;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import at.caspase.androidutils.MyDialogPreference;
import at.caspase.rxdroid.R;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Drug;

public class DrugNamePreference2 extends MyDialogPreference
{
	private EditText mEditText;
	private Button mBtnPositive;

	private String mOriginalName;
	private String mName;


	public DrugNamePreference2(Context context) {
		this(context, null);
	}

	public DrugNamePreference2(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setWidgetLayoutResource(0);
	}

	@Override
	public void setValue(Object value)
	{
		mOriginalName = mName = (String) value;
		setTitle(mName);
		Log.d("DrugNamePreference2", "Setting title to " + mName);
	}

	@Override
	public Object getValue() {
		return mName;
	}

	@Override
	public CharSequence getSummary() {
		return null;
	}

	@Override
	public CharSequence getDialogTitle() {
		return null;
	}

	@Override
	public void onDismiss(DialogInterface dialog)
	{
		super.onDismiss(dialog);
		mEditText = null;
		mBtnPositive = null;
	}

	@Override
	protected View onCreateDialogView()
	{
		mEditText = new EditText(getContext());
		mEditText.setText(mName);
		mEditText.addTextChangedListener(mWatcher);
		return mEditText;
	}

	@Override
	protected void onShowDialog(Dialog dialog)
	{
		dialog.setOnShowListener(new OnShowListener() {

			@Override
			public void onShow(DialogInterface dialog) {
				mBtnPositive = ((AlertDialog) dialog).getButton(Dialog.BUTTON_POSITIVE);
			}
		});
	};

	@Override
	protected int getSoftInputMode()
	{
		return WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE |
				WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult)
	{
		if(positiveResult)
		{
			String newText = mEditText.getText().toString();

			if(callChangeListener(newText))
			{
				setValue(newText);
				if(shouldPersist())
					persistString(newText);
			}
		}
	}

	private boolean isUniqueDrugName(String name)
	{
		for(Drug drug : Database.getAll(Drug.class))
		{
			if(name.equals(drug.getName()))
				return false;
		}

		return true;
	}

	private final TextWatcher mWatcher = new TextWatcher() {

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

		@Override
		public void afterTextChanged(Editable s)
		{
			String name = s.toString();
			if(!name.equals(mOriginalName) && !isUniqueDrugName(name))
			{
				mEditText.setError(getContext().getString(R.string._msg_err_non_unique_drug_name));
				mBtnPositive.setEnabled(false);
			}
			else
			{
				mEditText.setError(null);
				mBtnPositive.setEnabled(true);
			}
		}
	};
}
