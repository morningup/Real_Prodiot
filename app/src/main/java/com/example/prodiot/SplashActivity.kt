package com.example.prodiot

import android.content.Intent
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.annotations.concurrent.UiThread

class SplashActivity : AppCompatActivity() {
    private lateinit var splashTextView: TextView
    private lateinit var splashImageView: ImageView
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        splashTextView = findViewById(R.id.textView)
        splashImageView = findViewById(R.id.imageView)
        splashAnimation()
    }
    @UiThread
    private fun splashAnimation() {
        val textAnim = AnimationUtils.loadAnimation(this, R.anim.anim_splash_textview)
        splashTextView.startAnimation(textAnim)
        val imageAnim = AnimationUtils.loadAnimation(this, R.anim.anim_splash_imageview)
        splashImageView.startAnimation(imageAnim)

        imageAnim.setAnimationListener(object : AnimationListener{
            override fun onAnimationEnd(animation: Animation){
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                overridePendingTransition(R.anim.anim_splash_out_top, R.anim.anim_splash_in_down)
                finish()
            }

            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {}

        })
    }
}