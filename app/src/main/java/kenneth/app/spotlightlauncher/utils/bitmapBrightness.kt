package kenneth.app.spotlightlauncher.utils

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.roundToInt

private const val downscaleFactor = 0.4

/**
 * Calculates the brightness of the given bitmap, from 0 to 255 (darkest to lightest).
 *
 * Credit to [this post](https://stackoverflow.com/questions/52203887/detect-if-background-wallpaper-is-too-light-or-too-dark)
 */
fun calculateBitmapBrightness(bitmap: Bitmap): Int {
    val downscaledWidth = (bitmap.width * downscaleFactor).roundToInt()
    val downscaledHeight = (bitmap.height * downscaleFactor).roundToInt()

    val downscaledBitmap = Bitmap.createScaledBitmap(
        bitmap,
        downscaledWidth,
        downscaledHeight,
        false
    )

    var r = 0
    var g = 0
    var b = 0
    var n = 0

    val bitmapPixels = IntArray(downscaledWidth * downscaledHeight)

    downscaledBitmap.getPixels(
        bitmapPixels,
        0,
        downscaledWidth,
        0,
        0,
        downscaledWidth,
        downscaledHeight
    )

    for (pixel in bitmapPixels) {
        r += Color.red(pixel)
        g += Color.green(pixel)
        b += Color.blue(pixel)
        n += 1
    }

    return (r + g + b) / (n * 3)
}
