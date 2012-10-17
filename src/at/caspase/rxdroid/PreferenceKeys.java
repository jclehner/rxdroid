package at.caspase.rxdroid;

public final class PreferenceKeys
{
	public static final String KEY_USE_LED = get(R.string.key_use_led);
	public static final String KEY_USE_SOUND = get(R.string.key_use_sound);
	public static final String KEY_USE_VIBRATOR = get(R.string.key_use_vibrator);
	public static final String KEY_LOCKSCREEN_TIMEOUT = get(R.string.key_lockscreen_timeout);
	public static final String KEY_SCRAMBLE_NAMES = get(R.string.key_scramble_names);
	public static final String KEY_PIN = get(R.string.key_pin);
	public static final String KEY_LOW_SUPPLY_THRESHOLD = get(R.string.key_low_supply_threshold);
	public static final String KEY_ALARM_MODE = get(R.string.key_alarm_mode);
	public static final String KEY_SHOW_SUPPLY_MONITORS = get(R.string.key_show_supply_monitors);
	public static final String KEY_LAST_MSG_HASH = get(R.string.key_last_msg_hash);
	public static final String KEY_VERSION = get(R.string.key_version);
	public static final String KEY_LICENSES = get(R.string.key_licenses);
	public static final String KEY_HISTORY_SIZE = get(R.string.key_history_size);
	public static final String KEY_THEME_IS_DARK = get(R.string.key_theme_is_dark);
	public static final String KEY_NOTIFICATION_SOUND = get(R.string.key_notification_sound);

	private static String get(int resId) {
		return Application.getContext().getString(resId);
	}

	private PreferenceKeys() {}
}
