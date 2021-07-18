package kenneth.app.spotlightlauncher.searching.views

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ActivityContext
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.api.getDuckDuckGoRedirectUrlFromQuery
import kenneth.app.spotlightlauncher.databinding.WebResultCardBinding
import kenneth.app.spotlightlauncher.databinding.WebResultRelatedTopicItemBinding
import kenneth.app.spotlightlauncher.searching.SmartSearcher
import kenneth.app.spotlightlauncher.utils.RecyclerViewDataAdapter
import javax.inject.Inject

@AndroidEntryPoint
class WebResultCard(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    @Inject
    lateinit var relatedTopicListAdapter: RelatedTopicListAdapter

    private var shouldShowRelatedTopics = false

    private val binding = WebResultCardBinding.inflate(LayoutInflater.from(context), this, true)

    private lateinit var webResult: SmartSearcher.WebResult

    fun display(webResult: SmartSearcher.WebResult) {
        if (webResult.title.isNotBlank()) {
            this.webResult = webResult

            with(binding) {
                webResultTitle.text = webResult.title
                webResultContent.apply {
                    val hasNoDescription = webResult.content == ""

                    text =
                        if (hasNoDescription) context.getString(R.string.web_result_no_description_label)
                        else webResult.content

                    setTypeface(
                        typeface,
                        if (hasNoDescription) Typeface.ITALIC else Typeface.NORMAL
                    )
                }
                openInBrowserButton.setOnClickListener { openInBrowser() }

                if (webResult.relatedTopics.isNotEmpty()) {
                    relatedTopicsButton.apply {
                        visibility = View.VISIBLE
                        setOnClickListener { toggleRelatedTopics() }
                    }
                } else if (shouldShowRelatedTopics) {
                    relatedTopicsButton.isVisible = false
                    toggleRelatedTopics()
                }

                null
            }
        }
    }

    fun hide() {
        isVisible = false
        binding.webResultSectionCard.isVisible = false
    }

    private fun toggleRelatedTopics() {
        shouldShowRelatedTopics = !shouldShowRelatedTopics

        if (shouldShowRelatedTopics) {
            with(binding) {
                relatedTopicsSection.root.isVisible = true
                relatedTopicsButton.text = context.getString(R.string.hide_topics_label)
                relatedTopicsSection.relatedTopicsList.apply {
                    layoutManager = relatedTopicListAdapter.layoutManager
                    adapter = relatedTopicListAdapter
                }
            }

            relatedTopicListAdapter.run {
                data = webResult.relatedTopics
                notifyDataSetChanged()
            }
        } else {
            with(binding) {
                relatedTopicsButton.text = context.getString(R.string.related_topics_label)
                relatedTopicsSection.root.isVisible = false
            }
        }
    }

    private fun openInBrowser() {
        startActivity(
            context,
            Intent(
                Intent.ACTION_VIEW,
                getDuckDuckGoRedirectUrlFromQuery(webResult.query)
            ),
            null
        )
    }
}

class RelatedTopicListAdapter @Inject constructor(
    @ActivityContext private val context: Context
) : RecyclerViewDataAdapter<SmartSearcher.WebResult.Topic, RelatedTopicListItem>() {
    override var data = listOf<SmartSearcher.WebResult.Topic>()

    override val layoutManager = LinearLayoutManager(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelatedTopicListItem {
        val binding = WebResultRelatedTopicItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RelatedTopicListItem(binding)
    }
}

class RelatedTopicListItem(override val binding: WebResultRelatedTopicItemBinding) :
    RecyclerViewDataAdapter.ViewHolder<SmartSearcher.WebResult.Topic>(binding) {
    private val context = itemView.context

    override fun bindWith(data: SmartSearcher.WebResult.Topic) {
        with(binding) {
            Glide
                .with(context)
                .load(data.previewUrl)
                .into(relatedTopicPreview)

            relatedTopicPreview.contentDescription = context.getString(
                R.string.related_topic_preview_description,
                data.title
            )

            relatedTopicTitle.text = data.title
        }
    }
}
