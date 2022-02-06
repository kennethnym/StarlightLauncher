package kenneth.app.starlightlauncher.unitconverterwidget

import android.widget.EditText
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.addTextChangedListener
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.WidgetHolder
import kenneth.app.starlightlauncher.unitconverterwidget.databinding.UnitConverterWidgetBinding
import java.lang.Exception

class UnitConverterWidget(
    private val binding: UnitConverterWidgetBinding,
    private val launcher: StarlightLauncherApi,
) : WidgetHolder {
    override val rootView = binding.root

    private val context = binding.root.context

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

    init {
        with(binding) {
            selectedMeasurement = this@UnitConverterWidget.selectedMeasurement
            selectedSrcUnit = this@UnitConverterWidget.selectedSrcUnit
            selectedDestUnit = this@UnitConverterWidget.selectedDestUnit

            measurementSelectorBtn.setOnClickListener {
                shouldDrawCustomContextMenu = true
                openMeasurementSelector()
            }

            srcUnitSelectorBtn.setOnClickListener {
                shouldDrawCustomContextMenu = true
                openSrcUnitSelector()
            }

            destUnitSelectorBtn.setOnClickListener {
                shouldDrawCustomContextMenu = true
                openDestUnitSelector()
            }

            srcUnitValueEditText.addTextChangedListener { text ->
                startConversion(
                    srcEditText = srcUnitValueEditText,
                    srcUnit = this@UnitConverterWidget.selectedSrcUnit,
                    destEditText = destUnitValueEditText,
                    destUnit = this@UnitConverterWidget.selectedDestUnit,
                )
            }

            destUnitValueEditText.addTextChangedListener {
                startConversion(
                    srcEditText = destUnitValueEditText,
                    srcUnit = this@UnitConverterWidget.selectedDestUnit,
                    destEditText = srcUnitValueEditText,
                    destUnit = this@UnitConverterWidget.selectedSrcUnit,
                )
            }

            unitConverterWidgetBg.blurWith(launcher.blurHandler)
        }
    }

    private fun openMeasurementSelector() {
        launcher.showOptionMenu { menu ->
            context.resources.getStringArray(R.array.measurement_selector_entries)
                .map { MeasurementType.valueOf(it) }
                .forEach { measurement ->
                    menu.addItem(
                        if (selectedMeasurement == measurement)
                            AppCompatResources.getDrawable(context, R.drawable.ic_check)
                        else null,
                        measurement.label
                    ) {
                        changeMeasurement(measurement)
                        menu.hide()
                    }
                }
        }
    }

    private fun openSrcUnitSelector() {
        launcher.showOptionMenu { menu ->
            val arrId = when (selectedMeasurement) {
                MeasurementType.LENGTH -> R.array.length_unit_selector_entries
                MeasurementType.WEIGHT -> R.array.weight_unit_selector_entries
            }

            context.resources.getStringArray(arrId)
                .map { MeasurementUnit.valueOf(it) }
                .forEach { unit ->
                    menu.addItem(
                        if (selectedSrcUnit == unit)
                            AppCompatResources.getDrawable(context, R.drawable.ic_check)
                        else null,
                        unit.label
                    ) {
                        changeSrcUnit(unit)
                        menu.hide()
                    }
                }
        }
    }

    private fun openDestUnitSelector() {
        launcher.showOptionMenu { menu ->
            val arrId = when (selectedMeasurement) {
                MeasurementType.LENGTH -> R.array.length_unit_selector_entries
                MeasurementType.WEIGHT -> R.array.weight_unit_selector_entries
            }

            context.resources.getStringArray(arrId)
                .map { MeasurementUnit.valueOf(it) }
                .forEach { unit ->
                    menu.addItem(
                        if (selectedDestUnit == unit)
                            AppCompatResources.getDrawable(context, R.drawable.ic_check)
                        else null,
                        unit.label
                    ) {
                        changeDestUnit(unit)
                        menu.hide()
                    }
                }
        }
    }

    private fun changeMeasurement(measurementType: MeasurementType) {
        selectedMeasurement = measurementType
    }

    private fun changeSrcUnit(newUnit: MeasurementUnit) {
        selectedSrcUnit = newUnit
        startConversion(
            binding.srcUnitValueEditText,
            selectedSrcUnit,
            binding.destUnitValueEditText,
            selectedDestUnit,
        )
    }

    private fun changeDestUnit(newUnit: MeasurementUnit) {
        selectedDestUnit = newUnit
        startConversion(
            binding.srcUnitValueEditText,
            selectedSrcUnit,
            binding.destUnitValueEditText,
            selectedDestUnit,
        )
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