package com.example.demo.commands.impls

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.example.demo.commands.Command
import com.example.demo.commands.CommandList
import com.example.demo.commands.Metadata
import com.example.demo.models.LauncherApp
import com.example.demo.models.settingsIntent
import com.example.demo.models.uninstallIntent
import com.example.demo.shell.Action
import com.example.demo.shell.ShellContext
import com.example.demo.suggestions.Suggestions

internal val APP_COMMAND_GROUP = object : Command.Group("apps") {

    private val LIST_OPTION = Leaf(Metadata.Builder("ls").build()) {
        repository.loadLauncherApps().forEach {
            sendAction(Action.Message(it.name) {
                appContext.startActivity(it.launchIntent)
            })
        }
    }

    private val REMOVE_OPTION = Leaf(
        Metadata.Builder("rm")
            .addRequiredArg("name", Suggestions.Apps)
            .build()
    ) { arguments ->
        val app = lookupApplication(arguments[0].value) ?: return@Leaf
        sendAction(Action.StartIntent(app.uninstallIntent))
    }

    private val OPEN_OPTION = Leaf(
        Metadata.Builder("open").addRequiredArg("name", Suggestions.Apps).build()
    ) { arguments ->
        val app = lookupApplication(arguments[0].value) ?: return@Leaf
        sendAction(Action.StartIntent(app.launchIntent))
    }

    @Suppress("DEPRECATION")
    private val OPEN_PACKAGE = Leaf(
        Metadata.Builder("open-pkg")
            .addRequiredArg("package", Suggestions.AppsPackages)
            .build()
    ) {
        try {
            val packageManager = appContext.packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(it[0].value, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                packageManager.getApplicationInfo(it[0].value, 0)
            }
            sendAction(Action.StartIntent(Intent(packageManager.getLaunchIntentForPackage(appInfo.packageName))))
        } catch (e: PackageManager.NameNotFoundException) {
            sendAction(Action.Message("'${it[0].value}' not found"))
        }
    }

    private val SETTINGS_OPTION = Leaf(
        Metadata.Builder("st").addRequiredArg("name", Suggestions.Apps).build()
    ) { arguments ->
        val app = lookupApplication(arguments[0].value) ?: return@Leaf
        sendAction(Action.StartIntent(app.settingsIntent))
    }

    private val PIN_OPTION = Leaf(
        Metadata.Builder("pin").addRequiredArg("name", Suggestions.NotPinnedApps).build()
    ) { arguments ->
        val app = lookupApplication(arguments[0].value) ?: return@Leaf
        repository.pinApp(app.packageName)
    }

    private val UNPIN_OPTION = Leaf(
        Metadata.Builder("unpin").addRequiredArg("name", Suggestions.PinnedApps).build()
    ) { arguments ->
        val app = lookupApplication(arguments[0].value) ?: return@Leaf
        if (!repository.unpinApp(app.packageName)) {
            sendAction(Action.Message("${arguments[0].value} is not pinned"))
        }
    }

    override val commands: CommandList = CommandList.Builder()
        .putCommand(SETTINGS_OPTION)
        .putCommand(REMOVE_OPTION)
        .putCommand(OPEN_PACKAGE)
        .putCommand(UNPIN_OPTION)
        .putCommand(LIST_OPTION)
        .putCommand(OPEN_OPTION)
        .putCommand(PIN_OPTION)
        .build()

    private suspend fun ShellContext.lookupApplication(appName: String): LauncherApp? {

        val apps = repository
            .loadLauncherApps { it.equals(appName, true) }
            .takeIf { it.isNotEmpty() }

        if (apps == null) {
            sendAction(Action.Message("'$appName' not found"))
            return null
        }

        if (apps.size == 1) {
            return apps.first()
        }

        sendAction(Action.Message("There are more than one app with this name. Which one did you meant?"))

        apps.forEachIndexed { index, appModel ->
            sendAction(Action.Message("$index: ${appModel.packageName}"))
        }

        val index = sendAction(Action.Prompt("Enter a valid index"))
            .toIntOrNull()?.takeIf { it in apps.indices }

        if (index == null) {
            sendAction(Action.Message("Invalid index"))
            return null
        }

        return apps[index]
    }
}
