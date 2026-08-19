package com.kopipos.android.core.storage

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.edit

private val Context.sessionDataStore by preferencesDataStore("kopipos_session")
class SessionStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("auth_token")
    val token: Flow<String?> = context.sessionDataStore.data.map { it[tokenKey] }
    suspend fun saveToken(value: String) = context.sessionDataStore.edit { it[tokenKey] = value }
    suspend fun clear() = context.sessionDataStore.edit { it.remove(tokenKey) }
}
