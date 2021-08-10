package kenneth.app.spotlightlauncher.widgets.unitconverter

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.widget.addTextChangedListener
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

    private var selectedMeasurement = MeasurementType.LENGTH
        set(measurement) {
            field = measurement
            binding.selectedMeasurement = measurement
            // set default unit conversion for different measurement types
            when (measurement) {
                MeasurementType.LENGTH -> {
                    selectedSrcUnit = MeasurementUnit.MILES
                    selectedDestUnit = MeasurementUnit.KILOMETERS
                }
                MeasurementType.WEIGHT -> {
                    selectedSrcUnit = MeasurementUnit.POUND
                    selectedDestUnit = MeasurementUnit.KILOGRAMS
                }
            }
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

    /**
     * Whether [UnitConverterWidget] is modifying either srcUnitValueEditText or destUnitValueEditText.
     */
    private var isSelfUpdatingEditText = false

    /**
     * Whether [UnitConverterWidget] is responsible for drawing the floating context menu.
     * If false, the default floating context menu is drawn when requested.
     */
    private var shouldDrawCustomContextMenu = false

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
                shouldDrawCustomContextMenu = true
                activity?.openContextMenu(this@UnitConverterWidget)
            }

            srcUnitSelectorBtn.setOnClickListener {
                selectorValueType = SelectorValueType.SRC_UNIT
                shouldDrawCustomContextMenu = true
                activity?.openContextMenu(this@UnitConverterWidget)
            }

            destUnitSelectorBtn.setOnClickListener {
                selectorValueType = SelectorValueType.DEST_UNIT
                shouldDrawCustomContextMenu = true
                activity?.openContextMenu(this@UnitConverterWidget)
            }

            srcUnitValueEditText.addTextChangedListener { text ->
                startConversion(
                    srcEditText = srcUnitValueEditText,
                    srcUnit = this@UnitConverterWidget.selectedSrcUnit,
                    destEditText = destUnitValueEditText,
                    destUnit = this@UnitConverterWidget.selectedDestUnit,
                )
            }

            destUnitValueEditText.addTextChangedListener { text ->
                startConversion(
                    srcEditText = destUnitValueEditText,
                    srcUnit = this@UnitConverterWidget.selectedDestUnit,
                    destEditText = srcUnitValueEditText,
                    destUnit = this@UnitConverterWidget.selectedSrcUnit,
                )
            }
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu?) {
        if (shouldDrawCustomContextMenu) {
            appState.contextMenuCallbackForView = this
            menu?.let {
                val entries = when (selectorValueType) {
                    SelectorValueType.MEASUREMENT ->
                        resources
                            .getStringArray(R.array.measurement_selector_entries)
                            .map { MeasurementType.valueOf(it) }

                    SelectorValueType.SRC_UNIT,
                    SelectorValueType.DEST_UNIT -> when (selectedMeasurement) {
                        MeasurementType.LENGTH ->
                            resources
                                .getStringArray(R.array.length_unit_selector_entries)
                                .map { MeasurementUnit.valueOf(it) }
                        MeasurementType.WEIGHT ->
                            resources
                                .getStringArray(R.array.weight_unit_selector_entries)
                                .map { MeasurementUnit.valueOf(it) }
                    }
                }

                drawContextMenuItems(
                    menu,
                    labels = entries.map { it.label },
                    ids = entries.map { it.id }
                )
            }
        } else {
            super.onCreateContextMenu(menu)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean =
        try {
            when (selectorValueType) {
                SelectorValueType.MEASUREMENT -> {
                    selectedMeasurement = MeasurementType.fromId(item.itemId)
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

    override fun onContextMenuClosed() {
        shouldDrawCustomContextMenu = false
    }

    private fun drawContextMenuItems(menu: ContextMenu, labels: List<String>, ids: List<Int>) {
        for (i in labels.indices) {
            menu.add(Menu.NONE, ids[i], Menu.NONE, labels[i])
        }
    }

    /**
     * Parses values in the text boxes, then starts the unit conversion if the values are valid.
     *
     * @param destEditText The EditText that should contain the result of the conversion.
     */
    private fun startConversion(
        srcEditText: EditText,
        srcUnit: MeasurementUnit,
        destEditText: EditText,
        destUnit: MeasurementUnit,
    ) {
        if (isSelfUpdatingEditText)
            isSelfUpdatingEditText = false
        else try {
            Measurement(
                value = srcEditText.text.toString().toDouble(),
                unit = srcUnit,
            )
                .convertTo(destUnit)
                ?.let { newMeasurement ->
                    isSelfUpdatingEditText = true
                    destEditText.setText(String.format("%.3f", newMeasurement.value))
                }
        } catch (ex: Exception) {
            // the src text contains invalid number, ignoring
        }
    }
}