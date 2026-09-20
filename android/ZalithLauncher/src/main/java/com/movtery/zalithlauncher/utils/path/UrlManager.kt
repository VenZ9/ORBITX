package com.movtery.zalithlauncher.utils.path

import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.InfoDistributor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.util.concurrent.TimeUnit

class UrlManager {
    companion object {
        private const val URL_USER_AGENT: String = "${InfoDistributor.LAUNCHER_NAME}/${BuildConfig.VERSION_NAME}"
        @JvmField
        val TIME_OUT = Pair(8000, TimeUnit.MILLISECONDS)
        /**
         * Base for the optional update / notice feeds.  OrbitX has no separate info
         * repository, so this points at OrbitX's own repository; those requests simply
         * 404 until such feeds are published, and the callers already treat a failure as
         * "nothing new".  Redirecting it away from the upstream info repository is what
         * guarantees the launcher can never offer to download the upstream app.
         */
        const val URL_GITHUB_HOME: String = "https://api.github.com/repos/VenZ9/ORBITX/contents/"
        const val URL_MCMOD: String = "https://www.mcmod.cn/"
        const val URL_MINECRAFT: String = "https://www.minecraft.net/"
        const val URL_MINECRAFT_VERSION_REPOS: String = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
        const val URL_SUPPORT: String = "https://github.com/VenZ9/ORBITX"
        const val URL_HOME: String = "https://github.com/VenZ9/ORBITX"
        const val URL_LICENSE: String = "https://www.gnu.org/licenses/gpl-3.0.html"
        const val URL_FCL_RENDERER_PLUGIN: String = "https://github.com/ShirosakiMio/FCLRendererPlugin/releases/tag/Renderer"
        const val URL_FCL_DRIVER_PLUGIN: String = "https://github.com/FCL-Team/FCLDriverPlugin/releases/tag/Turnip"

        @JvmStatic
        fun createConnection(url: URL): URLConnection {
            val connection = url.openConnection()
            connection.setRequestProperty("User-Agent", URL_USER_AGENT)
            connection.setConnectTimeout(TIME_OUT.first)
            connection.setReadTimeout(TIME_OUT.first)

            return connection
        }

        @JvmStatic
        @Throws(IOException::class)
        fun createHttpConnection(url: URL): HttpURLConnection {
            return createConnection(url) as HttpURLConnection
        }

        @JvmStatic
        fun createRequestBuilder(url: String): Request.Builder {
            return createRequestBuilder(url, null)
        }

        @JvmStatic
        fun createRequestBuilder(url: String, body: RequestBody?): Request.Builder {
            val request = Request.Builder().url(url).header("User-Agent", URL_USER_AGENT)
            body?.let{ request.post(it) }
            return request
        }

        @JvmStatic
        fun createOkHttpClient(): OkHttpClient = createOkHttpClientBuilder().build()

        /**
         * 创建一个OkHttpClient，可自定义一些内容
         */
        @JvmStatic
        fun createOkHttpClientBuilder(action: (OkHttpClient.Builder) -> Unit = { }): OkHttpClient.Builder {
            return OkHttpClient.Builder()
                .callTimeout(TIME_OUT.first.toLong(), TIME_OUT.second)
                .apply(action)
        }
    }
}