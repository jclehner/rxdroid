/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2013 Joseph Lehner <joseph.c.lehner@gmail.com>
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import at.jclehner.rxdroid.BuildConfig;
import at.jclehner.rxdroid.DoseView;
import at.jclehner.rxdroid.DrugListActivity;
import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.Settings;
import at.jclehner.rxdroid.Settings.DoseTimeInfo;
import at.jclehner.rxdroid.Settings.Keys;
import at.jclehner.rxdroid.Theme;
import at.jclehner.rxdroid.db.DoseEvent;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entries;
import at.jclehner.rxdroid.util.Constants;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Timer;
import at.jclehner.rxdroid.util.Util;

public class DoseLogFragment extends ExpandableListFragment
{
	private static final String TAG = DoseLogFragment.class.getSimpleName();
	private static final boolean LOGV = BuildConfig.DEBUG;

	private static final int[] DOSE_NAME_IDS = {
		R.string._title_morning_dose,
		R.string._title_noon_dose,
		R.string._title_evening_dose,
		R.string._title_night_dose
	};

	private List<List<EventInfo>> mGroupedEvents;

	private Date mToday;

	public static final int SHOW_MISSED = 1;
	public static final int SHOW_TAKEN = 1 << 1;
	public static final int SHOW_SKIPPED = 1 << 2;

	public static DoseLogFragment newInstance(Drug drug, int flags)
	{
		DoseLogFragment f = new DoseLogFragment();
		f.setArguments(Util.createBundle("drug_id", drug.getId(), "flags", flags));
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mToday = DateTime.today();
		gatherEventInfos(getArguments().getInt("flags"));
		setListAdapter(new Adapter());
		setEmptyViewText(R.string._msg_no_history_data);
	}

	private Drug getDrug() {
		return Drug.get(getArguments().getInt("drug_id"));
	}

	private void gatherEventInfos(int flags)
	{
		final Timer t;
		if(LOGV)
			t = new Timer();

		final Drug drug = getDrug();
		final List<EventInfo> infos = new ArrayList<EventInfo>();
		final List<DoseEvent> events = Entries.findDoseEvents(drug, null, null);

		for(DoseEvent event : events)
		{
			boolean isSkipped = event.getDose().isZero();

			if((isSkipped && (flags & SHOW_SKIPPED) == 0) ||
					(!isSkipped && (flags & SHOW_TAKEN) == 0)) {
				continue;
			}

			infos.add(EventInfo.newTakenOrIgnoredEvent(event));
		}

		Date date = Settings.getDate(Keys.OLDEST_POSSIBLE_DOSE_EVENT_TIME);

		if(date != null && !events.isEmpty())
		{
			Collections.sort(infos, EventInfoByDateComparator.INSTANCE);
			date = DateTime.max(date, Settings.getOldestPossibleHistoryDate(mToday));
			date = DateTime.min(date, events.get(events.size() - 1).getDate());
		}
		else if(date == null)
			date = Settings.getOldestPossibleHistoryDate(mToday);

		if(false)
		{
			Log.v(TAG, "gatherEventInfos:\n" +
					"  oldest reliable DoseEvent date: " +
					DateTime.toDateString(Settings.getDate(Keys.OLDEST_POSSIBLE_DOSE_EVENT_TIME)) + "\n" +
					"  oldest possible history date  : " +
					DateTime.toDateString(Settings.getOldestPossibleHistoryDate(mToday)) + "\n" +
					"  oldest date in DoseEvent set  : " +
					DateTime.toDateString(events.get(events.size() - 1).getDate()) + "\n\n" +
					"  date used                     : " +
					DateTime.toDateString(date));
		}

		if(date == null)
		{
			Log.w(TAG, "gatherEventInfos(" + flags + "): no date to begin; giving up");
			return;
		}
		else if(LOGV)
			Log.v(TAG, "gatherEventInfos: date=" + date);

//		final Date lastDosesClearedDate = drug.getLastDosesClearedDate();
//		if(lastDosesClearedDate != null)
//		{
//			while(!date.after(lastDosesClearedDate))
//				date = DateTime.add(date, Calendar.DAY_OF_MONTH, 1);
//		}

		final DoseTimeInfo dtInfo = Settings.getDoseTimeInfo();

		if((flags & SHOW_MISSED) != 0)
		{
			while(!date.after(mToday))
			{
				if(drug.hasDoseOnDate(date))
				{
					for(int doseTime : Constants.DOSE_TIMES)
					{
						if(date.equals(mToday) && doseTime == dtInfo.activeOrNextDoseTime())
							break;

						Fraction dose = drug.getDose(doseTime, date);

						if(!dose.isZero() && !containsDoseEvent(events, date, doseTime))
						{
							//Log.d(TAG, "Creating missed event: date=" + date + ", doseTime=" + doseTime);
							infos.add(EventInfo.newMissedEvent(date, doseTime, dose));
						}
					}
				}

				date = DateTime.add(date, Calendar.DAY_OF_MONTH, 1);
			}
		}

		Collections.sort(infos, EventInfoByDateComparator.INSTANCE);

		mGroupedEvents = new ArrayList<List<EventInfo>>();

		for(int i = 0; i < infos.size();)
		{
			date = infos.get(i).date;

			final List<EventInfo> group = new ArrayList<EventInfo>();

			while(i < infos.size() && date.equals(infos.get(i).date))
			{
				//Log.d(TAG, "  CHILD: " + events.get(i));
				group.add(infos.get(i));
				++i;
			}

			if(!group.isEmpty())
			{
				Collections.sort(group, EventInfoByDoseTimeComparator.INSTANCE);
				mGroupedEvents.add(group);
			}
		}

		if(LOGV) Log.d(TAG, "gatherEvents: " + t);
	}

