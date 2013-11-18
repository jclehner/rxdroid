package at.jclehner.rxdroid.widget;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.widget.TextView;

public class HtmlTextView extends TextView
{
	private CharSequence mHtmlSource;

	public HtmlTextView(Context context) {
		super(context);
	}

	public HtmlTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public HtmlTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void setText(CharSequence text, BufferType type)
	{
		mHtmlSource = text;
		if(!isInEditMode())
			super.setText(Html.fromHtml(text.toString()), type);
		else
			super.setText(text, type);
	}

	@Override
	public CharSequence getText() {
		return mHtmlSource;
	}
}
