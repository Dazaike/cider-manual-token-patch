package com.dazaike.ciderpatcher

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.dazaike.ciderpatcher.ui.OverlayHost
import com.dazaike.ciderpatcher.ui.theme.PrismTheme

class MainActivity : ComponentActivity() {
    private val vm: PatchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bars = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(bars, bars)          // theme is always dark
        setContent { PrismTheme(vm.ui) { OverlayHost { PatchScreen(vm) } } }
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }

    /** Escape dismisses the topmost overlay (every kit overlay registers a BackHandler). */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (super.dispatchKeyEvent(event)) return true
        if (event.keyCode == KeyEvent.KEYCODE_ESCAPE && event.action == KeyEvent.ACTION_UP &&
            onBackPressedDispatcher.hasEnabledCallbacks()
        ) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return false
    }
}
