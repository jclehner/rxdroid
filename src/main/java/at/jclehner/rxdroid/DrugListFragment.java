package at.jclehner.rxdroid;


import android.content.Context;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;

import com.actionbarsherlock.app.SherlockFragment;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entries;
import at.jclehner.rxdroid.util.DateTime;

public class DrugListFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks
{
	public static class DrugLoader extends AsyncTaskLoader<List<Drug>>
	{
		private final int mPatientId;
		private List<Drug> mData;

		public DrugLoader(Context context, Date date, int activeDoseTime, int patientId)
		{
			super(context);
			mPatientId = patientId;
		}

		@Override
		public List<Drug> loadInBackground()
		{
			Database.init();
			final List<Drug> data = Entries.getAllDrugs(mPatientId);
			Collections.sort(data, new DrugComparator());
			return data;
		}

		@Override
		public void deliverResult(List<Drug> data)
		{
			mData = data;

			if(isStarted())
				super.deliverResult(data);
		}

		@Override
		protected void onStartLoading()
		{
			onContentChanged();

			if(mData != null)
				deliverResult(mData);

			if(takeContentChanged() || mData == null)
				forceLoad();
		}

		@Override
		protected void onStopLoading() {
			cancelLoad();
		}

		@Override
		protected void onReset()
		{
			super.onReset();
			onStopLoading();
			mData = null;
		}
	}

	public static class DrugComparator implements Comparator<Drug>
	{
		private final int mDoseTime;
		private final Date mDate;
		private final boolean mSmartSortEnabled =
				Settings.getBoolean(Settings.Keys.USE_SMART_SORT, false);

		public DrugComparator(Date date, int doseTime)
		{
			mDate = date;
			mDoseTime = doseTime;
		}

		@Override
		public int compare(Drug lhs, Drug rhs)
		{
			if(mSmartSortEnabled)
			{
				if(getSmartSortScore(lhs) < getSmartSortScore(rhs))
					return -1;
				else
					return 1;
			}

			return lhs.compareTo(rhs);
		}

		// lower score is better (higher up)
		private int getSmartSortScore(Drug drug)
		{
			if(!drug.isActive())
				return 10000 - drug.getId();

			int score = 0;

			if(!Entries.hasAllDoseEvents(drug, mDate, mDoseTime, false))
				score -= 5000;

			if(!drug.getDose(mDoseTime, mDate).isZero())
			{
				if(Entries.countDoseEvents(drug, mDate, mDoseTime) == 0)
					score -= 3000;
			}

			if(Entries.hasLowSupplies(drug, mDate))
				score -= 1000;

			if(DateTime.isToday(mDate))
			{
				if(Entries.hasMissingDosesBeforeDate(drug, mDate))
					score -= 1000;
			}

			if(drug.hasDoseOnDate(mDate))
				score -= 2500;

			return score;
		}
	}

	public static String ARG_DATE = "date";
	public static String ARG_ACTIVE_DOSE_TIME = "active_dose_time";

}
