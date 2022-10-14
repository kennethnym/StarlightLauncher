package kenneth.app.starlightlauncher.datetime

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.Place

@Composable
fun WeatherLocationPicker(
    onDismissRequest: () -> Unit,
    onLocationSelected: (location: Place) -> Unit,
    viewModel: WeatherLocationPickerViewModel = hiltViewModel()
) {
    fun closePicker() {
        viewModel.clearSearchResults()
        onDismissRequest()
    }

    AlertDialog(
        onDismissRequest = { closePicker() },
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f),
        title = {
            Text(stringResource(R.string.weather_location_picker_title))
        },
        text = {
            LocationPickerBody(
                locations = viewModel.locations,
                isLoading = viewModel.isSearching,
                onSearchRequested = {
                    viewModel.searchForLocations(it)
                },
                onLocationSelected = {
                    closePicker()
                    onLocationSelected(it)
                }
            )
        },
        confirmButton = {
            TextButton(onClick = { closePicker() }) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationPickerBody(
    onSearchRequested: (searchTerm: String) -> Unit,
    onLocationSelected: (location: Place) -> Unit,
    isLoading: Boolean,
    locations: List<Place>?,
) {
    var searchTerm by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextField(
            value = searchTerm,
            trailingIcon = {
                if (isLoading)
                    CircularProgressIndicator()
                else
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            onSearchRequested(searchTerm)
                        }
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_baseline_search_24),
                            contentDescription = ""
                        )
                    }
            },
            onValueChange = { searchTerm = it }
        )

        when {
            locations == null ->
                Box(Modifier.fillMaxHeight())

            locations.isEmpty() ->
                Text(stringResource(R.string.weather_location_picker_no_location))

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(locations) {
                    Text(
                        it.displayName,
                        modifier = Modifier
                            .clickable { onLocationSelected(it) }
                            .fillMaxWidth()
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}
