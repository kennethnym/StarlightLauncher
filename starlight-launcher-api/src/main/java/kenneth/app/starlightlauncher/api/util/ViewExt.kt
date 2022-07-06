package kenneth.app.starlightlauncher.api.util

import android.content.ContextWrapper
import android.view.View
import androidx.appcompat.app.AppCompatActivity

/**
 * Retrieves the activity that the current view is attached to.
 */
val View.activity: AppCompatActivity?
    get() {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is AppCompatActivity) return ctx
            ctx = (context as ContextWrapper).baseContext
        }
        return null
    }
