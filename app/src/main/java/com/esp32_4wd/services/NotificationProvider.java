package com.esp32_4wd.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import com.esp32_4wd.BuildConfig;
import com.esp32_4wd.R;
import com.esp32_4wd.utils.Annotator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

public class NotificationProvider {

    public abstract static class OnNotificationClick {

        public void onClick() {
        }
    }

    private static int generateFixedCode(String name) {
        Annotator annotator = new Annotator("notification_main_code.json");
        try {
            int values = 0;
            if (annotator.exists()) {
                JSONObject jsonObject = new JSONObject(annotator.getContent());
                JSONArray jsonArray = jsonObject.getJSONArray("ids");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject json = jsonArray.getJSONObject(i);
                    if (json.has(name)) {
                        return json.getInt(name);
                    } else {
                        values = json.getInt(json.keys().next());
                    }
                }
                if (values == 0) values = jsonObject.getInt("notification_main_code");
                values++;
                jsonArray.put(new JSONObject().put(name, values));
                annotator.setContent(jsonObject.toString());
            } else {
                char[] chars = BuildConfig.APPLICATION_ID.toCharArray();
                if (chars.length > 0) {
                    for (int i = 0; i < chars.length; i++) {
                        values += Character.codePointAt(chars, i);
                    }
                }
                if (values < 1000 && values > 0) values += 1234;
                else if (values > 65536) values = 65536 - new Random().nextInt(64302);
                else if (values == 0) values = new Random().nextInt(64302) + 1234;
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("notification_main_code", values);
                values++;
                JSONArray jsonArray = new JSONArray();
                jsonArray.put(new JSONObject().put(name, values));
                jsonObject.put("ids", jsonArray);
                annotator.setContent(jsonObject.toString());
                return values;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private final String app = BuildConfig.APPLICATION_ID;
    private final int NOTIFICATION_CODE;
    private final String NOTIFICATION_CLICK;
    private final String ID_ONE = "one_" + app;
    private final String ID_TWO = "two_" + app;

    private final Context context;
    private final OnNotificationClick callback;
    private final NotificationManager notificationManager;
    private Notification notification;
    private boolean isShowing;

    private Bitmap defaultIcon, tintedIcon;

    public NotificationProvider(Context context, String tag, int icon, OnNotificationClick callback) {
        this.context = context;
        this.callback = callback;
        NOTIFICATION_CODE = generateFixedCode(tag);
        NOTIFICATION_CLICK = NOTIFICATION_CODE + "_notification_click." + app;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        defaultIcon = tintBitmap(icon, Color.valueOf(context.getColor(R.color.main)));
        createChannels();
    }

    private void createChannels() {
        NotificationChannel builderChannel = new NotificationChannel(ID_ONE, context.getString(R.string.notification_small), NotificationManager.IMPORTANCE_DEFAULT);
        builderChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        builderChannel.enableLights(false);
        builderChannel.enableVibration(false);
        builderChannel.setSound(null, null);
        notificationManager.createNotificationChannel(builderChannel);

        AudioAttributes.Builder audioAttributes = new AudioAttributes.Builder();
        audioAttributes.setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_ALL);
        audioAttributes.setLegacyStreamType(AudioManager.STREAM_NOTIFICATION);
        audioAttributes.setUsage(AudioAttributes.USAGE_NOTIFICATION);

        builderChannel = new NotificationChannel(ID_TWO, context.getString(R.string.notification_alert), NotificationManager.IMPORTANCE_HIGH);
        builderChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        builderChannel.enableLights(true);
        builderChannel.enableVibration(true);
        //builderChannel.setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + BuildConfig.APPLICATION_ID + "/" + R.raw.notification_sound), audioAttributes.build());
        builderChannel.setLightColor(Color.argb(255, 255, 255, 255));
        builderChannel.setVibrationPattern(new long[]{250, 250, 250, 250});

        //notificationManager.deleteNotificationChannel(ID_TWO);
        //notificationManager.deleteNotificationChannel(ID_ONE);
        notificationManager.createNotificationChannel(builderChannel);
    }

    public int getNotificationCode() {
        return NOTIFICATION_CODE;
    }

    public Notification getNotification() {
        return notification;
    }

    public void setTintedIcon(int icon, int colorId) {
        tintedIcon = tintBitmap(icon, Color.valueOf(context.getColor(colorId)));
    }

    public void setIcon(int icon) {
        defaultIcon = BitmapFactory.decodeResource(context.getResources(), icon);
    }

    public void setIcon(int icon, int colorId) {
        defaultIcon = tintBitmap(icon, Color.valueOf(context.getColor(colorId)));
    }

    public void showAlert(String title, String detail) {
        isShowing = true;
        Notification.Builder builder;
        builder = new Notification.Builder(context, ID_TWO);
        builder.setAutoCancel(false);
        builder.setOngoing(true);
        builder.setShowWhen(false);
        builder.setColorized(true);
        builder.setTicker(title);
        builder.setContentText(title);
        builder.setSmallIcon(Icon.createWithBitmap(tintedIcon == null ? defaultIcon : tintedIcon));
        builder.setCategory(Notification.CATEGORY_SERVICE);
        builder.setContentIntent(PendingIntent.getBroadcast(context, NOTIFICATION_CODE, new Intent(NOTIFICATION_CLICK), 0));

        RemoteViews root = new RemoteViews(context.getPackageName(),  R.layout.notification_small);
        root.setImageViewBitmap(R.id.appIcon, tintedIcon == null ? defaultIcon : tintedIcon);
        if (detail != null) root.setTextViewText(R.id.textDescription, detail);
        builder.setCustomContentView(root);
        notification = builder.build();

        IntentFilter filter = new IntentFilter();
        filter.addAction(NOTIFICATION_CLICK);
        try {
            context.registerReceiver(receiver, filter);
        } catch (Exception ignore) {
        }
        notificationManager.notify(NOTIFICATION_CODE, notification);
    }

    public void showSmall(String title) {
        isShowing = true;
        Notification.Builder builder;
        builder = new Notification.Builder(context, ID_ONE);
        builder.setAutoCancel(false);
        builder.setOngoing(true);
        builder.setShowWhen(false);
        builder.setColorized(true);
        builder.setTicker(title);
        builder.setContentText(title);
        builder.setSmallIcon(Icon.createWithBitmap(tintedIcon == null ? defaultIcon : tintedIcon));
        builder.setCategory(Notification.CATEGORY_SERVICE);
        builder.setContentIntent(PendingIntent.getBroadcast(context, NOTIFICATION_CODE, new Intent(NOTIFICATION_CLICK), 0));

        RemoteViews root = new RemoteViews(context.getPackageName(), R.layout.notification_small);
        root.setViewVisibility(R.id.textTitle, View.INVISIBLE);
        root.setImageViewBitmap(R.id.appIcon, tintedIcon == null ? defaultIcon : tintedIcon);
        root.setTextViewText(R.id.textDescription, title);
        builder.setCustomContentView(root);
        notification = builder.build();

        notificationManager.notify(NOTIFICATION_CODE, notification);
    }

    public void hide() {
        isShowing = false;
        notificationManager.cancelAll();
        try {
            context.unregisterReceiver(receiver);
        } catch (Exception ignore) {
        }
    }

    public boolean isShowing() {
        return isShowing;
    }

    public Bitmap tintBitmap(int id, Color target) {
        Drawable drawable = context.getDrawable(id);
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        Bitmap bitmap2 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                int color = Color.argb(bitmap2.getColor(x, y).alpha(), target.red(), target.green(), target.blue());
                bitmap2.setPixel(x, y, color);
            }
        }
        bitmap.recycle();
        return bitmap2;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent == null || intent.getAction() == null ? "" : intent.getAction();
            if (action.equals(NOTIFICATION_CLICK)) {
                if (callback != null) callback.onClick();
            }
        }
    };
}
