package kenneth.app.starlightlauncher.api

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import kenneth.app.starlightlauncher.api.utils.BlurHandler
import kenneth.app.starlightlauncher.api.view.OptionMenu
import kenneth.app.starlightlauncher.api.view.OptionMenuBuilder

/**
 * This interface defines methods that can be called to interact with Spotlight Launcher.
 */
interface StarlightLauncherApi {
    /**
     * The context of the launcher
     */
    val context: Context

    /**
     * The [BlurHandler] that is handling the blur effects of this launcher.
     */
    val blurHandler: BlurHandler

    /**
     * Shows the option menu with the content added by [builder].
     *
     * To add content to the option menu, you can call [OptionMenu.addView]
     * (or other built-in methods that adds [View] to [OptionMenu]), or use [LayoutInflater]:
     *
     *     // view binding
     *     optionMenu.show { menu ->
     *         MyMenuContentBinding.inflate(LayoutInflater.from(context), menu)
     *     }
     *
     *     // LayoutInflater.inflate
     *     optionMenu.show { menu ->
     *         LayoutInflater.from(context).inflate(R.layout.my_menu, menu)
     *     }
     */
    fun showOptionMenu(builder: OptionMenuBuilder)

    /**
     * Get the currently applied icon pack.
     */
    fun getIconPack(): IconPack
}