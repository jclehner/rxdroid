package at.jclehner.rxdroid.util;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.github.espiandev.showcaseview.ShowcaseView;
import com.github.espiandev.showcaseview.ShowcaseView.OnShowcaseEventListener;

public final class ShowcaseViews
{
	private static final String TAG = ShowcaseViews.class.getSimpleName();

	private final List<ShowcaseView> mViews = new ArrayList<ShowcaseView>();
	private static final WeakHashMap<Context, ShowcaseViews> sInstances =
			new WeakHashMap<Context, ShowcaseViews>();

	public static ShowcaseViews create(Context context)
	{
		ShowcaseViews ret = sInstances.get(context);
		if(ret == null)
		{
			sInstances.put(context, ret = new ShowcaseViews());
			Log.d(TAG, "create: returning existing instance");
		}
		else
			Log.d(TAG, "create: returning new instance");

		return ret;
	}

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
		{
			final ShowcaseView sv = mViews.get(0);
			if(sv.getParent() == null)
				sv.show();

			mViews.remove(0);
		}
	}

	private ShowcaseViews() {}

	private final OnShowcaseEventListener mListener = new OnShowcaseEventListener() {

		@Override
		public void onShowcaseViewShow(ShowcaseView showcaseView)
		{

		}

		@Override
		public void onShowcaseViewHide(ShowcaseView showcaseView)
		{
			//mViews.remove(0);
			show();
		}
	};

}
