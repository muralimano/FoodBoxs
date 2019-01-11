package com.FoodBoxs.android.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.TextView;

import com.FoodBoxs.android.R;

public class OurPolicy extends AppCompatActivity {
    WebView wv_policy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_our_policy);
        TextView tv_tittle = findViewById(R.id.tv_tittle);
        tv_tittle.setTypeface(Home.tf_main_bold);
        tv_tittle.setText(R.string.policy);

        wv_policy = findViewById(R.id.wv_policy);
        wv_policy.loadData(getString(R.string.about_us_dummy_data),"text/html","UTF-8");
    }
}
