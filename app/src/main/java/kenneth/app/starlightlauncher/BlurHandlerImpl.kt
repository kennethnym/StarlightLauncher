package kenneth.app.starlightlauncher

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.WindowInsets
import android.widget.ImageView
import androidx.annotation.ColorInt
import com.google.android.renderscript.Toolkit
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.api.R
import kenneth.app.starlightlauncher.api.util.BlurHandler
import kenneth.app.starlightlauncher.api.util.Insets
import kenneth.app.starlightlauncher.api.view.Plate
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Defines how much a bitmap should be scaled before being blurred
 */
private const val BLUR_SCALE = 0.2f

private const val DEFAULT_BLUR_AMOUNT = 20

/**
 * Handles blurring of wallpapers and views.
 */
class BlurHandlerImpl @Inject constructor(
    @ApplicationContext context: Context
) : BlurHandler {
    override var isBlurEffectEnabled = true
        set(enabled) {
            field = enabled
            if (enabled) {
                registeredPlates.forEach { it.startBlur() }
            } else {
                imageViewCoors.clear()
            }
        }

    private val resources = context.resources

    /**
     * The blurred version of the wallpaper
     */
    private var blurredWallpaper: Bitmap? = null

    /**
     * Determines if wallpaper should be blurred. This should be true when
     * the blurred version of has not yet been created.
     */
    private var hasCache = false

    @ColorInt
    private var defaultNoBlurBackgroundColor: Int

    private var blurAmount: Int

    private val registeredPlates = mutableSetOf<Plate>()

    private val imageViewCoors = mutableMapOf<ImageView, IntArray>()

    init {
        context.obtainStyledAttributes(
            intArrayOf(
                R.attr.plateColor,
                R.attr.blurAmount,
            )
        ).run {
            try {
                defaultNoBlurBackgroundColor = getColor(
                    0,
                    context.getColor(android.R.color.transparent)
                )

                @SuppressLint("ResourceType")
                blurAmount = getInt(1, DEFAULT_BLUR_AMOUNT)
            } finally {
                recycle()
            }
        }
    }

    /**
     * Changes the wallpaper to be used for blur effect.
     */
    fun changeWallpaper(wallpaper: Bitmap) {
        cacheBlurredWallpaper(wallpaper)
        hasCache = false
        imageViewCoors.clear()
    }

    override fun registerPlate(plate: Plate) {
        registeredPlates += plate
        if (isBlurEffectEnabled) {
            plate.startBlur()
        }
    }

    override fun unregisterPlate(plate: Plate) {
        registeredPlates.remove(plate)
    }

    /**
     * Sets the blurred wallpaper as the background of the given [ImageView].
     * @param dest The [ImageView] this function should set the blurred wallpaper for.
     */
    override fun blurView(dest: ImageView) {
        val wallpaper = blurredWallpaper ?: return
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
        val bitmapX = wallpaper.width * max(viewX, 0) / screenWidth
        // the y coor of the cutout of the blurred wallpaper
        val bitmapY =
            wallpaper.height * max(
                min(viewY, screenHeight),
                0
            ) / screenHeight

        // if the coordinates are outside of wallpaper, do nothing
        if (bitmapY >= wallpaper.height || bitmapX > wallpaper.width) return

        // the width of the cutout
        val bgWidth = min(
            wallpaper.width * dest.width / screenWidth, screenWidth
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
        val bgHeight = wallpaper.height * destVisibleHeight / screenHeight

        if (bgWidth > 0 && bgHeight > 0 && bitmapX + bgWidth <= wallpaper.width && bitmapY + bgHeight <= wallpaper.height) {
            val bitmap = WeakReference(
                Bitmap.createBitmap(
                    wallpaper,
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
                if (drawable is BitmapDrawable) {
                    drawable.bitmap?.recycle()
                }
            }

            dest.setImageBitmap(bitmap.get())
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
    private fun cacheBlurredWallpaper(wallpaper: Bitmap) {
        blurredWallpaper = createBlurredBitmap(
            wallpaper,
            blurAmount,
        )
        hasCache = true
    }

    private fun createBlurredBitmap(bitmap: Bitmap, blurRadius: Int): Bitmap {
        val scaledWidth = (bitmap.width * BLUR_SCALE).roundToInt()
        val scaledHeight = (bitmap.height * BLUR_SCALE).roundToInt()

        val scaledBitmap =
            Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, false)

        return Toolkit.blur(scaledBitmap, blurRadius)
    }
}