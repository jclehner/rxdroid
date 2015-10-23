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
import android.os.Bundle;
import android.app.DialogFragment;
import android.view.ViewStub;
import android.view.View;
import android.widget.Button;
import at.jclehner.rxdroid.FractionInputDialog.OnFractionSetListener;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.Drug;

public class DrugSupplyEditFragment extends DialogFragment
{
	public static class Dialog extends FractionInputDialog
	{
		public Dialog(Context context, Fraction currentSupply, final int refillSize, OnFractionSetListener l)
		{
			super(context, currentSupply, l);

			//setTitle(R.string._title_current_supply);
			setAutoInputModeEnabled(true);

			if(refillSize != 0)
			{
				final ViewStub stub = getFooterStub();
				stub.setLayoutResource(R.layout.current_supply_button);

				final Button btn = (Button) stub.inflate().findViewById(R.id.btn_current_supply);
				btn.setText("+" + Integer.toString(refillSize));
				btn.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(final View v)
					{
						setValue(getValue().plus(refillSize));
						v.setEnabled(false);
						v.postDelayed(new Runnable() {

							@Override
							public void run()
							{
								v.setEnabled(true);
							}
						}, 500);
					}
				});
			}
		}
	}

	public static DrugSupplyEditFragment newInstance(Drug drug)
	{
		final DrugSupplyEditFragment instance = new DrugSupplyEditFragment();

		final Bundle args = new Bundle();
		args.putInt("drug_id", drug.getId());
		instance.setArguments(args);

		return instance;
	}

	@Override
	public Dialog onCreateDialog(Bundle icicle)
	{
		final Drug drug = getDrug();
		final Fraction value;

		if(icicle != null)
			value = icicle.getParcelable("value");
		else
			value = drug.getCurrentSupply();

		final Dialog d = new Dialog(getActivity(), value, drug.getRefillSize(), mListener);
		d.setTitle(getString(R.string._title_supply_edit, drug.getName()));
		return d;
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState)
	{
		super.onSaveInstanceState(savedInstanceState);

		FractionInputDialog dialog = getDialog();
		if(dialog != null)
			savedInstanceState.putParcelable("value", dialog.getValue());
	}

	@Override
	public FractionInputDialog getDialog() {
		return (FractionInputDialog) super.getDialog();
	}

	private Drug getDrug() {
		return Drug.get(getArguments().getInt("drug_id"));
	}

	private final OnFractionSetListener mListener = new OnFractionSetListener() {

		@Override
		public void onFractionSet(FractionInputDialog dialog, Fraction newValue)
		{
			final Drug drug = getDrug();
			drug.setCurrentSupply(newValue);
			Database.update(drug);
		}
	};
}
