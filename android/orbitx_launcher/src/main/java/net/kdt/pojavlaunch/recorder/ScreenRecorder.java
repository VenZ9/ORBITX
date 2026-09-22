package net.kdt.pojavlaunch.recorder;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import java.io.File;

/**
 * OrbitX — screen recorder.
 *
 * <p>Captures the game surface straight from the system compositor through a
 * {@link MediaProjection} + {@link VirtualDisplay} pair, so the recorded frames
 * contain <b>only</b> what the game draws plus whatever the launcher is showing
 * at that instant. OrbitX never draws a recorder button, badge or indicator, so
 * a recording started from the in-game drawer is completely clean.</p>
 *
 * <p>The caller is responsible for the one-time MediaProjection consent prompt
 * and for handing the resulting result-code/Intent pair to
 * {@link #start(int, Intent, RecorderFormat)}.</p>
 */
public class ScreenRecorder {

    private static final String TAG = "OrbitXRecorder";

    public interface StateListener {
        void onRecorderStateChanged(boolean recording);
        /** @param messageRes string resource describing the failure. */
        void onRecorderError(int messageRes);
    }

    private final Context mContext;
    private final StateListener mListener;

    @Nullable private MediaRecorder mRecorder;
    @Nullable private MediaProjection mProjection;
    @Nullable private VirtualDisplay mVirtualDisplay;
    @Nullable private MediaProjection.Callback mProjectionCallback;
    @Nullable private File mOutputFile;
    private boolean mRecording;

    public ScreenRecorder(Context context, StateListener listener) {
        mContext = context.getApplicationContext();
        mListener = listener;
    }

    public boolean isRecording() {
        return mRecording;
    }

    @Nullable
    public File getOutputFile() {
        return mOutputFile;
    }

    /**
     * Begin capturing.
     *
     * @param resultCode the RESULT_OK code returned by the consent activity
     * @param data       the Intent returned by the consent activity
     * @param format     target container/codec pair
     * @return true when capture is running
     */
    public boolean start(int resultCode, Intent data, RecorderFormat format, int messageResOnError) {
        if (mRecording) return true;
        if (data == null) {
            notifyError(messageResOnError);
            return false;
        }

        try {
            MediaProjectionManager manager =
                    (MediaProjectionManager) mContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (manager == null) {
                notifyError(messageResOnError);
                return false;
            }
            mProjection = manager.getMediaProjection(resultCode, data);
            if (mProjection == null) {
                notifyError(messageResOnError);
                return false;
            }

            // The projection must have a foreground service of type mediaProjection
            // alive before the first frame, otherwise Android 14+ kills it instantly.
            RecorderService.start(mContext);

            Point size = resolveScreenSize();
            DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
            int density = metrics.densityDpi;
            // Low-end friendly: scale the capture down on huge panels and keep the
            // bitrate proportional instead of hard-coding a flagship value.
            int width = size.x;
            int height = size.y;
            int longEdge = Math.max(width, height);
            float scale = longEdge > 1920 ? 1920f / longEdge : 1f;
            int encWidth = even((int) (width * scale));
            int encHeight = even((int) (height * scale));

            mOutputFile = RecorderPaths.newRecordingFile(mContext, format);

            mRecorder = new MediaRecorder();
            mRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(format.outputFormat);
            mRecorder.setVideoEncoder(format.videoEncoder);
            mRecorder.setAudioEncoder(format.audioEncoder);
            mRecorder.setVideoSize(encWidth, encHeight);
            mRecorder.setVideoFrameRate(30);
            mRecorder.setVideoEncodingBitRate(videoBitrateFor(encWidth, encHeight));
            mRecorder.setAudioEncodingBitRate(128_000);
            mRecorder.setAudioSamplingRate(44_100);
            mRecorder.setOutputFile(mOutputFile.getAbsolutePath());

            mRecorder.prepare();

            mVirtualDisplay = mProjection.createVirtualDisplay(
                    "OrbitXRecorder",
                    width, height, density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mRecorder.getSurface(), null, null);

            mProjectionCallback = new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    // User revoked the capture from the system UI.
                    if (mRecording) stop();
                }
            };
            mProjection.registerCallback(mProjectionCallback, null);

            mRecorder.start();
            mRecording = true;
            if (mListener != null) mListener.onRecorderStateChanged(true);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "recording failed to start", e);
            teardown();
            notifyError(messageResOnError);
            return false;
        }
    }

    /** Stop and finalise the current recording. Safe to call when not recording. */
    public void stop() {
        if (!mRecording && mRecorder == null) return;
        try {
            if (mRecorder != null) {
                mRecorder.stop();
            }
        } catch (Exception e) {
            // MediaRecorder.stop() throws when the capture was shorter than a
            // couple of frames; the partial file is simply discarded.
            Log.w(TAG, "recorder stop threw (short capture?)", e);
        } finally {
            teardown();
            mRecording = false;
            if (mListener != null) mListener.onRecorderStateChanged(false);
        }
    }

    /** Release everything without starting; call from onDestroy. */
    public void release() {
        if (mRecording) stop();
        else teardown();
    }

    private void teardown() {
        if (mRecorder != null) {
            try { mRecorder.reset(); } catch (Throwable ignored) { }
            try { mRecorder.release(); } catch (Throwable ignored) { }
            mRecorder = null;
        }
        if (mVirtualDisplay != null) {
            try { mVirtualDisplay.release(); } catch (Throwable ignored) { }
            mVirtualDisplay = null;
        }
        if (mProjection != null) {
            try {
                if (mProjectionCallback != null) mProjection.unregisterCallback(mProjectionCallback);
            } catch (Throwable ignored) { }
            try { mProjection.stop(); } catch (Throwable ignored) { }
            mProjection = null;
        }
        mProjectionCallback = null;
        RecorderService.stop(mContext);
    }

    private Point resolveScreenSize() {
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            DisplayMetrics m = mContext.getResources().getDisplayMetrics();
            return new Point(even(m.widthPixels), even(m.heightPixels));
        }
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        return new Point(even(size.x), even(size.y));
    }

    private static int even(int value) {
        return (value % 2 == 0) ? value : value - 1;
    }

    private static int videoBitrateFor(int width, int height) {
        // ~0.12 bit/pixel/frame at 30fps, clamped to a sane low-end window.
        int estimate = (int) (width * (long) height * 0.12f);
        return Math.max(2_500_000, Math.min(estimate, 16_000_000));
    }

    private void notifyError(int messageRes) {
        if (mListener != null) mListener.onRecorderError(messageRes);
    }
}
