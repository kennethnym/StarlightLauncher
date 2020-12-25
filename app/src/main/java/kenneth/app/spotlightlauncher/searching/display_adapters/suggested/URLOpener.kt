package kenneth.app.spotlightlauncher.searching.display_adapters.suggested

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import kenneth.app.spotlightlauncher.MainActivity
import kenneth.app.spotlightlauncher.MainApplication_HiltComponents
import kenneth.app.spotlightlauncher.R
import javax.inject.Inject

/**
 * Handles logic for opening URLs in browser.
 */
class URLOpener @Inject constructor(
    private val mainActivity: MainActivity,
) {
    private lateinit var controlContainer: LinearLayout

    /**
     * Displays control to let user open the URL they inputted in the search box.
     *
     * @param parentView The parent of the control
     * @param url The URL the user inputted in the search box.
     */
    fun displayControl(parentView: ViewGroup, url: String) {
        controlContainer = LayoutInflater.from(mainActivity)
            .inflate(R.layout.open_url_control, parentView)
            .findViewById(R.id.url_opener_control)

        with(controlContainer) {
            setOnClickListener { open(url) }

            findViewById<TextView>(R.id.url_opener_url_label)
                .text = url
        }
    }

    /**
     * Opens the given url in the default browser.
     */
    private fun open(url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fixURL(url)))

        if (browserIntent.resolveActivity(mainActivity.packageManager) != null) {
            mainActivity.startActivity(browserIntent)
        }
    }

    /**
     * Fixes missing https issues in the given url
     */
    private fun fixURL(url: String): String =
        if (url.startsWith("www."))
            "https://$url"
        else url
}