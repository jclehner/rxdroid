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

/*
 *  Copyright 2010 Kevin Gaudin
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package at.jclehner.rxdroid.util;

/*import org.acra.ACRA;
import org.acra.ACRAConstants;
import org.acra.collector.CrashReportData;
import org.acra.ReportField;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import at.jclehner.rxdroid.Version;*/

/**
 * Send reports through an email intent. The user will be asked to chose his
 * preferred email client. Included report fields can be defined using
 * {@link org.acra.annotation.ReportsCrashes#customReportContent()}. Crash receiving mailbox has to be
 * defined with {@link ReportsCrashes#mailTo()}.
 */
/*public class EmailIntentSender implements ReportSender
{
	private final Context mContext;

	public EmailIntentSender(Context ctx) {
		mContext = ctx;
	}

	@Override
	public void send(CrashReportData errorContent) throws ReportSenderException {

		final String subject = "[CRASH] " + Version.get();
		final String body = buildBody(errorContent);

		final Intent emailIntent = new Intent(android.content.Intent.ACTION_SENDTO);
		emailIntent.setData(Uri.fromParts("mailto", ACRA.getConfig().mailTo(), null));
		emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
		emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, body);
		//emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { ACRA.getConfig().mailTo() });

		mContext.startActivity(emailIntent);
	}

	private String buildBody(CrashReportData errorContent) {
		ReportField[] fields = ACRA.getConfig().customReportContent();
		if(fields.length == 0) {
			fields = ACRAConstants.DEFAULT_MAIL_REPORT_FIELDS;
		}

		final StringBuilder builder = new StringBuilder();
		for (ReportField field : fields) {
			builder.append(field.toString()).append("=");
			builder.append(errorContent.get(field));
			builder.append('\n');
		}
		return builder.toString();
	}
}
*/

