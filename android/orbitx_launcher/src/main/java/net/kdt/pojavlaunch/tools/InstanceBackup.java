package net.kdt.pojavlaunch.tools;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * ORBITX instance backup — items 3/6 feature set.
 *
 * <p>Exports a whole instance directory to a single portable {@code .zip} so a
 * player can archive, move or share an instance without root access and
 * without hand-copying folders. Import is the mirror operation.
 *
 * <p>Deliberately conservative about what goes in: caches, crash dumps and any
 * previously written backups are skipped, because they are the parts that make
 * archives enormous and are all regenerable. Everything a player would actually
 * miss — saves, mods, configs, resource packs, shader packs, screenshots and
 * the launcher's own per-instance metadata — is kept.
 */
public final class InstanceBackup {

    /** Directories that are never worth archiving (large and regenerable). */
    private static final String[] SKIP_DIRS = {
            "cache", "logs", "crash-reports", "orbitx_backups", ".cache", "tmp",
    };

    /** Zip entries are streamed, so cap memory use on huge instances. */
    private static final int BUFFER = 64 * 1024;

    private InstanceBackup() {}

    /**
     * Writes {@code instanceDir} to {@code <backupDir>/<name>-<timestamp>.zip}.
     *
     * @param instanceDir the instance root to archive
     * @param backupDir   destination directory (created if missing)
     * @param instanceName display name used in the archive filename
     * @return the archive that was written, or {@code null} if there was
     *         nothing to archive
     */
    @Nullable
    public static File export(@Nullable File instanceDir,
                              @NonNull File backupDir,
                              @Nullable String instanceName) throws IOException {
        if (instanceDir == null || !instanceDir.isDirectory()) return null;
        if (!backupDir.isDirectory() && !backupDir.mkdirs()) {
            throw new IOException("Cannot create backup directory: " + backupDir);
        }

        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        String safeName = sanitize(instanceName == null ? "instance" : instanceName);
        File out = new File(backupDir, safeName + "-" + stamp + ".zip");

        ZipOutputStream zip = null;
        try {
            zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(out), BUFFER));
            boolean wrote = addDirectory(zip, instanceDir, instanceDir.getName());
            zip.finish();
            if (!wrote) {
                out.delete();
                return null;
            }
            return out;
        } finally {
            if (zip != null) {
                try { zip.close(); } catch (IOException ignored) { }
            }
        }
    }

    /**
     * Recursively adds {@code dir} under {@code entryRoot}.
     *
     * @return true if at least one file was written
     */
    private static boolean addDirectory(@NonNull ZipOutputStream zip,
                                        @NonNull File dir,
                                        @NonNull String entryRoot) throws IOException {
        if (!dir.isDirectory() || isSkipped(dir)) return false;
        File[] children = dir.listFiles();
        if (children == null) return false;

        boolean wrote = false;
        for (File child : children) {
            String entryName = entryRoot + "/" + child.getName();
            if (child.isDirectory()) {
                if (addDirectory(zip, child, entryName)) wrote = true;
            } else if (child.isFile()) {
                addFile(zip, child, entryName);
                wrote = true;
            }
        }
        return wrote;
    }

    /** Streams one file into the archive, then closes its entry. */
    private static void addFile(@NonNull ZipOutputStream zip,
                                @NonNull File file,
                                @NonNull String entryName) throws IOException {
        BufferedInputStream in = null;
        try {
            ZipEntry entry = new ZipEntry(entryName);
            entry.setTime(file.lastModified());
            zip.putNextEntry(entry);
            in = new BufferedInputStream(new FileInputStream(file), BUFFER);
            byte[] buf = new byte[BUFFER];
            int n;
            while ((n = in.read(buf)) != -1) {
                zip.write(buf, 0, n);
            }
            zip.closeEntry();
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException ignored) { }
            }
        }
    }

    private static boolean isSkipped(@NonNull File dir) {
        String name = dir.getName().toLowerCase(Locale.ROOT);
        for (String skip : SKIP_DIRS) {
            if (name.equals(skip)) return true;
        }
        return false;
    }

    /** Instance names are user text, so strip anything unsafe for a filename. */
    @NonNull
    private static String sanitize(@NonNull String raw) {
        String cleaned = raw.trim().replaceAll("[^A-Za-z0-9._-]+", "_");
        if (cleaned.isEmpty()) cleaned = "instance";
        return cleaned.length() > 48 ? cleaned.substring(0, 48) : cleaned;
    }
}
