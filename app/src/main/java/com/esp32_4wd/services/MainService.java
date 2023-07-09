package com.esp32_4wd.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.LinkedHashMap;
import java.util.Map;

public class MainService extends BaseService {

    public interface Callback {

        void onSent();

        void onReceive(byte[] image, long imageLength);

        void onRssiChanged(int rssi);

        void onSocketStateChanged(boolean connected);
    }

    private WifiManager wifiManager;
    private final String ESP_SSID = "Esp32_Cam_4wd";
    private final String ESP_PASSWORD = "12345678";
    private Transceiver udp;
    private Callback callback;
    private Handler handler;
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (callback != null) callback.onRssiChanged(wifiManager.getConnectionInfo().getRssi());
            if (isConnectedOnWifi()) handler.postDelayed(this, 1000);
        }
    };
    private ConnectivityManager connectivityManager;
    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {

        @Override
        public void onAvailable(Network network) {
            if (isConnectedOnEspWifi() && !udp.isConnected()) udp.connect();
        }

        @Override
        public void onLost(Network network) {
            udp.close();
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSocketStateChanged(false);
                    }
                });
            }
        }
    };

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        udp = new Transceiver(new Transceiver.Callback() {
            @Override
            public void onConnectionStateChanged(boolean connected) {
                if (callback != null) callback.onSocketStateChanged(connected);
                handler.removeCallbacks(runnable);
                if (connected) handler.post(runnable);
            }

            @Override
            public void onReceive(byte[] image, long imageLength) {
                if (callback != null) callback.onReceive(image, imageLength);
            }

            @Override
            public void onSent() {
                if (callback != null) callback.onSent();
            }
        });
        //udp.repeatedly(true);

        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        builder.build();
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        connectivityManager.registerNetworkCallback(builder.build(), networkCallback);

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction("android.new.conn.CONNECTIVITY_CHANGE");
        //registerReceiver(stateReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        connectivityManager.unregisterNetworkCallback(networkCallback);
        handler.removeCallbacks(runnable);
        //unregisterReceiver(stateReceiver);
    }

    @Override
    public NotificationProvider onForegroundRequested() {
        return null;
    }

    public void close() {
        handler.removeCallbacks(runnable);
        if (udp.isConnected()) udp.close();
        callback.onSocketStateChanged(false);
    }

    public void clear() {
        udp.clear();
    }

    public void remove(Transceiver.Command command) {
        udp.remove(command);
    }

    public void put(Transceiver.Command command) {
        udp.put(command);
    }

    public void disableReceiver() {
        udp.disableReceiver();
    }

    public void enableReceiver() {
        udp.enableReceiver();
    }

    public boolean isConnectedUdp() {
        return udp.isConnected();
    }

    public void connect() {
        if (udp.isConnected() || !isConnectedOnEspWifi()) return;
        udp.connect();
    }

    public boolean isConnectedOnEspWifi() {
        return getNetworkName().equals(ESP_SSID);
    }

    public String getNetworkName() {
        if (!isConnectedOnWifi()) return "";
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) return "";
        else return wifiInfo.getSSID().replaceAll("\"", "");
    }

    private boolean isConnectedOnWifi() {
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {

        boolean first;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!first) return;
            first = true;

            if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_ENABLED:
                        break;
                    case WifiManager.WIFI_STATE_DISABLED:
                        udp.close();
                        if (callback != null) callback.onSocketStateChanged(false);
                        break;
                }
            }
        }
    };
}
