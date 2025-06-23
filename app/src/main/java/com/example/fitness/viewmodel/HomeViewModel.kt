package com.example.fitness.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitness.model.MacroTotals
import com.example.fitness.utils.fetchMacroTotals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
 * Responsible for holding and updating the user's macro totals for a selected day.
 */
class HomeViewModel : ViewModel() {

    // Private mutable state flow to hold macro totals (calories, protein, carbs, fat)
    private val _totals = MutableStateFlow(MacroTotals(0, 0, 0, 0))

    // Public read-only version of the flow exposed to the UI
    val totals = _totals.asStateFlow()

    /**
     * Loads macro totals from Firebase for the given user and date.
     * This triggers recomposition in any Composable observing [totals].
     *
     * @param uid User ID from FirebaseAuth
     * @param date Date string in "yyyy-MM-dd" format
     */
    fun load(uid: String, date: String) {
        viewModelScope.launch {
            _totals.value = fetchMacroTotals(uid, date)
        }
    }
}
