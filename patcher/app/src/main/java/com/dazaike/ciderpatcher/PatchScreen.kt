package com.dazaike.ciderpatcher

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dazaike.ciderpatcher.core.CiderPatcher
import com.dazaike.ciderpatcher.ui.AccentPicker
import com.dazaike.ciderpatcher.ui.ButtonSize
import com.dazaike.ciderpatcher.ui.ButtonVariant
import com.dazaike.ciderpatcher.ui.ConfirmDialog
import com.dazaike.ciderpatcher.ui.GhostRow
import com.dazaike.ciderpatcher.ui.GlassButton
import com.dazaike.ciderpatcher.ui.GlassIconButton
import com.dazaike.ciderpatcher.ui.GlassPasswordField
import com.dazaike.ciderpatcher.ui.GlassSegmented
import com.dazaike.ciderpatcher.ui.GlassSwitch
import com.dazaike.ciderpatcher.ui.GlassTextField
import com.dazaike.ciderpatcher.ui.HapticKind
import com.dazaike.ciderpatcher.ui.JourneyRail
import com.dazaike.ciderpatcher.ui.LocalHaptics
import com.dazaike.ciderpatcher.ui.LocalToasts
import com.dazaike.ciderpatcher.ui.PatchProgress
import com.dazaike.ciderpatcher.ui.PrismIcon
import com.dazaike.ciderpatcher.ui.PrismIcons
import com.dazaike.ciderpatcher.ui.SecretField
import com.dazaike.ciderpatcher.ui.SectionLabel
import com.dazaike.ciderpatcher.ui.SheetOverlay
import com.dazaike.ciderpatcher.ui.ToastKind
import com.dazaike.ciderpatcher.ui.TooltipBox
import com.dazaike.ciderpatcher.ui.rememberSyncedFieldState
import com.dazaike.ciderpatcher.ui.theme.LocalMotion
import com.dazaike.ciderpatcher.ui.theme.Prism
import com.dazaike.ciderpatcher.ui.theme.PrismText
import com.dazaike.ciderpatcher.ui.theme.accentAlpha
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle

private val APK_MIME_TYPES = arrayOf("application/vnd.android.package-archive", "application/octet-stream")

private enum class Stage { PATCH, INSTALL, SIGN_IN }

private enum class Hero { NEEDS_APK, PATCH, READY, PATCHING, FAILED, INSTALL, SIGN_IN, SIGNED_IN }

private fun stageOf(state: PatchState, patched: Boolean): Stage = when {
    state is PatchState.Done -> Stage.INSTALL
    state is PatchState.Idle && patched -> Stage.SIGN_IN
    else -> Stage.PATCH
}

