package kenneth.app.starlightlauncher.util

import android.content.ContextWrapper
import android.view.View
import androidx.appcompat.app.AppCompatActivity

internal val View.activity: AppCompatActivity?
    get() {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is AppCompatActivity) return ctx
            ctx = (context as ContextWrapper).baseContext
        }
        return null
    }
