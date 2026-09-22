package net.kdt.pojavlaunch.recorder;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * OrbitX — where in-game recordings live.
 *
 * <p>Clips are written to {@code &lt;external&gt;/OrbitX/Recordings}, a plain
 * user-visible folder, so a captured clip can be pulled off the device with any
 * file manager or USB connection — no media-scanner round trip and no hidden
 * app-private directory.</p>
 */
public final class RecorderPaths {

    public static final String ROOT_DIR_NAME = "OrbitX";
    public static final String RECORDINGS_DIR_NAME = "Recordings";

    private RecorderPaths() { }

    public static File recordingsDir(Context context) {
        File base = null;
        try {
            base = context.getExternalFilesDir(null);
        } catch (Throwable ignored) { }
        File root;
        if (base != null) {
            // getExternalFilesDir() -> Android/data/<pkg>/files ; go two levels up
            // so the clip lands in a folder that survives an uninstall and is easy
            // to find in a file manager.
            File androidData = base.getParentFile();
            File externalRoot = androidData != null ? androidData.getParentFile() : null;
            root = new File(externalRoot != null ? externalRoot : base, ROOT_DIR_NAME);
        } else {
            File external = Environment.getExternalStorageDirectory();
            root = new File(external != null ? external : context.getFilesDir(), ROOT_DIR_NAME);
        }
        File dir = new File(root, RECORDINGS_DIR_NAME);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    public static File newRecordingFile(Context context, RecorderFormat format) {
        String stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
        return new File(recordingsDir(context), "OrbitX_" + stamp + "." + format.extension);
    }
}
