package com.esp32_4wd.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.esp32_4wd.R;
import com.esp32_4wd.utils.Notify;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class FrameView extends ImageView {

    private final VideoRecorder videoRecorder;

    public FrameView(Context context) {
        super(context);
        videoRecorder = new VideoRecorder();
    }

    public void putFrame(Bitmap frame) {
        if (frame == null) return;
        setImageBitmap(frame);
        videoRecorder.putFrame(frame);
    }

    public void prepare(int videoWidth, int videoHeight) {
        videoRecorder.prepare(videoWidth, videoHeight);
    }

    public boolean hasFrame() {
        return videoRecorder.getFrame() != null;
    }

    public FrameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        videoRecorder = new VideoRecorder();
    }

    public boolean isRecording() {
        return videoRecorder.isRecording();
    }

    public boolean startRecord(VideoRecorder.TimeCallback timeCallback) {
        File file = getFile(true, "DCIM/" + getContext().getString(R.string.app_name) + "/" + generateFileName(false));
        return videoRecorder.startRecorder(file.getPath(), timeCallback);
    }

    public void stopRecord() {
        videoRecorder.stopRecorder();
    }

    public void saveFrame() {
        if (videoRecorder.getFrame() == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                File imageFile = getFile(true, "DCIM/" + getContext().getString(R.string.app_name) + "/" + generateFileName(true));
                File parent = imageFile.getParentFile();
                if (!parent.exists() || !parent.isDirectory()) parent.mkdirs();
                try {
                    if (!imageFile.exists() || !imageFile.isFile())
                        imageFile.createNewFile();
                    FileOutputStream outputStream = new FileOutputStream(imageFile);
                    videoRecorder.getFrame().compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
                    outputStream.close();
                    Notify.showToast(R.string.saved);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private String generateFileName(boolean isImage) {
        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss-SSS", Locale.getDefault());
        Calendar c = Calendar.getInstance();
        String textDate = date.format(c.getTime());
        if (isImage) return "IMG_" + textDate + ".jpg"; // Ex.: IMG_24-02-2016_22-42-56-963.jpg
        return "VID_" + textDate + ".mp4"; // Ex.: VID_24-02-2016_22-42-56-963.mp4
    }

    private File getFile(boolean internalMemory, String fileChild) {
        File[] files = getContext().getExternalFilesDirs(null);
        for (File f : files) {
            String path = f.getPath();
            if (path.contains("storage")) {
                if ((internalMemory && !path.contains("emulated")) || (!internalMemory && path.contains("emulated")))
                    continue;
                path = path.substring(0, path.indexOf("/Android"));
                if (fileChild == null) return new File(path);
                else return new File(path, fileChild);
            }
        }
        return null;
    }
}
