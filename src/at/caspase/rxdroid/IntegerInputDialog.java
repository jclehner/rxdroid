package at.caspase.rxdroid;

import android.content.Context;

public class IntegerInputDialog extends FractionInputDialog
{
	public interface OnIntegerSetListener
	{
		void onIntegerSet(IntegerInputDialog dialog, int value);
	}

	private int mValue;
	private OnIntegerSetListener mListener;

	public IntegerInputDialog(Context context, int value, OnIntegerSetListener listener)
	{
		super(context, new Fraction(value), null);

		mValue = value;
		mListener = listener;

		setOnFractionSetListener(mFractionSetListener);
		mInput.disableFractionInputMode(true);
	}

	public void setOnIntegerSetListener(OnIntegerSetListener listener) {
		mListener = listener;
	}

	public OnIntegerSetListener getOnIntegerSetListener() {
		return mListener;
	}

	public void setIntValue(int value)
	{
		mValue = value;
		super.setValue(new Fraction(mValue));
	}

	public int getIntValue() {
		return mValue;
	}

	@Override
	public Fraction getValue() {
		return new Fraction(mValue);
	}

	@Override
	public final void setOnFractionSetListener(OnFractionSetListener listener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final OnFractionSetListener getOnFractionSetListener() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void setValue(Fraction value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void setFractionInputMode(int mode) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void setAutoInputModeEnabled(boolean enabled) {
		throw new UnsupportedOperationException();
	}

	private final OnFractionSetListener mFractionSetListener = new OnFractionSetListener() {

		@Override
		public void onFractionSet(FractionInputDialog dialog, Fraction value)
		{
			if(!value.isInteger())
				throw new RuntimeException("Expected integer, got " + value);

			mValue = value.intValue();
		}
	};
}
