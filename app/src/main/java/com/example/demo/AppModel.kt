package com.example.demo

import android.content.Intent
import androidx.core.net.toUri

internal data class AppModel(
    internal val name: String,
    internal val packageName: String,
    internal val launchIntent: Intent,
)

internal val AppModel.uninstallIntent: Intent
    get() = Intent(Intent.ACTION_DELETE, "package:$packageName".toUri())
