package net.kdt.pojavlaunch.recorder;

import android.media.MediaRecorder;
import android.os.Build;

/**
 * OrbitX — the containers the in-game recorder can write and export.
 *
 * <p>Every option maps onto codecs the Android platform ships in
 * {@code MediaRecorder} / {@code MediaMuxer}, so no third-party encoder and no
 * ffmpeg binary is required for the plain recording path.</p>
 */
public enum RecorderFormat {

    /** H.264 + AAC in an MP4 container — the default, universally playable. */
    MP4("mp4", "video/mp4", MediaRecorder.OutputFormat.MPEG_4,
            MediaRecorder.VideoEncoder.H264, MediaRecorder.AudioEncoder.AAC,
            "video/avc", "audio/mp4a-latm"),

    /** VP8 + Vorbis in a WebM container — smaller files, open format. */
    WEBM("webm", "video/webm", MediaRecorder.OutputFormat.WEBM,
            MediaRecorder.VideoEncoder.VP8, MediaRecorder.AudioEncoder.VORBIS,
            "video/x-vnd.on2.vp8", "audio/vorbis");

    public final String extension;
    public final String mimeType;
    public final int outputFormat;
    public final int videoEncoder;
    public final int audioEncoder;
    /** MIME reported by MediaExtractor when re-muxing, used by the exporter. */
    public final String videoMime;
    public final String audioMime;

    RecorderFormat(String extension, String mimeType, int outputFormat,
                   int videoEncoder, int audioEncoder,
                   String videoMime, String audioMime) {
        this.extension = extension;
        this.mimeType = mimeType;
        this.outputFormat = outputFormat;
        this.videoEncoder = videoEncoder;
        this.audioEncoder = audioEncoder;
        this.videoMime = videoMime;
        this.audioMime = audioMime;
    }

    /** A human label for the format picker, e.g. {@code "MP4 (H.264)"}. */
    public String label() {
        switch (this) {
            case MP4:  return "MP4 (H.264 + AAC)";
            case WEBM: return "WebM (VP8 + Vorbis)";
            default:   return name();
        }
    }

    public static RecorderFormat fromOrdinal(int ordinal) {
        RecorderFormat[] values = values();
        if (ordinal < 0 || ordinal >= values.length) return MP4;
        return values[ordinal];
    }

    /** VP9 needs API 24; we standardise on VP8 so every supported device works. */
    public boolean isSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }
}
