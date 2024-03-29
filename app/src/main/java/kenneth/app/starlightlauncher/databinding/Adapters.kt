package kenneth.app.starlightlauncher.databinding

import android.graphics.Typeface
import android.util.Log
import android.widget.TextView
import androidx.databinding.BindingAdapter

@BindingAdapter("textStyle")
fun setTextStyle(v: TextView, style: Int) {
    v.typeface = Typeface.create(v.typeface, style)
}
