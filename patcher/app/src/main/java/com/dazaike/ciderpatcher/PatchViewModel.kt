package com.dazaike.ciderpatcher

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dazaike.ciderpatcher.core.CiderPatcher
import com.dazaike.ciderpatcher.core.PatchException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import rikka.shizuku.Shizuku

sealed interface PatchState {
    data object Idle : PatchState
    data class Ready(val apk: File, val label: String) : PatchState
    data class Patching(val log: List<String>, val step: CiderPatcher.Step?, val symbols: Int) : PatchState
    data class Done(val output: File, val log: List<String>) : PatchState
    data class Failed(val message: String, val log: List<String>) : PatchState
}

/** What the patched activity reported back; shown as a popup. */
enum class SignInOutcome { SUCCESS, INVALID, UNVERIFIED, CANCELLED, NOT_INSTALLED }

class PatchViewModel(app: Application) : AndroidViewModel(app) {
    private val context get() = getApplication<Application>()
    private val inputApk get() = File(context.cacheDir, "input.apk")
    private val patchedApk get() = File(context.cacheDir, "patched.apk")
    /** Unpatched input of the last successful patch, promoted to [originalApk] once that patch installs. */
    private val originalCandidate get() = File(context.filesDir, "original-candidate.apk")
    /** Official Cider matching the installed patched build; reinstalled by [confirmRevertAndInstall]. */
    private val originalApk get() = File(context.filesDir, "original.apk")
    private val shizuku = ShizukuInstaller(app)
    private val system = SystemInstaller(app)
    private val installer: Installer get() = if (installMethod == InstallMethod.SHIZUKU) shizuku else system
    private val prefs = app.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    private val binderReceived = Shizuku.OnBinderReceivedListener { shizukuRunning = true }
    private val binderDead = Shizuku.OnBinderDeadListener { shizukuRunning = false }

    private val _state = MutableStateFlow<PatchState>(PatchState.Idle)
    val state: StateFlow<PatchState> = _state

    /** The token for the next sign-in; persisted only if the user saves it under Saved keys. */
    var token by mutableStateOf("")
    var ciderInstalled by mutableStateOf(false)
        private set
    var patchedActivityExists by mutableStateOf(false)
        private set
    /** Installed Cider is signed by a different key than this patcher's. */
    var signerMismatch by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var confirmUninstall by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    /** [message] reports a failure; shown as an error banner. */
    var messageIsError by mutableStateOf(false)
        private set
    /** Token to hand to the patched activity; the screen launches it for a result and clears this. */
    var signInRequest by mutableStateOf<String?>(null)
        private set
    var signInOutcome by mutableStateOf<SignInOutcome?>(null)
        private set
    /** The patched activity is running and hasn't reported back yet. */
    var signingIn by mutableStateOf(false)
        private set
    /** Shows Choose APK and Save; persisted. */
    var advanced by mutableStateOf(prefs.getBoolean(KEY_ADVANCED, false))
        private set
    /** "<package> <version> (<code>)" of the saved original, or null if none is saved. */
    var originalLabel by mutableStateOf<String?>(null)
        private set
    var confirmRevert by mutableStateOf(false)
        private set
    var reverting by mutableStateOf(false)
        private set
    /** Persisted; the UI shows exactly one install button, for this method. */
    var installMethod by mutableStateOf(
        runCatching { InstallMethod.valueOf(prefs.getString(KEY_INSTALL_METHOD, null) ?: "") }.getOrDefault(InstallMethod.SHIZUKU)
    )
        private set
    var shizukuRunning by mutableStateOf(Shizuku.pingBinder())
        private set
    /** Optional, Keystore-encrypted copies the user chose to keep for quick copy/paste. */
    var savedLicenseKey by mutableStateOf<String?>(null)
        private set
    var savedToken by mutableStateOf<String?>(null)
        private set

