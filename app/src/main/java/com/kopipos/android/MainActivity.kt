package com.kopipos.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil3.compose.AsyncImage
import com.kopipos.android.data.*
import com.kopipos.android.core.bluetooth.BluetoothPrinterManager
import com.kopipos.android.ui.theme.KopiTheme

private val Orange = Color(0xFFE56B2F)
private val OrangeSoft = Color(0xFFFFF0E7)
private val Ink = Color(0xFF1D232A)
private val Muted = Color(0xFF667085)
private val Green = Color(0xFF1F8A5B)

class MainActivity : ComponentActivity() {
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen(); super.onCreate(savedInstanceState)
        setContent { KopiTheme { PosApp(permissions) } }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PosApp(permissions: ActivityResultLauncher<Array<String>>) {
    val vm: PosViewModel = viewModel()
    val state by vm.state.collectAsState()
    if (state.loggedIn) BackHandler { if (state.page != PosPage.CASHIER) vm.back() }
    if (!state.loggedIn) LoginScreen(state, vm) else AnimatedContent(targetState = state.page, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "page-transition") { page -> when (page) {
        PosPage.CASHIER -> CashierScreen(state, vm, permissions)
        PosPage.CART -> CartPage(state, vm)
        PosPage.CHECKOUT -> CheckoutScreen(state, vm)
        PosPage.SUCCESS -> SuccessScreen(state, vm)
        PosPage.ORDERS -> OrdersScreen(state, vm)
        PosPage.SHIFT -> ShiftScreen(state, vm)
        PosPage.MORE -> MoreScreen(state, vm)
        PosPage.PRINTER -> PrinterScreen(state, vm, permissions)
    } }
}

@Composable
private fun LoginScreen(state: PosState, vm: PosViewModel) {
    var login by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = Color(0xFFF7F8FA)) {
        Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
            Text("☕", fontSize = 48.sp, color = Orange)
            Text("KopiPOS", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Orange)
            Text("Masuk ke akun kasir", color = Muted, modifier = Modifier.padding(top = 4.dp, bottom = 28.dp))
            OutlinedTextField(login, { login = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Email atau username") }, leadingIcon = { Icon(Icons.Default.Person, null) }, shape = RoundedCornerShape(16.dp), singleLine = true)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(password, { password = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Password") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, trailingIcon = { IconButton({ visible = !visible }) { Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Tampilkan password") } }, visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), shape = RoundedCornerShape(16.dp), singleLine = true)
            Row(Modifier.fillMaxWidth().padding(vertical = 18.dp).clip(RoundedCornerShape(14.dp)).background(Color.White).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudDone, null, tint = Green); Column(Modifier.padding(start = 10.dp)) { Text("Server terhubung", fontWeight = FontWeight.Bold); Text("pos.zorroserver.net", color = Muted, fontSize = 12.sp) }; Spacer(Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, null, tint = Muted)
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp)) }
            Button(onClick = { vm.login(login, password) }, enabled = !state.loading && login.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Orange)) { if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp) else Text("MASUK", fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CashierScreen(state: PosState, vm: PosViewModel, permissions: ActivityResultLauncher<Array<String>>) {
    var query by remember { mutableStateOf("") }; var selectedCategory by remember { mutableStateOf<Int?>(null) }; var cartOpen by remember { mutableStateOf(false) }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val products = state.products.filter { (query.isBlank() || it.name.contains(query, true) || it.sku.contains(query, true)) && (selectedCategory == null || it.categoryId == selectedCategory) }
    Scaffold(containerColor = Color(0xFFF7F8FA), topBar = {
        TopAppBar(title = { Text("KopiPOS", fontWeight = FontWeight.Black, color = Color.White) }, actions = { IconButton({ vm.go(PosPage.MORE) }) { Icon(Icons.Default.MoreHoriz, "Lainnya", tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Orange))
    }, bottomBar = { Column { if (state.cart.isNotEmpty()) CartDock(state, onClick = { cartOpen = true }); BottomNav(state, vm) } }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusDot(if (state.shift != null) Green else Muted); Text(if (state.shift != null) "Shift aktif" else "Shift belum dibuka", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp)); Spacer(Modifier.weight(1f)); Icon(Icons.Default.Print, null, tint = Muted, modifier = Modifier.size(18.dp)); Text("Printer belum dipilih", fontSize = 12.sp, color = Muted, modifier = Modifier.padding(start = 5.dp))
            }
            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth().padding(14.dp), placeholder = { Text("Cari menu / SKU") }, leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { Icon(Icons.Default.Tune, null, tint = Muted) }, shape = RoundedCornerShape(16.dp), singleLine = true)
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryChip("Semua", selectedCategory == null) { selectedCategory = null }
                state.categories.forEach { category -> CategoryChip(category.name, selectedCategory == category.id) { selectedCategory = category.id } }
            }
            if (state.shift == null) Text("Buka shift dulu sebelum transaksi", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            LazyVerticalGrid(columns = GridCells.Adaptive(if (landscape) 190.dp else 150.dp), contentPadding = PaddingValues(if (landscape) 10.dp else 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                items(products, key = { it.id }) { product -> ProductCard(product) { if (state.shift != null) vm.add(product) } }
            }
        }
    }
    if (cartOpen) ModalBottomSheet(onDismissRequest = { cartOpen = false }, skipPartiallyExpanded = true, containerColor = Color.White, dragHandle = { BottomSheetDefaults.DragHandle() }) { CartSheet(state, vm) }
}

@Composable private fun StatusDot(color: Color, modifier: Modifier = Modifier) { Box(modifier.size(8.dp).clip(RoundedCornerShape(50)).background(color)) }
@Composable private fun CategoryChip(label: String, active: Boolean, onClick: () -> Unit) { Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = if (active) Orange else Color.White, border = if (active) null else ButtonDefaults.outlinedButtonBorder, modifier = Modifier.height(40.dp)) { Text(label, color = if (active) Color.White else Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) } }

