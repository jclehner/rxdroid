package at.jclehner.rxdroid;

import java.util.List;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entries;
import at.jclehner.rxdroid.db.Intake;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Extras;
import at.jclehner.rxdroid.util.Util;


public class DoseHistoryActivity extends ExpandableListActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Database.init();

		Drug drug = (Drug) getIntent().getSerializableExtra(Extras.DRUG);

		setTitle("History: " + drug.getName());
		setListAdapter(new DoseHistoryAdapter(this, drug));
	}

	class DoseHistoryAdapter extends BaseExpandableListAdapter
	{
		private Context mContext;
		final List<Intake> mIntakes;

		public DoseHistoryAdapter(Context context, Drug drug)
		{
			mContext = context;
			mIntakes = Entries.findIntakes(drug, null, null);
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return false;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View view, ViewGroup parent)
		{
			final Intake intake = mIntakes.get(groupPosition);
			final TextView tv;

			if(view == null)
			{
				view = LayoutInflater.from(mContext).inflate(android.R.layout.simple_expandable_list_item_1, null);
				//tv = new TextView(mContext);
				//parent.addView(tv);
			}
			else
				tv = (TextView) view;

			((TextView) view.findViewById(android.R.id.text1)).setText(DateTime.toNativeDateAndTime(intake.getTimestamp()));
			return view;
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public int getGroupCount() {
			return mIntakes.size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return null;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return 1;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent)
		{
			final Intake intake = mIntakes.get(groupPosition);
			final TextView tv;

			if(convertView == null)
			{
				tv = new TextView(mContext);
				//parent.addView(tv);
			}
			else
				tv = (TextView) convertView;

			tv.setText("Scheduled for " + intake.getDate() + "(" + Util.getDoseTimeName(intake.getDoseTime()) +")");
			return tv;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return groupPosition << 16 | childPosition;
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return null;
		}
	};


}
