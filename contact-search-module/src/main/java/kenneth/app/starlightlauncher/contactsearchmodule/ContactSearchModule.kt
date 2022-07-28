package kenneth.app.starlightlauncher.contactsearchmodule

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter

private const val EXTENSION_NAME = "kenneth.app.starlightlauncher.contactsearchmodule"
private const val MAX_RESULT = 2

class ContactSearchModule(context: Context) : SearchModule(context) {
    override val metadata = Metadata(
        extensionName = EXTENSION_NAME,
        displayName = context.getString(R.string.contact_search_module_display_name),
        description = context.getString(R.string.contact_search_module_description),
    )

    override lateinit var adapter: SearchResultAdapter
        private set

    override fun initialize(launcher: StarlightLauncherApi) {
        adapter = ContactSearchResultAdapter(launcher.context, launcher)
    }

    override fun cleanup() {}

    override suspend fun search(keyword: String, keywordRegex: Regex): SearchResult =
        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                ),
                "${ContactsContract.Contacts.DISPLAY_NAME} LIKE ?",
                arrayOf("%${keyword}%"),
                null,
            )?.use { cursor ->
                val contacts = mutableListOf<Contact>()
                var i = 0
                while (cursor.moveToNext() && i < MAX_RESULT) {
                    val idCol =
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val displayNameCol =
                        cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val photoThumbnailUriCol =
                        cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
                    val phoneNumberCol =
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    if (idCol < 0 || displayNameCol < 0 || photoThumbnailUriCol < 0 || phoneNumberCol < 0)
                        continue

                    val contactId = cursor.getString(idCol)

                    contacts += Contact(
                        uri = Uri.withAppendedPath(
                            ContactsContract.Contacts.CONTENT_URI,
                            contactId
                        ),
                        phoneNumber = cursor.getString(phoneNumberCol),
                        displayName = cursor.getString(displayNameCol),
                        thumbnailUri = cursor.getString(photoThumbnailUriCol)?.let { Uri.parse(it) }
                    )

                    i++
                }

                if (contacts.isEmpty())
                    SearchResult.None(keyword, EXTENSION_NAME)
                else
                    Result(keyword, contacts)
            }
                ?: SearchResult.None(keyword, EXTENSION_NAME)
        else NoPermission(keyword)

    internal class NoPermission(keyword: String) : SearchResult(keyword, EXTENSION_NAME)

    internal class Result(keyword: String, val contacts: List<Contact>) :
        SearchResult(keyword, EXTENSION_NAME)
}