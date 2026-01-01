package com.flash.flashlight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.HapticFeedbackConstants;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {

    private MaterialCardView btnPowerContainer;
    private ImageView ivPower;
    private TextView tvBattery;
    private SeekBar sbStrobe;
    private MaterialButton btnSOS, btnScreenLight;

    private boolean isFlashOn = false;
    private boolean isSOSOn = false;
    private CameraManager cameraManager;
    private String cameraId;

    private Handler strobeHandler = new Handler();
    private int strobeInterval = 0; // 0 means no strobe

    private MediaPlayer mediaPlayer;

    private static final int CAMERA_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Initialization
        btnPowerContainer = findViewById(R.id.btnPowerContainer);
        ivPower = findViewById(R.id.ivPower);
        tvBattery = findViewById(R.id.tvBattery);
        sbStrobe = findViewById(R.id.sbStrobe);
        btnSOS = findViewById(R.id.btnSOS);
        btnScreenLight = findViewById(R.id.btnScreenLight);

        // Initialize Sound
        mediaPlayer = MediaPlayer.create(this, R.raw.click_sound);

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (cameraManager.getCameraIdList().length > 0) {
                cameraId = cameraManager.getCameraIdList()[0];
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        // Battery Status
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        // Power Button Click
        btnPowerContainer.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            playClickSound();
            if (hasCameraPermission()) {
                toggleFlash();
            } else {
                requestCameraPermission();
            }
        });

        // Strobe Logic
        sbStrobe.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress > 0) {
                    strobeInterval = 1100 - (progress * 100); 
                    if (isFlashOn && !isSOSOn) {
                        startStrobe();
                    }
                } else {
                    strobeInterval = 0;
                    stopStrobe();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // SOS Button
        btnSOS.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            playClickSound();
            if (hasCameraPermission()) {
                toggleSOS();
            } else {
                requestCameraPermission();
            }
        });

        // Screen Light Button
        btnScreenLight.setOnClickListener(v -> {
            playClickSound();
            startActivity(new Intent(MainActivity.this, ScreenLightActivity.class));
        });
    }

    private void playClickSound() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    private void toggleFlash() {
        if (cameraId == null) return;
        try {
            isFlashOn = !isFlashOn;
            isSOSOn = false; 
            updateUI();
            
            if (strobeInterval > 0 && isFlashOn) {
                startStrobe();
            } else {
                stopStrobe();
                cameraManager.setTorchMode(cameraId, isFlashOn);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void toggleSOS() {
        if (cameraId == null) return;
        isSOSOn = !isSOSOn;
        if (isSOSOn) {
            isFlashOn = true;
            stopStrobe();
            startSOS();
        } else {
            isFlashOn = false;
            stopSOS();
        }
        updateUI();
    }

    private void startStrobe() {
        strobeHandler.removeCallbacksAndMessages(null);
        strobeHandler.post(new Runnable() {
            boolean state = false;
            @Override
            public void run() {
                try {
                    state = !state;
                    cameraManager.setTorchMode(cameraId, state);
                    strobeHandler.postDelayed(this, strobeInterval);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void stopStrobe() {
        strobeHandler.removeCallbacksAndMessages(null);
    }

    private void startSOS() {
        strobeHandler.removeCallbacksAndMessages(null);
        final long[] pattern = {200, 200, 200, 200, 200, 500, 500, 200, 500, 200, 500, 500, 200, 200, 200, 200, 200, 1000};
        strobeHandler.post(new Runnable() {
            int index = 0;
            boolean state = true;
            @Override
            public void run() {
                try {
                    cameraManager.setTorchMode(cameraId, state);
                    long delay = pattern[index];
                    index = (index + 1) % pattern.length;
                    state = !state;
                    strobeHandler.postDelayed(this, delay);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void stopSOS() {
        strobeHandler.removeCallbacksAndMessages(null);
        try {
            if (cameraId != null) cameraManager.setTorchMode(cameraId, false);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updateUI() {
        if (isFlashOn) {
            ivPower.setColorFilter(ContextCompat.getColor(this, R.color.accent_neon));
            btnPowerContainer.setStrokeColor(ContextCompat.getColor(this, R.color.accent_neon));
        } else {
            ivPower.setColorFilter(ContextCompat.getColor(this, R.color.text_grey));
            btnPowerContainer.setStrokeColor(ContextCompat.getColor(this, R.color.card_bg));
        }

        if (isSOSOn) {
            btnSOS.setBackgroundColor(ContextCompat.getColor(this, R.color.accent_sos));
            btnSOS.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            btnSOS.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            btnSOS.setTextColor(ContextCompat.getColor(this, R.color.accent_sos));
        }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            toggleFlash();
        } else {
            Toast.makeText(this, "Permission required", Toast.LENGTH_SHORT).show();
        }
    }

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int batteryPct = (int) ((level / (float) scale) * 100);
            tvBattery.setText(batteryPct + "%");
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        unregisterReceiver(batteryReceiver);
        stopStrobe();
        stopSOS();
    }
}