package kenneth.app.spotlightlauncher.api

data class LatLong(val lat: Float, val long: Float) {
    constructor(lat: Double, long: Double) : this(lat.toFloat(), long.toFloat())
}
