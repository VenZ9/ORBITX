/*
 * OrbitX Launcher - native EGL bridge.
 *
 * This is the Android-side context plumbing that a Minecraft: Java Edition session
 * needs: an EGL surface owned by the app, a GLES context bound to it, and a small
 * JNI surface that the (patched) LWJGL/GLFW layer calls into instead of talking to
 * the display server directly.
 *
 * The design follows Boardwalk / PojavLauncher: the Java side creates the Android
 * SurfaceView, passes its Surface over JNI, and the native layer configures an EGL
 * context and publishes it for LWJGL's android port to use. The GLFW-on-Android
 * shim in the bundled LWJGL calls orbitx_egl_get_context() to pick up the current
 * context rather than creating its own.
 *
 * NOTE: this bridge is the app-side half only. A complete session additionally
 * requires the patched LWJGL native build, the per-arch JRE, and a GL translation
 * layer (GL4ES / ANGLE / Mesa-Zink). Those are provisioned at runtime by
 * JvmRuntimeProvisioner, not compiled here.
 */

#include <jni.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES2/gl2.h>
#include <string.h>
#include <pthread.h>

#define LOG_TAG "OrbitX-EGL"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

typedef struct {
    EGLDisplay display;
    EGLSurface surface;
    EGLContext context;
    EGLConfig  config;
    ANativeWindow *window;
    int width;
    int height;
} orbitx_egl_state_t;

static orbitx_egl_state_t g_state;
static pthread_mutex_t   g_lock = PTHREAD_MUTEX_INITIALIZER;

static const EGLint CONFIG_ATTRS[] = {
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL_SURFACE_TYPE,    EGL_WINDOW_BIT,
        EGL_RED_SIZE,   8,
        EGL_GREEN_SIZE, 8,
        EGL_BLUE_SIZE,  8,
        EGL_ALPHA_SIZE, 8,
        EGL_DEPTH_SIZE, 24,
        EGL_STENCIL_SIZE, 8,
        EGL_NONE
};

static const EGLint CONTEXT_ATTRS[] = {
        EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL_NONE
};

/* Called by the patched LWJGL GLFW-on-Android shim. */
EGLContext orbitx_egl_get_context(void) {
    return g_state.context;
}

EGLDisplay orbitx_egl_get_display(void) {
    return g_state.display;
}

int orbitx_egl_get_width(void)  { return g_state.width; }
int orbitx_egl_get_height(void) { return g_state.height; }

JNIEXPORT jboolean JNICALL
Java_com_orbitx_launcher_GameActivity_nativeEglSetup(JNIEnv *env, jclass clazz, jobject surface) {
    pthread_mutex_lock(&g_lock);

    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    if (!window) {
        LOGE("ANativeWindow_fromSurface returned null");
        pthread_mutex_unlock(&g_lock);
        return JNI_FALSE;
    }
    g_state.window = window;
    g_state.width  = ANativeWindow_getWidth(window);
    g_state.height = ANativeWindow_getHeight(window);

    g_state.display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (g_state.display == EGL_NO_DISPLAY) {
        LOGE("no EGL display");
        goto fail;
    }
    if (!eglInitialize(g_state.display, NULL, NULL)) {
        LOGE("eglInitialize failed: 0x%x", eglGetError());
        goto fail;
    }

    EGLint numConfigs = 0;
    if (!eglChooseConfig(g_state.display, CONFIG_ATTRS, &g_state.config, 1, &numConfigs) || numConfigs < 1) {
        LOGE("eglChooseConfig failed: 0x%x", eglGetError());
        goto fail;
    }

    g_state.surface = eglCreateWindowSurface(g_state.display, g_state.config, window, NULL);
    if (g_state.surface == EGL_NO_SURFACE) {
        LOGE("eglCreateWindowSurface failed: 0x%x", eglGetError());
        goto fail;
    }

    g_state.context = eglCreateContext(g_state.display, g_state.config, EGL_NO_CONTEXT, CONTEXT_ATTRS);
    if (g_state.context == EGL_NO_CONTEXT) {
        LOGE("eglCreateContext failed: 0x%x", eglGetError());
        goto fail;
    }

    if (!eglMakeCurrent(g_state.display, g_state.surface, g_state.surface, g_state.context)) {
        LOGE("eglMakeCurrent failed: 0x%x", eglGetError());
        goto fail;
    }

    LOGI("EGL ready: %dx%d GL_VERSION=%s", g_state.width, g_state.height,
         (const char *) glGetString(GL_VERSION));
    pthread_mutex_unlock(&g_lock);
    return JNI_TRUE;

fail:
    if (g_state.window) { ANativeWindow_release(g_state.window); g_state.window = NULL; }
    memset(&g_state, 0, sizeof(g_state));
    pthread_mutex_unlock(&g_lock);
    return JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_orbitx_launcher_GameActivity_nativeEglResize(JNIEnv *env, jclass clazz, jint w, jint h) {
    pthread_mutex_lock(&g_lock);
    if (g_state.window) {
        ANativeWindow_setBuffersGeometry(g_state.window, w, h, WINDOW_FORMAT_RGBA_8888);
        g_state.width  = w;
        g_state.height = h;
        LOGI("resize -> %dx%d", w, h);
    }
    pthread_mutex_unlock(&g_lock);
}

JNIEXPORT void JNICALL
Java_com_orbitx_launcher_GameActivity_nativeEglSwap(JNIEnv *env, jclass clazz) {
    if (g_state.display != EGL_NO_DISPLAY && g_state.surface != EGL_NO_SURFACE) {
        eglSwapBuffers(g_state.display, g_state.surface);
    }
}

JNIEXPORT void JNICALL
Java_com_orbitx_launcher_GameActivity_nativeEglTeardown(JNIEnv *env, jclass clazz) {
    pthread_mutex_lock(&g_lock);
    if (g_state.display != EGL_NO_DISPLAY) {
        eglMakeCurrent(g_state.display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        if (g_state.context != EGL_NO_CONTEXT) eglDestroyContext(g_state.display, g_state.context);
        if (g_state.surface != EGL_NO_SURFACE) eglDestroySurface(g_state.display, g_state.surface);
        eglTerminate(g_state.display);
    }
    if (g_state.window) ANativeWindow_release(g_state.window);
    memset(&g_state, 0, sizeof(g_state));
    pthread_mutex_unlock(&g_lock);
    LOGI("EGL torn down");
}
