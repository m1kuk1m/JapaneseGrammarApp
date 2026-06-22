package com.example.japanesegrammarapp.ui

import androidx.lifecycle.ViewModel
import com.example.japanesegrammarapp.domain.repository.UiPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    val uiPreferencesRepository: UiPreferencesRepository
) : ViewModel()