@Composable private fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(10.dp)) {
            if (product.imageUrl != null) AsyncImage(product.imageUrl, product.name, Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop) else Box(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(14.dp)).background(OrangeSoft), contentAlignment = Alignment.Center) { Text("☕", fontSize = 40.sp) }
            Text(product.name, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 10.dp)); Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) { Text(rupiah(product.basePrice), color = Orange, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Surface(shape = RoundedCornerShape(10.dp), color = Orange, onClick = onClick) { Icon(Icons.Default.Add, "Tambah", tint = Color.White, modifier = Modifier.padding(6.dp).size(18.dp)) } }
        }
    }
}

@Composable private fun CartDock(state: PosState, onClick: () -> Unit) { Surface(onClick = onClick, color = Orange, shape = RoundedCornerShape(18.dp), modifier = Modifier.padding(12.dp).fillMaxWidth()) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.ShoppingCart, null, tint = Color.White); Column(Modifier.padding(start = 10.dp)) { Text("${state.cart.sumOf { it.quantity }} item", color = Color.White, fontWeight = FontWeight.Bold); Text(rupiah(state.total), color = Color.White, fontSize = 13.sp) }; Spacer(Modifier.weight(1f)); Text("LIHAT & BAYAR", color = Color.White, fontWeight = FontWeight.Black) } } }

@Composable private fun CartSheet(state: PosState, vm: PosViewModel, modifier: Modifier = Modifier) { Column(modifier.fillMaxWidth().padding(20.dp)) { Text("Keranjang", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); LazyColumn(Modifier.weight(1f, false)) { items(state.cart.size) { index -> val item = state.cart[index]; Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(item.product.name, fontWeight = FontWeight.Bold); Text(rupiah(item.lineTotal), color = Orange) }; IconButton({ vm.dec(item.product.id) }) { Icon(Icons.Default.Remove, "Kurangi") }; Text(item.quantity.toString(), fontWeight = FontWeight.Bold); IconButton({ vm.add(item.product) }) { Icon(Icons.Default.Add, "Tambah") }; IconButton({ vm.remove(item.product.id) }) { Icon(Icons.Default.DeleteOutline, "Hapus") } } } }; Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total", fontWeight = FontWeight.Black); Text(rupiah(state.total), color = Orange, fontWeight = FontWeight.Black) }; Button({ vm.startCheckout() }, enabled = state.cart.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Orange), shape = RoundedCornerShape(16.dp)) { Text("LANJUT BAYAR", fontWeight = FontWeight.Black) }; state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) } } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun AppHeader(title: String, onBack: (() -> Unit)? = null) { TopAppBar(title = { Text(title, fontWeight = FontWeight.Black) }, navigationIcon = { if (onBack != null) IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Kembali") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Orange, titleContentColor = Color.White, navigationIconContentColor = Color.White)) }
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun BottomNav(state: PosState, vm: PosViewModel) { NavigationBar { listOf(PosPage.CASHIER to "Kasir", PosPage.ORDERS to "Riwayat", PosPage.SHIFT to "Shift", PosPage.MORE to "Lainnya").forEach { (page, label) -> NavigationBarItem(selected = state.page == page, onClick = { if (page == PosPage.ORDERS) vm.loadOrders(); vm.go(page) }, icon = { Icon(Icons.Default.Storefront, null) }, label = { Text(label) }) } } }

