/*
 * Copyright (C) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sformica.torchlight.shaker.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.sformica.torchlight.shaker.BuildConfig;
import com.sformica.torchlight.shaker.R;
import com.sformica.torchlight.shaker.camera.CameraMarshmallow;
import com.sformica.torchlight.shaker.camera.CameraNormal;
import com.sformica.torchlight.shaker.camera.ICamera;
import com.sformica.torchlight.shaker.messageventbus.UpdateStateMessage;
import com.sformica.torchlight.shaker.utils.Utils;

import org.greenrobot.eventbus.EventBus;

import static android.os.Build.VERSION.SDK_INT;
import static com.sformica.torchlight.shaker.utils.Constant.MILLIS_TO_ENABLED_WIDGET;

/**
 * App widget provider class, to handle update broadcast intents and updates
 * for the app widget.
 */
public class TorchWidget extends AppWidgetProvider {

    private static final String TAG = TorchWidget.class.getSimpleName();

    private ICamera mCamera;
    private Context mContext;

    private static boolean isFirstState = false;
    private static boolean flashState = false;
    private static long mLast = -1;

    /**
     * Update a single app widget.  This is a helper method for the standard
     * onUpdate() callback that handles one widget update at a time.
     *
     * @param context          The application context.
     * @param appWidgetManager The app widget manager.
     * @param appWidgetId      The current app widget id.
     */
    private void updateAppWidget(Context context,
                                AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        if(BuildConfig.ENABLE_LOG)
            Log.d(TAG, "updateAppWidget()");

        mContext = context;
        // Get flash value from prefs.
        flashState = Utils.getPrefsFlashState(mContext);

        // Construct the RemoteViews object.
        RemoteViews views = new RemoteViews(mContext.getPackageName(),
                R.layout.layout_widget);
        // Init torch manager
        init();

        //Change torch state
        if(BuildConfig.ENABLE_LOG)
            Log.d(TAG, "changeTorchState:" + flashState + " isFirstState:" + isFirstState);

        if(Utils.lessThanNow(mLast))
            return;

        if(!isFirstState) {
            if (flashState) {
                views.setImageViewResource(R.id.widgetOnOffImage,
                        R.drawable.ic_power_off);

                disableTorch();
                Utils.vibrate(context);

                if (BuildConfig.SHOW_TOAST)
                    Toast.makeText(mContext, "DEBUG:Torch off", Toast.LENGTH_LONG).show();
            } else {
                views.setImageViewResource(R.id.widgetOnOffImage,
                        R.drawable.ic_power_on);
                toggleCamera(!flashState);
                Utils.vibrate(context);

                if (BuildConfig.SHOW_TOAST)
                    Toast.makeText(mContext, "DEBUG:Torch on", Toast.LENGTH_LONG).show();
            }
            // Save count back to prefs.
            Utils.setPrefsFlashState(mContext, flashState);
            EventBus.getDefault().post(new UpdateStateMessage());
        }

        mLast = System.currentTimeMillis();

        // Setup update button to send an update request as a pending intent.
        Intent intentUpdate = new Intent(mContext, TorchWidget.class);

        // The intent action must be an app widget update.
        intentUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        // Include the widget ID to be updated as an intent extra.
        int[] idArray = new int[]{appWidgetId};
        intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idArray);

        // Wrap it all in a pending intent to send a broadcast.
        // Use the app widget ID as the request code (third argument) so that
        // each intent is unique.
        PendingIntent pendingUpdate = PendingIntent.getBroadcast(mContext,
                appWidgetId, intentUpdate, PendingIntent.FLAG_UPDATE_CURRENT);

        // Assign the pending intent to the button onClick handler
        views.setOnClickPendingIntent(R.id.widgetOnOffImage, pendingUpdate);

        // Instruct the widget manager to update the widget.
        appWidgetManager.updateAppWidget(appWidgetId, views);

        // Change firstState
        isFirstState = false;
    }

    /**
     * Override for onUpdate() method, to handle all widget update requests.
     * @param context          The application context.
     * @param appWidgetManager The app widget manager.
     * @param appWidgetIds     An array of the app widget IDs.
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {

        if(BuildConfig.ENABLE_LOG)
            Log.d(TAG, "onUpdate()");

        // There may be multiple widgets active, so update all of them.
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);

        if(BuildConfig.ENABLE_LOG)
            Log.d(TAG, "onEnabled()");

        isFirstState = true;
        if(BuildConfig.SHOW_TOAST)
            Toast.makeText(context, context.getText(R.string.widget_added), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);

        if(BuildConfig.ENABLE_LOG)
            Log.d(TAG, "onDisabled()");

        Toast.makeText(context, context.getText(R.string.widget_removed), Toast.LENGTH_LONG).show();

        disableTorch();
        Utils.setPrefsFlashState(context, false);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);

        if(BuildConfig.ENABLE_LOG)
            Log.d(TAG, "onDeleted()");
    }

    private void init() {
        if(!Utils.haveCameraFlash(mContext))
            return;

        // Check Android Version
        if(SDK_INT >= Build.VERSION_CODES.M) {
            mCamera = new CameraMarshmallow();
        } else {
            mCamera = new CameraNormal();
        }
        // set up camera
        mCamera.init(mContext);
    }

    private void toggleCamera(boolean enable) {
        if(mCamera == null)
            return;

        if(mCamera.toggle(enable))
            flashState = enable;
    }

    private void disableTorch(){
        stop();
        close();
    }

    private void stop(){
        if(mCamera == null)
            return;

        flashState = false;
        mCamera.toggle(false);
        mCamera.release();
    }

    private void close(){
        if(mCamera == null)
            return;

        flashState = false;
        mCamera.release();
    }
}


