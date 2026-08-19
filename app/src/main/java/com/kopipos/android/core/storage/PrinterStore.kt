package com.kopipos.android.core.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.printerDataStore by preferencesDataStore("kopipos_printer")

class PrinterStore(private val context: Context) {
    private val addressKey = stringPreferencesKey("printer_address")
    private val nameKey = stringPreferencesKey("printer_name")
    val address = context.printerDataStore.data.map { it[addressKey] }
    suspend fun save(address: String, name: String) = context.printerDataStore.edit { it[addressKey] = address; it[nameKey] = name }
    suspend fun clear() = context.printerDataStore.edit { it.clear() }
}

