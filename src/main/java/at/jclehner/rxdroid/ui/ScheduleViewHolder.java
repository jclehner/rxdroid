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

import android.view.View;
import android.view.ViewGroup;
import at.jclehner.rxdroid.DoseView;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.db.Schedule;
import at.jclehner.rxdroid.util.Constants;

public class ScheduleViewHolder
{
	private static final int[] DIVIDER_IDS = { R.id.divider1, R.id.divider2, R.id.divider3 };

	public ViewGroup doseContainer;
	public DoseView[] doseViews = new DoseView[Schedule.DOSE_TIME_COUNT];
	public View[] dividers = new View[Schedule.DOSE_TIME_COUNT-1];

	public void setDoseViewsFromLayout(View layout)
	{
		doseContainer = (ViewGroup) layout.findViewById(R.id.dose_container);

		for(int i = 0; i != doseViews.length; ++i)
		{
			final int id = Constants.DOSE_VIEW_IDS[i];
			doseViews[i] = (DoseView) layout.findViewById(id);
		}
	}

	public void setDividersFromLayout(View layout)
	{
		for(int i = 0; i != dividers.length; ++i)
		{
			final int id = DIVIDER_IDS[i];
			dividers[i] = layout.findViewById(id);
		}
	}

	public void setDoseViewsAndDividersFromLayout(View layout)
	{
		setDoseViewsFromLayout(layout);
		setDividersFromLayout(layout);
	}
}
