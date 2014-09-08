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
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import at.jclehner.androidutils.LoaderListFragment;
import at.jclehner.androidutils.RefString;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.DatabaseHelper;
import at.jclehner.rxdroid.db.DatabaseHelper.DatabaseError;
import at.jclehner.rxdroid.db.DoseEvent;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entries;
import at.jclehner.rxdroid.db.Schedule;
import at.jclehner.rxdroid.ui.ScheduleViewHolder;
import at.jclehner.rxdroid.util.CollectionUtils;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Extras;
import at.jclehner.rxdroid.util.Util;
import at.jclehner.rxdroid.util.WrappedCheckedException;
import at.jclehner.rxdroid.widget.DrugNameView;
import at.jclehner.rxdroid.widget.DrugSupplyMonitor;

public class DrugListFragment extends LoaderListFragment<Drug> implements View.OnClickListener,
		View.OnLongClickListener
{
	static class Adapter extends LLFAdapter<Drug>
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

				holder.supply.setOnLongClickListener((View.OnLongClickListener) mFragment);

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

			final Loader.DrugWrapper wrapper = getItemHolder(position);

			holder.name.setDrug(wrapper.item);
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

	static class Loader extends LLFLoader<Drug>
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

		public Loader(Context context, Bundle args)
		{
			super(context);

			checkArgs(args);

			mDate = (Date) args.getSerializable(ARG_DATE);
			mDtInfo = (Settings.DoseTimeInfo) args.getSerializable(ARG_DOSE_TIME_INFO);
			mPatientId = args.getInt(ARG_PATIENT_ID);
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

					if(maxDoseTimeForNoDim >= Schedule.TIME_MORNING && mDtInfo.activeDoseTime() != Schedule.TIME_INVALID)
					{
						for(int i = 0; i != wrapper.doseViewDimmed.length; ++i)
						{
							final int doseTime = Schedule.TIME_MORNING + i;
							if(doseTime <= maxDoseTimeForNoDim && !drug.getDose(doseTime, mDate).isZero())
								wrapper.doseViewDimmed[i] = Entries.countDoseEvents(drug, mDate, doseTime) != 0;
							else
								wrapper.doseViewDimmed[i] = true;
						}
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
	public void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);

		final Bundle args = icicle != null ? icicle : getArguments();
		checkArgs(args);

		mPatientId = args.getInt(ARG_PATIENT_ID, -1);
		mDate = (Date) args.getSerializable(ARG_DATE);
		mDtInfo = (Settings.DoseTimeInfo) args.getSerializable(ARG_DOSE_TIME_INFO);
	}

	@Override
	public void onResume()
	{
		super.onResume();
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putAll(getCurrentArguments());
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
		else if(view instanceof DrugSupplyMonitor)
		{
			final Drug drug = ((DrugSupplyMonitor) view).getDrug();
			if(drug != null)
			{
				//final Date today = DateTime.today();

				final int daysLeft = Entries.getSupplyDaysLeftForDrug(drug, mDate);
				final String dateString = DateTime.toNativeDate(DateTime.add(mDate, Calendar.DAY_OF_MONTH, daysLeft));

				Toast.makeText(getActivity(), getString(R.string._toast_low_supplies, dateString), Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public boolean onLongClick(View view)
	{
		if(view instanceof DrugSupplyMonitor)
		{
			final Drug drug = ((DrugSupplyMonitor) view).getDrug();
			if(drug != null)
			{
				final DrugSupplyEditFragment dialog = DrugSupplyEditFragment.newInstance(drug);
				dialog.show(getFragmentManager(), "supply_edit_dialog");
			}
		}

		return true;
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
			menu.getItem(R.id.menuitem_remove_dose).setOnMenuItemClickListener(new OnContextMenuItemClickListener()
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
			menu.getItem(R.id.menuitem_skip).setOnMenuItemClickListener(new OnContextMenuItemClickListener()
			{
				@Override
				public boolean onMenuItemClick(MenuItem menuItem)
				{
					Database.create(new DoseEvent(drug, doseView.getDate(), doseTime));
					return true;
				}
			});
		}

		menu.getItem(R.id.menuitem_take).setOnMenuItemClickListener(new OnContextMenuItemClickListener()
		{
			@Override
			public boolean onMenuItemClick(MenuItem menuItem)
			{
				// FIXME
				doseView.performClick();
				return true;
			}
		});

		menu.getItem(R.id.menuitem_edit).setOnMenuItemClickListener(new OnContextMenuItemClickListener()
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
	protected void onLoaderException(RuntimeException e)
	{
		final DatabaseError error;

		if(e instanceof WrappedCheckedException && ((WrappedCheckedException) e).getCauseType() == DatabaseError.class)
			error = (DatabaseError) e.getCause();
		else if(e instanceof DatabaseError)
			error = (DatabaseError) e;
		else
			throw e;

		final StringBuilder sb = new StringBuilder();

		switch(error.getType())
		{
			case DatabaseError.E_GENERAL:
				sb.append(getString(R.string._msg_db_error_general));
				break;

			case DatabaseError.E_UPGRADE:
				sb.append(getString(R.string._msg_db_error_upgrade));
				break;

			case DatabaseError.E_DOWNGRADE:
				sb.append(getString(R.string._msg_db_error_downgrade));
				break;
		}

		sb.append(" " + RefString.resolve(getActivity(), R.string._msg_db_error_footer));

		getActivity().setContentView(R.layout.database_error);
		final TextView errMsg = (TextView) getActivity().findViewById(R.id.message);
		errMsg.setText(sb);
	}

	@Override
	protected LLFAdapter<Drug> onCreateAdapter() {
		return new Adapter(this);
	}

	@Override
	protected LLFLoader<Drug> onCreateLoader() {
		return new Loader(getActivity(), getCurrentArguments());
	}

	private Bundle getCurrentArguments()
	{
		final Bundle args = new Bundle();
		args.putInt(ARG_PATIENT_ID, mPatientId);
		args.putSerializable(ARG_DATE, mDate);
		args.putSerializable(ARG_DOSE_TIME_INFO, mDtInfo);

		return args;
	}

	private static void checkArgs(Bundle args)
	{
		if(!args.containsKey(ARG_DATE) || !args.containsKey(ARG_PATIENT_ID)
				|| !args.containsKey(ARG_DOSE_TIME_INFO)) {
			throw new IllegalArgumentException();
		}
	}
}