	private static boolean containsDoseEvent(List<DoseEvent> doseEvents, Date date, int doseTime)
	{
		for(DoseEvent doseEvent : doseEvents)
		{
			if(doseEvent.getDate().equals(date) && doseEvent.getDoseTime() == doseTime)
				return true;
		}

		return false;
	}

//	private class Adapter extends SimpleBaseExpandanbleListAdapter implements ListAdapter
	private class Adapter extends BaseExpandableListAdapter
	{
		private Timer mChildTimer = new Timer();

		LayoutInflater mInflater;

		public Adapter()
		{
			mInflater = LayoutInflater.from(getActivity());
		}

		@Override
		public int getGroupCount() {
			return mGroupedEvents.size();
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return mGroupedEvents.get(groupPosition).size() + 1;
		}

		@Override
		public Object getGroup(int groupPosition) {
			return mGroupedEvents.get(groupPosition);
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return mGroupedEvents.get(groupPosition).get(childPosition);
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View view, ViewGroup parent)
		{
			final GroupViewHolder holder;

			if(view == null)
			{
				view = mInflater.inflate(R.layout.log_group, null);
				holder = new GroupViewHolder();
				holder.status = (ImageView) view.findViewById(R.id.img_status);
				holder.date = (TextView) view.findViewById(R.id.text_date);
				holder.count = (TextView) view.findViewById(R.id.text_count);
				view.setTag(holder);
			}
			else
				holder = (GroupViewHolder) view.getTag();

			final List<EventInfo> events = mGroupedEvents.get(groupPosition);
			if(events.isEmpty())
			{
				Log.w(TAG, "Group " + groupPosition + " is empty...");
				holder.status.setImageResource(0);
				holder.date.setText("?");
				holder.count.setText("(0)");

				return view;
			}

			final Date date = events.get(0).date;
			holder.date.setText(DateTime.toNativeDate(date));
			holder.count.setText("(" + events.size() + ")");

			int statusResId = Theme.getResourceAttribute(R.attr.doseStatusTaken);
			for(EventInfo info : events)
			{
				if(info.status == EventInfo.STAT_MISSED)
				{
					statusResId = Theme.getResourceAttribute(R.attr.doseStatusMissed);
					break;
				}
			}

			holder.status.setImageResource(statusResId);
			return view;
		}

		@Override
		public View getChildView(final int groupPosition, int childPosition, boolean isLastChild, View view,
				ViewGroup parent)
		{
			if(childPosition == 0)
				mChildTimer.restart();

			final ChildViewHolder holder;

			if(view == null)
			{
				view = mInflater.inflate(R.layout.log_child, null);
				holder = new ChildViewHolder();
				holder.dose = (DoseView) view.findViewById(R.id.dose_dose);
				holder.time = (TextView) view.findViewById(R.id.text_time);
				holder.text = (TextView) view.findViewById(R.id.text_info);
				holder.gotoDate = (ImageView) view.findViewById(R.id.img_goto_date);
				view.setTag(holder);
			}
			else
				holder = (ChildViewHolder) view.getTag();

			if(!isLastChild)
			{
				holder.dose.setVisibility(View.VISIBLE);
				holder.time.setVisibility(View.VISIBLE);
				holder.gotoDate.setVisibility(View.INVISIBLE);
				//holder.text.setGravity(Gravity.CENTER_VERTICAL);

				final EventInfo info = mGroupedEvents.get(groupPosition).get(childPosition);

				holder.time.setText(info.getTimeString());
				holder.dose.setDose(info.dose);

				final int timeColorAttr;
				final int textResId;

				final DoseEvent doseEvent = info.intake;
				if(doseEvent != null)
				{
					//holder.dose.setDose(doseEvent.getDose());
					if(info.status == EventInfo.STAT_TAKEN)
					{
						textResId = R.string._title_taken;
						timeColorAttr = R.attr.colorStatusTaken;
					}
					else
					{
						textResId = R.string._title_skipped;
						timeColorAttr = R.attr.colorStatusSkipped;
					}
				}
				else
				{
					//holder.dose.setDoseFromDrugAndDate(info.date, null);
					//holder.dose.setVisibility(View.GONE);
					textResId = R.string._title_missed;
					timeColorAttr = R.attr.colorStatusMissed;
				}

				final StringBuilder sb = new StringBuilder();
				sb.append(getString(textResId) + ": ");
				sb.append(getString(DOSE_NAME_IDS[info.doseTime]));
				holder.text.setText(sb.toString());
				holder.text.setOnClickListener(null);

				final int color = Theme.getColorAttribute(timeColorAttr);

//				final int color = getResources().getColor(Theme.getColorAttribute(timeColorAttr));
				holder.time.setTextColor(color);
			}
			else
			{
				holder.dose.setVisibility(View.INVISIBLE);
				holder.time.setVisibility(View.INVISIBLE);
				holder.gotoDate.setVisibility(View.VISIBLE);
				//holder.text.setGravity(Gravity.CENTER);

				final OnClickListener clickListener = new OnClickListener() {

					@Override
					public void onClick(View v)
					{
						final Intent intent = new Intent(getActivity(), DrugListActivity.class);
						intent.putExtra(DrugListActivity.EXTRA_DATE, getGroupDate(groupPosition));
						intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
						startActivity(intent);
					}
				};


				holder.gotoDate.setOnClickListener(clickListener);
				holder.text.setOnClickListener(clickListener);
				holder.text.setText(R.string._msg_tap_to_view_date);
			}

			if(isLastChild)
				Log.i(TAG, (childPosition + 1) + " child views created in " + mChildTimer);

			return view;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition)
		{
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			// TODO Auto-generated method stub
			return childPosition;
		}

