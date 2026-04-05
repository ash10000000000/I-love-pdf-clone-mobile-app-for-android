package com.pdfox.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.pdfox.app.R
import timber.log.Timber

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        setupThemePreference()
        setupSaveLocationPreference()
        setupVersionPreference()
        setupPrivacyPreference()
        setupLicensesPreference()
    }

    private fun setupThemePreference() {
        val themePreference = findPreference<ListPreference>("theme_preference")
        themePreference?.setOnPreferenceChangeListener { _, newValue ->
            val themeValue = newValue as String
            applyTheme(themeValue)
            true
        }

        val savedTheme = themePreference?.value ?: "system"
        themePreference?.summary = getThemeSummary(savedTheme)
    }

    private fun applyTheme(themeValue: String) {
        val nightMode = when (themeValue) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            "system" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
        Timber.d("Theme changed to: $themeValue")
    }

    private fun getThemeSummary(themeValue: String): String {
        return when (themeValue) {
            "light" -> getString(R.string.theme_light)
            "dark" -> getString(R.string.theme_dark)
            "system" -> getString(R.string.theme_system)
            else -> getString(R.string.theme_system)
        }
    }

    private fun setupSaveLocationPreference() {
        val saveLocationPreference = findPreference<Preference>("save_location_preference")
        saveLocationPreference?.setOnPreferenceClickListener {
            showSaveLocationDialog()
            true
        }
    }

    private fun showSaveLocationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.settings_save_location))
            .setMessage("Files are saved in the PDFox folder within your app's external storage directory.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun setupVersionPreference() {
        val versionPreference = findPreference<Preference>("version_preference")
        val versionName = try {
            val packageInfo = requireContext().packageManager.getPackageInfo(
                requireContext().packageName,
                0
            )
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
        versionPreference?.summary = versionName
    }

    private fun setupPrivacyPreference() {
        val privacyPreference = findPreference<Preference>("privacy_preference")
        privacyPreference?.setOnPreferenceClickListener {
            openUrl("https://pdfox.app/privacy")
            true
        }
    }

    private fun setupLicensesPreference() {
        val licensesPreference = findPreference<Preference>("licenses_preference")
        licensesPreference?.setOnPreferenceClickListener {
            showLicensesDialog()
            true
        }
    }

    private fun showLicensesDialog() {
        val licensesText = """
            PDFox uses the following open-source libraries:
            
            - PDFBox Android (Apache 2.0)
            - Apache POI (Apache 2.0)
            - Hilt (Apache 2.0)
            - Coil (Apache 2.0)
            - Lottie (Apache 2.0)
            - Timber (Apache 2.0)
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.settings_licenses))
            .setMessage(licensesText)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to open URL: $url")
        }
    }

    companion object {
        fun applySavedTheme(context: android.content.Context) {
            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            val themeValue = prefs.getString("theme_preference", "system") ?: "system"

            val nightMode = when (themeValue) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                "system" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }
}
