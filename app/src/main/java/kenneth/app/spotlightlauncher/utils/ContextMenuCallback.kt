package kenneth.app.spotlightlauncher.utils

import android.view.MenuItem

/**
 * Implement this interface for classes that can be notified of what item in the
 * current floating context menu is selected.
 */
interface ContextMenuCallback {
    /**
     * Called when an item in the current floating context menu is selected.
     *
     * @param item The selected item in the current floating context menu.
     * @return false to allow normal context menu processing to proceed, true to consume it here
     */
    fun onContextItemSelected(item: MenuItem): Boolean

    fun onContextMenuClosed()
}