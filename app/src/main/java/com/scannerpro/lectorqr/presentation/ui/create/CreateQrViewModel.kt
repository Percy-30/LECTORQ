package com.scannerpro.lectorqr.presentation.ui.create

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scannerpro.lectorqr.domain.usecase.GenerateQrUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateQrUiState(
    val inputText: String = "",
    val qrBitmap: Bitmap? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class CreateQrViewModel @Inject constructor(
    private val generateQrUseCase: GenerateQrUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateQrUiState())
    val uiState: StateFlow<CreateQrUiState> = _uiState.asStateFlow()

    fun onTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun generateQr() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val bitmap = generateQrUseCase(_uiState.value.inputText)
            _uiState.update { it.copy(qrBitmap = bitmap, isLoading = false) }
        }
    }
}
