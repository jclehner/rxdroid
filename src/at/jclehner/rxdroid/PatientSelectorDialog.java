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

package at.jclehner.rxdroid;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.Patient;

public class PatientSelectorDialog extends DialogFragment
{
	public interface OnSelectOrDeletePatientListener
	{
		void onSelectPatient(Patient patient);
		void onDeletePatient(Patient patient);
	}

	static class ViewHolder
	{
		TextView name;
		ImageView delete;
	}

	class Adapter extends ArrayAdapter<Patient>
	{
		public Adapter() {
			super(getActivity().getApplicationContext(), 0, Database.getAll(Patient.class));
		}

		@Override
		public View getView(int position, View view, ViewGroup parent)
		{
			final Patient patient = getItem(position);
			final ViewHolder holder;

			if(view == null)
			{
				view = LayoutInflater.from(getContext()).inflate(R.layout.layout_patient_list, null);

				holder = new ViewHolder();
				holder.name = (TextView) view.findViewById(R.id.patient_name);
				holder.delete = (ImageView) view.findViewById(R.id.patient_delete);

				//holder.name.setOnClickListener(mViewClickListener);
				holder.delete.setOnClickListener(mViewClickListener);

				//holder.name.setTag(patient);
				holder.delete.setTag(patient);

				view.setTag(holder);
			}
			else
				holder = (ViewHolder) view.getTag();

			final String name;
			final boolean deleteVisible;

			if(patient.isDefaultPatient())
			{
				name = getString(R.string._title_me);
				deleteVisible = false;
			}
			else
			{
				name = patient.getName();
				deleteVisible = true;
			}

			holder.name.setText(name);
			holder.delete.setVisibility(deleteVisible ? View.VISIBLE : View.GONE);

			return view;
		}

		@Override
		public boolean isEnabled(int position) {
			return getItem(position).getId() != mActivePatientId;
		}
	}

	public static final String ARG_PATIENT_ID = "patient_id";

	private int mActivePatientId;
	private OnSelectOrDeletePatientListener mListener;
	private Adapter mAdapter;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		mAdapter = new Adapter();
		mActivePatientId = getArguments().getInt(ARG_PATIENT_ID);

		final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
		ab.setTitle(R.string._title_patient);
		ab.setAdapter(mAdapter, mDialogListener);

		ab.setNeutralButton(R.string._title_add, mDialogListener);

		return ab.create();
	}

	public static PatientSelectorDialog newInstance(int patiendId)
	{
		final PatientSelectorDialog dialog = new PatientSelectorDialog();
		final Bundle args = new Bundle();
		args.putInt(ARG_PATIENT_ID, patiendId);

		dialog.setArguments(args);

		return dialog;
	}

	private final OnClickListener mViewClickListener = new OnClickListener() {

		@Override
		public void onClick(View v)
		{
			if(v.getId() == R.id.patient_delete && mListener != null)
				mListener.onDeletePatient((Patient) v.getTag());

			dismiss();
		}
	};

	private final DialogInterface.OnClickListener mDialogListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			if(which == Dialog.BUTTON_NEUTRAL)
			{
				final Patient p = new Patient();
				p.setName("Patient #" + Database.countAll(Patient.class));
				Database.create(p);
			}
			else if(which >= 0 && mListener != null)
				mListener.onSelectPatient(mAdapter.getItem(which));
		}
	};
}
