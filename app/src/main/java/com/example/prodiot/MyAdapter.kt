package com.example.prodiot

import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*
import java.util.concurrent.TimeUnit

class freeboardadapter(private val postList: List<Post>) : RecyclerView.Adapter<freeboardadapter.ViewHolder>() {
    private lateinit var auth: FirebaseAuth

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.TitleTextView)
        val authorTextView: TextView = itemView.findViewById(R.id.AuthorTextView)
        val contentTextView: TextView = itemView.findViewById(R.id.ContentTextView)
        val createdAtView: TextView = itemView.findViewById(R.id.DateTimeTextView)
        val userImageView: ImageView = itemView.findViewById(R.id.ProfileImageView)
        val postImageView: ImageView = itemView.findViewById(R.id.PostImageView)
        val commentCountTextView: TextView = itemView.findViewById(R.id.CommentCountTextView)
        val replyCountTextView: TextView = itemView.findViewById(R.id.ReplyCountTextView)
        val viewsTextView: TextView = itemView.findViewById(R.id.viewsTextView)
        val btn_Unfold: Button = itemView.findViewById(R.id.btn_Unfold)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = postList[position]

        holder.titleTextView.text = post.title
        holder.authorTextView.text = post.author
        holder.contentTextView.text = post.content

        val currentTimestamp: Long = System.currentTimeMillis()
        val pastTimestamp: Long = post.timestamp.time
        val timeAgo = calculateTimeAgo(pastTimestamp, currentTimestamp)
        val timeAgo2 = timeAgo.toString()
        holder.createdAtView.text = timeAgo2

        holder.btn_Unfold

        val button = holder.btn_Unfold // 버튼의 ID에 따라 수정해야 함
        val textView = holder.contentTextView
        Log.d("contentTextview","$textView")

        val maxLines = 5 // 최대 줄 수 설정
        var flag = false

        // 텍스트뷰의 레이아웃이 정상적으로 계산될 때까지 기다립니다.
        textView.post {
            val lineCount = textView.lineCount // 텍스트뷰의 줄 수 가져오기
            Log.d("lineCount","$lineCount")

            if (lineCount > maxLines) {
                val layoutParams = textView.layoutParams
                layoutParams.height = textView.lineHeight * maxLines
                textView.layoutParams = layoutParams
                button.visibility = View.VISIBLE // 버튼을 보이게 설정
            } else {
                button.visibility = View.GONE // 버튼을 숨기게 설정
            }
        }

        button.setOnClickListener {
            flag = !flag
            if (flag) {
                button.setBackgroundResource(R.drawable.fold_icon)
                val layoutParams = textView.layoutParams
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                textView.layoutParams = layoutParams
            } else {
                button.setBackgroundResource(R.drawable.unfold_icon)
                val layoutParams = textView.layoutParams
                layoutParams.height = textView.lineHeight * maxLines
                textView.layoutParams = layoutParams
            }
        }

        // Firebase Realtime Database에서 views 값을 가져와서 보여주기
        val firebaseDatabase3 = FirebaseDatabase.getInstance()
        val postsRef3 = firebaseDatabase3.reference.child("posts")
        val postKey3 = postList[position].key.toString()

        postsRef3.child(postKey3).child("views")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val views = dataSnapshot.getValue(Int::class.java) ?: 0
                    val updateviews = views.toString()
                    holder.viewsTextView.text = updateviews
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.d("TAG", "onCancelled: ${databaseError.message}")
                }
            })

        holder.itemView.setOnClickListener {
            if (position != RecyclerView.NO_POSITION) {
                val context = holder.itemView.context
                val intent = Intent(context, FreeBoardView::class.java)
                val selectedKey = postList[position].key
                intent.putExtra("selected_item", selectedKey)
                context.startActivity(intent)

                // Firebase Realtime Database에서 views 값을 1 증가시키기
                val firebaseDatabase = FirebaseDatabase.getInstance()
                val postsRef = firebaseDatabase.reference.child("posts")
                val postKey = postList[position].key.toString()

                postsRef.child(postKey).child("views").runTransaction(object : Transaction.Handler {
                    override fun doTransaction(mutableData: MutableData): Transaction.Result {
                        val views = mutableData.getValue(Int::class.java) ?: 0
                        mutableData.value = views + 1
                        return Transaction.success(mutableData)
                    }

                    override fun onComplete(
                        databaseError: DatabaseError?,
                        committed: Boolean,
                        dataSnapshot: DataSnapshot?
                    ) {
                        if (databaseError != null) {
                            Log.d("TAG", "onComplete: ${databaseError.message}")
                        }
                    }
                })

            }
        }

        // Firebase Realtime Database에서 유저 이미지 URL 가져오기
        val firebaseDatabase = FirebaseDatabase.getInstance()
        val usersRef = firebaseDatabase.reference.child("users")
        auth = FirebaseAuth.getInstance()

        usersRef.child(auth.currentUser?.uid ?: "")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val user = dataSnapshot.getValue(User::class.java)
                        val userImageUrl = user?.imageUrl
                        // 이미지 로드 및 설정
                        Glide.with(holder.itemView.context)
                            .load(userImageUrl)
                            .into(holder.userImageView)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.d("TAG", "onCancelled: ${databaseError.message}")
                }
            })

        // Firebase Realtime Database에서 포스트 이미지 URL 가져오기
        val firebaseDatabase2 = FirebaseDatabase.getInstance()
        val postsRef = firebaseDatabase2.reference.child("posts")
        val postKey = post.key.toString()

        postsRef.child(postKey).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val post = dataSnapshot.getValue(Post::class.java)
                    val imageUrls = post?.imageUrls
                    if (!imageUrls.isNullOrEmpty()) {
                        val firstImageUrl = imageUrls[0] // 첫 번째 이미지 링크 가져오기
                        // 이미지 로드 및 설정
                        Glide.with(holder.itemView.context)
                            .load(firstImageUrl)
                            .into(holder.postImageView)
                        holder.postImageView.visibility = View.VISIBLE // ImageView 표시
                    } else {
                        holder.postImageView.visibility = View.GONE // ImageView 숨김
                    }
                } else {
                    holder.postImageView.visibility = View.GONE // ImageView 숨김
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("TAG", "onCancelled: ${databaseError.message}")
            }
        })


        // Firebase Realtime Database에서 댓글 개수 가져오기
        val commentsRef = firebaseDatabase.reference.child("comments")
        commentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var commentCount = 0
                var replyCount = 0

                for (childSnapshot in dataSnapshot.children) {
                    val postId = childSnapshot.child("postId").value.toString()
                    if (postId == postKey) {
                        commentCount++

                        // 댓글의 키 저장
                        val commentKey = childSnapshot.key
                        Log.d("commentKey", "$commentKey")

                        // 대댓글 개수 가져오기
                        val repliesRef = firebaseDatabase.reference.child("replys")
                        repliesRef.addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                for (replySnapshot in dataSnapshot.children) {
                                    val parentKey =
                                        replySnapshot.child("parentkey").value.toString()
                                    if (parentKey == commentKey) {
                                        replyCount++
                                        Log.d("replyCount", "$replyCount")
                                    }
                                }
                                // 댓글과 대댓글 개수 표시
                                holder.commentCountTextView.text = commentCount.toString()
                                holder.replyCountTextView.text = replyCount.toString()
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                Log.d("TAG", "onCancelled: ${databaseError.message}")
                            }
                        })
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("TAG", "onCancelled: ${databaseError.message}")
            }
        })
    }

    fun calculateTimeAgo(pastTimestamp: Long, currentTimestamp: Long): String {
        val past = Calendar.getInstance()
        val current = Calendar.getInstance()
        past.timeInMillis = pastTimestamp
        current.timeInMillis = currentTimestamp


        val timeInMilli = kotlin.math.abs(current.timeInMillis - past.timeInMillis)

        val days = TimeUnit.MILLISECONDS.toDays(timeInMilli)
        val hours = TimeUnit.MILLISECONDS.toHours(timeInMilli) - TimeUnit.DAYS.toHours(days)
        val minutes =
            TimeUnit.MILLISECONDS.toMinutes(timeInMilli) - TimeUnit.DAYS.toMinutes(days) - TimeUnit.HOURS.toMinutes(
                hours
            )

        if (days > 365) {
            val years = days / 365
            return "$years 년 전"
        } else if (days >= 30) {
            val months = days / 30
            return "$months 개월 전"
        } else if (days > 0) {
            return "$days 일 전"
        } else if (hours > 0) {
            return "$hours 시간 전"
        } else if (minutes > 0) {
            return "$minutes 분 전"
        } else {
            return "방금"
        }
    }
    override fun getItemCount(): Int {
        return postList.size
    }
}

