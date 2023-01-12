package com.example.demo

import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

internal data class AppModel(
    internal val name: String,
    internal val packageName: String,
    internal val launchIntent: Intent,
)

internal val AppModel.uninstallIntent: Intent
    get() = Intent(Intent.ACTION_DELETE, "package:$packageName".toUri())

internal val AppModel.settingsIntent: Intent
    get() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
