package com.sformica.torchlight.shaker.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.sformica.torchlight.shaker.R;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by SFormica on 3.06.2018.
 */
public class AboutActivity extends AppCompatActivity {


    @BindView(R.id.iconDev)
    ImageView mIcon;

    @BindView(R.id.link_playstore)
    TextView mLinkPlayStore;

    @BindView(R.id.link_linkdn)
    TextView mLinkdn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ButterKnife.bind(this);

        ActionBar ab = getSupportActionBar();
        if(ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        View mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            mainContent.setAlpha(0);
            mainContent.animate().alpha(1).setDuration(com.sformica.torchlight.shaker.ui.BaseActivity.MAIN_CONTENT_FADEIN_DURATION);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Picasso.get().
          load(R.drawable.ic_ant).into(mIcon);

        //mLinkPlayStore.setMovementMethod(LinkMovementMethod.getInstance());
        //mLinkdn.setMovementMethod(LinkMovementMethod.getInstance());
    }


}
