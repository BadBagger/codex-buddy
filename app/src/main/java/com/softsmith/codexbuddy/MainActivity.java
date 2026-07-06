package com.softsmith.codexbuddy;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.Collections;

public class MainActivity extends Activity {
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private View buildContent() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(28));
        root.setBackgroundColor(Color.rgb(247, 245, 239));
        scrollView.addView(root);

        TextView title = text("Codex Buddy", 30, Color.rgb(19, 37, 31));
        title.setGravity(Gravity.START);
        root.addView(title);

        TextView subtitle = text("A floating status buddy for your existing Codex work. Keep it running on this phone, then let a Codex hook on your PC post done or needs-attention updates here.", 16, Color.rgb(64, 72, 68));
        subtitle.setPadding(0, dp(10), 0, dp(20));
        root.addView(subtitle);

        status = text("", 15, Color.rgb(19, 37, 31));
        status.setPadding(dp(14), dp(12), dp(14), dp(12));
        status.setBackgroundColor(Color.rgb(224, 238, 232));
        root.addView(status, matchWrap());

        TextView addresses = text(listenerText(), 14, Color.rgb(35, 42, 39));
        addresses.setPadding(0, dp(18), 0, dp(8));
        root.addView(addresses);

        Button grant = button("Grant overlay permission");
        grant.setOnClickListener(v -> startActivity(new Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + getPackageName())
        )));
        root.addView(grant, matchWrapWithTop(10));

        Button start = button("Start floating buddy");
        start.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                refreshStatus();
                return;
            }
            maybeAskNotificationPermission();
            Intent intent = new Intent(this, OverlayChatService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            refreshStatus();
        });
        root.addView(start, matchWrapWithTop(10));

        Button stop = button("Stop floating buddy");
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, OverlayChatService.class));
            refreshStatus();
        });
        root.addView(stop, matchWrapWithTop(10));

        TextView notes = text("PC hook target: POST http://PHONE_IP:8787/notify with JSON title, message, and status. The phone and PC must be on the same local network.", 14, Color.rgb(82, 87, 84));
        notes.setPadding(0, dp(22), 0, 0);
        root.addView(notes);

        return scrollView;
    }

    private void refreshStatus() {
        boolean overlay = Settings.canDrawOverlays(this);
        status.setText((overlay ? "Overlay permission granted" : "Overlay permission needed") + " - listener port " + StatusBridgeServer.PORT);
    }

    private void maybeAskNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 42);
        }
    }

    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.08f);
        return view;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams matchWrapWithTop(int topDp) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(topDp);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String listenerText() {
        StringBuilder builder = new StringBuilder("Listener URLs:\n");
        boolean found = false;
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (java.net.InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                        builder.append("http://").append(address.getHostAddress()).append(":").append(StatusBridgeServer.PORT).append("/notify\n");
                        found = true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (!found) {
            builder.append("Connect to Wi-Fi, then reopen this screen.");
        }
        return builder.toString().trim();
    }
}
