package com.example.demo.commands

import com.example.demo.commands.impls.*

internal val Commands = CommandList.Builder()
    .putCommand(CONTACTS_COMMAND_GROUP)
    .putCommand(NOTE_COMMAND_GROUP)
    .putCommand(APP_COMMAND_GROUP)
    .putCommand(MAKE_DIR_COMMAND)
    .putCommand(CLEAR_COMMAND)
    .putCommand(FLASH_COMMAND)
    .putCommand(TOUCH_COMMAND)
    .putCommand(ECHO_COMMAND)
    .putCommand(EXIT_COMMAND)
    .putCommand(READ_COMMAND)
    .putCommand(PWD_COMMAND)
    .putCommand(RSS_COMMAND)
    .putCommand(LS_COMMAND)
    .putCommand(RM_COMMAND)
    .putCommand(CD_COMMAND)
    .putCommand(BT_COMMAND)
    .build()
