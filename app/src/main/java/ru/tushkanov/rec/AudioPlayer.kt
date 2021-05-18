package ru.tushkanov.rec

import android.media.MediaPlayer
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class AudioPlayer {

    sealed class State {
        object Idle : State()
        object Prepared : State()
        data class Playing(val progress: Float) : State()
    }

    private val playingThread = HandlerThread("Playing thread").apply { start() }
    private val playingHandler = Handler(playingThread.looper)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mediaPlayer by lazy { MediaPlayer() }

    private val stateInternal = MutableLiveData<State>()
    private var active = false
    private var startTime = -1L

    val state: LiveData<State> = stateInternal

    fun setTrack(path: String) {
        mainHandler.post {
            stateInternal.postValue(State.Idle)
            playingHandler.post {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(path)
                mediaPlayer.prepare()
                mainHandler.post {
                    stateInternal.postValue(State.Prepared)
                }
            }
        }
    }

    fun play() {
        playingHandler.post {
            if (active) return@post
            mediaPlayer.setOnCompletionListener {
                active = false
                startTime = -1L
                mainHandler.post {
                    stateInternal.postValue(State.Prepared)
                }
            }
            mediaPlayer.seekTo(0)
            mediaPlayer.start()
            active = true
            startTime = -1L
            tick()
        }
    }

    private fun tick() {
        if (!active) return
        val relTime: Long
        if (startTime == -1L) {
            startTime = SystemClock.elapsedRealtime()
            relTime = 0
        } else {
            relTime = SystemClock.elapsedRealtime() - startTime
        }
        mainHandler.post {
            stateInternal.postValue(State.Playing(relTime / mediaPlayer.duration.toFloat()))
        }
        playingHandler.postDelayed(::tick, 10)
    }

    fun stop() {
        playingHandler.post {
            if (!active) return@post
            mediaPlayer.pause()
            mediaPlayer.seekTo(0)
            active = false
            startTime = -1L
            mainHandler.post {
                stateInternal.postValue(State.Prepared)
            }
        }
    }

    fun release() {
        playingHandler.post {
            mediaPlayer.release()
            playingThread.quitSafely()
        }
    }
}