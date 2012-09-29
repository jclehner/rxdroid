package at.caspase.rxdroid;

public final class Theme
{
	/* package */ static final String KEY = "theme_is_dark";

	public static final int LIGHT = R.style.LightTheme;
	public static final int DARK = R.style.DarkTheme;

	public static boolean isDark() {
		return Settings.getBoolean(KEY, Version.SDK_IS_PRE_HONEYCOMB);
	}

	public static void setDark(boolean dark) {
		Settings.putBoolean(KEY, dark);
	}

	public static int get() {
		return isDark() ? DARK : LIGHT;
	}

	private Theme() {}
}
