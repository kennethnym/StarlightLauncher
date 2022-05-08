package kenneth.app.starlightlauncher.contactsearchmodule

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter
import kenneth.app.starlightlauncher.contactsearchmodule.databinding.ContactListItemBinding
import kenneth.app.starlightlauncher.contactsearchmodule.databinding.ContactSearchResultCardBinding

class ContactSearchResultAdapter(
    private val context: Context,
    private val launcher: StarlightLauncherApi
) : SearchResultAdapter {
    private lateinit var binding: ContactSearchResultCardBinding

    override fun onCreateViewHolder(parent: ViewGroup): SearchResultAdapter.ViewHolder {
        val binding = ContactSearchResultCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContactSearchResultViewHolder(binding)
    }

    override fun onBindSearchResult(
        holder: SearchResultAdapter.ViewHolder,
        searchResult: SearchResult
    ) {
        if (holder is ContactSearchResultViewHolder) {
            binding = holder.binding
            onBindSearchResult(holder, searchResult)
        }
    }

    private fun onBindSearchResult(
        holder: ContactSearchResultViewHolder,
        result: SearchResult,
    ) {
        when (result) {
            is ContactSearchModule.Result -> {
                with(holder.binding) {
                    hasPermission = true

                    cardBackground.blurWith(launcher.blurHandler)
                    contactList.removeAllViews()

                    result.contacts.forEach { contact ->
                        ContactListItemBinding.inflate(
                            LayoutInflater.from(context),
                            contactList,
                            true
                        )
                            .apply {
                                this.contact = contact
                                root.setOnClickListener { viewContact(contact) }
                                phoneBtn.setOnClickListener { openDialPad(contact) }
                            }
                    }
                }
            }

            is ContactSearchModule.NoPermission -> {
                with(holder.binding) {
                    hasPermission = false

                    cardBackground.blurWith(launcher.blurHandler)

                    grantPermissionsBtn.setOnClickListener { requestPermission() }
                }
            }
        }
    }

    private fun requestPermission() {
        launcher.requestPermission(Manifest.permission.READ_CONTACTS) { hasGranted ->
            if (hasGranted) {
                binding.hasPermission = true
            }
        }
    }

    private fun viewContact(contact: Contact) {
        context.startActivity(Intent(Intent.ACTION_VIEW, contact.uri))
    }

    private fun openDialPad(contact: Contact) {
        contact.phoneNumber?.let {
            context.startActivity(Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${it}")
            })
        }
    }
}

class ContactSearchResultViewHolder(internal val binding: ContactSearchResultCardBinding) :
    SearchResultAdapter.ViewHolder {
    override val rootView = binding.root
}