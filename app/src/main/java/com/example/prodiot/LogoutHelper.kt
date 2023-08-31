package com.example.prodiot

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class LogoutHelper(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun logout() {
        auth.signOut()
        Toast.makeText(context, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }
}
