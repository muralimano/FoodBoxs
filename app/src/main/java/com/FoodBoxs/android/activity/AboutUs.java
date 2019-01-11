package com.FoodBoxs.android.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.TextView;

import com.FoodBoxs.android.R;

public class AboutUs extends AppCompatActivity {
    WebView wv_aboutUs;
    TextView tv_tittle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);
        wv_aboutUs = findViewById(R.id.wv_aboutUs);
        tv_tittle = findViewById(R.id.tv_tittle);
        tv_tittle.setTypeface(Home.tf_main_bold);

        wv_aboutUs.loadData(getString(R.string.about_us_dummy_data),"text/html","UTF-8");
    }
}
