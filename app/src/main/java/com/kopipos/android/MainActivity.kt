package com.kopipos.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil3.compose.AsyncImage
import com.kopipos.android.data.*
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

@Composable
fun PosApp(permissions: ActivityResultLauncher<Array<String>>) {
    val vm: PosViewModel = viewModel()
    val state by vm.state.collectAsState()
    if (!state.loggedIn) LoginScreen(state, vm) else CashierScreen(state, vm, permissions)
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
            Button({ vm.login(login, password) }, enabled = !state.loading && login.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Orange)) { Text(if (state.loading) "MEMUAT..." else "MASUK", fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CashierScreen(state: PosState, vm: PosViewModel, permissions: ActivityResultLauncher<Array<String>>) {
    var query by remember { mutableStateOf("") }; var selectedCategory by remember { mutableStateOf<Int?>(null) }; var cartOpen by remember { mutableStateOf(false) }
    val products = state.products.filter { (query.isBlank() || it.name.contains(query, true) || it.sku.contains(query, true)) && (selectedCategory == null || it.categoryId == selectedCategory) }
    Scaffold(containerColor = Color(0xFFF7F8FA), topBar = {
        TopAppBar(title = { Text("KopiPOS", fontWeight = FontWeight.Black, color = Color.White) }, actions = { IconButton({ vm.logout() }) { Icon(Icons.Default.MoreVert, "Menu", tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Orange))
    }, bottomBar = { if (state.cart.isNotEmpty()) CartDock(state, onClick = { cartOpen = true }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusDot(Green); Text("Shift aktif", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp)); Spacer(Modifier.weight(1f)); Icon(Icons.Default.Print, null, tint = Green, modifier = Modifier.size(18.dp)); Text("RPP02N", fontSize = 12.sp, color = Muted, modifier = Modifier.padding(start = 5.dp)); StatusDot(Green, Modifier.padding(start = 5.dp))
            }
            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth().padding(14.dp), placeholder = { Text("Cari menu / SKU") }, leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { Icon(Icons.Default.Tune, null, tint = Muted) }, shape = RoundedCornerShape(16.dp), singleLine = true)
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryChip("Semua", selectedCategory == null) { selectedCategory = null }
                state.categories.forEach { category -> CategoryChip(category.name, selectedCategory == category.id) { selectedCategory = category.id } }
            }
            if (!state.shiftOpen) Text("Buka shift dulu sebelum transaksi", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), contentPadding = PaddingValues(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                items(products, key = { it.id }) { product -> ProductCard(product) { if (state.shiftOpen) vm.add(product) } }
            }
        }
    }
    if (cartOpen) ModalBottomSheet(onDismissRequest = { cartOpen = false }, containerColor = Color.White) { CartSheet(state, vm) }
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

@Composable private fun CartSheet(state: PosState, vm: PosViewModel) { Column(Modifier.fillMaxWidth().padding(20.dp)) { Text("Keranjang", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); LazyColumn(Modifier.weight(1f, false)) { items(state.cart.size) { index -> val item = state.cart[index]; Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(item.product.name, fontWeight = FontWeight.Bold); Text(rupiah(item.lineTotal), color = Orange) }; IconButton({ vm.dec(item.product.id) }) { Icon(Icons.Default.Remove, "Kurangi") }; Text(item.quantity.toString(), fontWeight = FontWeight.Bold); IconButton({ vm.add(item.product) }) { Icon(Icons.Default.Add, "Tambah") }; IconButton({ vm.remove(item.product.id) }) { Icon(Icons.Default.DeleteOutline, "Hapus") } } } }; Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total", fontWeight = FontWeight.Black); Text(rupiah(state.total), color = Orange, fontWeight = FontWeight.Black) }; Button({ vm.checkout() }, enabled = !state.loading, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Orange), shape = RoundedCornerShape(16.dp)) { Text("LANJUT BAYAR", fontWeight = FontWeight.Black) }; state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) } } }
