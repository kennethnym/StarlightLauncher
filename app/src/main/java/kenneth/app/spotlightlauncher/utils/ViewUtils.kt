package kenneth.app.spotlightlauncher.utils

import android.app.Activity
import android.content.ContextWrapper
import android.view.View
import androidx.appcompat.app.AppCompatActivity

val View.activity: AppCompatActivity?
    get() {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is AppCompatActivity) return ctx
            ctx = (context as ContextWrapper).baseContext
        }
        return null
    }
