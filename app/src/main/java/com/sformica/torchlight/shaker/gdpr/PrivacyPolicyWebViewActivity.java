package com.sformica.torchlight.shaker.gdpr;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

import com.sformica.torchlight.shaker.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PrivacyPolicyWebViewActivity extends AppCompatActivity {

    private static final String TAG = PrivacyPolicyWebViewActivity.class.getSimpleName();

    @BindView(R.id.webViewGDPR)
    WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy_web_view);

        ButterKnife.bind(this);
        Log.d(TAG, "onCreate()");

        mWebView.loadUrl("file:///android_asset/privacy_policy.html");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // chromium, enable hardware acceleration
            mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            // older android version, disable hardware acceleration
            mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

}
