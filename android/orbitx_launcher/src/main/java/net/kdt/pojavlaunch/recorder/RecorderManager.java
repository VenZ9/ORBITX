package net.kdt.pojavlaunch.recorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.widget.Toast;

import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.io.File;

/**
 * OrbitX — process-wide entry point for the in-game replay/recording mod.
 *
 * <p>The manager owns the single {@link ScreenRecorder}, translates the
 * one-time MediaProjection consent round-trip, and exposes a simple
 * {@link #toggle(Activity)} used by the in-game controls menu.</p>
 *
 * <p>Design note: OrbitX deliberately renders <b>no</b> recording button,
 * badge, timer or overlay while capturing. The recorder is driven entirely from
 * the existing in-game drawer, so a clip contains nothing but the game.</p>
 */
public class RecorderManager implements ScreenRecorder.StateListener {

    public static final int REQUEST_RECORD_SCREEN = 0x0B17;

    /** Preference key: the container the recorder writes to. */
    public static final String PREF_RECORD_FORMAT = "orbitx_record_format";

    private static RecorderManager sInstance;

    public static synchronized RecorderManager get() {
        if (sInstance == null) sInstance = new RecorderManager();
        return sInstance;
    }

    private Context mAppContext;
    private ScreenRecorder mRecorder;
    private RecorderFormat mPendingFormat = RecorderFormat.MP4;
    @Nullable private File mLastRecording;
    @Nullable private Listener mListener;

    public interface Listener {
        /** Called on the main thread whenever capture starts or stops. */
        void onRecordingStateChanged(boolean recording);
    }

    private RecorderManager() { }

    public void init(Context context) {
        if (mAppContext == null) {
            mAppContext = context.getApplicationContext();
            mRecorder = new ScreenRecorder(mAppContext, this);
        }
    }

    public void setListener(@Nullable Listener listener) {
        mListener = listener;
    }

    public boolean isRecording() {
        return mRecorder != null && mRecorder.isRecording();
    }

    @Nullable
    public File getLastRecording() {
        if (mRecorder != null && mRecorder.isRecording()) {
            return mRecorder.getOutputFile();
        }
        return mLastRecording;
    }

    public RecorderFormat getPreferredFormat() {
        if (mAppContext == null) return RecorderFormat.MP4;
        int ordinal = LauncherPreferences.DEFAULT_PREF.getInt(PREF_RECORD_FORMAT, RecorderFormat.MP4.ordinal());
        return RecorderFormat.fromOrdinal(ordinal);
    }

    public void setPreferredFormat(RecorderFormat format) {
        if (mAppContext == null) return;
        LauncherPreferences.DEFAULT_PREF.edit()
                .putInt(PREF_RECORD_FORMAT, format.ordinal())
                .apply();
    }

    /** Start (asking for consent) or stop the current capture. */
    public void toggle(Activity activity) {
        if (mRecorder == null) init(activity);
        if (isRecording()) {
            mRecorder.stop();
            mLastRecording = mRecorder.getOutputFile();
            if (mLastRecording != null) {
                Toast.makeText(activity,
                        activity.getString(R.string.orbitx_recording_saved,
                                mLastRecording.getAbsolutePath()),
                        Toast.LENGTH_LONG).show();
            }
            return;
        }

        mPendingFormat = getPreferredFormat();
        MediaProjectionManager manager =
                (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (manager == null) {
            Toast.makeText(activity, R.string.orbitx_recording_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            activity.startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_RECORD_SCREEN);
        } catch (Throwable t) {
            Toast.makeText(activity, R.string.orbitx_recording_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * @return true when the result belonged to the recorder and was consumed.
     */
    public boolean handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != REQUEST_RECORD_SCREEN) return false;
        if (mRecorder == null) return true;
        if (!mRecorder.start(resultCode, data, mPendingFormat, R.string.orbitx_recording_failed)) {
            return true;
        }
        mLastRecording = mRecorder.getOutputFile();
        return true;
    }

    /** Export the most recent clip to another container, or to GIF. */
    public void exportLast(@Nullable RecorderFormat target, boolean asGif, VideoExporter.Callback callback) {
        File source = getLastRecording();
        if (source == null || !source.exists()) {
            if (callback != null) callback.onExportFinished(null, false);
            return;
        }
        VideoExporter.export(source, target, asGif, callback);
    }

    public void release() {
        if (mRecorder != null) mRecorder.release();
    }

    // ── ScreenRecorder.StateListener ─────────────────────────────────────────

    @Override
    public void onRecorderStateChanged(boolean recording) {
        if (mListener != null) mListener.onRecordingStateChanged(recording);
    }

    @Override
    public void onRecorderError(int messageRes) {
        if (mAppContext != null) {
            Toast.makeText(mAppContext, messageRes, Toast.LENGTH_LONG).show();
        }
    }
}
