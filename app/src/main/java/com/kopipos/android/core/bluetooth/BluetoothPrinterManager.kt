package com.kopipos.android.core.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

sealed interface PrinterState { data object NoPrinter : PrinterState; data object Connecting : PrinterState; data class Connected(val name: String, val address: String) : PrinterState; data class Error(val message: String) : PrinterState }

class BluetoothPrinterManager(private val context: Context) {
    private val adapter: BluetoothAdapter? get() = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    fun pairedDevices(): Set<BluetoothDevice> = if (!hasConnectPermission()) emptySet() else adapter?.bondedDevices.orEmpty()
    fun hasConnectPermission() = android.os.Build.VERSION.SDK_INT < 31 || ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    fun hasScanPermission() = android.os.Build.VERSION.SDK_INT < 31 || ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
    suspend fun connect(device: BluetoothDevice): PrinterState = withContext(Dispatchers.IO) {
        if (!hasConnectPermission() || !hasScanPermission()) return@withContext PrinterState.Error("Izin Bluetooth belum diberikan")
        try { socket?.close(); adapter?.cancelDiscovery(); socket = device.createRfcommSocketToServiceRecord(uuid); socket?.connect(); PrinterState.Connected(device.name ?: "Printer", device.address) }
        catch (e: Exception) { socket = null; PrinterState.Error("Gagal terhubung ke printer: ${e.message ?: "koneksi gagal"}") }
    }
    suspend fun print(bytes: ByteArray): PrinterState = withContext(Dispatchers.IO) { try { socket?.outputStream?.write(bytes); socket?.outputStream?.flush(); PrinterState.Connected("Printer", socket?.remoteDevice?.address ?: "") } catch (e: Exception) { PrinterState.Error("Gagal mencetak: ${e.message ?: "printer offline"}") } }
    suspend fun testPrint(): PrinterState = print(byteArrayOf(0x1B, 0x40, 0x1B, 0x61, 0x01, 0x4B, 0x6F, 0x70, 0x69, 0x50, 0x4F, 0x53, 0x0A, 0x0A, 0x1D, 0x56, 0x00))
    fun disconnect() { runCatching { socket?.close() }; socket = null }
}
