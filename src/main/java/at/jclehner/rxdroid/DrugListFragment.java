package at.jclehner.rxdroid;


import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import at.jclehner.androidutils.LoaderListFragment;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.DoseEvent;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entries;
import at.jclehner.rxdroid.db.Schedule;
import at.jclehner.rxdroid.ui.ScheduleViewHolder;
import at.jclehner.rxdroid.util.CollectionUtils;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Extras;
import at.jclehner.rxdroid.util.Util;
import at.jclehner.rxdroid.widget.DrugNameView;
import at.jclehner.rxdroid.widget.DrugSupplyMonitor;

public class DrugListFragment extends LoaderListFragment<Drug> implements View.OnClickListener
{
	public static class Adapter extends LLFAdapter<Drug>
	{
		static class ViewHolder extends ScheduleViewHolder
		{
			DrugNameView name;
			ImageView icon;
			DrugSupplyMonitor supply;
			View history;
		}

		public Adapter(DrugListFragment fragment) {
			super(fragment);
		}

		@Override
		public View getView(int position, View view, ViewGroup viewGroup)
		{
			final ViewHolder holder;

			if(view == null)
			{
				view = mInflater.inflate(R.layout.drug_view, viewGroup, false);
				holder = new ViewHolder();
				holder.icon = (ImageView) view.findViewById(R.id.drug_icon);
				holder.name = (DrugNameView) view.findViewById(R.id.drug_name);
				holder.supply = (DrugSupplyMonitor) view.findViewById(R.id.text_supply);
				holder.history = view.findViewById(R.id.frame_history_menu);

				holder.name.setOnClickListener((View.OnClickListener) mFragment);
				holder.history.setOnClickListener((View.OnClickListener) mFragment);
				holder.supply.setOnClickListener((View.OnClickListener) mFragment);

				view.findViewById(R.id.img_missed_dose_warning).setVisibility(View.GONE);

				holder.setDoseViewsAndDividersFromLayout(view);

				for(DoseView doseView : holder.doseViews)
				{
					doseView.setOnClickListener((View.OnClickListener) mFragment);
					doseView.setOnCreateContextMenuListener(mFragment);
				}

				view.setTag(holder);
			}
			else
				holder = (ViewHolder) view.getTag();

			Loader.DrugWrapper wrapper = getItemHolder(position);

			holder.name.setText(wrapper.item.getName());
			holder.name.setTag(wrapper.item.getId());

			holder.icon.setImageResource(Util.getDrugIconDrawable(wrapper.item.getIcon()));
			holder.supply.setDrugAndDate(wrapper.item, wrapper.date);
			holder.supply.setVisibility(wrapper.isSupplyVisible ? View.VISIBLE : View.INVISIBLE);
			holder.history.setTag(wrapper.item.getId());

			for(int i = 0; i != holder.doseViews.length; ++i)
			{
				final DoseView doseView = holder.doseViews[i];

				if(!doseView.hasInfo(wrapper.date, wrapper.item))
					doseView.setDoseFromDrugAndDate(wrapper.date, wrapper.item);

				doseView.setDimmed(wrapper.doseViewDimmed[i]);
			}

			final int dividerVisibility;
			if(view.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
				dividerVisibility = View.GONE;
			else
				dividerVisibility = View.VISIBLE;

			for(View divider : holder.dividers)
			{
				if(divider != null)
					divider.setVisibility(dividerVisibility);
			}

			return view;
		}
	}

	public static class Loader extends LLFLoader<Drug>
	{
		public static class DrugWrapper extends ItemHolder<Drug>
		{
			public DrugWrapper(Drug drug) {
				super(drug);
			}

			public Date date;
			public boolean isToday;
			public boolean isSupplyLow;
			public boolean isSupplyVisible;
			public boolean[] doseViewDimmed = { false, false, false, false };
		}

		private final int mPatientId;
		private final Settings.DoseTimeInfo mDtInfo;
		private final Date mDate;