private fun heroOf(state: PatchState, patched: Boolean, signedIn: Boolean, ciderInstalled: Boolean): Hero = when (state) {
    is PatchState.Patching -> Hero.PATCHING
    is PatchState.Failed -> Hero.FAILED
    is PatchState.Ready -> Hero.READY
    is PatchState.Done -> Hero.INSTALL
    PatchState.Idle -> when {
        patched -> if (signedIn) Hero.SIGNED_IN else Hero.SIGN_IN
        ciderInstalled -> Hero.PATCH
        else -> Hero.NEEDS_APK
    }
}

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
    val sidConfig = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        vm.onSidConfigResult(result.resultCode)
    }
    LaunchedEffect(vm.sidConfigRequest) {
        val (key, model) = vm.sidConfigRequest ?: return@LaunchedEffect
        vm.sidConfigLaunched()
        try {
            sidConfig.launch(CiderApp.sidConfigIntent(key, model))
        } catch (_: ActivityNotFoundException) {
            vm.onSidConfigLaunchFailed()
        }
    }
    val working = state is PatchState.Patching || vm.busy

    val colors = Prism.colors
    val motion = LocalMotion.current
    val toasts = LocalToasts.current
    val haptics = LocalHaptics.current

    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showSid by rememberSaveable { mutableStateOf(false) }
    var showKeys by rememberSaveable { mutableStateOf(false) }

    val stage = stageOf(state, vm.patchedActivityExists)
    val hero = heroOf(state, vm.patchedActivityExists, vm.signedIn, vm.ciderInstalled)

    // Messages are shown as toasts once the work behind them is finished. Long instructions need longer to read.
    LaunchedEffect(vm.message, vm.messageIsError, vm.busy) {
        val m = vm.message ?: return@LaunchedEffect
        if (vm.busy) return@LaunchedEffect
        toasts.show(
            m,
            if (vm.messageIsError) ToastKind.Error else ToastKind.Info,
            durationMs = (2500L + m.length * 45L).coerceAtMost(9000L),
        )
        vm.consumeMessage()
    }
    LaunchedEffect(hero) {
        when (hero) {
            Hero.INSTALL -> haptics.perform(HapticKind.Success)
            Hero.FAILED -> haptics.perform(HapticKind.Error)
            else -> Unit
        }
    }
    LaunchedEffect(vm.signInOutcome) {
        if (vm.signInOutcome == SignInOutcome.SUCCESS) haptics.perform(HapticKind.Success)
    }

    // pageBackdrop = flat decorative background only (what page glass refracts).
    // sheetBackdrop = the whole screen (what sheets refract).
    val pageBackdrop = rememberLayerBackdrop()
    val sheetBackdrop = rememberLayerBackdrop()

    Box(Modifier.fillMaxSize().background(Prism.background)) {
        Box(Modifier.fillMaxSize().layerBackdrop(sheetBackdrop)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .layerBackdrop(pageBackdrop)
                    .background(colors.background),
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .imePadding()
                    .padding(horizontal = 16.dp),
            ) {
                Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                    PrismText(
                        "Cider Patcher",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    TooltipBox("Settings") {
                        GlassIconButton(pageBackdrop, PrismIcons.Settings, "Settings", { showSettings = true })
                    }
                }
                JourneyRail(current = stage.ordinal, allDone = hero == Hero.SIGNED_IN)
                Spacer(Modifier.height(20.dp))

                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    AnimatedContent(
                        targetState = hero,
                        transitionSpec = {
                            fadeIn(motion.fade(220, delayMs = 60)) togetherWith fadeOut(motion.fade(120)) using
                                SizeTransform(clip = false) { _, _ -> motion.settle() }
                        },
                        label = "hero",
                    ) { h ->
                        HeroBlock(h, state, vm, pageBackdrop)
                    }
                    if (hero != Hero.PATCHING) {
                        Spacer(Modifier.height(28.dp))
                        SectionLabel("More")
                        Spacer(Modifier.height(8.dp))
                        GhostRow(
                            "Sid",
                            if (vm.savedGeminiKey != null) {
                                "Cloud Gemini · ${vm.savedGeminiModel ?: "gemini-flash-latest"}"
                            } else {
                                "Off"
                            },
                            { showSid = true },
                        )
                        GhostRow(
                            "Saved keys",
                            "License key and token, encrypted on this phone",
                            { showKeys = true },
                        )
                        if (vm.originalLabel != null && vm.patchedActivityExists) {
                            GhostRow(
                                "Revert to original Cider",
                                "Saved: ${vm.originalLabel}",
                                { vm.requestRevert() },
                                enabled = !vm.busy && !vm.signingIn,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                // Pinned outside the scroll area so the buttons never scroll away.
                AnimatedContent(
                    targetState = hero,
                    transitionSpec = {
                        fadeIn(motion.fade(220, delayMs = 60)) togetherWith fadeOut(motion.fade(120)) using
                            SizeTransform(clip = false) { _, _ -> motion.settle() }
                    },
                    label = "heroActions",
                ) { h ->
                    HeroActions(
                        h, vm, state, working, pageBackdrop,
                        onPick = { pickApk.launch(APK_MIME_TYPES) },
                        onSave = { saveApk.launch(vm.suggestedFileName) },
                    )
                }
            }
        }

        // Sheets last so their BackHandlers win; imePadding keeps fields above the keyboard.
        Box(Modifier.fillMaxSize().imePadding()) {
            SettingsSheet(sheetBackdrop, showSettings, vm) { showSettings = false }
            SidSheet(sheetBackdrop, showSid, vm) { showSid = false }
            KeysSheet(sheetBackdrop, showKeys, vm) { showKeys = false }
        }

        ConfirmDialog(
            visible = vm.confirmRevert,
            title = "Revert to original Cider?",
            message = "This uninstalls the patched Cider and reinstalls ${vm.originalLabel.orEmpty()}. " +
                "Cider's data is deleted, so you'll sign in again afterwards.",
            confirmLabel = "Revert",
            destructive = true,
            onConfirm = vm::confirmRevertAndInstall,
            onDismiss = vm::dismissRevert,
            confirmLoading = vm.reverting,
        )
        ConfirmDialog(
            visible = vm.confirmUninstall,
            title = "Replace installed Cider?",
            message = "Installed Cider is signed with a different key. It must be uninstalled first — " +
                "this deletes its data and you'll sign in with your token again." +
                if (vm.installMethod == InstallMethod.SYSTEM) {
                    "\n\nAndroid will ask you to confirm the uninstall, then the install."
                } else {
                    ""
                },
            confirmLabel = "Uninstall & install",
            destructive = true,
            onConfirm = vm::confirmUninstallAndInstall,
            onDismiss = vm::dismissUninstall,
        )
        // Keep the last outcome so the text doesn't blank while the dialog fades out.
        val lastOutcome = remember { arrayOfNulls<SignInOutcome>(1) }
        vm.signInOutcome?.let { lastOutcome[0] = it }
        val outcome = lastOutcome[0]
        if (outcome != null) {
            val copy = outcomeCopy(outcome)
            ConfirmDialog(
                visible = vm.signInOutcome != null,
                title = copy.title,
                message = copy.body,
                confirmLabel = if (copy.offerOpen) "Open Cider" else "OK",
                onConfirm = if (copy.offerOpen) {
                    { vm.openCider(); vm.dismissSignInOutcome() }
                } else {
                    vm::dismissSignInOutcome
                },
                onDismiss = vm::dismissSignInOutcome,
                dismissLabel = if (copy.offerOpen) "Close" else null,
            )
        }
    }
}

// ---- Hero ------------------------------------------------------------------------------------

private class HeroCopy(
    val eyebrow: String,
    val title: String,
    val body: String? = null,
    val bodyIsError: Boolean = false,
    val badge: Boolean = false,
)

private fun heroCopy(hero: Hero, state: PatchState): HeroCopy = when (hero) {
    Hero.NEEDS_APK -> HeroCopy("STEP 1 OF 3", "Patch Cider", "Cider isn't installed. Choose a Cider APK file to patch.")
    Hero.PATCH -> HeroCopy(
        "STEP 1 OF 3", "Patch Cider",
        "Adds token sign-in and cloud Sid to the Cider installed on this phone.",
    )
    Hero.READY -> HeroCopy("STEP 1 OF 3", "Ready to patch", (state as? PatchState.Ready)?.label)
    Hero.PATCHING -> HeroCopy("STEP 1 OF 3", "Patching…")
    Hero.FAILED -> HeroCopy(
        "STEP 1 OF 3", "Patching failed",
        (state as? PatchState.Failed)?.message, bodyIsError = true,
    )
    Hero.INSTALL -> HeroCopy(
        "STEP 2 OF 3", "Patched APK ready",
        "Install it to replace the Cider on this phone.", badge = true,
    )
    Hero.SIGN_IN -> HeroCopy(
        "STEP 3 OF 3", "Sign in",
        "Sign in to your Cider account once so Cider can check your token with Apple, " +
            "then paste your Music-User-Token.",
    )
    Hero.SIGNED_IN -> HeroCopy("ALL SET", "You're signed in", "Cider accepted your token.", badge = true)
}

@Composable
private fun HeroBlock(hero: Hero, state: PatchState, vm: PatchViewModel, pageBackdrop: Backdrop) {
    val colors = Prism.colors
    val copy = heroCopy(hero, state)
    Column(Modifier.fillMaxWidth()) {
        if (copy.badge) {
            Box(
                Modifier
                    .size(56.dp)
                    .background(Prism.accent.copy(alpha = colors.accentAlpha(0.85f)), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                PrismIcon(PrismIcons.Check, null, size = 28.dp, tint = Prism.onAccent)
            }
            Spacer(Modifier.height(16.dp))
        }
        PrismText(
            copy.eyebrow,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = colors.subText,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(6.dp))
        PrismText(copy.title, fontSize = 34.sp, fontWeight = FontWeight.Light)
        if (copy.body != null) {
            Spacer(Modifier.height(8.dp))
            PrismText(copy.body, fontSize = 16.sp, color = if (copy.bodyIsError) colors.error else colors.subText)
        }
        Spacer(Modifier.height(20.dp))
        when (hero) {
            Hero.PATCHING -> {
                val p = state as? PatchState.Patching
                PatchProgress(p?.step, p?.symbols ?: 0)
            }
            Hero.FAILED -> {
                val log = (state as? PatchState.Failed)?.log.orEmpty()
                if (log.isNotEmpty()) LogDetails(log)
            }
            Hero.INSTALL -> {
                val log = (state as? PatchState.Done)?.log.orEmpty()
                val warnings = buildList {
                    if (vm.installMethod == InstallMethod.SHIZUKU && !vm.shizukuRunning) add(ShizukuInstaller.NOT_RUNNING)
                    if (vm.advanced && vm.signerMismatch) {
                        add("Uninstall the current Cider before installing a saved copy of this file.")
                    }
                    log.firstOrNull { it.startsWith(CiderPatcher.SID_LOG_SKIPPED_PREFIX) }?.let { add(it) }
                }
                if (warnings.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { warnings.forEach { Warning(it) } }
                    Spacer(Modifier.height(12.dp))
                }
                if (log.isNotEmpty()) LogDetails(log)
            }
            Hero.SIGN_IN -> TokenField(vm, pageBackdrop)
            else -> Unit
        }
    }
}

@Composable
private fun Warning(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        PrismIcon(PrismIcons.Alert, null, modifier = Modifier.padding(top = 2.dp), size = 16.dp, tint = Prism.colors.error)
        PrismText(text, fontSize = 14.sp)
    }
}

@Composable
private fun TokenField(vm: PatchViewModel, backdrop: Backdrop) {
    val motion = LocalMotion.current
    val tokenState = rememberSyncedFieldState(vm.token) { vm.token = it }
    Column {
        GlassPasswordField(tokenState, "Music-User-Token", Modifier.fillMaxWidth(), onSubmit = { vm.signIn() })
        AnimatedVisibility(
            visible = vm.savedToken != null && vm.token != vm.savedToken,
            enter = expandVertically(motion.settle()) + fadeIn(motion.fade(220)),
            exit = shrinkVertically(motion.exit(200)) + fadeOut(motion.fade(120)),
        ) {
            Column {
                Spacer(Modifier.height(8.dp))
                GlassButton(
                    backdrop, "Use saved token", vm::useSavedToken,
                    variant = ButtonVariant.Ghost, size = ButtonSize.Small,
                )
            }
        }
    }
}

@Composable
private fun LogDetails(lines: List<String>) {
    val colors = Prism.colors
    val motion = LocalMotion.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        GhostRow(
            "Details", null, { expanded = !expanded },
            trailingIcon = if (expanded) PrismIcons.ChevronUp else PrismIcons.ChevronDown,
        )
        AnimatedVisibility(
            expanded,
            enter = expandVertically(motion.settle()) + fadeIn(motion.fade(220)),
            exit = shrinkVertically(motion.exit(200)) + fadeOut(motion.fade(120)),
        ) {
            val listState = rememberLazyListState()
            LaunchedEffect(lines.size) {
                if (lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex)
            }
            LazyColumn(
                Modifier
                    .heightIn(max = 260.dp)
                    .fillMaxWidth()
                    .clip(RoundedRectangle(14.dp))
                    .background(colors.fillWeak)
                    .padding(10.dp),
                state = listState,
            ) {
                items(lines) { line ->
                    BasicText(
                        line,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = colors.subText,
                        ),
                    )
                }
            }
        }
    }
}

