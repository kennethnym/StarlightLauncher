package kenneth.app.spotlightlauncher.api

import android.location.Location

data class LatLong(val lat: Float, val long: Float) {
    constructor(location: Location) : this(
        location.latitude.toFloat(),
        location.longitude.toFloat()
    )
}