class codestepadapter(private val stepList: List<Step>) : RecyclerView.Adapter<codestepadapter.ViewHolder>() {
    private lateinit var auth: FirebaseAuth

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.TitleTextView)
        val authorTextView: TextView = itemView.findViewById(R.id.AuthorTextView)
        val contentTextView: TextView = itemView.findViewById(R.id.ContentTextView)
        val createdAtView: TextView = itemView.findViewById(R.id.DateTimeTextView)
        val userImageView: ImageView = itemView.findViewById(R.id.ProfileImageView)
        val commentCountTextView: TextView = itemView.findViewById(R.id.CommentCountTextView)
        val replyCountTextView: TextView = itemView.findViewById(R.id.ReplyCountTextView)
        val viewsTextView: TextView = itemView.findViewById(R.id.viewsTextView)
        val btn_Unfold: Button = itemView.findViewById(R.id.btn_Unfold)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val step = stepList[position]
        holder.titleTextView.text = step.title
        holder.authorTextView.text = step.author
        holder.contentTextView.text = step.code

        val currentTimestamp: Long = System.currentTimeMillis()
        val pastTimestamp : Long = step.timestamp.time
        val timeAgo = calculateTimeAgo(pastTimestamp, currentTimestamp)
        val timeAgo2 = timeAgo.toString()
        holder.createdAtView.text = timeAgo2

        holder.btn_Unfold

        val button = holder.btn_Unfold // 버튼의 ID에 따라 수정해야 함
        val textView = holder.contentTextView
        Log.d("contentTextview","$textView")

        val maxLines = 5 // 최대 줄 수 설정
        var flag = false

        // 텍스트뷰의 레이아웃이 정상적으로 계산될 때까지 기다립니다.
        textView.post {
            val lineCount = textView.lineCount // 텍스트뷰의 줄 수 가져오기
            Log.d("lineCount","$lineCount")

            if (lineCount > maxLines) {
                val layoutParams = textView.layoutParams
                layoutParams.height = textView.lineHeight * maxLines
                textView.layoutParams = layoutParams
                button.visibility = View.VISIBLE // 버튼을 보이게 설정
            } else {
                button.visibility = View.GONE // 버튼을 숨기게 설정
            }
        }

        button.setOnClickListener {
            flag = !flag
            if (flag) {
                button.setBackgroundResource(R.drawable.fold_icon)
                val layoutParams = textView.layoutParams
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                textView.layoutParams = layoutParams
            } else {
                button.setBackgroundResource(R.drawable.unfold_icon)
                val layoutParams = textView.layoutParams
                layoutParams.height = textView.lineHeight * maxLines
                textView.layoutParams = layoutParams
            }
        }


        // Firebase Realtime Database에서 views 값을 가져와서 보여주기
        val firebaseDatabase3 = FirebaseDatabase.getInstance()
        val stepsRef3 = firebaseDatabase3.reference.child("steps")
        val stepKey3 = stepList[position].key.toString()

        stepsRef3.child(stepKey3).child("views").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val views = dataSnapshot.getValue(Int::class.java) ?: 0
                val updateviews = views.toString()
                holder.viewsTextView.text = updateviews
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("TAG", "onCancelled: ${databaseError.message}")
            }
        })

        holder.itemView.setOnClickListener {
            if (position != RecyclerView.NO_POSITION) {
                val context = holder.itemView.context
                val intent = Intent(context, CodeStepView::class.java)
                val selectedKey = stepList[position].key
                intent.putExtra("selected_item", selectedKey)
                context.startActivity(intent)

                // Firebase Realtime Database에서 views 값을 1 증가시키기
                val firebaseDatabase = FirebaseDatabase.getInstance()
                val stepRef = firebaseDatabase.reference.child("steps")
                val stepKey = stepList[position].key.toString()

                stepRef.child(stepKey).child("views").runTransaction(object : Transaction.Handler {
                    override fun doTransaction(mutableData: MutableData): Transaction.Result {
                        val views = mutableData.getValue(Int::class.java) ?: 0
                        mutableData.value = views + 1
                        return Transaction.success(mutableData)
                    }

                    override fun onComplete(databaseError: DatabaseError?, committed: Boolean, dataSnapshot: DataSnapshot?) {
                        if (databaseError != null) {
                            Log.d("TAG", "onComplete: ${databaseError.message}")
                        }
                    }
                })

            }
        }

        // Firebase Realtime Database에서 이미지 URL 가져오기
        val firebaseDatabase = FirebaseDatabase.getInstance()
        val usersRef = firebaseDatabase.reference.child("users")
        auth = FirebaseAuth.getInstance()

        usersRef.child(auth.currentUser?.uid ?: "").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val user = dataSnapshot.getValue(User::class.java)
                    val userImageUrl = user?.imageUrl

                    // 이미지 로드 및 설정
                    Glide.with(holder.itemView.context)
                        .load(userImageUrl)
                        .into(holder.userImageView)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("TAG", "onCancelled: ${databaseError.message}")
            }
        })
        val stepKey = step.key.toString()
        // Firebase Realtime Database에서 댓글 개수 가져오기
        val commentsRef = firebaseDatabase.reference.child("comments")
        commentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var commentCount = 0
                var replyCount = 0

                for (childSnapshot in dataSnapshot.children) {
                    val stepId = childSnapshot.child("stepId").value.toString()
                    if (stepId == stepKey) {
                        commentCount++

                        // 댓글의 키 저장
                        val commentKey = childSnapshot.key
                        Log.d("commentKey", "$commentKey")

                        // 대댓글 개수 가져오기
                        val repliesRef = firebaseDatabase.reference.child("replys")
                        repliesRef.addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                for (replySnapshot in dataSnapshot.children) {
                                    val parentKey = replySnapshot.child("parentkey").value.toString()
                                    if (parentKey == commentKey) {
                                        replyCount++
                                        Log.d("replyCount", "$replyCount")
                                    }
                                }
                                // 댓글과 대댓글 개수 표시
                                holder.commentCountTextView.text = commentCount.toString()
                                holder.replyCountTextView.text = replyCount.toString()
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                Log.d("TAG", "onCancelled: ${databaseError.message}")
                            }
                        })
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("TAG", "onCancelled: ${databaseError.message}")
            }
        })
    }

    fun calculateTimeAgo(pastTimestamp: Long, currentTimestamp: Long): String {
        val past = Calendar.getInstance()
        val current = Calendar.getInstance()
        past.timeInMillis = pastTimestamp
        current.timeInMillis = currentTimestamp



        val timeInMilli = kotlin.math.abs(current.timeInMillis - past.timeInMillis)

        val days = TimeUnit.MILLISECONDS.toDays(timeInMilli)
        val hours = TimeUnit.MILLISECONDS.toHours(timeInMilli) - TimeUnit.DAYS.toHours(days)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMilli) - TimeUnit.DAYS.toMinutes(days) - TimeUnit.HOURS.toMinutes(hours)

        if (days > 365) {
            val years = days / 365
            return "$years 년 전"
        } else if (days >= 30) {
            val months = days / 30
            return "$months 개월 전"
        } else if (days > 0) {
            return "$days 일 전"
        } else if (hours > 0) {
            return "$hours 시간 전"
        } else if (minutes > 0) {
            return "$minutes 분 전"
        } else {
            return "방금"
        }
    }

    override fun getItemCount(): Int {
        return stepList.size
    }
}


