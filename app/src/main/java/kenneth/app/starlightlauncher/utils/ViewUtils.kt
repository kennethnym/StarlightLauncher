package kenneth.app.starlightlauncher.utils

import android.content.ContextWrapper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kenneth.app.starlightlauncher.MainActivity

internal val View.activity: AppCompatActivity?
    get() {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is AppCompatActivity) return ctx
            ctx = (context as ContextWrapper).baseContext
        }
        return null
    }

internal val View.mainActivity: MainActivity?
    get() {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is MainActivity) return ctx
            ctx = (context as ContextWrapper).baseContext
        }
        return null
    }
