package at.caspase.androidutils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.preference.PreferenceManager.OnActivityDestroyListener;
import android.util.AttributeSet;
import at.caspase.rxdroid.preferences.StateSaver;
import at.caspase.rxdroid.preferences.StateSaver.SaveState;

public class MyDialogPreference extends DialogPreference implements
	OnActivityDestroyListener, OnDismissListener
{
	@SaveState
	private CharSequence mNeutralButtonText;
	private Dialog mDialog;
	
	private static final String KEY_IS_SHOWING = "is_showing";
	
	public MyDialogPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public MyDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
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
	
	@Override
	public void onDismiss(DialogInterface dialog) {
		mDialog = null;
	}
	
	@Override
	public void onActivityDestroy()
	{
		if(mDialog != null)
			mDialog.dismiss();
	}
	
	protected Dialog onGetCustomDialog() {
		return null;
	}
		
	@Override
	protected void showDialog(Bundle state)
	{
		mDialog = onGetCustomDialog();
		if(mDialog == null)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
			onPrepareDialogBuilder(builder);
			mDialog = builder.create();	
		}
		
		mDialog.setOnDismissListener(this);
	}	

	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder)
	{
		super.onPrepareDialogBuilder(builder);
		
		if(mNeutralButtonText != null)
			builder.setNeutralButton(mNeutralButtonText, this);
	}
	
	@Override
	protected Parcelable onSaveInstanceState()
	{
		Parcelable superState = super.onSaveInstanceState();
		Bundle extras = new Bundle();
		extras.putBoolean(KEY_IS_SHOWING, getDialog() != null);
			
		return StateSaver.createInstanceState(this, superState, extras);		
	}
	
	@Override
	protected void onRestoreInstanceState(Parcelable state)
	{
		super.onRestoreInstanceState(StateSaver.getSuperState(state));
		StateSaver.restoreInstanceState(this, state);
		
		Bundle extras = StateSaver.getExtras(state);
		if(extras != null)
		{
			if(extras.getBoolean(KEY_IS_SHOWING, false))
				showDialog(null);
		}
	}
}
