package com.kopipos.android

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kopipos.android.data.*

import com.kopipos.android.ui.theme.KopiTheme
import java.util.UUID

class MainActivity: ComponentActivity() {
 private val bt = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { KopiTheme { PosApp(bt) } } }
}

@Composable fun PosApp(permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
 val vm: PosViewModel = viewModel()
 val s by vm.state.collectAsState()
 if (!s.loggedIn) LoginScreen(s, vm)
 else PosScreen(s, vm, permissionLauncher)
}

@Composable fun LoginScreen(s: PosState, vm: PosViewModel) { var login by remember { mutableStateOf("") }; var pass by remember { mutableStateOf("") }
 Surface(Modifier.fillMaxSize()) { Column(Modifier.padding(28.dp).fillMaxSize(), verticalArrangement=Arrangement.Center) {
  Text("KopiPOS", style=MaterialTheme.typography.displaySmall); Text("Kasir native", color=MaterialTheme.colorScheme.primary)
  Spacer(Modifier.height(28.dp)); OutlinedTextField(login,{login=it},label={Text("Username / email")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(pass,{pass=it},label={Text("Password")},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
  s.error?.let { Text(it,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(top=8.dp)) }
  Button({vm.login(login,pass)},enabled=!s.loading&&login.isNotBlank()&&pass.isNotBlank(),modifier=Modifier.fillMaxWidth().padding(top=18.dp)){Text(if(s.loading)"Masuk..." else "Masuk")}
 } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PosScreen(s: PosState, vm: PosViewModel, launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
 var query by remember { mutableStateOf("") }; var cartOpen by remember { mutableStateOf(false) }
 val products=s.products.filter { it.name.contains(query,true)||it.sku.contains(query,true) }
 Scaffold(topBar={TopAppBar(title={Text(s.storeName)},actions={Text(if(s.printerConnected)"● Printer" else "○ Printer",modifier=Modifier.padding(12.dp)); IconButton({vm.logout()}){Icon(Icons.Default.ExitToApp,"Logout")}})},bottomBar={if(s.cart.isNotEmpty()) Button({cartOpen=true},modifier=Modifier.fillMaxWidth().padding(12.dp)){Text("${s.cart.sumOf{it.quantity}} item  •  ${rupiah(s.total)}  •  BAYAR")}}) { pad ->
 Column(Modifier.padding(pad).padding(horizontal=12.dp)) { OutlinedTextField(query,{query=it},leadingIcon={Icon(Icons.Default.Search,null)},label={Text("Cari produk / SKU")},modifier=Modifier.fillMaxWidth())
  if(!s.shiftOpen) Text("Buka shift dulu sebelum transaksi",color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(8.dp))
  LazyVerticalGrid(columns=GridCells.Adaptive(150.dp),contentPadding=PaddingValues(vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) { items(products,key={it.id}) { p -> Card(onClick={if(s.shiftOpen)vm.add(p)},enabled=p.isAvailable){Column(Modifier.padding(12.dp)){Text(p.name,style=MaterialTheme.typography.titleMedium);Text(rupiah(p.basePrice),style=MaterialTheme.typography.titleSmall);if(!p.isAvailable)Text("Habis")}} } }
 }
 }
 if(cartOpen) ModalBottomSheet(onDismissRequest={cartOpen=false}) { CartSheet(s,vm) }
}
@Composable fun CartSheet(s: PosState,vm:PosViewModel){ Column(Modifier.padding(20.dp).fillMaxWidth()){Text("Keranjang",style=MaterialTheme.typography.headlineSmall);LazyColumn{items(s.cart){i->Row(Modifier.fillMaxWidth().padding(vertical=8.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("${i.quantity}x ${i.product.name}");Row{Text(rupiah(i.lineTotal));IconButton({vm.dec(i.product.id)}){Icon(Icons.Default.Remove,null)};IconButton({vm.add(i.product)}){Icon(Icons.Default.Add,null)}}}}};Text("Total ${rupiah(s.total)}",style=MaterialTheme.typography.titleLarge);Button({vm.checkout()},enabled=!s.loading,modifier=Modifier.fillMaxWidth()){Text("BAYAR")};s.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}}}
fun rupiah(n:Int)="Rp "+String.format("%,d",n).replace(',','.')
