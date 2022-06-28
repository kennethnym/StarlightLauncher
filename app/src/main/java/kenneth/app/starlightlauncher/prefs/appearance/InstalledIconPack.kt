package kenneth.app.starlightlauncher.prefs.appearance

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.ScaleDrawable
import android.os.UserHandle
import android.view.Gravity
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import kenneth.app.starlightlauncher.api.IconPack
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import kotlin.random.Random

private const val RES_TYPE_DRAWABLE = "drawable"
private const val RES_TYPE_XML = "xml"

/**
 * Represents an icon pack installed on the device.
 */
internal class InstalledIconPack(private val context: Context, val packageName: String) : IconPack {
    private val packageInfo by lazy {
        packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
    }

    /**
     * Get the bitmap of the icon of this icon pack.
     */
    override val icon: Bitmap by lazy {
        packageInfo
            .applicationInfo
            .loadIcon(packageManager)
            .toBitmap()
    }

    /**
     * The name of this icon pack.
     */
    override val name: String by lazy {
        packageInfo
            .applicationInfo
            .loadLabel(packageManager)
            .toString()
    }

    private val packageManager = context.packageManager

    private val backImages = mutableListOf<Bitmap>()
    private var maskImage: Bitmap? = null
    private var frontImage: Bitmap? = null
    private var scaleFactor: Float = 1f

    /**
     * A map of icons included in this icon pack.
     * Maps component name of the icon to the drawable name of it.
     */
    private val icons = mutableMapOf<String, String>()

    private lateinit var iconPackResources: Resources

    private val defaultIcons = mutableMapOf<String, Drawable>()

    private val loadedIcons = mutableMapOf<String, Drawable>()

