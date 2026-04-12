package com.example.todonotifications

import android.os.Bundle
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.todonotifications.databinding.ActivitySettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var changed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.switchShowOld.isChecked  = AppPreferences.getShowOldEvents(this)
        binding.switchDemoMode.isChecked = AppPreferences.getDemoMode(this)

        val daysBefore = AppPreferences.getDaysBefore(this)
        binding.sliderDaysBefore.stepSize = 1f
        binding.sliderDaysBefore.value = daysBefore.coerceIn(0, 365).toFloat()
        binding.textDaysBeforeValue.text = "$daysBefore days"

        val daysAfter = AppPreferences.getDaysAfter(this)
        binding.sliderDaysAfter.stepSize = 1f
        binding.sliderDaysAfter.value = daysAfter.coerceIn(0, 365).toFloat()
        binding.textDaysAfterValue.text = "$daysAfter days"

        binding.rowShowOld.setOnClickListener  { toggleShowOld() }
        binding.rowDemoMode.setOnClickListener { toggleDemoMode() }

        binding.sliderDaysBefore.addOnChangeListener(Slider.OnChangeListener { _, value, _ ->
            val v = value.toInt()
            AppPreferences.setDaysBefore(this, v)
            binding.textDaysBeforeValue.text = "$v days"
            changed = true
        })

        binding.sliderDaysAfter.addOnChangeListener(Slider.OnChangeListener { _, value, _ ->
            val v = value.toInt()
            AppPreferences.setDaysAfter(this, v)
            binding.textDaysAfterValue.text = "$v days"
            changed = true
        })

        binding.textCalendarName.text = AppPreferences.getCalendarName(this)
        binding.rowCalendarName.setOnClickListener { pickCalendarName() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        super.onStop()
        if (changed) TodoForegroundService.startOrUpdate(this)
    }

    private fun toggleShowOld() {
        val newVal = !binding.switchShowOld.isChecked
        AppPreferences.setShowOldEvents(this, newVal)
        syncSwitches()
        changed = true
    }

    private fun pickCalendarName() {
        val input = EditText(this).apply {
            setText(AppPreferences.getCalendarName(this@SettingsActivity))
            hint = AppPreferences.DEFAULT_CALENDAR_NAME
            setPadding(48, 24, 48, 8)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.menu_calendar_name)
            .setMessage(R.string.calendar_name_message)
            .setView(input)
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    AppPreferences.setCalendarName(this, name)
                    binding.textCalendarName.text = name
                    changed = true
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun toggleDemoMode() {
        val newVal = !binding.switchDemoMode.isChecked
        AppPreferences.setDemoMode(this, newVal)
        binding.switchDemoMode.isChecked = newVal
        changed = true
    }

    private fun syncSwitches() {
        binding.switchShowOld.isChecked = AppPreferences.getShowOldEvents(this)
    }
}
