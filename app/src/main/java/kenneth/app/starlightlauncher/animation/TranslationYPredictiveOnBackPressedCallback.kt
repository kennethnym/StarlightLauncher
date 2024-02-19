package kenneth.app.starlightlauncher.animation

import android.os.Build
import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi

abstract class TranslationYPredictiveOnBackPressedCallback(
    private val targetView: View,
    enabled: Boolean
) :
    OnBackPressedCallback(enabled) {
    private var beginningTranslationY = 0f

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun handleOnBackStarted(backEvent: BackEventCompat) {
        beginningTranslationY = targetView.translationY
    }

    override fun handleOnBackProgressed(backEvent: BackEventCompat) {
        targetView.translationY = beginningTranslationY + 600 * backEvent.progress
    }

    override fun handleOnBackCancelled() {
        targetView.translationY = beginningTranslationY
    }
}