    /**
     * Loads all icons of this icon pack into memory.
     * Must be called before getting individual icons of an app.
     */
    fun load() {
        // every icon pack declares an `appfilter.xml` xml resource that contains
        // information of the icons the pack contains.
        // we need to manually parse the xml in order to obtain drawables of the icons.
        // an example of `appfilter.xml` can be found here:
        // https://github.com/the1dynasty/IconPack-Template/blob/master/IconPack-Template/res/xml/appfilter.xml

        iconPackResources = packageManager.getResourcesForApplication(packageName)

        val appFilterXmlId = iconPackResources.getIdentifier(
            "appfilter",
            RES_TYPE_XML,
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

        loadAppFilterXmlContent(appFilterXml)
    }

    override fun getIconOf(launcherActivityInfo: LauncherActivityInfo, user: UserHandle): Drawable =
        getIconOf(
            launcherActivityInfo.applicationInfo.packageName,
            defaultIcons.getOrPut(
                launcherActivityInfo.applicationInfo.packageName
            ) { launcherActivityInfo.getIcon(0) }
        )

    override fun getIconOf(applicationInfo: ApplicationInfo, user: UserHandle): Drawable {
        val default = applicationInfo.loadIcon(packageManager)
        return getIconOf(
            applicationInfo.packageName,
            defaultIcons.getOrPut(applicationInfo.packageName) { default }
        )
    }

    /**
     * Retrieves the icon of the given package. Must call `load()` to load the bitmaps of icons,
     * otherwise this function will return null.
     */
    private fun getIconOf(packageName: String, default: Drawable): Drawable =
        loadedIcons.getOrPut(packageName) {
            val packageIntent = packageManager.getLaunchIntentForPackage(packageName)

            if (packageIntent != null) {
                val componentName = packageIntent.component.toString()
                val iconDrawableName = icons[componentName]

                if (iconDrawableName != null) {
                    val iconBitmap = getDrawableRes(iconDrawableName)

                    return iconBitmap ?: generateAppIcon(default)
                }

                return findIconInResources(componentName) ?: generateAppIcon(default)
            }

            return default
        }

    /**
     * Try to find the icon for the package in icon pack resources. Useful when the icon for
     * the package is not declared in appfilter.xml (and therefore not loaded into memory)
     * but may exist in drawable resources of the icon pack.
     *
     * @param componentName The component name of the package this function should find the icon for.
     * Should be of format: `ComponentInfo{component-name}`.
     */
    private fun findIconInResources(componentName: String): Drawable? {
        // first, quickly verify the format of the given component name

        val startBraceIndex = componentName.indexOf('{') + 1
        val endBraceIndex = componentName.indexOf('}', startBraceIndex)

        if (endBraceIndex > startBraceIndex) {
            // format should be correct

            val drawableName = componentName.substring(startBraceIndex, endBraceIndex)
                .lowercase()
                .replace(Regex("(.|/)"), "_")

            return getDrawableRes(drawableName)
        }

        return null
    }

    private fun loadAppFilterXmlContent(xml: XmlPullParser) {
        var xmlEvent = xml.eventType

        while (xmlEvent != XmlPullParser.END_DOCUMENT) {
            if (xmlEvent == XmlPullParser.START_TAG) {
                when (xml.name) {
                    "iconback" -> {
                        // current tag is <iconback>
                        // loop over every attribute of the tag
                        for (i in 0 until xml.attributeCount) {
                            val attrName = xml.getAttributeName(i)
                            if (attrName.startsWith("img")) {
                                val drawableName = xml.getAttributeValue(i)
                                val backImageBitmap = getDrawableRes(drawableName)
                                if (backImageBitmap != null) {
                                    backImages.add(backImageBitmap.toBitmap())
                                }
                            }
                        }
                    }
                    "iconmask" -> {
                        if (xml.attributeCount > 0 && xml.getAttributeName(0) == "img1") {
                            val drawableName = xml.getAttributeValue(0)
                            maskImage = getDrawableRes(drawableName)?.toBitmap()
                        }
                    }
                    "iconupon" -> {
                        if (xml.attributeCount > 0 && xml.getAttributeName(0) == "img1") {
                            val drawableName = xml.getAttributeValue(0)
                            frontImage = getDrawableRes(drawableName)?.toBitmap()
                        }
                    }
                    "scale" -> {
                        if (xml.attributeCount > 0 && xml.getAttributeName(0) == "factor") {
                            scaleFactor = xml.getAttributeValue(0).toFloat()
                        }
                    }
                    "item" -> {
                        var componentName: String? = null
                        var drawableName: String? = null

                        for (i in 0 until xml.attributeCount) {
                            when (xml.getAttributeName(i)) {
                                "component" -> {
                                    componentName = xml.getAttributeValue(i)
                                }
                                "drawable" -> {
                                    drawableName = xml.getAttributeValue(i)
                                }
                            }
                        }

                        if (
                            componentName != null
                            && drawableName != null
                            && !icons.containsKey(componentName)
                        ) {
                            icons[componentName] = drawableName
                        }
                    }
                }
            }

            xmlEvent = xml.next()
        }
    }

    /**
     * Retrieves the bitmap version of a given drawable in the resources of this icon pack.
     */
    private fun getDrawableRes(drawableName: String): Drawable? {
        val drawableResId = iconPackResources.getIdentifier(
            drawableName,
            RES_TYPE_DRAWABLE,
            packageName
        )

        return if (drawableResId > 0) {
            try {
                ResourcesCompat.getDrawable(iconPackResources, drawableResId, null)
            } catch (ex: Resources.NotFoundException) {
                null
            }
        } else null
    }

    /**
     * Generates the icon bitmap of an app by combining the default icon with the
     * back images, mask image and front image. Useful when this icon pack does not
     * have support for the app yet.
     */
    private fun generateAppIcon(baseIcon: Drawable): Drawable {
        if (backImages.isEmpty())
            return baseIcon

        val backImage = backImages[Random.Default.nextInt(backImages.size)]
        val resultBitmap = Bitmap.createBitmap(
            backImage.width,
            backImage.height,
            Bitmap.Config.ARGB_8888
        )
        val resultCanvas = Canvas(resultBitmap).also {
            it.drawBitmap(backImage, 0f, 0f, Paint())
        }

        val scaledBaseIcon =
            if (baseIcon.intrinsicWidth > backImage.width || baseIcon.intrinsicHeight > backImage.height) {
                ScaleDrawable(baseIcon, Gravity.CENTER, 1f, 1f).apply {
                    level = 8000
                }
            } else {
                baseIcon
            }

        val maskBitmap =
            Bitmap.createBitmap(backImage.width, backImage.height, Bitmap.Config.ARGB_8888)

        // create a canvas that draws the mask image to maskBitmap
        Canvas(maskBitmap).also {
            it.drawBitmap(maskImage ?: backImage, 0f, 0f, Paint())
        }

        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }

        val maskedIcon = Bitmap.createBitmap(
            maskBitmap.width,
            maskBitmap.height,
            Bitmap.Config.ARGB_8888,
        )

        Canvas(maskedIcon).run {
            // draw the base icon
            drawBitmap(
                scaledBaseIcon.toBitmap(),
                (width - scaledBaseIcon.intrinsicWidth) / 2f,
                (height - scaledBaseIcon.intrinsicHeight) / 2f,
                null,
            )
            // draw the mask on top of the icon
            drawBitmap(maskBitmap, 0f, 0f, maskPaint)
        }

        // draw the masked icon onto the final result canvas
        // position the icon at the center of the result canvas
        resultCanvas.drawBitmap(
            maskedIcon,
            ((resultCanvas.width - maskedIcon.width) / 2).toFloat(),
            ((resultCanvas.height - maskedIcon.height) / 2).toFloat(),
            null
        )

        frontImage?.let {
            resultCanvas.drawBitmap(it, 0f, 0f, null)
        }

        return BitmapDrawable(context.resources, resultBitmap)
    }
}