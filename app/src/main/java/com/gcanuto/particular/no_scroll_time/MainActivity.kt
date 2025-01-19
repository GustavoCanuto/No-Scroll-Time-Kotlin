package com.gcanuto.particular.no_scroll_time

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import android.view.accessibility.AccessibilityManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val accessibilityButton: Button = findViewById(R.id.accessibility_button)
        val toggleSwitch: Switch = findViewById(R.id.toggle_switch)
        val toggleScrollLimit: Switch = findViewById(R.id.toggle_scroll_limit)
        val toggleWebBlock: Switch = findViewById(R.id.toggle_web_block)

        updateUI(accessibilityButton, toggleSwitch, toggleScrollLimit, toggleWebBlock)

        accessibilityButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            setBlockingEnabled(isChecked)
        }

        toggleScrollLimit.setOnCheckedChangeListener { _, isChecked ->
            setScrollLimitEnabled(isChecked)
        }

        toggleWebBlock.setOnCheckedChangeListener { _, isChecked ->
            setWebBlockEnabled(isChecked)
        }
    }

    override fun onResume() {
        super.onResume()

        val accessibilityButton: Button = findViewById(R.id.accessibility_button)
        val toggleSwitch: Switch = findViewById(R.id.toggle_switch)
        val toggleScrollLimit: Switch = findViewById(R.id.toggle_scroll_limit)
        val toggleWebBlock: Switch = findViewById(R.id.toggle_web_block)

        updateUI(accessibilityButton, toggleSwitch, toggleScrollLimit, toggleWebBlock)
    }

    private fun updateUI(
        accessibilityButton: Button,
        toggleSwitch: Switch,
        toggleScrollLimit: Switch,
        toggleWebBlock: Switch
    ) {
        if (isAccessibilityEnabled()) {
            accessibilityButton.visibility = View.GONE
            toggleSwitch.visibility = View.VISIBLE
            toggleScrollLimit.visibility = View.VISIBLE
            toggleWebBlock.visibility = View.VISIBLE
            toggleSwitch.isChecked = isBlockingEnabled()
            toggleScrollLimit.isChecked = isScrollLimitEnabled()
            toggleWebBlock.isChecked = isWebBlockEnabled()
        } else {
            accessibilityButton.visibility = View.VISIBLE
            toggleSwitch.visibility = View.GONE
            toggleScrollLimit.visibility = View.GONE
            toggleWebBlock.visibility = View.GONE
        }
    }

    private fun isScrollLimitEnabled(): Boolean {
        val sharedPref = getSharedPreferences("no_scroll_time_prefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("scroll_limit_enabled", false)
    }

    private fun setScrollLimitEnabled(enabled: Boolean) {
        val sharedPref = getSharedPreferences("no_scroll_time_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("scroll_limit_enabled", enabled)
            apply()
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val colonSplitter = enabledServices.split(":")
        val expectedComponentName = "$packageName/.BlockAccessService"

        for (componentName in colonSplitter) {
            if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return accessibilityManager.isEnabled
    }

    private fun isBlockingEnabled(): Boolean {
        val sharedPref = getSharedPreferences("no_scroll_time_prefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("block_enabled", false)
    }

    private fun setBlockingEnabled(enabled: Boolean) {
        val sharedPref = getSharedPreferences("no_scroll_time_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("block_enabled", enabled)
            apply()
        }
    }

    private fun isWebBlockEnabled(): Boolean {
        val sharedPref = getSharedPreferences("no_scroll_time_prefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("web_block_enabled", false)
    }

    private fun setWebBlockEnabled(enabled: Boolean) {
        val sharedPref = getSharedPreferences("no_scroll_time_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("web_block_enabled", enabled)
            apply()
        }
    }
}