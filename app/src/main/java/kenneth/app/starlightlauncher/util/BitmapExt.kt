package kenneth.app.starlightlauncher.util

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.roundToInt

private const val downscaleFactor = 0.4

private fun Bitmap.downscale(): Bitmap {
    val downscaledWidth = (width * downscaleFactor).roundToInt()
    val downscaledHeight = (height * downscaleFactor).roundToInt()

    return Bitmap.createScaledBitmap(
        this,
        downscaledWidth,
        downscaledHeight,
        false
    )
}

internal fun Bitmap.calculateBrightness(): Int {
    val downscaledBitmap = downscale()
    val downscaledWidth = downscaledBitmap.width
    val downscaledHeight = downscaledBitmap.height

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

internal fun Bitmap.calculateDominantColor(): Int {
    val downscaledBitmap = downscale()
    val downscaledWidth = downscaledBitmap.width
    val downscaledHeight = downscaledBitmap.height

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

    return Color.rgb(r / n, g / n, b / n)
}
