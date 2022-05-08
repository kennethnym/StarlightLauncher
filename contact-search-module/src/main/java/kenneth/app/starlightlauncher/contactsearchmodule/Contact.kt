package kenneth.app.starlightlauncher.contactsearchmodule

import android.net.Uri

/**
 * Describes a contact stored on the phone.
 */
data class Contact(
    /**
     * The [Uri] of this contact.
     */
    val uri: Uri,

    /**
     * The phone number of this contact. null if it doesn't exist.
     */
    val phoneNumber: String?,

    /**
     * The display name of this contact.
     */
    val displayName: String,

    /**
     * The [Uri] of the thumbnail of this contact.
     */
    val thumbnailUri: Uri?,
)