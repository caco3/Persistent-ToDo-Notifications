package com.example.todonotifications

import android.os.Bundle
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.todonotifications.databinding.ActivitySettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var changed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.switchShowOld.isChecked   = AppPreferences.getShowOldEvents(this)
        binding.switchNearOnly.isChecked  = AppPreferences.getNearOnly(this)
        binding.switchMonthOnly.isChecked = AppPreferences.getMonthOnly(this)
        binding.switchDemoMode.isChecked  = AppPreferences.getDemoMode(this)

        binding.rowShowOld.setOnClickListener   { toggleShowOld() }
        binding.rowNearOnly.setOnClickListener  { toggleNearOnly() }
        binding.rowMonthOnly.setOnClickListener { toggleMonthOnly() }
        binding.rowDemoMode.setOnClickListener  { toggleDemoMode() }

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
        if (newVal) {
            AppPreferences.setNearOnly(this, false)
            AppPreferences.setMonthOnly(this, false)
        }
        syncSwitches()
        changed = true
    }

    private fun toggleNearOnly() {
        val newVal = !binding.switchNearOnly.isChecked
        AppPreferences.setNearOnly(this, newVal)
        if (newVal) {
            AppPreferences.setShowOldEvents(this, false)
            AppPreferences.setMonthOnly(this, false)
        }
        syncSwitches()
        changed = true
    }

    private fun toggleMonthOnly() {
        val newVal = !binding.switchMonthOnly.isChecked
        AppPreferences.setMonthOnly(this, newVal)
        if (newVal) {
            AppPreferences.setShowOldEvents(this, false)
            AppPreferences.setNearOnly(this, false)
        }
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
        binding.switchShowOld.isChecked   = AppPreferences.getShowOldEvents(this)
        binding.switchNearOnly.isChecked  = AppPreferences.getNearOnly(this)
        binding.switchMonthOnly.isChecked = AppPreferences.getMonthOnly(this)
    }
}