// ---- Pinned actions --------------------------------------------------------------------------

@Composable
private fun HeroActions(
    hero: Hero,
    vm: PatchViewModel,
    state: PatchState,
    working: Boolean,
    backdrop: Backdrop,
    onPick: () -> Unit,
    onSave: () -> Unit,
) {
    @Composable
    fun Primary(text: String, onClick: () -> Unit, enabled: Boolean = true, loading: Boolean = false) {
        GlassButton(
            backdrop, text, onClick, Modifier.fillMaxWidth(),
            variant = ButtonVariant.Primary, size = ButtonSize.Large, enabled = enabled, loading = loading,
        )
    }

    @Composable
    fun Secondary(
        text: String,
        onClick: () -> Unit,
        enabled: Boolean = true,
        trailingIcon: Int? = null,
    ) {
        GlassButton(
            backdrop, text, onClick, Modifier.fillMaxWidth(),
            variant = ButtonVariant.Ghost, enabled = enabled, trailingIcon = trailingIcon,
        )
    }

    Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val status = vm.message
        if (vm.busy && status != null) {
            PrismText(status, fontSize = 14.sp, color = Prism.colors.subText)
        }
        when (hero) {
            Hero.NEEDS_APK -> Primary("Choose APK file", onPick, enabled = !working)
            Hero.PATCH -> {
                Primary("Patch Cider", vm::patchInstalledCider, enabled = !working)
                if (vm.advanced) Secondary("Choose APK file", onPick, enabled = !working)
            }
            Hero.READY -> {
                Primary("Patch", vm::patch)
                Secondary("Cancel", vm::reset)
            }
            Hero.PATCHING -> Primary("Patching…", {}, enabled = false, loading = true)
            Hero.FAILED -> Primary("Try again", vm::reset)
            Hero.INSTALL -> {
                Primary(
                    if (vm.installMethod == InstallMethod.SHIZUKU) "Install via Shizuku" else "Install",
                    vm::install,
                    enabled = !working,
                    loading = vm.busy,
                )
                if (vm.advanced) Secondary("Save APK", onSave, enabled = !working)
                Secondary("Start over", vm::reset, enabled = !working)
            }
            Hero.SIGN_IN -> {
                Primary(
                    "Sign in to Cider",
                    vm::signIn,
                    enabled = vm.signingIn || (vm.token.isNotBlank() && vm.patchedActivityExists && !vm.busy),
                    loading = vm.signingIn,
                )
                Secondary(
                    "Open Cider", { vm.openCider() },
                    enabled = vm.ciderInstalled, trailingIcon = PrismIcons.ArrowRight,
                )
            }
            Hero.SIGNED_IN -> {
                Primary("Open Cider", { vm.openCider() })
                Secondary("Sign in with another token", vm::signInAgain)
            }
        }
    }
}

