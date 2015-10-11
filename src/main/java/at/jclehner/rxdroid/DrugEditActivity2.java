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

import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;

import at.jclehner.rxdroid.util.Components;

public class DrugEditActivity2 extends AppCompatActivity
{
	public static final String EXTRA_DRUG_ID = "drug_id";
	public static final String EXTRA_FOCUS_ON_CURRENT_SUPPLY = "focus_on_current_supply";
	public static final String EXTRA_DISALLOW_DELETE = "disallow_delete";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Components.onCreateActivity(this, 0);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.simple_activity);

		if(savedInstanceState == null)
		{
			getFragmentManager().beginTransaction().replace(android.R.id.content,
					new DrugEditFragment(), "content").commit();
		}
	}

	@Override
	protected void onResume()
	{
		Components.onResumeActivity(this, 0);
		super.onResume();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if(keyCode == KeyEvent.KEYCODE_BACK)
		{
			final DrugEditFragment f = (DrugEditFragment) getFragmentManager().findFragmentByTag("content");
			f.onBackPressed();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}
}
