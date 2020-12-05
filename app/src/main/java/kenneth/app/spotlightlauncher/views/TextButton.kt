package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import kenneth.app.spotlightlauncher.R

class TextButton(context: Context, attrs: AttributeSet) :
    androidx.appcompat.widget.AppCompatButton(context, attrs, R.style.TextButton) {

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        super.onTouchEvent(event)

        return when (event?.action) {
            MotionEvent.ACTION_BUTTON_PRESS -> {
                performClick()
                true
            }
            MotionEvent.ACTION_DOWN -> {
                showClickedEffect()
                true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                hideClickedEffect()
                true
            }
            else -> false
        }
    }

    private fun showClickedEffect() {
        alpha = 0.5f
    }

    private fun hideClickedEffect() {
        alpha = 1f
    }
}