@Composable private fun CartPage(state: PosState, vm: PosViewModel) { Scaffold(topBar = { AppHeader("Keranjang") }, bottomBar = { BottomNav(state, vm) }) { p -> CartSheet(state, vm, Modifier.padding(p)) } }
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun CheckoutScreen(state: PosState, vm: PosViewModel) { val c = state.checkout ?: CheckoutState(); Scaffold(topBar = { AppHeader("Checkout") }, bottomBar = { Column(Modifier.navigationBarsPadding().padding(16.dp)) { Button(onClick = { vm.checkout() }, enabled = !state.loading, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Orange)) { Text(if (state.loading) "MEMPROSES..." else "PROSES PEMBAYARAN", fontWeight = FontWeight.Black) } } }) { p -> LazyColumn(Modifier.padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Text("Tipe Pesanan", fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("DINE_IN" to "Makan di tempat", "TAKE_AWAY" to "Bawa pulang").forEach { (value, label) -> FilterChip(selected = c.orderType == value, onClick = { vm.updateCheckout(c.copy(orderType = value)) }, label = { Text(label) }) } } }; item { OutlinedTextField(c.customerName, { vm.updateCheckout(c.copy(customerName = it)) }, modifier = Modifier.fillMaxWidth(), label = { Text("Nama Pelanggan (optional)") }) }; if (c.orderType == "DINE_IN") item { OutlinedTextField(c.tableNumber, { vm.updateCheckout(c.copy(tableNumber = it)) }, modifier = Modifier.fillMaxWidth(), label = { Text("Nomor Meja") }, singleLine = true) }; item { Text("Metode Pembayaran", fontWeight = FontWeight.Bold); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PaymentMethod.values().forEach { method -> FilterChip(selected = c.payment == method, onClick = { vm.updateCheckout(c.copy(payment = method)) }, label = { Text(method.label) }) } } }; item { Text("Total Pembayaran", color = Muted); Text(rupiah(state.total), color = Orange, fontSize = 28.sp, fontWeight = FontWeight.Black) }; if (c.payment == PaymentMethod.CASH) item { OutlinedTextField(c.receivedAmount.takeIf { it > 0 }?.let(::formatMoney) ?: "", { vm.updateCheckout(c.copy(receivedAmount = it.filter(Char::isDigit).toIntOrNull() ?: 0)) }, modifier = Modifier.fillMaxWidth(), label = { Text("Uang Diterima") }, singleLine = true); Text("Kembalian ${rupiah((c.receivedAmount - state.total).coerceAtLeast(0))}", color = Green, fontWeight = FontWeight.Bold) } else item { OutlinedTextField(c.reference, { vm.updateCheckout(c.copy(reference = it)) }, modifier = Modifier.fillMaxWidth(), label = { Text("Nomor Referensi (optional)") }) }; state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } } } } }

@Composable private fun SuccessScreen(state: PosState, vm: PosViewModel) { Scaffold(topBar = { AppHeader("Transaksi Berhasil") }) { p -> Column(Modifier.padding(p).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text("✓", color = Green, fontSize = 64.sp, fontWeight = FontWeight.Black); Text("Transaksi berhasil", color = Green, fontSize = 22.sp, fontWeight = FontWeight.Black); Text("#${state.completedOrder?.orderNumber}", color = Muted, modifier = Modifier.padding(8.dp)); Text(rupiah(state.completedOrder?.grandTotal ?: 0), color = Orange, fontSize = 30.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(24.dp)); Button({ vm.newTransaction() }, Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Orange)) { Text("TRANSAKSI BARU") } } } }

