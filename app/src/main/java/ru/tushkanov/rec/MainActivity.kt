package ru.tushkanov.rec

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private val audioRecorder: AudioRecorder by lazy { AudioRecorder() }
    private val audioPlayer: AudioPlayer by lazy { AudioPlayer() }
    private val permissionManager: PermissionManagerImpl by lazy { PermissionManagerImpl(this) }
    private val vm: RecordingViewModelImpl by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.makeTransparent()

        vm.inject(audioRecorder, audioPlayer, permissionManager)
        lifecycle.addObserver(vm)
        audioRecorder.state.observe(this, vm::notifyRecorderStateChanged)
        audioPlayer.state.observe(this, vm::notifyPlayerStateChanged)

        vm.openSettingsRequest.observe(this) {
            permissionManager.openSettings()
        }

        val scene = RecordingScene.create(RecordingScene.Contract.create(window.decorView.rootView, this))
        scene.bind(vm)
    }

    override fun onDestroy() {
        audioRecorder.release()
        audioPlayer.release()
        super.onDestroy()
    }

}