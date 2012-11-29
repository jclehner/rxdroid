package at.jclehner.rxdroid.db.v54;

import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entry;

public final class UpgradeHelper
{
	public static void updateDrugTableEntry(Entry entry) {
		((Drug) entry).setSortRank(Integer.MAX_VALUE);
	}

	private UpgradeHelper() {}
}
