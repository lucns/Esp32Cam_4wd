package com.esp32_4wd.views;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import com.esp32_4wd.R;
import com.esp32_4wd.utils.FileAndroidTeen;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoRecorder {

    public interface TimeCallback {
        void onTimeChanged(long milliseconds);
    }

    private boolean isRecording;
    private String path;
    private int width, height;
    private Thread thread;
    private boolean hasNext;
    private Bitmap bitmap;
    private final int frameRate = 25;
    private Handler mainLoop;

    public VideoRecorder() {
        mainLoop = new Handler(Looper.getMainLooper());
    }

    public void prepare(int videoWidth, int videoHeight) {
        this.width = videoWidth;
        this.height = videoHeight;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean startRecorder(String path, TimeCallback callback) {
        if (isRecording) return true;
        isRecording = true;
        this.path = path;

        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1000000); //1000kbps
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        MediaCodec encoder;
        MediaMuxer muxer;
        int trackIndex;
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                FileAndroidTeen file = new FileAndroidTeen(path);
                file.createFile();
                muxer = new MediaMuxer(file.getParcelFileDescriptor().getFileDescriptor(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            } else {
                muxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            }
            trackIndex = muxer.addTrack(mediaFormat);
            muxer.setOrientationHint(0);
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            encoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        Surface surface = encoder.createInputSurface();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        muxer.start();
        encoder.start();

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Paint paint = new Paint();
                //Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
                Canvas canvas;
                long timeUs = 0;
                long intervalUs = 1000000 / frameRate; // frameRate = 25 -> 40mS
                int bufferIndex;
                long startTime;
                ByteBuffer byteBuffer;
                while (thread != null && !thread.isInterrupted()) {
                    if (bitmap == null) continue;
                    hasNext = false;
                    startTime = System.currentTimeMillis();

                    canvas = surface.lockCanvas(new Rect(0, 0, width, height));
                    canvas.drawBitmap(bitmap, 0, 0, paint);
                    surface.unlockCanvasAndPost(canvas);

                    do {
                        bufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000);
                        if (hasNext) break;
                    } while (bufferIndex < 0 && thread != null && !thread.isInterrupted());
                    if (bufferIndex < 0) continue;
                    if (timeUs == 0) startTime = System.currentTimeMillis();

                    if (bufferInfo.size > 0) {
                        bufferInfo.presentationTimeUs = timeUs;
                        byteBuffer = encoder.getOutputBuffer(bufferIndex);
                        if (byteBuffer != null) {
                            byteBuffer.position(bufferInfo.offset);
                            byteBuffer.limit(bufferInfo.offset + bufferInfo.size);
                            muxer.writeSampleData(trackIndex, byteBuffer, bufferInfo);
                            encoder.releaseOutputBuffer(bufferIndex, false);
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }

                    timeUs += intervalUs;
                    long time = timeUs / 1000;
                    mainLoop.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onTimeChanged(time);
                        }
                    });
                    long delay = (timeUs / 1000) - (System.currentTimeMillis() - startTime);
                    if (delay > 0) {
                        while (System.currentTimeMillis() < startTime + delay) {
                            if (hasNext) break; // sleep
                        }
                    }
                }
                encoder.signalEndOfInputStream();
                encoder.stop();
                encoder.release();
                try {
                    muxer.stop();
                    muxer.release();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        return true;
    }

    public void stopRecorder() {
        isRecording = false;
        if (thread != null && !thread.isInterrupted()) thread.interrupt();
        thread = null;
    }

    public void putFrame(Bitmap bitmap) {
        this.bitmap = bitmap;
        hasNext = true;
    }

    public Bitmap getFrame() {
        return bitmap;
    }
}