		@Override
		public long getGroupId(int groupPosition) {
			// TODO Auto-generated method stub
			return groupPosition;
		}

		@Override
		public boolean hasStableIds() {
			// TODO Auto-generated method stub
			return false;
		}

		private Date getGroupDate(int groupPosition)
		{
			if(!mGroupedEvents.get(groupPosition).isEmpty())
				return mGroupedEvents.get(groupPosition).get(0).date;

			return null;
		}

	}
}

class GroupViewHolder
{
	ImageView status;
	TextView date;
	TextView count;
}

class ChildViewHolder
{
	DoseView dose;
	TextView time;
	TextView text;
	ImageView gotoDate;
}

enum EventInfoByDateComparator implements Comparator<EventInfo>
{
	INSTANCE;

	@Override
	public int compare(EventInfo lhs, EventInfo rhs) {
		return signum(rhs.getSortingTime() - lhs.getSortingTime());
	}

	private static int signum(long l)
	{
		if(l == 0)
			return 0;

		return l < 0 ? -1 : 1;
	}
}

enum EventInfoByDoseTimeComparator implements Comparator<EventInfo>
{
	INSTANCE;

	@Override
	public int compare(EventInfo lhs, EventInfo rhs) {
		return lhs.doseTime - rhs.doseTime;
	}
}

class EventInfo
{
	static final int STAT_TAKEN = 0;
	static final int STAT_MISSED = 1;
	static final int STAT_IGNORED = 2;

	Date timestamp;
	final Date date;
	final int doseTime;
	final Fraction dose;
	final int status;

	final DoseEvent intake;

	static EventInfo newTakenOrIgnoredEvent(DoseEvent intake) {
		return new EventInfo(intake);
	}

	static EventInfo newMissedEvent(Date date, int doseTime, Fraction dose) {
		return new EventInfo(date, doseTime, dose);
	}

	@Override
	public String toString()
	{
		String statusStr;

		switch(status)
		{
			case 0:
				statusStr = "taken";
				break;

			case 1:
				statusStr = "missed";
				break;

			case 2:
				statusStr = "ignored";
				break;

			default:
				statusStr = "???";
		}

		return "EventInfo { " + timestamp + ", " + DateTime.toDateString(date) + ", " +
				Util.getDoseTimeName(doseTime) + ", " + statusStr + " }";
	}

	@Override
	public boolean equals(Object o)
	{
		if(o == null || !(o instanceof EventInfo))
			return false;

		return getSortingTime() == ((EventInfo) o).getSortingTime();
	}

	@Override
	public int hashCode() {
		return (int) (getSortingTime() & 0x00000000ffffffffL);
	}

	long getSortingTime() {
		return date.getTime();
	}

	String getTimeString()
	{
		if(timestamp == null)
		{
			final long offset = Settings.getTrueDoseTimeEndOffset(doseTime);
			timestamp = DateTime.add(date, Calendar.MILLISECOND, (int) offset);
		}

		final StringBuilder sb = new StringBuilder(DateTime.toNativeTime(timestamp, false));
		long diffDays = DateTime.diffDays(date, timestamp);

		if(diffDays != 0)
		{
			sb.append(' ');

			if(diffDays > 0)
				sb.append('+');

			sb.append(diffDays);
		}

		return sb.toString();
	}

	private EventInfo(DoseEvent intake)
	{
		timestamp = intake.getTimestamp();
		date = intake.getDate();
		doseTime = intake.getDoseTime();
		dose = intake.getDose();
		status = dose.isZero() ? STAT_IGNORED : STAT_TAKEN;

		this.intake = intake;

		if(timestamp == null)
		{
			Log.d("EventInfo", "Empty timestamp in " + intake);
		}
	}

	private EventInfo(Date date, int doseTime, Fraction dose)
	{
		this.date = date;
		this.doseTime = doseTime;
		this.dose = dose;

		status = STAT_MISSED;
		intake = null;
	}
}