// ---- Sign-in outcome copy --------------------------------------------------------------------

private class OutcomeCopy(val title: String, val body: String, val offerOpen: Boolean)

private fun outcomeCopy(outcome: SignInOutcome): OutcomeCopy = when (outcome) {
    SignInOutcome.SUCCESS -> OutcomeCopy(
        "You're signed in",
        "Cider accepted your token and restarted with your library.",
        offerOpen = true,
    )
    SignInOutcome.INVALID -> OutcomeCopy(
        "Token rejected",
        "Apple says this Music-User-Token isn't valid or has expired. Copy a fresh media-user-token " +
            "from a signed-in music.apple.com session (or Cider desktop) and try again.",
        offerOpen = false,
    )
    SignInOutcome.UNVERIFIED -> OutcomeCopy(
        "Couldn't check your token",
        "Cider couldn't ask Apple about your token. This usually means Cider isn't signed in to your " +
            "Cider account yet, which it needs to talk to Apple Music.\n\nOpen Cider, sign in to your Cider " +
            "account, then come back and tap Sign in to Cider again. This can also happen when you're offline.",
        offerOpen = true,
    )
    SignInOutcome.CANCELLED -> OutcomeCopy(
        "Sign-in didn't finish",
        "Cider closed before it could report back. Try again. If it keeps happening, make sure a battery " +
            "saver or app killer (like Brevent) isn't stopping Cider.",
        offerOpen = false,
    )
    SignInOutcome.NOT_INSTALLED -> OutcomeCopy(
        "Patched Cider isn't installed",
        "Install the patched Cider first, then sign in.",
        offerOpen = false,
    )
}

