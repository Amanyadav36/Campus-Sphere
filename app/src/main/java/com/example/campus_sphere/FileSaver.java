package com.example.campus_sphere;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public final class FileSaver {

    private FileSaver() {}

    public static Uri saveCsvToDownloads(Context context, String fileName, String csvContent) throws Exception {
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = "registrations.csv";
        }
        if (!fileName.toLowerCase().endsWith(".csv")) {
            fileName = fileName + ".csv";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CampusSphere");

            ContentResolver resolver = context.getContentResolver();
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                throw new IllegalStateException("Failed to create download entry");
            }

            try (OutputStream os = resolver.openOutputStream(uri)) {
                if (os == null) throw new IllegalStateException("Failed to open output stream");
                CsvUtils.writeUtf8(os, csvContent);
            }
            return uri;
        }

        // Pre-Android 10: save into app-specific downloads (no storage permission required).
        File base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (base == null) {
            throw new IllegalStateException("External files dir not available");
        }
        File dir = new File(base, "CampusSphere");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        File out = new File(dir, fileName);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            CsvUtils.writeUtf8(fos, csvContent);
        }
        return Uri.fromFile(out);
    }
}
