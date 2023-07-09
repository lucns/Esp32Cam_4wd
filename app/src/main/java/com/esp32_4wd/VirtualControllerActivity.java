package com.esp32_4wd;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;

import com.esp32_4wd.services.MainService;
import com.esp32_4wd.services.Transceiver;
import com.esp32_4wd.utils.FileAndroidTeen;
import com.esp32_4wd.utils.Notify;
import com.esp32_4wd.utils.Prefs;
import com.esp32_4wd.utils.Utils;
import com.esp32_4wd.views.AngleView;
import com.esp32_4wd.views.FrameView;
import com.esp32_4wd.views.JoystickView;
import com.esp32_4wd.views.VideoRecorder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VirtualControllerActivity extends BaseActivity {

    private PopupMenu popupMenu;
    private MainService mainService;
    private TextView textRssi, textFps, textNetworkVelocity, textTime;
    private ImageButton buttonConnection;
    private TextView textTx, textRx;
    private final int runSpeed = 1023;
    private int sideSpeed;
    private FrameView frameView;
    private boolean resized;
    private int imageWidth, imageHeight, imageViewHeight;

    private Sensor accelerometer;
    private SensorManager sensorManager;
    private AngleView angleView;
    private TextView textAngleSide;
    private double angleSide;

    private final int IDLE = 0;
    private final int NEGATIVE = 1;
    private final int POSITIVE = 2;
    private int directionState;
    private int sideState;
    private JoystickView joystickView;

    @Override
    public boolean onCreated() {
        setContentView(R.layout.activity_virtual_controller);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        angleView = findViewById(R.id.angleView);
        angleView.setAlpha(0.5f);
        textAngleSide = findViewById(R.id.textAngleSide);
        joystickView = findViewById(R.id.controllerOne);
        joystickView.setCallback(new JoystickView.Callback() {
            @Override
            public void onTouchEvent(boolean touched) {
                if (touched) {
                    Utils.vibrate();
                } else {
                    Utils.vibrate(40);
                    directionState = IDLE;
                    mainService.put(new Transceiver.Command("motors", "ld rd", false));
                }
            }

            @Override
            public void onChange(int intensity) {
                //Log.d("lucas", "intensity:" + intensity);
                String leftCommand, rightCommand;
                boolean persistent = true;
                if (intensity > 0) {
                    directionState = POSITIVE;
                    if (sideState == NEGATIVE) {
                        leftCommand = "lf" + sideSpeed;
                        rightCommand = "rf" + runSpeed;
                    } else if (sideState == POSITIVE) {
                        leftCommand = "lf" + runSpeed;
                        rightCommand = "rf" + sideSpeed;
                    } else {
                        leftCommand = "lf" + runSpeed;
                        rightCommand = "rf" + runSpeed;
                    }
                } else if (intensity < 0) {
                    directionState = NEGATIVE;
                    if (sideState == NEGATIVE) {
                        leftCommand = "lb" + sideSpeed;
                        rightCommand = "rb" + runSpeed;
                    } else if (sideState == POSITIVE) {
                        leftCommand = "lb" + runSpeed;
                        rightCommand = "rb" + sideSpeed;
                    } else {
                        leftCommand = "lb" + runSpeed;
                        rightCommand = "rb" + runSpeed;
                    }
                } else {
                    directionState = IDLE;
                    if (sideState == NEGATIVE) {
                        leftCommand = "lb" + runSpeed;
                        rightCommand = "rf" + runSpeed;
                    } else if (sideState == POSITIVE) {
                        leftCommand = "lf" + runSpeed;
                        rightCommand = "rb" + runSpeed;
                    } else {
                        leftCommand = "ld";
                        rightCommand = "rd";
                        persistent = false;
                    }
                }
                mainService.put(new Transceiver.Command("motors", leftCommand + " " + rightCommand, persistent));
            }
        });

        VideoRecorder.TimeCallback timeCallback = new VideoRecorder.TimeCallback() {
            @Override
            public void onTimeChanged(long milliseconds) {
                textTime.setText(timeToString(milliseconds));
            }

            private String timeToString(long time) {
                Date dateDuration = new Date(time);
                DateFormat formatter;
                if (time >= 3600 * 1000)
                    formatter = new SimpleDateFormat("HH:mm ss", Locale.getDefault());
                else formatter = new SimpleDateFormat("mm:ss SSS", Locale.getDefault());
                return formatter.format(dateDuration);
            }
        };

        textRssi = findViewById(R.id.textRssiValue);
        textFps = findViewById(R.id.textFpsValue);
        textNetworkVelocity = findViewById(R.id.textNetworkVelocityValue);
        frameView = findViewById(R.id.imageView);
        frameView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                imageViewHeight = frameView.getHeight();
                frameView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        textTx = findViewById(R.id.textTx);
        textRx = findViewById(R.id.textRx);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.buttonMenu:
                        MenuItem item = popupMenu.getMenu().findItem(R.id.menu_record);
                        item.setTitle(frameView.isRecording() ? R.string.stop_record : R.string.start_record);
                        item.setEnabled(mainService.isConnectedUdp() && Prefs.getBoolean("camera_state"));
                        item = popupMenu.getMenu().findItem(R.id.menu_change_camera_state);
                        item.setTitle(Prefs.getBoolean("camera_state") ? R.string.disable_camera : R.string.enable_camera);
                        item.setEnabled(mainService.isConnectedUdp());
                        item = popupMenu.getMenu().findItem(R.id.menu_take_picture);
                        item.setEnabled(frameView.hasFrame());
                        popupMenu.show();
                        break;
                    case R.id.buttonConnection:
                        if (mainService == null) break;
                        if (mainService.isConnectedOnEspWifi()) {
                            if (mainService.isConnectedUdp()) {
                                mainService.put(new Transceiver.Command("control", "z"));
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        mainService.close();
                                    }
                                }, 100);
                            } else {
                                mainService.connect();
                            }
                        } else {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_WIFI_SETTINGS);
                            startActivity(intent);
                        }
                        break;
                    case R.id.buttonWifi:
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_WIFI_SETTINGS);
                        startActivity(intent);
                        break;
                }
            }
        };
        findViewById(R.id.buttonWifi).setOnClickListener(onClickListener);
        textTime = findViewById(R.id.textTime);
        buttonConnection = findViewById(R.id.buttonConnection);
        buttonConnection.setOnClickListener(onClickListener);
        ImageButton buttonMenu = findViewById(R.id.buttonMenu);
        buttonMenu.setOnClickListener(onClickListener);

        popupMenu = new PopupMenu(this, buttonMenu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_lights:
                        showDialogLights();
                        break;
                    case R.id.menu_record:
                        if (frameView.isRecording()) {
                            Notify.showToast(R.string.saved);
                            textTime.setVisibility(View.INVISIBLE);
                            frameView.stopRecord();
                        } else {
                            frameView.prepare(imageWidth, imageHeight);
                            if (frameView.startRecord(timeCallback)) {
                                textTime.setVisibility(View.VISIBLE);
                            } else {
                                textTime.setVisibility(View.INVISIBLE);
                                Notify.showToast(R.string.fail_record);
                            }
                        }
                        break;
                    case R.id.menu_take_picture:
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                            if (!new FileAndroidTeen().hasInternalAccess()) {
                                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                                i.addFlags(
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
                                startActivityForResult(i, 1234);
                                break;
                            }
                        } else {
                            if (!Environment.isExternalStorageManager()) {
                                Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                                i.addCategory(Intent.CATEGORY_DEFAULT);
                                i.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                                try {
                                    startActivity(i);
                                    break;
                                } catch (ActivityNotFoundException e) {
                                    e.printStackTrace();
                                }
                                showDialogError(getString(R.string.error_device_not_supported));
                                break;
                            }
                        }
                        frameView.saveFrame();
                        Notify.showToast(R.string.saved);
                        break;
                    case R.id.menu_change_camera_state:
                        resized = false;
                        if (!mainService.isConnectedUdp()) break;
                        boolean cameraEnabled = !Prefs.getBoolean("camera_state");
                        Prefs.setBoolean("camera_state", cameraEnabled);
                        Notify.showToast(cameraEnabled ? R.string.enabled : R.string.disabled);
                        if (cameraEnabled) {
                            mainService.enableReceiver();
                            frameView.setVisibility(View.VISIBLE);
                        } else {
                            frameView.setVisibility(View.INVISIBLE);
                            mainService.disableReceiver();
                        }
                        mainService.put(new Transceiver.Command("camera_state", cameraEnabled ? "c1" : "c0"));
                        textFps.setText(R.string.zero);
                        textNetworkVelocity.setText(R.string.zero);
                        break;
                    case R.id.menu_controller:
                        startActivity(new Intent(VirtualControllerActivity.this, EasyControllerActivity.class));
                        finish();
                        break;
                }
                return true;
            }
        });
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.menu_virtual, popupMenu.getMenu());

        Intent intent = new Intent(this, MainService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        return true;
    }

    private void showDialogLights() {
        Dialog dialog = generateDialog(R.layout.dialog_lights, true);
        dialog.findViewById(R.id.buttonPositive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        TextView textView = dialog.findViewById(R.id.textPercent);
        SeekBar seekBar = dialog.findViewById(R.id.seekbar);
        int ledFront = Prefs.getInt("led_front");
        if (ledFront < 0) ledFront = 1023;
        int percent = (int) (100 * ((double) ledFront / 1023.0d));
        textView.setText(percent + "%");
        seekBar.setProgress(ledFront);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int percent = (int) (100 * ((double) progress / 1023.0d));
                textView.setText(percent + "%");
                if (mainService == null || !mainService.isConnectedUdp()) return;
                mainService.put(new Transceiver.Command("led_front", "g" + progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Prefs.setInt("led_front", seekBar.getProgress());
            }
        });

        dialog.show();
    }

    @Override
    public void onResumed() {
        sensorManager.registerListener(sensorEvent, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        if (mainService != null) {
            buttonConnection.setImageResource(mainService.isConnectedUdp() ? R.drawable.icon_close : R.drawable.icon_reconnect);
            if (mainService.isConnectedOnEspWifi() && !mainService.isConnectedUdp()) {
                mainService.connect();
            }
        }
    }

    @Override
    public void onPaused() {
        sensorManager.unregisterListener(sensorEvent);
        popupMenu.dismiss();
        if (mainService != null && mainService.isConnectedUdp()) {
            mainService.clear();
            mainService.put(new Transceiver.Command("motors", "ld rd", false));
        }
    }

    @Override
    public void onDestroyed() {
        if (frameView.isRecording()) {
            Notify.showToast(R.string.saved);
            frameView.stopRecord();
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (mainService != null) {
            unbindService(serviceConnection);
        }
    }

    private final SensorEventListener sensorEvent = new SensorEventListener() {

        boolean zBreak;

        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            if (z > 9f) {
                if (zBreak) return;
                zBreak = true;
                angleView.setAngle(0);
                textAngleSide.setText(String.valueOf(0));
                if (mainService != null) {
                    mainService.put(new Transceiver.Command("motors", "ld rd", false));
                }
                return;
            }
            zBreak = false;
            //Log.d("lucas", "x:" + x + " y:" + y + " z:" + z);
            //Log.d("lucas", "x:" + x);
            //Log.d("lucas", "y:" + y);
            //Log.d("lucas", "z:" + z);
            angleSide = ((-1) * (Math.atan2(x, y) / (Math.PI / 180) - 90));
            //angleDirection = (int) ((-1) * (Math.atan2(x, z) / (Math.PI / 180) - 90));
            textAngleSide.setText(String.valueOf((int) angleSide));
            angleView.setAngle((int) angleSide);
            generateCommands();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private void generateCommands() {
        int angleGate;
        int angleMaximum = 25;
        int pwmSideMaximum = 1023;
        if (directionState != IDLE) angleGate = 5;
        else angleGate = 15;
        String leftCommand = "";
        String rightCommand = "";
        boolean persistent = true;
        if (angleSide == 0) {
            sideSpeed = 0;
        } else if (angleSide > angleMaximum) {
            sideSpeed = 0;
        } else if (angleSide > angleGate || angleSide < -angleGate) {
            double a = angleSide > 0 ? angleSide : -angleSide;
            float scale = 1f - ((float) a / angleMaximum);
            sideSpeed = (int) (pwmSideMaximum * scale);
        }
        if (sideSpeed < 0) sideSpeed = 0;
        else if (sideSpeed > pwmSideMaximum) sideSpeed = pwmSideMaximum;
        //Log.d("lucas", "sideSpeed: " + sideSpeed);
        if (angleSide >= angleGate) { // right
            //if (sideState != POSITIVE) Utils.vibrate();
            sideState = POSITIVE;
            if (directionState == POSITIVE) {
                leftCommand = "lf" + runSpeed;
                rightCommand = "rf" + sideSpeed;
            } else if (directionState == NEGATIVE) {
                leftCommand = "lb" + sideSpeed;
                rightCommand = "rb" + runSpeed;
            } else {
                leftCommand = "lf" + runSpeed; // rotationSpeed
                rightCommand = "rb" + runSpeed; // rotationSpeed
            }
        } else if (angleSide <= -angleGate) { // left
            //if (sideState != NEGATIVE) Utils.vibrate();
            sideState = NEGATIVE;
            if (directionState == POSITIVE) {
                leftCommand = "lf" + sideSpeed;
                rightCommand = "rf" + runSpeed;
            } else if (directionState == NEGATIVE) {
                leftCommand = "lb" + runSpeed;
                rightCommand = "rb" + sideSpeed;
            } else {
                leftCommand = "lb" + runSpeed; // rotationSpeed
                rightCommand = "rf" + runSpeed; // rotationSpeed
            }
        } else {
            sideSpeed = 0;
            if (sideState != IDLE) {
                //Utils.vibrate();
                sideState = IDLE;
                if (directionState == IDLE) {
                    leftCommand = "ld";
                    rightCommand = "rd";
                    persistent = false;
                } else {
                    if (directionState == POSITIVE) {
                        leftCommand = "lf" + runSpeed;
                        rightCommand = "rf" + runSpeed;
                    } else if (directionState == NEGATIVE) {
                        leftCommand = "lb" + runSpeed;
                        rightCommand = "rb" + runSpeed;
                    }
                }
            }
        }
        if (leftCommand.length() > 0) {
            Log.d("Lucas", "command:" + leftCommand + " " + rightCommand);
            if (mainService != null) mainService.put(new Transceiver.Command("motors", leftCommand + " " + rightCommand, persistent));
        }
    }

    private final MainService.Callback callback = new MainService.Callback() {

        long startTime = System.currentTimeMillis();
        int fps, kbps;

        final Handler hT = new Handler(Looper.getMainLooper());
        final Runnable rT = new Runnable() {
            @Override
            public void run() {
                textTx.setTextColor(getColor(R.color.white_2));
            }
        };

        final Handler hR = new Handler(Looper.getMainLooper());
        final Runnable rR = new Runnable() {
            @Override
            public void run() {
                textRx.setTextColor(getColor(R.color.white_2));
            }
        };

        @Override
        public void onSent() {
            hT.removeCallbacks(rT);
            hT.postDelayed(rT, 100);
            textTx.setTextColor(getColor(R.color.main));
        }

        @Override
        public void onReceive(byte[] image, long imageLength) {
            hR.removeCallbacks(rR);
            hR.postDelayed(rR, 250);
            textRx.setTextColor(getColor(R.color.main));
            if (image != null) {
                fps++;
                kbps += image.length;
                long time = System.currentTimeMillis();
                if (time - startTime >= 1000) {
                    textFps.setText(String.valueOf(fps));
                    if (kbps > 0) textNetworkVelocity.setText(String.valueOf(kbps / 1024));
                    startTime = time;
                    fps = 0;
                    kbps = 0;
                }

                Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, (int) imageLength);
                if (bitmap == null) {
                    Log.d("lucas", "Bitmap is null!");
                    return;
                }
                if (!resized && bitmap.getWidth() > 0 && bitmap.getHeight() > 0) {
                    resized = true;
                    ViewGroup.LayoutParams params = frameView.getLayoutParams();
                    params.width = (int) ((((float) imageViewHeight) / bitmap.getHeight()) * bitmap.getWidth());
                    params.height = imageViewHeight;
                    frameView.setLayoutParams(params);
                    imageWidth = bitmap.getWidth();
                    imageHeight = bitmap.getHeight();
                }
                frameView.setImageBitmap(bitmap);

                if (frameView.isRecording()) {
                    //Canvas canvas = surface.lockCanvas(new Rect(0, 0, imageWidth, imageHeight));
                    //canvas.drawBitmap(bitmap, 0, 0, new Paint());
                    //surface.unlockCanvasAndPost(canvas);
                    frameView.putFrame(bitmap);
                }
                //Log.d("lucas", "time: " + (System.currentTimeMillis() - time));
            }
        }

        private double resizeNumber(double number) {
            int i = (int) (number * 100);
            return (double) i / 100;
        }

        @Override
        public void onRssiChanged(int value) {
            textRssi.setText(String.valueOf(value));
            if (value <= -90) {
                textRssi.setTextColor(getColor(R.color.red));
            } else if (value <= -80) {
                textRssi.setTextColor(getColor(R.color.orange));
            } else {
                textRssi.setTextColor(getColor(R.color.green));
            }
        }

        @Override
        public void onSocketStateChanged(boolean connected) {
            resized = false;
            Utils.vibrate();
            Notify.showToast(connected ? R.string.connected : R.string.disconnected);
            buttonConnection.setImageResource(connected ? R.drawable.icon_close : R.drawable.icon_reconnect);
            textNetworkVelocity.setText(R.string.zero);
            textFps.setText(R.string.zero);
            //textRssi.setText(R.string.zero_db);
            textRssi.setTextColor(getColor(R.color.red));

            if (frameView.isRecording()) {
                Notify.showToast(R.string.saved);
                frameView.stopRecord();
                textTime.setVisibility(View.INVISIBLE);
            }

            if (connected) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                sendSettings();
            }
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MainService.LocalBinder binder = (MainService.LocalBinder) service;
            mainService = (MainService) binder.getServiceInstance();
            mainService.setCallback(callback);
            buttonConnection.setImageResource(mainService.isConnectedUdp() ? R.drawable.icon_close : R.drawable.icon_reconnect);
            if (mainService.isConnectedUdp()) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                sendSettings();
            } else if (mainService.isConnectedOnEspWifi()) {
                mainService.connect();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    private void sendSettings() {
        int ledFront = Prefs.getInt("led_front");
        if (ledFront < 0) ledFront = 1023;
        boolean cameraEnabled = Prefs.getBoolean("camera_state");
        if (cameraEnabled) {
            resized = false;
            mainService.enableReceiver();
            frameView.setVisibility(View.VISIBLE);
        } else {
            mainService.disableReceiver();
        }
        int timeOn = Prefs.getInt("rotation_time_on");
        if (timeOn < 0) timeOn = 40;
        int timeOff = Prefs.getInt("rotation_time_off");
        if (timeOff < 0) timeOff = 40;
        mainService.put(new Transceiver.Command("led_front", "g" + ledFront));
        mainService.put(new Transceiver.Command("camera_state", cameraEnabled ? "c1" : "c0"));
        mainService.put(new Transceiver.Command("rotation_state", Prefs.getBoolean("pulsed_rotation") ? "p1" : "p0"));
        mainService.put(new Transceiver.Command("rotation_time_on", "tn" + (timeOn + 10)));
        mainService.put(new Transceiver.Command("rotation_time_off", "tf" + (timeOff + 10)));
    }
}
