package at.caspase.rxdroid.test;

import android.os.Bundle;
import android.os.Parcelable;
import android.test.AndroidTestCase;
import android.util.Log;
import at.caspase.androidutils.StateSaver;
import at.caspase.androidutils.StateSaver.SaveState;

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
			return StateSaver.createInstanceState(this, superState, null);
		}

		@Override
		public void restoreInstanceState(Parcelable state) 
		{
			Log.v(getClass().getName(), "restoreInstanceState");
			StateSaver.restoreInstanceState(this, state);
		}
	}
	
	static class FooExtended extends FooBase
	{
		@SaveState
		int i = 0xcafebabe;
		
		@Override
		public Parcelable onSaveInstanceState()
		{
			Log.v(getClass().getName(), "onSaveInstanceState");
			Parcelable superState = super.onSaveInstanceState();
			return StateSaver.createInstanceState(this, superState, null);			
		}
		
		@Override
		public void restoreInstanceState(Parcelable state)
		{
			super.restoreInstanceState(StateSaver.getSuperState(state));
			Log.v(getClass().getName(), "restoreInstanceState");
			StateSaver.restoreInstanceState(this, state);
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
