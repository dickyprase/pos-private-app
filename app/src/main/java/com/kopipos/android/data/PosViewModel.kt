package com.kopipos.android.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kopipos.android.BuildConfig
import com.kopipos.android.core.storage.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

enum class PosPage { CASHIER, CART, CHECKOUT, SUCCESS, ORDERS, SHIFT, MORE, PRINTER }
enum class PaymentMethod(val api: String, val label: String) { CASH("CASH", "Tunai"), QRIS("QRIS", "QRIS"), DEBIT("DEBIT_CARD", "Debit"), CREDIT("CREDIT_CARD", "Kredit"), TRANSFER("BANK_TRANSFER", "Transfer"), EWALLET("EWALLET", "E-Wallet"), OTHER("OTHER", "Lainnya") }
data class CheckoutState(val orderType: String = "TAKE_AWAY", val customerName: String = "", val tableNumber: String = "", val payment: PaymentMethod = PaymentMethod.CASH, val receivedAmount: Int = 0, val reference: String = "", val submissionToken: String = UUID.randomUUID().toString())
data class PosState(val loggedIn: Boolean = false, val loading: Boolean = false, val error: String? = null, val products: List<Product> = emptyList(), val categories: List<Category> = emptyList(), val cart: List<CartItem> = emptyList(), val shift: Shift? = null, val orders: List<Order> = emptyList(), val page: PosPage = PosPage.CASHIER, val checkout: CheckoutState? = null, val completedOrder: Order? = null, val storeName: String = "KopiPOS") { val total get() = cart.sumOf { it.lineTotal } }

class PosViewModel(app: Application) : AndroidViewModel(app) {
    private val session = SessionStore(app); private var authToken: String? = null; private val api = ApiClient { authToken }.api
    private val _state = MutableStateFlow(PosState()); val state = _state.asStateFlow()
    init { viewModelScope.launch { session.token.collect { token -> if (token != null && authToken == null) { authToken = token; _state.update { it.copy(loggedIn = true) }; loadCatalog() } } } }
    fun login(login: String, password: String) = viewModelScope.launch { _state.update { it.copy(loading = true, error = null) }; try { val r = api.login(TokenRequest(login, password)); if (r.success && r.data != null) { authToken = r.data.token; session.saveToken(authToken!!); _state.update { it.copy(loggedIn = true, loading = false) }; loadCatalog() } else fail(r.message) } catch (e: HttpException) { fail(if (e.code() == 422) "Username atau password salah" else "Server error ${e.code()}") } catch (_: IOException) { fail("Koneksi gagal. Periksa internet") } catch (e: Exception) { fail("Login gagal: ${e.message ?: "response tidak valid"}") } }
    private suspend fun loadCatalog() { try { val products = api.products().data.orEmpty().map { it.toDomain(BuildConfig.BASE_URL.removeSuffix("/api/")) }; val cats = api.categories().data.orEmpty().map { it.toDomain() }; val shift = api.activeShift().data?.toDomain(); _state.update { it.copy(products = products, categories = cats, shift = shift, loading = false) } } catch (_: Exception) { fail("Catalog gagal dimuat") } }
    fun add(p: Product) = _state.update { s -> s.copy(cart = buildList { var found = false; s.cart.forEach { item -> if (item.product.id == p.id) { add(item.copy(quantity = item.quantity + 1)); found = true } else add(item) }; if (!found) add(CartItem(p, 1)) }) }
    fun dec(id: Int) = _state.update { s -> s.copy(cart = s.cart.flatMap { if (it.product.id != id) listOf(it) else if (it.quantity > 1) listOf(it.copy(quantity = it.quantity - 1)) else emptyList() }) }
    fun remove(id: Int) = _state.update { it.copy(cart = it.cart.filterNot { item -> item.product.id == id }) }
    fun go(page: PosPage) = _state.update { it.copy(page = page, error = null) }
    fun back() = _state.update { state -> when (state.page) { PosPage.CHECKOUT -> state.copy(page = PosPage.CASHIER); PosPage.CART, PosPage.ORDERS, PosPage.SHIFT, PosPage.MORE, PosPage.PRINTER -> state.copy(page = PosPage.CASHIER); PosPage.SUCCESS -> state.copy(page = PosPage.CASHIER); else -> state } }
    fun startCheckout() = _state.update { it.copy(page = PosPage.CHECKOUT, checkout = CheckoutState()) }
    fun updateCheckout(value: CheckoutState) = _state.update { it.copy(checkout = value) }
    fun newTransaction() = _state.update { it.copy(page = PosPage.CASHIER, cart = emptyList(), checkout = null, completedOrder = null, error = null) }
    fun checkout() = viewModelScope.launch { val s = _state.value; val c = s.checkout ?: return@launch; if (s.cart.isEmpty()) return@launch; if (c.orderType == "DINE_IN" && c.tableNumber.isBlank()) { fail("Nomor meja wajib diisi untuk makan di tempat."); return@launch }; if (c.payment == PaymentMethod.CASH && c.receivedAmount < s.total) { fail("Tunai yang diterima kurang dari total pembayaran."); return@launch }; _state.update { it.copy(loading = true, error = null) }; try { val r = api.checkout(OrderRequest(c.submissionToken, if (c.orderType == "DINE_IN") c.tableNumber else "-", c.customerName.ifBlank { "Kasir Android" }, c.orderType, 0, s.cart.map { OrderItemRequest(it.product.id, it.quantity, notes = it.note) }, PaymentRequest(c.payment.api, if (c.payment == PaymentMethod.CASH) c.receivedAmount else s.total, c.reference.ifBlank { null }))); if (r.success && r.data != null) _state.update { it.copy(loading = false, page = PosPage.SUCCESS, completedOrder = r.data.toDomain(), error = null) } else fail(r.message ?: "Pembayaran gagal diproses") } catch (e: HttpException) { fail("Pembayaran gagal (${e.code()}). Periksa data transaksi.") } catch (_: IOException) { fail("Tidak dapat terhubung ke server. Periksa koneksi internet lalu coba lagi.") } catch (_: Exception) { fail("Pembayaran gagal diproses. Cart tetap tersimpan.") } }
    fun loadOrders() = viewModelScope.launch { try { _state.update { it.copy(loading = true) }; val rows = api.orders().data.orEmpty().map { it.toDomain() }; _state.update { it.copy(orders = rows, loading = false) } } catch (_: Exception) { fail("Riwayat gagal dimuat") } }
    fun logout() = viewModelScope.launch { runCatching { api.logout() }; session.clear(); authToken = null; _state.value = PosState() }
    private fun fail(message: String?) = _state.update { it.copy(loading = false, error = message ?: "Request gagal") }
}
fun rupiah(value: Int) = "Rp " + String.format("%,d", value).replace(',', '.')
fun rupiah(value: Long) = "Rp " + String.format("%,d", value).replace(',', '.')
typealias PosViewModelFactory = androidx.lifecycle.ViewModelProvider.Factory