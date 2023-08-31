package com.example.prodiot

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

class AppUserUpdate : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var nameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var iconButton: Button
    private lateinit var storageReference: StorageReference
    private lateinit var databaseReference: DatabaseReference
    private lateinit var imageView: ImageView

    companion object {
        private const val REQUEST_IMAGE_SELECT = 1
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_userupdate)

        imageView = findViewById(R.id.imageView)
        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference.child("users")
        val currentUserRef2 = databaseReference.child(auth.currentUser?.uid ?: "")
        currentUserRef2.child("imageUrl").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val imageUrl = dataSnapshot.getValue(String::class.java)
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this@AppUserUpdate)
                        .load(imageUrl)
                        .into(imageView)
                    imageView.requestFocus() // 포커스 요청
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle database error if needed
            }
        })


        nameEditText = findViewById(R.id.et_subname)
        passwordEditText = findViewById(R.id.et_supass)

        iconButton = findViewById(R.id.btn_icon)
        iconButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_IMAGE_SELECT)
        }

        // 페이지 이동 함수
        val moveToAnotherPage = { destination: Class<*> ->
            startActivity(Intent(this, destination))
            overridePendingTransition(R.anim.left_in, R.anim.left_out)
        }

        // 이벤트 핸들러
        findViewById<Button>(R.id.btn_update).setOnClickListener {
            updateUserInformation()
            moveToAnotherPage(AppUserInfo::class.java)
        }

        auth = FirebaseAuth.getInstance()
        storageReference = FirebaseStorage.getInstance().reference
        databaseReference = FirebaseDatabase.getInstance().reference.child("users")
    }

    private fun updateUserInformation() {
        val user = auth.currentUser
        val uid = user?.uid
        val newName = nameEditText.text.toString()
        val newPassword = passwordEditText.text.toString()
        //이름과 패스워드 둘다 빈칸이 아니여야지만 변경가능 및 둘중하나 라도 있으면 그것만 변경
        if (newName.isNotEmpty() || newPassword.isNotEmpty()) {
            if (newName.isNotEmpty()) {
                val database = FirebaseDatabase.getInstance() // Firebase 데이터베이스 인스턴스를 초기화합니다.
                val usersRef = database.getReference("users") // "users" 테이블에 대한 참조를 가져옵니다.
                usersRef.addListenerForSingleValueEvent(object : ValueEventListener { // 기본 키 값을 가져오기 위한 쿼리를 실행합니다.
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (snapshot in dataSnapshot.children) {
                            val key = snapshot.key
                            Log.d("uid","$uid")
                            Log.d("key","$key")
                            if (uid == key) {
                                usersRef.child("$uid").child("name").setValue(newName)
                            }
                        }
                    }
                    override fun onCancelled(databaseError: DatabaseError) {
                        // 오류 처리를 수행합니다.
                    }
                })
            }
            if (newPassword.isNotEmpty()) {
                user?.updatePassword(newPassword)?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // 비밀번호 변경 성공
                        Toast.makeText(this, "비밀번호 변경 성공", Toast.LENGTH_SHORT).show()
                    } else {
                        // 비밀번호 변경 실패
                        Toast.makeText(this, "비밀번호가 공란입니다", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_SELECT && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data
            val filename = auth.currentUser?.uid + "_" + UUID.randomUUID().toString() // 랜덤 UID 생성
            val imageRef = storageReference.child("userimages/$filename")

            if (imageUri != null) {
                // 이전 이미지 삭제 및 새 이미지 등록
                val currentUserRef = databaseReference.child(auth.currentUser?.uid ?: "")
                currentUserRef.child("imageUrl").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val imageUrl = dataSnapshot.getValue(String::class.java)
                        if (imageUrl != null) {
                            // 이전 이미지 파일 삭제
                            val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
                            storageReference.delete().addOnSuccessListener {
                                // 이미지 파일 삭제 성공
                                uploadNewImage(imageUri, imageRef, currentUserRef)
                            }.addOnFailureListener {
                                // 이미지 파일 삭제 실패
                                Toast.makeText(this@AppUserUpdate, "이미지 파일 삭제 실패", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // imageUrl 필드가 null인 경우
                            uploadNewImage(imageUri, imageRef, currentUserRef)
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // 취소된 경우
                        Toast.makeText(this@AppUserUpdate, "데이터베이스 오류: $databaseError", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }

    private fun uploadNewImage(imageUri: Uri, imageRef: StorageReference, currentUserRef: DatabaseReference) {
        imageRef.putFile(imageUri).addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                val newImageUrl = uri.toString()
                currentUserRef.child("imageUrl").setValue(newImageUrl).addOnSuccessListener {
                    Toast.makeText(this@AppUserUpdate, "이미지 업로드 및 저장 성공", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(this@AppUserUpdate, "이미지 저장 실패", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this@AppUserUpdate, "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this@AppUserUpdate, "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
        }
    }

}
