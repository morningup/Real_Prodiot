package com.example.prodiot

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.StorageReference


class AppUserInfo : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var userText: TextView
    private lateinit var nameText: TextView
    private lateinit var emailText: TextView
    private lateinit var databaseReference: DatabaseReference
    private lateinit var imageView: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_userinfo)

        // Firebase 인증 객체 초기화
        auth = FirebaseAuth.getInstance()

        userText = findViewById(R.id.usertext)
        nameText = findViewById(R.id.et_name)
        emailText = findViewById(R.id.et_id)

        imageView = findViewById(R.id.imageView)
        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference.child("users")
        val currentUserRef2 = databaseReference.child(auth.currentUser?.uid ?: "")
        currentUserRef2.child("imageUrl").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val imageUrl = dataSnapshot.getValue(String::class.java)
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this@AppUserInfo)
                        .load(imageUrl)
                        .into(imageView)
                    imageView.requestFocus() // 포커스 요청
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle database error if needed
            }
        })



        val moveToAnotherPage = { destination: Class<*> ->
            startActivity(Intent(this, destination))
            overridePendingTransition(R.anim.left_in, R.anim.left_out)
        }

        findViewById<Button>(R.id.btn_user_delete).setOnClickListener {
            deleteCurrentUser()
            moveToAnotherPage(LoginActivity::class.java)
        }
        findViewById<Button>(R.id.btn_user_update).setOnClickListener {
            moveToAnotherPage(AppUserUpdate::class.java)
        }
        findViewById<Button>(R.id.btn_back).setOnClickListener {
            moveToAnotherPage(MainMenu::class.java)
        }
    }

    override fun onResume() {
        super.onResume()
        // 사용자 정보 업데이트
        updateUserInfo()
    }

    private fun updateUserInfo() {
        val user = auth.currentUser
        val uid = user?.uid
        val database = FirebaseDatabase.getInstance() // Firebase 데이터베이스 인스턴스를 초기화합니다.
        val usersRef = database.getReference("users") // "users" 테이블에 대한 참조를 가져옵니다.
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener { // 기본 키 값을 가져오기 위한 쿼리를 실행합니다.
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val key = snapshot.key
                    if (uid == key) {
                        val user = snapshot.getValue(User::class.java)
                        // 가져온 데이터를 뷰에 설정
                        user?.let {
                            userText.text = user.name
                            nameText.text = user.name
                        }
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // 오류 처리를 수행합니다.
            }
        })
        val email = user?.email ?: ""
        emailText.text = email
    }

    private fun deleteCurrentUser() {
        val user = auth.currentUser
        val uid = user?.uid
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users")
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val key = snapshot.key
                    if (uid == key) {
                        usersRef.child(key.toString()).removeValue()
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
        user?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // 사용자 삭제 성공
                Toast.makeText(this, "삭제 성공", Toast.LENGTH_SHORT).show()
            } else {
                // 사용자 삭제 실패
            }
        }
    }
}

