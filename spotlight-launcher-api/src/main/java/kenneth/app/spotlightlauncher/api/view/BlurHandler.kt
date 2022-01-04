package kenneth.app.spotlightlauncher.api.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.widget.ImageView
import com.google.android.renderscript.Toolkit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Defines how much a bitmap should be scaled before being blurred
 */
private const val BLUR_SCALE = 0.2f

/**
 * Handles bluring of wallpapers and views.
 */
class BlurHandler(context: Context) {
    private val resources = context.resources

    /**
     * The current wallpaper of the home screen.
     *
     * This wallpaper will be used to create blur backgrounds for other views.
     */
    private var wallpaper: Bitmap? = null

    /**
     * The blurred version of the wallpaper
     */
    private var blurredWallpaper: Bitmap? = null

    /**
     * Determines if wallpaper should be blurred. This should be true when
     * the blurred version of has not yet been created.
     */
    private var shouldBlurWallpaper = true

    /**
     * Changes the wallpaper to be used for blur effect.
     */
    fun changeWallpaper(wallpaper: Bitmap) {
        this.wallpaper = wallpaper
        shouldBlurWallpaper = true
    }

    /**
     * Sets the blurred wallpaper as the background of the given [ImageView].
     * @param dest The [ImageView] this function should set the blurred wallpaper for.
     * @param blurAmount Amount of blur
     */
    fun blurView(dest: ImageView, blurAmount: Int) {
        if (shouldBlurWallpaper) {
            cacheBlurredWallpaper(blurAmount)
            shouldBlurWallpaper = false
        }

        blurredWallpaper?.let {
            if (dest.width <= 0 || dest.height <= 0) return

            val (viewX, viewY) = IntArray(2).run {
                dest.getLocationOnScreen(this)
                this
            }

            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels

            val bitmapX = it.width * max(viewX, 0) / screenWidth

            val bitmapY =
                it.height * max(min(viewY, screenHeight), 0) / screenHeight

            if (bitmapY >= it.height || bitmapX > it.width) return

            val bgWidth = min(
                it.width * dest.width / screenWidth, screenWidth
            )
            val scaledHeight = it.height * dest.height / screenHeight
            val bgHeight =
                if (bitmapY + scaledHeight > it.height)
                    it.height - bitmapY
                else
                    scaledHeight

            if (bgWidth > 0 && bgHeight > 0 && bitmapX + bgWidth <= it.width && bitmapY + bgHeight <= it.height) {
                val bitmap = Bitmap.createBitmap(
                    it,
                    bitmapX,
                    bitmapY,
                    bgWidth,
                    bgHeight,
                )
                val bgAspectRatio = bgWidth.toFloat() / bgHeight
                val destWidthFloat = dest.width.toFloat()

                dest.imageMatrix = Matrix().apply {
                    setTranslate(0f, if (viewY < 0) -viewY.toFloat() else 0f)
                    preScale(
                        destWidthFloat / bgWidth,
                        destWidthFloat / bgAspectRatio / bgHeight,
                    )
                }

                dest.setImageBitmap(bitmap)
            }
        }
    }

    /**
     * Tries to cache the current wallpaper to this.wallpaperDrawable.
     *
     * If READ_EXTERNAL_STORAGE is not granted, nothing will be cached
     */
    private fun cacheBlurredWallpaper(blurRadius: Int) {
        wallpaper?.let {
            blurredWallpaper = createBlurredBitmap(
                it,
                blurRadius,
            )
        }
    }

    private fun createBlurredBitmap(bitmap: Bitmap, blurRadius: Int): Bitmap {
        val scaledWidth = (bitmap.width * BLUR_SCALE).roundToInt()
        val scaledHeight = (bitmap.height * BLUR_SCALE).roundToInt()

        val scaledBitmap =
            Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, false)

        return Toolkit.blur(scaledBitmap, blurRadius)
    }
}