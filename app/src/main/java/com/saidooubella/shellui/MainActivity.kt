@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalLayoutApi::class,
)

package com.saidooubella.shellui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saidooubella.shellui.data.DataRepository
import com.saidooubella.shellui.data.ShellDatabase
import com.saidooubella.shellui.models.LogItem
import com.saidooubella.shellui.preferences.ShellPreferences
import com.saidooubella.shellui.suggestions.MergeAction
import com.saidooubella.shellui.suggestions.Suggestion
import com.saidooubella.shellui.ui.theme.ShellUITheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<MainViewModel> {
        MainViewModel.Factory(
            application, ShellPreferences(application),
            DataRepository(packageManager, ShellDatabase.get(application), contentResolver)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

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

            val isDarkTheme = viewModel.inDarkTheme.collectAsStateWithLifecycle().value

            val pinnedSuggestions = viewModel.pinned.collectAsStateWithLifecycle(listOf()).value

            ShellUITheme(isDarkTheme) {

                if (screenState.exit) {
                    finish()
                    viewModel.finishExiting()
                }

                if (screenState.intent != null) {
                    startActivity(screenState.intent)
                    viewModel.finishIntent()
                }

                if (screenState.intentForResult != null && !screenState.intentForResult.triggered) {
                    intentResult.launch(screenState.intentForResult.intent)
                    viewModel.markIntentForResultTriggered()
                }

                if (screenState.permissions != null && !screenState.permissions.triggered) {
                    permissions.launch(screenState.permissions.permissions)
                    viewModel.markPermissionsTriggered()
                }

                Screen(
                    onFieldTextChange = viewModel::changeFieldText,
                    pinnedSuggestions = pinnedSuggestions,
                    onThemeChange = viewModel::toggleTheme,
                    onSubmit = viewModel::submitLine,
                    isDarkTheme = isDarkTheme,
                    state = screenState
                )
            }
        }
    }
}

@Composable
private fun Screen(
    state: ScreenState,
    isDarkTheme: Boolean,
    pinnedSuggestions: List<Suggestion>,
    onFieldTextChange: (TextFieldValue) -> Unit,
    onThemeChange: () -> Unit,
    onSubmit: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { ShellTopBar(onThemeChange, isDarkTheme) },
        bottomBar = {
            ShellBottomBar(
                pinnedSuggestions,
                state.suggestions.suggestions,
                state.suggestions.mergeAction,
                state.fieldText,
                state.mode,
                state.isIdle,
                onFieldTextChange,
                onSubmit
            )
        }
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
            ShellLogsList(state.logs)
            ShellEmptyView(state.logs.isEmpty() && state.isIdle)
        }
    }
}

@Composable
private fun BoxScope.ShellEmptyView(visible: Boolean) {
    AnimatedVisibility(
        modifier = Modifier.align(Alignment.Center),
        enter = fadeIn(), exit = fadeOut(),
        visible = visible,
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
    logs: PersistentList<LogItem>,
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
        items(logs) { log ->
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
private fun ShellBottomBar(
    pinnedSuggestions: List<Suggestion>,
    suggestions: List<Suggestion>,
    mergeAction: MergeAction,
    promptText: TextFieldValue,
    mode: ShellMode,
    isIdle: Boolean,
    onFieldTextChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
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
        ShellTextField(mode, promptText, onFieldTextChange, onSubmit)
        AnimatedVisibility(isIdle && (suggestions.isNotEmpty() || (promptText.text.isEmpty() && pinnedSuggestions.isNotEmpty()))) {
            ShellSuggestionsBox(
                promptText.text,
                mergeAction,
                suggestions,
                pinnedSuggestions,
                onFieldTextChange,
                onSubmit
            )
        }
        AnimatedVisibility(visible = !isIdle) {
            LinearProgressIndicator(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ShellSuggestionsBox(
    promptText: String,
    mergeAction: MergeAction,
    suggestions: List<Suggestion>,
    pinned: List<Suggestion>,
    onFieldTextChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (promptText.isEmpty() && pinned.isNotEmpty()) {
            items(pinned) { suggestion ->
                SuggestionItem(promptText, mergeAction, suggestion, onFieldTextChange, onSubmit)
            }
            item { Text("●") }
        }
        items(suggestions) { suggestion ->
            SuggestionItem(promptText, mergeAction, suggestion, onFieldTextChange, onSubmit)
        }
    }
}

@Composable
private fun SuggestionItem(
    promptText: String,
    mergeAction: MergeAction,
    suggestion: Suggestion,
    onFieldTextChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit
) {
    SuggestionChip(
        onClick = {
            val text = when (mergeAction) {
                is MergeAction.Append -> {
                    if (promptText.isNotEmpty()) {
                        buildString {
                            append(promptText)
                            if (!promptText.last().isWhitespace())
                                append(' ')
                            append(suggestion.replacement)
                        }
                    } else suggestion.replacement
                }
                is MergeAction.Replace -> {
                    StringBuilder(promptText)
                        .replace(mergeAction.start, mergeAction.end, suggestion.replacement)
                        .toString()
                }
            }
            onFieldTextChange(TextFieldValue(text, TextRange(text.length, text.length)))
            if (suggestion.runnable) {
                onSubmit()
            }
        },
        label = { Text(suggestion.label, fontSize = 16.sp) }
    )
}

@Composable
private fun ShellTextField(
    mode: ShellMode,
    fieldText: TextFieldValue,
    onFieldTextChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
) {
    OutlinedTextField(
        placeholder = {
            Text(
                text = when (mode) {
                    is ShellMode.PromptMode -> mode.hint
                    is ShellMode.RegularMode -> "Enter a command..."
                },
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
            )
        },
        value = fieldText,
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
private fun ShellTopBar(onThemeChange: () -> Unit, isDark: Boolean) {
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
                    imageVector = if (isDark)
                        Icons.Outlined.LightMode
                    else
                        Icons.Outlined.DarkMode,
                    contentDescription = "Dark Theme Toggle",
                )
            }
        }
    )
}
