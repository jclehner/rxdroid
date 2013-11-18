package at.jclehner.rxdroid.widget;


import android.content.Context;
import android.util.AttributeSet;

import at.jclehner.androidutils.RefString;

public class RefResolvingTextView extends HtmlTextView
{
	public RefResolvingTextView(Context context) {
		super(context);
	}

	public RefResolvingTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public RefResolvingTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void setText(CharSequence text, BufferType type) {
		super.setText(RefString.resolve(getContext(), text), type);
	}
}
