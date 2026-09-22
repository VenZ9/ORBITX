package net.kdt.pojavlaunch.recorder;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

/**
 * OrbitX — clip exporter.
 *
 * <p>Takes a finished recording and produces one of the alternative formats
 * offered in the export picker:</p>
 * <ul>
 *   <li><b>MP4 / WebM</b> — a lossless re-mux of the original streams, no
 *       re-encode, so it is fast even on a low-end phone.</li>
 *   <li><b>GIF</b> — sampled frames re-encoded through {@link GifEncoder}.</li>
 * </ul>
 *
 * <p>Everything runs on a worker thread and reports back through
 * {@link Callback}; the UI shows its own progress affordance.</p>
 */
public final class VideoExporter {

    private static final String TAG = "OrbitXExporter";

    public interface Callback {
        /** @param output the produced file, or null when the export failed. */
        void onExportFinished(@Nullable File output, boolean success);
    }

    private VideoExporter() { }

    /**
     * @param source  the recorded clip
     * @param target  desired container, or null when {@code asGif} is set
     * @param asGif   when true a GIF is produced instead of a video container
     */
    public static void export(File source, @Nullable RecorderFormat target, boolean asGif, Callback callback) {
        new Thread(() -> {
            File output = null;
            boolean ok = false;
            try {
                if (asGif) {
                    output = exportGif(source);
                    ok = output != null && output.length() > 0;
                } else if (target != null) {
                    output = exportContainer(source, target);
                    ok = output != null && output.length() > 0;
                }
            } catch (Throwable t) {
                Log.w(TAG, "export failed", t);
                ok = false;
            }
            if (!ok) output = null;
            File result = output;
            boolean success = ok;
            if (callback != null) callback.onExportFinished(result, success);
        }, "OrbitX-Export").start();
    }

    @Nullable
    private static File exportContainer(File source, RecorderFormat target) throws Exception {
        if (!source.exists()) return null;
        File dst = siblingFile(source, target.extension);
        //noinspection ResultOfMethodCallIgnored
        if (dst.exists()) dst.delete();

        MediaExtractor extractor = new MediaExtractor();
        MediaMuxer muxer = null;
        try {
            extractor.setDataSource(source.getAbsolutePath());

            int videoTrack = -1;
            int audioTrack = -1;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime == null) continue;
                if (mime.startsWith("video/") && videoTrack < 0) videoTrack = i;
                else if (mime.startsWith("audio/") && audioTrack < 0) audioTrack = i;
            }
            if (videoTrack < 0 && audioTrack < 0) return null;

            int outputFormat = target == RecorderFormat.WEBM
                    ? MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
                    : MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4;
            muxer = new MediaMuxer(dst.getAbsolutePath(), outputFormat);

            int videoOut = videoTrack >= 0
                    ? muxer.addTrack(extractor.getTrackFormat(videoTrack)) : -1;
            int audioOut = audioTrack >= 0
                    ? muxer.addTrack(extractor.getTrackFormat(audioTrack)) : -1;
            muxer.start();

            ByteBuffer buffer = ByteBuffer.allocate(1 << 20);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

            for (int[] pair : new int[][]{{videoTrack, videoOut}, {audioTrack, audioOut}}) {
                int inTrack = pair[0];
                int outTrack = pair[1];
                if (inTrack < 0 || outTrack < 0) continue;
                extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                extractor.selectTrack(inTrack);
                while (true) {
                    buffer.clear();
                    int size = extractor.readSampleData(buffer, 0);
                    if (size < 0) break;
                    long time = extractor.getSampleTime();
                    if (time < 0) break;
                    info.set(0, size, time, extractor.getSampleFlags());
                    muxer.writeSampleData(outTrack, buffer, info);
                    if (!extractor.advance()) break;
                }
                extractor.unselectTrack(inTrack);
            }

            muxer.stop();
            return dst;
        } finally {
            if (muxer != null) {
                try { muxer.release(); } catch (Throwable ignored) { }
            }
            extractor.release();
        }
    }

    @Nullable
    private static File exportGif(File source) throws Exception {
        if (!source.exists()) return null;

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(source.getAbsolutePath());
        String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long durationMs = 0;
        try {
            durationMs = Long.parseLong(durationStr == null ? "0" : durationStr);
        } catch (NumberFormatException ignored) { }
        if (durationMs <= 0) durationMs = 5000;

        // GIF is intentionally small and short: 8 fps, ≤ 480 px wide, ≤ 20 s.
        final int intervalMs = 125;
        final int maxFrames = 160;
        final int targetWidth = 480;

        Bitmap first = null;
        try {
            first = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        } catch (Throwable ignored) { }
        if (first == null) {
            retriever.release();
            return null;
        }
        int srcW = first.getWidth();
        int srcH = first.getHeight();
        int outW = Math.min(targetWidth, srcW);
        int outH = Math.max(2, Math.round(srcH * (outW / (float) srcW)));
        // GIF dimensions are stored in 16-bit fields; guard against silly sizes.
        outW = Math.min(outW, 4096);
        outH = Math.min(outH, 4096);

        File dst = siblingFile(source, "gif");
        //noinspection ResultOfMethodCallIgnored
        if (dst.exists()) dst.delete();

        try (BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(dst))) {
            GifEncoder encoder = new GifEncoder(stream, outW, outH, intervalMs / 10, 0);
            Bitmap frame = first;
            int count = 0;
            for (long t = 0; t < durationMs && count < maxFrames; t += intervalMs, count++) {
                if (frame == null) break;
                Bitmap scaled = frame;
                if (scaled.getWidth() != outW || scaled.getHeight() != outH) {
                    scaled = Bitmap.createScaledBitmap(frame, outW, outH, true);
                }
                encoder.addFrame(scaled);
                if (scaled != frame) scaled.recycle();
                frame.recycle();
                frame = null;
                long next = t + intervalMs;
                if (next >= durationMs || count + 1 >= maxFrames) break;
                try {
                    frame = retriever.getFrameAtTime(next * 1000L,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                } catch (Throwable ignored) { }
            }
            encoder.finish();
        } finally {
            retriever.release();
        }
        return dst;
    }

    private static File siblingFile(File source, String extension) {
        String name = source.getName();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        return new File(source.getParentFile(), base + "." + extension);
    }
}
