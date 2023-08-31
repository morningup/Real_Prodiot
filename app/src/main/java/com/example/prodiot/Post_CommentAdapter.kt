package com.example.prodiot

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageButton
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.greenrobot.eventbus.EventBus

class Post_CommentAdapter(private val postComments: MutableList<Post_Comment>) : RecyclerView.Adapter<Post_CommentAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val authorTextView: TextView = itemView.findViewById(R.id.commentAuthorTextView)
        val contentTextView: TextView = itemView.findViewById(R.id.commentTextView)
        val deleteButton: AppCompatImageButton = itemView.findViewById(R.id.delete_button)
        val replyButton: AppCompatImageButton = itemView.findViewById(R.id.reply_button)
        private val replyRecyclerView: RecyclerView = itemView.findViewById(R.id.replyRecyclerView)
        private val replyAdapter = Post_ReplyAdapter(mutableListOf()) // ReplyAdapter를 ViewHolder 내부에서 선언

        init {
            replyRecyclerView.adapter = replyAdapter
            replyRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
        }

        fun bindReplies(replies: MutableList<Post_Reply>) {
            replyAdapter.setReplies(replies)
        }

        init {
            itemView.setOnLongClickListener(object : View.OnLongClickListener {
                override fun onLongClick(v: View): Boolean {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val selectedAuthor = "@${postComments[position].author} "
                        EventBus.getDefault().post(CommentAuthorSelectedEvent(selectedAuthor))
                    }
                    return true
                }
            })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.comment_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = postComments[position]
        // Post_Comment 객체에서 commentKey를 가져옵니다.
        val commentKey = comment.key
        getRepliesForComment(commentKey.toString()) { replies ->
            holder.bindReplies(replies)
        }

        holder.authorTextView.text = comment.author
        holder.contentTextView.text = comment.content
        holder.deleteButton.setOnClickListener {
            val selectedComment = postComments[position].key
            if (selectedComment != null) {
                deleteComment(selectedComment)
                Toast.makeText(holder.itemView.context, "댓글 삭제 완료", Toast.LENGTH_SHORT).show()
            }
        }

        // Shared Preferences를 초기화합니다.
        val context = holder.itemView.context
        val sharedPreferences = context.getSharedPreferences("selectedComment", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        // selectedComment 값을 저장합니다.
        editor.putBoolean("isReply", false)
        editor.apply()
        holder.replyButton.setOnClickListener {
            val selectedComment = postComments[position].key
            // selectedComment 값을 저장합니다.
            editor.putString("selectedComment", selectedComment)
            editor.putBoolean("isReply", true)
            editor.apply()
        }


    }

    override fun getItemCount(): Int {
        return postComments.size
    }

    fun deleteComment(commentKey: String) {
        val database = FirebaseDatabase.getInstance()
        val commentsRef = database.reference.child("comments")
        val repliesRef = database.reference.child("replys") // 대댓글에 대한 레퍼런스 추가
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
                commentsRef.child(commentKey).removeValue().addOnCompleteListener {
                    notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // 에러 처리 로직을 추가합니다.
            }
        })
    }


    private fun getRepliesForComment(commentKey: String, callback: (MutableList<Post_Reply>) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val repliesRef = database.reference.child("replys")
            .orderByChild("parentkey").equalTo(commentKey)
        val repliesList = mutableListOf<Post_Reply>()
        repliesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                repliesList.clear()
                for (childSnapshot in snapshot.children) {
                    val postReply = childSnapshot.getValue(Post_Reply::class.java)
                    postReply?.let {
                        val key = childSnapshot.key // 아이템의 키값 가져오기
                        Log.d("FreeboardView", "post: $key")
                        it.key = key // Post_Comment 객체에 키값 저장
                        Log.d("FreeboardView", "post: $it")
                        repliesList.add(it)
                    }
                }
                // 데이터를 모두 가져온 후에 콜백을 호출하여 목록을 전달합니다.
                callback(repliesList)
            }

            override fun onCancelled(error: DatabaseError) {
                // 에러 처리 로직을 추가합니다.
            }
        })
    }
}

class Post_ReplyAdapter(private var replies: MutableList<Post_Reply>) : RecyclerView.Adapter<Post_ReplyAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val replyAuthorTextView: TextView = itemView.findViewById(R.id.replyAuthorTextView)
        val replyTextView: TextView = itemView.findViewById(R.id.replyTextView)
        val replydeleteButton: AppCompatImageButton = itemView.findViewById(R.id.reply_delete_button)

        init {
            itemView.setOnLongClickListener(object : View.OnLongClickListener {
                override fun onLongClick(v: View): Boolean {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val selectedAuthor = "@${replies[position].author} "
                        EventBus.getDefault().post(CommentAuthorSelectedEvent(selectedAuthor))
                    }
                    return true
                }
            })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.reply_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reply = replies[position]
        // Post_Comment 객체에서 commentKey를 가져옵니다.
        val replyKey = reply.key
        holder.replyAuthorTextView.text = reply.author
        holder.replyTextView.text = reply.content
        holder.replydeleteButton.setOnClickListener {
            Toast.makeText(holder.itemView.context, "대댓글 삭제 완료", Toast.LENGTH_SHORT).show()
            val selectedReply = replies[position].key
            if (selectedReply != null) {
                deleteReply(selectedReply)
            }
        }
    }

    override fun getItemCount(): Int {
        return replies.size
    }

    fun setReplies(replies: MutableList<Post_Reply>) {
        this.replies = replies
        notifyDataSetChanged()
    }

    fun deleteReply(replyKey: String) {
        val database = FirebaseDatabase.getInstance()
        val replysRef = database.reference.child("replys")
        replysRef.child(replyKey).removeValue().addOnCompleteListener {
            notifyDataSetChanged()
        }
    }
}

class CommentAuthorSelectedEvent(val selectedAuthor: String)




