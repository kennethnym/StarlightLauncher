package kenneth.app.spotlightlauncher.prefs.appearance

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

private const val NOVA_LAUNCHER_INTENT = "com.teslacoilsw.launcher.THEME"

object IconPackManager {
    val installedIconPacks: List<IconPack>
        get() = iconPackIntents
            .flatMap {
                packageManager.queryIntentActivities(it, PackageManager.GET_META_DATA)
            }
            .map { IconPack(context, it.activityInfo.packageName) }

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager

    /**
     * A list of intents that icon packs declare.
     * This is used to load installed icon packs that support
     * the following intents.
     */
    private val iconPackIntents = listOf(
        Intent(NOVA_LAUNCHER_INTENT)
    )

    fun getInstance(context: Context) = this.apply {
        this.context = context
        packageManager = context.packageManager
    }
}

class IconPack(context: Context, private val packageName: String) {
    private val packageManager = context.packageManager

    private val maskImage: Bitmap? = null
    private val backImage: Bitmap? = null
    private val frontImage: Bitmap? = null

    /**
     * Loads all icons of this icon pack into memory.
     * Must be called before getting individual icons of an app.
     */
    fun load() {
        // every icon pack declares an "appfilter.xml" xml resource that contains
        // information of the icons the pack contains.
        // we need to manually parse the xml in order to obtain drawables of the icons.
        // an example can be found here:
        // https://github.com/the1dynasty/IconPack-Template/blob/master/IconPack-Template/res/xml/appfilter.xml

        val iconPackResources = packageManager.getResourcesForApplication(packageName)
        val appFilterXmlId = iconPackResources.getIdentifier(
            "appfilter",
            "xml",
            packageName,
        )

        val appFilterXml = if (appFilterXmlId > 0) {
            // appfilter.xml exists, load the xml file.
            iconPackResources.getXml(appFilterXmlId)
        } else {
            // cannot obtain appfilter.xml id, try to load it manually by reading it

            val xmlContent = iconPackResources.assets.open("appfilter.xml")

            XmlPullParserFactory.newInstance().run {
                isNamespaceAware = true
                newPullParser()
            }.apply {
                setInput(xmlContent, "utf-8")
            }
        }
    }

    private fun loadAppFilterXmlContent(xml: XmlResourceParser) {
        var xmlEvent = xml.eventType

        while (xmlEvent != XmlPullParser.END_DOCUMENT) {
            if (xmlEvent == XmlPullParser.START_TAG) {
                val tagName = xml.name

                when (tagName) {
                    "iconback" -> {

                    }
                }
            }
        }
    }
}
