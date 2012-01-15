package at.caspase.rxdroid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Drug;


public class DrugSortActivity extends ListActivity
{
	private static final String TAG = DrugSortActivity.class.getName();

	private DraggableListView mListView;
	private int mFromPos = -1;

	@Override
	protected void onCreate(Bundle state)
	{
		super.onCreate(state);

		GlobalContext.set(getApplicationContext());
		Database.init();

		setContentView(R.layout.drug_sort);

		mListView = (DraggableListView) getListView();

		/*List<Drug> drugs = getAllDrugs();
		Collections.sort(drugs);

		ArrayList<String> drugNames = new ArrayList<String>();
		for(Drug drug : drugs)
			drugNames.add(drug.getName());*/

		String[] values = { "Foo 0", "Foo 1", "Foo 2", "Foo 3", "Foo 4", "Foo 5", "Foo 6", "Foo 7", "Foo 8", "Foo 9", "Foo 10", "Foo 11" };
		ArrayList<String> drugNames = new ArrayList<String>(Arrays.asList(values));

		mListView.setAdapter(
				new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1, android.R.id.text1, drugNames));

	}

	private static List<Drug> getAllDrugs()
	{
		List<Drug> drugs = Database.getAll(Drug.class);
		Collections.sort(drugs);
		return drugs;
	}

	/*@Override
	public void onStartDrag(View itemView)
	{
		itemView.setVisibility(View.INVISIBLE);

		Rect rect = new Rect();
		itemView.getHitRect(rect);
		//itemView.getDrawingRect(hitRect);
		//itemView.getGlobalVisibleRect(rect);

		int x = itemView.getLeft() + 1;
		int y = itemView.getTop() + 1;

		int firstViewPos = mListView.getFirstVisiblePosition();
		int adapterIndex = mListView.pointToPosition(x, y);

		//mFromPos = adapterIndex



		Toast toast = Toast.makeText(getBaseContext(), "(" + x + ", " + y + ")", Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.NO_GRAVITY, x, y);
		toast.show();

		mFromPos = mListView.pointToPosition(x, y);
		Log.d(TAG, "onStartDrag: x=" + x + ", y=" + y + ", mFromPos=" + mFromPos);

		if(mFromPos == ListView.INVALID_POSITION)
			mFromPos = -1;
	}

	@Override
	public void onDrag(int x, int y, ListView listView)
	{
		int pos = mListView.pointToPosition(x, y);
		//Log.d(TAG, "onDrag: x=" + x + ", y=" + y + ", pos=" + pos);

		if(mFromPos != -1 && pos != -1)
		{
			Log.d(TAG, "onDrag: x=" + x + ", y=" + y + ", pos=" + pos);
			//((DragNDropAdapter) mListView.getAdapter()).onDrop(mFromPos, pos);
			//mListView.invalidateViews();
		}
	}

	@Override
	public void onStopDrag(View itemView)
	{
		Log.d(TAG, "onStopDrag");

		itemView.setVisibility(View.VISIBLE);
		mFromPos = -1;
	}*/

}
