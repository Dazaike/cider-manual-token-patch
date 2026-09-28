package com.dazaike.ciderpatcher

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import androidx.compose.animation.scaleOut
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.VisualTransformation
import com.dazaike.ciderpatcher.ui.SquircleIconButton
import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dazaike.ciderpatcher.core.CiderPatcher
import com.dazaike.ciderpatcher.ui.ButtonStyle
import com.dazaike.ciderpatcher.ui.PatchProgress
import com.dazaike.ciderpatcher.ui.PillSquircle
import com.dazaike.ciderpatcher.ui.SectionCard
import com.dazaike.ciderpatcher.ui.SquircleButton
import com.dazaike.ciderpatcher.ui.SquircleShape
import com.dazaike.ciderpatcher.ui.SuccessBurst
import com.dazaike.ciderpatcher.ui.shake

private val APK_MIME_TYPES = arrayOf("application/vnd.android.package-archive", "application/octet-stream")

@Composable
fun PatchScreen(vm: PatchViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pickApk = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::pickFile)
    }
    val saveApk = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri -> uri?.let(vm::save) }
    val signIn = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        vm.onSignInResult(result.resultCode, result.data?.getStringExtra(CiderPatcher.RESULT_EXTRA))
    }
    LaunchedEffect(vm.signInRequest) {
        val token = vm.signInRequest ?: return@LaunchedEffect
        vm.signInLaunched()
        try {
            signIn.launch(CiderApp.signInIntent(token))
        } catch (_: ActivityNotFoundException) {
            vm.onSignInLaunchFailed()
        }
    }
    val working = state is PatchState.Patching || vm.busy

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EnterFromTop(0) {
                Header(
                    advanced = vm.advanced,
                    onAdvancedChange = vm::updateAdvanced,
                    installMethod = vm.installMethod,
                    onInstallMethodChange = vm::updateInstallMethod,
                )
            }

            EnterFromTop(1) {
                AnimatedVisibility(
                    visible = vm.message != null,
                    enter = expandVertically(spring(dampingRatio = 0.8f)) + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    StatusBanner(vm.message.orEmpty(), vm.busy, vm.messageIsError)
                }
            }

            EnterFromTop(2) {
                SectionCard(1, "Choose Cider") {
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SquircleButton(
                            "Use installed Cider",
                            onClick = vm::useInstalledCider,
                            icon = Icons.Rounded.PhoneAndroid,
                            enabled = vm.ciderInstalled && !working,
                        )
                        AnimatedVisibility(vm.advanced, enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn(), exit = fadeOut()) {
                            SquircleButton(
                                "Choose APK",
                                onClick = { pickApk.launch(APK_MIME_TYPES) },
                                icon = Icons.Rounded.FolderOpen,
                                style = ButtonStyle.Tonal,
                                enabled = !working,
                            )
                        }
                    }
                    AnimatedVisibility(!vm.ciderInstalled && !vm.advanced) {
                        Hint(
                            Icons.Rounded.Info,
                            "Install Cider first, or turn on Advanced options (top right) to choose an APK file.",
                        )
                    }
                }
            }

            EnterFromTop(3) {
                SectionCard(2, "Patch") {
                    AnimatedContent(
                        targetState = state,
                        contentKey = { it::class },
                        transitionSpec = {
                            (fadeIn(tween(260, delayMillis = 80)) + slideInVertically(spring(dampingRatio = 0.8f)) { it / 6 }) togetherWith
                                fadeOut(tween(140))
                        },
                        label = "patchState",
                    ) { s ->
                        PatchSection(
                            state = s,
                            working = working,
                            advanced = vm.advanced,
                            signerMismatch = vm.signerMismatch,
                            installMethod = vm.installMethod,
                            shizukuRunning = vm.shizukuRunning,
                            onPatch = vm::patch,
                            onSave = { saveApk.launch(vm.suggestedFileName) },
                            onInstall = vm::install,
                        )
                    }
                }
            }

            EnterFromTop(4) {
                SectionCard(3, "Sign in to Cider") {
                    AnimatedVisibility(
                        visible = vm.patchedActivityExists,
                        enter = expandVertically(spring(dampingRatio = 0.7f)) + scaleIn(initialScale = 0.8f) + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        SquircleButton(
                            "Open Cider",
                            onClick = { vm.openCider() },
                            icon = Icons.AutoMirrored.Rounded.OpenInNew,
                            style = ButtonStyle.Tonal,
                        )
                    }
                    Hint(
                        Icons.Rounded.Info,
                        "Open Cider once and sign in to your Cider account first. Cider needs it to check your token with Apple.",
                    )
                    OutlinedTextField(
                        value = vm.token,
                        onValueChange = { vm.token = it },
                        label = { Text("Music-User-Token") },
                        leadingIcon = { Icon(Icons.Rounded.Key, null) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AnimatedVisibility(
                        visible = vm.savedToken != null && vm.token != vm.savedToken,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        SquircleButton(
                            "Use saved token",
                            onClick = vm::useSavedToken,
                            icon = Icons.Rounded.Password,
                            style = ButtonStyle.Tonal,
                        )
                    }
                    SquircleButton(
                        if (vm.signingIn) "Signing in…" else "Sign in to Cider",
                        onClick = vm::signIn,
                        icon = Icons.AutoMirrored.Rounded.Login,
                        enabled = vm.signingIn || (vm.token.isNotBlank() && vm.patchedActivityExists && !vm.busy),
                        loading = vm.signingIn,
                    )
                    AnimatedVisibility(!vm.patchedActivityExists) {
                        Text(
                            "Install a patched Cider first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = vm.originalLabel != null && vm.patchedActivityExists,
                enter = expandVertically(spring(dampingRatio = 0.8f)) + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                SectionCard(4, "Cider updates") {
                    Hint(
                        Icons.Rounded.Info,
                        "Cider's updates are signed by its developers, so they can't install over the patched app. " +
                            "Revert to the original first, update Cider, then patch it again here.",
                    )
                    Text(
                        "Saved original: ${vm.originalLabel.orEmpty()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SquircleButton(
                        "Revert to original",
                        onClick = vm::requestRevert,
                        icon = Icons.Rounded.Restore,
                        style = ButtonStyle.Outlined,
                        enabled = !vm.busy && !vm.signingIn,
                        loading = vm.reverting,
                    )
                }
            }

            EnterFromTop(5) {
                SavedKeysCard(
                    savedLicense = vm.savedLicenseKey,
                    savedToken = vm.savedToken,
                    onSave = vm::saveKeys,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (vm.confirmRevert) {
        AlertDialog(
            onDismissRequest = vm::dismissRevert,
            icon = { Icon(Icons.Rounded.Restore, null) },
            title = { Text("Revert to original Cider?") },
            text = {
                Text(
                    "This uninstalls the patched Cider and reinstalls ${vm.originalLabel.orEmpty()}. " +
                        "Cider's data is deleted, so you'll sign in again afterwards."
                )
            },
            confirmButton = {
                TextButton(onClick = vm::confirmRevertAndInstall, shape = PillSquircle) { Text("Revert") }
            },
            dismissButton = { TextButton(onClick = vm::dismissRevert, shape = PillSquircle) { Text("Cancel") } },
        )
    }

    if (vm.confirmUninstall) {
        AlertDialog(
            onDismissRequest = vm::dismissUninstall,
            icon = { Icon(Icons.Rounded.Warning, null) },
            title = { Text("Replace installed Cider?") },
            text = {
                Text(
                    "Installed Cider is signed with a different key. It must be uninstalled first — " +
                        "this deletes its data and you'll sign in with your token again." +
                        if (vm.installMethod == InstallMethod.SYSTEM) {
                            "\n\nAndroid will ask you to confirm the uninstall, then the install."
                        } else {
                            ""
                        }
                )
            },
            confirmButton = {
                TextButton(onClick = vm::confirmUninstallAndInstall, shape = PillSquircle) { Text("Uninstall & install") }
            },
            dismissButton = { TextButton(onClick = vm::dismissUninstall, shape = PillSquircle) { Text("Cancel") } },
        )
    }

    vm.signInOutcome?.let { outcome ->
        SignInOutcomeDialog(outcome, onOpenCider = { vm.openCider() }, onDismiss = vm::dismissSignInOutcome)
    }
}

@Composable
private fun PatchSection(
    state: PatchState,
    working: Boolean,
    advanced: Boolean,
    signerMismatch: Boolean,
    installMethod: InstallMethod,
    shizukuRunning: Boolean,
    onPatch: () -> Unit,
    onSave: () -> Unit,
    onInstall: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (state) {
            PatchState.Idle -> Hint(Icons.Rounded.Info, "Choose Cider above to get started.")
            is PatchState.Ready -> {
                Hint(Icons.Rounded.Description, state.label)
                SquircleButton("Patch", onClick = onPatch, icon = Icons.Rounded.AutoFixHigh)
            }
            is PatchState.Patching -> PatchProgress(state.step, state.symbols)
            is PatchState.Done -> {
                SuccessBurst()
                Text(
                    "Patched APK ready",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SquircleButton(
                        if (installMethod == InstallMethod.SHIZUKU) "Install via Shizuku" else "Install",
                        onClick = onInstall,
                        icon = Icons.Rounded.InstallMobile,
                        enabled = !working,
                    )
                    AnimatedVisibility(advanced, enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn(), exit = fadeOut()) {
                        SquircleButton(
                            "Save APK",
                            onClick = onSave,
                            icon = Icons.Rounded.Save,
                            style = ButtonStyle.Outlined,
                            enabled = !working,
                        )
                    }
                }
                AnimatedVisibility(
                    visible = installMethod == InstallMethod.SHIZUKU && !shizukuRunning,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    ErrorHint(ShizukuInstaller.NOT_RUNNING)
                }
                AnimatedVisibility(advanced && signerMismatch) {
                    Hint(Icons.Rounded.Warning, "Uninstall the current Cider before installing a saved copy of this file.")
                }
                LogDetails(state.log)
            }
            is PatchState.Failed -> {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .shake(state.message)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(12.dp))
                    Text(state.message, color = MaterialTheme.colorScheme.onErrorContainer)
                }
                if (state.log.isNotEmpty()) LogDetails(state.log)
            }
        }
    }
}

@Composable
private fun Header(
    advanced: Boolean,
    onAdvancedChange: (Boolean) -> Unit,
    installMethod: InstallMethod,
    onInstallMethodChange: (InstallMethod) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val gear by animateFloatAsState(if (menuOpen) 90f else 0f, spring(dampingRatio = 0.5f), label = "settingsGear")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Cider Patcher", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Adds token sign-in to Cider, right on your phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            Surface(
                onClick = { menuOpen = true },
                shape = SquircleShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings", modifier = Modifier.rotate(gear))
                }
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                shape = MaterialTheme.shapes.medium,
            ) {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("Advanced options")
                            Text(
                                "Choose an APK file and save the patched APK",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = { onAdvancedChange(!advanced) },
                    leadingIcon = { Icon(Icons.Rounded.Tune, null) },
                    trailingIcon = { Switch(checked = advanced, onCheckedChange = onAdvancedChange) },
                )
                if (advanced) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text(
                        "Install with",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    InstallMethodItem(
                        title = "Shizuku",
                        subtitle = "Installs silently, no prompts",
                        selected = installMethod == InstallMethod.SHIZUKU,
                        onClick = { onInstallMethodChange(InstallMethod.SHIZUKU) },
                    )
                    InstallMethodItem(
                        title = "Android installer",
                        subtitle = "Asks you to confirm, like opening an APK file",
                        selected = installMethod == InstallMethod.SYSTEM,
                        onClick = { onInstallMethodChange(InstallMethod.SYSTEM) },
                    )
                }
            }
        }
    }
}

