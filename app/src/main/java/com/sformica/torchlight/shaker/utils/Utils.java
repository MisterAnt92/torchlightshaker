package com.sformica.torchlight.shaker.utils;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.sformica.torchlight.shaker.R;
import com.sformica.torchlight.shaker.service.MyService;
import com.sformica.torchlight.shaker.widget.TorchWidget;

import java.util.ArrayList;

import static com.sformica.torchlight.shaker.utils.Constant.ANIMATION_BLINK_TIME;
import static com.sformica.torchlight.shaker.utils.Constant.BLINK_KEY;
import static com.sformica.torchlight.shaker.utils.Constant.CLOSE_ON_PAUSE_KEY;
import static com.sformica.torchlight.shaker.utils.Constant.COUNT_KEY;
import static com.sformica.torchlight.shaker.utils.Constant.FIRSTRUN_KEY;
import static com.sformica.torchlight.shaker.utils.Constant.FIRST_SHOW_KEY;
import static com.sformica.torchlight.shaker.utils.Constant.MILLIS_TO_ENABLED_WIDGET;
import static com.sformica.torchlight.shaker.utils.Constant.MILLIS_TO_LAST_FLASH_ON;
import static com.sformica.torchlight.shaker.utils.Constant.SENSIBILITY_KEY;
import static com.sformica.torchlight.shaker.utils.Constant.SHAKE_KEY;
import static com.sformica.torchlight.shaker.utils.Constant.SHARED_PREF_FILE;
import static com.sformica.torchlight.shaker.utils.Constant.SHARED_PREF_FILE_SETTINGS;
import static com.sformica.torchlight.shaker.utils.Constant.START_ON_BOOT_KEY;
import static com.sformica.torchlight.shaker.utils.Constant.VIBRATOR_TIME_DEFAULT;
import static com.squareup.seismic.ShakeDetector.SENSITIVITY_MEDIUM;

public abstract class Utils {


    public static void startServiceShake(Context mContext){
        mContext.startService(new Intent(mContext, MyService.class));
    }

    public static void vibrate(Context context){
        Vibrator mVib = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        mVib.vibrate(VIBRATOR_TIME_DEFAULT);
    }

    public static void vibrate(Context context,int time){
        Vibrator mVib = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        mVib.vibrate(time);
    }


    public static boolean lessThanNow(long time){
        return (System.currentTimeMillis() < ( time + MILLIS_TO_LAST_FLASH_ON));
    }


    /**
     * Return value for flash state
     * saved in prefs
     * @param mContext
     * @return
     */
    public static boolean getPrefsFlashState(Context mContext){
        SharedPreferences prefs =
                mContext.getSharedPreferences(SHARED_PREF_FILE, 0);
        return prefs.getBoolean(COUNT_KEY, false);
    }

    /**
     * Set value for flash state
     * in prefs
     * @param mContext
     * @param value
     */
    public static void setPrefsFlashState(Context mContext, boolean value){
        SharedPreferences prefs =
                mContext.getSharedPreferences(SHARED_PREF_FILE, 0);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putBoolean(COUNT_KEY, value);
        prefEditor.apply();
    }

    public static void updateWidget(Context context, boolean flashState){
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.layout_widget);
        ComponentName thisWidget = new ComponentName(context, TorchWidget.class);

