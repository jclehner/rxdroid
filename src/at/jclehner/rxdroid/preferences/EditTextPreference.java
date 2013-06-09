package at.jclehner.rxdroid.preferences;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.Window;
import android.view.WindowManager;

public class EditTextPreference extends android.preference.EditTextPreference
{
	public EditTextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void showDialog(Bundle bundle)
	{
		super.showDialog(bundle);

		final Dialog dialog = getDialog();
		if(dialog != null)
		{
			Window window = dialog.getWindow();
			window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE |
					WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		}
	}
}