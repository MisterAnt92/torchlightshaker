package com.sformica.torchlight.shaker.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sformica.torchlight.shaker.BuildConfig;
import com.sformica.torchlight.shaker.utils.Utils;

public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = BootCompletedReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        if(BuildConfig.ENABLE_LOG)
            Log.d(TAG, "Start MyService on boot complete received");
        //Disable flash on boot
        Utils.setPrefsFlashState(context,false);
        // check flag start on boot
        boolean startOnBoot = Utils.getPrefsStartOnBoot(context);
        if(startOnBoot)
            Utils.startServiceShake(context);

        if(BuildConfig.ENABLE_LOG)
            Log.d(TAG, "startOnBoot:" + startOnBoot);
    }
}
