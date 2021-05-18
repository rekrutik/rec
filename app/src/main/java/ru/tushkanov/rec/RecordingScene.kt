package ru.tushkanov.rec

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner


interface RecordingScene {
    companion object {
        fun create(contract: Contract): RecordingScene =
            Impl(contract)
    }

    fun bind(vm: RecordingViewModel)

    interface Contract {
        companion object {
            fun create(parent: View, lifecycleOwner: LifecycleOwner): Contract = GenericContract(parent, lifecycleOwner)
        }

        val lifecycleOwner: LifecycleOwner
        val context: Context
        val parent: View
        val histogram: HistogramView
        val recordingButton: FrameLayout
        val recordingButtonPane: View
        val playButton: FrameLayout
        val playButtonPane: View
        val resetButton: FrameLayout
        val resetButtonPane: View
        val recordingButtonStartIcon: ImageView
        val recordingButtonStopIcon: ImageView
        val playButtonPlayIcon: ImageView
        val playButtonPauseIcon: ImageView
        val background: FrameLayout
        val recordingButtonTitle: TextView
        val playButtonTitle: TextView

        private class GenericContract(override val parent: View, override val lifecycleOwner: LifecycleOwner) : Contract {
            override val context: Context = parent.context
            override val histogram: HistogramView = parent.findViewById(R.id.histogram)
            override val recordingButton: FrameLayout = parent.findViewById(R.id.recording_button)
            override val recordingButtonPane: View = parent.findViewById(R.id.recording_button_pane)
            override val playButton: FrameLayout = parent.findViewById(R.id.play_button)
            override val playButtonPane: View = parent.findViewById(R.id.play_button_pane)
            override val resetButton: FrameLayout = parent.findViewById(R.id.reset_button)
            override val resetButtonPane: View = parent.findViewById(R.id.reset_button_pane)
            override val recordingButtonStartIcon: ImageView = parent.findViewById(R.id.recording_button_start_icon)
            override val recordingButtonStopIcon: ImageView = parent.findViewById(R.id.recording_button_stop_icon)
            override val playButtonPlayIcon: ImageView = parent.findViewById(R.id.play_button_play_icon)
            override val playButtonPauseIcon: ImageView = parent.findViewById(R.id.play_button_pause_icon)
            override val recordingButtonTitle: TextView = parent.findViewById(R.id.recording_button_title)
            override val playButtonTitle: TextView = parent.findViewById(R.id.play_button_title)
            override val background: FrameLayout = parent.findViewById(R.id.main_content)
        }
    }

    private class Impl(private val contract: Contract) : RecordingScene {

        private var prevState: RecordingViewModel.State = RecordingViewModel.State.Initial

        private var backgroundAnimator: ValueAnimator? = null
        private var histogramAnimator: ValueAnimator? = null
        private var isPlaying = false
        private var lastSeenColor: Int = ContextCompat.getColor(contract.context, R.color.bg_start)

        init {
            enableBouncingClick(contract.recordingButton)
            enableBouncingClick(contract.playButton)
            enableBouncingClick(contract.resetButton)
        }

