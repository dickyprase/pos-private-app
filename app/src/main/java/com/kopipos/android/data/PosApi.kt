package com.kopipos.android.data

import retrofit2.http.*

interface PosApi {
    @POST("auth/token") suspend fun login(@Body body: TokenRequest): ApiEnvelope<LoginData>
    @POST("auth/logout") suspend fun logout(): ApiEnvelope<Any>
    @GET("products") suspend fun products(@Query("is_available") available: Int = 1): ApiEnvelope<List<ProductDto>>
    @GET("categories") suspend fun categories(): ApiEnvelope<List<CategoryDto>>
    @GET("shifts/active") suspend fun activeShift(): ApiEnvelope<ShiftDto?>
    @POST("orders") suspend fun checkout(@Body body: OrderRequest): ApiEnvelope<OrderDto>
    @GET("orders") suspend fun orders(): ApiEnvelope<List<OrderDto>>
    @GET("orders/{id}") suspend fun order(@Path("id") id: Int): ApiEnvelope<OrderDto>
    @Streaming @GET("orders/{id}/receipt/raw") suspend fun receiptRaw(@Path("id") id: Int): okhttp3.ResponseBody
}
