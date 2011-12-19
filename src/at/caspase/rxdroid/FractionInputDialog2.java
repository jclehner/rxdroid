package at.caspase.rxdroid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.util.Log;

public class FractionInputDialog2 extends AlertDialog implements OnClickListener, FractionInput.OnChangedListener
{
	private static final String TAG = FractionInputDialog2.class.getName();
	
	public interface OnFractionSetListener
	{
		void onFractionSet(FractionInputDialog2 dialog, Fraction value);
	}
	
	private FractionInput mInput;
	private Fraction mValue;	
	private OnFractionSetListener mListener;

	public FractionInputDialog2(Context context, Fraction value, OnFractionSetListener listener)
	{
		super(context);
		
		mInput = new FractionInput(context, null);
		mInput.setOnChangeListener(this);
		mInput.setValue(value);
		
		mValue = value;
		
		setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok), this);
		setButton(BUTTON_NEGATIVE, context.getString(android.R.string.cancel), 
				(OnClickListener) null);
		
		setView(mInput);
	}
	
	public void setOnFractionSetListener(OnFractionSetListener listener) {
		mListener = listener;
	}
	
	public OnFractionSetListener getOnFractionSetListener() {
		return mListener;
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		if(which == BUTTON_POSITIVE)
		{
			Log.d(TAG, "onClick: mListener=" + mListener);
			if(mListener != null)
				mListener.onFractionSet(this, mValue);
		}
	}

	@Override
	public void onChanged(FractionInput widget, Fraction oldValue)
	{
		mValue = widget.getValue();
	}
}
