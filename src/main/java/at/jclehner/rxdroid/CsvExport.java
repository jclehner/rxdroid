package at.jclehner.rxdroid;

import android.content.Context;
import android.util.Log;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entries;
import at.jclehner.rxdroid.db.Schedule;

public class CsvExport
{
	public static void export(Context context, int patientId)
	{
		StringBuilder sb = new StringBuilder("\"\"");

		final int[] headerIds = {
				R.string._title_morning,
				R.string._title_noon,
				R.string._title_evening,
				R.string._title_night,
				R.string._title_repeat,
		};

		for (int id : headerIds) {
			sb.append(",\"" + context.getString(id) + "\"");
		}
		sb.append("\r\n");

		for (Drug drug : Entries.getAllDrugs(patientId)) {
			if (!drug.isActive()) {
				continue;
			}

			sb.append(quote(drug.getName()) + ",");
			for (Fraction f : drug.getSimpleSchedule()) {
				sb.append(quote(f.toString()) + ",");
			}

			final String summary;
			if (drug.getRepeatMode() == Schedule.REPEAT_DAILY) {
				summary = context.getString(R.string._title_daily);
			} else {
				summary = DrugEditFragment.RepeatModePreferenceController.getSummary(
						context, drug.getRepeatMode(), drug.getRepeatArg(), drug.getRepeatOrigin());
			}
			sb.append(quote(summary != null ? summary : ""));
			sb.append("\r\n");
		}

		Log.i("CsvExport", sb.toString());
	}

	private static String quote(String str)
	{
		return "\"" + str + "\"";
	}

}
