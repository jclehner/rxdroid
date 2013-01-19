package at.jclehner.rxdroid.util;

public final class Exceptions
{
	public static class UnexpectedValueInSwitch extends RuntimeException
	{
		public UnexpectedValueInSwitch() {
			super("Unexpected value in switch statement");
		}

		public UnexpectedValueInSwitch(long value) {
			super("Unexpected value in switch statement: " + value);
		}
	}

	private Exceptions() {}
}