        override fun bind(vm: RecordingViewModel) {

            contract.recordingButton.setOnClickListener {
                vm.notifyStartRecordRequested()
            }

            contract.playButton.setOnClickListener {
                vm.notifyPlayToggleRequested()
            }

            contract.resetButton.setOnClickListener {
                vm.notifyResetRequested()
            }

            vm.state.observe(contract.lifecycleOwner) { it ->
                when (it) {
                    RecordingViewModel.State.Initial -> {
                        if (prevState !is RecordingViewModel.State.Initial) {
                            histogramAnimator?.cancel()

                            contract.histogram.state?.let {
                                val animator = ValueAnimator()
                                animator.setFloatValues(0f, 1f)
                                animator.addUpdateListener { valueAnimator ->
                                    val fraction = valueAnimator.animatedValue as Float
                                    (contract.histogram.state as? HistogramView.DrawState.Narrow)?.let { h ->
                                        contract.histogram.update(HistogramView.DrawState.Narrow(h.levels, h.preTransition, fraction, h.progress))
                                    } ?: contract.histogram.update(HistogramView.DrawState.Narrow(it.levels, 0f, fraction))

                                }
                                animator.duration = 200
                                animator.start()
                                histogramAnimator = animator
                            }

                            contract.recordingButtonTitle.text = contract.context.getString(R.string.record)
                            contract.recordingButtonStartIcon.clearAnimation()
                            contract.recordingButtonStopIcon.clearAnimation()
                            contract.recordingButtonStartIcon.apply {
                                scaleX = 1f
                                scaleY = 1f
                            }
                            contract.recordingButtonStopIcon.apply {
                                scaleX = 0.5f
                                scaleY = 0.5f
                            }
                            AnimatorSet().apply {
                                play(getHideAnimation(contract.playButtonPane))
                                    .with(getHideAnimation(contract.resetButtonPane))
                                    .with(getAppearAnimation(contract.recordingButtonPane))
                            }.start()
                            animateBackground(ContextCompat.getColor(contract.context, R.color.bg_start))
                        }
                    }
                    is RecordingViewModel.State.Recording -> {
                        if (prevState !is RecordingViewModel.State.Recording) {
                            contract.recordingButtonTitle.text = contract.context.getString(R.string.finish)
                            contract.recordingButtonStartIcon.animate()
                                .scaleX(0.5f)
                                .scaleY(0.5f)
                                .setDuration(200).start()
                            contract.recordingButtonStopIcon.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(200)
                                .start()
                            animateBackground(ContextCompat.getColor(contract.context, R.color.bg_recording))
                            contract.histogram.update(HistogramView.DrawState.Recording(emptyList()))
                        } else {
                            contract.histogram.update(HistogramView.DrawState.Recording(it.amplitudeList))
                        }
                    }
                    is RecordingViewModel.State.Finished -> {
                        if (prevState !is RecordingViewModel.State.Finished) {
                            AnimatorSet().apply {
                                play(getAppearAnimation(contract.playButtonPane))
                                    .with(getAppearAnimation(contract.resetButtonPane).apply { startDelay = 100 })
                                    .with(getHideAnimation(contract.recordingButtonPane))
                            }.start()
                            histogramAnimator?.cancel()
                            val animator = ValueAnimator()
                            animator.setFloatValues(0f, 1f)
                            animator.addUpdateListener { valueAnimator ->
                                val fraction = valueAnimator.animatedValue as Float
                                (contract.histogram.state as? HistogramView.DrawState.Narrow)?.let {
                                    contract.histogram.update(HistogramView.DrawState.Narrow(it.levels, fraction, it.postTransition, it.progress))
                                } ?: contract.histogram.update(HistogramView.DrawState.Narrow(it.amplitudeList, fraction, progress = 1f))
                            }
                            animator.duration = 200
                            animator.start()
                            histogramAnimator = animator
                            animateBackground(ContextCompat.getColor(contract.context, R.color.bg_finish))
                        }
                    }
                }
                prevState = it
            }

            vm.playingState.observe(contract.lifecycleOwner) {
                when (it) {
                    is RecordingViewModel.PlayingState.Paused -> {
                        if (isPlaying) {
                            isPlaying = false
                            contract.playButtonTitle.text = contract.context.getString(R.string.play)
                            contract.playButtonPlayIcon.animate().rotation(0f).scaleX(1f).scaleY(1f).start()
                            contract.playButtonPauseIcon.animate().rotation(-90f).scaleX(0f).scaleY(0f).start()
                        }
                        (contract.histogram.state as? HistogramView.DrawState.Narrow)?.let { h ->
                            contract.histogram.update(HistogramView.DrawState.Narrow(h.levels, h.preTransition, h.postTransition, 1f))
                        }
                    }
                    is RecordingViewModel.PlayingState.Playing -> {
                        if (!isPlaying) {
                            isPlaying = true
                            contract.playButtonTitle.text = contract.context.getString(R.string.pause)
                            contract.playButtonPlayIcon.animate().rotation(90f).scaleX(0f).scaleY(0f).start()
                            contract.playButtonPauseIcon.animate().rotation(0F).scaleX(1f).scaleY(1f).start()
                        }
                        (contract.histogram.state as? HistogramView.DrawState.Narrow)?.let { h ->
                            contract.histogram.update(HistogramView.DrawState.Narrow(h.levels, h.preTransition, h.postTransition, it.progress))
                        }
                    }
                }
            }
        }

        private fun enableBouncingClick(forView: View) = forView.setOnTouchListener { it, e ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        it.animate()
                            .scaleX(0.8f)
                            .scaleY(0.8f)
                            .setDuration(200)
                            .setInterpolator(ViewUtils.DECELERATE_125F)
                            .start()
                    }
                    MotionEvent.ACTION_UP -> {
                        it.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .setInterpolator(ViewUtils.DECELERATE_125F)
                            .start()
                    }
                    else -> {}
                }
                return@setOnTouchListener false
            }

        private fun getAppearAnimation(forView: View) =
            ObjectAnimator.ofFloat(forView, "translationY", 0f.dip(contract.context))
            .setDuration(500)
            .apply {
                interpolator = ViewUtils.OVERSHOOT_1F
            }

        private fun getHideAnimation(forView: View) =
            ObjectAnimator.ofFloat(forView, "translationY", 400f.dip(contract.context))
            .setDuration(500)
            .apply {
                interpolator = ViewUtils.DECELERATE_125F
            }

        private fun animateBackground(to: Int) {
            val from = lastSeenColor
            backgroundAnimator?.cancel()
            val animator = ValueAnimator()
            animator.setIntValues(from, to)
            animator.setEvaluator(ArgbEvaluator())
            animator.addUpdateListener { valueAnimator ->
                val color = valueAnimator.animatedValue as Int
                contract.background.setBackgroundColor(color)
                contract.recordingButtonStartIcon.imageTintList = ColorStateList.valueOf(color)
                contract.recordingButtonStopIcon.imageTintList = ColorStateList.valueOf(color)
                lastSeenColor = color
            }
            animator.duration = 200
            backgroundAnimator = animator
            animator.start()
        }

    }
}