@Composable
private fun InstallMethodItem(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Column {
                Text(title)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        onClick = onClick,
        leadingIcon = { RadioButton(selected = selected, onClick = onClick) },
    )
}

@Composable
private fun StatusBanner(message: String, busy: Boolean, error: Boolean) {
    val container = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    val content = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
    Column(
        Modifier
            .fillMaxWidth()
            .shake(if (error) message else null)
            .clip(MaterialTheme.shapes.medium)
            .background(container)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnimatedContent(
            targetState = message,
            transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut()) },
            label = "statusMessage",
        ) { text ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (error) {
                    Icon(Icons.Rounded.ErrorOutline, null, tint = content)
                    Spacer(Modifier.width(12.dp))
                }
                Text(text, color = content)
            }
        }
        AnimatedVisibility(busy) {
            LinearProgressIndicator(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(PillSquircle),
            )
        }
    }
}

@Composable
private fun ErrorHint(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

/** Optional encrypted storage for the license key and token, with one-tap copy. */
@Composable
private fun SavedKeysCard(savedLicense: String?, savedToken: String?, onSave: (String, String) -> Unit) {
    var license by rememberSaveable(savedLicense) { mutableStateOf(savedLicense.orEmpty()) }
    var token by rememberSaveable(savedToken) { mutableStateOf(savedToken.orEmpty()) }
    val changed = license.trim() != savedLicense.orEmpty() || token.trim() != savedToken.orEmpty()
    val hasSaved = savedLicense != null || savedToken != null
    SectionCard(number = null, title = "Saved keys", icon = Icons.Rounded.Lock) {
        Hint(
            Icons.Rounded.Info,
            "Optional. Keep your Cider license key and Music-User-Token here to copy them quickly. " +
                "They're encrypted and stay on this phone.",
        )
        SecretField("Cider license key", license) { license = it }
        SecretField("Music-User-Token", token) { token = it }
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SquircleButton("Save", onClick = { onSave(license, token) }, icon = Icons.Rounded.Save, enabled = changed)
            AnimatedVisibility(hasSaved, enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn(), exit = fadeOut()) {
                SquircleButton(
                    "Clear",
                    onClick = {
                        license = ""
                        token = ""
                        onSave("", "")
                    },
                    icon = Icons.Rounded.Delete,
                    style = ButtonStyle.Outlined,
                )
            }
        }
    }
}

