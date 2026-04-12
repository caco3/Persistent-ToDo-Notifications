package com.example.todonotifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.TextView
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

        binding.rowAddCalendar.setOnClickListener { addCalendarName() }
        binding.btnAddCalendar.setOnClickListener { addCalendarName() }
        rebuildCalendarList()
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

    private fun addCalendarName() {
        val already = AppPreferences.getCalendarNames(this)
        val available = CalendarTodoSource.getAvailableCalendarNames(this)
            .filter { it !in already }
        if (available.isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.menu_calendar_name)
                .setMessage(R.string.calendar_none_available)
                .setPositiveButton(R.string.dialog_cancel, null)
                .show()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.menu_calendar_name)
            .setItems(available.toTypedArray()) { _, which ->
                val name = available[which]
                val names = AppPreferences.getCalendarNames(this).toMutableSet()
                names.add(name)
                AppPreferences.setCalendarNames(this, names)
                rebuildCalendarList()
                changed = true
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun rebuildCalendarList() {
        val container = binding.containerCalendarNames
        container.removeAllViews()
        val names = AppPreferences.getCalendarNames(this).sorted()
        for (name in names) {
            val row = LayoutInflater.from(this).inflate(R.layout.item_calendar_name, container, false)
            row.findViewById<TextView>(R.id.textCalendarItemName).text = name
            row.findViewById<ImageButton>(R.id.btnRemoveCalendar).setOnClickListener {
                val current = AppPreferences.getCalendarNames(this).toMutableSet()
                current.remove(name)
                AppPreferences.setCalendarNames(this, current)
                rebuildCalendarList()
                changed = true
            }
            container.addView(row)
        }
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
