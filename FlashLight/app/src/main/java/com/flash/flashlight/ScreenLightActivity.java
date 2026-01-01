package com.flash.flashlight;

import android.os.Bundle;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;

public class ScreenLightActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_light);

        // Set brightness to max
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = 1F;
        getWindow().setAttributes(layout);

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
        findViewById(R.id .main_container).setOnClickListener(v -> finish());
    }
}