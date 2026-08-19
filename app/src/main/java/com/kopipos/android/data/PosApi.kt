package com.kopipos.android.data

import retrofit2.http.*

interface PosApi {
    @POST("auth/token") suspend fun login(@Body body: TokenRequest): ApiEnvelope<LoginData>
    @POST("auth/logout") suspend fun logout(): ApiEnvelope<Any>
    @GET("products") suspend fun products(@Query("is_available") available: Int = 1): ApiEnvelope<List<Product>>
    @GET("categories") suspend fun categories(): ApiEnvelope<List<Category>>
    @GET("shifts/active") suspend fun activeShift(): ApiEnvelope<Shift?>
    @POST("orders") suspend fun checkout(@Body body: OrderRequest): ApiEnvelope<Order>
    @GET("orders") suspend fun orders(): ApiEnvelope<List<Order>>
    @GET("orders/{id}") suspend fun order(@Path("id") id: Int): ApiEnvelope<Order>
}
