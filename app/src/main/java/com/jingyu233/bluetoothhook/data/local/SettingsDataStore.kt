package com.jingyu233.bluetoothhook.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jingyu233.bluetoothhook.data.model.AppSettings
import com.jingyu233.bluetoothhook.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * DataStore设置持久化
 * 使用 Preferences DataStore 存储应用设置
 */
class SettingsDataStore(private val context: Context) {

    companion object {
        private val TAG = Logger.Tags.DATA_DATABASE
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

        // DataStore Keys
        private val KEY_WEBDAV_URL = stringPreferencesKey("webdav_url")
        private val KEY_WEBDAV_USERNAME = stringPreferencesKey("webdav_username")
        private val KEY_WEBDAV_PASSWORD = stringPreferencesKey("webdav_password")
        private val KEY_AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        private val KEY_SYNC_INTERVAL_SECONDS = intPreferencesKey("sync_interval_seconds")
    }

    /**
     * 设置Flow
     * 自动响应数据变化
     */
    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            // 处理读取错误
            if (exception is IOException) {
                Logger.App.e(TAG, "Error reading settings from DataStore", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                webdavUrl = preferences[KEY_WEBDAV_URL] ?: "",
                webdavUsername = preferences[KEY_WEBDAV_USERNAME] ?: "",
                webdavPassword = preferences[KEY_WEBDAV_PASSWORD] ?: "",
                autoSyncEnabled = preferences[KEY_AUTO_SYNC_ENABLED] ?: false,
                syncIntervalSeconds = preferences[KEY_SYNC_INTERVAL_SECONDS] ?: 60
            )
        }

    /**
     * 更新WebDAV URL
     */
    suspend fun updateWebDavUrl(url: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_WEBDAV_URL] = url
            }
            Logger.App.d(TAG, "Updated WebDAV URL")
        } catch (e: IOException) {
            Logger.App.e(TAG, "Failed to update WebDAV URL", e)
        }
    }

    /**
     * 更新WebDAV用户名
     */
    suspend fun updateWebDavUsername(username: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_WEBDAV_USERNAME] = username
            }
            Logger.App.d(TAG, "Updated WebDAV username")
        } catch (e: IOException) {
            Logger.App.e(TAG, "Failed to update WebDAV username", e)
        }
    }

    /**
     * 更新WebDAV密码
     */
    suspend fun updateWebDavPassword(password: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_WEBDAV_PASSWORD] = password
            }
            Logger.App.d(TAG, "Updated WebDAV password")
        } catch (e: IOException) {
            Logger.App.e(TAG, "Failed to update WebDAV password", e)
        }
    }

    /**
     * 切换自动同步
     */
    suspend fun toggleAutoSync(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_AUTO_SYNC_ENABLED] = enabled
            }
            Logger.App.d(TAG, "Updated auto sync enabled: $enabled")
        } catch (e: IOException) {
            Logger.App.e(TAG, "Failed to update auto sync", e)
        }
    }

    /**
     * 更新同步间隔(秒)
     */
    suspend fun updateSyncInterval(seconds: Int) {
        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_SYNC_INTERVAL_SECONDS] = seconds
            }
            Logger.App.d(TAG, "Updated sync interval: $seconds seconds")
        } catch (e: IOException) {
            Logger.App.e(TAG, "Failed to update sync interval", e)
        }
    }

    /**
     * 批量更新WebDAV设置
     */
    suspend fun updateWebDavSettings(url: String, username: String, password: String, autoSync: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_WEBDAV_URL] = url
                preferences[KEY_WEBDAV_USERNAME] = username
                preferences[KEY_WEBDAV_PASSWORD] = password
                preferences[KEY_AUTO_SYNC_ENABLED] = autoSync
            }
            Logger.App.i(TAG, "Updated all WebDAV settings")
        } catch (e: IOException) {
            Logger.App.e(TAG, "Failed to update WebDAV settings", e)
        }
    }

    /**
     * 清空所有设置
     */
    suspend fun clearAll() {
        try {
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
            Logger.App.w(TAG, "Cleared all settings from DataStore")
        } catch (e: IOException) {
            Logger.App.e(TAG, "Failed to clear settings", e)
        }
    }
}
