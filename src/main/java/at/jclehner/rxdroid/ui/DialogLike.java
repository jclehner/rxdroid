/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2014 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

public abstract class DialogLike extends Fragment
{
	public static class SimpleDialogLike extends DialogLike
	{
		@Override
		public void onCreate(Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);
			setPositiveButtonText(0);
			setNegativeButtonText(0);
		}

		@Override
		public void onButtonClick(Button button, int which)
		{
			// won't be called unless a button text is set on any
			// button
			getActivity().finish();
		}
	}

	public static final int BUTTON_POSITIVE = DialogInterface.BUTTON_POSITIVE;
	public static final int BUTTON_NEGATIVE = DialogInterface.BUTTON_NEGATIVE;

	public static final int TEXT_DEFAULT = 0;
	public static final int TEXT_HTML = 1;
	public static final int TEXT_REFSTRING = 2;

	private TextView mTitle;
	private TextView mMessage;
	private TextView mDetail;
	private ImageView mIcon;

	private Button mPositiveBtn;
	private Button mNegativeBtn;

	public abstract void onButtonClick(Button button, int which);

	public DialogLike() {
		setArguments(new Bundle());
	}

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

		mTitle.setText(getArguments().getCharSequence("title"));
		mMessage.setText(getArguments().getCharSequence("message"));
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
			onButtonClick((Button) v, v == mPositiveBtn ? BUTTON_POSITIVE : BUTTON_NEGATIVE);
		}
	};
}
