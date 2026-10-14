package com.example.coldturkey;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private int emergencyTaps = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        emergencyTaps = 0;
        renderUI();
    }

    private void renderUI() {
        SharedPreferences prefs = getSharedPreferences("BlockerPrefs", MODE_PRIVATE);
        long unlockTime = prefs.getLong("unlockTime", 0);
        long remainingMs = unlockTime - System.currentTimeMillis();

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 80, 60, 80);
        layout.setGravity(Gravity.CENTER);

        if (remainingMs > 0) {
            long minsLeft = (remainingMs / 60000) + 1;
            TextView lockMsg = new TextView(this);
            lockMsg.setText("DNS SETTINGS LOCKED\n\nYou cannot change Android or Brave DNS right now.\nTime left: " + minsLeft + " min\n");
            lockMsg.setTextSize(20);
            lockMsg.setGravity(Gravity.CENTER);
            layout.addView(lockMsg);

            Button homeBtn = new Button(this);
            homeBtn.setText("Return to Home Screen");
            homeBtn.setOnClickListener(v -> goHome());
            layout.addView(homeBtn);

            Button panicBtn = new Button(this);
            panicBtn.setText("Emergency Unlock (50 taps left)");
            panicBtn.setOnClickListener(v -> {
                emergencyTaps++;
                int left = 50 - emergencyTaps;
                if (left <= 0) {
                    prefs.edit().putLong("unlockTime", 0).apply();
                    Toast.makeText(this, "DNS Lock Disengaged!", Toast.LENGTH_SHORT).show();
                    renderUI();
                } else {
                    panicBtn.setText("Emergency Unlock (" + left + " taps left)");
                }
            });
            layout.addView(panicBtn);

        } else {
            TextView info = new TextView(this);
            info.setText("Step 1: Set your Private DNS in Settings first (e.g. family.cloudflare-dns.com).\n\nStep 2: Enable 'DNS Guard' inside Accessibility -> Installed Apps.\n");
            layout.addView(info);

            Button accBtn = new Button(this);
            accBtn.setText("Open Accessibility Settings");
            accBtn.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
            layout.addView(accBtn);

            TextView timeLabel = new TextView(this);
            timeLabel.setText("\nLock Duration in Minutes (Max 1440 / 24h):");
            layout.addView(timeLabel);

            EditText timeInput = new EditText(this);
            timeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
            timeInput.setText("60");
            layout.addView(timeInput);

            Button startBtn = new Button(this);
            startBtn.setText("LOCK DNS SETTINGS");
            startBtn.setOnClickListener(v -> {
                String minsStr = timeInput.getText().toString().trim();
                if (minsStr.isEmpty()) return;
                long mins = Long.parseLong(minsStr);

                if (mins <= 0 || mins > 1440) {
                    Toast.makeText(this, "Safety limit: Enter between 1 and 1440 minutes (24h).", Toast.LENGTH_LONG).show();
                    return;
                }

                long targetTime = System.currentTimeMillis() + (mins * 60 * 1000);
                prefs.edit().putLong("unlockTime", targetTime).apply();

                Toast.makeText(this, "DNS Settings locked for " + mins + " minutes!", Toast.LENGTH_SHORT).show();
                renderUI();
            });
            layout.addView(startBtn);
        }

        setContentView(layout);
    }

    private void goHome() {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(home);
    }
}