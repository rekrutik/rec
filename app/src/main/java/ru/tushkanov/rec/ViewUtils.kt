package ru.tushkanov.rec

import android.animation.TimeInterpolator
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Window
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

class ViewUtils {
    companion object {
        val DECELERATE_125F: TimeInterpolator = DecelerateInterpolator(1.25f)
        val OVERSHOOT_25F: TimeInterpolator = OvershootInterpolator(2.5f)
        val OVERSHOOT_1F: TimeInterpolator = OvershootInterpolator(1f)

        fun dp(ctx: Context, value: Float): Int {
            return dp(ctx.resources, value)
        }

        fun dp(resources: Resources, value: Float): Int {
            return dp(resources.displayMetrics, value)
        }

        fun dp(dm: DisplayMetrics, value: Float): Int {
            return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, dm))
        }

        fun dip(ctx: Context, value: Float): Float {
            return dip(ctx.resources, value)
        }

        fun dip(resources: Resources, value: Float): Float {
            return dip(resources.displayMetrics, value)
        }

        fun dip(dm: DisplayMetrics, value: Float): Float {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, dm)
        }

        fun sp(ctx: Context, value: Float): Int {
            return sp(ctx.resources, value)
        }

        fun sp(resources: Resources, value: Float): Int {
            return sp(resources.displayMetrics, value)
        }

        fun sp(dm: DisplayMetrics, value: Float): Int {
            return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, dm))
        }

        fun density(ctx: Context): Float {
            return ctx.resources.displayMetrics.density
        }

        fun makeTransparentWindow(window: Window) {
            setWindowFlag(window, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
        }

        fun setWindowFlag(window: Window, bits: Int, on: Boolean) {
            val winParams: WindowManager.LayoutParams = window.attributes
            if (on) {
                winParams.flags = winParams.flags or bits
            } else {
                winParams.flags = winParams.flags and bits.inv()
            }
            window.attributes = winParams
        }
    }
}