		public Loader(Context context, int patientId, Date date, Settings.DoseTimeInfo dtInfo)
		{
			super(context);
			mPatientId = patientId;
			mDate = date;
			mDtInfo = dtInfo;
		}

		@Override
		public List<DrugWrapper> loadInBackground()
		{
			Database.init();

			final List<Drug> drugs = (List<Drug>)
					CollectionUtils.filter(Entries.getAllDrugs(mPatientId), mFilter);

			Collections.sort(drugs, mComparator);
			final ArrayList<DrugWrapper> data = new ArrayList<DrugWrapper>(drugs.size());

			for(Drug drug : drugs)
			{
				final DrugWrapper wrapper = new DrugWrapper(drug);
				wrapper.date = mDate;
				wrapper.isToday = DateTime.isToday(mDate);
				wrapper.isSupplyLow = Entries.hasLowSupplies(drug, mDate);
				wrapper.isSupplyVisible = (drug.getRefillSize() != 0 || !drug.getCurrentSupply().isZero()) && !mDate.before(mDtInfo.currentDate());

				if(wrapper.isToday)
				{
					// This will "underflow" if nextDoseTime is TIME_MORNING, but
					// this doesn't matter since it just dims all doses
					final int maxDoseTimeForNoDim = mDtInfo.nextDoseTime() - 1;

					for(int i = 0; i != wrapper.doseViewDimmed.length; ++i)
					{
						final int doseTime = Schedule.TIME_MORNING + i;
						if(doseTime <= maxDoseTimeForNoDim && !drug.getDose(doseTime, mDate).isZero())
							wrapper.doseViewDimmed[i] = Entries.countDoseEvents(drug, mDate, doseTime) != 0;
						else
							wrapper.doseViewDimmed[i] = true;
					}
				}

				data.add(wrapper);
			}

			return data;
		}

		private final CollectionUtils.Filter<Drug> mFilter = new CollectionUtils.Filter<Drug>()
		{
			@Override
			public boolean matches(Drug drug)
			{
				if(!drug.isActive() || drug.hasAutoDoseEvents())
					return false;

				if(Entries.countDoseEvents(drug, mDate, null) != 0)
					return true;

				if(!drug.hasDoseOnDate(mDate))
					return false;

				return true;
			}
		};

