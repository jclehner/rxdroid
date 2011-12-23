package at.caspase.androidutils;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager.OnActivityDestroyListener;
import android.util.AttributeSet;

public class MyDialogPreference extends DialogPreference implements
	OnActivityDestroyListener, OnDismissListener
{
	private CharSequence mNeutralButtonText;
	private Dialog mDialog;
	private boolean mIsCustomDialog = false;
	
	public MyDialogPreference(Context context, AttributeSet attrs, int defStyle) 
	{
		super(context, attrs, defStyle);
	}
	
	public void setNeutralButtonText(CharSequence text) {
		mNeutralButtonText = text;
	}
	
	public void setNeutralButtonText(int resId) {
		setNeutralButtonText(getContext().getString(resId));
	}
	
	public CharSequence getNeutralButtonText() {
		return mNeutralButtonText;
	}
	
	@Override
	public Dialog getDialog() {
		return mDialog;
	}
	
	protected Dialog onGetCustomDialog() {
		return null;
	}
	
	@Override
	protected void showDialog(Bundle state)
	{
		
	}
}
