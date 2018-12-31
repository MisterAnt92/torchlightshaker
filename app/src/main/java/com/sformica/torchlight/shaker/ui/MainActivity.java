package com.sformica.torchlight.shaker.ui;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.sformica.torchlight.shaker.BuildConfig;
import com.sformica.torchlight.shaker.R;
import com.sformica.torchlight.shaker.camera.CameraNormal;
import com.sformica.torchlight.shaker.camera.ICamera;
import com.sformica.torchlight.shaker.camera.CameraMarshmallow;
import com.sformica.torchlight.shaker.messageventbus.UpdateBlinkMode;
import com.sformica.torchlight.shaker.messageventbus.UpdateStateMessage;
import com.sformica.torchlight.shaker.service.MyService;
import com.sformica.torchlight.shaker.utils.Constant;
import com.sformica.torchlight.shaker.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.fabric.sdk.android.Fabric;
import ir.neo.stepbarview.StepBarView;

import static android.os.Build.VERSION.SDK_INT;
import static com.sformica.torchlight.shaker.utils.Constant.SENSIBILITY_MEDIUM;

public class MainActivity extends BaseActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.btnSwitch)
    ImageButton btnSwitch;

    @BindView(R.id.switchBlinking)
    Switch switchBlinking;

    @BindView(R.id.cbPause)
    CheckBox pauseStateCheckBox;

    @BindView(R.id.cbOnBoot)
    CheckBox onBootCheckBox;

    @BindView(R.id.cbShake)
    CheckBox shakeEnabledCheckBox;

    @BindView(R.id.myStepBarView)
    StepBarView stepBarView;

    @BindView(R.id.myStepBarViewTitle)
    TextView stepBarViewTitle;

    private MyService mService = null;
    private Activity thisActivity = this;
    private ICamera mCamera;

    private NoFlashDialog noFlashDialog;
    private WelcomeDialog welcomeDialog;

    private int sensibilityState = SENSIBILITY_MEDIUM;
    private long mLast;

    private boolean mBound = false;
    private boolean startOnBoot = true;
    private boolean endWhenPaused = true;
    private boolean shakeEnabled = false;
    private boolean isBlinking = false;

    private boolean flashState = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Fabric.with(this, new Crashlytics());
        ButterKnife.bind(this);

        thisActivity = this;
        flashState = false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateCheckBox();
        checkFirstRun();
        init();

        Utils.blinkView(btnSwitch);

        //Bind myService
        Intent mService = new Intent(this, MyService.class);
        bindService(mService, mServerConn, Context.BIND_AUTO_CREATE);
    }

    private void updateCheckBox(){
        endWhenPaused = Utils.getPrefsCloseOnPause(this);
        startOnBoot = Utils.getPrefsStartOnBoot(this);
        sensibilityState = Utils.getPrefsSensibility(this);
        shakeEnabled = Utils.getPrefsShakeEnabled(this);

        if(pauseStateCheckBox != null) {
            pauseStateCheckBox.setChecked(endWhenPaused);
            pauseStateCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    endWhenPaused = !endWhenPaused;
                    Utils.setPrefsCloseOnPause( thisActivity, endWhenPaused);

                    if(BuildConfig.ENABLE_LOG)
                        Log.d(TAG, "change value -> endWhenPaused:" + endWhenPaused);
                }
            });
        }

        if(onBootCheckBox != null) {
            onBootCheckBox.setChecked(startOnBoot);
            onBootCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startOnBoot = !startOnBoot;
                    Utils.setPrefsStartOnBoot( thisActivity, startOnBoot);

                    if(BuildConfig.ENABLE_LOG)
                        Log.d(TAG, "change value -> startOnBoot:" + startOnBoot);
                }
            });
        }

        if(shakeEnabledCheckBox != null) {
            shakeEnabledCheckBox.setChecked(shakeEnabled);
            shakeEnabledCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    shakeEnabled = !shakeEnabled;
                    Utils.setPrefsShakeEnabled( thisActivity, shakeEnabled);

                    if(BuildConfig.ENABLE_LOG)
                        Log.d(TAG, "change value -> shakeEnabled:" + shakeEnabled);

                    updateStepBarViewState(shakeEnabled);
                }
            });
        }

        if(stepBarView != null) {
            updateStepBarViewState(shakeEnabled);
        }

        updateBlinkSwitch();
    }

    private void updateBlinkSwitch(){
        isBlinking = Utils.getPrefsIsBlinking(this);
        if(switchBlinking != null && switchBlinking.isShown())
            switchBlinking.setChecked(isBlinking);
    }

    private void updateStepBarViewState(boolean value){
        if(value){
            stepBarViewTitle.setVisibility(View.VISIBLE);
            stepBarView.setVisibility(View.VISIBLE);

            stepBarView.setReachedStep(sensibilityState);
            stepBarView.setOnStepChangeListener(new StepBarView.OnStepChangeListener() {
                @Override
                public void onStepChanged(int index) {

                    if(BuildConfig.ENABLE_LOG)
                        Log.d(TAG, "change value -> Sensibility:" + index);

                    sensibilityState = index;
                    Utils.setPrefsSensibility( thisActivity, sensibilityState);
                    // Update sensibility level in shake action
                    updateServiceSensibility();
                }
            });
        } else {
            stepBarViewTitle.setVisibility(View.INVISIBLE);
            stepBarView.setVisibility(View.INVISIBLE);
        }
    }

    protected ServiceConnection mServerConn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            if(BuildConfig.ENABLE_LOG)
                Log.d(TAG, "onServiceConnected");

            MyService.MyBinder binder = (MyService.MyBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            if(BuildConfig.ENABLE_LOG)
                Log.d(TAG, "onServiceDisconnected");

            mBound = false;
        }
    };

    private void updateServiceSensibility(){
        if(BuildConfig.ENABLE_LOG)
            Log.d(TAG, " updateServiceSensibility()");

        if(mServerConn == null && !mBound)
            return;

        if(mService != null)
            mService.updateSensibility();
    }

    @OnClick(R.id.switchBlinking)
    public void switchBlinking(){
        // Set value
        isBlinking = ! isBlinking;
        Utils.setPrefsIsBlinking(getApplicationContext(),isBlinking);
        //
        if(mService != null)
            mService.blinkingMode();
    }

    /**
     * Check if is the first time
     * you open the application
     */
    private void checkFirstRun(){
        boolean firstShow = Utils.getPrefsFirstShow(thisActivity);
        Utils.setPrefsFirstShow(thisActivity, firstShow);

        boolean firstRun = Utils.getPrefsFirstRun(thisActivity);
        if (firstRun) {

            if(!Utils.haveCameraFlash(thisActivity)){

                btnSwitch.setEnabled(false);
                pauseStateCheckBox.setEnabled(false);
                onBootCheckBox.setEnabled(false);
                shakeEnabledCheckBox.setEnabled(false);
                updateStepBarViewState(false);

                noFlashDialog = new NoFlashDialog();
                noFlashDialog.setCancelable(false);
                noFlashDialog.show(getFragmentManager(), "NoFlashDialog");

            } else {
                welcomeDialog = new WelcomeDialog();
                welcomeDialog.setCancelable(false);
                welcomeDialog.show(getFragmentManager(), "WelcomeDialog");

                Utils.setPrefsShakeEnabled(thisActivity, true);
                Utils.setPrefsCloseOnPause(thisActivity, true);
                Utils.setPrefsFirstRun(thisActivity, false);
                Utils.setPrefsSensibility( thisActivity, sensibilityState);

                updateCheckBox();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Register for service msg
        EventBus.getDefault().register(this);

        Intent intent = new Intent(this, MyService.class);
        bindService(intent, mServerConn, Context.BIND_AUTO_CREATE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(UpdateStateMessage event) {
        if(btnSwitch != null && btnSwitch.isShown()){
            flashState = Utils.getPrefsFlashState(thisActivity);
            updateTorchImage();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(UpdateBlinkMode event) {
        if(switchBlinking != null && switchBlinking.isShown()){
            updateBlinkSwitch();
        }
    }

    private void init() {
        // if device support camera?
        if (!Utils.haveCameraFlash(this)) {

            if(BuildConfig.ENABLE_LOG)
                Log.e(TAG, "E:Device has no camera!");

            return;
        }

        // Check Android Version
        if(SDK_INT >= Build.VERSION_CODES.M) {
            mCamera = new CameraMarshmallow();
        } else {
            mCamera = new CameraNormal();
        }

        // set up camera
        mCamera.init(this);

        btnSwitch.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // can we have permissions that are revoked?
                if(SDK_INT >= Build.VERSION_CODES.M) {
                    // check if we have the permission we need -> if not request it and turn on the light afterwards
                    if (ContextCompat.checkSelfPermission(thisActivity, Manifest.permission.CAMERA) !=
                            PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(thisActivity,
                                new String[]{Manifest.permission.CAMERA},0);
                        return;
                    }
                }

                toggleCamera(!flashState);
            }
        });

        Utils.startServiceShake(this);
        updateTorchImage();
    }

    private void updateTorchImage(){
        flashState = Utils.getPrefsFlashState(thisActivity);
        btnSwitch.setImageResource(flashState ? R.drawable.ic_power_on : R.drawable.ic_power_off);
    }

    private void toggleCamera(boolean enable) {
        if(Utils.lessThanNow(mLast))
            return;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Utils.vibrate(getApplicationContext(), Constant.VIBRATOR_TIME_MIN);
            }
        }, Constant.HANDLER_DELAY);

        if(mCamera.toggle(enable))
            flashState = enable;

        if(BuildConfig.ENABLE_LOG)
            Log.d(TAG, "FlashState:" + enable);

        btnSwitch.setImageResource(enable ? R.drawable.ic_power_on : R.drawable.ic_power_off);
        Utils.setPrefsFlashState(thisActivity, flashState);
        Utils.updateWidget(thisActivity, flashState);
        // Update value
        mLast = System.currentTimeMillis();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // yay, we got the permission -> turn on the light!
                    toggleCamera(!flashState);
                } else {
                    Toast.makeText(this, getString(R.string.cannot_use_this) ,Toast.LENGTH_SHORT).show();
                    // permission denied, boo!
                }
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unregister for service msg
        EventBus.getDefault().unregister(this);
        // Unregister service
        unbindService(mServerConn);
        mBound = false;

        if(!flashState) {
            close();
        } else if(endWhenPaused) {
            stop();
        }
    }

    private void stop(){
        flashState = false;
        if(mCamera != null){
            mCamera.toggle(false);
            mCamera.release();
        }
    }

    private void close(){
        flashState = false;
        if(mCamera != null)
            mCamera.release();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected int getNavigationDrawerID() {
        return 0;
    }

    public static class WelcomeDialog extends DialogFragment {

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater i = getActivity().getLayoutInflater();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setView(i.inflate(R.layout.welcome_dialog, null));
            builder.setIcon(R.mipmap.ic_torch);
            builder.setTitle(getActivity().getString(R.string.welcome));
            builder.setPositiveButton(getActivity().getString(R.string.okay), null);
            builder.setNegativeButton(getActivity().getString(R.string.viewhelp), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    ((MainActivity)getActivity()).goToNavigationItem(R.id.nav_help);
                }
            });

            return builder.create();
        }
    }

    public static class NoFlashDialog extends DialogFragment {

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater i = getActivity().getLayoutInflater();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setView(i.inflate(R.layout.no_flash_dialog, null));
            builder.setIcon(R.mipmap.ic_torch);
            builder.setTitle(getActivity().getString(R.string.welcome));
            builder.setNegativeButton(getActivity().getString(R.string.okay), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getActivity().finish();
                }
            });

            return builder.create();
        }
    }


}
