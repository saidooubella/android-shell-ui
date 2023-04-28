package com.saidooubella.shellui.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.saidooubella.shellui.data.pinned.PinnedApp
import com.saidooubella.shellui.suggestions.Suggestion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

internal inline fun <T> catch(block: () -> T): T? {
    return try {
        block()
    } catch (_: Exception) {
        null
    }
}

internal fun PackageManager.loadFromPackage(packageName: String): ApplicationInfo {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        getApplicationInfo(packageName, 0)
    }
}

internal fun Context.hasNotPermission(permission: String): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED
    } else {
        false
    }
}

internal val MANAGE_FILES_SETTINGS: Intent
    @RequiresApi(Build.VERSION_CODES.R)
    get() = Intent(
        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
        Uri.parse("package:" + com.saidooubella.shellui.BuildConfig.APPLICATION_ID)
    )

@Suppress("FunctionName")
internal fun OpenableApp(label: String, packageName: String): Suggestion {
    return Suggestion(label, "apps open-pkg $packageName", true)
}

internal fun <T> DataStore<Preferences>.toStateFlow(
    scope: CoroutineScope, key: Preferences.Key<T>, default: T
): StateFlow<T> = this.data.map { settings -> settings[key] ?: default }
    .stateIn(scope, SharingStarted.WhileSubscribed(), this[key] ?: default)

internal operator fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>): T? {
    return runBlocking { data.first()[key] }
}
