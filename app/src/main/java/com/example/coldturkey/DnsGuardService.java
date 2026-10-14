package com.example.coldturkey;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class DnsGuardService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;

        SharedPreferences prefs = getSharedPreferences("BlockerPrefs", MODE_PRIVATE);
        long unlockTime = prefs.getLong("unlockTime", 0);
        if (System.currentTimeMillis() >= unlockTime) return; // Do nothing when unlocked

        String pkg = event.getPackageName().toString();
        if (pkg.equals(getPackageName())) return; // Never block our own app screen

        String cls = event.getClassName() != null ? event.getClassName().toString() : "";

        // 1. Block Brave Settings menu right away if opened
        if (pkg.equals("com.brave.browser") && cls.contains("Settings")) {
            blockAndRedirect();
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        StringBuilder screenText = new StringBuilder();
        collectText(root, screenText);
        String text = screenText.toString().toLowerCase();

        // 2. Inside Brave: block Secure DNS settings and internal flags/settings URLs
        if (pkg.equals("com.brave.browser")) {
            if (text.contains("use secure dns") ||
                text.contains("brave shields & privacy") ||
                text.contains("select another provider") ||
                text.contains("brave://settings") ||
                text.contains("chrome://settings") ||
                text.contains("brave://flags") ||
                text.contains("chrome://flags")) {
                blockAndRedirect();
            }
        }
        // 3. Inside Android Settings or Package Installer: block DNS changes & uninstalling DNS Guard
        else if (pkg.contains("settings") || pkg.contains("packageinstaller")) {
            if (text.contains("private dns") ||
                text.contains("more connection settings") ||
                text.contains("reset wi-fi") ||
                text.contains("reset network") ||
                text.contains("dns guard")) {
                blockAndRedirect();
            }
        }
    }

    private void collectText(AccessibilityNodeInfo node, StringBuilder sb) {
        if (node == null || sb.length() > 4000) return;
        if (node.getText() != null) {
            sb.append(node.getText()).append(" ");
        }
        if (node.getContentDescription() != null) {
            sb.append(node.getContentDescription()).append(" ");
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectText(node.getChild(i), sb);
        }
    }

    private void blockAndRedirect() {
        performGlobalAction(GLOBAL_ACTION_BACK);
        Intent lockIntent = new Intent(this, MainActivity.class);
        lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(lockIntent);
    }

    @Override
    public void onInterrupt() {}
}