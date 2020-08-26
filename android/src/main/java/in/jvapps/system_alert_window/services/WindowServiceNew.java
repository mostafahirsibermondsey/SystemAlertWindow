package in.jvapps.system_alert_window.services;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Space;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import in.jvapps.system_alert_window.R;
import in.jvapps.system_alert_window.SystemAlertWindowPlugin;
import in.jvapps.system_alert_window.models.Margin;
import in.jvapps.system_alert_window.utils.Commons;
import in.jvapps.system_alert_window.utils.NumberUtils;
import in.jvapps.system_alert_window.utils.UiBuilder;


import static in.jvapps.system_alert_window.utils.Constants.INTENT_EXTRA_PARAMS_MAP;
import static in.jvapps.system_alert_window.utils.Constants.KEY_BODY;
import static in.jvapps.system_alert_window.utils.Constants.KEY_FOOTER;
import static in.jvapps.system_alert_window.utils.Constants.KEY_GRAVITY;
import static in.jvapps.system_alert_window.utils.Constants.KEY_HEADER;
import static in.jvapps.system_alert_window.utils.Constants.KEY_HEIGHT;
import static in.jvapps.system_alert_window.utils.Constants.KEY_MARGIN;
import static in.jvapps.system_alert_window.utils.Constants.KEY_WIDTH;

import com.google.android.flexbox.AlignContent;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexLine;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayout;

public class WindowServiceNew extends Service{

    private static final String TAG = WindowServiceNew.class.getSimpleName();
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static int NOTIFICATION_ID = 1;
    public static final String INTENT_EXTRA_IS_UPDATE_WINDOW = "IsUpdateWindow";
    public static final String INTENT_EXTRA_IS_CLOSE_WINDOW = "IsCloseWindow";

    private WindowManager wm;

    private String windowGravity;
    private int windowWidth;
    private int windowHeight;
    private Margin windowMargin;

    private LinearLayout windowView;

    private float offsetX;
    private float offsetY;
    private int originalXPos;
    private int originalYPos;
    private boolean moving;

    private Context mContext;
    static final String CALLBACK_TYPE_ONCLICK = "onClick";

    @Override
    public void onCreate() {
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, SystemAlertWindowPlugin.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Overlay window service is running")
                .setSmallIcon(R.drawable.ic_desktop_windows_black_24dp)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        if (null != intent && intent.getExtras() != null) {
            @SuppressWarnings("unchecked")
            HashMap<String, Object> paramsMap = (HashMap<String, Object>) intent.getSerializableExtra(INTENT_EXTRA_PARAMS_MAP);
            assert paramsMap != null;
            mContext = this;
            boolean isCloseWindow = intent.getBooleanExtra(INTENT_EXTRA_IS_CLOSE_WINDOW, false);
            if (!isCloseWindow) {
                boolean isUpdateWindow = intent.getBooleanExtra(INTENT_EXTRA_IS_UPDATE_WINDOW, false);
                if (isUpdateWindow && windowView != null) {
                    updateWindow(paramsMap);
                } else {
                    createWindow(paramsMap);
                }
            } else {
                closeWindow(true);
            }
        }
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            assert manager != null;
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void setWindowManager() {
        if (wm == null) {
            wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        }
    }

    private void setWindowLayoutFromMap(HashMap<String, Object> paramsMap) {
        windowMargin = UiBuilder.getMargin(mContext, paramsMap.get(KEY_MARGIN));
        windowGravity = (String) paramsMap.get(KEY_GRAVITY);
        windowWidth = NumberUtils.getInt(paramsMap.get(KEY_WIDTH));
        windowHeight = NumberUtils.getInt(paramsMap.get(KEY_HEIGHT));
    }

    private WindowManager.LayoutParams getLayoutParams() {
        final WindowManager.LayoutParams params;
        params = new WindowManager.LayoutParams();
        params.width = (windowWidth == 0) ? android.view.WindowManager.LayoutParams.MATCH_PARENT : Commons.getPixelsFromDp(mContext, windowWidth);
        params.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
        params.format = PixelFormat.TRANSPARENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            params.flags = android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        } else {
            params.type = android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ALERT | android.view.WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
            params.flags = android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }
        params.gravity = Commons.getGravity(windowGravity, Gravity.BOTTOM);

        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        int newY = (int) (offsetY + metrics.heightPixels);
        params.y = newY;
        return params;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setWindowView(WindowManager.LayoutParams params, boolean isCreate) {
        if (isCreate) {
            windowView = new LinearLayout(mContext);
        }

        windowView.setBackgroundColor(Color.GRAY);
        windowView.setLayoutParams(params);

        WindowManager.LayoutParams param = new WindowManager.LayoutParams();
        param.width = android.view.WindowManager.LayoutParams.MATCH_PARENT;

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        windowView = (LinearLayout) inflater.inflate(R.layout.flexbox_layout, null);

        ImageButton backBtn = windowView.findViewById(R.id.back_button);
        backBtn.setOnClickListener(v -> {
            if (!SystemAlertWindowPlugin.sIsIsolateRunning.get()) {
                SystemAlertWindowPlugin.startCallBackHandler(mContext);
            }
            SystemAlertWindowPlugin.invokeCallBack(mContext, CALLBACK_TYPE_ONCLICK, "back_button");
        });
        ImageButton homeBtn = windowView.findViewById(R.id.home_button);
        homeBtn.setOnClickListener(v -> {
            if (!SystemAlertWindowPlugin.sIsIsolateRunning.get()) {
                SystemAlertWindowPlugin.startCallBackHandler(mContext);
            }
            SystemAlertWindowPlugin.invokeCallBack(mContext, CALLBACK_TYPE_ONCLICK, "home_button");
        });
        ImageButton settingsBtn = windowView.findViewById(R.id.settings_button);
        settingsBtn.setOnClickListener(v -> {
            if (!SystemAlertWindowPlugin.sIsIsolateRunning.get()) {
                SystemAlertWindowPlugin.startCallBackHandler(mContext);
            }
            SystemAlertWindowPlugin.invokeCallBack(mContext, CALLBACK_TYPE_ONCLICK, "settings_button");
        });
    }

    private void createWindow(HashMap<String, Object> paramsMap) {
        closeWindow(false);
        setWindowManager();
        setWindowLayoutFromMap(paramsMap);
        WindowManager.LayoutParams params = getLayoutParams();
        setWindowView(params, true);
        wm.addView(windowView, params);
    }

    private void updateWindow(HashMap<String, Object> paramsMap) {
        setWindowLayoutFromMap(paramsMap);
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) windowView.getLayoutParams();
        setWindowView(params, false);
        wm.updateViewLayout(windowView, params);
    }

    private void closeWindow(boolean isEverythingDone) {
        Log.i(TAG, "Closing the overlay window");
        try {
            if (wm != null) {
                if (windowView != null) {
                    wm.removeView(windowView);
                    windowView = null;
                }
            }
            wm = null;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "view not found");
        }
        if(isEverythingDone){
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        //Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Destroying the overlay window service");
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        notificationManager.cancel(NOTIFICATION_ID);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
