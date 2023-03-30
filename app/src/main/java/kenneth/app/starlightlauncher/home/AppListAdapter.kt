package kenneth.app.starlightlauncher.home

import android.content.pm.LauncherActivityInfo
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.IconPack
import kenneth.app.starlightlauncher.databinding.AppListItemBinding
import java.util.*

internal class AppListAdapter(
    apps: List<LauncherActivityInfo>,
    private var allSections: Array<String>,
    iconPack: IconPack,
    locale: Locale,
    private val callback: Callback
) :
    RecyclerView.Adapter<AppListViewHolder>() {
    private var recyclerView: RecyclerView? = null

    interface Callback {
        fun onSectionClicked(availableSections: Set<String>)

        fun onItemClicked(app: LauncherActivityInfo, sourceBounds: Rect)
    }

    var locale = locale
        set(newLocale) {
            field = newLocale
            allItemTypes.clear()
            sectionsInList.clear()
            visibleApps = appList.toList()
            selectedSection = null
            refreshList()
        }

    var iconPack = iconPack
        set(newIconPack) {
            field = newIconPack
            notifyItemRangeChanged(0, appList.size)
        }

    private var appList = apps.sortedBy { it.label.toString() }
    private var visibleApps = appList.toList()

    private val allItemTypes = mutableListOf<Int>()

    private val sectionsInList = mutableSetOf<String>()

    private var selectedSection: String? = null

    init {
        indexSections()
    }

    override fun getItemCount(): Int = visibleApps.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppListViewHolder {
        val binding =
            AppListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppListViewHolder, position: Int) {
        val app = visibleApps[position]
        val appLabel = app.label
        val itemType = itemTypeOfApp(app, locale)

        with(holder.binding) {
            categoryLabel.apply {
                text = allSections[itemType]
                isInvisible =
                    position != 0 && itemType == itemTypeOfApp(visibleApps[position - 1], locale)

                setOnClickListener {
                    callback.onSectionClicked(sectionsInList)
                }
            }

            appIcon.apply {
                contentDescription =
                    context.getString(R.string.app_icon_content_description, app.name)

                Glide.with(context)
                    .load(iconPack.getIconOf(app, app.user))
                    .into(this)
            }

            this.appLabel.text = appLabel

            root.setOnClickListener {
                val sourceBounds = Rect().run {
                    appIcon.getGlobalVisibleRect(this)
                    this
                }
                callback.onItemClicked(app, sourceBounds)
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    fun update(appList: List<LauncherActivityInfo>) {
        this.appList = appList.sortedBy { it.label.toString() }.toList()
        allItemTypes.clear()
        indexSections()
        refreshList()
    }

    fun onlyShowSection(label: String) {
        val sectionItemType = allSections.indexOf(label)
        if (sectionItemType < 0) return

        selectedSection = label

        refreshList()
    }

    fun showAllApps() {
        selectedSection = null
        refreshList()
    }

    private fun indexSections() {
        appList.forEachIndexed { i, app ->
            val itemType = itemTypeOfApp(app, locale)
            allItemTypes += itemType
            if (i == 0 || allItemTypes[i - 1] != itemType) {
                val sectionLabel = allSections[itemType]
                sectionsInList += sectionLabel
            }
        }
    }

    private fun refreshList() {
        val newList = calculateVisibleApps()
        val diffResult = DiffUtil.calculateDiff(AppListDiffCallback(visibleApps, newList))
        this.visibleApps = newList
        diffResult.dispatchUpdatesTo(this)
    }

    private fun calculateVisibleApps(): List<LauncherActivityInfo> =
        selectedSection?.let { selectedSection ->
            appList.mapIndexedNotNull { i, app ->
                val itemType = allItemTypes[i]
                val label = allSections[itemType]

                if (label == selectedSection) app
                else null
            }
        } ?: appList.toList()
}

internal class AppListViewHolder(internal val binding: AppListItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    init {
        binding.root.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_BUTTON_PRESS -> {
                    v.performClick()
                }

                MotionEvent.ACTION_DOWN -> {
                    binding.isClicked = true
                    true
                }

                MotionEvent.ACTION_UP -> {
                    binding.isClicked = false
                    v.performClick()
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    binding.isClicked = false
                    true
                }

                else -> false
            }
        }
    }
}
