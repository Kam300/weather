package com.example.weathertyre

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ThemeBottomSheetFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_theme_bottom_sheet, container, false)

    }

    override fun onViewCreated(@NonNull view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lightThemeButton: Button = view.findViewById(R.id.lightThemeButton)
        val darkThemeButton: Button = view.findViewById(R.id.darkThemeButton)

        lightThemeButton.setOnClickListener {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            saveThemePreference(false)
            dismiss()  // закрываем окно после выбора
        }

        darkThemeButton.setOnClickListener {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            saveThemePreference(true)
            dismiss()  // закрываем окно после выбора
        }
    }

    private fun saveThemePreference(isDarkMode: Boolean) {
        requireActivity().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("dark_mode", isDarkMode)
            .apply()
    }
}
