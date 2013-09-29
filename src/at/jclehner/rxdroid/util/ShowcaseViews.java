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

	public void add(ShowcaseView sv)
	{
		if(sv.getVisibility() == View.VISIBLE)
		{
			sv.setOnShowcaseEventListener(mListener);
			mViews.add(sv);
		}
		else
			Log.d("ShowcaseViews", "add: not adding; view might be redundant!");
	}

	public boolean hasViews() {
		return !mViews.isEmpty();
	}

	public void show()
	{
		if(hasViews())
			mViews.get(0).show();
	}

	private final OnShowcaseEventListener mListener = new OnShowcaseEventListener() {

		@Override
		public void onShowcaseViewShow(ShowcaseView showcaseView)
		{

		}

		@Override
		public void onShowcaseViewHide(ShowcaseView showcaseView)
		{
			mViews.remove(0);
			show();
		}
	};

}
