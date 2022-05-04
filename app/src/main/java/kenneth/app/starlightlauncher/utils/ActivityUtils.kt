package kenneth.app.starlightlauncher.utils

import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity

internal fun AppCompatActivity.addBackPressedCallback(callback: () -> Unit) {
    onBackPressedDispatcher.addCallback(this) { callback() }
}
