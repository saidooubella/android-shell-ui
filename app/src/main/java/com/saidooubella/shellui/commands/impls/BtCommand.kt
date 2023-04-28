package com.saidooubella.shellui.commands.impls

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.saidooubella.shellui.commands.Command
import com.saidooubella.shellui.commands.Metadata
import com.saidooubella.shellui.shell.Action
import com.saidooubella.shellui.utils.hasNotPermission

@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
internal val BT_COMMAND = Command.Leaf(Metadata.Builder("bt").build()) {

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        if (appContext.hasNotPermission(Manifest.permission.BLUETOOTH_ADMIN)
            || appContext.hasNotPermission(Manifest.permission.BLUETOOTH_CONNECT)
            || appContext.hasNotPermission(Manifest.permission.BLUETOOTH)
        ) {
            if (!sendAction(
                    Action.RequestPermissions(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.BLUETOOTH
                        )
                    )
                )
            ) {
                sendAction(Action.Message("Bluetooth permission denied"))
                return@Leaf
            }
        }
    } else {
        if (appContext.hasNotPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            if (!sendAction(Action.RequestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT)))) {
                sendAction(Action.Message("Bluetooth permission denied"))
                return@Leaf
            }
        }
    }

    val adapter = appContext.getSystemService<BluetoothManager>()?.adapter ?: run {
        sendAction(Action.Message("Bluetooth isn't supported on this device"))
        return@Leaf
    }

    val shouldEnable = !adapter.isEnabled

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        if (shouldEnable) adapter.enable() else adapter.disable()
    } else if (shouldEnable) {
        val result =
            sendAction(Action.StartIntentForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)))
        if (result.resultCode != Activity.RESULT_OK) {
            sendAction(Action.Message("Bluetooth wasn't enabled"))
            return@Leaf
        }
    } else {
        sendAction(Action.Message("Bluetooth cannot be disabled. This action must be done manually"))
        return@Leaf
    }

    sendAction(Action.Message("Bluetooth is " + if (shouldEnable) "enabled" else "disabled"))
}
