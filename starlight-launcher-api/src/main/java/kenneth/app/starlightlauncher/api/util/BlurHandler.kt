package kenneth.app.starlightlauncher.api.util

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.google.android.renderscript.Toolkit
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Defines how much a bitmap should be scaled before being blurred
 */
private const val BLUR_SCALE = 0.2f

/**
 * Handles blurring of wallpapers and views.
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

    private val imageViewCoors = mutableMapOf<ImageView, IntArray>()

    /**
     * Changes the wallpaper to be used for blur effect.
     */
    fun changeWallpaper(wallpaper: Bitmap) {
        this.wallpaper = wallpaper
        shouldBlurWallpaper = true
        imageViewCoors.clear()
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

            // get window insets
            val insets = getInsets(dest)
            val currentCoors = IntArray(2).run {
                dest.getLocationOnScreen(this)
                this
            }

            if (!imageViewCoors.contains(dest)) {
                imageViewCoors[dest] = currentCoors
            } else {
                imageViewCoors[dest]?.let { coor ->
                    if (coor contentEquals currentCoors) {
                        return
                    }
                    imageViewCoors[dest] = currentCoors
                }
            }

            val (viewX, viewY) = currentCoors
            // y coor of the bottom of the dest view
            val viewBottomY = viewY + dest.height

            val screenWidth = resources.displayMetrics.widthPixels + insets.left + insets.right
            val screenHeight = resources.displayMetrics.heightPixels + insets.bottom + insets.top

            // the x coor of the cutout of the blurred wallpaper
            val bitmapX = it.width * max(viewX, 0) / screenWidth
            // the y coor of the cutout of the blurred wallpaper
            val bitmapY =
                it.height * max(
                    min(viewY, screenHeight),
                    0
                ) / screenHeight

            // if the coordinates are outside of wallpaper, do nothing
            if (bitmapY >= it.height || bitmapX > it.width) return

            // the width of the cutout
            val bgWidth = min(
                it.width * dest.width / screenWidth, screenWidth
            )

            // height of the visible part of the dest view
            val destVisibleHeight =
                // if top or bottom of dest view is outside of screen
                if (viewBottomY > screenHeight || viewY < 0)
                // visible height is the total height minus the height of invisible parts
                    dest.height - max(viewBottomY - screenHeight, 0) + min(viewY, 0)
                else
                    dest.height

            // the height of the cutout
            val bgHeight = it.height * destVisibleHeight / screenHeight

            if (bgWidth > 0 && bgHeight > 0 && bitmapX + bgWidth <= it.width && bitmapY + bgHeight <= it.height) {
                val bitmap = WeakReference(
                    Bitmap.createBitmap(
                        it,
                        bitmapX,
                        bitmapY,
                        bgWidth,
                        bgHeight,
                    )
                )
                val bgAspectRatio = bgWidth.toFloat() / bgHeight
                val destWidthFloat = dest.width.toFloat()

                dest.imageMatrix = Matrix().apply {
                    // if dest's top part is partially blocked, move the cutout down to the bottom
                    setTranslate(0f, if (viewY < 0) -viewY.toFloat() else 0f)
                    preScale(
                        destWidthFloat / bgWidth,
                        destWidthFloat / bgAspectRatio / bgHeight,
                    )
                }

                dest.drawable.let { drawable ->
                    if (drawable is BitmapDrawable) drawable.bitmap.recycle()
                }

                dest.setImageBitmap(bitmap.get())
            }
        }
    }

    private fun getInsets(dest: ImageView): Insets = dest.context.run {
        if (dest.isAttachedToWindow && this is Activity) {
            window.decorView.rootWindowInsets.let { windowInsets: WindowInsets ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Insets(windowInsets.getInsets(WindowInsets.Type.systemBars()))
                } else {
                    Insets(
                        top = windowInsets.systemWindowInsetTop,
                        bottom = windowInsets.systemWindowInsetBottom,
                    )
                }
            }
        } else {
            Insets()
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