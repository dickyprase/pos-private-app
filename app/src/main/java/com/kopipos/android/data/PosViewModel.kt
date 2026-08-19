package com.kopipos.android.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kopipos.android.core.storage.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

 data class PosState(
    val loggedIn: Boolean = false, val loading: Boolean = false, val error: String? = null,
    val products: List<Product> = emptyList(), val categories: List<Category> = emptyList(),
    val cart: List<CartItem> = emptyList(), val shiftOpen: Boolean = false,
    val orders: List<Order> = emptyList(), val storeName: String = "KopiPOS",
    val printerConnected: Boolean = false
) { val total get() = cart.sumOf { it.lineTotal } }

class PosViewModel(app: Application) : AndroidViewModel(app) {
    private val session = SessionStore(app)
    private var authToken: String? = null
    private val api = ApiClient { authToken }.api
    private val _state = MutableStateFlow(PosState())
    val state = _state.asStateFlow()

    init { viewModelScope.launch { session.token.collect { token -> if (token != null && authToken == null) { authToken = token; loadCatalog() } } } }

    fun login(login: String, password: String) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        try {
            val response = api.login(TokenRequest(login, password))
            if (response.success && response.data != null) {
                authToken = response.data.token; session.saveToken(authToken!!)
                _state.update { it.copy(loggedIn = true, loading = false) }; loadCatalog()
            } else fail(response.message)
        } catch (e: HttpException) { fail(if (e.code() == 422) "Username atau password salah" else "Server error ${e.code()}") }
        catch (_: IOException) { fail("Koneksi gagal. Periksa internet") }
        catch (e: Exception) { fail("Login gagal: ${e.message ?: "response tidak valid"}") }
    }

    private suspend fun loadCatalog() {
        try {
            val products = api.products().data.orEmpty(); val categories = api.categories().data.orEmpty()
            val shift = api.activeShift().data
            _state.update { it.copy(loggedIn = true, products = products, categories = categories, shiftOpen = shift != null, loading = false) }
        } catch (e: Exception) { fail("Catalog gagal dimuat") }
    }

    fun add(product: Product) = _state.update { state ->
        state.copy(cart = buildList<CartItem> {
            var found = false
            state.cart.forEach { item -> if (item.product.id == product.id) { add(item.copy(quantity = item.quantity + 1)); found = true } else add(item) }
            if (!found) add(CartItem(product, 1))
        })
    }
    fun dec(id: Int) = _state.update { state -> state.copy(cart = state.cart.flatMap { if (it.product.id != id) listOf(it) else if (it.quantity > 1) listOf(it.copy(quantity = it.quantity - 1)) else emptyList() }) }
    fun remove(id: Int) = _state.update { it.copy(cart = it.cart.filterNot { item -> item.product.id == id }) }
    fun clearCart() = _state.update { it.copy(cart = emptyList()) }

    fun checkout() = viewModelScope.launch {
        val current = _state.value; if (current.cart.isEmpty()) return@launch
        _state.update { it.copy(loading = true, error = null) }
        try {
            val response = api.checkout(OrderRequest(UUID.randomUUID().toString(), null, null, "TAKE_AWAY", 0, current.cart.map { OrderItemRequest(it.product.id, it.quantity, notes = it.note) }, PaymentRequest("CASH", current.total)))
            if (response.success) _state.update { it.copy(loading = false, cart = emptyList(), error = "Transaksi ${response.data?.order_number ?: "berhasil"}") } else fail(response.message)
        } catch (_: Exception) { fail("Checkout gagal") }
    }
    fun loadOrders() = viewModelScope.launch { try { val rows: List<Order> = api.orders().data.orEmpty(); _state.update { it.copy(orders = rows) } } catch (_: Exception) { fail("Riwayat gagal dimuat") } }
    fun logout() = viewModelScope.launch { runCatching { api.logout() }; session.clear(); authToken = null; _state.value = PosState() }
    private fun fail(message: String?) = _state.update { it.copy(loading = false, error = message ?: "Request gagal") }
}

fun rupiah(value: Int) = "Rp " + String.format("%,d", value).replace(',', '.')
fun rupiah(value: Long) = "Rp " + String.format("%,d", value).replace(',', '.')

typealias PosViewModelFactory = androidx.lifecycle.ViewModelProvider.Factory
