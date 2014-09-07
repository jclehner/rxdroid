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

package at.jclehner.rxdroid.util;

public class WrappedCheckedException extends RuntimeException
{
	private static final long serialVersionUID = 7323462591621207126L;

	public WrappedCheckedException(Exception ex) {
		super(ex);
	}

	public WrappedCheckedException(String detailMessage, Exception ex) {
		super(detailMessage, ex);
	}

	/*public WrappedCheckedException(WrappedCheckedException ex) {
		super(ex.getCause());
	}

	public WrappedCheckedException(String detailMessage, WrappedCheckedException ex) {
		super(detailMessage, ex.getCause());
	}*/

	public Throwable getRootCause()
	{
		Throwable cause = getFirstWrappedCause();
		while(cause.getCause() != null)
			cause = cause.getCause();
		return cause;
	}

	@Override
	public Throwable getCause() {
		return getFirstWrappedCause();
	}

	@Override
	public String toString()
	{
		final Throwable cause = getFirstWrappedCause();
		if(cause == null)
			return super.toString();

		final String message = getMessage();
		if(message != null)
			return cause.toString() + ": " + message;

		return cause.toString();
	}

	public Throwable getFirstWrappedCause()
	{
		Throwable t = super.getCause();

		//while(t != null && t instanceof WrappedCheckedException)
		//	t = t.getCause();

		if(t != null && t instanceof WrappedCheckedException)
			return t.getCause();

		return t;
	}

	public Class<?> getCauseType()
	{
		final Throwable cause = getCause();
		return cause == null ? null : cause.getClass();
	}

}
