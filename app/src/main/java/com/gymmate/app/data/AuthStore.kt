package com.gymmate.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.authDataStore by preferencesDataStore(name = "auth_store")

data class AuthSession(
    val token: String = "",
)

class AuthStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("auth_token")

    val session: Flow<AuthSession> = context.authDataStore.data
        .catch {
            if (it is IOException) emit(emptyPreferences()) else throw it
        }
        .map { prefs ->
            AuthSession(token = prefs[tokenKey].orEmpty())
        }

    suspend fun saveToken(token: String) {
        context.authDataStore.edit { prefs ->
            prefs[tokenKey] = token
        }
    }

    suspend fun clear() {
        context.authDataStore.edit { prefs ->
            prefs.remove(tokenKey)
        }
    }
}

