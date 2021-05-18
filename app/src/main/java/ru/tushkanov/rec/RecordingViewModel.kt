package ru.tushkanov.rec
import androidx.lifecycle.LiveData

interface RecordingViewModel {

    sealed class State {
        object Initial : State()
        data class Recording(val amplitudeList: List<Float>) : State()
        data class Finished(val amplitudeList: List<Float>) : State()
    }

    sealed class PlayingState {
        object Paused : PlayingState()
        data class Playing(val progress: Float) : PlayingState()
    }

    val state: LiveData<State>
    val playingState: LiveData<PlayingState>
    val openSettingsRequest: LiveData<Unit>

    fun notifyStartRecordRequested()
    fun notifyPlayToggleRequested()
    fun notifyResetRequested()
}