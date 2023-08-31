package com.example.prodiot

import OptionMenuHandler
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class MainMenu : AppCompatActivity() {
    private lateinit var optionMenuHandler: OptionMenuHandler
    private lateinit var auth: FirebaseAuth
    private lateinit var userText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mainmenu)

        userText = findViewById(R.id.tv_app_name)

        // Firebase 인증 객체 초기화
        auth = FirebaseAuth.getInstance()

        // 툴바 설정
        setSupportActionBar(findViewById(R.id.toolbar))

        // 페이지 이동 함수
        val moveToAnotherPage = { destination: Class<*> ->
            startActivity(Intent(this, destination))
            overridePendingTransition(R.anim.left_in, R.anim.left_out)
        }

        // 이벤트 핸들러
        findViewById<Button>(R.id.btn_freeboard).setOnClickListener {
            moveToAnotherPage(FreeBoardList::class.java)
        }
        findViewById<Button>(R.id.btn_codestep).setOnClickListener {
            moveToAnotherPage(CodeStepList::class.java)
        }

        // OptionMenuHandler 초기화
        optionMenuHandler = OptionMenuHandler(this)
    }

    override fun onResume() {
        super.onResume()
        // 사용자 정보 업데이트
        updateUserInfo()
    }

    private fun updateUserInfo() {
        val user = auth.currentUser
        val uid = user?.uid
        // Firebase 데이터베이스 인스턴스를 초기화합니다.
        val database = FirebaseDatabase.getInstance()
        // "users" 테이블에 대한 참조를 가져옵니다.
        val usersRef = database.getReference("users")
        Log.d("uid", "post: $uid")
        // 기본 키 값을 가져오기 위한 쿼리를 실행합니다.
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val key = snapshot.key // 기본 키 값을 가져옵니다.
                    // 가져온 기본 키 값을 사용하여 원하는 작업을 수행합니다.
                    // 예: 기본 키 값에 해당하는 사용자 데이터를 가져오거나 수정합니다.

                    Log.d("key", "post: $key")
                    if (uid == key) {
                        val user = snapshot.getValue(User::class.java)

                        Log.d("user", "post: $user")
                        // 가져온 데이터를 뷰에 설정
                        user?.let {
                            userText.text = ("${user.name} 님 환영합니다.")
                            Log.d("user.name", "post: ${user.name}")
                        }
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // 오류 처리를 수행합니다.
            }
        })
    }

    // 옵션 메뉴 생성
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.mainmenu, menu)
        return true
    }

    // 옵션 메뉴 클릭 핸들러
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return optionMenuHandler.handleItemSelected(item) || super.onOptionsItemSelected(item)
    }
}

