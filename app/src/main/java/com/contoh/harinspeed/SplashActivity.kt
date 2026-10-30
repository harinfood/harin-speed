package com.contoh.harinspeed

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView

class SplashActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private val lanjut = Runnable {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.parseColor("#0B1016"))

        // Cahaya merah berdenyut di belakang logo
        val cahaya = View(this)
        val radial = GradientDrawable()
        radial.setShape(GradientDrawable.OVAL)
        radial.setGradientType(GradientDrawable.RADIAL_GRADIENT)
        radial.setGradientRadius(dpF(210f))
        radial.setColors(intArrayOf(Color.parseColor("#66FF1744"), Color.parseColor("#00FF1744")))
        cahaya.background = radial

        val logo = ImageView(this)
        logo.setImageResource(R.drawable.logo_speed)
        logo.scaleType = ImageView.ScaleType.FIT_CENTER
        logo.alpha = 0f
        logo.scaleX = 0.6f
        logo.scaleY = 0.6f

        root.addView(cahaya, FrameLayout.LayoutParams(dp(400), dp(400), Gravity.CENTER))
        root.addView(logo, FrameLayout.LayoutParams(dp(270), dp(270), Gravity.CENTER))
        setContentView(root)

        logo.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(900L)
            .setInterpolator(OvershootInterpolator())
            .start()

        ObjectAnimator.ofFloat(cahaya, "scaleX", 0.9f, 1.12f).apply {
            setDuration(900L)
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            start()
        }
        ObjectAnimator.ofFloat(cahaya, "scaleY", 0.9f, 1.12f).apply {
            setDuration(900L)
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            start()
        }

        handler.postDelayed(lanjut, 2800L)
    }

    override fun onDestroy() {
        handler.removeCallbacks(lanjut)
        super.onDestroy()
    }
}
