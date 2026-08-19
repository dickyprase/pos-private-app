package com.kopipos.android.data

import com.kopipos.android.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiClient(private val tokenProvider: () -> String?) {
    val api: PosApi = Retrofit.Builder().baseUrl(BuildConfig.BASE_URL)
        .client(OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            val request = chain.request().newBuilder().addHeader("Accept", "application/json").apply {
                tokenProvider()?.let { addHeader("Authorization", "Bearer $it") }
            }.build(); chain.proceed(request)
        }).build()).addConverterFactory(GsonConverterFactory.create()).build().create(PosApi::class.java)
}
