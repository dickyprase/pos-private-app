package com.kopipos.android.data
import retrofit2.http.*
interface PosApi { @POST("auth/token") suspend fun login(@Body b:TokenRequest):ApiEnvelope<LoginData>; @POST("auth/logout") suspend fun logout():ApiEnvelope<Any>; @GET("products") suspend fun products():ApiEnvelope<List<Product>>; @GET("categories") suspend fun categories():ApiEnvelope<List<Category>>; @GET("shifts/active") suspend fun activeShift():ApiEnvelope<Any>; @POST("orders") suspend fun checkout(@Body b:OrderRequest):ApiEnvelope<Order> }