    init {
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                SecretStore.get(context, SecretStore.LICENSE_KEY) to SecretStore.get(context, SecretStore.MUSIC_USER_TOKEN)
            }.let { (license, token) ->
                savedLicenseKey = license
                savedToken = token
            }
        }
    }

    override fun onCleared() {
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
    }

    private fun showMessage(text: String?, error: Boolean = false) {
        message = text
        messageIsError = error
    }

    /** `versionName` of the chosen input, used to name the saved file. */
    private var inputVersionName: String = "unknown"

    val suggestedFileName get() = "Cider-$inputVersionName-patched.apk"

    fun onResume() {
        shizuku.onHostResumed()
        shizukuRunning = Shizuku.pingBinder()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val installed = CiderApp.installedInfo(context) != null
            val activity = CiderApp.hasPatchedActivity(context)
            val mismatch = withContext(Dispatchers.Default) { hasSignerMismatch() }
            ciderInstalled = installed
            patchedActivityExists = activity
            signerMismatch = mismatch
            originalLabel = withContext(Dispatchers.IO) { if (originalApk.isFile) apkLabel(originalApk) else null }
        }
    }

    private fun hasSignerMismatch(): Boolean {
        val signer = CiderApp.signerSha256(context) ?: return false
        return !signer.contentEquals(SigningKeyStore.get(context).certSha256())
    }

    private fun apkLabel(apk: File): String? =
        context.packageManager.getPackageArchiveInfo(apk.path, 0)?.let {
            "${it.packageName} ${it.versionName} (${it.longVersionCode})"
        }

    fun updateAdvanced(enabled: Boolean) {
        advanced = enabled
        prefs.edit().putBoolean(KEY_ADVANCED, enabled).apply()
    }

    fun updateInstallMethod(method: InstallMethod) {
        installMethod = method
        prefs.edit().putString(KEY_INSTALL_METHOD, method.name).apply()
    }

    /** Stores (or, when blank, removes) the license key and token on this phone, encrypted. */
    fun saveKeys(licenseKey: String, token: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    SecretStore.put(context, SecretStore.LICENSE_KEY, licenseKey)
                    SecretStore.put(context, SecretStore.MUSIC_USER_TOKEN, token)
                }
                savedLicenseKey = licenseKey.trim().ifEmpty { null }
                savedToken = token.trim().ifEmpty { null }
                showMessage(if (savedLicenseKey == null && savedToken == null) "Saved keys removed." else "Keys saved on this phone.")
            } catch (e: Exception) {
                showMessage(describe(e), error = true)
            }
        }
    }

    fun useSavedToken() {
        savedToken?.let { token = it }
    }

    fun pickFile(uri: Uri) = loadInput {
        context.contentResolver.openInputStream(uri)?.use { input ->
            inputApk.outputStream().use { input.copyTo(it) }
        } ?: throw PatchException("Couldn't open the selected file.")
    }

    fun useInstalledCider() = loadInput {
        val info = CiderApp.installedInfo(context) ?: throw PatchException("Cider isn't installed.")
        if (!info.splitSourceDirs.isNullOrEmpty()) {
            throw PatchException("Installed Cider is a split install; choose an APK file instead")
        }
        File(info.sourceDir).inputStream().use { input -> inputApk.outputStream().use { input.copyTo(it) } }
    }

    private fun loadInput(copy: () -> Unit) {
        showMessage(null)
        viewModelScope.launch {
            _state.value = try {
                withContext(Dispatchers.IO) {
                    copy()
                    val info = context.packageManager.getPackageArchiveInfo(inputApk.path, 0)
                        ?: throw PatchException("Not an APK")
                    inputVersionName = info.versionName ?: "unknown"
                    PatchState.Ready(inputApk, apkLabel(inputApk) ?: info.packageName)
                }
            } catch (e: Exception) {
                PatchState.Failed(describe(e), emptyList())
            }
        }
    }

    fun patch() {
        val ready = _state.value as? PatchState.Ready ?: return
        showMessage(null)
        viewModelScope.launch {
            val lock = Any()
            val log = mutableListOf<String>()
            var step: CiderPatcher.Step? = null
            var symbols = 0
            fun publish() {
                _state.value = PatchState.Patching(log.toList(), step, symbols)
            }
            _state.value = PatchState.Patching(emptyList(), null, 0)
            _state.value = try {
                withContext(Dispatchers.Default) {
                    patchedApk.delete()
                    val key = SigningKeyStore.get(context)
                    CiderPatcher.patch(
                        ready.apk, patchedApk, key,
                        log = { line ->
                            synchronized(lock) {
                                log += line
                                if (" → " in line) symbols++
                                publish()
                            }
                        },
                        onStep = { s -> synchronized(lock) { step = s } },
                    )
                    // The input passed ManifestPatcher's already-patched check, so it's an original build.
                    ready.apk.copyTo(originalCandidate, overwrite = true)
                }
                PatchState.Done(patchedApk, synchronized(lock) { log.toList() })
            } catch (e: Throwable) {
                patchedApk.delete()
                PatchState.Failed(describe(e), synchronized(lock) { log.toList() })
            }
        }
    }

    fun save(uri: Uri) {
        val done = _state.value as? PatchState.Done ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        done.output.inputStream().use { it.copyTo(out) }
                    } ?: throw PatchException("Couldn't write the selected file.")
                }
                showMessage("Saved.")
            } catch (e: Exception) {
                showMessage(describe(e), error = true)
            }
        }
    }

    /** Installs directly unless the installed Cider has a different signer, which needs an uninstall first. */
    fun install() {
        if (_state.value !is PatchState.Done || busy) return
        viewModelScope.launch {
            val mismatch = withContext(Dispatchers.Default) { hasSignerMismatch() }
            if (mismatch) confirmUninstall = true else runInstall(uninstallFirst = false)
        }
    }

    fun dismissUninstall() {
        confirmUninstall = false
    }

    fun confirmUninstallAndInstall() {
        confirmUninstall = false
        viewModelScope.launch { runInstall(uninstallFirst = true) }
    }

    private suspend fun runInstall(uninstallFirst: Boolean) {
        val done = _state.value as? PatchState.Done ?: return
        busy = true
        showMessage(if (uninstallFirst) "Uninstalling Cider and installing…" else "Installing…")
        try {
            val output = if (uninstallFirst) {
                installer.uninstallThenInstall(CiderPatcher.TARGET_PACKAGE, done.output)
            } else {
                installer.install(done.output)
            }
            if (output.contains("Success") && originalCandidate.isFile) {
                withContext(Dispatchers.IO) {
                    java.nio.file.Files.move(
                        originalCandidate.toPath(), originalApk.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    )
                }
            }
            when {
                !output.contains("Success") -> showMessage(output, error = true)
                // A fresh install has no Cider account sign-in, so Cider can't check the token yet.
                uninstallFirst -> showMessage("Installed. Open Cider and sign in to your Cider account, then come back and tap Sign in to Cider.")
                token.isNotBlank() -> {
                    signInRequest = token
                    showMessage("Installed. Signing in to Cider…")
                }
                else -> showMessage("Installed. Enter your Music-User-Token and tap Sign in to Cider.")
            }
        } catch (e: Exception) {
            showMessage(describe(e), error = true)
        } finally {
            busy = false
            refresh()
        }
    }

    fun requestRevert() {
        if (!busy && originalApk.isFile) confirmRevert = true
    }

    fun dismissRevert() {
        confirmRevert = false
    }

    /**
     * Reinstalls the saved official Cider. It's signed by Cider's developers, not this patcher, so the patched
     * build has to be uninstalled first; after that, Cider's own updates install normally again.
     */
    fun confirmRevertAndInstall() {
        confirmRevert = false
        if (busy || !originalApk.isFile) return
        viewModelScope.launch {
            busy = true
            reverting = true
            showMessage("Restoring the original Cider…")
            try {
                val output = installer.uninstallThenInstall(CiderPatcher.TARGET_PACKAGE, originalApk)
                if (output.contains("Success")) {
                    showMessage("Original Cider restored. Update it as usual, then patch the new version here.")
                } else {
                    showMessage(output, error = true)
                }
            } catch (e: Exception) {
                showMessage(describe(e), error = true)
            } finally {
                busy = false
                reverting = false
                refresh()
            }
        }
    }

    fun signIn() {
        if (token.isNotBlank()) signInRequest = token
    }

    /** The screen took [signInRequest] and launched it. */
    fun signInLaunched() {
        signInRequest = null
        signingIn = true
    }

    fun onSignInLaunchFailed() {
        signingIn = false
        signInOutcome = SignInOutcome.NOT_INSTALLED
    }

    fun onSignInResult(resultCode: Int, result: String?) {
        signingIn = false
        signInOutcome = when {
            resultCode == Activity.RESULT_OK -> SignInOutcome.SUCCESS
            result == CiderPatcher.RESULT_INVALID -> SignInOutcome.INVALID
            result == CiderPatcher.RESULT_UNVERIFIED -> SignInOutcome.UNVERIFIED
            else -> SignInOutcome.CANCELLED
        }
        if (signInOutcome == SignInOutcome.SUCCESS) showMessage(null)
    }

    fun dismissSignInOutcome() {
        signInOutcome = null
    }

    /** Opens Cider normally. Returns false if it isn't installed. */
    fun openCider(): Boolean {
        val intent = CiderApp.launchIntent(context) ?: return false
        context.startActivity(intent)
        return true
    }

    private fun describe(e: Throwable): String = when (e) {
        is PatchException, is ShizukuException -> e.message ?: e.toString()
        // The Shizuku server went away mid-call (DeadObjectException), e.g. it was stopped.
        is android.os.RemoteException -> ShizukuInstaller.STOPPED
        else -> "${e::class.java.simpleName}: ${e.message}"
    }

    private companion object {
        const val KEY_ADVANCED = "advanced"
        const val KEY_INSTALL_METHOD = "install_method"
    }
}