// ---- Sheets ----------------------------------------------------------------------------------

@Composable
private fun SheetTitle(text: String) {
    PrismText(text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun SettingRow(label: String, caption: String?, control: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            PrismText(label, fontSize = 18.sp)
            if (caption != null) PrismText(caption, fontSize = 13.sp, color = Prism.colors.subText)
        }
        control()
    }
}

@Composable
private fun BoxScope.SettingsSheet(backdrop: Backdrop, visible: Boolean, vm: PatchViewModel, onDismiss: () -> Unit) {
    SheetOverlay(backdrop, visible, onDismiss, heightFraction = 0.7f) { surface ->
        val motion = LocalMotion.current
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SheetTitle("Settings")
            SettingRow("Advanced options", "Choose an APK file and save the patched APK") {
                GlassSwitch(vm.advanced, vm::updateAdvanced, contentDescription = "Advanced options")
            }
            AnimatedVisibility(
                visible = vm.advanced,
                enter = expandVertically(motion.settle()) + fadeIn(motion.fade(220)),
                exit = shrinkVertically(motion.exit(200)) + fadeOut(motion.fade(120)),
            ) {
                Column {
                    Spacer(Modifier.height(28.dp))
                    PrismText("Install with", fontSize = 18.sp)
                    Spacer(Modifier.height(10.dp))
                    GlassSegmented(
                        listOf("Shizuku", "Android installer"),
                        selectedIndex = vm.installMethod.ordinal,
                        onSelect = { vm.updateInstallMethod(InstallMethod.entries[it]) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    PrismText(
                        if (vm.installMethod == InstallMethod.SHIZUKU) {
                            "Installs silently, no prompts"
                        } else {
                            "Asks you to confirm, like opening an APK file"
                        },
                        fontSize = 13.sp,
                        color = Prism.colors.subText,
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            SettingRow(
                "Open Cider after install",
                "Copies your saved license key and opens Cider after a Shizuku install",
            ) {
                GlassSwitch(
                    vm.autoOpenAfterInstall,
                    vm::updateAutoOpenAfterInstall,
                    contentDescription = "Open Cider after install",
                )
            }
            Spacer(Modifier.height(28.dp))
            SettingRow("Haptics", null) {
                GlassSwitch(vm.ui.haptics, { v -> vm.updateUi { it.copy(haptics = v) } }, contentDescription = "Haptics")
            }
            Spacer(Modifier.height(28.dp))
            PrismText("Accent colour", fontSize = 18.sp)
            Spacer(Modifier.height(10.dp))
            AccentPicker(surface, Color(vm.ui.accent)) { c -> vm.updateUi { it.copy(accent = c.toArgb()) } }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun BoxScope.SidSheet(backdrop: Backdrop, visible: Boolean, vm: PatchViewModel, onDismiss: () -> Unit) {
    SheetOverlay(backdrop, visible, onDismiss, heightFraction = 0.8f) { surface ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SheetTitle("Sid")
            PrismText(
                "Sid needs Gemini Nano, which this phone may not have. Add a Google AI Studio API key and Sid uses " +
                    "Gemini in the cloud instead. What you ask Sid is sent to Google. Leave the key blank and send to turn it off.",
                fontSize = 14.sp,
                color = Prism.colors.subText,
            )
            Spacer(Modifier.height(20.dp))
            key(vm.savedGeminiKey, vm.savedGeminiModel) {
                val keyState = rememberTextFieldState(vm.savedGeminiKey.orEmpty())
                val modelState = rememberTextFieldState(vm.savedGeminiModel.orEmpty())
                val model = modelState.text.toString()
                val modelValid = model.isBlank() || model.trim().matches(Regex("[A-Za-z0-9._-]+"))
                SecretField(surface, "Gemini API key", keyState)
                Spacer(Modifier.height(12.dp))
                GlassTextField(
                    modelState,
                    "Model id",
                    Modifier.fillMaxWidth(),
                    placeholder = "gemini-flash-latest",
                    error = if (modelValid) null else "Letters, digits, '.', '_' and '-' only",
                )
                Spacer(Modifier.height(24.dp))
                if (!vm.patchedActivityExists) {
                    PrismText("Install the patched Cider first.", fontSize = 13.sp, color = Prism.colors.subText)
                    Spacer(Modifier.height(8.dp))
                }
                GlassButton(
                    surface,
                    if (vm.sendingSidConfig) "Sending…" else "Save & send to Cider",
                    { vm.sendSidConfig(keyState.text.toString(), modelState.text.toString()) },
                    Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Primary,
                    size = ButtonSize.Large,
                    loading = vm.sendingSidConfig,
                    enabled = vm.patchedActivityExists && !vm.sendingSidConfig && modelValid,
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun BoxScope.KeysSheet(backdrop: Backdrop, visible: Boolean, vm: PatchViewModel, onDismiss: () -> Unit) {
    SheetOverlay(backdrop, visible, onDismiss, heightFraction = 0.7f) { surface ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SheetTitle("Saved keys")
            PrismText(
                "Optional. Keys are encrypted and stay on this phone.",
                fontSize = 14.sp,
                color = Prism.colors.subText,
            )
            Spacer(Modifier.height(20.dp))
            key(vm.savedLicenseKey, vm.savedToken) {
                val license = rememberTextFieldState(vm.savedLicenseKey.orEmpty())
                val token = rememberTextFieldState(vm.savedToken.orEmpty())
                SecretField(surface, "Cider license key", license)
                Spacer(Modifier.height(12.dp))
                SecretField(surface, "Music-User-Token", token)
                Spacer(Modifier.height(24.dp))
                val changed = license.text.toString().trim() != vm.savedLicenseKey.orEmpty() ||
                    token.text.toString().trim() != vm.savedToken.orEmpty()
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton(
                        surface, "Save",
                        { vm.saveKeys(license.text.toString(), token.text.toString()) },
                        variant = ButtonVariant.Primary,
                        enabled = changed,
                    )
                    if (vm.savedLicenseKey != null || vm.savedToken != null) {
                        GlassButton(
                            surface, "Clear",
                            {
                                license.clearText()
                                token.clearText()
                                vm.saveKeys("", "")
                            },
                            variant = ButtonVariant.Outlined,
                            leadingIcon = PrismIcons.Trash,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
