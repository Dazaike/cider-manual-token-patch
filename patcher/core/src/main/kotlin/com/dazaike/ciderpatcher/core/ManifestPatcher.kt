package com.dazaike.ciderpatcher.core

import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.arsc.value.ValueType

object ManifestPatcher {
    private const val ATTR_THEME = 0x01010000
    private const val ATTR_NAME = 0x01010003
    private const val ATTR_EXPORTED = 0x01010010
    private const val ATTR_EXCLUDE_FROM_RECENTS = 0x01010017
    /**
     * `@android:style/Theme.DeviceDefault.Panel`: no title, no frame, transparent, no dim. The token-extra path
     * shows nothing while it signs in (the patcher draws its own spinner); the no-extra fallback's AlertDialog
     * still gets DeviceDefault dialog styling.
     */
    const val THEME_DEVICE_DEFAULT_PANEL = 0x0103013A

    /**
     * Adds the exported, launcher-less sign-in activity to Cider's binary manifest.
     * Attributes are added in ascending resource-id order, which the framework's parser requires.
     */
    fun patch(bytes: ByteArray): ByteArray {
        val manifest = AndroidManifestBlock.load(bytes.inputStream())
        val pkg = manifest.packageName
        if (pkg != CiderPatcher.TARGET_PACKAGE) throw PatchException("Not a Cider APK (package $pkg)")
        if (manifest.isSplit) throw PatchException("Split APKs are not supported; use the full Cider APK")
        if (manifest.getActivity(CiderPatcher.PATCH_ACTIVITY, false) != null) {
            throw PatchException("This APK is already patched")
        }
        val application = manifest.applicationElement ?: throw PatchException("Not a Cider APK (no <application>)")
        val activity = application.newElement("activity")
        activity.getOrCreateAndroidAttribute("theme", ATTR_THEME).apply {
            valueType = ValueType.REFERENCE
            data = THEME_DEVICE_DEFAULT_PANEL
        }
        activity.getOrCreateAndroidAttribute("name", ATTR_NAME).setValueAsString(CiderPatcher.PATCH_ACTIVITY)
        activity.getOrCreateAndroidAttribute("exported", ATTR_EXPORTED).setValueAsBoolean(true)
        activity.getOrCreateAndroidAttribute("excludeFromRecents", ATTR_EXCLUDE_FROM_RECENTS).setValueAsBoolean(true)
        manifest.refresh()
        return manifest.bytes
    }
}
