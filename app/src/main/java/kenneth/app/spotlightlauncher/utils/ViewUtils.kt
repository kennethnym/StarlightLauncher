package kenneth.app.spotlightlauncher.utils

import android.app.Activity
import android.content.ContextWrapper
import android.view.View

val View.activity: Activity?
    get() {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = (context as ContextWrapper).baseContext
        }
        return null
    }
