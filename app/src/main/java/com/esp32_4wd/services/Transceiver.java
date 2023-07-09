package com.esp32_4wd.services;

import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashMap;
import java.util.Map;

public class Transceiver {

    public interface Callback {
        void onConnectionStateChanged(boolean connected);

        void onReceive(byte[] image, long imageLength);

        void onSent();
    }

    public static class Command {
        public String key, text;
        public boolean persistent;

        public Command(String key, String text, boolean persistent) {
            this.key = key;
            this.text = text;
            this.persistent = persistent;
        }

        public Command(String key, String text) {
            this.key = key;
            this.text = text;
        }
    }

    private final Map<String, Command> map;
    private Thread threadWriter, threadReader;
    private DatagramSocket socket;
    private final Handler mainLoopReceive, mainLoopSent;
    private final Callback callback;
    private boolean repeatedly, connected;
    private long imageLength;

    public Transceiver(Callback callback) {
        this.callback = callback;
        mainLoopReceive = new Handler(Looper.getMainLooper());
        mainLoopSent = new Handler(Looper.getMainLooper());
        map = new LinkedHashMap<>();
    }

    public void repeatedly(boolean repeatedly) {
        this.repeatedly = repeatedly;
    }

    public void clear() {
        map.clear();
    }

    public void put(Command command) {
        if (!isConnected()) return;
        map.put(command.key, command);
    }

    public void remove(Command command) {
        map.remove(command.key);
    }

    public void disableReceiver() {
        if (threadReader != null && threadReader.getState() == Thread.State.RUNNABLE) {
            threadReader.interrupt();
        }
    }

    private void initializeLoops() {
        if (threadWriter != null && threadWriter.getState() == Thread.State.RUNNABLE) return;
        threadWriter = new Thread(new Runnable() {
            @Override
            public void run() {
                while (threadWriter != null && threadWriter.getState() == Thread.State.RUNNABLE) {
                    if (map.size() == 0) continue;
                    String[] keys;
                    try {
                        keys = map.keySet().toArray(new String[0]);
                    } catch (ConcurrentModificationException e) {
                        e.printStackTrace();
                        continue;
                    }
                    for (String key : keys) {
                        if (!map.containsKey(key)) continue;
                        Command command = map.get(key);
                        if (command == null) continue;
                        if (!command.persistent) map.remove(key);
                        byte[] buf = command.text.getBytes();
                        try {
                            DatagramPacket packet = new DatagramPacket(buf, buf.length);
                            if (socket == null || socket.isClosed()) break;
                            socket.send(packet);
                        } catch (IOException e) {
                            e.printStackTrace();
                            connected = false;
                            return;
                        }
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return;
                        }
                        mainLoopReceive.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSent();
                            }
                        });
                    }
                }
            }
        });
        threadWriter.start();
    }

    public void enableReceiver() {
        if (threadReader != null && threadReader.getState() == Thread.State.RUNNABLE) return;
        threadReader = new Thread(new Runnable() {
            @Override
            public void run() {
                imageLength = 0;
                int bufferLength = 32768;
                try {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    DatagramPacket packet;
                    while (threadReader != null && threadReader.getState() == Thread.State.RUNNABLE) {
                        if (socket == null || socket.isClosed()) break;
                        byte[] b = new byte[bufferLength];
                        packet = new DatagramPacket(b, bufferLength);
                        socket.setSoTimeout(1000);
                        socket.receive(packet);
                        if (packet.getLength() > 0) {
                            if (b[0] == 'l' && b[1] == 'e' && b[2] == 'n' && b[3] == 'g' && b[4] == 't' && b[5] == 'h') {
                                String value = new String(packet.getData(), 0, packet.getLength());
                                imageLength = Long.parseLong(value.substring(value.indexOf(" ") + 1));
                                byteArrayOutputStream.reset();
                                mainLoopReceive.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (socket == null || socket.isClosed()) return;
                                        //Log.d("lucas", "total packet length: " + imageLength);
                                        callback.onReceive(null, imageLength);
                                    }
                                });
                            } else if (byteArrayOutputStream.size() <= imageLength) {
                                //Log.d("lucas", "fractionated packet length " + packet.getLength());
                                byteArrayOutputStream.write(b, 0, packet.getLength());
                                if (byteArrayOutputStream.size() == imageLength) {
                                    byte[] image = byteArrayOutputStream.toByteArray();
                                    byteArrayOutputStream.reset();
                                    mainLoopReceive.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (socket == null || socket.isClosed()) return;
                                            //Log.d("lucas", "final packet length " + image.length);
                                            callback.onReceive(image, image.length);
                                        }
                                    });
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (threadReader.isInterrupted()) return;
                    //if (socket.isConnected()) return;
                    //if (!socket.isClosed()) return;
                    connected = false;
                    mainLoopReceive.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onConnectionStateChanged(false);
                        }
                    });
                }
            }
        });
        threadReader.start();
    }

    public void connect() {
        if (connected) {
            callback.onConnectionStateChanged(true);
            return;
        }
        // close();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new DatagramSocket();
                    socket.connect(InetAddress.getByName("192.168.1.1"), 1234);

                    if (!socket.isConnected()) {
                        mainLoopReceive.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onConnectionStateChanged(false);
                            }
                        });
                    } else {
                        //socket.send(new DatagramPacket(new byte[] {1}, 1)); // send 1 number for wifi module catch android address
                        connected = true;
                        mainLoopReceive.post(new Runnable() {
                            @Override
                            public void run() {
                                initializeLoops();
                                callback.onConnectionStateChanged(true);
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void send(String s) {
        if (socket == null || socket.isClosed() || s == null || s.isEmpty()) return;
        byte[] buf = s.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
            connected = false;
        }
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected() && connected;
    }

    public void close() {
        connected = false;
        if (socket != null && !socket.isClosed()) socket.close();
        if (threadWriter != null && threadWriter.getState() == Thread.State.RUNNABLE)
            threadWriter.interrupt();
        if (threadReader != null && threadReader.getState() == Thread.State.RUNNABLE)
            threadReader.interrupt();
    }
}
