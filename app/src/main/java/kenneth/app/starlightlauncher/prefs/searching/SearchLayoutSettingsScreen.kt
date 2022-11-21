package kenneth.app.starlightlauncher.prefs.searching

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.compose.pref.SettingsListItem
import kenneth.app.starlightlauncher.api.compose.pref.SettingsScreen
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@Composable
internal fun SearchLayoutSettingsScreen(
    viewModel: SearchLayoutSettingsScreenViewModel = hiltViewModel(),
) {
    val listState = rememberReorderableLazyListState(onMove = { from, to ->
        viewModel.changeSearchModuleOrder(from.index, to.index)
    })

    SettingsScreen(
        title = stringResource(R.string.pref_search_layout_title),
        description = stringResource(R.string.pref_search_layout_subtitle)
    ) {
        LazyColumn(
            state = listState.listState,
            modifier = Modifier
                .reorderable(listState)
                .detectReorderAfterLongPress(listState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(viewModel.searchModuleOrder, { it.extensionName }) { item ->
                ReorderableItem(listState, key = item.extensionName) { isDragging ->
                    val scale by animateFloatAsState(if (isDragging) 1.1f else 1f)
                    Box(Modifier.scale(scale)) {
                        SettingsListItem(
                            title = item.displayName,
                            summary = item.description,
                            emptyIcon = false
                        )
                    }
                }
            }
        }
    }
}
