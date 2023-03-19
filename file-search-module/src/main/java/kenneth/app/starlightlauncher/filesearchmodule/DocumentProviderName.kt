package kenneth.app.starlightlauncher.filesearchmodule

import android.net.Uri
import androidx.annotation.StringRes

enum class DocumentProviderName(
    val authority: String,
    @StringRes val displayName: Int
) {
    DOWNLOADS("com.android.providers.downloads.documents", R.string.document_provider_downloads),
    EXTERNAL(
        "com.android.externalstorage.documents",
        R.string.document_provider_external_storage
    ), ;

    companion object {
        private val values =
            DocumentProviderName.values().associateBy(DocumentProviderName::authority)

        fun fromUri(uri: Uri) = values[uri.authority]
    }
}
