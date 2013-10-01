package at.jclehner.rxdroid.util;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import android.view.View;

import com.github.espiandev.showcaseview.ShowcaseView;
import com.github.espiandev.showcaseview.ShowcaseView.OnShowcaseEventListener;

public final class ShowcaseViews
{
	private final List<ShowcaseView> mViews = new ArrayList<ShowcaseView>();

	private OnShowcaseEventListener mListener;

	public void add(ShowcaseView sv)
	{
		if(sv.getVisibility() == View.VISIBLE)
		{
			sv.setOnShowcaseEventListener(mLocalListener);
			mViews.add(sv);
		}
		else
			Log.d("ShowcaseViews", "add: not adding; view might be redundant!");
	}

	public void setOnShowcaseEventListener(OnShowcaseEventListener l) {
		mListener = l;
	}

	public boolean hasViews() {
		return !mViews.isEmpty();
	}

	public void show()
	{
		if(hasViews())
		{
			final ShowcaseView sv = mViews.get(0);
			if(sv.getParent() == null)
				sv.show();

			mViews.remove(0);
		}
	}

	public ShowcaseViews() {}

	private final OnShowcaseEventListener mLocalListener = new OnShowcaseEventListener() {

		@Override
		public void onShowcaseViewShow(ShowcaseView showcaseView)
		{
			if(mListener != null)
				mListener.onShowcaseViewShow(showcaseView);
		}

		@Override
		public void onShowcaseViewHide(ShowcaseView showcaseView)
		{
			//mViews.remove(0);
			show();

			if(mListener != null)
				mListener.onShowcaseViewHide(showcaseView);
		}
	};

}
