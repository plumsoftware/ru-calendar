package com.plumsoftware.rucalendar.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.plumsoftware.rucalendar.R;
import com.plumsoftware.rucalendar.services.AppForegroundService;

public class SettingsActivity extends AppCompatActivity {
    private static final String SETTINGS_PREFS = "app_settings";
    private static final String KEY_DARK_THEME = "dark_theme";
    private static final String KEY_BACKGROUND_MODE = "background_mode";
    private static final String KEY_NO_BATTERY_SAVE = "no_battery_save";
    private static final String KEY_NO_INTERRUPT_BG = "no_interrupt_bg";

    private static final String KEY_STOP_ON_IDLE = "stop_on_idle";
    private static final int REQUEST_STORAGE_PERMISSION = 2001;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 2002;

    private SharedPreferences settingsPrefs;
    private MaterialButton memoryPermissionButton;
    private MaterialButton notificationsPermissionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        settingsPrefs = getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE);
        boolean isDarkTheme = settingsPrefs.getBoolean(KEY_DARK_THEME, false);
        AppCompatDelegate.setDefaultNightMode(isDarkTheme
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setupEdgeToEdge();
        setContentView(R.layout.activity_settings);

        View settingsBack = findViewById(R.id.settings_back);
        settingsBack.setOnClickListener(v -> finish());

        SwitchCompat switchBackgroundMode = findViewById(R.id.switch_background_mode);
        SwitchCompat switchBatterySaver = findViewById(R.id.switch_battery_saver);
        SwitchCompat switchNoInterrupt = findViewById(R.id.switch_no_interrupt_bg);
        SwitchCompat switchStopOnIdle = findViewById(R.id.switch_stop_on_idle);
        SwitchCompat switchDarkTheme = findViewById(R.id.switch_dark_theme);
        memoryPermissionButton = findViewById(R.id.button_memory_permission);
        notificationsPermissionButton = findViewById(R.id.button_notifications_permission);

        boolean backgroundMode = settingsPrefs.getBoolean(KEY_BACKGROUND_MODE, false);
        boolean noBatterySave = settingsPrefs.getBoolean(KEY_NO_BATTERY_SAVE, false);
        boolean noInterruptBg = settingsPrefs.getBoolean(KEY_NO_INTERRUPT_BG, false);
        boolean stopOnIdle = settingsPrefs.getBoolean(KEY_STOP_ON_IDLE, false);

        switchBackgroundMode.setChecked(backgroundMode);
        switchBatterySaver.setChecked(noBatterySave);
        switchNoInterrupt.setChecked(noInterruptBg);
        switchStopOnIdle.setChecked(stopOnIdle);
        switchDarkTheme.setChecked(isDarkTheme);

        switchBackgroundMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsPrefs.edit().putBoolean(KEY_BACKGROUND_MODE, isChecked).apply();
            if (isChecked) {
                requestIgnoreBatteryOptimizations();
            }
        });

        switchBatterySaver.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsPrefs.edit().putBoolean(KEY_NO_BATTERY_SAVE, isChecked).apply();
            if (isChecked) {
                startActivity(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS));
            }
        });

        switchNoInterrupt.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsPrefs.edit().putBoolean(KEY_NO_INTERRUPT_BG, isChecked).apply();
            Intent serviceIntent = new Intent(this, AppForegroundService.class);
            if (isChecked) {
                ContextCompat.startForegroundService(this, serviceIntent);
            } else {
                stopService(serviceIntent);
            }
        });

        switchStopOnIdle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsPrefs.edit().putBoolean(KEY_STOP_ON_IDLE, isChecked).apply();
            if (!isChecked) {
                openBackgroundRestrictionSettings();
            }
        });

        switchDarkTheme.setOnCheckedChangeListener((buttonView, isCheckedValue) -> {
            settingsPrefs.edit().putBoolean(KEY_DARK_THEME, isCheckedValue).apply();
            AppCompatDelegate.setDefaultNightMode(isCheckedValue
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
        });

        memoryPermissionButton.setOnClickListener(v -> requestStoragePermissionIfNeeded());
        notificationsPermissionButton.setOnClickListener(v -> requestNotificationPermissionIfNeeded());

        updatePermissionButtonsState();
    }

    private void requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private void requestStoragePermissionIfNeeded() {
        if (hasStoragePermission()) {
            updatePermissionButtonsState();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            updatePermissionButtonsState();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION
            );
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    REQUEST_STORAGE_PERMISSION
            );
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            updatePermissionButtonsState();
            return;
        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQUEST_NOTIFICATION_PERMISSION
        );
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        boolean hasRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            return hasRead;
        }
        boolean hasWrite = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        return hasRead && hasWrite;
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void openBackgroundRestrictionSettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent = new Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
        }
        startActivity(intent);
    }

    private void updatePermissionButtonsState() {
        memoryPermissionButton.setText(getString(
                hasStoragePermission() ? R.string.settings_permission_granted : R.string.settings_permission_request
        ));
        notificationsPermissionButton.setText(getString(
                hasNotificationPermission() ? R.string.settings_permission_granted : R.string.settings_permission_request
        ));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionButtonsState();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION || requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            updatePermissionButtonsState();
        }
    }

    private void setupEdgeToEdge() {
        Window window = getWindow();

        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkTheme = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;

        int systemUiVisibilityFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isDarkTheme) {
            systemUiVisibilityFlags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isDarkTheme) {
            systemUiVisibilityFlags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }

        window.getDecorView().setSystemUiVisibility(systemUiVisibilityFlags);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                if (!isDarkTheme) {
                    controller.setSystemBarsAppearance(
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    );
                }
            }
        }
    }
}
