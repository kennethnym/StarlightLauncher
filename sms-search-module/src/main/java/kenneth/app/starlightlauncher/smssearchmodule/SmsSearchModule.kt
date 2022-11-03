package kenneth.app.starlightlauncher.smssearchmodule

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony.Sms
import android.util.Log
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter

private const val EXTENSION_NAME = "kenneth.app.starlightlauncher.smssearchmodule"
private const val MAX_RESULT = 3 // TODO: make configurable
private const val MAX_MESSAGE_LENGTH = 30

class SmsSearchModule(context: Context) : SearchModule(context) {
    override val metadata = Metadata(
        extensionName = EXTENSION_NAME,
        displayName = context.getString(R.string.sms_search_display_name),
        description = context.getString(R.string.sms_search_module_description)
    )
    override lateinit var adapter: SearchResultAdapter
        private set

    override fun initialize(launcher: StarlightLauncherApi) {
        adapter = SmsSearchAdapter(launcher.context, launcher)
    }

    override fun cleanup() {}

    override suspend fun search(keyword: String, keywordRegex: Regex): SearchResult {
        if (context.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED)
        {
            val uri = Sms.CONTENT_URI
            context.contentResolver.query(uri, null, "${Sms.BODY} LIKE ?", arrayOf("%${keyword}%"), "date desc")
                ?.use {
                    val messages = mutableListOf<Message>()
                    while (it.moveToNext() && messages.count() < MAX_RESULT) {
                        val messageIndex = it.getColumnIndex(Sms.BODY)
                        val addressIndex = it.getColumnIndex(Sms.ADDRESS)

                        if (messageIndex < 0 || addressIndex < 0)
                            continue

                        var message = it.getString(messageIndex)
                        if (message.length > MAX_MESSAGE_LENGTH)
                            message = message.substring(0, MAX_MESSAGE_LENGTH) + "..."
                        val address = it.getString(addressIndex)

                        messages.add(Message(message, address))
                    }

                    if (messages.isEmpty())
                        return SearchResult.None(keyword, EXTENSION_NAME)

                    return Result(keyword, messages)
                }

            return SearchResult.None(keyword, EXTENSION_NAME)
        } else {
            return NoPermission(keyword)
        }
    }

    internal class NoPermission(keyword: String) : SearchResult(keyword, EXTENSION_NAME)

    internal class Result(keyword: String, val messages: List<Message>) :
            SearchResult(keyword, EXTENSION_NAME)
}