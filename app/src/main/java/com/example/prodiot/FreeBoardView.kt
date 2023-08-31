package com.example.prodiot

import OptionMenuHandler
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.ContentObserver
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.text.SimpleDateFormat
import java.util.*

class FreeBoardView : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var bottomNavigationHelper: BottomNavigationHelper
    private lateinit var optionMenuHandler: OptionMenuHandler
    private lateinit var webViewHelper: PostWebViewHelper
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var TitleView: TextView
    private lateinit var ContentView: TextView
    private lateinit var AuthorView: TextView
    private lateinit var CodeView: TextView
    private lateinit var InputView: TextView
    private lateinit var CreatedAtView: TextView
    private var selectedPostId: String = ""
    private lateinit var imageRecyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var commentEditText: TextView

    private val selectedImageUris: ArrayList<Uri> = ArrayList()
    private val PICK_IMAGE_REQUEST = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_freeboardview)

        // 네비게이션 아이템 클릭 리스너 설정
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationHelper = BottomNavigationHelper(this, bottomNavigationView)

        TitleView = findViewById(R.id.title_edittext)
        ContentView = findViewById(R.id.content_edittext)
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
        selectedPostId = key.toString()
        imageRecyclerView = findViewById(R.id.image_recyclerView)
        imageAdapter = ImageAdapter()
        imageRecyclerView.layoutManager = LinearLayoutManager(this)
        imageRecyclerView.adapter = imageAdapter
        // 리사이클러뷰 설정
        recyclerView = findViewById(R.id.commentsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        // OptionMenuHandler 초기화
        optionMenuHandler = OptionMenuHandler(this)


        fetchUserFromFirebase(uid)
        fetchPostDataFromFirebase(selectedPostId)
        // 게시물 조회 함수 호출
        retrievePostFromFirebase(key.toString())
        // 댓글 출력
        retrieveCommentsFromDatabase(selectedPostId)
        // 툴바 설정
        setSupportActionBar(findViewById(R.id.toolbar))

        // 페이지 이동 함수
        val moveToAnotherPage = { destination: Class<*> ->
            startActivity(Intent(this, destination))
            overridePendingTransition(R.anim.left_in, R.anim.left_out)
        }

        btn_comment.setOnClickListener {
            val sharedPreferences = getSharedPreferences("selectedComment", MODE_PRIVATE)
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
            val sharedPref = getSharedPreferences("post_data", MODE_PRIVATE)
            val progressDialog = CustomProgressDialog(this)
            progressDialog.show()
            val editor = sharedPref.edit()
            editor.putString("CodeString", CodeView.text.toString())
            editor.putString("InputString", InputView.text.toString())
            editor.apply()

            webViewHelper = PostWebViewHelper(this)
            webViewHelper.configureWebView(webView)
            webViewHelper.submitCode(webView, progressDialog)
        }

        Delete_content.setOnClickListener{
            deleteContentWithCommentsAndImages(key.toString())
            moveToAnotherPage(FreeBoardList::class.java)
        }

        Change_content.setOnClickListener {
            changeContent(key.toString())
            moveToAnotherPage(FreeBoardList::class.java)
        }

        val setupButton: Button = findViewById(R.id.setupButton) // setupButton을 찾음

        setupButton.setOnClickListener {
            // 이미지 리스트 초기화
            selectedImageUris.clear()
            imageAdapter.setImageUrls(emptyList())
            imageAdapter.notifyDataSetChanged()
        }

        // 이벤트 핸들러
        findViewById<Button>(R.id.uploadButton).setOnClickListener {
            openGallery()
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
    private fun retrievePostFromFirebase(postId: String) {
        val firebaseDatabase = FirebaseDatabase.getInstance()
        val databaseReference = firebaseDatabase.reference.child("posts")
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // 데이터베이스에서 해당 게시물에 대한 데이터 가져오기
                val postSnapshot = dataSnapshot.child(postId)
                val postId = postSnapshot.key.toString() // 게시물 아이디 가져오기
                val sharedPref = getSharedPreferences("my_postId", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putString("my_postId", postId)
                editor.apply()
                Log.d("FreeboardView", "post: $postSnapshot")
                val post = postSnapshot.getValue(Post::class.java)

                // 가져온 데이터를 뷰에 설정
                post?.let {
                    TitleView.text = post.title
                    ContentView.text = post.content
                    AuthorView.text = post.author
                    CodeView.text = post.code
                    InputView.text = post.input
                    CreatedAtView.text = post.timestamp.toString()
                    findViewById<Toolbar>(R.id.toolbar).title = post.title
                }
                val sharedPref2 = getSharedPreferences("auth_id", Context.MODE_PRIVATE)
                val editor2 = sharedPref2.edit()
                if (post != null) {
                    editor2.putString("auth_id", post.author)
                }
                editor2.apply()
            }

            override fun onCancelled(error: DatabaseError) {
                // 오류 처리
                Log.e("FreeboardView", "Failed to read post data.", error.toException())
            }
        })
    }

    private fun fetchPostDataFromFirebase(postId: String) {
        val database = FirebaseDatabase.getInstance()
        val postsRef = database.getReference("posts").child(postId)
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val post = dataSnapshot.getValue(Post::class.java)
                post?.imageUrls?.let {
                    imageAdapter.setImageUrls(it)
                    imageAdapter.notifyDataSetChanged()
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // 오류 처리를 수행합니다.
            }
        }
        postsRef.addValueEventListener(valueEventListener)
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


    private fun deleteContentWithCommentsAndImages(key: String) {
        val database = FirebaseDatabase.getInstance()
        val postRef = database.reference.child("posts").child(key)
        val commentsRef = database.reference.child("comments")
        val repliesRef = database.reference.child("replys")
        // 게시글에 대한 댓글을 조회하기 위한 쿼리
        val commentsQuery = commentsRef.orderByChild("postId").equalTo(key)
        commentsQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(commentsSnapshot: DataSnapshot) {
                // 각 댓글에 대한 대댓글 삭제
                for (commentSnapshot in commentsSnapshot.children) {
                    val commentKey = commentSnapshot.key
                    commentKey?.let {
                        // 대댓글 삭제를 위한 쿼리 생성
                        val repliesQuery = repliesRef.orderByChild("parentkey").equalTo(commentKey)
                        repliesQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                // 대댓글 삭제
                                for (replySnapshot in snapshot.children) {
                                    val replyKey = replySnapshot.key
                                    replyKey?.let {
                                        repliesRef.child(it).removeValue()
                                    }
                                }
                                // 댓글 삭제
                                commentsRef.child(commentKey).removeValue()
                            }
                            override fun onCancelled(error: DatabaseError) {
                                // 에러 처리 로직을 추가합니다.
                            }
                        })
                    }
                }
                // 이미지 삭제
                postRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(postSnapshot: DataSnapshot) {
                        val post = postSnapshot.getValue(Post::class.java)
                        post?.let {
                            val imageUrls = post.imageUrls
                            deleteImages(imageUrls)
                        }
                        // 댓글과 대댓글 삭제 후 게시글 삭제
                        postRef.removeValue().addOnSuccessListener {
                            Toast.makeText(this@FreeBoardView, "해당 게시글을 삭제했습니다.", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener { e ->
                            Toast.makeText(this@FreeBoardView, "게시글 삭제에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        // 에러 처리 로직
                    }
                })
            }
            override fun onCancelled(error: DatabaseError) {
                // 에러 처리 로직
            }
        })
    }

    // 이미지를 삭제하는 함수
    private fun deleteImages(imageUrls: List<String>) {
        val storage = FirebaseStorage.getInstance()
        for (imageUrl in imageUrls) {
            // 이미지 URL에서 파일명을 추출하여 참조 생성
            val fileName = imageUrl.substringAfterLast("2F").substringBefore("?")
            val imageRef: StorageReference = storage.getReferenceFromUrl("gs://prodiot.appspot.com/images/$fileName")
            // 이미지 파일 삭제
            imageRef.delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 이미지 삭제 성공
                } else {
                    // 이미지 삭제 실패
                }
            }
        }
    }



    private fun changeContent(key: String) {
        val sharedPref1 = getSharedPreferences("user_name", Context.MODE_PRIVATE)
        val user_name = sharedPref1.getString("user_name", "")
        val sharedPref2 = getSharedPreferences("auth_id", Context.MODE_PRIVATE)
        val auth_id = sharedPref2.getString("auth_id", "")
        if (user_name == auth_id) {
            val database = FirebaseDatabase.getInstance()
            val postRef = database.reference.child("posts").child(key)
            val dataFormat = "yyyyMMdd HH:mm"
            val date = Date(System.currentTimeMillis())
            val simpleDateFormat = SimpleDateFormat(dataFormat)
            val simpleDate: String = simpleDateFormat.format(date)
            val simpleDateParse: Date = simpleDateFormat.parse(simpleDate)
            postRef.child("title").setValue(TitleView.text.toString())
            postRef.child("content").setValue(ContentView.text.toString())
            postRef.child("author").setValue(AuthorView.text.toString())
            postRef.child("code").setValue(CodeView.text.toString())
            postRef.child("input").setValue(InputView.text.toString())
            postRef.child("timestamp").setValue(simpleDateParse)

                .addOnSuccessListener {
                    Toast.makeText(this, "해당 게시글을 수정했습니다.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "게시글 수정에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            // 이미지 삭제
            postRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(postSnapshot: DataSnapshot) {
                    val post = postSnapshot.getValue(Post::class.java)
                    post?.let {
                        val imageUrls = post.imageUrls
                        deleteImages(imageUrls)
                        uploadImages(postRef.key, selectedImageUris)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    // 에러 처리 로직
                }
            })
        } else {
            Toast.makeText(this, "작성자가 아닙니다.", Toast.LENGTH_SHORT).show()
        }
    }


    // 댓글 추가 함수
    private fun onAddCommentClicked() {
        val commentEditText = findViewById<EditText>(R.id.comment_edittext)
        val commentContent: String = commentEditText.text.toString().trim()
        val currentUser: String = getCurrentUser() // 현재 사용자 ID 가져오는 함수 호출
        val sharedPref = getSharedPreferences("my_postId", Context.MODE_PRIVATE)
        val postId = sharedPref.getString("my_postId", "")
        if (commentContent.isNotEmpty() && currentUser.isNotEmpty()) {
            val postComment = Post_Comment(
                key = UUID.randomUUID().toString(),
                postId = postId, // 게시물 ID 설정
                content = commentContent,
                author = currentUser,
                time = getCurrentTime()
            )
            // commentsManager에 댓글 추가하는 로직 호출
            val commentsRef = FirebaseDatabase.getInstance().getReference("comments")
            commentsRef.push().setValue(postComment)
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
            val postReply = Post_Reply(
                key = UUID.randomUUID().toString(),
                parentkey = selectedComment, // 게시물 ID 설정
                content = replyContent,
                author = currentUser,
                time = getCurrentTime()
            )
            val replysRef = FirebaseDatabase.getInstance().getReference("replys")
            replysRef.push().setValue(postReply)
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
        val sharedPref1 = getSharedPreferences("user_name", Context.MODE_PRIVATE)
        val user_name = sharedPref1.getString("user_name", "")

        return user_name.toString()
    }

    // 댓글 출력 함수
    private fun retrieveCommentsFromDatabase(postId: String) {
        val firebaseDatabase = FirebaseDatabase.getInstance()
        val databaseReference = firebaseDatabase.reference.child("comments")
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val postComments = mutableListOf<Post_Comment>()
                for (commentSnapshot in dataSnapshot.children) {
                    val postComment = commentSnapshot.getValue(Post_Comment::class.java)
                    postComment?.let {
                        val key = commentSnapshot.key // 아이템의 키값 가져오기
                        Log.d("FreeboardView", "post: $key")
                        it.key = key // Post_Comment 객체에 키값 저장
                        Log.d("FreeboardView", "post: $it")
                        if (it.postId == postId) {
                            postComments.add(it)
                            Log.d("FreeboardView", "post: $postComments")
                        }
                    }
                }
                val adapter = Post_CommentAdapter(postComments)
                Log.d("FreeboardView", "post: $adapter")
                recyclerView.adapter = adapter
                Log.d("FreeboardView", "post: ${recyclerView.adapter}")
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // 오류 처리 (선택사항)
            }
        })
    }

    private fun uploadImages(postId: String?, imageUriList: ArrayList<Uri>) {
        val storageRef = FirebaseStorage.getInstance().reference
        val uploadTasks: ArrayList<UploadTask> = ArrayList()
        val imageUrlList: ArrayList<String> = ArrayList()
        for (imageUri in imageUriList) {
            val originalFileName = UUID.randomUUID().toString() // 랜덤 UID 생성
            val imageRef = storageRef.child("images/$originalFileName")
            val uploadTask = imageRef.putFile(imageUri)
            uploadTasks.add(uploadTask)
            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                imageRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUrl = task.result?.toString()
                    downloadUrl?.let { imageUrlList.add(it) }
                    // 모든 이미지 다운로드 URL을 얻었는지 확인
                    if (imageUrlList.size == imageUriList.size) {
                        // 이미지 업로드가 완료되면 게시글에 이미지 URL들을 저장
                        postId?.let { saveImageUrls(it, imageUrlList) }
                    }
                } else {
                    // 이미지 다운로드 URL 가져오기 실패
                }
            }
        }
    }



    // 게시글에 이미지 URL들 저장
    private fun saveImageUrls(postId: String, imageUrlList: ArrayList<String>) {
        val database = FirebaseDatabase.getInstance()
        val postsRef = database.reference.child("posts").child(postId)
        val post = HashMap<String, Any>()
        post["imageUrls"] = imageUrlList
        postsRef.updateChildren(post).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // 이미지 URL 저장 성공
            } else {
                // 이미지 URL 저장 실패
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            if (data.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val imageUri: Uri = data.clipData!!.getItemAt(i).uri
                    selectedImageUris.add(imageUri)
                }
            } else if (data.data != null) {
                val imageUri: Uri = data.data!!
                selectedImageUris.add(imageUri)
            }
            imageAdapter.setImageUrls(selectedImageUris.map { it.toString() })
            imageAdapter.notifyDataSetChanged()
        }
    }

    // 이미지 선택 버튼 클릭 이벤트 핸들러
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(
            Intent.createChooser(intent, "Select Picture"),
            PICK_IMAGE_REQUEST
        )
    }
}
