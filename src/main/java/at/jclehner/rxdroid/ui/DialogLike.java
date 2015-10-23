/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2014 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. Additional terms apply (see LICENSE).
 *
 * RxDroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RxDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package at.jclehner.rxdroid.ui;

import android.annotation.TargetApi;
import android.support.v7.app.AppCompatActivity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import at.jclehner.rxdroid.BuildConfig;
import at.jclehner.rxdroid.R;

public class DialogLike extends Fragment
{
	public interface OnButtonClickListener
	{
		abstract void onButtonClick(DialogLike dialogLike, int which);
	}

	public static final int BUTTON_POSITIVE = DialogInterface.BUTTON_POSITIVE;
	public static final int BUTTON_NEGATIVE = DialogInterface.BUTTON_NEGATIVE;

	private TextView mTitle;
	private TextView mMessage;
	private TextView mDetail;
	private ImageView mIcon;

	private View mCustomView;

	private Button mPositiveBtn;
	private Button mNegativeBtn;
	private View mButtonBar;

	private boolean mAutoInitializeButtons = true;

	public DialogLike()
	{
		setArguments(new Bundle());
	}

	public void onButtonClick(DialogLike dialogLike, int which)
	{
		try
		{
			((OnButtonClickListener) getActivity()).onButtonClick(dialogLike, which);
		}
		catch(ClassCastException e)
		{
			throw new IllegalStateException("Fragment must override DialogLike.onButtonClick(...) or hosting Activity " +
					"must implement DialogLike.OnButtonClickListener");
		}
	}

	public void onBindCustomView(View view) {
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if(mAutoInitializeButtons)
		{
			setPositiveButtonText(android.R.string.ok);
			setNegativeButtonText(android.R.string.cancel);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		final View v = inflater.inflate(R.layout.layout_dialogue_like, container, false);

		mTitle = (TextView) v.findViewById(R.id.title);
		mMessage = (TextView) v.findViewById(R.id.message);
		mDetail = (TextView) v.findViewById(R.id.detail);
		mIcon = (ImageView) v.findViewById(R.id.icon);
		mButtonBar = v.findViewById(R.id.button_bar);

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

		final int customView = getArguments().getInt("custom_view");
		final FrameLayout customViewFrame = (FrameLayout) v.findViewById(R.id.custom);

		if(mCustomView != null)
			customViewFrame.addView(mCustomView);
		else if(customView != 0)
			mCustomView = inflater.inflate(customView, customViewFrame, true);

		if(mCustomView != null)
		{
			customViewFrame.setVisibility(View.VISIBLE);
			onBindCustomView(mCustomView);
		}

		applyArguments();

		return v;
	}

	public void setTitle(int resId, Object... formatArgs)
	{
		getArguments().putCharSequence("title", getString(resId, formatArgs));
		applyArguments();
	}

	public void setTitle(CharSequence title)
	{
		getArguments().putCharSequence("title", title);
		applyArguments();
	}

	public void setMessage(int resId, Object... formatArgs)
	{
		getArguments().putCharSequence("message", getString(resId, formatArgs));
		applyArguments();
	}

	public void setMessage(CharSequence message)
	{
		getArguments().putCharSequence("message", message);
		applyArguments();
	}

	public void setCustomView(int resId)
	{
		getArguments().putInt("custom_view", resId);
		applyArguments();
	}

	public void setCustomView(View view) {
		mCustomView = view;
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

	public void setNegativeButtonText(CharSequence text)
	{
		getArguments().putCharSequence("neg", text);
		applyArguments();
		mAutoInitializeButtons = false;
	}

	public void setNegativeButtonText(int resId) {
		setNegativeButtonText(getStringInternal(resId));
	}

	public void setPositiveButtonText(CharSequence text)
	{
		getArguments().putCharSequence("pos", text);
		applyArguments();
		mAutoInitializeButtons = false;
	}

	public void setPositiveButtonText(int resId) {
		setPositiveButtonText(getStringInternal(resId));
	}

	public Button getButton(int which)
	{
		if(which == BUTTON_POSITIVE)
			return mPositiveBtn;
		else if(which == BUTTON_NEGATIVE)
			return mNegativeBtn;

		throw new IllegalArgumentException();
	}

	private String getStringInternal(int resId) {
		return resId != 0 ? getString(resId) : null;
	}

	private void applyArguments()
	{
		if(mTitle == null)
			return;

		mTitle.setText(getArguments().getCharSequence("title"));
		mMessage.setText(getArguments().getCharSequence("message"));
		mIcon.setImageResource(getArguments().getInt("icon"));

		boolean showButtonBar = false;

		CharSequence btnText = getArguments().getCharSequence("pos");
		mPositiveBtn.setVisibility(btnText != null ? View.VISIBLE : View.GONE);
		mPositiveBtn.setText(btnText);

		showButtonBar |= btnText != null;

		btnText = getArguments().getCharSequence("neg");
		mNegativeBtn.setVisibility(btnText != null ? View.VISIBLE : View.GONE);
		mNegativeBtn.setText(btnText);

		showButtonBar |= btnText != null;

		mButtonBar.setVisibility(showButtonBar ? View.VISIBLE : View.GONE);

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
			onButtonClick(DialogLike.this, v == mPositiveBtn ? BUTTON_POSITIVE : BUTTON_NEGATIVE);
		}
	};
}