@Composable
private fun SecretField(label: String, value: String, onValueChange: (String) -> Unit) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            Row {
                SquircleIconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (visible) "Hide" else "Show",
                    )
                }
                SquircleIconButton(
                    onClick = {
                        copySensitive(context, label, value.trim())
                        copied = true
                    },
                    enabled = value.isNotBlank(),
                ) {
                    AnimatedContent(
                        targetState = copied,
                        transitionSpec = { (scaleIn(spring(dampingRatio = 0.4f)) + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                        label = "copyIcon",
                    ) { done ->
                        Icon(if (done) Icons.Rounded.Check else Icons.Rounded.ContentCopy, contentDescription = "Copy")
                    }
                }
            }
        },
    )
}

/** Copies [text] and flags it sensitive so Android's clipboard preview hides it. */
private fun copySensitive(context: Context, label: String, text: String) {
    val clip = ClipData.newPlainText(label, text)
    clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
}

@Composable
private fun Hint(icon: ImageVector, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LogDetails(lines: List<String>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val arrow by animateFloatAsState(if (expanded) 180f else 0f, spring(dampingRatio = 0.6f), label = "logArrow")
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Details", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ExpandMore, null, modifier = Modifier.rotate(arrow))
        }
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            val listState = rememberLazyListState()
            LaunchedEffect(lines.size) {
                if (lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex)
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(10.dp),
            ) {
                items(lines) { line -> Text(line, fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
            }
        }
    }
}