@Composable private fun OrdersScreen(state: PosState, vm: PosViewModel) { Scaffold(topBar = { AppHeader("Riwayat Transaksi") }, bottomBar = { BottomNav(state, vm) }) { p -> LazyColumn(Modifier.padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { if (state.orders.isEmpty()) item { Text("Belum ada transaksi.", color = Muted) }; items(state.orders.size) { index -> val order = state.orders[index]; Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("#${order.orderNumber}", fontWeight = FontWeight.Bold); Text(order.status, color = Green) }; Text(rupiah(order.grandTotal), color = Orange, fontWeight = FontWeight.Bold) } } } } } }
@Composable private fun ShiftScreen(state: PosState, vm: PosViewModel) { Scaffold(topBar = { AppHeader("Shift") }, bottomBar = { BottomNav(state, vm) }) { p -> Column(Modifier.padding(p).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { if (state.shift == null) Text("Belum ada shift aktif", fontSize = 20.sp, fontWeight = FontWeight.Black) else { Text("Shift #${state.shift.id}", fontSize = 22.sp, fontWeight = FontWeight.Black); Text("Aktif", color = Green, fontWeight = FontWeight.Bold); Text("Kas awal ${rupiah(state.shift.openingCash)}") } } } }
@Composable private fun MoreScreen(state: PosState, vm: PosViewModel) { var confirm by remember { mutableStateOf(false) }; Scaffold(topBar = { AppHeader("Lainnya") }, bottomBar = { BottomNav(state, vm) }) { p -> Column(Modifier.padding(p).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Akun Kasir", fontSize = 20.sp, fontWeight = FontWeight.Black); ListItem(headlineContent = { Text("Printer") }, supportingContent = { Text("RPP02N") }, leadingContent = { Icon(Icons.Default.Print, null) }, modifier = Modifier.clickable { vm.go(PosPage.PRINTER) }); ListItem(headlineContent = { Text("Server / Environment") }, supportingContent = { Text("Production") }, leadingContent = { Icon(Icons.Default.Cloud, null) }); HorizontalDivider(); TextButton({ confirm = true }) { Text("Logout", color = MaterialTheme.colorScheme.error) } } }; if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Keluar dari akun?") }, text = { Text("Anda perlu masuk kembali untuk menggunakan aplikasi.") }, confirmButton = { TextButton({ vm.logout() }) { Text("KELUAR", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton({ confirm = false }) { Text("BATAL") } }) }
@Composable private fun PrinterScreen(state: PosState, vm: PosViewModel, permissions: ActivityResultLauncher<Array<String>>) { val context = androidx.compose.ui.platform.LocalContext.current; val scope = rememberCoroutineScope(); val manager = remember { BluetoothPrinterManager(context) }; var devices by remember { mutableStateOf(emptyList<android.bluetooth.BluetoothDevice>()) }; var message by remember { mutableStateOf("Belum terhubung") }; Scaffold(topBar = { AppHeader("Printer") }) { p -> Column(Modifier.padding(p).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Printer thermal", fontSize = 24.sp, fontWeight = FontWeight.Black); Text(message, color = if (message.startsWith("Terhubung")) Green else Muted); Button(onClick = { if (!manager.hasConnectPermission() || !manager.hasScanPermission()) permissions.launch(arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_SCAN)) else devices = manager.pairedDevices().toList() }, colors = ButtonDefaults.buttonColors(containerColor = Orange)) { Text("PILIH PRINTER") }; devices.forEach { device -> ListItem(headlineContent = { Text(device.name ?: "Bluetooth printer") }, supportingContent = { Text(device.address) }, leadingContent = { Icon(Icons.Default.Print, null) }, modifier = Modifier.clickable { scope.launch { message = "Menghubungkan..."; vm.selectPrinter(device.address, device.name ?: "Printer"); message = when (val result = manager.connect(device)) { is com.kopipos.android.core.bluetooth.PrinterState.Connected -> "Terhubung — ${result.name}"; is com.kopipos.android.core.bluetooth.PrinterState.Error -> result.message; else -> "Belum terhubung" } } }) }; OutlinedButton(onClick = { scope.launch { message = when (val result = manager.testPrint()) { is com.kopipos.android.core.bluetooth.PrinterState.Connected -> "Test print berhasil"; is com.kopipos.android.core.bluetooth.PrinterState.Error -> result.message; else -> "Belum terhubung" } } }, enabled = message.startsWith("Terhubung")) { Text("CETAK TEST") } } } } }
