package kenneth.app.starlightlauncher.appsearchmodule

import androidx.recyclerview.widget.DiffUtil

internal class AppListDiffCallback(
    private val oldAppList: AppList,
    private val newAppList: AppList
) :
    DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldAppList.size

    override fun getNewListSize(): Int = newAppList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldAppList[oldItemPosition]
        val newItem = newAppList[newItemPosition]

        return oldItem.componentName == newItem.componentName && oldItem.user == newItem.user
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldAppList[oldItemPosition]
        val newItem = newAppList[newItemPosition]

        return oldItem.label == newItem.label
    }
}