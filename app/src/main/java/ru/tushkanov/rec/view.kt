package ru.tushkanov.rec

import android.content.Context
import android.view.Window

typealias Dp = Int
typealias Sp = Int
typealias Dip = Float
typealias Px = Int

inline fun Dp.dp(context: Context): Px = ViewUtils.dp(context, this.toFloat())

inline fun Dip.dp(context: Context): Px = ViewUtils.dp(context, this)

inline fun Dp.dip(context: Context): Float = ViewUtils.dip(context, this.toFloat())

inline fun Dip.dip(context: Context): Float = ViewUtils.dip(context, this)

inline fun Sp.sp(context: Context): Px = ViewUtils.sp(context, this.toFloat())

inline fun Window.makeTransparent() = ViewUtils.makeTransparentWindow(this)