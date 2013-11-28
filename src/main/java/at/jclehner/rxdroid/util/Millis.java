package at.jclehner.rxdroid.util;


public class Millis
{
	public static long days(int days) {
		return days * 86400000;
	}

	public static long hours(int hours) {
		return hours * 3600000;
	}

	public static long minutes(int minutes) {
		return minutes * 60000;
	}

	public static long seconds(int seconds) {
		return seconds * 1000;
	}
}
