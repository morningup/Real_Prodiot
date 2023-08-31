package com.example.prodiot

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()
        val loginButton = findViewById<Button>(R.id.btn_login)
        loginButton.setOnClickListener {
            login()
        }
        // 페이지 이동 함수
        val moveToAnotherPage = { destination: Class<*> ->
            startActivity(Intent(this, destination))
            overridePendingTransition(R.anim.left_in, R.anim.left_out)
        }
        // 이벤트 핸들러
        findViewById<Button>(R.id.btn_signin).setOnClickListener {
            moveToAnotherPage(SignUpEmail::class.java)
        }
    }

    private fun login() {
        val emailEditText = findViewById<EditText>(R.id.login_id)
        val passwordEditText = findViewById<EditText>(R.id.login_pw)
        val email = emailEditText.text.toString()
        val password = passwordEditText.text.toString()
        if (email.isEmpty() || password.isEmpty()) { // 필수 정보가 입력되지 않은 경우 처리
            return
        }
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {   // 로그인 성공
                val user = auth.currentUser
                if (user != null) { // 로그인 성공 후 처리할 작업을 여기에 추가
                    Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()
                    // 예: 다음 화면으로 이동
                    val intent = Intent(this, MainMenu::class.java)
                    startActivity(intent)
                    finish()
                }
            }
            else { // 로그인 실패
                Toast.makeText(this, "로그인 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
