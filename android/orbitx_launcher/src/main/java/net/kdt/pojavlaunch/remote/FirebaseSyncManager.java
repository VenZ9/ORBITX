package net.kdt.pojavlaunch.remote;

import android.app.Activity;
import android.content.Context;

/**
 * OrbitX — remote sync placeholder.
 *
 * <p>The upstream base shipped a Firebase-backed admin panel (announcements,
 * remote update checks and a sponsorship gate). OrbitX is a fully
 * self-contained launcher with no remote admin backend, so this class keeps the
 * historical call surface as inert no-ops. Nothing here performs network I/O,
 * reads a remote config, or phones home.</p>
 */
public final class FirebaseSyncManager {

    private FirebaseSyncManager() { }

    /** Called from the launcher when it resumes. Intentionally inert. */
    public static void onResume(Context ctx) { }

    /** Opened when an update notification is tapped. Intentionally inert. */
    public static void checkForUpdateFromFcm(Activity act, String version, String url, String changelog) { }

    /** Manual "check for updates" action. Intentionally inert in OrbitX. */
    public static void checkForUpdateManual(Activity act) { }

    /**
     * Local markdown dialog used for release notes / changelogs bundled with the
     * launcher. No remote content is fetched.
     */
    public static void showMarkdownDialog(Activity act, String title, String markdown, boolean fullPage) {
        if (act == null) return;
        try {
            new android.app.AlertDialog.Builder(act)
                    .setTitle(title == null ? "OrbitX" : title)
                    .setMessage(markdown == null ? "" : markdown)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        } catch (Throwable ignored) { }
    }

    /**
     * OrbitX ships every feature unconditionally — there is no remote sponsor
     * gate, so this always reports enabled.
     */
    public static boolean isSponsorshipEnabled() { return true; }
}
