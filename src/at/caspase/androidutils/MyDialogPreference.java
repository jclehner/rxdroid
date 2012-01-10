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
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import at.caspase.rxdroid.preferences.StateSaver;
import at.caspase.rxdroid.preferences.StateSaver.SaveState;


/**
 * Improved DialogPreference class.
 * <p>
 * This class, while retaining all functionality of the original DialogPreference,
 * allows for more customized dialogs. 
 * 
 * 
 * @author Joseph Lehner
 *
 */
@SaveState
public class MyDialogPreference extends DialogPreference implements
	OnActivityDestroyListener, OnDismissListener
{
	private static final String TAG = MyDialogPreference.class.getName();
	
	@SaveState
	private CharSequence mNeutralButtonText;
	@SaveState
	private int mSoftInputMode = 0;	
	
	private Dialog mDialog;
	private OnDismissListener mDialogOnDismissListener;	
	
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
	
	/**
	 * Returns a custom dialog, if present.
	 * <p>
	 * Note that if overriding this function, you must do all
	 * initialization before returning. If this function returns
	 * something other than <code>null</code>, 
	 * {@link #onPrepareDialogBuilder(android.app.AlertDialog.Builder)}
	 * will never be called. Also note that an OnDismissListener
	 * set on the returned dialog will be overridden in {@link #showDialog(Bundle)}!
	 * <p>
	 * Also note that dialog properties returned by functions such as 
	 * {@link #getDialogTitle()} will <em>not</em> be automatically
	 * applied if using a custom dialog.
	 * 
	 * @return the custom dialog, or <code>null</code> by default.
	 */
	protected Dialog onGetCustomDialog() {
		return null;
	}
		
	@Override
	protected void showDialog(Bundle state)
	{
		if(state != null)
			Log.w(TAG, "showDialog: ignoring non-null state");
		
		mDialog = onGetCustomDialog();
		if(mDialog == null)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
			onPrepareDialogBuilder(builder);
			mDialog = builder.create();	
		}
		
		if(mSoftInputMode != 0)
		{
			Window window = mDialog.getWindow();
			window.setSoftInputMode(mSoftInputMode);			
		}
		
		mDialog.setOnDismissListener(this);
		mDialog.show();
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