        remoteViews.setImageViewResource(R.id.widgetOnOffImage,
                flashState ? R.drawable.ic_power_on : R.drawable.ic_power_off);
        appWidgetManager.updateAppWidget(thisWidget, remoteViews);
    }

    /**
     * Return value for start on boot state
     * saved in prefs
     * @param mContext
     * @return
     */
    public static boolean getPrefsStartOnBoot(Context mContext){
        SharedPreferences prefs =
                mContext.getSharedPreferences(SHARED_PREF_FILE_SETTINGS, 0);
        return prefs.getBoolean(START_ON_BOOT_KEY, true);
    }

    /**
     * Set value for Start on boot
     * in prefs
     * @param mContext
     * @param value
     */
    public static void setPrefsStartOnBoot(Context mContext, boolean value){
        SharedPreferences prefs =
                mContext.getSharedPreferences(SHARED_PREF_FILE_SETTINGS, 0);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putBoolean(START_ON_BOOT_KEY, value);
        prefEditor.apply();
    }

    /**
     * Return value for close on pause
     * saved in prefs
     * @param mContext
     * @return
     */
    public static boolean getPrefsCloseOnPause(Context mContext){
        SharedPreferences prefs =
                mContext.getSharedPreferences(SHARED_PREF_FILE_SETTINGS, 0);
        return prefs.getBoolean(CLOSE_ON_PAUSE_KEY, false);
    }

    /**
     * Set value for close on pause
     * in prefs
     * @param mContext
     * @param value
     */
    public static void setPrefsCloseOnPause(Context mContext, boolean value){
        SharedPreferences prefs =
                mContext.getSharedPreferences(SHARED_PREF_FILE_SETTINGS, 0);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putBoolean(CLOSE_ON_PAUSE_KEY, value);
        prefEditor.apply();
    }

    public static boolean getPrefsFirstShow(Context mContext){
        SharedPreferences prefs =
                mContext.getSharedPreferences(SHARED_PREF_FILE_SETTINGS, 0);
        return prefs.getBoolean(FIRST_SHOW_KEY, true);
    }

    public static void setPrefsShakeEnabled(Context mContext, boolean value){
        SharedPreferences prefs =
                mContext.getSharedPreferences(SHARED_PREF_FILE_SETTINGS, 0);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putBoolean(SHAKE_KEY, value);
        prefEditor.apply();
    }

    public static boolean getPrefsShakeEnabled(Context mContext){
        SharedPreferences prefs =
                mContext.getSharedPreferences(SHARED_PREF_FILE_SETTINGS, 0);
        return prefs.getBoolean(SHAKE_KEY, false);
    }


    public static void setPrefsSensibility(Context mContext, int value){
        SharedPreferences prefs =
                mContext.getSharedPreferences(SHARED_PREF_FILE_SETTINGS, 0);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putInt(SENSIBILITY_KEY, value);
        prefEditor.apply();
    }

    public static int getPrefsSensibility(Context mContext){
        SharedPreferences prefs =
                mContext.getSharedPreferences(SHARED_PREF_FILE_SETTINGS, 0);
        return prefs.getInt(SENSIBILITY_KEY, 0);
    }

    public static void setPrefsFirstShow(Context mContext, boolean value){
        SharedPreferences prefs =
                mContext.getSharedPreferences(SHARED_PREF_FILE_SETTINGS, 0);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putBoolean(FIRST_SHOW_KEY, value);
        prefEditor.apply();
    }

    public static boolean getPrefsFirstRun(Context mContext){
        SharedPreferences prefs =
                mContext.getSharedPreferences(SHARED_PREF_FILE_SETTINGS, 0);
        return prefs.getBoolean(FIRSTRUN_KEY, true);
    }

    public static void setPrefsFirstRun(Context mContext, boolean value){
        SharedPreferences prefs =
                mContext.getSharedPreferences(SHARED_PREF_FILE_SETTINGS, 0);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putBoolean(FIRSTRUN_KEY, value);
        prefEditor.apply();
    }

    public static boolean getPrefsIsBlinking(Context mContext){
        SharedPreferences prefs =
                mContext.getSharedPreferences(SHARED_PREF_FILE_SETTINGS, 0);
        return prefs.getBoolean(BLINK_KEY, false);
    }

    public static void setPrefsIsBlinking(Context mContext, boolean value){
        SharedPreferences prefs =
                mContext.getSharedPreferences(SHARED_PREF_FILE_SETTINGS, 0);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putBoolean(BLINK_KEY, value);
        prefEditor.apply();
    }

    public static void blinkView(View mView){
        Animation animation = new AlphaAnimation(1, 0);
        animation.setDuration(ANIMATION_BLINK_TIME);
        animation.setInterpolator(new LinearInterpolator());
        animation.setRepeatCount(3);
        animation.setRepeatMode(Animation.REVERSE);
        mView.startAnimation(animation);
    }

    public static boolean haveCameraFlash(Context mContext) {
        PackageManager pm = mContext.getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) |
                !pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            return false;
        } else {
            return true;
        }
    }
}
