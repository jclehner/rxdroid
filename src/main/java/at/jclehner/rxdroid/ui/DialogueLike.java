package at.jclehner.rxdroid.ui;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import at.jclehner.rxdroid.R;

public abstract class DialogueLike extends Fragment
{
	public static final int BUTTON_POSITIVE = DialogInterface.BUTTON_POSITIVE;
	public static final int BUTTON_NEGATIVE = DialogInterface.BUTTON_NEGATIVE;

	private TextView mTitle;
	private TextView mMessage;
	private TextView mDetail;
	private ImageView mIcon;

	private Button mPositiveBtn;
	private Button mNegativeBtn;

	private String mTitleText;
	private String mMessageText;

	private String mPosBtnText;
	private String mNegBtnText;

	public abstract void onButtonClick(View button, int which);

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setPositiveButtonText(android.R.string.ok);
		setNegativeButtonText(android.R.string.cancel);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		final View v = inflater.inflate(R.layout.layout_dialogue_like, container, false);

		mTitle = (TextView) v.findViewById(R.id.title);
		mMessage = (TextView) v.findViewById(R.id.message);
		mDetail = (TextView) v.findViewById(R.id.detail);
		mIcon = (ImageView) v.findViewById(R.id.icon);

		final int posBtn, negBtn;

		if(!isRtlLanguage())
		{
			posBtn = R.id.btn_right;
			negBtn = R.id.btn_left;
		}
		else
		{
			posBtn = R.id.btn_left;
			negBtn = R.id.btn_right;
		}

		mPositiveBtn = (Button) v.findViewById(posBtn);
		mNegativeBtn = (Button) v.findViewById(negBtn);

		mPositiveBtn.setOnClickListener(mBtnListener);
		mNegativeBtn.setOnClickListener(mBtnListener);

		applyArguments();

		return v;
	}

	public void setTitle(int resId, Object... formatArgs)
	{
		getArguments().putString("title", getString(resId, formatArgs));
		applyArguments();
	}

	public void setMessage(int resId, Object... formatArgs)
	{
		getArguments().putString("message", getString(resId, formatArgs));
		applyArguments();
	}

	public void setDetail(CharSequence detail)
	{
		getArguments().putCharSequence("detail", detail);
		applyArguments();
	}

	public void setIcon(int resId)
	{
		getArguments().putInt("icon", resId);
		applyArguments();
	}

	public void setNegativeButtonText(int resId)
	{
		getArguments().putString("neg", getString(resId));
		applyArguments();
	}

	public void setPositiveButtonText(int resId)
	{
		getArguments().putString("pos", getString(resId));
		applyArguments();
	}

	public Button getButton(int which)
	{
		if(which == BUTTON_POSITIVE)
			return mPositiveBtn;
		else if(which == BUTTON_NEGATIVE)
			return mNegativeBtn;

		throw new IllegalArgumentException();
	}

	private void applyArguments()
	{
		if(mTitle == null)
			return;

		mTitle.setText(getArguments().getString("title"));
		mMessage.setText(getArguments().getString("message"));
		mIcon.setImageResource(getArguments().getInt("icon"));
		mPositiveBtn.setText(getArguments().getString("pos"));
		mNegativeBtn.setText(getArguments().getString("neg"));

		final CharSequence detail = getArguments().getCharSequence("detail");
		mDetail.setVisibility(detail != null ? View.VISIBLE : View.GONE);
		mDetail.setText(detail);
	}

	@TargetApi(17)
	private boolean isRtlLanguage()
	{
		if(Build.VERSION.SDK_INT < 17)
			return false;

		return getActivity().getResources().getConfiguration().getLayoutDirection()
				== View.LAYOUT_DIRECTION_RTL;
	}

	private final View.OnClickListener mBtnListener = new View.OnClickListener() {
		@Override
		public void onClick(View v)
		{
			onButtonClick(v, v == mPositiveBtn ? BUTTON_POSITIVE : BUTTON_NEGATIVE);
		}
	};
}
