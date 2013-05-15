package at.jclehner.rxdroid.ui;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import at.jclehner.rxdroid.BuildConfig;
import at.jclehner.rxdroid.DoseView;
import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.Settings;
import at.jclehner.rxdroid.Theme;
import at.jclehner.rxdroid.Settings.Keys;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entries;
import at.jclehner.rxdroid.db.DoseEvent;
import at.jclehner.rxdroid.util.Constants;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.SimpleBaseExpandanbleListAdapter;
import at.jclehner.rxdroid.util.Timer;
import at.jclehner.rxdroid.util.Util;

public class DoseLogFragment extends ExpandableListFragment
{
	private static final String TAG = DoseLogFragment.class.getSimpleName();
	private static final boolean LOGV = BuildConfig.DEBUG;

	private List<List<EventInfo>> mGroupedEvents;
	private Date mToday;

	public static DoseLogFragment newInstance(Drug drug)
	{
		DoseLogFragment f = new DoseLogFragment();
		f.setArguments(Util.createBundle("drug_id", drug.getId()));

		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mToday = DateTime.today();
		gatherEventInfos();
		setListAdapter(new Adapter());
	}

	private Drug getDrug() {
		return Drug.get(getArguments().getInt("drug_id"));
	}

	private void gatherEventInfos()
	{
		final Timer t;
		if(LOGV)
			t = new Timer();

		final Drug drug = getDrug();
		final List<DoseEvent> events = Entries.findDoseEvents(drug, null, null);
		if(events.isEmpty())
			return;

		Date date = Settings.getDate(Keys.OLDEST_POSSIBLE_DOSE_EVENT_TIME);
		if(date == null)
			date = events.get(0).getDate();

		List<EventInfo> infos = new ArrayList<EventInfo>();
		for(DoseEvent event : events)
			infos.add(EventInfo.newTakenOrIgnoredEvent(event));

//		final Date lastDosesClearedDate = drug.getLastDosesClearedDate();
//		if(lastDosesClearedDate != null)
//		{
//			while(!date.after(lastDosesClearedDate))
//				date = DateTime.add(date, Calendar.DAY_OF_MONTH, 1);
//		}

		while(!date.after(mToday))
		{
			if(drug.hasDoseOnDate(date))
			{

				for(int doseTime : Constants.DOSE_TIMES)
				{
					Fraction dose = drug.getDose(doseTime, date);

					if(!dose.isZero() && !containsDoseEvent(events, date, doseTime))
					{
						//Log.d(TAG, "Creating missed event: date=" + date + ", doseTime=" + doseTime);
						infos.add(EventInfo.newMissedEvent(date, doseTime));
					}
				}
			}

			date = DateTime.add(date, Calendar.DAY_OF_MONTH, 1);
		}

		Collections.sort(infos, EventInfoByDateComparator.INSTANCE);

		mGroupedEvents = new ArrayList<List<EventInfo>>();

		for(int i = 0; i < events.size();)
		{
			date = infos.get(i).date;

			final List<EventInfo> group = new ArrayList<EventInfo>();

			while(i < events.size() && date.equals(infos.get(i).date))
			{
				//Log.d(TAG, "  CHILD: " + events.get(i));
				group.add(infos.get(i));
				++i;
			}

			Collections.sort(group, EventInfoByDoseTimeComparator.INSTANCE);

			mGroupedEvents.add(group);
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

	private class Adapter extends SimpleBaseExpandanbleListAdapter implements ListAdapter
	{
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
			return mGroupedEvents.get(groupPosition).size();
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
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view,
				ViewGroup parent)
		{
			final EventInfo event = mGroupedEvents.get(groupPosition).get(childPosition);
			final ChildViewHolder holder;

			if(view == null)
			{
				view = mInflater.inflate(R.layout.log_child, null);
				holder = new ChildViewHolder();
				holder.dose = (DoseView) view.findViewById(R.id.dose_dose);
				holder.text = (TextView) view.findViewById(R.id.text_info);
				view.setTag(holder);
			}
			else
				holder = (ChildViewHolder) view.getTag();

			holder.dose.setDoseTime(event.doseTime);

			final DoseEvent doseEvent = event.intake;
			if(doseEvent != null)
			{
				holder.dose.setDose(doseEvent.getDose());

				String text;
				if(event.status == EventInfo.STAT_TAKEN)
					holder.text.setText("Taken: " + doseEvent.getTimestamp());
				else
					holder.text.setText("Skipped: " + doseEvent.getTimestamp());
			}
			else
			{
				holder.dose.setDose(Fraction.ZERO);
				holder.text.setText("Missed");
			}

			return view;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition)
		{
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public int getCount()
		{
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Object getItem(int position)
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position)
		{
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getItemViewType(int position)
		{
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getViewTypeCount()
		{
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean isEnabled(int position)
		{
			// TODO Auto-generated method stub
			return false;
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
	TextView text;
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
	final int status;

	final DoseEvent intake;

	static EventInfo newTakenOrIgnoredEvent(DoseEvent intake) {
		return new EventInfo(intake);
	}

	static EventInfo newMissedEvent(Date date, int doseTime) {
		return new EventInfo(date, doseTime);
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

	private EventInfo(DoseEvent intake)
	{
		timestamp = intake.getTimestamp();
		date = intake.getDate();
		doseTime = intake.getDoseTime();
		status = intake.getDose().isZero() ? STAT_IGNORED : STAT_TAKEN;

		this.intake = intake;

		if(timestamp == null)
		{
			Log.d("EventInfo", "Empty timestamp in " + intake);
		}

//		generateTimestamp();
	}

	private EventInfo(Date date, int doseTime)
	{
		this.date = date;
		this.doseTime = doseTime;
		status = STAT_MISSED;
		intake = null;

//		generateTimestamp();
	}

	private void generateTimestamp()
	{
		if(timestamp == null)
		{
			long offset = Settings.getDoseTimeBeginOffset(doseTime);
			//timestamp = DateTime.add(date, Calendar.MILLISECOND, (int) offset);
			timestamp = new Timestamp(date.getTime() + offset);
			Log.d("EventInfoComparator", "Forging time for " + this + " to " + DateTime.toNativeDateAndTime(timestamp));
		}
	}
}
