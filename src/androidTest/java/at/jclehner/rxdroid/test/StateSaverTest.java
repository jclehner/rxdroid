/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2013 Joseph Lehner <joseph.c.lehner@gmail.com>
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

package at.jclehner.rxdroid.test;

import android.os.Bundle;
import android.os.Parcelable;
import android.test.AndroidTestCase;
import android.util.Log;
import at.jclehner.androidutils.InstanceState;
import at.jclehner.androidutils.InstanceState.SaveState;

public class StateSaverTest extends AndroidTestCase
{
	static final int CAFEBABE = 0xcafebabe;
	static final int DEADBEEF = 0xdeadbeef;

	static class Value
	{
		Object value;

		Value() {
			// empty
		}

		Value(Object o) {
			value = o;
		}

		Object get() {
			return value;
		}
	}

	interface Restorable
	{
		Parcelable onSaveInstanceState();
		void restoreInstanceState(Parcelable state);
	}

	static class FooBase implements Restorable
	{
		@SaveState
		int i;

		@SaveState
		float f;

		@SaveState
		Value v;

		@Override
		public Parcelable onSaveInstanceState()
		{
			Log.v(getClass().getName(), "onSaveInstanceState");
			Parcelable superState = new Bundle();
			return InstanceState.createFrom(this, superState, null);
		}

		@Override
		public void restoreInstanceState(Parcelable state)
		{
			Log.v(getClass().getName(), "restoreInstanceState");
			InstanceState.restoreTo(this, state);
		}
	}

	static class FooExtended extends FooBase
	{
		@SaveState
		int i;

		@Override
		public Parcelable onSaveInstanceState()
		{
			Log.v(getClass().getName(), "onSaveInstanceState");
			Parcelable superState = super.onSaveInstanceState();
			return InstanceState.createFrom(this, superState, null);
		}

		@Override
		public void restoreInstanceState(Parcelable state)
		{
			super.restoreInstanceState(InstanceState.getSuperState(state));
			Log.v(getClass().getName(), "restoreInstanceState");
			InstanceState.restoreTo(this, state);
		}
	}

	public void testStateSaverWithBase()
	{
		FooBase fooBaseA = new FooBase();
		fooBaseA.i = 0xdeadbeef;
		fooBaseA.f = 3.1415927f;
		fooBaseA.v = new Value(0xdeadbeef);

		FooBase fooBaseB = new FooBase();
		fooBaseB.restoreInstanceState(fooBaseA.onSaveInstanceState());

		assertEquals(fooBaseA.i, fooBaseB.i);
		assertEquals(fooBaseA.f, fooBaseB.f);
		assertEquals(fooBaseA.v, fooBaseB.v);
	}

	public void testStateSaverWithExtended()
	{
		FooBase fooA = new FooExtended();
		fooA.i = 0xdeadbeef;
		fooA.f = 3.1415927f;
		fooA.v = new Value(0xdeadbeef);

		((FooExtended) fooA).i = 0xcafebabe;

		FooBase fooB = new FooExtended();
		fooB.restoreInstanceState(fooA.onSaveInstanceState());

		assertEquals(((FooExtended) fooA).i, ((FooExtended) fooB).i);

		assertEquals(fooA.i, fooB.i);
		assertEquals(fooA.f, fooB.f);
		assertEquals(fooA.v, fooB.v);
	}
}
