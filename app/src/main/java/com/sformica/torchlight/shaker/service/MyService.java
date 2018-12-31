package com.sformica.torchlight.shaker.service;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.sformica.torchlight.shaker.BuildConfig;
import com.sformica.torchlight.shaker.R;
import com.sformica.torchlight.shaker.camera.CameraMarshmallow;
import com.sformica.torchlight.shaker.camera.CameraNormal;
import com.sformica.torchlight.shaker.camera.ICamera;
import com.sformica.torchlight.shaker.messageventbus.UpdateBlinkMode;
import com.sformica.torchlight.shaker.messageventbus.UpdateStateMessage;
import com.sformica.torchlight.shaker.utils.Constant;
import com.sformica.torchlight.shaker.utils.Utils;
import com.squareup.seismic.ShakeDetector;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static android.os.Build.VERSION.SDK_INT;
import static com.sformica.torchlight.shaker.utils.Constant.MILLIS_TO_LAST_FLASH_ON;
import static com.sformica.torchlight.shaker.utils.Constant.SENSIBILITY_HARD;
import static com.sformica.torchlight.shaker.utils.Constant.SENSIBILITY_LIGHT;
import static com.sformica.torchlight.shaker.utils.Constant.SENSIBILITY_MEDIUM;
import static com.sformica.torchlight.shaker.utils.Utils.getPrefsCloseOnPause;
import static com.sformica.torchlight.shaker.utils.Utils.lessThanNow;

public class MyService extends Service implements ShakeDetector.Listener {

    private static final String TAG = MyService.class.getSimpleName();

    private final IBinder mBinder = new MyBinder();

    private ArrayList<Long> mLastSwitchOn = new ArrayList();

    private Runnable taskBlinkMode;
    private ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor();

    public ShakeDetector mSahkerDetector;
    public SensorManager sensorManager;
    private ICamera mCamera;

    private long mLastFlash;

    private boolean blinkingMode = false;
    private boolean isFlashingBlinking = false;

    private boolean flashState = false;

    ////////////////////////

    public MyService() {}

    public void onCreate() {
        super.onCreate();

        if(BuildConfig.ENABLE_LOG)
            Log.d(TAG, "onCreate()");

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSahkerDetector = new ShakeDetector(this);
        mSahkerDetector.start(sensorManager);

        mLastFlash = -1;
    }

    @Override
    public void onDestroy() {

        if(BuildConfig.ENABLE_LOG)
            Log.d(TAG, "onDestroy()");

        if(!flashState)
            close();

        worker.shutdown();

        super.onDestroy();
    }

    public int onStartCommand (Intent intent, int flags, int startId){
        super.onStartCommand(intent, flags, startId);
        //your code here
        init();
        //
        return START_STICKY;
    }

    public class MyBinder extends Binder {
        public MyService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MyService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void updateSensibility(){

        if(BuildConfig.ENABLE_LOG)
            Log.d(TAG, "Sensibility changed!");

        if(mSahkerDetector == null)
            return;

        int sensibility = Utils.getPrefsSensibility(this);
        switch (sensibility){
            case SENSIBILITY_LIGHT:
                sensibility = ShakeDetector.SENSITIVITY_LIGHT;
                break;
            case SENSIBILITY_MEDIUM:
                sensibility = ShakeDetector.SENSITIVITY_MEDIUM;
                break;
            case SENSIBILITY_HARD:
                sensibility = ShakeDetector.SENSITIVITY_HARD;
                break;
        }
        mSahkerDetector.setSensitivity(sensibility);
        mSahkerDetector.start(sensorManager);
    }

    public void blinkingMode(){

        if(BuildConfig.ENABLE_LOG)
            Log.d(TAG, "Blinking mode changed!");

        blinkingMode = Utils.getPrefsIsBlinking(this);

        if(blinkingMode){

            taskBlinkMode = new Runnable() {
                public void run() {

                    if (!blinkingMode) {

                        toggleCamera(false);
                        Utils.updateWidget(getApplicationContext(), false);
                    } else {

                        isFlashingBlinking = !isFlashingBlinking;
                        toggleCamera(isFlashingBlinking);
                        blinkingMode = Utils.getPrefsIsBlinking(getApplicationContext());

                        executeScheduler();
                    }
                }
            };

            executeScheduler();
        }
    }

    private void executeScheduler(){

        try {
            worker.schedule(taskBlinkMode, Constant.MILLIS_TO_BLINKING, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            Log.e(TAG, "Exception on scheduler ", ex);
        }
    }

    private void init() {
        if(BuildConfig.ENABLE_LOG)
            Log.d(TAG, "init()");

        if(!Utils.haveCameraFlash(this))
            return;

        // Check Android Version
        if(SDK_INT >= Build.VERSION_CODES.M) {
            mCamera = new CameraMarshmallow();
        } else {
            mCamera = new CameraNormal();
        }

        // set up camera
        mCamera.init(this);
    }

    private void toggleCamera(boolean enable) {
        if(mCamera.toggle(enable))
            flashState = enable;
        // Save last flash value state
        Utils.setPrefsFlashState(this, enable);
        EventBus.getDefault().post(new UpdateStateMessage());
        // Last switch
        mLastFlash = System.currentTimeMillis();
    }

    private void close(){
        flashState = false;
        if(mCamera == null)
            return;

        mCamera.release();
    }

    @Override
    public void hearShake() {
        Context context = this; // or you can replace **'this'** with your **ActivityName.this**
        boolean isShaker = Utils.getPrefsShakeEnabled(context);

        if(BuildConfig.ENABLE_LOG)
            Log.d(TAG, "Received shake event! isShaker " + isShaker);

        if(Utils.getPrefsIsBlinking(context) && worker!= null){
            //worker.shutdown();
            toggleCamera(false);
            // Vibrate
            Utils.vibrate(context);
            //Update widget
            Utils.updateWidget(getApplicationContext(), false);
            Utils.setPrefsIsBlinking(context, false);
            EventBus.getDefault().post(new UpdateBlinkMode());
            return;
        }

        if(lessThanNow(mLastFlash)){

            if(BuildConfig.ENABLE_LOG)
                Log.d(TAG, "LESS THAN " + MILLIS_TO_LAST_FLASH_ON);

            if(BuildConfig.SHOW_TOAST)
                Toast.makeText(this, "LESS THAN " + MILLIS_TO_LAST_FLASH_ON + " MILLIS", Toast.LENGTH_SHORT).show();
            return;

        } else {

            if(BuildConfig.ENABLE_LOG)
                Log.d(TAG, "MORE THAN " + MILLIS_TO_LAST_FLASH_ON);

            if(BuildConfig.SHOW_TOAST)
                Toast.makeText(this, "MORE THAN " + MILLIS_TO_LAST_FLASH_ON + " MILLIS", Toast.LENGTH_SHORT).show();
        }

        if(isShaker){

            if(BuildConfig.ENABLE_LOG)
                Log.d(TAG, "getPrefsShakeEnabled true");

            //get value
            flashState = Utils.getPrefsFlashState(context);
            toggleCamera(!flashState);
            // Vibrate
            Utils.vibrate(context);
            //Update widget
            Utils.updateWidget(context, flashState);

            mLastSwitchOn.add(mLastFlash);
        }


    }

}
