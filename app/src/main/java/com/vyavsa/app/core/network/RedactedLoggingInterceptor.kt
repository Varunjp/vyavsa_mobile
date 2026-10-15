package com.vyavsa.app.core.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class RedactedLoggingInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val method = request.method

        // Redact Authorization headers
        val safeHeaders = request.headers.names().joinToString(", ") { name ->
            if (name.equals("Authorization", ignoreCase = true) ||
                name.equals("Cookie", ignoreCase = true)
            ) {
                "$name: [REDACTED]"
            } else {
                "$name: ${request.header(name)}"
            }
        }

        Log.d(TAG, "--> $method $url ($safeHeaders)")

        val startNs = System.nanoTime()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            Log.e(TAG, "<-- $method $url FAILED: ${e.message}")
            throw e
        }

        val tookMs = (System.nanoTime() - startNs) / 1e6
        Log.d(TAG, "<-- ${response.code} ${response.message} $url (${tookMs}ms)")

        return response
    }

    companion object {
        private const val TAG = "VyavsaNetwork"
    }
}
