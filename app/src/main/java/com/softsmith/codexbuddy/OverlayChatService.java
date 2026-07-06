package com.softsmith.codexbuddy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OverlayChatService extends Service {
    private static final String CHANNEL_ID = "codex_buddy_overlay";
    private static final int NOTIFICATION_ID = 9001;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final OpenAiChatClient chatClient = new OpenAiChatClient();

    private WindowManager windowManager;
    private FrameLayout bubble;
    private LinearLayout panel;
    private TextView transcript;
    private ScrollView transcriptScroll;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams panelParams;
    private StatusBridgeServer statusServer;
    private int alertId = 9100;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        startForeground(NOTIFICATION_ID, buildNotification());
        statusServer = new StatusBridgeServer((title, message, status) -> mainHandler.post(() -> onStatusEvent(title, message, status)));
        statusServer.start();
        if (Settings.canDrawOverlays(this)) {
            showBubble();
        } else {
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        removePanel();
        removeBubble();
        if (statusServer != null) {
            statusServer.stop();
        }
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void showBubble() {
        if (bubble != null) {
            return;
        }
        bubble = new FrameLayout(this);
        bubble.setBackground(makeRound(Color.rgb(31, 122, 104), dp(26)));
        TextView glyph = new TextView(this);
        glyph.setText("C");
        glyph.setTextColor(Color.WHITE);
        glyph.setTextSize(22);
        glyph.setGravity(Gravity.CENTER);
        glyph.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        bubble.addView(glyph, new FrameLayout.LayoutParams(dp(54), dp(54), Gravity.CENTER));

        bubbleParams = baseParams(dp(54), dp(54));
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = dp(18);
        bubbleParams.y = dp(130);

        bubble.setOnTouchListener(new DragTouchListener(bubbleParams, true));
        windowManager.addView(bubble, bubbleParams);
    }

    private void togglePanel() {
        if (panel == null) {
            showPanel();
        } else {
            removePanel();
        }
    }

    private void showPanel() {
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(12), dp(14), dp(12));
        panel.setBackground(makeRound(Color.rgb(252, 251, 247), dp(10)));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("Codex Buddy");
        title.setTextColor(Color.rgb(19, 37, 31));
        title.setTextSize(17);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button close = smallButton("X");
        close.setOnClickListener(v -> removePanel());
        top.addView(close, new LinearLayout.LayoutParams(dp(42), dp(38)));
        panel.addView(top);

        transcript = new TextView(this);
        transcript.setText("Waiting for Codex status events on port " + StatusBridgeServer.PORT + ".\n");
        transcript.setTextColor(Color.rgb(35, 42, 39));
        transcript.setTextSize(14);
        transcript.setLineSpacing(0, 1.08f);
        transcriptScroll = new ScrollView(this);
        transcriptScroll.addView(transcript);
        panel.addView(transcriptScroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        panelParams = baseParams(dp(342), dp(470));
        panelParams.gravity = Gravity.TOP | Gravity.START;
        panelParams.x = Math.max(dp(8), bubbleParams == null ? dp(20) : bubbleParams.x);
        panelParams.y = bubbleParams == null ? dp(190) : bubbleParams.y + dp(64);
        panel.setOnTouchListener(new DragTouchListener(panelParams, false));
        windowManager.addView(panel, panelParams);

    }

    private void sendMessage(String message) {
        append("Buddy: thinking...\n");
        executor.execute(() -> {
            String answer;
            try {
                answer = chatClient.send(AppSettings.apiKey(this), AppSettings.model(this), message);
            } catch (Exception e) {
                answer = "Request failed: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            }
            String finalAnswer = answer;
            mainHandler.post(() -> append("Buddy: " + finalAnswer + "\n\n"));
        });
    }

    private void append(String text) {
        if (transcript == null) {
            return;
        }
        transcript.append(text);
        if (transcriptScroll != null) {
            transcriptScroll.post(() -> transcriptScroll.fullScroll(View.FOCUS_DOWN));
        }
    }

    private void onStatusEvent(String title, String message, String status) {
        append(title + "\n" + message + "\n\n");
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? new Notification.Builder(this, CHANNEL_ID)
            : new Notification.Builder(this);
        Notification notification = builder
            .setSmallIcon(R.drawable.ic_stat_codex_buddy)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(new Notification.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(alertId++, notification);
        }
    }

    private void removeBubble() {
        if (bubble != null) {
            windowManager.removeView(bubble);
            bubble = null;
        }
    }

    private void removePanel() {
        if (panel != null) {
            windowManager.removeView(panel);
            panel = null;
            transcript = null;
            transcriptScroll = null;
        }
    }

    private WindowManager.LayoutParams baseParams(int width, int height) {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            width,
            height,
            type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        );
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        return params;
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Codex Buddy overlay",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? new Notification.Builder(this, CHANNEL_ID)
            : new Notification.Builder(this);
        return builder
            .setSmallIcon(R.drawable.ic_stat_codex_buddy)
            .setContentTitle("Codex Buddy is listening")
            .setContentText("Port " + StatusBridgeServer.PORT + " is ready for Codex status updates.")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    private Button smallButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(13);
        return button;
    }

    private android.graphics.drawable.GradientDrawable makeRound(int color, int radius) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (color == Color.rgb(252, 251, 247)) {
            drawable.setStroke(dp(1), Color.rgb(207, 217, 211));
        }
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class DragTouchListener implements View.OnTouchListener {
        private final WindowManager.LayoutParams params;
        private final boolean bubbleTouch;
        private int initialX;
        private int initialY;
        private float initialTouchX;
        private float initialTouchY;
        private long downTime;
        private boolean moved;

        DragTouchListener(WindowManager.LayoutParams params, boolean bubbleTouch) {
            this.params = params;
            this.bubbleTouch = bubbleTouch;
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    downTime = System.currentTimeMillis();
                    moved = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int dx = Math.round(event.getRawX() - initialTouchX);
                    int dy = Math.round(event.getRawY() - initialTouchY);
                    if (Math.abs(dx) > dp(4) || Math.abs(dy) > dp(4)) {
                        moved = true;
                    }
                    params.x = Math.max(0, initialX + dx);
                    params.y = Math.max(0, initialY + dy);
                    windowManager.updateViewLayout(view, params);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (bubbleTouch && !moved && System.currentTimeMillis() - downTime < 350) {
                        togglePanel();
                    }
                    return true;
                default:
                    return false;
            }
        }
    }
}
