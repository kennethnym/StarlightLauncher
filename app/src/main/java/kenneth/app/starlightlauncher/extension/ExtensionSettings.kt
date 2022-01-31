package kenneth.app.starlightlauncher.extension

import android.content.Intent
import android.graphics.drawable.Drawable

/**
 * Describes settings for an extension
 */
data class ExtensionSettings(
    /**
     * The title of the settings
     */
    val title: String,

    /**
     * Description for what the settings adjust.
     */
    val description: String,

    /**
     * The icon for this settings
     */
    val icon: Drawable? = null,

    /**
     * An [Intent] to launch the settings activity.
     */
    val intent: Intent,
)