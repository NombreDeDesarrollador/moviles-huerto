package com.example.huertolindo.data.remote

import android.content.Context
import android.content.SharedPreferences
import okhttp3.OkHttpClient
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080/api/"
    internal const val COOKIE_PREFERENCES = "huerto_backend_session"

    @Volatile
    private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val cookieJarDelegate = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val context = applicationContext
            ?: throw IllegalStateException("RetrofitClient debe inicializarse con el contexto de la aplicación.")
        SessionCookieJar(context)
    }
    private val sessionCookieJar by cookieJarDelegate

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(sessionCookieJar)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun clearSessionCookies() {
        if (cookieJarDelegate.isInitialized()) {
            sessionCookieJar.clear()
        }
    }

    val api: HuertoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HuertoApiService::class.java)
    }
}

private class SessionCookieJar(context: Context) : CookieJar {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        RetrofitClient.COOKIE_PREFERENCES,
        Context.MODE_PRIVATE
    )
    private val cookies = mutableMapOf<String, List<Cookie>>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val incomingKeys = cookies.mapTo(mutableSetOf(), ::cookieKey)
        val existingCookies = (this.cookies[url.host].orEmpty() + loadPersistedCookies(url))
            .distinctBy(::cookieKey)
            .filterNot { cookieKey(it) in incomingKeys }
        val validCookies = cookies.filter { it.expiresAt >= System.currentTimeMillis() }
        storeCookies(url.host, existingCookies + validCookies)
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val validCookies = (cookies[url.host].orEmpty() + loadPersistedCookies(url))
            .distinctBy(::cookieKey)
            .filter { it.expiresAt >= System.currentTimeMillis() && it.matches(url) }
        storeCookies(url.host, validCookies)
        return validCookies
    }

    @Synchronized
    fun clear() {
        cookies.clear()
        preferences.edit().clear().apply()
    }

    private fun loadPersistedCookies(url: HttpUrl): List<Cookie> =
        preferences.getStringSet(url.host, emptySet())
            .orEmpty()
            .mapNotNull { Cookie.parse(url, it) }

    private fun storeCookies(host: String, cookies: List<Cookie>) {
        this.cookies[host] = cookies
        if (cookies.isEmpty()) {
            preferences.edit().remove(host).apply()
        } else {
            preferences.edit()
                .putStringSet(host, cookies.mapTo(mutableSetOf()) { it.toString() })
                .apply()
        }
    }

    private fun cookieKey(cookie: Cookie): String =
        "${cookie.name}|${cookie.domain}|${cookie.path}"
}
