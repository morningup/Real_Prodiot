package com.example.prodiot

import OptionMenuHandler
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Date

class CodeStepWrite : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var bottomNavigationHelper: BottomNavigationHelper
    private lateinit var titleEditText: EditText
    private lateinit var codeEditText: EditText
    private lateinit var inputEditText: EditText
    private lateinit var createButton: Button
    private lateinit var optionMenuHandler: OptionMenuHandler
    private lateinit var auth: FirebaseAuth
    private lateinit var webViewHelper: StepWebViewHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_codestepwrite)

        // 네비게이션 아이템 클릭 리스너 설정
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationHelper = BottomNavigationHelper(this, bottomNavigationView)

        // Firebase 인증 객체 초기화
        auth = FirebaseAuth.getInstance()

        titleEditText = findViewById(R.id.title_edittext)
        codeEditText = findViewById(R.id.code_edittext)
        inputEditText = findViewById(R.id.input_edittext)
        createButton = findViewById(R.id.btn_write_test)

        val user = auth.currentUser
        val uid = user?.uid
        fetchUserFromFirebase(uid)
        val sharedPref1 = getSharedPreferences("user_name", Context.MODE_PRIVATE)
        val user_name = sharedPref1.getString("user_name", "")

        createButton.setOnClickListener {
            val title = titleEditText.text.toString().trim()
            val code = codeEditText.text.toString().trim()
            val user = auth.currentUser
            val author = user_name.toString()
            val input = inputEditText.text.toString().trim()
            val timestamp = Date(System.currentTimeMillis())

            if (title.isNotEmpty() && code.isNotEmpty() && author.isNotEmpty()) {
                saveStep(title, code, author, input, timestamp)
                val intent = Intent(this, CodeStepList::class.java)
                startActivity(intent)
                finish()
            }
        }

        val webView: WebView = findViewById(R.id.webView)
        val movebutton = findViewById<Button>(R.id.btn_run)
        movebutton.setOnClickListener {
            val sharedPref = getSharedPreferences("step_data", Context.MODE_PRIVATE)
            val progressDialog = CustomProgressDialog(this)
            progressDialog.show()
            val editor = sharedPref.edit()
            editor.putString("CodeString", codeEditText.text.toString())
            Log.d("MainActivity", "Output Text: ${codeEditText.toString()}")
            editor.putString("InputString", inputEditText.text.toString())
            editor.apply()

            webViewHelper = StepWebViewHelper(this)
            webViewHelper.configureWebView(webView)
            webViewHelper.submitCode(webView, progressDialog)
        }
    }

    private fun saveStep(title: String, code: String, author: String,
                         input: String, timestamp: Date) {
        val database = FirebaseDatabase.getInstance()
        val stepsRef = database.reference.child("steps")
        val newstepRef = stepsRef.push()

        val step = HashMap<String, Any>()
        step["title"] = title
        step["code"] = code
        step["author"] = author
        step["input"] = input
        step["timestamp"] = timestamp

        newstepRef.setValue(step).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // 게시글 저장 성공
                titleEditText.text.clear()
                codeEditText.text.clear()
                inputEditText.text.clear()
            } else {
                // 게시글 저장 실패
            }
        }

        // 툴바 설정
        setSupportActionBar(findViewById(R.id.toolbar))

        // 페이지 이동 함수
        val moveToAnotherPage = { destination: Class<*> ->
            startActivity(Intent(this, destination))
            overridePendingTransition(R.anim.left_in, R.anim.left_out)
        }
    }
    // 옵션 메뉴 생성
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.mainmenu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return optionMenuHandler.handleItemSelected(item) || super.onOptionsItemSelected(item)
    }

    private fun fetchUserFromFirebase(uid: String?) {
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val key2 = snapshot.key
                    if (uid == key2) {
                        val user = snapshot.getValue(User::class.java)
                        user?.let {
                            val sharedPref3 = getSharedPreferences("user_name", Context.MODE_PRIVATE)
                            val editor3 = sharedPref3.edit()
                            editor3.putString("user_name", user.name)
                            editor3.apply()
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // 오류 처리를 수행합니다.
            }
        })
    }
}
