package com.example.prodiot

import OptionMenuHandler
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.text.SimpleDateFormat
import java.util.*

class CodeStepView : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var bottomNavigationHelper: BottomNavigationHelper
    private lateinit var optionMenuHandler: OptionMenuHandler
    private lateinit var webViewHelper: StepWebViewHelper
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var TitleView: TextView
    private lateinit var AuthorView: TextView
    private lateinit var CodeView: TextView
    private lateinit var InputView: TextView
    private lateinit var CreatedAtView: TextView
    private var selectedStepId: String = ""
    private lateinit var commentEditText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_codestepview)

        // 네비게이션 아이템 클릭 리스너 설정
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationHelper = BottomNavigationHelper(this, bottomNavigationView)

        TitleView = findViewById(R.id.title_edittext)
        AuthorView = findViewById(R.id.author_edittext)
        CodeView = findViewById(R.id.code_edittext)
        InputView = findViewById(R.id.input_edittext)
        CreatedAtView = findViewById(R.id.created_at_edittext)
        commentEditText = findViewById<EditText>(R.id.comment_edittext)
        val Change_content = findViewById<Button>(R.id.btn_change)
        val Delete_content = findViewById<Button>(R.id.btn_delete)
        val webView: WebView = findViewById(R.id.webView)
        val movebutton = findViewById<Button>(R.id.btn_run)
        val btn_comment = findViewById<Button>(R.id.btn_comment)
        val key = intent.getStringExtra("selected_item")
        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        val uid = user?.uid
        selectedStepId = key.toString()
        // 리사이클러뷰 설정
        recyclerView = findViewById(R.id.commentsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        // OptionMenuHandler 초기화
        optionMenuHandler = OptionMenuHandler(this)

        fetchUserFromFirebase(uid)
        // 게시물 조회 함수 호출
        retrievePostFromFirebase(key.toString())
        // 댓글 출력
        retrieveCommentsFromDatabase(selectedStepId)
        // 툴바 설정
        setSupportActionBar(findViewById(R.id.toolbar))

        // 페이지 이동 함수
        val moveToAnotherPage = { destination: Class<*> ->
            startActivity(Intent(this, destination))
            overridePendingTransition(R.anim.left_in, R.anim.left_out)
        }

        btn_comment.setOnClickListener {
            val sharedPreferences = getSharedPreferences("selectedComment", Context.MODE_PRIVATE)
            val isReply = sharedPreferences.getBoolean("isReply", false)

            if (isReply) {
                // 대댓글 작성 모드인 경우 content_edittext를 대댓글 작성을 위한 EditText로 설정
                commentEditText.hint = "대댓글 작성"
                commentEditText.requestFocus() // 포커스 요청
                onAddReplyClicked()
            } else {
                // 댓글 작성 모드인 경우 content_edittext를 댓글 작성을 위한 EditText로 설정
                commentEditText.hint = "댓글 작성"
                commentEditText.requestFocus() // 포커스 요청
                onAddCommentClicked()
            }
        }

        movebutton.setOnClickListener {
            val sharedPref = getSharedPreferences("step_data", Context.MODE_PRIVATE)
            val progressDialog = CustomProgressDialog(this)
            progressDialog.show()
            val editor = sharedPref.edit()
            editor.putString("CodeString", CodeView.text.toString())
            editor.putString("InputString", InputView.text.toString())
            editor.apply()

            webViewHelper = StepWebViewHelper(this)
            webViewHelper.configureWebView(webView)
            webViewHelper.submitCode(webView, progressDialog)
        }

        Delete_content.setOnClickListener{
            deleteContent(key.toString())
            moveToAnotherPage(FreeBoardList::class.java)
        }

        Change_content.setOnClickListener {
            changeContent(key.toString())
            moveToAnotherPage(FreeBoardList::class.java)
        }
        // EventBus 등록
        EventBus.getDefault().register(this)

    }

    // 이벤트 수신 메서드
    @Subscribe
    fun onCommentAuthorSelected(event: CommentAuthorSelectedEvent) {
        val fullText = event.selectedAuthor
        val spannableString = SpannableString(fullText)

        // 선택된 작성자 부분을 파란색으로 설정
        val blueColorSpan = ForegroundColorSpan(Color.BLUE)
        spannableString.setSpan(blueColorSpan, 0, fullText.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)

        commentEditText.text = spannableString
    }

    override fun onDestroy() {
        super.onDestroy()

        // EventBus 등록 해제
        EventBus.getDefault().unregister(this)
    }

    // 게시물 조회 함수
    private fun retrievePostFromFirebase(stepId: String) {
        val firebaseDatabase = FirebaseDatabase.getInstance()
        val databaseReference = firebaseDatabase.reference.child("steps")
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // 데이터베이스에서 해당 게시물에 대한 데이터 가져오기
                val stepSnapshot = dataSnapshot.child(stepId)
                val stepId = stepSnapshot.key.toString() // 게시물 아이디 가져오기
                val sharedPref = getSharedPreferences("my_stepId", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putString("my_stepId", stepId)
                editor.apply()
                Log.d("FreeboardView", "step: $stepSnapshot")
                val step = stepSnapshot.getValue(Step::class.java)
                // 가져온 데이터를 뷰에 설정
                step?.let {
                    TitleView.text = step.title
                    CodeView.text = step.code
                    AuthorView.text = step.author
                    InputView.text = step.input
                    CreatedAtView.text = step.timestamp.toString()
                    findViewById<Toolbar>(R.id.toolbar).title = step.title
                }
                val sharedPref2 = getSharedPreferences("auth_id", Context.MODE_PRIVATE)
                val editor2 = sharedPref2.edit()
                if (step != null) { //이게 그 물음표 구조
                    editor2.putString("auth_id", step.author)
                }
                editor2.apply()
            }
            override fun onCancelled(error: DatabaseError) {
                // 오류 처리
                Log.e("CodeStepView", "Failed to read step data.", error.toException())
            }
        })
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


    private fun deleteContent(key: String) {
        val sharedPref1 = getSharedPreferences("user_name", Context.MODE_PRIVATE)
        val user_name = sharedPref1.getString("user_name", "")
        val sharedPref2 = getSharedPreferences("auth_id", Context.MODE_PRIVATE)
        val auth_id = sharedPref2.getString("auth_id", "")

        if (user_name == auth_id) {
            val database = FirebaseDatabase.getInstance()
            val stepRef = database.reference.child("steps").child(key)
            stepRef.removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "해당 게시글을 삭제했습니다.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "게시글 삭제에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "작성자가 아닙니다.", Toast.LENGTH_SHORT).show()
        }

    }

    private fun changeContent(key: String) {
        val sharedPref1 = getSharedPreferences("user_name", Context.MODE_PRIVATE)
        val user_name = sharedPref1.getString("user_name", "")
        val sharedPref2 = getSharedPreferences("auth_id", Context.MODE_PRIVATE)
        val auth_id = sharedPref2.getString("auth_id", "")

        if (user_name == auth_id) {
            val database = FirebaseDatabase.getInstance()
            val stepRef = database.reference.child("steps").child(key)
            val dataFormat = "yyyyMMdd HH:mm"
            val date = Date(System.currentTimeMillis())
            val simpleDateFormat = SimpleDateFormat(dataFormat)
            val simpleDate: String = simpleDateFormat.format(date)
            val simpleDateParse: Date = simpleDateFormat.parse(simpleDate)
            stepRef.child("title").setValue(TitleView.text.toString())
            stepRef.child("author").setValue(AuthorView.text.toString())
            stepRef.child("code").setValue(CodeView.text.toString())
            stepRef.child("input").setValue(InputView.text.toString())
            stepRef.child("timestamp").setValue(simpleDateParse)
                .addOnSuccessListener {
                    Toast.makeText(this, "해당 게시글을 수정했습니다.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "게시글 수정에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "작성자가 아닙니다.", Toast.LENGTH_SHORT).show()
        }

    }


    // 댓글 추가 함수
    private fun onAddCommentClicked() {
        val commentEditText = findViewById<EditText>(R.id.comment_edittext)
        val commentContent: String = commentEditText.text.toString().trim()
        val currentUser: String = getCurrentUser() // 현재 사용자 ID 가져오는 함수 호출
        val sharedPref = getSharedPreferences("my_stepId", Context.MODE_PRIVATE)
        val stepId = sharedPref.getString("my_stepId", "")
        if (commentContent.isNotEmpty() && currentUser.isNotEmpty()) {
            val stepComment = Step_Comment(
                key = UUID.randomUUID().toString(),
                stepId = stepId, // 게시물 ID 설정
                content = commentContent,
                author = currentUser,
                time = getCurrentTime()
            )
            // commentsManager에 댓글 추가하는 로직 호출
            val commentsRef = FirebaseDatabase.getInstance().getReference("comments")
            commentsRef.push().setValue(stepComment)
            // 댓글 작성 후 EditText 초기화
            commentEditText.setText("")
            Toast.makeText(this, "댓글 작성 완료", Toast.LENGTH_SHORT).show()
        }
    }

    //대 댓글 추가 함수
    private fun onAddReplyClicked() {
        // FreeBoardView 액티비티에서 selectedComment 값을 가져옵니다.
        val sharedPreferences = getSharedPreferences("selectedComment", Context.MODE_PRIVATE)
        val selectedComment = sharedPreferences.getString("selectedComment", "")
        val replyEditText = findViewById<EditText>(R.id.comment_edittext)
        val replyContent: String = replyEditText.text.toString().trim()
        val currentUser: String = getCurrentUser() // 현재 사용자 ID 가져오는 함수 호출
        if (replyContent.isNotEmpty() && currentUser.isNotEmpty()) {
            val stepReply = Step_Reply(
                key = UUID.randomUUID().toString(),
                parentkey = selectedComment, // 게시물 ID 설정
                content = replyContent,
                author = currentUser,
                time = getCurrentTime()
            )
            val replysRef = FirebaseDatabase.getInstance().getReference("replys")
            replysRef.push().setValue(stepReply)
            replyEditText.setText("")
            val sharedPreferences = getSharedPreferences("selectedComment", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("isReply", false)
            editor.apply()
            Toast.makeText(this, "대 댓글 작성 완료", Toast.LENGTH_SHORT).show()
        }
    }

    // 현재 시간을 가져오는 함수
    private fun getCurrentTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun getCurrentUser(): String {
        // Firebase 인증 객체 초기화
        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        val currentUser = user?.displayName ?: ""

        return currentUser
    }

    // 댓글 출력 함수
    private fun retrieveCommentsFromDatabase(stepId: String) {
        val firebaseDatabase = FirebaseDatabase.getInstance()
        val databaseReference = firebaseDatabase.reference.child("comments")
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val stepComments = mutableListOf<Step_Comment>()
                for (commentSnapshot in dataSnapshot.children) {
                    val stepComment = commentSnapshot.getValue(Step_Comment::class.java)
                    stepComment?.let {
                        val key = commentSnapshot.key // 아이템의 키값 가져오기
                        Log.d("FreeboardView", "step: $key")
                        it.key = key // Step_Comment 객체에 키값 저장
                        Log.d("FreeboardView", "step: $it")
                        if (it.stepId == stepId) {
                            stepComments.add(it)
                            Log.d("FreeboardView", "step: $stepComments")
                        }
                    }
                }
                val adapter = Step_CommentAdapter(stepComments)
                Log.d("FreeboardView", "step: $adapter")
                recyclerView.adapter = adapter
                Log.d("FreeboardView", "step: ${recyclerView.adapter}")
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // 오류 처리 (선택사항)
            }
        })
    }
}
