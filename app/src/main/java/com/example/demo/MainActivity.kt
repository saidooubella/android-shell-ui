@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalLayoutApi::class,
    ExperimentalLifecycleComposeApi::class,
)

package com.example.demo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.demo.db.AppDatabase
import com.example.demo.ui.theme.DemoTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal val MANAGE_FILES_SETTINGS: Intent
    @RequiresApi(Build.VERSION_CODES.R)
    get() = Intent(
        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
        Uri.parse("package:" + BuildConfig.APPLICATION_ID)
    )

private fun Context.toast(message: String, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, length).show()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val viewModel by viewModels<ScreenViewModel> {
            ScreenViewModel.Factory(
                application, Repository(packageManager, AppDatabase.get(application))
            )
        }

        onBackPressedDispatcher.addCallback(this, false) {}

        val permissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                val handler =
                    viewModel.screenState.value.permissions ?: return@registerForActivityResult
                handler.continuation.complete(it.values.all { granted -> granted })
                viewModel.finishPermissions()
            }

        val intentResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                val handler =
                    viewModel.screenState.value.intentForResult ?: return@registerForActivityResult
                handler.continuation.complete(it)
                viewModel.finishIntentForResult()
            }

        setContent {

            val screenState = viewModel.screenState.collectAsStateWithLifecycle().value

            DemoTheme(screenState.isDark) {

                if (screenState.exit) {
                    finish()
                    viewModel.finishExiting()
                }

                if (screenState.intent != null) {
                    startActivity(screenState.intent)
                    viewModel.finishIntent()
                }

                if (screenState.intentForResult != null) {
                    intentResult.launch(screenState.intentForResult.intent)
                }

                if (screenState.permissions != null) {
                    permissions.launch(screenState.permissions.permissions)
                }

                Screen(
                    onFieldTextChange = viewModel::changeFieldText,
                    onThemeChange = viewModel::toggleTheme,
                    onSubmit = viewModel::submitLine,
                    state = screenState
                )
            }
        }
    }
}

@Composable
private fun Screen(
    state: ScreenState,
    onFieldTextChange: (TextFieldValue) -> Unit,
    onThemeChange: () -> Unit,
    onSubmit: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { ShellTopBar(onThemeChange, state) },
        bottomBar = { ShellBottomBar(onSubmit, state, onFieldTextChange) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                    bottom = paddingValues.calculateBottomPadding(),
                    top = paddingValues.calculateTopPadding(),
                )
                .fillMaxSize(),
        ) {
            ShellLogsList(state)
            ShellEmptyView(state)
        }
    }
}

@Composable
private fun ShellBottomBar(
    onSubmit: () -> Unit,
    state: ScreenState,
    onFieldTextChange: (TextFieldValue) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ShellTextField(onSubmit, state, onFieldTextChange)
        AnimatedVisibility(!state.isIdle) {
            LinearProgressIndicator(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            )
        }
        ShellSuggestionsBox(state, onFieldTextChange)
    }
}

@Composable
private fun BoxScope.ShellEmptyView(state: ScreenState) {
    AnimatedVisibility(
        modifier = Modifier.align(Alignment.Center),
        enter = fadeIn(), exit = fadeOut(),
        visible = state.logs.isEmpty() && state.isIdle
    ) {
        Icon(
            modifier = Modifier
                .padding(8.dp)
                .size(64.dp),
            imageVector = Icons.Outlined.Terminal,
            contentDescription = "Empty Shell",
        )
    }
}

@Composable
private fun ShellLogsList(
    screenState: ScreenState,
    scope: CoroutineScope = rememberCoroutineScope(),
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imeNestedScroll(),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        items(screenState.logs) { log ->
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(log.action != null) {
                        scope.launch { log.action?.invoke() }
                    },
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                text = log.message,
            )
        }
    }
}

@Composable
private fun ShellSuggestionsBox(
    state: ScreenState,
    onFieldTextChange: (TextFieldValue) -> Unit,
) {
    AnimatedVisibility(
        visible = state.isIdle && state.suggestions.suggestions.isNotEmpty(),
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(state.suggestions.suggestions) { suggestion ->
                SuggestionChip(
                    onClick = {
                        val text = when (val action = state.suggestions.mergeAction) {
                            is MergeAction.Append -> {
                                if (state.fieldText.text.isNotEmpty())
                                    buildString {
                                        append(state.fieldText.text)
                                        if (!state.fieldText.text.last().isWhitespace())
                                            append(' ')
                                        append(suggestion.replacement)
                                    }
                                else suggestion.replacement
                            }
                            is MergeAction.Replace -> {
                                StringBuilder(state.fieldText.text)
                                    .replace(action.start, action.end, suggestion.replacement)
                                    .toString()
                            }
                        }
                        onFieldTextChange(TextFieldValue(text, TextRange(text.length, text.length)))
                    },
                    label = { Text(suggestion.label, fontSize = 16.sp) }
                )
            }
        }
    }
}

@Composable
private fun ShellTextField(
    onSubmit: () -> Unit,
    state: ScreenState,
    onFieldTextChange: (TextFieldValue) -> Unit,
) {
    OutlinedTextField(
        placeholder = {
            Text(
                text = when (state.mode) {
                    is ShellMode.PromptMode -> state.mode.hint
                    is ShellMode.RegularMode -> "Enter a command..."
                },
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
            )
        },
        value = state.fieldText,
        onValueChange = onFieldTextChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .onKeyEvent {
                when (it.key) {
                    Key.Enter -> {
                        onSubmit()
                        true
                    }
                    else -> false
                }
            },
        keyboardActions = KeyboardActions(onGo = { onSubmit() }),
        keyboardOptions = KeyboardOptions(
            autoCorrect = false,
            imeAction = ImeAction.Go,
        ),
        shape = RoundedCornerShape(20),
        trailingIcon = {
            Surface(
                color = Color.Transparent,
                onClick = onSubmit,
                shape = RoundedCornerShape(20),
            ) {
                Icon(
                    modifier = Modifier.padding(16.dp),
                    imageVector = Icons.Outlined.Send,
                    contentDescription = "Send",
                )
            }
        },
        textStyle = TextStyle(fontSize = 16.sp, fontFamily = FontFamily.Monospace),
        singleLine = true
    )
}

@Composable
private fun ShellTopBar(onThemeChange: () -> Unit, state: ScreenState) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        ),
        title = {
            Text(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                text = "Shell-UI"
            )
        },
        actions = {
            Surface(
                onClick = onThemeChange,
                color = Color.Transparent,
                shape = RoundedCornerShape(20),
            ) {
                Icon(
                    modifier = Modifier.padding(16.dp),
                    imageVector = if (state.isDark)
                        Icons.Outlined.LightMode
                    else
                        Icons.Outlined.DarkMode,
                    contentDescription = "Dark Theme Toggle",
                )
            }
        }
    )
}
