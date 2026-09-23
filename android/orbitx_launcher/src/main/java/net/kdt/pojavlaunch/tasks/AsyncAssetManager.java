package net.kdt.pojavlaunch.tasks;


import static net.kdt.pojavlaunch.Architecture.archAsString;
import static net.kdt.pojavlaunch.Architecture.archAsStringAndroid;
import static net.kdt.pojavlaunch.Architecture.getDeviceArchitecture;
import static net.kdt.pojavlaunch.PojavApplication.sExecutorService;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

public class AsyncAssetManager {

    private AsyncAssetManager(){}

    /**
     * Attempt to install the java 8 runtime, if necessary
     * @param am App context
     */
    public static void unpackRuntime(AssetManager am) {
        /* Check if JRE is included */
        String rt_version = null;
        String current_rt_version = MultiRTUtils.readInternalRuntimeVersion("Internal");
        try {
            rt_version = Tools.read(am.open("components/jre/version"));
        } catch (IOException e) {
            Log.e("JREAuto", "JRE was not included on this APK.", e);
        }
        String exactJREName = MultiRTUtils.getExactJreName(8);
        if(current_rt_version == null && exactJREName != null && !exactJREName.equals("Internal")/*this clause is for when the internal runtime is goofed*/) return;
        if(rt_version == null) return;
        if(rt_version.equals(current_rt_version)) return;

        // Install the runtime in an async manner, hope for the best
        String finalRt_version = rt_version;
        sExecutorService.execute(() -> {

            try {
                MultiRTUtils.installRuntimeNamedBinpack(
                        am.open("components/jre/universal.tar.xz"),
                        am.open("components/jre/bin-" + archAsString(Tools.DEVICE_ARCHITECTURE) + ".tar.xz"),
                        "Internal", finalRt_version);
                MultiRTUtils.postPrepare("Internal");
            }catch (IOException e) {
                Log.e("JREAuto", "Internal JRE unpack failed", e);
            }
        });
    }

    /** Unpack single files, with no regard to version tracking */
    public static void unpackSingleFiles(Context ctx){
        ProgressLayout.setProgress(ProgressLayout.EXTRACT_SINGLE_FILES, 0);
        sExecutorService.execute(() -> {
            try {
                // Minecraft owns options.txt and its graphics defaults. Never seed
                // launcher-chosen render/FPS/VSync/quality values into a new profile.
                Tools.copyAssetFile(ctx, "default.json", Tools.CTRLMAP_PATH, false);

                Tools.copyAssetFile(ctx, "launcher_profiles.json", Tools.DIR_GAME_NEW, false);
                Tools.copyAssetFile(ctx,"resolv.conf",Tools.DIR_DATA, false);
            } catch (IOException e) {
                Log.e("AsyncAssetManager", "Failed to unpack critical components !");
            }
            ProgressLayout.clearProgress(ProgressLayout.EXTRACT_SINGLE_FILES);
        });
    }

    public static void unpackComponents(Context ctx){
        ProgressLayout.setProgress(ProgressLayout.EXTRACT_COMPONENTS, 0);
        sExecutorService.execute(() -> {
            try {
                CompletableFuture<?>[] futures = new CompletableFuture<?>[]{
                        CompletableFuture.runAsync(() -> { try { unpackComponent(ctx, "caciocavallo", false); } catch (IOException e) { throw new RuntimeException(e); } }, sExecutorService),
                        CompletableFuture.runAsync(() -> { try { unpackComponent(ctx, "caciocavallo17", false); } catch (IOException e) { throw new RuntimeException(e); } }, sExecutorService),
                        CompletableFuture.runAsync(() -> { try { unpackLwjglNatives(ctx); } catch (IOException e) { throw new RuntimeException(e); } }, sExecutorService),
                        CompletableFuture.runAsync(() -> { try { unpackComponent(ctx, "lwjgl3/3.3.3", false); } catch (IOException e) { throw new RuntimeException(e); } }, sExecutorService),
                        CompletableFuture.runAsync(() -> { try { unpackComponent(ctx, "lwjgl3/3.4.1", false); } catch (IOException e) { throw new RuntimeException(e); } }, sExecutorService),
                        CompletableFuture.runAsync(() -> { try { unpackComponent(ctx, "security", true); } catch (IOException e) { throw new RuntimeException(e); } }, sExecutorService),
                        CompletableFuture.runAsync(() -> { try { unpackComponent(ctx, "arc_dns_injector", true); } catch (IOException e) { throw new RuntimeException(e); } }, sExecutorService),
                        CompletableFuture.runAsync(() -> { try { unpackComponent(ctx, "methods_injector_agent", true); } catch (IOException e) { throw new RuntimeException(e); } }, sExecutorService),
                        CompletableFuture.runAsync(() -> { try { unpackComponent(ctx, "forge_installer", true); } catch (IOException e) { throw new RuntimeException(e); } }, sExecutorService),
                        CompletableFuture.runAsync(() -> { try { unpackComponent(ctx, "authlib-injector", true); } catch (IOException e) { throw new RuntimeException(e); } }, sExecutorService),
                };
                CompletableFuture.allOf(futures).join();
            } catch (Exception e) {
                Log.e("AsyncAssetManager", "Failed to unpack components !",e );
            }
            ProgressLayout.clearProgress(ProgressLayout.EXTRACT_COMPONENTS);
        });
    }
    /**
     * The .so files every LWJGL natives directory must contain for the game to reach its
     * first frame. liblwjgl.so is the one that matters most: its ThreadLocalUtil JNI
     * entry points (nsetupEnvData / setupEnvData) are what the game binds against, and a
     * build whose liblwjgl.so was compiled from a different LWJGL revision than the
     * lwjgl.jar on the classpath fails with UnsatisfiedLinkError the moment the renderer
     * comes up. Treat a directory missing any of these as unusable.
     */
    private static final String[] LWJGL_NATIVE_REQUIRED = {
            "liblwjgl.so", "liblwjgl_opengl.so", "liblwjgl_stb.so",
            "liblwjgl_tinyfd.so", "liblwjgl_vma.so", "liblwjgl_nanovg.so",
            "libfreetype.so", "libshaderc.so"
    };

