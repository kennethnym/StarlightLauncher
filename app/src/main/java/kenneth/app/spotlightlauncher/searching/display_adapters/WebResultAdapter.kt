package kenneth.app.spotlightlauncher.searching.display_adapters

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.api.getDuckDuckGoRedirectUrlFromQuery
import kenneth.app.spotlightlauncher.searching.SmartSearcher
import kenneth.app.spotlightlauncher.utils.RecyclerViewDataAdapter
import kenneth.app.spotlightlauncher.views.BlurView
import kenneth.app.spotlightlauncher.views.TextButton
import javax.inject.Inject

/**
 * An adapter that displays web result to the user.
 */
class WebResultAdapter @Inject constructor(private val activity: Activity) :
    SectionResultAdapter<SmartSearcher.WebResult>() {
    private lateinit var webResultCard: LinearLayout
    private lateinit var cardBlurBackground: BlurView
    private lateinit var webResultTitle: TextView
    private lateinit var webResultContent: TextView
    private lateinit var openInBrowserButton: TextButton
    private lateinit var relatedTopicsButton: TextButton

    private lateinit var webResult: SmartSearcher.WebResult

    private val topicListAdapter = RelatedTopicListDataAdapter.getInstance(activity)

    private var showRelatedTopics = false

    override fun displayResult(result: SmartSearcher.WebResult) {
        webResult = result

        findViews()

        if (webResult.title.isNotBlank()) {
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

                if (showRelatedTopics) {
                    if (activity.findViewById<RecyclerView>(R.id.related_topics_list) != null) {
                        topicListAdapter.displayData(webResult.relatedTopics)
                    } else {
                        showRelatedTopics = false
                        toggleRelatedTopics()
                    }
                }
            } else if (showRelatedTopics) {
                relatedTopicsButton.visibility = View.GONE
                topicListAdapter.hideList()
                showRelatedTopics = false
            }
        } else {
            hideWebResult()
            showRelatedTopics = false
        }
    }

    fun hideWebResult() {
        if (::webResultCard.isInitialized && ::cardBlurBackground.isInitialized) {
            cardBlurBackground.apply {
                pauseBlur()
                isVisible = false
            }
            webResultCard.isVisible = false
        }
    }

    private fun findViews() {
        with(activity) {
            if (!::webResultCard.isInitialized) {
                webResultCard = findViewById(R.id.web_result_section_card)
            }

            if (!::cardBlurBackground.isInitialized) {
                cardBlurBackground = findViewById(R.id.web_result_section_card_blur_background)
            }

            if (!::webResultTitle.isInitialized) {
                webResultTitle = findViewById(R.id.web_result_title)
            }

            if (!::webResultContent.isInitialized) {
                webResultContent = findViewById(R.id.web_result_content)
            }

            if (!::openInBrowserButton.isInitialized) {
                openInBrowserButton = findViewById(R.id.open_in_browser_button)
            }

            if (!::relatedTopicsButton.isInitialized) {
                relatedTopicsButton = findViewById(R.id.related_topics_button)
            }
        }
    }

    private fun openInBrowser() {
        activity.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                getDuckDuckGoRedirectUrlFromQuery(webResult.query)
            )
        )
    }

    private fun toggleRelatedTopics() {
        showRelatedTopics = !showRelatedTopics

        if (showRelatedTopics) {
            LayoutInflater.from(activity)
                .inflate(
                    R.layout.web_result_related_topics_section,
                    webResultCard,
                )

            relatedTopicsButton.text = activity.getString(R.string.hide_topics_label)
            topicListAdapter.displayData(webResult.relatedTopics)
        } else {
            relatedTopicsButton.text = activity.getString(R.string.related_topics_label)
            topicListAdapter.hideList()
        }
    }
}

private object RelatedTopicListDataAdapter :
    RecyclerViewDataAdapter<SmartSearcher.WebResult.Topic, RelatedTopicListViewHolder>() {
    override val layoutManager: RecyclerView.LayoutManager
        get() = LinearLayoutManager(activity)

    override val recyclerView: RecyclerView
        get() = activity.findViewById(R.id.related_topics_list)

    override fun getInstance(activity: Activity) = this.apply { this.activity = activity }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelatedTopicListViewHolder {
        val listItemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.web_result_related_topic_item, parent, false)
                as LinearLayout

        return RelatedTopicListViewHolder(listItemView, activity)
    }

    override fun displayData(data: List<SmartSearcher.WebResult.Topic>?) {
        super.displayData(data)

        val topics = data

        if (topics != null) {
            this.data = topics
            notifyDataSetChanged()
        }
    }

    fun hideList() {
        val webResultCardLayout =
            activity.findViewById<LinearLayout>(R.id.web_result_section_card)

        unbindAdapterFromRecyclerView()

        for (i in 0..2) {
            webResultCardLayout?.removeViewAt(webResultCardLayout.childCount - 1)
        }
    }
}

private class RelatedTopicListViewHolder(view: LinearLayout, activity: Activity) :
    RecyclerViewDataAdapter.ViewHolder<SmartSearcher.WebResult.Topic>(view, activity) {
    override fun bindWith(data: SmartSearcher.WebResult.Topic) {
        val topic = data

        with(view) {
            val preview =
                findViewById<ImageView>(R.id.related_topic_preview)

            Glide
                .with(this)
                .load(topic.previewUrl)
                .into(preview)

            preview.contentDescription =
                context.getString(
                    R.string.related_topic_preview_description,
                    topic.title
                )

            findViewById<TextView>(R.id.related_topic_title).text =
                topic.title
        }
    }
}
