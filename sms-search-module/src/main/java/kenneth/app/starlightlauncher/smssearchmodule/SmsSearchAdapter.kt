package kenneth.app.starlightlauncher.smssearchmodule

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter
import kenneth.app.starlightlauncher.smssearchmodule.databinding.SmsListItemBinding
import kenneth.app.starlightlauncher.smssearchmodule.databinding.SmsSearchBinding

class SmsSearchAdapter(
    private val context: Context,
    private val launcher: StarlightLauncherApi
) : SearchResultAdapter {
    private lateinit var binding: SmsSearchBinding

    override fun onCreateViewHolder(parent: ViewGroup): SearchResultAdapter.ViewHolder {
        val binding = SmsSearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SmsSearchBindingHolder(binding)
    }

    override fun onBindSearchResult(
        holder: SearchResultAdapter.ViewHolder,
        searchResult: SearchResult
    ) {
        if (holder is SmsSearchBindingHolder)
        {
            binding = holder.binding
            onBindSearchResult(searchResult, holder)
        }
    }

    private fun onBindSearchResult(
        searchResult: SearchResult,
        holder: SmsSearchBindingHolder
    ) {
        if (searchResult is SmsSearchModule.Result) {
            with(holder.binding) {
                hasPermission = true
                cardBackground.blurWith(launcher.blurHandler)
                messageList.removeAllViews()

                searchResult.messages.forEach { msg ->
                    SmsListItemBinding.inflate(
                        LayoutInflater.from(context),
                        messageList,
                        true
                    )
                        .apply {
                            this.message = msg
                            root.setOnClickListener { openMessenger(msg) }
                            messageButton.setOnClickListener { openMessenger(msg) }
                        }
                }
            }
        } else if (searchResult is SmsSearchModule.NoPermission)
            with(holder.binding) {
                hasPermission = false
                cardBackground.blurWith(launcher.blurHandler)

                Log.i("reindltest", "nopermission")

                grantPermission.setOnClickListener { requestPermission() }
            }
    }

    private fun requestPermission() {
        launcher.requestPermission(Manifest.permission.READ_SMS) { hasGranted ->
            if (hasGranted) {
                binding.hasPermission = true
            }
        }
    }

    private fun openMessenger(message: Message) {
        val defaultMessenger = Telephony.Sms.getDefaultSmsPackage(context)

        val messageIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:${message.address}")
        }
        messageIntent.setPackage(defaultMessenger)
        context.startActivity(messageIntent)
    }
}

class SmsSearchBindingHolder(internal val binding: SmsSearchBinding):
    SearchResultAdapter.ViewHolder {
    override val rootView = binding.root
}