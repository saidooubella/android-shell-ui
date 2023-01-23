package com.example.demo.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.example.demo.BuildConfig
import com.example.demo.data.pinned.PinnedApp
import com.example.demo.suggestions.Suggestion

internal inline fun <T> catch(block: () -> T): T? {
    return try {
        block()
    } catch (_: Exception) {
        null
    }
}

internal fun PackageManager.loadFromPackage(app: PinnedApp): ApplicationInfo {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getApplicationInfo(app.packageName, PackageManager.ApplicationInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        getApplicationInfo(app.packageName, 0)
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
        Uri.parse("package:" + BuildConfig.APPLICATION_ID)
    )

@Suppress("FunctionName")
internal fun OpenableApp(label: String, packageName: String): Suggestion {
    return Suggestion(label, "apps open-pkg $packageName", true)
}
