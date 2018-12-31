package com.sformica.torchlight.shaker.utils;

public abstract class Constant {

    // Time in millis
    public static int VIBRATOR_TIME_DEFAULT = 400;
    public static int VIBRATOR_TIME_MIN = 125;

    public static int HANDLER_DELAY = 250;

    public static int ANIMATION_BLINK_TIME = 1500;

    // Name of torchState preferences file & key
    public static final String SHARED_PREF_FILE =
            "com.sformica.torchlight.shaker.widget";
    public static final String COUNT_KEY = "torchState";

    // Name of torchState preferences file & key
    public static final String SHARED_PREF_FILE_SETTINGS =
            "com.sformica.torchlight.settings";
    public static final String START_ON_BOOT_KEY = "start_on_boot_key";
    public static final String CLOSE_ON_PAUSE_KEY = "close_on_pause";
    public static final String FIRST_SHOW_KEY = "first_show";
    public static final String FIRSTRUN_KEY = "firstrun";
    public static final String SENSIBILITY_KEY = "sensibility";
    public static final String SHAKE_KEY = "shake_enabled";
    public static final String BLINK_KEY = "blink_enabled";

    public static final int SENSIBILITY_LIGHT = 0;
    public static final int SENSIBILITY_MEDIUM = 1;
    public static final int SENSIBILITY_HARD = 2;

    public static final int MILLIS_TO_LAST_FLASH_ON = 2500;
    public static final int MILLIS_TO_BLINKING = 2500;
    public static final int MILLIS_TO_ENABLED_WIDGET = 5000;

}
