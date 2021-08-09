package kenneth.app.spotlightlauncher.widgets.unitconverter

import android.content.Context
import android.util.AttributeSet
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.AppState
import kenneth.app.spotlightlauncher.HANDLED
import kenneth.app.spotlightlauncher.NOT_HANDLED
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.databinding.UnitConverterWidgetBinding
import kenneth.app.spotlightlauncher.utils.ContextMenuCallback
import kenneth.app.spotlightlauncher.utils.activity
import java.lang.Exception
import javax.inject.Inject

/**
 * The type of value that the context menu is showing and will be modifying.
 */
private enum class SelectorValueType {
    /**
     * The context menu is showing a list of measurements.
     */
    MEASUREMENT,

    /**
     * The context menu is showing a list of units that [UnitConverterWidget] will
     * convert measurements from.
     */
    SRC_UNIT,

    /**
     * The context menu is showing a list of units that [UnitConverterWidget] will
     * convert measurements to.
     */
    DEST_UNIT
}

@AndroidEntryPoint
class UnitConverterWidget(context: Context, attrs: AttributeSet) :
    LinearLayout(context, attrs),
    ContextMenuCallback {
    @Inject
    lateinit var appState: AppState

    private val binding =
        UnitConverterWidgetBinding.inflate(LayoutInflater.from(context), this, true)

    private var selectedMeasurement = Measurement.LENGTH
        set(measurement) {
            field = measurement
            binding.selectedMeasurement = measurement
        }

    private var selectedSrcUnit = MeasurementUnit.MILES
        set(unit) {
            field = unit
            binding.selectedSrcUnit = unit
        }

    private var selectedDestUnit = MeasurementUnit.KILOMETERS
        set(unit) {
            field = unit
            binding.selectedDestUnit = unit
        }

    private lateinit var selectorValueType: SelectorValueType

    init {
        activity?.registerForContextMenu(this)

        with(binding) {
            selectedMeasurement = this@UnitConverterWidget.selectedMeasurement
            selectedSrcUnit = this@UnitConverterWidget.selectedSrcUnit
            selectedDestUnit = this@UnitConverterWidget.selectedDestUnit

            unitConverterWidgetBlurBackground.startBlur()

            measurementSelectorBtn.setOnClickListener {
                selectorValueType = SelectorValueType.MEASUREMENT
                activity?.openContextMenu(this@UnitConverterWidget)
            }

            srcUnitSelectorBtn.setOnClickListener {
                selectorValueType = SelectorValueType.SRC_UNIT
                activity?.openContextMenu(this@UnitConverterWidget)
            }

            destUnitSelectorBtn.setOnClickListener {
                selectorValueType = SelectorValueType.DEST_UNIT
                activity?.openContextMenu(this@UnitConverterWidget)
            }
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu?) {
        appState.contextMenuCallbackForView = this
        menu?.let {
            val entries = when (selectorValueType) {
                SelectorValueType.MEASUREMENT ->
                    resources
                        .getStringArray(R.array.measurement_selector_entries)
                        .map { Measurement.valueOf(it) }

                SelectorValueType.SRC_UNIT,
                SelectorValueType.DEST_UNIT -> when (selectedMeasurement) {
                    Measurement.LENGTH ->
                        resources
                            .getStringArray(R.array.length_unit_selector_entries)
                            .map { MeasurementUnit.valueOf(it) }
                }
            }

            drawContextMenuItems(
                menu,
                labels = entries.map { it.label },
                ids = entries.map { it.id }
            )
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean =
        try {
            when (selectorValueType) {
                SelectorValueType.MEASUREMENT -> {
                    selectedMeasurement = Measurement.fromId(item.itemId)
                }
                SelectorValueType.SRC_UNIT -> {
                    selectedSrcUnit = MeasurementUnit.fromId(item.itemId)
                }
                SelectorValueType.DEST_UNIT -> {
                    selectedDestUnit = MeasurementUnit.fromId(item.itemId)
                }
            }
            HANDLED
        } catch (ex: Exception) {
            NOT_HANDLED
        }

    private fun drawContextMenuItems(menu: ContextMenu, labels: List<String>, ids: List<Int>) {
        for (i in labels.indices) {
            menu.add(Menu.NONE, ids[i], Menu.NONE, labels[i])
        }
    }
}