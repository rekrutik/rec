package ru.tushkanov.rec

import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class AudioRecorder {

    sealed class State {
        object Idle : State()
        object Prepared : State()
        data class Recording(val progress: Long, val amplitude: Float) : State()
    }

    private val recordingThread = HandlerThread("Recording thread").apply { start() }
    private val recordingHandler = Handler(recordingThread.looper)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mediaRecorder by lazy { MediaRecorder() }

    private val stateInternal = MutableLiveData<State>()
    private var active = false
    private var startTime = -1L

    val state: LiveData<State> = stateInternal
    var path: String? = null
        private set

    fun startRecording(path: String) {
        recordingHandler.post {
            if (active) return@post
            mediaRecorder.reset()
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setAudioSamplingRate(44100)
            mediaRecorder.setAudioEncodingBitRate(128000)
            mediaRecorder.setOutputFile(path)
            this.path = path
            mediaRecorder.prepare()
            mainHandler.post {
                stateInternal.postValue(State.Prepared)
            }
            mediaRecorder.start()
            active = true
            startTime = -1L
            tick()
        }
    }

    private fun tick() {
        if (!active) return
        val relTime: Long
        val amp: Int
        if (startTime == -1L) {
            startTime = SystemClock.elapsedRealtime()
            relTime = 0
            var curAmp = mediaRecorder.maxAmplitude
            while (curAmp == 0) {
                HandlerThread.sleep(0, 5000)
                curAmp = mediaRecorder.maxAmplitude
            }
            amp = curAmp
        } else {
            relTime = SystemClock.elapsedRealtime() - startTime
            amp = mediaRecorder.maxAmplitude
        }
        mainHandler.post {
            stateInternal.postValue(State.Recording(relTime, amp / 32768f))
        }
        recordingHandler.postDelayed(::tick, 32)
    }

    fun stopRecorder() {
        recordingHandler.post {
            if (!active) return@post
            mediaRecorder.stop()
            active = false
            mainHandler.post {
                stateInternal.postValue(State.Idle)
            }
        }
    }

    fun release() {
        recordingHandler.post {
            mediaRecorder.release()
            recordingThread.quitSafely()
        }
    }
}