/** Fades and slides a block down into place, staggered by [index]. */
@Composable
private fun EnterFromTop(index: Int, content: @Composable () -> Unit) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn(tween(400, delayMillis = index * 80)) +
            slideInVertically(tween(500, delayMillis = index * 80, easing = FastOutSlowInEasing)) { -it / 4 },
    ) { content() }
}

@Composable
private fun SignInOutcomeDialog(outcome: SignInOutcome, onOpenCider: () -> Unit, onDismiss: () -> Unit) {
    data class Copy(val icon: ImageVector, val title: String, val body: String, val offerOpen: Boolean)
    val copy = when (outcome) {
        SignInOutcome.SUCCESS -> Copy(
            Icons.Rounded.CheckCircle,
            "You're signed in",
            "Cider accepted your token and restarted with your library.",
            offerOpen = true,
        )
        SignInOutcome.INVALID -> Copy(
            Icons.Rounded.ErrorOutline,
            "Token rejected",
            "Apple says this Music-User-Token isn't valid or has expired. Copy a fresh media-user-token " +
                "from a signed-in music.apple.com session (or Cider desktop) and try again.",
            offerOpen = false,
        )
        SignInOutcome.UNVERIFIED -> Copy(
            Icons.Rounded.Info,
            "Couldn't check your token",
            "Cider couldn't ask Apple about your token. This usually means Cider isn't signed in to your " +
                "Cider account yet, which it needs to talk to Apple Music.\n\nOpen Cider, sign in to your Cider " +
                "account, then come back and tap Sign in to Cider again. This can also happen when you're offline.",
            offerOpen = true,
        )
        SignInOutcome.CANCELLED -> Copy(
            Icons.Rounded.Warning,
            "Sign-in didn't finish",
            "Cider closed before it could report back. Try again. If it keeps happening, make sure a battery " +
                "saver or app killer (like Brevent) isn't stopping Cider.",
            offerOpen = false,
        )
        SignInOutcome.NOT_INSTALLED -> Copy(
            Icons.Rounded.Warning,
            "Patched Cider isn't installed",
            "Install the patched Cider first, then sign in.",
            offerOpen = false,
        )
    }
    val iconScale = remember { androidx.compose.animation.core.Animatable(0.4f) }
    LaunchedEffect(outcome) {
        iconScale.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                copy.icon,
                null,
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer {
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                    },
                tint = if (outcome == SignInOutcome.INVALID) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text(copy.title) },
        text = { Text(copy.body) },
        confirmButton = {
            if (copy.offerOpen) {
                TextButton(onClick = { onOpenCider(); onDismiss() }, shape = PillSquircle) { Text("Open Cider") }
            } else {
                TextButton(onClick = onDismiss, shape = PillSquircle) { Text("OK") }
            }
        },
        dismissButton = if (copy.offerOpen) {
            { TextButton(onClick = onDismiss, shape = PillSquircle) { Text("Close") } }
        } else {
            null
        },
    )
}
