package com.example.prodiot

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var etMail: EditText
    private lateinit var etPw: EditText
    private lateinit var etName: EditText
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // 데이터 가져오기
        etMail = findViewById(R.id.regster_id)
        etPw = findViewById(R.id.regster_pw)
        etName = findViewById(R.id.regster_name)
        val sharedPref = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        val id = sharedPref.getString("id", "")
        val pw = sharedPref.getString("pw", "")
        val name = sharedPref.getString("name", "")
        etMail.setText(id)
        etPw.setText(pw)
        etName.setText(name)

        auth = FirebaseAuth.getInstance()

        val signUpButton = findViewById<Button>(R.id.btn_register)
        signUpButton.setOnClickListener {
            signUp()
        }
    }

    private fun signUp() {
        val email = etMail.text.toString()
        val password = etPw.text.toString()
        val name = etName.text.toString()
        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            // 필수 정보가 입력되지 않은 경우 처리
            Toast.makeText(this, "모든 필수 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) { // 회원가입 성공
                val user: FirebaseUser? = auth.currentUser
                // Update user's display name
                val profileUpdates = userProfileChangeRequest {
                    displayName = name
                }
                if (user != null) {
                    // 회원가입 성공 후 처리할 작업을 여기에 추가
                    user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                        if (profileTask.isSuccessful) { // 사용자 이름 저장 성공
                            // Store name in the database
                            val database = FirebaseDatabase.getInstance()
                            val usersRef = database.getReference("users")
                            val userRef = usersRef.child(user?.uid ?: "")
                            val userData = User()
                            userData.name = name
                            userRef.setValue(userData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show()
                                    // 예: 다음 화면으로 이동
                                    val intent = Intent(this, LoginActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { exception ->
                                    Toast.makeText(this, "회원가입 실패", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            // 사용자 이름 저장 실패
                            Toast.makeText(this, "이름 저장 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                // 회원가입 실패 // 실패 이유에 따라 처리
                Toast.makeText(this, "회원가입 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