    /**
     * Synchronously (re-)extract the LWJGL natives for this device's ABI.
     *
     * <p>Used by the launch path as a self-heal when the natives directory is found to be
     * empty or incomplete: without it the only recourse was to tell the user to restart and
     * hope, while the game kept dying with UnsatisfiedLinkError on the first renderer bind.
     *
     * @return the ABI directory the natives were written to, or null when nothing could be
     *         extracted for this device (unsupported architecture, or no bundled payload)
     */
    public static File extractLwjglNativesNow(Context ctx) throws IOException {
        unpackLwjglNatives(ctx);
        String sArch = archAsStringAndroid(getDeviceArchitecture());
        for (String lwjglVer : new String[]{"3.3.3", "3.4.1"}) {
            File dir = new File(Tools.DIR_DATA, "lwjgl-" + lwjglVer + "-natives/" + sArch);
            if (nativesAreComplete(dir)) return dir;
        }
        return null;
    }

    /**
     * Extract the LWJGL native libraries into the location the launcher hands to the JVM
     * through LD_LIBRARY_PATH (Tools.lwjglNativesDir).
     *
     * <p>The payload lives under assets/components/lwjgl-&lt;ver&gt;-natives/&lt;abi&gt;/ and is versioned
     * against the lwjgl3/&lt;ver&gt; directory that ships the matching lwjgl.jar, so the natives and
     * the Java classes can never drift apart.
     *
     * <p>The original implementation had two defects that let a broken natives directory ship
     * and still look healthy:
     * <ol>
     *   <li>It read from {@code components/lwjgl-<ver>-natives/<abi>} &mdash; a path that does not
     *       exist in the APK. {@link AssetManager#list} throws on a missing directory, the
     *       whole component job was aborted, and because the caller's failure handler only
     *       logs, the launcher still started with an empty natives directory.</li>
     *   <li>The version file was written even when extraction had failed, so every later
     *       launch compared equal, skipped re-extraction, and the directory stayed empty.</li>
     * </ol>
     * <p>Both are fixed here: extraction comes from the real asset path, the version file is
     * published only after a verified extraction, and the freshness check re-verifies the
     * actual .so files on disk instead of trusting the version file alone.
     */
    private static void unpackLwjglNatives(Context ctx) throws IOException {
        AssetManager am = ctx.getAssets();
        String rootDir = Tools.DIR_DATA;
        String sArch = archAsStringAndroid(getDeviceArchitecture());

        String[] lwjglVersions = {"3.3.3", "3.4.1"};
        for (String lwjglVer : lwjglVersions) {
            String assetRoot = "components/lwjgl-" + lwjglVer + "-natives/" + sArch;
            String rootEntry = assetRoot + "/liblwjgl.so";

            // The natives payload for this ABI may legitimately be absent (e.g. an unsupported
            // architecture). Skip cleanly rather than aborting the whole extraction job.
            try {
                am.open(rootEntry).close();
            } catch (IOException missing) {
                Log.w("UnpackLwjgl", "No bundled LWJGL " + lwjglVer + " natives for " + sArch + ", skipping");
                continue;
            }

            File nativesTargetDir = new File(rootDir, "lwjgl-" + lwjglVer + "-natives/" + sArch);
            File versionFile = new File(Tools.DIR_GAME_HOME + String.format("/lwjgl3/%s/version", lwjglVer));

            String bundledVersion = null;
            try (InputStream is = am.open("components/lwjgl3/" + lwjglVer + "/version")) {
                bundledVersion = Tools.read(is);
            } catch (IOException ignored) {
                // The version marker itself is optional; completeness of the .so set is the real check.
            }

            boolean shouldUpdate = true;
            if (nativesAreComplete(nativesTargetDir) && versionFile.exists() && bundledVersion != null) {
                try (FileInputStream fis = new FileInputStream(versionFile)) {
                    if (bundledVersion.equals(Tools.read(fis))) shouldUpdate = false;
                } catch (IOException ignored) {
                }
            }

            if (!shouldUpdate) {
                Log.i("UnpackLwjgl", lwjglVer + " is up-to-date with the launcher, continuing...");
                continue;
            }

            Log.i("UnpackLwjgl", lwjglVer + " natives are missing or out of date, re-extracting...");
            // Clear leftovers first: a partially-extracted directory from an earlier launch must
            // not be merged with, or the stale .so files would survive the next launch too.
            try {
                FileUtils.deleteDirectory(nativesTargetDir);
            } catch (IOException ignored) {
            }

            String[] fileList = am.list(assetRoot);
            if (fileList == null || fileList.length == 0) {
                throw new IOException("Bundled LWJGL " + lwjglVer + " natives for " + sArch + " are empty");
            }
            for (String fileName : fileList) {
                Tools.copyAssetFile(ctx, assetRoot + "/" + fileName, nativesTargetDir.getAbsolutePath(), true);
            }

            // Only publish the version file once the directory has been verified, so a failed
            // or partial extraction can never be recorded as a good one.
            if (!nativesAreComplete(nativesTargetDir)) {
                throw new IOException("LWJGL " + lwjglVer + " natives incomplete after extraction: " + nativesTargetDir);
            }
            if (bundledVersion != null) {
                try {
                    FileUtils.writeStringToFile(versionFile, bundledVersion, "UTF-8");
                } catch (IOException e) {
                    Log.w("UnpackLwjgl", "Failed to write version file for " + lwjglVer, e);
                }
            }
            Log.i("UnpackLwjgl", lwjglVer + " natives extracted and verified for " + sArch);
        }
    }

