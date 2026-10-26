package com.androidtech.base.extension

import com.androidtech.base.BuildConfig
import com.androidtech.base.logger.Logger
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.IOException
import kotlin.jvm.Throws

class LoggingInterceptor: Interceptor {

    private val defaultErrorCode = 999

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        Logger.d("Request url: ${originalRequest.url}")

        // Create new request with header
        val modifierRequest = originalRequest.newBuilder()
//            .addHeader("Authorization", "Bearer ${BuildConfig.TOKEN}")
            .build()

        try {
            val response = chain.proceed(modifierRequest)
            val responseBody = response.body
            val bodyContent = responseBody?.string()
            Logger.d("Response body: $bodyContent")

            // Rebuild the response before returning because body can be read only once
            val mediaType = responseBody?.contentType()
            val newResponseBody = bodyContent?.toResponseBody(mediaType)

            return response.newBuilder().body(newResponseBody).build()
        } catch (e: IOException) {
            e.printStackTrace()

            val errorMessage = when (e) {
                else -> e.message ?: "Error occurred"
            }

            return Response.Builder()
                .request(modifierRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(defaultErrorCode)
                .message(errorMessage)
                .body("$e".toResponseBody("text/plain".toMediaTypeOrNull()))
                .build()
        }
    }
}