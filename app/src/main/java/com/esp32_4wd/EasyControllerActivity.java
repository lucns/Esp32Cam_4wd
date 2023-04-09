package com.esp32_4wd;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
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
import com.esp32_4wd.views.FrameView;
import com.esp32_4wd.views.TriangleView;
import com.esp32_4wd.views.VideoRecorder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EasyControllerActivity extends BaseActivity {

    private PopupMenu popupMenu;
    private MainService mainService;
    private TextView textRssi, textFps, textNetworkVelocity, textTime;
    private ImageButton buttonConnection;
    private TextView textTx, textRx;
    private int runSpeed, bendSpeed, sideProportion;
    private final int rotationPwm = 1023;
    private FrameView frameView;
    private boolean resized;
    private int imageWidth, imageHeight, imageViewHeight;

    @Override
    public boolean onCreated() {
        setContentView(R.layout.activity_easy_controller);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

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

        runSpeed = Prefs.getInt("run_speed");
        if (runSpeed < 0) runSpeed = 1023;
        sideProportion = Prefs.getInt("side_proportion");
        if (sideProportion < 0) sideProportion = 50;
        if (sideProportion == 0) bendSpeed = 0;
        else bendSpeed = (int) (runSpeed * (sideProportion) / 100.0d);

        TriangleView buttonUp = findViewById(R.id.buttonUp);
        TriangleView buttonDown = findViewById(R.id.buttonDown);
        TriangleView buttonLeft = findViewById(R.id.buttonLeft);
        TriangleView buttonRight = findViewById(R.id.buttonRight);
        buttonUp.setPosition(TriangleView.Positions.TOP);
        buttonDown.setPosition(TriangleView.Positions.BOTTOM);
        buttonLeft.setPosition(TriangleView.Positions.LEFT);
        buttonRight.setPosition(TriangleView.Positions.RIGHT);

        TriangleView.TouchCallback touchCallback = new TriangleView.TouchCallback() {

            @Override
            public void onTouch(View view, boolean touched) {
                if (touched) Utils.vibrate(75);
                else Utils.vibrate(40);
                String leftCommand, rightCommand;
                boolean persistent = true;
                switch (view.getId()) {
                    case R.id.buttonUp:
                        if (buttonDown.isTouched()) return;
                        if (touched) {
                            if (buttonLeft.isTouched()) {
                                leftCommand = "lf" + bendSpeed;
                                rightCommand = "rf" + runSpeed;
                            } else if (buttonRight.isTouched()) {
                                leftCommand = "lf" + runSpeed;
                                rightCommand = "rf" + bendSpeed;
                            } else {
                                leftCommand = "lf" + runSpeed;
                                rightCommand = "rf" + runSpeed;
                            }
                        } else {
                            if (buttonLeft.isTouched()) {
                                leftCommand = "lb" + runSpeed;
                                rightCommand = "rf" + runSpeed;
                            } else if (buttonRight.isTouched()) {
                                leftCommand = "lf" + runSpeed;
                                rightCommand = "rb" + runSpeed;
                            } else {
                                leftCommand = "ld";
                                rightCommand = "rd";
                                persistent = false;
                            }
                        }
                        break;
                    case R.id.buttonDown:
                        if (buttonUp.isTouched()) return;
                        if (touched) {
                            if (buttonLeft.isTouched()) {
                                leftCommand = "lb" + bendSpeed;
                                rightCommand = "rb" + runSpeed;
                            } else if (buttonRight.isTouched()) {
                                leftCommand = "lb" + runSpeed;
                                rightCommand = "rb" + bendSpeed;
                            } else {
                                leftCommand = "lb" + runSpeed;
                                rightCommand = "rb" + runSpeed;
                            }
                        } else {
                            if (buttonLeft.isTouched()) {
                                leftCommand = "lb" + runSpeed;
                                rightCommand = "rf" + runSpeed;
                            } else if (buttonRight.isTouched()) {
                                leftCommand = "lf" + runSpeed;
                                rightCommand = "rb" + runSpeed;
                            } else {
                                leftCommand = "ld";
                                rightCommand = "rd";
                                persistent = false;
                            }
                        }
                        break;
                    case R.id.buttonLeft:
                        if (buttonRight.isTouched()) return;
                        if (touched) {
                            if (buttonUp.isTouched()) {
                                leftCommand = "lb" + bendSpeed;
                                rightCommand = "rf" + runSpeed;
                            } else if (buttonDown.isTouched()) {
                                leftCommand = "lb" + runSpeed;
                                rightCommand = "rf" + bendSpeed;
                            } else {
                                leftCommand = "lb" + rotationPwm;
                                rightCommand = "rf" + rotationPwm;
                            }
                        } else {
                            if (buttonUp.isTouched()) {
                                leftCommand = "lf" + runSpeed;
                                rightCommand = "rf" + runSpeed;
                            } else if (buttonDown.isTouched()) {
                                leftCommand = "lb" + runSpeed;
                                rightCommand = "rb" + runSpeed;
                            } else {
                                leftCommand = "ld";
                                rightCommand = "rd";
                                persistent = false;
                            }
                        }
                        break;
                    case R.id.buttonRight:
                        if (buttonLeft.isTouched()) return;
                        if (touched) {
                            if (buttonUp.isTouched()) {
                                leftCommand = "lf" + runSpeed;
                                rightCommand = "rb" + bendSpeed;
                            } else if (buttonDown.isTouched()) {
                                leftCommand = "lf" + bendSpeed;
                                rightCommand = "rb" + runSpeed;
                            } else {
                                leftCommand = "lf" + rotationPwm;
                                rightCommand = "rb" + rotationPwm;
                            }
                        } else {
                            if (buttonUp.isTouched()) {
                                leftCommand = "lf" + runSpeed;
                                rightCommand = "rf" + runSpeed;
                            } else if (buttonDown.isTouched()) {
                                leftCommand = "lb" + runSpeed;
                                rightCommand = "rb" + runSpeed;
                            } else {
                                leftCommand = "ld";
                                rightCommand = "rd";
                                persistent = false;
                            }
                        }
                        break;
                    default:
                        return;
                }
                mainService.put(new Transceiver.Command("motors", leftCommand + " " + rightCommand, persistent));
            }
        };

        buttonUp.setTouchCallback(touchCallback);
        buttonDown.setTouchCallback(touchCallback);
        buttonLeft.setTouchCallback(touchCallback);
        buttonRight.setTouchCallback(touchCallback);

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
                    case R.id.menu_speeds:
                        showDialogSpeed();
                        break;
                    case R.id.menu_lights:
                        showDialogLights();
                        break;
                    case R.id.menu_record:
                        Utils.vibrate();
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
                        Utils.vibrate();
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
                }
                return true;
            }
        });
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.menu_main, popupMenu.getMenu());

        Intent intent = new Intent(this, MainService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == RESULT_OK) {
            Uri uri = resultData.getData();
            FileAndroidTeen androidFile = new FileAndroidTeen();
            if (!androidFile.isInternalUri(uri)) {
                showDialogError(getString(R.string.error_invalid_system_files));
                return;
            }
            int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            getContentResolver().takePersistableUriPermission(uri, flags);
        }
    }

    private void showDialogSpeed() {
        Dialog dialog = generateDialog(R.layout.dialog_speed, true);
        dialog.findViewById(R.id.buttonPositive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        TextView textView = dialog.findViewById(R.id.textPercent);
        TextView textView2 = dialog.findViewById(R.id.textPercent2);
        TextView textTimeOnValue = dialog.findViewById(R.id.textTimeOnValue);
        TextView textTimeOffValue = dialog.findViewById(R.id.textTimeOffValue);
        CheckBox checkBox = dialog.findViewById(R.id.checkBox);
        SeekBar seekBar = dialog.findViewById(R.id.seekbar);
        SeekBar seekBar2 = dialog.findViewById(R.id.seekbar2);
        SeekBar seekbarTimeOn = dialog.findViewById(R.id.seekbarTimeOn);
        SeekBar seekbarTimeOff = dialog.findViewById(R.id.seekbarTimeOff);
        checkBox.setChecked(Prefs.getBoolean("pulsed_rotation"));
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Prefs.setBoolean("pulsed_rotation", isChecked);
                mainService.put(new Transceiver.Command("rotation_state", isChecked ? "p1" : "p0"));
            }
        });

        int percent = (int) (100 * ((double) runSpeed / 1023.0d));
        textView.setText(percent + "%");
        seekBar.setProgress(runSpeed);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                runSpeed = progress;
                int percent = (int) (100 * ((double) progress / 1023.0d));
                textView.setText(percent + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Prefs.setInt("run_speed", seekBar.getProgress());
            }
        });
        textView2.setText(sideProportion + "%");
        seekBar2.setProgress(sideProportion);
        seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sideProportion = progress;
                if (sideProportion == 0) bendSpeed = 0;
                else bendSpeed = (int) (runSpeed * (sideProportion) / 100.0d);
                textView2.setText(sideProportion + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Prefs.setInt("side_proportion", seekBar.getProgress());
            }
        });
        int timeOn = Prefs.getInt("rotation_time_on");
        if (timeOn < 0) timeOn = 40;
        textTimeOnValue.setText((timeOn + 10) + "mS");
        seekbarTimeOn.setProgress(timeOn);
        seekbarTimeOn.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Prefs.setInt("rotation_time_on", progress);
                textTimeOnValue.setText((progress + 10) + "mS");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mainService.put(new Transceiver.Command("rotation_time_on", "tn" + (seekBar.getProgress() + 10)));
            }
        });

        int timeOff = Prefs.getInt("rotation_time_off");
        if (timeOff < 0) timeOff = 40;
        textTimeOffValue.setText((timeOff + 10) + "mS");
        seekbarTimeOff.setProgress(timeOff);
        seekbarTimeOff.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Prefs.setInt("rotation_time_off", progress);
                textTimeOffValue.setText((progress + 10) + "mS");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mainService.put(new Transceiver.Command("rotation_time_off", "tf" + (seekBar.getProgress() + 10)));
            }
        });

        dialog.show();
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
        super.onResumed();
        if (mainService != null) {
            buttonConnection.setImageResource(mainService.isConnectedUdp() ? R.drawable.icon_close : R.drawable.icon_reconnect);
            if (mainService.isConnectedOnEspWifi() && !mainService.isConnectedUdp()) {
                mainService.connect();
            }
        }
    }

    @Override
    public void onPaused() {
        super.onPaused();
        popupMenu.dismiss();
        if (mainService != null && mainService.isConnectedUdp()) {
            mainService.clear();
            mainService.put(new Transceiver.Command("motors", "ld rd", false));
        }
    }

    @Override
    public void onDestroyed() {
        super.onDestroyed();
        if (frameView.isRecording()) {
            Notify.showToast(R.string.saved);
            frameView.stopRecord();
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (mainService != null) {
            unbindService(serviceConnection);
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