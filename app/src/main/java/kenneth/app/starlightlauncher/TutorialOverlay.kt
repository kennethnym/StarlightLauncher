package kenneth.app.starlightlauncher

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.TypedValue
import android.view.LayoutInflater
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.Target
import com.takusemba.spotlight.shape.RoundedRectangle
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

/**
 * An overlay that gives the user a simple tutorial on how to use the launcher.
 */
@ActivityScoped
internal class TutorialOverlay @Inject constructor(
    @ActivityContext private val context: Context,
    private val sharedPreferences: SharedPreferences,
    private val launcherState: LauncherState,
) {
    private lateinit var spotlight: Spotlight

    private val targets by lazy {
        listOf(
            // widgets panel target instructing user to swipe up to expand the panel
            Target.Builder()
                .setAnchor(launcherState.screenWidth / 2f, launcherState.screenHeight * 0.75f - 100)
                .setShape(
                    RoundedRectangle(
                        launcherState.halfScreenHeight.toFloat() + 100,
                        launcherState.screenWidth.toFloat(),
                        0f
                    )
                )
                .setOverlay(
                    LayoutInflater.from(context).inflate(
                        R.layout.spotlight_overlay_widgets_panel,
                        null,
                    ).also {
                        it.setOnClickListener { spotlight.next() }
                    }
                )
                .build(),

            // target instructing user to long press anywhere above the search box
            // to open the launcher menu
            Target.Builder()
                .setAnchor(launcherState.screenWidth / 2f, launcherState.screenHeight * 0.25f)
                .setShape(
                    RoundedRectangle(
                        launcherState.halfScreenHeight.toFloat(),
                        launcherState.screenWidth.toFloat(),
                        0f
                    )
                )
                .setOverlay(
                    LayoutInflater.from(context).inflate(
                        R.layout.spotlight_overlay_launcher_menu,
                        null,
                    ).also {
                        it.translationY = 500f
                        it.setOnClickListener { finish() }
                    }
                )
                .build()
        )
    }

    /**
     * Starts the tutorial and shows the overlay.
     */
    fun start() {
        spotlight = Spotlight.Builder(context as Activity)
            .setTargets(targets)
            .setBackgroundColor(TypedValue().run {
                context.theme.resolveAttribute(R.attr.plateColor, this, true)
                data
            })
            .build()
            .also { it.start() }
    }

    private fun finish() {
        sharedPreferences
            .edit()
            .putBoolean(context.getString(R.string.pref_key_tutorial_finished), true)
            .apply()

        spotlight.finish()
    }
}