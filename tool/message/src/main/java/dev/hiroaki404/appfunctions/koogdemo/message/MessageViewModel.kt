package dev.hiroaki404.appfunctions.koogdemo.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MessageViewModel(repository: MessageRepository) : ViewModel() {
    val uiState: StateFlow<MessageUiState> = repository.history
        .map { it.toUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = repository.history.value.toUiState(),
        )
}
