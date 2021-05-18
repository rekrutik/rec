package ru.tushkanov.rec

import android.Manifest
import android.app.Application
import androidx.lifecycle.*
import java.io.File

class RecordingViewModelImpl(application: Application) : RecordingViewModel, AndroidViewModel(application), LifecycleObserver {

    override val state = MutableLiveData<RecordingViewModel.State>(RecordingViewModel.State.Initial)
    private lateinit var audioRecorder: AudioRecorder
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var permissionManager: PermissionManager
    private var playingStateInternal = MutableLiveData<RecordingViewModel.PlayingState>(RecordingViewModel.PlayingState.Paused)
    override val openSettingsRequest = MutableLiveData<Unit>()
    override val playingState = playingStateInternal.distinctUntilChanged()

    override fun notifyStartRecordRequested() {
        state.value?.let {
            when (it) {
                RecordingViewModel.State.Initial -> {
                    when (permissionManager.checkPermission(Manifest.permission.RECORD_AUDIO)) {
                        PermissionManager.PermissionState.ALLOWED -> {
                            val outputFile: File = File.createTempFile("rec", ".m4a", getApplication<Application>().cacheDir)
                            audioRecorder.startRecording(outputFile.absolutePath)
                        }
                        else -> {
                            permissionManager.requestPermission(Manifest.permission.RECORD_AUDIO)
                        }
                    }

                }
                is RecordingViewModel.State.Recording -> {
                    audioRecorder.stopRecorder()
                    state.postValue(RecordingViewModel.State.Finished(amplitudeList))
                }
                else -> {}
            }
        }
    }

    override fun notifyPlayToggleRequested() {
        when (audioPlayer.state.value) {
            is AudioPlayer.State.Prepared -> {
                audioPlayer.play()
            }
            is AudioPlayer.State.Playing -> {
                audioPlayer.stop()
            }
        }
    }

    override fun notifyResetRequested() {
        audioPlayer.stop()
        amplitudeList = mutableListOf<Float>()
        state.postValue(RecordingViewModel.State.Initial)
    }


    fun inject(audioRecorder: AudioRecorder, audioPlayer: AudioPlayer, permissionManager: PermissionManager) {
        this.audioRecorder = audioRecorder
        this.audioPlayer = audioPlayer
        this.permissionManager = permissionManager
    }

    private var amplitudeList = mutableListOf<Float>()

    fun notifyRecorderStateChanged(state: AudioRecorder.State) {
        when (state) {
            is AudioRecorder.State.Recording -> {
                if (this.state.value !is RecordingViewModel.State.Finished) {
                    amplitudeList.add(state.amplitude)
                    this.state.postValue(RecordingViewModel.State.Recording(amplitudeList))
                }
            }
            is AudioRecorder.State.Idle -> {
                audioRecorder.path?.let {
                    audioPlayer.setTrack(it)
                }
            }
        }
    }

    fun notifyPlayerStateChanged(state: AudioPlayer.State) {
        when (state) {
            is AudioPlayer.State.Playing -> playingStateInternal.postValue(RecordingViewModel.PlayingState.Playing(state.progress))
            else -> playingStateInternal.postValue(RecordingViewModel.PlayingState.Paused)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun onPause() {
        state.value?.let {
            when (it) {
                is RecordingViewModel.State.Recording -> {
                    audioRecorder.stopRecorder()
                    state.postValue(RecordingViewModel.State.Finished(amplitudeList))
                }
                else -> {}
            }
        }
        audioPlayer.stop()
    }
}