package at.jclehner.rxdroid.ui;

import android.view.View;
import android.view.ViewGroup;
import at.jclehner.rxdroid.DoseView;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.util.Constants;

public class ScheduleViewHolder
{
	private static final int[] DIVIDER_IDS = { R.id.divider1, R.id.divider2, R.id.divider3 };

	public ViewGroup doseContainer;
	public DoseView[] doseViews = new DoseView[4];
	public View[] dividers = new View[3];

	public void fillDoseViewsAndDividers(View layout)
	{
		doseContainer = (ViewGroup) layout.findViewById(R.id.dose_container);

		for(int i = 0; i != doseViews.length; ++i)
		{
			final int id = Constants.DOSE_VIEW_IDS[i];
			doseViews[i] = (DoseView) layout.findViewById(id);
		}

		for(int i = 0; i != dividers.length; ++i)
		{
			final int id = DIVIDER_IDS[i];
			dividers[i] = layout.findViewById(id);
		}
	}
}
