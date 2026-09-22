package net.kdt.pojavlaunch.recorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import net.kdt.pojavlaunch.R;

/**
 * OrbitX — screen-recording foreground service.
 *
 * <p>Android 14 (API 34) requires a running foreground service of type
 * {@code mediaProjection} before {@code MediaProjectionManager} will hand out a
 * {@link android.media.projection.MediaProjection}. This service exists purely
 * to satisfy that contract for the in-game replay/recording mod; it performs no
 * UI work of its own and shows a single silent, minimal notification.</p>
 *
 * <p>Deliberately kept free of any overlay: nothing here draws on top of the
 * game, so a recording started while playing contains no launcher chrome.</p>
 */
public class RecorderService extends Service {

    public static final String CHANNEL_ID = "orbitx_recorder";
    public static final int NOTIFICATION_ID = 0x0B17;

    public static void start(Context context) {
        Intent intent = new Intent(context, RecorderService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, RecorderService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ensureChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_orbitx_recording)
                .setContentTitle(getString(R.string.orbitx_recorder_service_title))
                .setContentText(getString(R.string.orbitx_recorder_service_text))
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        return START_NOT_STICKY;
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.orbitx_recorder_channel_name),
                NotificationManager.IMPORTANCE_MIN);
        channel.setShowBadge(false);
        channel.setSound(null, null);
        manager.createNotificationChannel(channel);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
