package net.kdt.pojavlaunch.remote;

import android.app.Activity;
import android.content.Context;

/**
 * OrbitX — push-messaging placeholder.
 *
 * <p>The upstream base wired a Firebase Cloud Messaging service for a remote
 * admin panel. OrbitX has no push backend and no push registration, so this
 * class exists only to keep the historical call surface compiling as inert
 * no-ops.</p>
 */
public final class CsFirebaseMessagingService {

    public static final String CHANNEL_UPDATES_ID = "orbitx_updates";
    public static final String CHANNEL_ANNOUNCEMENTS_ID = "orbitx_announcements";

    private CsFirebaseMessagingService() { }

    /** Intentionally inert — OrbitX creates no remote-notification channels. */
    public static void createNotificationChannels(Context context) { }

    /** Intentionally inert — no push registration is performed. */
    public static void initFcm(Context context) { }

    /** Intentionally inert — there is no FCM token to display. */
    public static void showFcmTokenDebugDialog(Activity act) { }
}
