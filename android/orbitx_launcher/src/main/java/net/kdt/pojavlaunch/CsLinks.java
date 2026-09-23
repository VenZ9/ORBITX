package net.kdt.pojavlaunch;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;

/**
 * ORBITX — the launcher's outbound web links, in ONE place.
 *
 * <p>Community channels (Discord, GitHub, YouTube) were removed from the app
 * UI, so no channel constants live here any more. What remains is the single
 * helper used for any link the launcher still needs to open externally (for
 * example a resource page reached from the Modrinth browser).
 */
public final class CsLinks {

    private CsLinks() {}

    /** Opens an http(s) URL in whatever app can handle it, never throwing. */
    public static void open(@NonNull Context ctx, @NonNull String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(ctx, "No app can open this link", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(ctx, "Could not open link", Toast.LENGTH_SHORT).show();
        }
    }
}
