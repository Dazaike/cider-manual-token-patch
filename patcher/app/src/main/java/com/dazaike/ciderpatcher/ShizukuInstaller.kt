package com.dazaike.ciderpatcher

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ShizukuException(message: String) : Exception(message)

/** Installs through a Shizuku user service that runs `pm` as the shell user, so there are no prompts. */
class ShizukuInstaller(private val context: Context) : Installer {

    private val args = Shizuku.UserServiceArgs(ComponentName(context.packageName, InstallService::class.java.name))
        .daemon(false)
        .processNameSuffix("installer")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    /** Completes the in-flight permission request; set only while Shizuku's dialog is expected. */
    @Volatile private var pendingRequest: ((Boolean) -> Unit)? = null

    /**
     * Call from the host activity's onResume. Shizuku's dialog pauses the host, so being resumed
     * means the dialog is gone. If Shizuku never reported a result (its dialog can close without
     * dispatching one), settle the request from the server's current permission state.
     */
    fun onHostResumed() {
        val finish = pendingRequest ?: return
        finish(Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)
    }

    /** Ensures Shizuku is running and this app holds its permission, asking the user if needed. */
    private suspend fun ensureReady() {
        if (!Shizuku.pingBinder()) throw ShizukuException(NOT_RUNNING)
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) return
        val granted = suspendCancellableCoroutine { cont ->
            lateinit var listener: Shizuku.OnRequestPermissionResultListener
            val finish: (Boolean) -> Unit = { ok ->
                Shizuku.removeRequestPermissionResultListener(listener)
                pendingRequest = null
                if (cont.isActive) cont.resume(ok)
            }
            listener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
                if (requestCode == REQUEST_CODE) finish(grantResult == PackageManager.PERMISSION_GRANTED)
            }
            Shizuku.addRequestPermissionResultListener(listener)
            pendingRequest = finish
            cont.invokeOnCancellation {
                Shizuku.removeRequestPermissionResultListener(listener)
                pendingRequest = null
            }
            Shizuku.requestPermission(REQUEST_CODE)
        }
        if (!granted) {
            throw ShizukuException(
                "Shizuku didn't grant permission. Open Shizuku → Authorized applications, " +
                    "turn on Cider Patcher, then try again."
            )
        }
    }

    suspend fun <T> withService(block: (IInstallService) -> T): T {
        ensureReady()
        var connection: ServiceConnection? = null
        try {
            val service = suspendCancellableCoroutine { cont ->
                val conn = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                        if (binder == null || !binder.pingBinder()) {
                            if (cont.isActive) cont.resumeWithException(ShizukuException("Shizuku install service failed to start."))
                            return
                        }
                        if (cont.isActive) cont.resume(IInstallService.Stub.asInterface(binder))
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        if (cont.isActive) cont.resumeWithException(ShizukuException(STOPPED))
                    }
                }
                connection = conn
                Shizuku.bindUserService(args, conn)
            }
            return withContext(Dispatchers.IO) { block(service) }
        } finally {
            connection?.let { runCatching { Shizuku.unbindUserService(args, it, true) } }
        }
    }

    override suspend fun install(apk: File): String = withService { service -> installVia(service, apk) }

    override suspend fun uninstallThenInstall(packageName: String, apk: File): String = withService { service ->
        val removed = service.uninstall(packageName)
        if (!removed.contains("Success")) return@withService removed
        installVia(service, apk)
    }

    private fun installVia(service: IInstallService, apk: File): String =
        ParcelFileDescriptor.open(apk, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            service.install(pfd, apk.length())
        }

    companion object {
        private const val REQUEST_CODE = 1001
        const val NOT_RUNNING = "Shizuku isn't running. Start Shizuku, then try again."
        /** Shown when Shizuku's server goes away mid-operation (its binder dies). */
        const val STOPPED = "Shizuku stopped while installing. Start Shizuku, then tap Install again."
    }
}