		private final Comparator<Drug> mComparator = new Comparator<Drug>() {

			@Override
			public int compare(Drug lhs, Drug rhs)
			{
				if(Settings.getBoolean(Settings.Keys.USE_SMART_SORT, false))
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

				if(!Entries.hasAllDoseEvents(drug, mDate, mDtInfo.activeOrNextDoseTime(), false))
					score -= 5000;

				if(!drug.getDose(mDtInfo.activeOrNextDoseTime(), mDate).isZero())
				{
					if(Entries.countDoseEvents(drug, mDate, mDtInfo.activeOrNextDoseTime()) == 0)
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
		};
	}
	public static String ARG_DATE = "date";
	public static String ARG_PATIENT_ID = "patient_id";
	public static String ARG_DOSE_TIME_INFO = "dose_time_info";

	private int mPatientId;
	private Date mDate;
	private Settings.DoseTimeInfo mDtInfo;

	public static DrugListFragment newInstance(Date date, int patientId, Settings.DoseTimeInfo dtInfo)
	{
		final Bundle args = new Bundle();
		args.putSerializable(ARG_DATE, date);
		args.putInt(ARG_PATIENT_ID, patientId);
		args.putSerializable(ARG_DOSE_TIME_INFO, dtInfo);

		final DrugListFragment instance = new DrugListFragment();
		instance.setArguments(args);

		return instance;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mPatientId = getArguments().getInt(ARG_PATIENT_ID, -1);
		mDate = (Date) getArguments().getSerializable(ARG_DATE);
		mDtInfo = (Settings.DoseTimeInfo) getArguments().getSerializable(ARG_DOSE_TIME_INFO);

		if(mDate == null)
			throw new NullPointerException(ARG_DATE);
		else if(mDtInfo == null)
			throw new NullPointerException(ARG_DOSE_TIME_INFO);
		else if(mPatientId == -1)
			throw new IllegalArgumentException(ARG_PATIENT_ID);
	}

	@Override
	public void onClick(View view)
	{
		if(view.getId() == R.id.frame_history_menu)
		{
			Intent intent = new Intent(getActivity(), DoseHistoryActivity.class);
			intent.putExtra(Extras.DRUG_ID, (Integer) view.getTag());
			startActivity(intent);
		}
		else if(view instanceof DoseView)
		{
			final Bundle args = new Bundle();
			args.putInt(DoseDialog.ARG_DRUG_ID, ((DoseView) view).getDrug().getId());
			args.putInt(DoseDialog.ARG_DOSE_TIME, ((DoseView) view).getDoseTime());
			args.putSerializable(DoseDialog.ARG_DATE, mDate);
			args.putBoolean(DoseDialog.ARG_FORCE_SHOW, false);

			final DoseDialog dialog = new DoseDialog(getActivity());
			dialog.setArgs(args);
			dialog.show();
		}
		else if(view.getId() == R.id.drug_name)
		{
			Intent intent = new Intent(getActivity(), DrugEditActivity.class);
			intent.setAction(Intent.ACTION_EDIT);
			intent.putExtra(DrugEditActivity.EXTRA_DRUG_ID, (Integer) view.getTag());

			startActivity(intent);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, final View v, ContextMenu.ContextMenuInfo menuInfo)
	{
		new MenuInflater(getActivity()).inflate(R.menu.dose_view_context_menu, menu);

		final DoseView doseView = (DoseView) v;
		final Drug drug = doseView.getDrug();
		final int doseTime = doseView.getDoseTime();

		//menu.setHeaderIcon(Util.getDrugIconDrawable(drug.getIcon()));
		menu.setHeaderTitle(drug.getName());

		if(doseView.wasDoseTaken())
		{
			menu.removeItem(R.id.menuitem_skip);
			menu.findItem(R.id.menuitem_remove_dose).setOnMenuItemClickListener(new OnContextMenuItemClickListener()
			{
				@Override
				public boolean onMenuItemClick(MenuItem menuItem)
				{
					final Fraction.MutableFraction dose = new Fraction.MutableFraction();
					for(DoseEvent intake : Entries.findDoseEvents(drug, mDate, doseTime))
					{
						dose.add(intake.getDose());
						Database.delete(intake);
					}

					drug.setCurrentSupply(drug.getCurrentSupply().plus(dose));
					Database.update(drug);

					return true;
				}
			});

		}
		else
		{
			menu.removeItem(R.id.menuitem_remove_dose);
			menu.findItem(R.id.menuitem_skip).setOnMenuItemClickListener(new OnContextMenuItemClickListener()
			{
				@Override
				public boolean onMenuItemClick(MenuItem menuItem)
				{
					Database.create(new DoseEvent(drug, doseView.getDate(), doseTime));
					return true;
				}
			});
		}

		menu.findItem(R.id.menuitem_take).setOnMenuItemClickListener(new OnContextMenuItemClickListener()
		{
			@Override
			public boolean onMenuItemClick(MenuItem menuItem)
			{
				// FIXME
				doseView.performClick();
				return true;
			}
		});

		menu.findItem(R.id.menuitem_edit).setOnMenuItemClickListener(new OnContextMenuItemClickListener()
		{
			@Override
			public boolean onMenuItemClick(MenuItem menuItem)
			{
				final Intent intent = new Intent(Intent.ACTION_EDIT);
				intent.setClass(getActivity(), DrugEditActivity.class);
				intent.putExtra(DrugEditActivity.EXTRA_DRUG_ID, drug.getId());
				startActivity(intent);
				return true;
			}
		});
	}

	@Override
	protected LLFAdapter<Drug> onCreateAdapter() {
		return new Adapter(this);
	}

	@Override
	protected LLFLoader<Drug> onCreateLoader() {
		return new Loader(getActivity(), mPatientId, mDate, mDtInfo);
	}
}
