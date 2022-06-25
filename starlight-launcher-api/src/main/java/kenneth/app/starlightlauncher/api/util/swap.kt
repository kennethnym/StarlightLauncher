package kenneth.app.starlightlauncher.api.util

fun <T> MutableList<T>.swap(fromIndex: Int, toIndex: Int) {
    with(this[fromIndex]) {
        this@swap[fromIndex] = this@swap[toIndex]
        this@swap[toIndex] = this
    }
}

fun <K, V> MutableMap<K, V>.swap(fromKey: K, toKey: K) {
    with(this[fromKey]) {
        this@swap[fromKey] = this@swap[toKey]!!
        this@swap[toKey] = this!!
    }
}
