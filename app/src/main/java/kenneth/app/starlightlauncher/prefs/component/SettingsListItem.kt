package kenneth.app.starlightlauncher.prefs.component

import android.view.MotionEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kenneth.app.starlightlauncher.Manrope

val SETTINGS_LIST_ITEM_ICON_SIZE = 24.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SettingsListItem(
    icon: Painter? = null,
    emptyIcon: Boolean = icon == null,
    title: String,
    summary: String? = null,
    onTap: (() -> Unit)? = null,
    control: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    var isTapped by remember { mutableStateOf(false) }

    val scale: Float by animateFloatAsState(
        if (isTapped) 0.9f else 1.0f
    )

    fun onTouchEvent(event: MotionEvent): Boolean =
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isTapped = true
                true
            }

            MotionEvent.ACTION_UP -> {
                isTapped = false
                onTap?.let { it() }
                true
            }

            MotionEvent.ACTION_CANCEL -> {
                isTapped = false
                true
            }

            else -> false
        }

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .scale(scale)
            .fillMaxWidth()
            .then(
                if (onTap != null && enabled)
                    Modifier.pointerInteropFilter { onTouchEvent(it) }
                else Modifier
            )
            .alpha(if (enabled) 1f else 0.5f)
    ) {
        when {
            icon != null -> Image(
                painter = icon,
                contentDescription = title,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .width(SETTINGS_LIST_ITEM_ICON_SIZE)
                    .height(SETTINGS_LIST_ITEM_ICON_SIZE),
            )
            emptyIcon -> Spacer(
                modifier = Modifier
                    .width(SETTINGS_LIST_ITEM_ICON_SIZE)
                    .height(SETTINGS_LIST_ITEM_ICON_SIZE)
            )
        }

        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.weight(1F)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            summary?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }

        control?.let { it() }
    }
}