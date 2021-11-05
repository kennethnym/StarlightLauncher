package kenneth.app.spotlightlauncher.prefs.appearance

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import kotlin.math.roundToInt
import kotlin.random.Random

private const val RES_TYPE_DRAWABLE = "drawable"
private const val RES_TYPE_XML = "xml"

/**
 * Represents an icon pack installed on the device.
 */
class InstalledIconPack(context: Context, val packageName: String) : IconPack {
    private val packageInfo by lazy {
        packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
    }

    /**
     * Get the bitmap of the icon of this icon pack. Meta.
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

    /**
     * Retrieves the icon of the given [ResolveInfo].
     *
     * @return The icon for the [ResolveInfo], or the default icon if this icon pack
     * doesn't have an icon for it.
     */
    override fun getIconOf(resolveInfo: ResolveInfo): Bitmap {
        val default = resolveInfo.loadIcon(packageManager)
        return getIconOf(resolveInfo.activityInfo.packageName, default.toBitmap())
    }

    /**
     * Retrieves the icon of the given package. Must call `load()` to load the bitmaps of icons,
     * otherwise this function will return null.
     */
    private fun getIconOf(packageName: String, default: Bitmap): Bitmap {
        val packageIntent = packageManager.getLaunchIntentForPackage(packageName)

        if (packageIntent != null) {
            val componentName = packageIntent.component.toString()
            val iconDrawableName = icons[componentName]

            if (iconDrawableName != null) {
                val iconBitmap = getBitmapOfDrawableRes(iconDrawableName)

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
    private fun findIconInResources(componentName: String): Bitmap? {
        // first, quickly verify the format of the given component name

        val startBraceIndex = componentName.indexOf('{') + 1
        val endBraceIndex = componentName.indexOf('}', startBraceIndex)

        if (endBraceIndex > startBraceIndex) {
            // format should be correct

            val drawableName = componentName.substring(startBraceIndex, endBraceIndex)
                .lowercase()
                .replace(Regex("(.|/)"), "_")

            return getBitmapOfDrawableRes(drawableName)
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
                                val backImageBitmap = getBitmapOfDrawableRes(drawableName)
                                if (backImageBitmap != null) {
                                    backImages.add(backImageBitmap)
                                }
                            }
                        }
                    }
                    "iconmask" -> {
                        if (xml.attributeCount > 0 && xml.getAttributeName(0) == "img1") {
                            val drawableName = xml.getAttributeValue(0)
                            maskImage = getBitmapOfDrawableRes(drawableName)
                        }
                    }
                    "iconupon" -> {
                        if (xml.attributeCount > 0 && xml.getAttributeName(0) == "img1") {
                            val drawableName = xml.getAttributeValue(0)
                            frontImage = getBitmapOfDrawableRes(drawableName)
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
    private fun getBitmapOfDrawableRes(drawableName: String): Bitmap? {
        val drawableResId = iconPackResources.getIdentifier(
            drawableName,
            RES_TYPE_DRAWABLE,
            packageName
        )

        if (drawableResId > 0) {
            val drawable = ResourcesCompat.getDrawable(iconPackResources, drawableResId, null)
            if (drawable is BitmapDrawable) return drawable.bitmap
        }

        return null
    }

    /**
     * Generates the icon bitmap of an app by combining the default icon with the
     * back images, mask image and front image. Useful when this icon pack does not
     * have support for the app yet.
     */
    private fun generateAppIcon(baseIconBitmap: Bitmap): Bitmap {
        if (backImages.isEmpty())
            return baseIconBitmap

        val backImage = backImages[Random.Default.nextInt(backImages.size)]
        val resultBitmap = Bitmap.createBitmap(
            backImage.width,
            backImage.height,
            Bitmap.Config.ARGB_8888
        )
        val resultCanvas = Canvas(resultBitmap).also {
            it.drawBitmap(backImage, 0f, 0f, null)
        }

        val scaledBaseIcon =
            if (baseIconBitmap.width > backImage.width || baseIconBitmap.height > backImage.height) {
                Bitmap.createScaledBitmap(
                    baseIconBitmap,
                    (backImage.width * scaleFactor).roundToInt(),
                    (backImage.height * scaleFactor).roundToInt(),
                    false,
                )
            } else {
                baseIconBitmap
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

        // draw the default app icon onto the canvas
        // position the icon at the center of the result canvas
        with(resultCanvas) {
            drawBitmap(
                scaledBaseIcon,
                ((backImage.width - scaledBaseIcon.width) / 2).toFloat(),
                ((backImage.height - scaledBaseIcon.height) / 2).toFloat(),
                null
            )

            // draw the mask on top of the icon
            drawBitmap(maskBitmap, 0f, 0f, maskPaint)
        }

        maskPaint.xfermode = null

        frontImage?.let {
            resultCanvas.drawBitmap(it, 0f, 0f, null)
        }

        return resultBitmap
    }
}