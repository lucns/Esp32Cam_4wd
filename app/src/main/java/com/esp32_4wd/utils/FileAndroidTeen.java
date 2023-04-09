package com.esp32_4wd.utils;

import android.content.Context;
import android.content.UriPermission;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class FileAndroidTeen {

    public static final int INTERNAL_STORAGE = 1;
    public static final int SD_CARD = 2;
    public static final int DATA_APP = 3;

    private Context context;
    private String fullPath;
    private File javaFile;

    public FileAndroidTeen() {
        context = App.getContext();
    }

    public FileAndroidTeen(int path) {
        this(path, "", "");
    }

    public FileAndroidTeen(String path) {
        if (path == null || path.isEmpty())
            throw new NullPointerException("Invalid path. Parameter null os empty is forbidden.");
        context = App.getContext();
        fullPath = path;
        javaFile = new File(fullPath);
    }

    public FileAndroidTeen(int path, String fileName) {
        this(path, "", fileName);
    }

    public FileAndroidTeen(String path, String fileName) {
        if (path == null || path.isEmpty())
            throw new NullPointerException("Invalid path. Parameter null os empty is forbidden.");
        if (fileName == null || fileName.isEmpty())
            throw new NullPointerException("Invalid file name. Parameter null os empty is forbidden.");
        context = App.getContext();
        fullPath = path + "/" + fileName;
        javaFile = new File(fullPath);
    }

    public FileAndroidTeen(int path, String directory, String fileName) {
        if (path != INTERNAL_STORAGE && path != SD_CARD) {
            if (fileName == null || fileName.isEmpty())
                throw new NullPointerException("Invalid file name. Parameter null os empty is forbidden.");
        }

        context = App.getContext();
        fileName = fileName.startsWith("/") ? fileName : "/" + fileName;
        directory = directory.startsWith("/") ? directory : "/" + directory;
        switch (path) {
            case INTERNAL_STORAGE:
                fullPath = getInternalPath() + directory + fileName;
                break;
            case SD_CARD:
                fullPath = getSdCardPath() + directory + fileName;
                break;
            case DATA_APP:
                fullPath = getInternalFiles() + directory + fileName;
                break;
            default:
                throw new NullPointerException("Invalid file path. Path parameter can be EXTERNAL_STORAGE, INTERNAL_STORAGE or a String.");
        }
        javaFile = new File(fullPath);
    }

    private void setFile(File f) {
        javaFile = f;
        fullPath = f.getPath();
    }

    public long lastModified() {
        return javaFile.lastModified();
    }

    public boolean isFile() {
        return javaFile.isFile();
    }

    public boolean isDirectory() {
        return javaFile.isDirectory();
    }

    public boolean exists() {
        return javaFile.exists();
    }

    public long length() {
        return javaFile.length();
    }

    public String getName() {
        return javaFile.getName();
    }

    public void setName(String name) {
        javaFile = new File(javaFile, name);
        fullPath = javaFile.getPath();
    }

    public String getPath() {
        return javaFile.getPath();
    }

    public void setPath(String path) {
        fullPath = path;
        javaFile = new File(fullPath);
    }

    public String getParent() {
        return javaFile.getParent();
    }

    public FileAndroidTeen getParentFile() {
        return new FileAndroidTeen(getParent());
    }

    public Uri getUri() {
        String storageName = "primary";
        String path = fullPath.substring(getInternalPath().length());
        String parent = path.substring(0, path.lastIndexOf("/"));
        Uri parentUri = Uri.parse("content://com.android.externalstorage.documents/tree/" + storageName + ":" + parent);
        return DocumentsContract.buildDocumentUriUsingTree(parentUri, storageName + ":" + path);
    }

    public boolean delete() {
        String storageName = "primary";
        String path = fullPath.substring(getInternalPath().length());
        String parent = path.substring(0, path.lastIndexOf("/"));
        Uri documentUri = Uri.parse("content://com.android.externalstorage.documents/tree/" + storageName + ":" + parent);
        documentUri = DocumentsContract.buildDocumentUriUsingTree(documentUri, storageName + ":" + path);
        try {
            return DocumentsContract.deleteDocument(context.getContentResolver(), documentUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean createFile() {
        String fileName = fullPath.substring(fullPath.lastIndexOf("/") + 1);
        String folder = fullPath.substring(0, fullPath.lastIndexOf("/"));
        if (!createDirectories(folder)) return false;
        String mime = null;
        if (fileName.contains(".") && !fileName.startsWith(".")) {
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
            mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        if (mime == null) mime = "*/*";

        String storageName = "primary";
        String path = fullPath.substring(getInternalPath().length());
        String parent = path.substring(0, path.lastIndexOf("/"));
        Uri parentUri = Uri.parse("content://com.android.externalstorage.documents/tree/" + storageName + ":" + parent);
        parentUri = DocumentsContract.buildDocumentUriUsingTree(parentUri, storageName + ":" + parent);
        try {
            return DocumentsContract.createDocument(context.getContentResolver(), parentUri, mime, fileName) != null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean createDirectory() {
        return createDirectory(fullPath);
    }

    private boolean createDirectories() {
        return createDirectories(fullPath);
    }

    private boolean createDirectory(String fullPath) {
        File folder = new File(fullPath);
        if (folder.exists() && folder.isDirectory()) return true;
        
        String fileName = fullPath.substring(fullPath.lastIndexOf("/") + 1);
        String storageName = "primary";
        String path = fullPath.substring(getInternalPath().length());
        String parent = path.substring(0, path.lastIndexOf("/"));

        Uri parentUri = Uri.parse("content://com.android.externalstorage.documents/tree/" + storageName + ":" + parent);
        parentUri = DocumentsContract.buildDocumentUriUsingTree(parentUri, storageName + ":" + parent);
        try {
            return DocumentsContract.createDocument(context.getContentResolver(), parentUri, DocumentsContract.Document.MIME_TYPE_DIR, fileName) != null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return true;
    }

    private boolean createDirectories(String fullPath) {
        FileAndroidTeen folder = new FileAndroidTeen(fullPath);
        if (folder.exists() && folder.isDirectory()) return true;
        String path = fullPath.substring(getInternalPath().length());
        String[] children = path.split("/");
        StringBuilder builder = new StringBuilder();
        builder.append(getInternalPath());
        for (String child : children) {
            builder.append("/");
            builder.append(child);
            if (!createDirectory(builder.toString())) return false;
        }
        return true;
    }

    public InputStream getInputStream() {
        if (!exists() || isDirectory()) return null;
        String storageName = "primary";
        String path = fullPath.substring(getInternalPath().length());
        String parent = path.substring(0, path.lastIndexOf("/"));
        Uri parentUri = Uri.parse("content://com.android.externalstorage.documents/tree/" + storageName + ":" + parent);
        Uri fileUri = DocumentsContract.buildDocumentUriUsingTree(parentUri, storageName + ":" + path);
        try {
            return context.getContentResolver().openInputStream(fileUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public OutputStream getOutputStream() {
        if (!exists() || isDirectory()) return null;
        String storageName = "primary";
        String path = fullPath.substring(getInternalPath().length());
        String parent = path.substring(0, path.lastIndexOf("/"));
        Uri parentUri = Uri.parse("content://com.android.externalstorage.documents/tree/" + storageName + ":" + parent);
        Uri fileUri = DocumentsContract.buildDocumentUriUsingTree(parentUri, storageName + ":" + path);
        try {
            return context.getContentResolver().openOutputStream(fileUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ParcelFileDescriptor getParcelFileDescriptor() {
        String storageName = "primary";
        String path = fullPath.substring(getInternalPath().length());
        String parent = path.substring(0, path.lastIndexOf("/"));
        Uri parentUri = Uri.parse("content://com.android.externalstorage.documents/tree/" + storageName + ":" + parent);
        Uri fileUri = DocumentsContract.buildDocumentUriUsingTree(parentUri, storageName + ":" + path);
        try {
            return context.getContentResolver().openFileDescriptor(fileUri, "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean setContent(byte[] content) {
        if ((!exists() || isDirectory()) && !createFile()) return false;

        OutputStream out = getOutputStream();
        if (out == null) return false;

        try {
            out.write(content);
            out.flush();
            out.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public File getInternalFile() {
        File[] files = context.getExternalFilesDirs(null);
        for (File f : files) {
            String path = f.getPath();
            if (path.contains("storage") && path.contains("emulated")) {
                String internal = f.getPath().substring(0, f.getPath().lastIndexOf("/Android"));
                return new File(internal);
            }
        }
        return null;
    }

    public String getInternalPath() {
        return getInternalFile().getPath();
    }

    public boolean isInternalUri(Uri uri) {
        if (uri == null) return false;
        return uri.toString().equals("content://com.android.externalstorage.documents/tree/primary%3A");
    }

    public boolean hasInternalAccess() {
        List<UriPermission> uriPermissions = context.getContentResolver().getPersistedUriPermissions();
        for (UriPermission uriPermission : uriPermissions) {
            if (isInternalUri(uriPermission.getUri()))
                return uriPermission.isReadPermission() && uriPermission.isWritePermission();
        }
        return false;
    }

    public static File getPrivateFiles() { // /data/data/com.package_name/files
        return App.getContext().getFilesDir();
    }

    public static File getPrivateCache() { // /data/data/com.package_name/cache
        return App.getContext().getCacheDir();
    }

    public static File getInternalFiles() { // /storage/emulated/0/Android/data/com.package_name/files
        return App.getContext().getExternalFilesDir(null);
    }

    public static File getInternalCache() { // /storage/emulated/0/Android/data/com.package_name/cache
        return App.getContext().getExternalCacheDir();
    }

    public String getSdCardName() {
        String sdcard = getSdCardPath();
        if (sdcard == null) return null;
        return sdcard.substring(sdcard.lastIndexOf("/") + 1);
    }

    public String getSdCardPath() {
        File[] files = context.getExternalFilesDirs(null);
        String sdcard = null;
        for (File f : files) {
            String path = f.getPath();
            if (path.contains("storage") && !path.contains("emulated")) {
                sdcard = path.substring(0, path.lastIndexOf("/Android"));
            }
        }
        return sdcard;
    }
}