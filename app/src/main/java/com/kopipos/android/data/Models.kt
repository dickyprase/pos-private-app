package com.kopipos.android.data

import com.google.gson.annotations.SerializedName

data class ProductDto(
    val id: Int,
    @SerializedName("category_id") val categoryId: Int,
    val name: String,
    val sku: String = "",
    @SerializedName("image_path") val imagePath: String? = null,
    @SerializedName("base_price") val basePrice: Int,
    @SerializedName("is_available") val isAvailable: Boolean = true
)
data class CategoryDto(val id: Int, val name: String, val slug: String? = null)
data class ShiftDto(val id: Int, val status: String, @SerializedName("opening_cash") val openingCash: Int = 0)
data class OrderDto(val id: Int, @SerializedName("order_number") val orderNumber: String = "", @SerializedName("grand_total") val grandTotal: Int = 0, val status: String = "", @SerializedName("created_at") val createdAt: String? = null)

data class Product(val id: Int, val name: String, val sku: String = "", val basePrice: Int = 0, val isAvailable: Boolean = true, val categoryId: Int? = null, val imageUrl: String? = null)
data class Category(val id: Int, val name: String)
data class CartItem(val product: Product, val quantity: Int, val note: String? = null) { val lineTotal get() = product.basePrice * quantity }
data class ApiEnvelope<T>(val success: Boolean = false, val data: T? = null, val message: String? = null, val errors: Any? = null)
data class TokenRequest(val login: String, val password: String, val device_name: String = "android-pos")
data class LoginData(val token: String, val token_type: String = "Bearer", val user: User)
data class User(val id: Int, val name: String, val username: String? = null, val role: String = "CASHIER")
data class Shift(val id: Int, val status: String = "OPEN", val openingCash: Int = 0)
data class OrderRequest(val submission_token: String, val table_number: String?, val customer_name: String?, val order_type: String, val discount: Int, val items: List<OrderItemRequest>, val payment: PaymentRequest)
data class OrderItemRequest(val product_id: Int, val quantity: Int, val modifier_ids: List<Int> = emptyList(), val notes: String? = null)
data class PaymentRequest(val method: String, val received_amount: Int, val reference_number: String? = null)
data class Order(val id: Int, val orderNumber: String = "", val grandTotal: Int = 0, val status: String = "", val createdAt: String? = null)

fun ProductDto.toDomain(baseUrl: String) = Product(id, name, sku, basePrice, isAvailable, categoryId, imagePath?.let { if (it.startsWith("http")) it else baseUrl.trimEnd('/') + "/storage/" + it.trimStart('/') })
fun CategoryDto.toDomain() = Category(id, name)
fun ShiftDto.toDomain() = Shift(id, status, openingCash)
fun OrderDto.toDomain() = Order(id, orderNumber, grandTotal, status, createdAt)
fun Order.toDto() = OrderDto(id, orderNumber, grandTotal, status, createdAt)