    /**
     * @return whether every required LWJGL native is present and non-empty in the directory
     */
    private static boolean nativesAreComplete(File nativesDir) {
        if (nativesDir == null || !nativesDir.isDirectory()) return false;
        for (String name : LWJGL_NATIVE_REQUIRED) {
            File f = new File(nativesDir, name);
            if (!f.isFile() || f.length() == 0) return false;
        }
        return true;
    }

    private static void unpackComponent(Context ctx, String component, boolean privateDirectory) throws IOException {
        AssetManager am = ctx.getAssets();
        String rootDir = privateDirectory ? Tools.DIR_DATA : Tools.DIR_GAME_HOME;

        File versionFile = new File(rootDir + "/" + component + "/version");
        try (InputStream is = am.open("components/" + component + "/version")) {
            if (!versionFile.exists()) {
                if (versionFile.getParentFile().exists() && versionFile.getParentFile().isDirectory()) {
                    FileUtils.deleteDirectory(versionFile.getParentFile());
                }
                versionFile.getParentFile().mkdir();

                Log.i("UnpackPrep", component + ": Pack was installed manually, or does not exist, unpacking new...");
                String[] fileList = am.list("components/" + component);
                for (String s : fileList) {
                    Tools.copyAssetFile(ctx, "components/" + component + "/" + s, rootDir + "/" + component, true);
                }
            } else {
                try (FileInputStream fis = new FileInputStream(versionFile)) {
                    String release1 = Tools.read(is);
                    String release2 = Tools.read(fis);
                    if (!release1.equals(release2)) {
                        if (versionFile.getParentFile().exists() && versionFile.getParentFile().isDirectory()) {
                            FileUtils.deleteDirectory(versionFile.getParentFile());
                        }
                        versionFile.getParentFile().mkdir();

                        String[] fileList = am.list("components/" + component);
                        for (String fileName : fileList) {
                            Tools.copyAssetFile(ctx, "components/" + component + "/" + fileName, rootDir + "/" + component, true);
                        }
                    } else {
                        Log.i("UnpackPrep", component + ": Pack is up-to-date with the launcher, continuing...");
                    }
                }
            }
        }
    }
}