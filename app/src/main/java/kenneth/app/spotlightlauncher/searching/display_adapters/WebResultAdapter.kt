package kenneth.app.spotlightlauncher.searching.display_adapters

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kenneth.app.spotlightlauncher.MainActivity
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.api.getDuckDuckGoRedirectUrlFromQuery
import kenneth.app.spotlightlauncher.databinding.ActivityMainBinding
import kenneth.app.spotlightlauncher.searching.SmartSearcher
import kenneth.app.spotlightlauncher.utils.RecyclerViewDataAdapter
import kenneth.app.spotlightlauncher.views.BlurView
import javax.inject.Inject

/**
 * An adapter that displays web result to the user.
 */
class WebResultAdapter @Inject constructor(private val activity: Activity) :
    SectionResultAdapter<SmartSearcher.WebResult>() {
    private lateinit var webResultCard: LinearLayout
    private lateinit var cardBlurBackground: BlurView

    private lateinit var webResult: SmartSearcher.WebResult
    private lateinit var relatedTopicsButton: Button

    private val topicListAdapter = RelatedTopicListDataAdapter.getInstance(activity)

    private var showRelatedTopics = false

    private val layoutTransitionListener = object : LayoutTransition.TransitionListener {
        override fun startTransition(
            transition: LayoutTransition?,
            viewGroup: ViewGroup?,
            view: View?,
            type: Int
        ) {
            if (type == LayoutTransition.APPEARING) {
                viewGroup?.findViewById<View>(R.id.related_topics_section_dividers)
                    ?.alpha = 0.1f
            }
        }

        override fun endTransition(
            transition: LayoutTransition?,
            viewGroup: ViewGroup?,
            view: View?,
            type: Int
        ) {
            if (type == LayoutTransition.APPEARING) {
                viewGroup?.findViewById<View>(R.id.related_topics_section_dividers)
                    ?.alpha = 0.1f
            }
        }
    }

    override fun displayResult(result: SmartSearcher.WebResult) {
        webResult = result
        val cardList = activity.findViewById<LinearLayout>(R.id.section_card_list)

        webResultCard =
            cardList.findViewById<LinearLayout>(R.id.web_result_section_card).apply {
                visibility = View.VISIBLE
            }

        cardBlurBackground =
            cardList.findViewById<BlurView>(R.id.web_result_section_card_blur_background)
                .also { it.startBlur() }

        if (webResult.title.isNotBlank()) {
            with(webResultCard) {
                findViewById<TextView>(R.id.web_result_title).text = webResult.title

                findViewById<TextView>(R.id.web_result_content).apply {
                    val hasNoDescription = webResult.content == ""

                    text =
                        if (hasNoDescription) context.getString(R.string.web_result_no_description_label)
                        else webResult.content

                    setTypeface(
                        typeface,
                        if (hasNoDescription) Typeface.ITALIC else Typeface.NORMAL
                    )
                }

                findViewById<Button>(R.id.open_in_browser_button)
                    .setOnClickListener { openInBrowser() }

                relatedTopicsButton = findViewById(R.id.related_topics_button)

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
            }
        } else {
            hideWebResult()
            showRelatedTopics = false
        }
    }

    fun hideWebResult() {
        if (::webResultCard.isInitialized && ::cardBlurBackground.isInitialized) {
            cardBlurBackground.pauseBlur()
            webResultCard.visibility = View.GONE
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
