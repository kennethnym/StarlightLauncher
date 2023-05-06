package kenneth.app.starlightlauncher.api

import android.content.Context
import android.view.View
import android.view.ViewGroup

/**
 * This class is responsible for creating a Starlight Widget that can be added to the home screen.
 * Note that this is different from Android widgets.
 *
 * @param context The application context of this widget creator.
 *                This is different from the application context of the launcher itself.
 *                This context can be used to query strings/resources declared in the same package as this widget creator.
 */
abstract class WidgetCreator(private val context: Context) {
    abstract val metadata: Metadata

    abstract fun initialize(launcher: StarlightLauncherApi)

    abstract fun createWidget(parent: ViewGroup): WidgetHolder

    data class Metadata(
        /**
         * A unique string to identify the extension containing the search module.
         * The package name of the extension should be used, e.g. `com.my.extension`.
         */
        val extensionName: String,

        /**
         * A user facing name of the widget.
         */
        val displayName: String,

        /**
         * A user facing description that describes what the widget does
         */
        val description: String,
    )
}

interface WidgetHolder {
    val rootView: View
}
