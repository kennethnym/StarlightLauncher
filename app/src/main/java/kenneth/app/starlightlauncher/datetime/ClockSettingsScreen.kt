package kenneth.app.starlightlauncher.datetime

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.TemperatureUnit
import kenneth.app.starlightlauncher.prefs.component.*

@ExperimentalMaterialApi
@Composable
internal fun ClockSettingsScreen(
    viewModel: ClockSettingsScreenViewModel = hiltViewModel(),
) {
    var isLocationPickerOpen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val permissionRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.changeShouldUseAutoWeatherLocation(
            // only use auto weather location if location permission is granted
            shouldUse = isGranted
        )
    }

    fun toggleAutoWeatherLocation(shouldEnableAutoLocation: Boolean) {
        val hasLocationPermission =
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (shouldEnableAutoLocation && !hasLocationPermission) {
            permissionRequestLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        } else {
            viewModel.changeShouldUseAutoWeatherLocation(shouldEnableAutoLocation)
        }
    }

    SettingsScreen(
        title = stringResource(R.string.pref_clock_settings_screen_title),
        description = stringResource(R.string.pref_clock_settings_screen_subtitle)
    ) {
        SettingsList {
            SingleChoiceSettingsListItem(
                icon = painterResource(R.drawable.ic_expand_arrows),
                labels = stringArrayResource(R.array.clock_size_entries).asIterable(),
                values = DateTimeViewSize.values().asIterable(),
                choice = viewModel.dateTimeViewSize,
                title = stringResource(R.string.date_time_clock_size_title),
                onChoiceSelected = { viewModel.changeDateTimeViewSize(it) }
            )

            SwitchSettingsListItem(
                title = stringResource(R.string.date_time_use_24hr_clock_title),
                checked = viewModel.shouldUse24HrClock,
                onCheckedChange = { viewModel.changeShouldUse24HrClock(it) }
            )

            SettingsSection(title = stringResource(R.string.pref_weather_section_title)) {
                SwitchSettingsListItem(
                    icon = painterResource(R.drawable.ic_cloud_sun),
                    title = stringResource(R.string.date_time_show_weather_title),
                    summary = stringResource(R.string.date_time_show_weather_summary),
                    checked = viewModel.shouldShowWeather,
                    onCheckedChange = { viewModel.changeShouldShowWeather(it) }
                )

                AnimatedVisibility(
                    visible = viewModel.shouldShowWeather,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    SettingsList {
                        SwitchSettingsListItem(
                            title = stringResource(R.string.date_time_use_auto_location_title),
                            summary = stringResource(R.string.date_time_use_auto_location_summary),
                            checked = viewModel.shouldUseAutoWeatherLocation,
                            onCheckedChange = { toggleAutoWeatherLocation(it) }
                        )

                        AnimatedVisibility(
                            visible = !viewModel.shouldUseAutoWeatherLocation,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            SettingsListItem(
                                title = stringResource(R.string.date_time_pick_location_title),
                                summary = stringResource(
                                    R.string.date_time_pick_location_summary,
                                    viewModel.weatherLocationName
                                        ?: stringResource(R.string.weather_location_not_set)
                                ),
                                onTap = { isLocationPickerOpen = true }
                            )
                        }

                        val weatherFrequencyLabels =
                            stringArrayResource(R.array.weather_check_frequency_labels)

                        SingleChoiceSettingsListItem(
                            labels = weatherFrequencyLabels.asIterable(),
                            values = WEATHER_UPDATE_FREQUENCY_VALUES,
                            choice = viewModel.weatherCheckFrequency,
                            icon = painterResource(R.drawable.ic_clock),
                            title = stringResource(R.string.date_time_weather_check_frequency_title),
                            summary = stringResource(
                                R.string.date_time_weather_check_frequency_summary,
                                weatherFrequencyLabels[WEATHER_UPDATE_FREQUENCY_VALUES.indexOf(
                                    viewModel.weatherCheckFrequency
                                )]
                            ),
                            onChoiceSelected = { viewModel.changeWeatherCheckFrequency(it) }
                        )

                        SingleChoiceSettingsListItem(
                            labels = stringArrayResource(R.array.weather_unit_labels).asIterable(),
                            values = TemperatureUnit.values().asIterable(),
                            choice = viewModel.weatherUnit,
                            icon = painterResource(R.drawable.ic_ruler),
                            title = stringResource(R.string.date_time_weather_unit_title),
                            onChoiceSelected = { viewModel.changeWeatherUnit(it) }
                        )
                    }
                }
            }
        }

        if (isLocationPickerOpen) {
            WeatherLocationPicker(
                onDismissRequest = { isLocationPickerOpen = false },
                onLocationSelected = { viewModel.changeWeatherLocation(it) }
            )
        }
    }
}
