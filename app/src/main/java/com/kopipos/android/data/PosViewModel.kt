package com.kopipos.android.data
import androidx.lifecycle.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID

data class PosState(val loggedIn:Boolean=false,val loading:Boolean=false,val error:String?=null,val products:List<Product> = emptyList(),val cart:List<CartItem> = emptyList(),val shiftOpen:Boolean=false,val storeName:String="KopiPOS",val printerConnected:Boolean=false){val total get()=cart.sumOf{it.lineTotal}}
class PosViewModel:ViewModel(){
 private val api=Retrofit.Builder().baseUrl(com.kopipos.android.BuildConfig.BASE_URL).addConverterFactory(GsonConverterFactory.create()).build().create(PosApi::class.java)
 private val _state=MutableStateFlow(PosState()); val state=_state.asStateFlow()
 fun login(login:String,password:String)=viewModelScope.launch{_state.update{it.copy(loading=true,error=null)};try{val x=api.login(TokenRequest(login,password));if(x.success&&x.data!=null){_state.update{it.copy(loggedIn=true,loading=false,products=api.products().data?:emptyList(),shiftOpen=api.activeShift().data!=null)}}else fail(x.message)}catch(e:Exception){fail("Koneksi gagal")}}
 fun add(p:Product)=_state.update{s->s.copy(cart=buildList { var found=false; s.cart.forEach { if(it.product.id==p.id) { add(it.copy(quantity=it.quantity+1)); found=true } else add(it) }; if(!found) add(CartItem(p,1)) })}
 fun dec(id:Int)=_state.update{s->s.copy(cart=s.cart.flatMap{if(it.product.id!=id)listOf(it)else if(it.quantity>1)listOf(it.copy(quantity=it.quantity-1))else emptyList()})}
 fun checkout()=viewModelScope.launch{val s=_state.value;if(s.cart.isEmpty())return@launch;_state.update{it.copy(loading=true,error=null)};try{val r=api.checkout(OrderRequest(UUID.randomUUID().toString(),"-","Kasir","TAKE_AWAY",0,s.cart.map{OrderItemRequest(it.product.id,it.quantity)},PaymentRequest("CASH",s.total)));if(r.success)_state.update{it.copy(loading=false,cart=emptyList(),error="Transaksi ${r.data?.order_number} berhasil") }else fail(r.message)}catch(e:Exception){fail("Checkout gagal")}}
 fun logout()=viewModelScope.launch{runCatching{api.logout()};_state.value=PosState()};private fun fail(m:String?)=_state.update{it.copy(loading=false,error=m?:"Request gagal")}
}
