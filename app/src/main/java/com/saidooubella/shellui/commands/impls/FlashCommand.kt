package com.saidooubella.shellui.commands.impls

import android.Manifest
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import com.saidooubella.shellui.commands.Command
import com.saidooubella.shellui.commands.Metadata
import com.saidooubella.shellui.managers.FlashManager
import com.saidooubella.shellui.shell.Action
import com.saidooubella.shellui.suggestions.Suggestion
import com.saidooubella.shellui.suggestions.Suggestions
import com.saidooubella.shellui.utils.hasNotPermission

internal val FLASH_COMMAND = Command.Leaf(
    Metadata.Builder("flash")
        .addRequiredArg("facing", Suggestions.Custom(listOf(Suggestion("front"), Suggestion("back"))))
        .addRequiredArg("state", Suggestions.Custom(listOf(Suggestion("on"), Suggestion("off"))))
        .build()
) { arguments ->

    val facing = arguments[0].value.takeIf { it == "front" || it == "back" } ?: run {
        sendAction(Action.Message("facing mode could be either 'back' or 'front'"))
        return@Leaf
    }

    val state = arguments[1].value.takeIf { it == "on" || it == "off" } ?: run {
        sendAction(Action.Message("state mode could be either 'on' or 'off'"))
        return@Leaf
    }

    if (flashManager.NeedsPermission && appContext.hasNotPermission(Manifest.permission.CAMERA)) {
        if (!sendAction(Action.RequestPermissions(arrayOf(Manifest.permission.CAMERA)))) {
            sendAction(Action.Message("Can't have access to the camera"))
            return@Leaf
        }
    }

    val facingMode = if (facing == "front") flashManager.FacingFront else flashManager.FacingBack

    val message = when (flashManager.setTorchMode(facingMode, state == "on")) {
        FlashManager.Result.CameraNotFound -> "The $facing camera isn't available"
        FlashManager.Result.FlashNotFound -> "${facing.capitalize(Locale.current)} camera doesn't have a flash unit"
        FlashManager.Result.Success -> "Flash is $state"
    }

    sendAction(Action.Message(message))
}
